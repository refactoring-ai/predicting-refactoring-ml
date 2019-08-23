from utils.date_utils import now
from utils.log import log


class MLPipeline:
    """
    Represents a generic pipeline.

    The `run` method is the one that does the magic. Other methods mostly support logging.
    """

    def __init__(self, models_to_run, refactorings, datasets):
        self._models_to_run = models_to_run
        self._refactorings = refactorings
        self._datasets = datasets
        self._start_hour = None
        self._current_execution_number = 0

    def run(self):
        pass

    def _finish_time(self, dataset, model, refactoring):
        finish_hour = now()
        log("Finished at %s" % finish_hour)
        log(
            ("TIME,%s,%s,%s,%s,%s" % (dataset, refactoring.name(), model.name(), self._start_hour, finish_hour)))

    def _start_time(self):
        self._count_execution()
        self._start_hour = now()
        log("Started at %s" % self._start_hour)

    def _total_number_of_executions(self):
        return len(self._models_to_run)*len(self._refactorings)*len(self._datasets)

    def _count_execution(self):
        self._current_execution_number = self._current_execution_number+1
        log("Execution: {}/{}".format(self._current_execution_number, self._total_number_of_executions()))
        pass



