import warnings

from scipy.stats import randint
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, RandomizedSearchCV, cross_validate
from sklearn.utils.multiclass import unique_labels

from file_utils import print_scores_2, print_best_parameters
from ml_utils import plot_confusion_matrix

warnings.filterwarnings("ignore")


def run_random_forest(x, columns, y, f, refactoring_name, cm=False):

    # run randomized search
    model = RandomForestClassifier(random_state=42, n_jobs=-1)

    param_dist = {"max_depth": [3, 6, 12, 24, None],
                  "max_features": randint(1, 11),
                  "min_samples_split": randint(2, 11),
                  "bootstrap": [True, False],
                  "criterion": ["gini", "entropy"]}

    print("Performing hyper parameter tuning")
    n_iter_search = 100
    tuned_model = RandomizedSearchCV(model, param_distributions=param_dist,
                                     n_iter=n_iter_search, cv=10, iid=False, n_jobs=-1)

    tuned_model.fit(x, y)

    # print best parameters
    print_best_parameters(f, tuned_model)

    # print the confusion matrix (not needed now)
    if cm:
        X_train, X_test, y_train, y_test = train_test_split(x, y,
                                                            test_size=0.3,
                                                            random_state=42)
        tuned_model.fit(X_train, y_train)

        # plot confusion matrix
        y_pred = tuned_model.predict(X_test)
        classes = unique_labels(y_test, y_pred)
        plot_confusion_matrix(y_test, y_pred, classes,
                              title="{} with Random Forest".format(refactoring_name))

    # perform 10-fold validation
    model_for_cv = RandomForestClassifier(random_state=42, n_jobs=-1,
                                        max_depth=tuned_model.best_params_["max_depth"],max_features=tuned_model.best_params_["max_features"],
                                        min_samples_split=tuned_model.best_params_["min_samples_split"], bootstrap=tuned_model.best_params_["bootstrap"],
                                        criterion=tuned_model.best_params_["criterion"])

    scores = cross_validate(model_for_cv, x, y, cv=10, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'])

    print_scores_2(tuned_model.best_estimator_, columns, f, scores)

    return tuned_model.best_estimator_

