import keras_metrics
import numpy
from keras.callbacks import EarlyStopping
from keras.layers import Dense, Dropout
from keras.models import Sequential
from keras.regularizers import l2
from sklearn.metrics import accuracy_score, precision_score, recall_score
from sklearn.model_selection import StratifiedKFold

from configs import N_CV


# Kfold example from: https://machinelearningmastery.com/evaluate-performance-deep-learning-models-keras/
def run_deep_learning(dataset, refactoring_name, model_name, x, columns, y, f):
    seed = 42
    numpy.random.seed(seed)

    kfold = StratifiedKFold(n_splits=N_CV, shuffle=True, random_state=seed)
    accuracy_scores = []
    precision_scores = []
    recall_scores = []

    best_model = None
    best_model_accuracy = 0

    for train, test in kfold.split(x, y):

        # network architecture
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
        model.fit(x[train], y[train], epochs=1000, batch_size=128, callbacks=[early_stop])

        # evaluate
        y_pred = model.predict_classes(x[test])

        accuracy = accuracy_score(y[test], y_pred)
        accuracy_scores.append(accuracy)

        precision = precision_score(y[test], y_pred)
        precision_scores.append(precision)

        recall = recall_score(y[test], y_pred)
        recall_scores.append(recall)

        # store the most accurate model seen
        first_model = best_model is None
        new_model_is_better = best_model is not None and best_model_accuracy < accuracy
        if first_model or new_model_is_better:
            best_model = model
            best_model_accuracy = accuracy

    # calculating the mean of all recall, precision, and accuracy
    precision_mean = numpy.mean(precision_scores)
    recall_mean = numpy.mean(recall_scores)
    accuracy_mean = numpy.mean(accuracy_scores)

    f.write("Precision: %s\n" % ", ".join(precision_scores))
    f.write("Recall: %s\n" % ", ".join(recall_scores))
    f.write("Accuracy: %s\n" % ", ".join(accuracy_scores))

    f.write(f'CSV,{dataset},{refactoring_name},{model_name},{precision_mean},{recall_mean},{accuracy_mean}\n')

    return best_model
