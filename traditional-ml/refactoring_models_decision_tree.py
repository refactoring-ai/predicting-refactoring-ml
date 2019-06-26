import warnings

from scipy.stats import randint
from sklearn.model_selection import RandomizedSearchCV, cross_validate
from sklearn.tree import DecisionTreeClassifier

from configs import N_ITER, N_CV
from file_utils import print_scores_2, print_best_parameters

warnings.filterwarnings("ignore")


def run_decision_tree(x, columns, y, f):
    model = DecisionTreeClassifier(random_state=42)

    param_dist = {"max_depth": [3, 6, 12, 24, None],
                  "max_features": ["auto", "sqrt", "log2", None],
                  "min_samples_split": randint(2, 11),
                  "splitter": ["best", "random"],
                  "criterion": ["gini", "entropy"]}

    search = RandomizedSearchCV(model, param_dist,
                                n_iter=N_ITER, cv=N_CV, iid=False, n_jobs=-1)

    scores = cross_validate(search, x, y, cv=N_CV, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'])

    print_best_parameters(f, search)
    print_scores_2(search.best_estimator_, columns, f, scores)

    return search.best_estimator_
