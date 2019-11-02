from random import uniform

from sklearn.linear_model import LogisticRegression

from ml.models.base import SupervisedMLRefactoringModel


class LogisticRegressionRefactoringModel(SupervisedMLRefactoringModel):
    def params_to_tune(self):
        return {
            "max_iter": [50, 100, 200, 300, 500, 1000, 2000, 5000],
            "C": [uniform(0.01, 100) for i in range(0, 10)]}

    def model(self, best_params=None):
        if best_params is not None:
            return LogisticRegression(solver='lbfgs', max_iter=best_params["max_iter"], C=best_params["C"])

        return LogisticRegression(solver='lbfgs')

