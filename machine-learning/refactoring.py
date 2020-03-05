from configs import CLASS_LEVEL_REFACTORINGS, VARIABLE_LEVEL_REFACTORINGS, METHOD_LEVEL_REFACTORINGS, FIELD_LEVEL_REFACTORINGS
from db.QueryBuilder import get_level_refactorings, get_all_level_stable
from db.DBConnector import execute_query


class LowLevelRefactoring():
    _name = ""

    def __init__(self, name):
        self._name = name

    def get_refactored_instances(self, dataset):
        pass

    def get_non_refactored_instances(self, dataset):
        pass

    def refactoring_level(self):
        pass

    def name(self):
        return self._name


class MethodLevelRefactoring(LowLevelRefactoring):

    def __init__(self, name):
        super().__init__(name)

    def get_refactored_instances(self, dataset):
        return execute_query(get_level_refactorings(2, self._name, dataset))

    def get_non_refactored_instances(self, dataset):
        return execute_query(get_all_level_stable(2, dataset))

    def refactoring_level(self):
        return "method"


class ClassLevelRefactoring(LowLevelRefactoring):

    def __init__(self, name):
        super().__init__(name)

    def get_refactored_instances(self, dataset):
        return execute_query(get_level_refactorings(1, self._name, dataset))

    def get_non_refactored_instances(self, dataset):
        return execute_query(get_all_level_stable(1, dataset))

    def refactoring_level(self):
        return "class"


class VariableLevelRefactoring(LowLevelRefactoring):

    def __init__(self, name):
        super().__init__(name)

    def get_refactored_instances(self, dataset):
        return execute_query(get_level_refactorings(3, self._name, dataset))

    def get_non_refactored_instances(self, dataset):
        return execute_query(get_all_level_stable(3, dataset))

    def refactoring_level(self):
        return "variable"


class FieldLevelRefactoring(LowLevelRefactoring):

    def __init__(self, name):
        super().__init__(name)

    def get_refactored_instances(self, dataset):
        return execute_query(get_level_refactorings(4, self._name, dataset))

    def get_non_refactored_instances(self, dataset):
        return execute_query(get_all_level_stable(4, dataset))

    def refactoring_level(self):
        return "field"


def build_refactorings():
    class_level_refactorings = list(map(lambda r: ClassLevelRefactoring(r), CLASS_LEVEL_REFACTORINGS))
    method_level_refactorings = list(map(lambda r: MethodLevelRefactoring(r), METHOD_LEVEL_REFACTORINGS))
    variable_level_refactorings = list(map(lambda r: VariableLevelRefactoring(r), VARIABLE_LEVEL_REFACTORINGS))
    field_level_refactorings = list(map(lambda r: FieldLevelRefactoring(r), FIELD_LEVEL_REFACTORINGS))

    return class_level_refactorings + method_level_refactorings + variable_level_refactorings,field_level_refactorings
