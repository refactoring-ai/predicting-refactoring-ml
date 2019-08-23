import joblib


class MLModel(object):
    def name(self):
        return type(self).__name__

    def persist(self, dataset, refactoring_name, model_obj, scaler_obj):
        pass


class SupervisedMLRefactoringModel(MLModel):

    def params_to_tune(self):
        pass

    def model(self, best_params=None):
        pass

    def persist(self,dataset,refactoring_name,model_obj, scaler_obj):
        file_name = "results/model-" + self.name() + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".joblib"
        joblib.dump(model_obj, file_name)

        file_name = "results/scaler-" + self.name() + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".joblib"
        joblib.dump(scaler_obj, file_name)


class DeepMLRefactoringModel(MLModel):

    def run(self, x, y):
        pass

    def persist(self,dataset,refactoring_name,model_obj, scaler_obj):
        file_name = "results/model-" + self.name() + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".hg"
        model_obj.save(file_name)

        file_name = "results/scaler-" + self.name() + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".joblib"
        joblib.dump(scaler_obj, file_name)
