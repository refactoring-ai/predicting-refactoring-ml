import warnings
from random import uniform

from scipy.stats import randint
from sklearn.model_selection import RandomizedSearchCV, cross_validate
from sklearn.svm import SVC

from file_utils import print_scores_1, print_best_parameters
from main import N_ITER, N_CV

warnings.filterwarnings("ignore")


def run_svm(x, columns, y, f):
    model = SVC()

    param_dist = {"C": [uniform(0.01, 10) for i in range(0, 10)],
                  "kernel": ["linear"],  # , "poly", "rbf", "sigmoid"
                  "degree": randint(2, 5),
                  "gamma": [uniform(0.01, 10) for i in range(0, 10)],
                  "decision_function_shape": ["ovo", "ovr"]}

    search = RandomizedSearchCV(model, param_dist,
                                n_iter=N_ITER, cv=N_CV, iid=False, n_jobs=-1)

    scores = cross_validate(search, x, y, cv=N_CV, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'])

    print_best_parameters(f, search)
    print_scores_1(scores, search.best_estimator_, columns, f)

    return search.best_estimator_
