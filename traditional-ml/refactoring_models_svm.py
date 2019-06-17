import warnings
from random import uniform

from scipy.stats import randint
from sklearn.model_selection import train_test_split, RandomizedSearchCV, cross_validate
from sklearn.svm import SVC
from sklearn.utils.multiclass import unique_labels

from file_utils import print_scores_1, print_best_parameters
from ml_utils import plot_confusion_matrix

warnings.filterwarnings("ignore")


def run_svm(x, columns, y, f, refactoring_name, cm=False):
    model = SVC()

    # run randomized search
    param_dist = {"C": [uniform(0.01, 10) for i in range(0, 10)],
                  "kernel": ["linear", "poly", "rbf", "sigmoid"],
                  "degree": randint(2, 5),
                  "gamma": [uniform(0.01, 10) for i in range(0, 10)],
                  "decision_function_shape": ["ovo", "ovr"]}

    print("Performing hyper parameter tuning")
    n_iter_search = 100
    tuned_model = RandomizedSearchCV(model, param_distributions=param_dist,
                                     n_iter=n_iter_search, cv=10, iid=False, n_jobs=-1)

    tuned_model.fit(x, y)

    # print the best parameters for this model
    print_best_parameters(f, tuned_model)

    # print the confusion matrix (not needed for now)
    if cm:
        # split train and test data
        X_train, X_test, y_train, y_test = train_test_split(x, y,
                                                            test_size=0.3,
                                                            random_state=42)
        tuned_model.fit(X_train, y_train)

        # plot confusion matrix
        y_pred = tuned_model.predict(X_test)
        classes = unique_labels(y_test, y_pred)
        plot_confusion_matrix(y_test, y_pred, classes,
                              title="{} with SVM".format(refactoring_name))

    # apply 10-fold validation
    # create a clean model, now with the best parameters
    best_model = SVC(C=tuned_model.best_params_["C"], kernel=tuned_model.best_params_["kernel"],
                     degree=tuned_model.best_params_["degree"], gamma=tuned_model.best_params_["gamma"],
                     decision_function_shape=tuned_model.best_params_["decision_function_shape"])

    # apply 10-fold validation
    scores = cross_validate(best_model, x, y, cv=10, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'])

    print_scores_1(scores, best_model, columns, f)

    return tuned_model.best_estimator_

