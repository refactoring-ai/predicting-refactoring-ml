import db
import os
import joblib
import pandas as pd

from keras.models import Sequential
from keras.layers import Dense, Activation

from sklearn.preprocessing import MinMaxScaler
from ml_utils import perform_under_sampling, create_persistence_file_name

def run_deep_learning(m_refactoring, refactorings, non_refactored_methods, f):
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

    #create (or load) the linear SVM model
    print("Starting Deep Learning training for %s" % m_refactoring)
    persistence_file_name_without_extension = create_persistence_file_name(f, m_refactoring)
    if(os.path.isfile(persistence_file_name_without_extension  + '.joblib')):
        print("Loading preexisting model for %s" % m_refactoring)
        model = joblib.load(persistence_file_name_without_extension + '.joblib')
    else:
        print("Building model for %s" % m_refactoring)
        
        #network architecture
        model = Sequential()
        model.add(Dense(64, input_dim=balanced_x.shape[1]))
        model.add(Dropout(0.2))
        model.add(Dense(64))
        model.add(Dropout(0.2))
        model.add(Activation('relu'))
        model.add(Dense(balanced_y.shape[1]))
        model.add(Activation('softmax'))
        model.compile(optimizer='rmsprop', loss='categorical_crossentropy', metrics=['accuracy'])
        
        #fit model
        model.fit(balanced_x, balanced_y, epochs=100, batch_size=64)
        #save model to file
        joblib.dump(model, persistence_file_name_without_extension + '.joblib') 

    # validation
    scores = model.evaluate(balanced_x, balanced_y, batch_size=128)

    print("Accuracy: %0.2f" % (scores[1]))


    f.write("\n---\n")
    f.write(m_refactoring + "\n")
    f.write("instances: %d\n" % refactorings.shape[0])
    f.write("Accuracy: %0.2f\n" % (scores[1]))
    f.write("\n---\n")

    return model