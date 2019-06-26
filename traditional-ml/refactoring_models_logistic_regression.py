from random import uniform
from warnings import simplefilter

from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import cross_validate, RandomizedSearchCV

from file_utils import print_scores_1, print_best_parameters
from main import N_CV, N_ITER

simplefilter(action='ignore', category=FutureWarning)


def run_logistic_regression(x, columns, y, f):
    model = LogisticRegression(solver='lbfgs', max_iter=3000)

    param_dist = {"C": [uniform(0.01, 100) for i in range(0, 10)]}
    search = RandomizedSearchCV(model, param_dist,
                                n_iter=N_ITER, cv=N_CV, iid=False,
                                n_jobs=-1)

    scores = cross_validate(search, x, y, cv=N_CV, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'])

    print_best_parameters(f, search)
    print_scores_1(scores, search.best_estimator_, columns, f)

    return search.best_estimator_
