from configs import MODELS, DEEP_MODELS
from ml.models.decision_tree import DecisionTreeRefactoringModel
from ml.models.extra_tree import ExtraTreeRefactoringModel
from ml.models.logistic_regression import LogisticRegressionRefactoringModel
from ml.models.naive_bayes import GaussianNaiveBayesRefactoringModel
from ml.models.neural_network import NeuralNetworkDeepRefactoringModel
from ml.models.random_forest import RandomForestRefactoringModel
from ml.models.svm import LinearSVMRefactoringModel, NonLinearSVMRefactoringModel


def build_models():
    all_available_models = {
        "svm": LinearSVMRefactoringModel(),
        "random-forest": RandomForestRefactoringModel(),
        "decision-tree": DecisionTreeRefactoringModel(),
        "logistic-regression": LogisticRegressionRefactoringModel(),
        "svm-non-linear": NonLinearSVMRefactoringModel(),
        "naive-bayes": GaussianNaiveBayesRefactoringModel(),
        "extra-trees": ExtraTreeRefactoringModel()
    }

    return [all_available_models[model] for model in MODELS]


def build_deep_models():
    models = []
    for model_name in DEEP_MODELS:
        if model_name == 'neural-network':
            models.append(NeuralNetworkDeepRefactoringModel())
    return models
