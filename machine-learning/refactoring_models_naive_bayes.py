from warnings import simplefilter

from sklearn.model_selection import cross_validate, RandomizedSearchCV
from sklearn.naive_bayes import GaussianNB

from configs import N_ITER, N_CV
from date_utils import now
from file_utils import print_scores_1, print_best_parameters

simplefilter(action='ignore', category=FutureWarning)


def run_naive_bayes(dataset, refactoring_name, model_name, x, columns, y, f):
    model = GaussianNB()

    # search
    param_dist = {"var_smoothing": [1e-10, 1e-09, 1e-08, 1e-07, 1e-06, 1e-05]}
    search = RandomizedSearchCV(model, param_dist,
                                n_iter=N_ITER, cv=N_CV, iid=False,
                                n_jobs=-1, verbose=2)

    print("Search started at %s\n" % now())
    f.write("Search started at %s\n" % now())

    search.fit(x, y)
    print_best_parameters(f, search)

    # cv
    model_for_cv = GaussianNB(var_smoothing=search.best_params_["var_smoothing"])

    print("Cross validation started at %s\n" % now())
    f.write("Cross validation started at %s\n" % now())

    scores = cross_validate(model_for_cv, x, y, cv=N_CV, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'], verbose=2)

    print_scores_1(dataset, refactoring_name, model_name, scores, search.best_estimator_, columns, f)

    return search.best_estimator_
