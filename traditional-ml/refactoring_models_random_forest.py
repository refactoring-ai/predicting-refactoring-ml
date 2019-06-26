import warnings

from scipy.stats import randint
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import RandomizedSearchCV, cross_validate

from configs import N_ITER, N_CV
from file_utils import print_scores_2, print_best_parameters

warnings.filterwarnings("ignore")


def run_random_forest(x, columns, y, f):
    model = RandomForestClassifier(random_state=42, n_jobs=-1)

    param_dist = {"max_depth": [3, 6, 12, 24, None],
                  "max_features": randint(1, 11),
                  "min_samples_split": randint(2, 11),
                  "bootstrap": [True, False],
                  "criterion": ["gini", "entropy"],
                  "n_estimators": [10, 50, 100]}

    search = RandomizedSearchCV(model, param_dist,
                                n_iter=N_ITER, cv=N_CV, iid=False, n_jobs=-1)

    scores = cross_validate(search, x, y, cv=N_CV, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'])

    print_best_parameters(f, search)
    print_scores_2(search.best_estimator_, columns, f, scores)

    return search.best_estimator_
