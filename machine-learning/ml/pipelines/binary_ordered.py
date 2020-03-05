import pandas as pd
import traceback
from collections import Counter

from sklearn.model_selection import RandomizedSearchCV, cross_validate, StratifiedKFold, GridSearchCV

from configs import SEARCH, N_CV_SEARCH, N_ITER_RANDOM_SEARCH, N_CV
from ml.pipelines.pipelines import MLPipeline
from ml.preprocessing.preprocessing import retrieve_labelled_instances, retrieve_ordered_labelled_instances
from ml.utils.output import format_results, format_best_parameters, format_results_single_run
from utils.date_utils import now
from utils.log import log
from sklearn import metrics

class BinaryOrderedClassificationPipeline(MLPipeline):
    """
    Train models for binary classification
    """

    def __init__(self, models_to_run, refactorings, datasets):
        super().__init__(models_to_run, refactorings, datasets)

    def run(self):
        """
        The main method of this pipeline.

        For each combination of dataset, refactoring, and model, it:
        1) Retrieved the labelled instances
        2) Performs the hyper parameter search
        3) Performs k-fold cross-validation
        4) Persists evaluation results and the best model
        """

        for dataset in self._datasets:
            log("Dataset {}".format(dataset))

            for refactoring in self._refactorings:
                refactoring_name = refactoring.name()
                log("**** Refactoring %s" % refactoring_name)

                features, x_train, y_train, x_test, y_test, scaler = retrieve_ordered_labelled_instances(dataset, refactoring)

                log("Final information about the dataset:")
                log("- Train: {}".format(Counter(y_train)))
                log("- Test: {}".format(Counter(y_test)))

                # for debugging purposes, let's save them
                #export_train = pd.concat([x_train, pd.DataFrame(y_train)], axis=1)
                #export_train.to_csv("results/train-{}-{}.csv".format(refactoring_name.replace(" ", ""), dataset))

                #export_test = pd.concat([x_test, pd.DataFrame(y_test)], axis=1)
                #export_test.to_csv("results/test-{}-{}.csv".format(refactoring_name.replace(" ", ""), dataset))

                for model in self._models_to_run:
                    model_name = model.name()

                    # reset the seed
                    pd.np.random.seed(42)

                    try:
                        log("Model {}".format(model.name()))
                        self._start_time()
                        precision, recall, accuracy, tn, fp, fn, tp, model_to_save = self._run_ordered(model, x_train, y_train, x_test, y_test)

                        # log the results
                        log(format_results_single_run(dataset, refactoring_name, model_name, precision, recall, accuracy, tn, fp, fn, tp, model_to_save, features))

                        # we save the best estimator we had during the search
                        model.persist(dataset, refactoring_name, features, model_to_save, scaler)

                        self._finish_time(dataset, model, refactoring)
                    except Exception as e:
                        print(e)
                        print(str(traceback.format_exc()))

                        log("An error occurred while working on refactoring " + refactoring_name + " model " + model.name())
                        log(str(e))
                        log(str(traceback.format_exc()))


    def _run_ordered(self, model_def, x_train, y_train, x_test, y_test):
        model = model_def.model()

        # perform the search for the best hyper parameters
        param_dist = model_def.params_to_tune()
        search = None

        # choose which search to apply
        if SEARCH == 'randomized':
            search = RandomizedSearchCV(model, param_dist, n_iter=N_ITER_RANDOM_SEARCH, cv=StratifiedKFold(n_splits=N_CV_SEARCH, shuffle=True), iid=False, n_jobs=-1)
        elif SEARCH == 'grid':
            search = GridSearchCV(model, param_dist, cv=StratifiedKFold(n_splits=N_CV_SEARCH, shuffle=True), iid=False, n_jobs=-1)

        log("Search started at %s\n" % now())
        search.fit(x_train, y_train)
        log(format_best_parameters(search))

        log("Training again started at %s\n" % now())
        final_model = model_def.model(search.best_params_)
        final_model.fit(x_train, y_train)

        # what's the accuracy of the model?
        log("Test started at %s\n" % now())
        y_pred = final_model.predict(x_test)

        accuracy = metrics.accuracy_score(y_test, y_pred)
        precision = metrics.precision_score(y_test, y_pred)
        recall = metrics.recall_score(y_test, y_pred)
        tn, fp, fn, tp = metrics.confusion_matrix(y_test, y_pred).ravel()

        # return the scores and the final model
        return precision, recall, accuracy, tn, fp, fn, tp, final_model

