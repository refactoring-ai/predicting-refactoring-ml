import warnings
from random import uniform

from scipy.stats import randint
from sklearn.model_selection import RandomizedSearchCV, cross_validate
from sklearn.svm import SVC

from configs import N_ITER, N_CV
from utils.date_utils import now
from ml.output import print_scores_1

warnings.filterwarnings("ignore")


def run_svm_non_linear(dataset, refactoring_name, model_name, x, columns, y, f):
    model = SVC(verbose=True)

    param_dist = {"C": [uniform(0.01, 10) for i in range(0, 10)],
                  "kernel": ["poly", "rbf", "sigmoid"],
                  "degree": randint(2, 5),
                  "gamma": [uniform(0.01, 10) for i in range(0, 10)],
                  "decision_function_shape": ["ovo", "ovr"]}

    search = RandomizedSearchCV(model, param_dist,
                                n_iter=N_ITER, cv=N_CV, iid=False, n_jobs=-1, verbose=2)

    print("Search started at %s\n" % now())
    f.write("Search started at %s\n" % now())

    search.fit(x, y)

    # cv
    model_for_cv = SVC(C=search.best_params_["C"], kernel=search.best_params_["kernel"],
                       degree=search.best_params_["degree"], gamma=search.best_params_["gamma"],
                       decision_function_shape=search.best_params_["decision_function_shape"], verbose=True)

    print("Cross validation started at %s\n" % now())
    f.write("Cross validation started at %s\n" % now())

    scores = cross_validate(model_for_cv, x, y, cv=N_CV, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'], verbose=2)

    print_scores_1(dataset, refactoring_name, model_name, scores, search.best_estimator_, columns, f)

    return search.best_estimator_
