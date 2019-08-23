from random import uniform

from sklearn.svm import SVC

from ml.models.base import SupervisedMLRefactoringModel


class LinearSVMRefactoringModel(SupervisedMLRefactoringModel):
    def params_to_tune(self):
        return {"C": [uniform(0.01, 10) for i in range(0, 10)],
                  "kernel": ["linear"],
                  "shrinking": [False]
                  }

    def model(self, best_params=None):
        if best_params is not None:
            return SVC(shrinking=False, C=best_params["C"], kernel=best_params["kernel"])

        return SVC(shrinking=False)



class NonLinearSVMRefactoringModel(SupervisedMLRefactoringModel):
    def params_to_tune(self):
        return {"C": [uniform(0.01, 10) for i in range(0, 10)],
                  "kernel": ["poly", "rbf", "sigmoid"],
                  "degree": [2, 3, 5, 7, 10],
                  "gamma": [uniform(0.01, 10) for i in range(0, 10)],
                  "decision_function_shape": ["ovo", "ovr"]}

    def model(self, best_params=None):
        if best_params is not None:
            return SVC(C=best_params["C"], kernel=best_params["kernel"],
                       degree=best_params["degree"], gamma=best_params["gamma"],
                       decision_function_shape=best_params["decision_function_shape"])

        return SVC(shrinking=False)


