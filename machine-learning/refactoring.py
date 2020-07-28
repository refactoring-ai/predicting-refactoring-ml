from configs import Level, levelMap
from db.QueryBuilder import get_level_refactorings, get_all_level_stable
from db.DBConnector import execute_query
from utils.log import log


class LowLevelRefactoring:
    _name = ""
    _level = Level.NONE

    def __init__(self, name, level):
        self._name = name
        self._level = level

    def get_refactored_instances(self, dataset):
        return execute_query(get_level_refactorings(int(self._level), self._name, dataset))

    def get_non_refactored_instances(self, dataset):
        return execute_query(get_all_level_stable(int(self._level), dataset))

    def refactoring_level(self) -> str:
        return str(self._level)

    def name(self) -> str:
        return self._name


def build_refactorings():
    all_refactorings = []
    for level in Level:
        for refactoring in levelMap[level]:
            all_refactorings += [LowLevelRefactoring(refactoring, level)]
    log("Trying to fetch " + str(len(all_refactorings)) + " refactoring types.")

    return all_refactorings
