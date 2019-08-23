class SupervisedMLRefactoringModel(object):

    def params_to_tune(self):
        pass

    def model(self, best_params=None):
        pass

    def output_function(self):
        pass


class DeepMLRefactoringModel(object):

    def run(self,dataset, model, refactoring_name, scaler, x, y):
        pass

