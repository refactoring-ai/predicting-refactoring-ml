import db
import pandas as pd
from ml_utils import perform_under_sampling

from sklearn.preprocessing import MinMaxScaler
from sklearn.model_selection import cross_val_score
from sklearn.ensemble import RandomForestClassifier

def run_random_forest(m_refactoring, refactorings, non_refactored_methods, f):
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
    assert balanced_x.shape[0] == balanced_y.shape[0], "Undersampling did not work while building the random forest model"

    #perform some scaling to speed up the whole model-building thingy
    scaler = MinMaxScaler()  #default behavior scales data to the following range: (0,1)
    balanced_x = scaler.fit_transform(balanced_x)

    #create random forest classifier object
    print("Starting to build the random forest model for %s" % m_refactoring)
    #use all available cores by setting n_jobs=-1.
    model = RandomForestClassifier(random_state=42, n_jobs=-1)
    #train model
    model.fit(balanced_x, balanced_y)
    
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

