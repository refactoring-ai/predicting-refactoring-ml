from sklearn.linear_model import LogisticRegression

import db
import os
import joblib
import pandas as pd

from sklearn.preprocessing import MinMaxScaler
from sklearn.svm import LinearSVC
from sklearn import svm
from sklearn.model_selection import cross_val_score, GridSearchCV
from ml_utils import perform_under_sampling, create_persistence_file_name

def run_logistic_regression(m_refactoring, refactorings, non_refactored_methods, f):
    assert refactorings.shape[0] > 0, "No refactorings found"

    # set the prediction variable as true and false in the datasets
    refactorings["prediction"] = 1
    non_refactored_methods["prediction"] = 0

    # now, combine both datasets (with both TRUE and FALSE predictions)
    assert non_refactored_methods.shape[1] == refactorings.shape[1], "number of columns differ from both datasets"
    merged_dataset = pd.concat([refactorings, non_refactored_methods])

    # separate the x from the y (as required by the API)
    x = merged_dataset.drop("prediction", axis=1)
    y = merged_dataset["prediction"]

    # balance the datasets, as we have way more 'non refactored examples' rather than refactoring examples
    balanced_x, balanced_y = perform_under_sampling(x, y)
    assert balanced_x.shape[0] == balanced_y.shape[0], "Undersampling did not work"

    # apply some scaling to speed up the algorithm
    scaler = MinMaxScaler()  # Default behavior is to scale to [0,1]
    balanced_x = scaler.fit_transform(balanced_x)

    #create (or load) the logistic regression model
    print("Starting the SVM training for %s" % m_refactoring)
    persistence_file_name_without_extension = create_persistence_file_name(f, m_refactoring)
    if(os.path.isfile(persistence_file_name_without_extension  + '.joblib')):
        print("Loading preexisting model for %s" % m_refactoring)
        model = joblib.load(persistence_file_name_without_extension + '.joblib')
    else:
        print("Building model for %s" % m_refactoring)
        model = LogisticRegression()
        #fit model
        model.fit(balanced_x, balanced_y)
        #save model to file
        joblib.dump(model, persistence_file_name_without_extension + '.joblib')

    # apply 10-fold validation
    scores = cross_val_score(model, balanced_x, balanced_y, cv=10)
    print(scores)
    print("Accuracy: %0.2f (+/- %0.2f)" % (scores.mean(), scores.std() * 2))

    print("Features")
    print(model.coef_)

    precision = cross_val_score(model, balanced_x, balanced_y, scoring='precision', cv=10)
    recall = cross_val_score(model, balanced_x, balanced_y, scoring='recall', cv=10)
    precision_scores_str = "Precision scores: " + ', '.join(list([f"{e:.2f}" for e in precision]))
    precision_scores_str += f'\n(Min and max: {precision.min():.2f} and {precision.max():.2f})'
    precision_scores_str += f'\nMean precision: {precision.mean():.2f}'
    recall_scores_str = "Recall scores: " + ', '.join(list([f"{e:.2f}" for e in recall]))
    recall_scores_str += f'\n(Min and max: {recall.min():.2f} and {recall.max():.2f})'
    recall_scores_str += f'\nMean recall: {recall.mean():.2f}'
    print(precision_scores_str)
    print(recall_scores_str)
    print("\n")

    f.write("\n---\n")
    f.write(m_refactoring + "\n")
    f.write("instances: %d\n" % refactorings.shape[0])
    f.write("Accuracy: %0.2f (+/- %0.2f)\n" % (scores.mean(), scores.std() * 2))
    f.write("\nFeatures:\n")
    f.write(', '.join(str(e) for e in list(x.columns.values)))
    f.write("\n")
    f.write("Coefficients:\n")
    f.write(''.join(str(e) for e in model.coef_.tolist()))
    f.write("\n")
    f.write(precision_scores_str)
    f.write("\n")
    f.write(recall_scores_str)
    f.write("\n---\n")

    return model

# might be useful in the future:

# values for the search grid
# Cs = [0.001, 0.01, 0.1, 1, 10]
# param_grid = {'C': Cs}

# search for the best parameters and perform 10-fold cross validation
# gd_sr = GridSearchCV(estimator=model,
#                      param_grid=param_grid,
#                      scoring='accuracy',
#                      cv=10,
#                      n_jobs=20)
#
# gd_sr.fit(balanced_x, balanced_y)
#
# best_parameters = gd_sr.best_params_
# print(best_parameters)
# best_result = gd_sr.best_score_
# print("Best result: " + str(best_result))
