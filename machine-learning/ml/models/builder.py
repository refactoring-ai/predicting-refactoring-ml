from configs import MODELS, DEEP_MODELS
from ml.models.decision_tree import DecisionTreeRefactoringModel
from ml.models.extra_tree import ExtraTreeRefactoringModel
from ml.models.logistic_regression import LogisticRegressionRefactoringModel
from ml.models.naive_bayes import GaussianNaiveBayesRefactoringModel
from ml.models.neural_network import NeuralNetworkDeepRefactoringModel
from ml.models.random_forest import RandomForestRefactoringModel
from ml.models.svm import LinearSVMRefactoringModel, NonLinearSVMRefactoringModel


def build_models():
    models = []
    for model_name in MODELS:
        if model_name == 'svm':
            models.append(LinearSVMRefactoringModel())
        elif model_name == 'random-forest':
            models.append(RandomForestRefactoringModel())
        elif model_name == 'decision-tree':
            models.append(DecisionTreeRefactoringModel())
        elif model_name == 'logistic-regression':
            models.append(LogisticRegressionRefactoringModel())
        elif model_name == 'svm-non-linear':
            models.append(NonLinearSVMRefactoringModel())
        elif model_name == 'naive-bayes':
            models.append(GaussianNaiveBayesRefactoringModel())
        elif model_name == 'extra-trees':
            models.append(ExtraTreeRefactoringModel())
    return models


def build_deep_models():
    models = []
    for model_name in DEEP_MODELS:
        if model_name == 'neural-network':
            models.append(NeuralNetworkDeepRefactoringModel())
    return models