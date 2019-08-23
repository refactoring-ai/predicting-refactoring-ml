import traceback

from sklearn.model_selection import RandomizedSearchCV, cross_validate

from configs import N_ITER, N_CV, SEARCH
from utils.log import log
from ml.output import print_best_parameters
from ml.preprocessing import retrieve_labelled_instances
from utils.date_utils import now
from utils.ml_utils import save_object


class MLPipeline:

    def __init__(self, models_to_run, deep_models_to_run, refactorings, datasets):
        self.deep_models_to_run = deep_models_to_run
        self._models_to_run = models_to_run
        self._refactorings = refactorings
        self._datasets = datasets
        self._start_hour = None
        self._current_execution_number = 0

    def run(self):
        pass

    def _finish_time(self, dataset, model_name, refactoring_name):
        finish_hour = now()
        log("Finished at %s" % finish_hour)
        log(
            ("TIME,%s,%s,%s,%s,%s" % (dataset, refactoring_name, model_name, self._start_hour, finish_hour)))

    def _start_time(self):
        self._count_execution()
        self._start_hour = now()
        log("Started at %s" % self._start_hour)

    def _total_number_of_executions(self):
        return len(self._models_to_run)*len(self._refactorings)*len(self._datasets)+len(self.deep_models_to_run) * len(self._refactorings) * len(self._datasets)

    def _count_execution(self):
        self._current_execution_number = self._current_execution_number+1
        log("Execution: {}/{}".format(self._current_execution_number, self._total_number_of_executions()))
        pass



class BinaryClassificationPipeline(MLPipeline):

    def __init__(self, models_to_run, deep_models_to_run, refactorings, datasets):
        super(models_to_run, deep_models_to_run, refactorings, datasets)

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
                        self._run_single_model(dataset, model, refactoring_name, scaler, x, y)
                        self._finish_time(dataset, model_name, refactoring_name)
                    except Exception as e:
                        print("An error occurred while working on refactoring " + refactoring_name + " model " + model)
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
                        log("An error occurred while working on refactoring " + refactoring_name + " model " + model)
                        log(str(e))
                        log(str(traceback.format_exc()))

    def _run_single_model(self, dataset, model, refactoring_name, scaler, x, y):
        model = model.model()

        # start the search
        param_dist = model.params_to_tune()
        search = None

        if SEARCH == 'randomized':
            search = RandomizedSearchCV(model, param_dist, n_iter=N_ITER, cv=N_CV, iid=False, n_jobs=-1)

        log("Search started at %s\n" % now())
        search.fit(x, y)
        print_best_parameters(search)

        # cross-validation
        model_for_cv = model.model(search.best_params_)
        print("Cross validation started at %s\n" % now())
        log("Cross validation started at %s\n" % now())
        scores = cross_validate(model_for_cv, x, y, cv=N_CV, n_jobs=-1,
                                scoring=['accuracy', 'precision', 'recall'])

        # output (both results and models)
        model_name = type(model).__name__
        model.output_function(dataset, refactoring_name, model_name, search.best_estimator_,
                              x.columns.values, scores)
        save_object("model", model, model_name, dataset, refactoring_name)
        save_object("scaler", scaler, model_name, dataset, refactoring_name)
