from random import uniform
from warnings import simplefilter

from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import cross_validate, train_test_split, RandomizedSearchCV
from sklearn.utils.multiclass import unique_labels

from file_utils import print_scores_1, print_best_parameters
from ml_utils import plot_confusion_matrix

simplefilter(action='ignore', category=FutureWarning)


def run_logistic_regression(x, columns, y, f, refactoring_name, cm=False):
    model = LogisticRegression(solver='lbfgs', max_iter=3000)

    # run randomized search
    print("Performing hyper parameter tuning")
    param_dist = {"C": [uniform(0.01, 100) for i in range(0, 10)]}
    n_iter_search = 100
    tuned_model = RandomizedSearchCV(model, param_distributions=param_dist,
                                     n_iter=n_iter_search, cv=10, iid=False,
                                     n_jobs=-1)

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
                              title="{} with Logistic Regression".format(refactoring_name))

    # create a clean model, now with the best parameters
    best_model = LogisticRegression(solver='lbfgs', max_iter=3000, C=tuned_model.best_params_["C"])

    # apply 10-fold validation
    scores = cross_validate(best_model, x, y, cv=10, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'])

    print_scores_1(scores, tuned_model.best_estimator_, columns, f)

    return tuned_model.best_estimator_
