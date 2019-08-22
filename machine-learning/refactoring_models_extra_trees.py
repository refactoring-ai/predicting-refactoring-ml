from sklearn.ensemble import ExtraTreesClassifier

from configs import N_ITER, N_CV
from utils.date_utils import now
from ml.output import print_best_parameters, print_scores_2
from sklearn.model_selection import RandomizedSearchCV, cross_validate

def run_extra_trees(dataset, refactoring_name, model_name, x, columns, y, f):
    model = ExtraTreesClassifier(random_state=42)

    # search
    param_dist = {"max_depth": [3, 6, 12, 24, None],
                  "max_leaf_nodes": [1, 2, 3, 5, 6, 10, None],
                  "max_features": ["auto", "sqrt", "log2", None],
                  "min_samples_split": [2, 3, 4, 5, 10],
                  "min_samples_leaf": [1, 2, 3, 4, 5, 10],
                  "bootstrap": [True, False],
                  "criterion": ["gini", "entropy"],
                  "n_estimators": [10, 50, 100, 150, 200]}

    search = RandomizedSearchCV(model, param_dist,
                                n_iter=N_ITER, cv=N_CV, iid=False, n_jobs=-1, verbose=2)

    print("Search started at %s\n" % now())
    f.write("Search started at %s\n" % now())

    search.fit(x, y)
    print_best_parameters(f, search)

    # model for the cross-validation
    model_for_cv = ExtraTreesClassifier(random_state=42,
                                        max_depth=search.best_params_["max_depth"],
                                        max_leaf_nodes=search.best_params_["max_leaf_nodes"],
                                        max_features=search.best_params_["max_features"],
                                        min_samples_split=search.best_params_["min_samples_split"],
                                        min_samples_leaf=search.best_params_["min_samples_leaf"],
                                        bootstrap=search.best_params_["bootstrap"],
                                        criterion=search.best_params_["criterion"],
                                        n_estimators=search.best_params_["n_estimators"])

    print("Cross validation started at %s\n" % now())
    f.write("Cross validation started at %s\n" % now())

    scores = cross_validate(model_for_cv, x, y, cv=N_CV, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'], verbose=2)

    print_scores_2(dataset, refactoring_name, model_name, search.best_estimator_, columns, f, scores)

    return search.best_estimator_
