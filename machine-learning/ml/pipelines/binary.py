import traceback

from sklearn.model_selection import RandomizedSearchCV, cross_validate, StratifiedKFold, GridSearchCV

from configs import SEARCH, N_CV_SEARCH, N_ITER_RANDOM_SEARCH, N_CV
from ml.pipelines.pipelines import MLPipeline
from ml.preprocessing.preprocessing import retrieve_labelled_instances
from ml.utils.output import format_results, format_best_parameters
from utils.date_utils import now
from utils.log import log


class BinaryClassificationPipeline(MLPipeline):

    def __init__(self, models_to_run, refactorings, datasets):
        super().__init__(models_to_run, refactorings, datasets)

    def run(self):

        for dataset in self._datasets:
            log("Dataset {}".format(dataset))

            for refactoring in self._refactorings:
                refactoring_name = refactoring.name()
                log("Refactoring %s" % refactoring_name)

                features, x, y, scaler = retrieve_labelled_instances(dataset, refactoring)

                for model in self._models_to_run:
                    model_name = model.name()

                    try:
                        log("Model {}".format(model.name()))
                        self._start_time()
                        precision_scores, recall_scores, accuracy_scores, model_to_save = self._run_single_model(dataset, model, refactoring, features, x, y, scaler)

                        # log the results
                        log(format_results(dataset, refactoring_name, model_name, precision_scores, recall_scores, accuracy_scores, model_to_save, features))

                        # we save the best estimator we had during the search
                        model.persist(dataset, refactoring_name, model_to_save, scaler)

                        self._finish_time(dataset, model, refactoring)
                    except Exception as e:
                        print(e)
                        print(str(traceback.format_exc()))

                        log("An error occurred while working on refactoring " + refactoring_name + " model " + model.name())
                        log(str(e))
                        log(str(traceback.format_exc()))

    def _run_single_model(self, dataset, model_def, refactoring, features, x, y, scaler):
        model = model_def.model()

        # start the search
        param_dist = model_def.params_to_tune()
        search = None

        if SEARCH == 'randomized':
            search = RandomizedSearchCV(model, param_dist, n_iter=N_ITER_RANDOM_SEARCH, cv=StratifiedKFold(n_splits=N_CV_SEARCH, shuffle=True), iid=False, n_jobs=-1)
        elif SEARCH == 'grid':
            search = GridSearchCV(model, param_dist, cv=StratifiedKFold(n_splits=N_CV_SEARCH, shuffle=True), iid=False, n_jobs=-1)

        log("Search started at %s\n" % now())
        search.fit(x, y)
        log(format_best_parameters(search))
        best_estimator = search.best_estimator_

        # cross-validation
        model_for_cv = model_def.model(search.best_params_)
        log("Cross validation started at %s\n" % now())
        scores = cross_validate(model_for_cv, x, y, cv=N_CV, n_jobs=-1,
                                scoring=['accuracy', 'precision', 'recall'])

        return scores["test_precision"], scores["test_recall"], scores['test_accuracy'], best_estimator


class DeepLearningBinaryClassificationPipeline(BinaryClassificationPipeline):
    def __init__(self, models_to_run, refactorings, datasets):
        super().__init__(models_to_run, refactorings, datasets)

    def _run_single_model(self, dataset, model_def, refactoring, features, x, y, scaler):
        return model_def.run(dataset, model_def, refactoring, features, x, y, scaler)



