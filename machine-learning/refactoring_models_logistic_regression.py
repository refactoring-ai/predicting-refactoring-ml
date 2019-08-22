from random import uniform
from warnings import simplefilter

from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import cross_validate, RandomizedSearchCV

from configs import N_ITER, N_CV
from utils.date_utils import now
from ml.output import print_scores_1, print_best_parameters

simplefilter(action='ignore', category=FutureWarning)


def run_logistic_regression(dataset, refactoring_name, model_name, x, columns, y, f):
    model = LogisticRegression(solver='lbfgs', max_iter=3000, verbose=2)

    # search
    param_dist = {"C": [uniform(0.01, 100) for i in range(0, 10)]}
    search = RandomizedSearchCV(model, param_dist,
                                n_iter=N_ITER, cv=N_CV, iid=False,
                                n_jobs=-1, verbose=2)

    print("Search started at %s\n" % now())
    f.write("Search started at %s\n" % now())

    search.fit(x, y)
    print_best_parameters(f, search)

    # cv
    model_for_cv = LogisticRegression(solver='lbfgs', max_iter=3000, C=search.best_params_["C"], verbose=2)

    print("Cross validation started at %s\n" % now())
    f.write("Cross validation started at %s\n" % now())

    scores = cross_validate(model_for_cv, x, y, cv=N_CV, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'], verbose=2)

    print_scores_1(dataset, refactoring_name, model_name, scores, search.best_estimator_, columns, f)

    return search.best_estimator_
