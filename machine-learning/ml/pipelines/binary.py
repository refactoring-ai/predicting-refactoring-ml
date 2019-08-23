import traceback

from sklearn.model_selection import RandomizedSearchCV, cross_validate

from configs import SEARCH, N_CV, N_ITER
from ml.output import format_results, format_best_parameters
from ml.pipelines.pipelines import MLPipeline
from ml.preprocessing import retrieve_labelled_instances
from utils.date_utils import now
from utils.log import log
from utils.ml_utils import save_object


def _run_single_model(dataset, model_def, refactoring, features, x, y, scaler):
    model = model_def.model()

    refactoring_name = refactoring.name()
    model_name = model_def.name()

    # start the search
    param_dist = model_def.params_to_tune()
    search = None

    if SEARCH == 'randomized':
        search = RandomizedSearchCV(model, param_dist, n_iter=N_ITER, cv=N_CV, iid=False, n_jobs=-1)

    log("Search started at %s\n" % now())
    search.fit(x, y)
    log(format_best_parameters(search))
    best_estimator = search.best_estimator_

    # cross-validation
    model_for_cv = model_def.model(search.best_params_)
    print("Cross validation started at %s\n" % now())
    log("Cross validation started at %s\n" % now())
    scores = cross_validate(model_for_cv, x, y, cv=N_CV, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'])

    # output (both results and models)
    log(format_results(dataset, refactoring_name, model_name, scores, best_estimator, features))

    # we save the best estimator we had during the search
    model_to_save = best_estimator
    save_object("model", model_to_save, model_name, dataset, refactoring_name)
    save_object("scaler", scaler, model_name, dataset, refactoring_name)


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
                    try:
                        log("Model {}".format(model.name()))
                        self._start_time()
                        _run_single_model(dataset, model, refactoring, features, x, y, scaler)
                        self._finish_time(dataset, model, refactoring)
                    except Exception as e:
                        print("An error occurred while working on refactoring " + refactoring_name + " model " + model.name())
                        print(e)
                        print(traceback.format_exc())

