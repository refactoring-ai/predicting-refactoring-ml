import keras_metrics
import numpy
from keras import Sequential
from keras.callbacks import EarlyStopping
from keras.layers import Dense, Dropout
from keras.regularizers import l2
from sklearn import metrics
from sklearn.metrics import accuracy_score, precision_score, recall_score
from sklearn.model_selection import StratifiedKFold

from configs import N_CV_DNN
from ml.models.base import DeepMLRefactoringModel


def build_model_architecture(x_shape):
    model = Sequential()
    model.add(Dense(128, input_dim=x_shape, activation='relu',
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
    model.summary()

    # early stop
    early_stop = EarlyStopping(monitor='val_loss', min_delta=0.01, mode='auto',
                               verbose=1, patience=5)

    return early_stop, model


class NeuralNetworkDeepRefactoringModel(DeepMLRefactoringModel):
    def run(self, x, y):
        seed = 42
        numpy.random.seed(seed)

        kfold = StratifiedKFold(n_splits=N_CV_DNN, shuffle=True, random_state=seed)
        accuracy_scores = []
        precision_scores = []
        recall_scores = []
        tn_scores = []
        fp_scores = []
        fn_scores = []
        tp_scores = []

        for train, test in kfold.split(x, y):

            x_train, x_test = x.iloc[train, :], x.iloc[test, :]
            y_train, y_test = y[train], y[test]

            # fit model
            early_stop, model = build_model_architecture(x.shape[1])
            model.fit(x_train, y_train, epochs=1000, batch_size=128, callbacks=[early_stop])

            # evaluate
            y_pred = model.predict_classes(x_test)

            accuracy = accuracy_score(y_test, y_pred)
            accuracy_scores.append(accuracy)

            precision = precision_score(y_test, y_pred)
            precision_scores.append(precision)

            recall = recall_score(y_test, y_pred)
            recall_scores.append(recall)

            tn, fp, fn, tp = metrics.confusion_matrix(y_test, y_pred).ravel()
            tn_scores.append(tn)
            fp_scores.append(fp)
            fn_scores.append(fn)
            tp_scores.append(tp)

        # now, train the model with all the data
        early_stop, super_model = build_model_architecture(x.shape[1])
        super_model.fit(x, y, epochs=1000, batch_size=128, callbacks=[early_stop])

        return precision_scores, recall_scores, accuracy_scores, \
               tn_scores, fp_scores, fn_scores, tp_scores, super_model
