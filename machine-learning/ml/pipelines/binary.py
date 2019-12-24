import traceback
from collections import Counter

from sklearn import metrics
from sklearn.metrics import confusion_matrix, make_scorer, accuracy_score, precision_score, recall_score
from sklearn.model_selection import RandomizedSearchCV, StratifiedKFold, GridSearchCV, cross_validate

from configs import SEARCH, N_CV_SEARCH, N_ITER_RANDOM_SEARCH, N_CV
from ml.pipelines.pipelines import MLPipeline
from ml.preprocessing.preprocessing import retrieve_labelled_instances
from ml.utils.output import format_results, format_best_parameters
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

        for dataset in self._datasets:
            log("Dataset {}".format(dataset))

            for refactoring in self._refactorings:
                refactoring_name = refactoring.name()
                log("**** Refactoring %s" % refactoring_name)

                features, x, y, scaler = retrieve_labelled_instances(dataset, refactoring)

                for model in self._models_to_run:
                    model_name = model.name()

                    try:
                        log("Model {}".format(model.name()))
                        self._start_time()
                        precision, recall, accuracy, tn, fp, fn, tp, model_to_save = self._run_single_model(model, x, y)

                        # log the results
                        log(format_results(dataset, refactoring_name, model_name, precision, recall, accuracy, tn, fp, fn, tp, model_to_save, features))

                        # we save the best estimator we had during the search
                        model.persist(dataset, refactoring_name, features, model_to_save, scaler)

                        self._finish_time(dataset, model, refactoring)
                    except Exception as e:
                        print(e)
                        print(str(traceback.format_exc()))

                        log("An error occurred while working on refactoring " + refactoring_name + " model " + model.name())
                        log(str(e))
                        log(str(traceback.format_exc()))


    def _run_single_model(self, model_def, x, y):
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
        search.fit(x, y)
        log(format_best_parameters(search))
        best_estimator = search.best_estimator_

        # cross-validation
        log("Cross validation started at %s\n" % now())

        def tp(y_true, y_pred):
            return confusion_matrix(y_true, y_pred)[0, 0]

        def tn(y_true, y_pred):
            return confusion_matrix(y_true, y_pred)[1, 1]

        def fp(y_true, y_pred):
            return confusion_matrix(y_true, y_pred)[1, 0]

        def fn(y_true, y_pred):
            return confusion_matrix(y_true, y_pred)[0, 1]

        scoring = {'tp': make_scorer(tp), 'tn': make_scorer(tn),
                   'fp': make_scorer(fp), 'fn': make_scorer(fn),
                   'accuracy': make_scorer(accuracy_score),
                   'precision': make_scorer(precision_score),
                   'recall': make_scorer(recall_score)}

        scores = cross_validate(model_def.model(search.best_params_),
                                x, y,
                                cv=StratifiedKFold(n_splits=N_CV,shuffle=True),
                                n_jobs=-1,
                                scoring=scoring)

        # return the scores and the best estimator
        return scores["precision"], scores["recall"], scores['accuracy'], scores['tn'], scores['fp'], scores['fn'], scores['tp'], best_estimator


class DeepLearningBinaryClassificationPipeline(BinaryClassificationPipeline):
    def __init__(self, models_to_run, refactorings, datasets):
        super().__init__(models_to_run, refactorings, datasets)

    def _run_single_model(self, model_def, x, y):
        return model_def.run(x, y)



