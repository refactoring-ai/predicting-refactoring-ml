import warnings
from random import uniform

from sklearn.model_selection import RandomizedSearchCV, cross_validate
from sklearn.svm import SVC

from configs import N_ITER_SVM, N_CV_SVM
from utils.date_utils import now
from ml.output import print_scores_1, print_best_parameters

warnings.filterwarnings("ignore")


def run_svm(dataset, refactoring_name, model_name, x, columns, y, f):
    model = SVC(verbose=True,shrinking=False)

    param_dist = {"C": [uniform(0.01, 10) for i in range(0, 10)],
                  "kernel": ["linear"],
                  "shrinking": [False]
                  }

    search = RandomizedSearchCV(model, param_dist,
                                n_iter=N_ITER_SVM, cv=N_CV_SVM, iid=False, n_jobs=-1, verbose=2)

    print("Search started at %s\n" % now())
    f.write("Search started at %s\n" % now())

    search.fit(x, y)
    print_best_parameters(f, search)

    # cv
    model_for_cv = SVC(shrinking=False, C=search.best_params_["C"], kernel=search.best_params_["kernel"],verbose=True)

    print("Cross validation started at %s\n" % now())
    f.write("Cross validation started at %s\n" % now())

    scores = cross_validate(model_for_cv, x, y, cv=N_CV_SVM, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'], verbose=2)

    print_scores_1(dataset, refactoring_name, model_name, scores, search.best_estimator_, columns, f)

    return search.best_estimator_
