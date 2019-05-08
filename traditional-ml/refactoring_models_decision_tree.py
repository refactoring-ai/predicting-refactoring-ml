import db
import os
import joblib
import pandas as pd

from ml_utils import perform_under_sampling, create_persistence_file_name
from sklearn.preprocessing import MinMaxScaler
from sklearn.tree import DecisionTreeClassifier
from sklearn.model_selection import cross_val_score

def run_decision_tree(m_refactoring, refactorings, non_refactored_methods, f):
    assert refactorings.shape[0] > 0, "No refactorings found"
    #set the prediction variable as true and false in the datasets
    refactorings["prediction"] = 1
    non_refactored_methods["prediction"] = 0

    #combine both datasets (with both TRUE and FALSE predictions)
    assert non_refactored_methods.shape[1] == refactorings.shape[1], "Datasets have a different number of columns"
    merged_dataset = pd.concat([refactorings, non_refactored_methods])

    #separate x from y 
    x = merged_dataset.drop("prediction", axis=1)
    y = merged_dataset["prediction"]

    #balance the datasets 
    #as mentioned elsewhere in the code, we have way more 'non-refactored examples' than refactoring examples
    balanced_x, balanced_y = perform_under_sampling(x, y)
    assert balanced_x.shape[0] == balanced_y.shape[0], "Undersampling did not work while building the decision tree model"

    #perform some scaling to speed up the whole model-building thingy
    scaler = MinMaxScaler()  #default behavior scales data to the following range: (0,1)
    balanced_x = scaler.fit_transform(balanced_x)

    #create or load decision tree classifier object
    print("Starting to build the decision tree for %s" % m_refactoring)
    #load existing model or create and persist a new one 
    persistence_file_name_without_extension = create_persistence_file_name(f, m_refactoring)
    if(os.path.isfile(persistence_file_name_without_extension  + '.joblib')):
        print("Loading preexisting model for %s" % m_refactoring)
        model = joblib.load(persistence_file_name_without_extension + '.joblib')
    else:
        print("Building model for %s" % m_refactoring)
        model = DecisionTreeClassifier(random_state=42)
        #train model
        model.fit(balanced_x, balanced_y)
        #save model to file
        joblib.dump(model, persistence_file_name_without_extension + '.joblib') 
    
    #perform 10-fold validation 
    scores = cross_val_score(model, balanced_x, balanced_y, cv=10)
    print(scores)
    print("Accuracy: %0.2f (+/- %0.2f)" % (scores.mean(), scores.std() * 2))
    #show feature importances
    feature_importances_str = ''.join(["%-33s: %-5.4f\n" % (feature, importance) for feature, importance in 
        zip(x.columns.values, model.feature_importances_)])
    print(feature_importances_str)
    
    #output results to file 
    f.write("\n---\n")
    f.write(m_refactoring + "\n")
    f.write("Instances: %d\n" % refactorings.shape[0])
    f.write("Accuracy: %0.2f (+/- %0.2f)\n" % (scores.mean(), scores.std() * 2))
    f.write("\nFeature Importances\n")
    f.write(feature_importances_str)
    f.write("\n---\n")

    return model


