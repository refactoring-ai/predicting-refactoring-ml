import pandas as pd
import traceback

from sklearn.metrics import make_scorer, accuracy_score, precision_score, recall_score, confusion_matrix
from sklearn.model_selection import RandomizedSearchCV, StratifiedKFold, GridSearchCV, cross_validate, train_test_split

from configs import SEARCH, N_CV_SEARCH, N_ITER_RANDOM_SEARCH, N_CV, TEST_SPLIT_SIZE
from ml.pipelines.pipelines import MLPipeline
from ml.preprocessing.preprocessing import retrieve_labelled_instances
from ml.utils.cm import tp, tn, fn, fp
from ml.utils.output import format_results, format_best_parameters, format_test_results
from utils.date_utils import now
from utils.log import log


class BinaryClassificationPipeline(MLPipeline):
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

        # if there is no datasets, refactorings, and models to run, just stop
        if not self._datasets or not self._refactorings or not self._models_to_run:
            return

        for dataset in self._datasets:
            log("Dataset {}".format(dataset))

            for refactoring in self._refactorings:
                refactoring_name = refactoring.name()
                log("**** Refactoring %s" % refactoring_name)

                features, x, y, scaler = retrieve_labelled_instances(dataset, refactoring)

                # we split in train and test
                # (note that we use the same split for all the models)
                x_train, x_test, y_train, y_test = train_test_split(x, y, test_size=TEST_SPLIT_SIZE, random_state=42)

                for model in self._models_to_run:
                    model_name = model.name()

                    try:
                        log("Model {}".format(model.name()))
                        self._start_time()
                        test_scores, model_to_save = self._run_single_model(model, x, y, x_train, x_test, y_train, y_test)

                        # log test scores
                        log(format_test_results(dataset, refactoring_name, model_name + " test", test_scores["precision"],
                                           test_scores["recall"], test_scores['accuracy'], test_scores['tn'],
                                           test_scores['fp'], test_scores['fn'], test_scores['tp']))

                        # we save the best estimator we had during the search
                        model.persist(dataset, refactoring_name, features, model_to_save, scaler)

                        self._finish_time(dataset, model, refactoring)
                    except Exception as e:
                        print(e)
                        print(str(traceback.format_exc()))

                        log("An error occurred while working on refactoring " + refactoring_name + " model " + model.name())
                        log(str(e))
                        log(str(traceback.format_exc()))


    def _run_single_model(self, model_def, x, y, x_train, x_test, y_train, y_test):
        model = model_def.model()

        # perform the search for the best hyper parameters
        param_dist = model_def.params_to_tune()
        search = None

        # choose which search to apply
        if SEARCH == 'randomized':
            search = RandomizedSearchCV(model, param_dist, n_iter=N_ITER_RANDOM_SEARCH, cv=StratifiedKFold(n_splits=N_CV_SEARCH, shuffle=True), iid=False, n_jobs=-1)
        elif SEARCH == 'grid':
            search = GridSearchCV(model, param_dist, cv=StratifiedKFold(n_splits=N_CV_SEARCH, shuffle=True), iid=False, n_jobs=-1)

        # Train and test the model
        test_scores = self._evaluate_model(search, x_train, x_test, y_train, y_test)

        # Run cross validation on whole dataset and safe production ready model
        super_model = self._build_production_model(model_def, search.best_params_, x, y)

        # return the scores and the best estimator
        return test_scores, super_model


    def _evaluate_model(self, search, x_train, x_test, y_train, y_test):
        log("Test search started at %s\n" % now())
        search.fit(x_train, y_train)
        log(format_best_parameters(search))
        best_estimator = search.best_estimator_

        # Predict unseen results
        y_pred = best_estimator.predict(x_test)

        test_scores = {'accuracy': accuracy_score(y_test, y_pred), 'precision': precision_score(y_test, y_pred),
                        'recall': recall_score(y_test, y_pred), 'tn': confusion_matrix(y_test, y_pred).ravel()[0],
                        'fp': confusion_matrix(y_test, y_pred).ravel()[1],
                        'fn': confusion_matrix(y_test, y_pred).ravel()[2],
                        'tp': confusion_matrix(y_test, y_pred).ravel()[3]}

        return test_scores

    def _build_production_model(self, model_def, best_params, x, y):
        log("Production model build started at %s\n" % now())

        super_model = model_def.model(best_params)
        super_model.fit(x, y)

        return super_model


class DeepLearningBinaryClassificationPipeline(BinaryClassificationPipeline):
    def __init__(self, models_to_run, refactorings, datasets):
        super().__init__(models_to_run, refactorings, datasets)

    def _run_single_model(self, model_def, x, y):
        return model_def.run(x, y)



