import joblib


class MLModel(object):
    def name(self):
        return type(self).__name__

    def persist(self, dataset, refactoring_name, features, model_obj, scaler_obj):
        pass

    def _save_scaler(self, dataset, refactoring_name, scaler_obj):
        file_name = "results/scaler-" + self.name() + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".joblib"
        joblib.dump(scaler_obj, file_name)

    def _save_features(self, dataset, refactoring_name, features):
        file_name = "results/features-" + self.name() + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".csv"
        with open(file_name, "w") as f:
            f.write("\n".join(str(item) for item in features))


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

    def persist(self, dataset, refactoring_name, features, model_obj, scaler_obj):
        file_name = "results/model-" + self.name() + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".joblib"
        joblib.dump(model_obj, file_name)

        self._save_scaler(dataset, refactoring_name, scaler_obj)
        self._save_features(dataset, refactoring_name, features)