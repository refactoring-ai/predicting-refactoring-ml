from sklearn.naive_bayes import GaussianNB

from ml.models.base import SupervisedMLRefactoringModel


class GaussianNaiveBayesRefactoringModel(SupervisedMLRefactoringModel):
    def params_to_tune(self):
        return {"var_smoothing": [1e-10, 1e-09, 1e-08, 1e-07, 1e-06, 1e-05]}

    def model(self, best_params=None):
        if best_params is not None:
            return GaussianNB(var_smoothing=best_params["var_smoothing"])

        return GaussianNB()


