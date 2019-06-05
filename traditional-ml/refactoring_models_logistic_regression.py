from sklearn.linear_model import LogisticRegression
from sklearn.utils.multiclass import unique_labels
from sklearn.model_selection import cross_val_score, train_test_split, RandomizedSearchCV
from ml_utils import plot_confusion_matrix
from random import uniform
import json
import warnings

warnings.filterwarnings("ignore")


def run_logistic_regression(x, columns, y, f, refactoring_name):

    model = LogisticRegression()

    param_dist = {"penalty": ["l1", "l2", "elasticnet", "none"],
                  "dual": [True, False],
                  "C": [uniform(0.01, 10) for i in range(0,10)],
                  "solver": ["newton-cg", "lbfgs", "liblinear", "sag", "saga"],
                  "multi_class": ["ovr", "multinomial", "auto"]}

    # run randomized search
    print("Performing hyper parameter tuning")
    n_iter_search = 100
    tuned_model = RandomizedSearchCV(model, param_distributions=param_dist,
                                     n_iter=n_iter_search, cv=10, iid=False)

    # split train and test data
    X_train, X_test, y_train, y_test = train_test_split(x, y,
                                                        test_size=0.3,
                                                        random_state=42)
    tuned_model.fit(X_train, y_train)

    best_parameters = tuned_model.best_params_
    f.write("\nHyperparametrization:\n")
    f.write("Best parameters:\n")
    f.write(json.dumps(best_parameters, indent=2))
    print("Best parameters: " + json.dumps(best_parameters, indent=2))
    best_result = tuned_model.best_score_
    print("Best result: " + str(best_result))
    f.write("Best result:\n")
    f.write(str(best_result))

    # plot confusion matrix
    y_pred = tuned_model.predict(X_test)
    classes = unique_labels(y_test, y_pred)
    plot_confusion_matrix(y_test, y_pred, classes,
                          title="{} with Random Forest".format(refactoring_name))

    model.fit(x, y)

    # apply 10-fold validation
    scores = cross_val_score(model, x, y, cv=10)
    print(scores)
    print("Accuracy: %0.2f (+/- %0.2f)" % (scores.mean(), scores.std() * 2))

    precision = cross_val_score(model, x, y, scoring='precision', cv=10)
    recall = cross_val_score(model, x, y, scoring='recall', cv=10)
    precision_scores_str = "Precision scores: " + ', '.join(list([f"{e:.2f}" for e in precision]))
    precision_scores_str += f'\n(Min and max: {precision.min():.2f} and {precision.max():.2f})'
    precision_scores_str += f'\nMean precision: {precision.mean():.2f}'
    recall_scores_str = "Recall scores: " + ', '.join(list([f"{e:.2f}" for e in recall]))
    recall_scores_str += f'\n(Min and max: {recall.min():.2f} and {recall.max():.2f})'
    recall_scores_str += f'\nMean recall: {recall.mean():.2f}'

    f.write("Accuracy: %0.2f (+/- %0.2f)\n" % (scores.mean(), scores.std() * 2))
    f.write("\nFeatures:\n")
    f.write(', '.join(str(e) for e in list(columns)))
    f.write("\n")
    f.write("Coefficients:\n")
    f.write(''.join(str(e) for e in model.coef_.tolist()))
    f.write("\n")
    f.write(precision_scores_str)
    f.write("\n")
    f.write(recall_scores_str)

    return model
