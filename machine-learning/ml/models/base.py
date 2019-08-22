from configs import MODELS, DEEP_MODELS
from ml.models.decision_tree import DecisionTreeRefactoringModel
from ml.models.extra_tree import ExtraTreeRefactoringModel
from ml.models.logistic_regression import LogisticRegressionRefactoringModel
from ml.models.naive_bayes import GaussianNaiveBayesRefactoringModel
from ml.models.neural_network import NeuralNetworkDeepRefactoringModel
from ml.models.random_forest import RandomForestRefactoringModel
from ml.models.svm import LinearSVMRefactoringModel, NonLinearSVMRefactoringModel


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


def build_models():
    for model_name in MODELS:
        if model_name == 'svm':
            yield LinearSVMRefactoringModel()
        elif model_name == 'random-forest':
            yield RandomForestRefactoringModel()
        elif model_name == 'decision-tree':
            yield DecisionTreeRefactoringModel()
        elif model_name == 'logistic-regression':
            yield LogisticRegressionRefactoringModel()
        elif model_name == 'svm-non-linear':
            yield NonLinearSVMRefactoringModel()
        elif model_name == 'naive-bayes':
            yield GaussianNaiveBayesRefactoringModel()
        elif model_name == 'extra-trees':
            yield ExtraTreeRefactoringModel()


def build_deep_models():
    for model_name in DEEP_MODELS:
        if model_name == 'neural-network':
            yield NeuralNetworkDeepRefactoringModel()