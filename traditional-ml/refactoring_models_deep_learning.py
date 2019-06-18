from keras.layers import Dense, Activation, Dropout
from keras.models import Sequential
from keras.regularizers import l2
from keras.callbacks import EarlyStopping
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, precision_score, recall_score
import keras_metrics


def run_deep_learning(x, columns, y, f, refactoring_name):

    x_train, x_test, y_train, y_test = train_test_split (x, y, test_size=0.2,
                                                         random_state=42)

    #network architecture
    model = Sequential()
    
    model.add(Dense(128, input_dim=x.shape[1], activation='relu',
                    kernel_regularizer=l2(0.01)))
    model.add(Dropout(0.2))

    model.add(Dense(64, activation='relu',
                    kernel_regularizer=l2(0.01)))

    model.add(Dropout(0.2))
    
    # for binary classifiers
    model.add(Dense(1, activation='sigmoid'))
    
    model.compile(optimizer='adam', loss='binary_crossentropy',
                  metrics=['accuracy',
                           keras_metrics.precision(),
                           keras_metrics.recall()])

    # for multiclass classifier
    # model.add(Dense(y.shape[1]))
    # model.add(Activation('softmax'))
    # model.compile(optimizer='rmsprop', loss='categorical_crossentropy', metrics=['accuracy'])


    model.summary()

    # early stop
    early_stop = EarlyStopping(monitor='val_loss', min_delta=0.001, mode='auto',
                               verbose=1, patience=50)

    # fit model
    model.fit(x_train, y_train, validation_data=(x_test, y_test), epochs=1000,
              batch_size=128, callbacks=[early_stop])

    # evaluate
    y_pred = model.predict_classes(x_test)

    accuracy = accuracy_score(y_test, y_pred)
    print("Accuracy: %0.2f" % (accuracy))
    f.write("Accuracy: %0.2f\n" % (accuracy))
    
    precision = precision_score(y_test, y_pred)
    print("Precision: %0.2f" % (precision))
    f.write("Precision: %0.2f\n" % (precision))
    
    recall = recall_score(y_test, y_pred)
    print("Recall: %0.2f" % (recall))
    f.write("Recall: %0.2f\n" % (recall))

    return model