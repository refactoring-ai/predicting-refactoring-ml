from configs import CLASS_LEVEL_REFACTORINGS, VARIABLE_LEVEL_REFACTORINGS, METHOD_LEVEL_REFACTORINGS
from db.refactoringdb import get_method_level_refactorings, get_non_refactored_methods, get_non_refactored_classes, \
    get_class_level_refactorings, get_variable_level_refactorings, get_non_refactored_variables


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
        return get_method_level_refactorings(self._name, dataset)

    def get_non_refactored_instances(self, dataset):
        return get_non_refactored_methods(dataset)

    def refactoring_level(self):
        return "method"


class ClassLevelRefactoring(LowLevelRefactoring):

    def __init__(self, name):
        super().__init__(name)

    def get_refactored_instances(self, dataset):
        return get_class_level_refactorings(self._name, dataset)

    def get_non_refactored_instances(self, dataset):
        return get_non_refactored_classes(dataset)

    def refactoring_level(self):
        return "class"


class VariableLevelRefactoring(LowLevelRefactoring):

    def __init__(self, name):
        super().__init__(name)

    def get_refactored_instances(self, dataset):
        return get_variable_level_refactorings(self._name, dataset)

    def get_non_refactored_instances(self, dataset):
        return get_non_refactored_variables(dataset)

    def refactoring_level(self):
        return "variable"


def build_refactorings():
    class_level_refactorings = list(map(lambda r: ClassLevelRefactoring(r), CLASS_LEVEL_REFACTORINGS))
    method_level_refactorings = list(map(lambda r: MethodLevelRefactoring(r), METHOD_LEVEL_REFACTORINGS))
    variable_level_refactorings = list(map(lambda r: VariableLevelRefactoring(r), VARIABLE_LEVEL_REFACTORINGS))

    return class_level_refactorings + method_level_refactorings + variable_level_refactorings
