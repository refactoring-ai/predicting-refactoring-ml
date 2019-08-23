class MLModel(object):
    def name(self):
        return type(self).__name__


class SupervisedMLRefactoringModel(MLModel):

    def params_to_tune(self):
        pass

    def model(self, best_params=None):
        pass


class DeepMLRefactoringModel(MLModel):

    def run(self, dataset, model, refactoring_name, scaler, x, y):
        pass
