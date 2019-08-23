import traceback

from sklearn.model_selection import RandomizedSearchCV, cross_validate

from configs import SEARCH, N_CV, N_ITER
from ml.output import print_best_parameters
from ml.pipelines.pipelines import MLPipeline
from ml.preprocessing import retrieve_labelled_instances
from utils.date_utils import now
from utils.log import log
from utils.ml_utils import save_object


def _run_single_model(dataset, model_def, refactoring_name, scaler, x, y):
    model = model_def.model()

    # start the search
    param_dist = model_def.params_to_tune()
    search = None

    if SEARCH == 'randomized':
        search = RandomizedSearchCV(model, param_dist, n_iter=N_ITER, cv=N_CV, iid=False, n_jobs=-1)

    log("Search started at %s\n" % now())
    search.fit(x, y)
    print_best_parameters(search)

    # cross-validation
    model_for_cv = model_def.model(search.best_params_)
    print("Cross validation started at %s\n" % now())
    log("Cross validation started at %s\n" % now())
    scores = cross_validate(model_for_cv, x, y, cv=N_CV, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'])

    # output (both results and models)
    model_name = type(model_def).__name__
    model_def.output_function(dataset, refactoring_name, model_name, search.best_estimator_,
                          x.columns.values, scores)

    # we save the best estimator we had during the search
    model_to_save = search.best_estimator_
    save_object("model", model_to_save, model_name, dataset, refactoring_name)
    save_object("scaler", scaler, model_name, dataset, refactoring_name)


class BinaryClassificationPipeline(MLPipeline):

    def __init__(self, models_to_run, deep_models_to_run, refactorings, datasets):
        super().__init__(models_to_run, deep_models_to_run, refactorings, datasets)

    def run(self):

        for dataset in self._datasets:
            log("Dataset {}".format(dataset))

            for refactoring in self._refactorings:
                refactoring_name = refactoring.name
                log("Refactoring %s" % refactoring_name)

                x, y, scaler = retrieve_labelled_instances(dataset, refactoring)

                for model in self._models_to_run:
                    model_name = type(model).__name__

                    try:
                        log("Model {}".format(model_name))
                        self._start_time()
                        _run_single_model(dataset, model, refactoring_name, scaler, x, y)
                        self._finish_time(dataset, model_name, refactoring_name)
                    except Exception as e:
                        print("An error occurred while working on refactoring " + refactoring_name + " model " + model_name)
                        print(e)
                        print(traceback.format_exc())

                # the pipeline for deep learning is different for now.
                # TODO: can we merge both pipelines somehow?
                for model in self._deep_models_to_run:

                    model_name = type(model).__name__

                    try:
                        log("Model {}".format(model_name))
                        self._start_time()
                        model.run(dataset, model, refactoring_name, scaler, x, y)
                        self._finish_time()
                    except Exception as e:
                        log("An error occurred while working on refactoring " + refactoring_name + " model " + model_name)
                        log(str(e))
                        log(str(traceback.format_exc()))
