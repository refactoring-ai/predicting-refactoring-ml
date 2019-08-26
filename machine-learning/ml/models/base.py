import joblib


class MLModel(object):
    def name(self):
        return type(self).__name__

    def persist(self, dataset, refactoring_name, model_obj, scaler_obj):
        pass


class SupervisedMLRefactoringModel(MLModel):
    """
    Represents all supervised ML models.

    They all must have 3 features:
    - params_to_tune: A dictionary with the list of parameters and values for the hyperparameter search.
    - model: Returns a new model. If params is passed, with the configured params.
    - persist: Persists the best found estimator as well as the final model.

    Note: Whenever you create a new model, do not forget to add it to `builder.py`.
    """

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
    """
    Represents all deep learning supervised ML models.
    We are still designing this class.
    """

    def run(self, x, y):
        pass

    def persist(self,dataset,refactoring_name,model_obj, scaler_obj):
        file_name = "results/model-" + self.name() + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".hg"
        model_obj.save(file_name)

        file_name = "results/scaler-" + self.name() + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".joblib"
        joblib.dump(scaler_obj, file_name)
