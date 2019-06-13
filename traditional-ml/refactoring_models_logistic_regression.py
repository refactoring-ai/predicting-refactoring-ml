from sklearn.linear_model import LogisticRegression
from sklearn.utils.multiclass import unique_labels
from sklearn.model_selection import cross_validate, train_test_split, RandomizedSearchCV
from ml_utils import plot_confusion_matrix
from random import uniform
import json
from warnings import simplefilter

simplefilter(action='ignore', category=FutureWarning)

def run_logistic_regression(x, columns, y, f, refactoring_name, cm=False):

    model = LogisticRegression(solver='lbfgs', max_iter=3000)

    param_dist = {"C": [uniform(0.01, 100) for i in range(0,10)]}

    # run randomized search
    print("Performing hyper parameter tuning")
    n_iter_search = 100
    tuned_model = RandomizedSearchCV(model, param_distributions=param_dist,
                                     n_iter=n_iter_search, cv=10, iid=False,
                                     n_jobs=-1)

    tuned_model.fit(x, y)

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
    

    best_parameters = tuned_model.best_params_
    f.write("\nHyperparametrization:\n")
    f.write("Best parameters:\n")
    f.write(json.dumps(best_parameters, indent=2))
    print("Best parameters: " + json.dumps(best_parameters, indent=2))
    best_result = tuned_model.best_score_
    print("Best result: " + str(best_result))
    f.write("Best result:\n")
    f.write(str(best_result))


    # apply 10-fold validation
    scores = cross_validate(tuned_model, x, y, cv=10, n_jobs=-1,
                            scoring=['accuracy', 'precision', 'recall'])
    print(scores)
    print("Accuracy: %0.2f (+/- %0.2f)" % (scores['test_accuracy'].mean(), scores['test_accuracy'].std() * 2))

    precision_scores_str = "Precision scores: " + ', '.join(list([f"{e:.2f}" for e in scores['test_precision']]))
    precision_scores_str += f'\n(Min and max: {scores["test_precision"].min():.2f} and {scores["test_precision"].max():.2f})'
    precision_scores_str += f'\nMean precision: {scores["test_precision"].mean():.2f}'
    recall_scores_str = "Recall scores: " + ', '.join(list([f"{e:.2f}" for e in scores["test_recall"]]))
    recall_scores_str += f'\n(Min and max: {scores["test_recall"].min():.2f} and {scores["test_recall"].max():.2f})'
    recall_scores_str += f'\nMean recall: {scores["test_recall"].mean():.2f}'

    f.write("Accuracy: %0.2f (+/- %0.2f)\n" % (scores['test_accuracy'].mean(), scores['test_accuracy'].std() * 2))
    f.write("\nFeatures:\n")
    f.write(', '.join(str(e) for e in list(columns)))
    f.write("\n")
    f.write("Coefficients:\n")
    f.write(''.join(str(e) for e in tuned_model.coef_.tolist()))
    f.write("\n")
    f.write(precision_scores_str)
    f.write("\n")
    f.write(recall_scores_str)

    return tuned_model.best_estimator_
