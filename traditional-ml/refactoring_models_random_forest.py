from sklearn.ensemble import RandomForestClassifier
from sklearn.utils.multiclass import unique_labels
from sklearn.model_selection import cross_val_score, train_test_split, RandomizedSearchCV
from ml_utils import plot_confusion_matrix
from scipy.stats import randint
import json
import warnings

warnings.filterwarnings("ignore")


def run_random_forest(x, y, f, refactoring_name):

    #use all available cores by setting n_jobs=-1.
    model = RandomForestClassifier(random_state=42, n_jobs=-1)

    param_dist = {"max_depth": [3, 6, 12, 24, None],
                  "max_features": randint(1, 11),
                  "min_samples_split": randint(2, 11),
                  "bootstrap": [True, False],
                  "criterion": ["gini", "entropy"]}

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

    #fit model
    tuned_model.fit(x, y)

    #perform 10-fold validation 
    scores = cross_val_score(tuned_model, x, y, cv=10)
    print(scores)
    print("Accuracy: %0.2f (+/- %0.2f)" % (scores.mean(), scores.std() * 2))
    #show feature importances ANICHE
    # feature_importances_str = ''.join(["%-33s: %-5.4f\n" % (feature, importance) for feature, importance in 
    #     zip(x.columns.values, tuned_model.feature_importances_)])

    precision = cross_val_score(tuned_model, x, y, scoring='precision', cv=10)
    recall = cross_val_score(tuned_model, x, y, scoring='recall', cv=10)
    precision_scores_str = "Precision scores: " + ', '.join(list([f"{e:.2f}" for e in precision]))
    precision_scores_str += f'\n(Min and max: {precision.min():.2f} and {precision.max():.2f})'
    precision_scores_str += f'\nMean precision: {precision.mean():.2f}'
    recall_scores_str = "Recall scores: " + ', '.join(list([f"{e:.2f}" for e in recall]))
    recall_scores_str += f'\n(Min and max: {recall.min():.2f} and {recall.max():.2f})'
    recall_scores_str += f'\nMean recall: {recall.mean():.2f}'

    # output results to file
    f.write("Accuracy: %0.2f (+/- %0.2f)\n" % (scores.mean(), scores.std() * 2))
    # ANICHE
    # f.write("\nFeature Importances\n")
    # f.write(feature_importances_str)
    # f.write("\n")
    f.write(precision_scores_str)
    f.write("\n")
    f.write(recall_scores_str)
    f.write("\n---\n")

    return tuned_model

