class SupervisedMLRefactoringModel(object):

    def params_to_tune(self):
        pass

    def model(self, best_params=None):
        pass

    def name(self):
        return type(self).__name__


class DeepMLRefactoringModel(object):

    def run(self, dataset, model, refactoring_name, scaler, x, y):
        pass
