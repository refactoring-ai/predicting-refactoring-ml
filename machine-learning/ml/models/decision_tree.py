from sklearn.tree import DecisionTreeClassifier

from ml.models.base import SupervisedMLRefactoringModel


class DecisionTreeRefactoringModel(SupervisedMLRefactoringModel):
    def params_to_tune(self):
        return {"max_depth": [3, 6, 12, 24, None],
                  "max_features": ["auto", "sqrt", "log2", None],
                  "min_samples_split": [2, 3, 5, 10, 11],
                  "splitter": ["best", "random"],
                  "criterion": ["gini", "entropy"]}

    def model(self, best_params=None):
        if best_params is not None:
            return DecisionTreeClassifier(random_state=42, max_depth=best_params["max_depth"],
                                   max_features=best_params["max_features"],
                                   min_samples_split=best_params["min_samples_split"],
                                   splitter=best_params["splitter"],
                                   criterion=best_params["criterion"])

        return DecisionTreeClassifier(random_state=42)

