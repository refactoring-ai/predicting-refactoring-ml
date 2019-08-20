import warnings

from scipy.stats import randint
from sklearn.model_selection import RandomizedSearchCV, cross_validate
from sklearn.tree import DecisionTreeClassifier

from configs import N_ITER, N_CV
from date_utils import now
from file_utils import print_scores_2, print_best_parameters

warnings.filterwarnings("ignore")


def run_decision_tree(dataset, refactoring_name, model_name, x, columns, y, f):
    model = DecisionTreeClassifier(random_state=42)

    # search
    param_dist = {"max_depth": [3, 6, 12, 24, None],
                  "max_features": ["auto", "sqrt", "log2", None],
                  "min_samples_split": [2, 3, 5, 10, 11],
                  "splitter": ["best", "random"],
                  "criterion": ["gini", "entropy"]}

    search = RandomizedSearchCV(model, param_dist,
                                n_iter=N_ITER, cv=N_CV, iid=False, n_jobs=-1, verbose=2)

    print("Search started at %s\n" % now())
    f.write("Search started at %s\n" % now())

    search.fit(x, y)
    print_best_parameters(f, search)

    # model for the cross-validation
    model_for_cv = DecisionTreeClassifier(random_state=42, max_depth=search.best_params_["max_depth"],
                                          max_features=search.best_params_["max_features"],
                                          min_samples_split=search.best_params_["min_samples_split"],
                                          splitter=search.best_params_["splitter"],
                                          criterion=search.best_params_["criterion"])

    print("Cross validation started at %s\n" % now())
    f.write("Cross validation started at %s\n" % now())

    scores = cross_validate(model_for_cv, x, y, cv=N_CV, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'], verbose=2)

    print_scores_2(dataset, refactoring_name, model_name, search.best_estimator_, columns, f, scores)

    return search.best_estimator_
