from keras.layers import Dense, Activation, Dropout
from keras.models import Sequential
from keras.regularizers import l2
from keras.callbacks import EarlyStopping
from sklearn.model_selection import train_test_split


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
    
    model.compile(optimizer='adam', loss='binary_crossentropy', metrics=['accuracy'])

    # for multiclass classifier
    # model.add(Dense(y.shape[1]))
    # model.add(Activation('softmax'))
    # model.compile(optimizer='rmsprop', loss='categorical_crossentropy', metrics=['accuracy'])


    model.summary()

    # early stop
    early_stop = EarlyStopping(monitor='val_loss', min_delta=0.001, mode='auto',
                               verbose=1, patience=50)

    #fit model
    model.fit(x_train, y_train, validation_data=(x_test, y_test), epochs=1000,
              batch_size=128, callbacks=[early_stop])

    # validation
    scores = model.evaluate(x_test, y_test, batch_size=128)

    print("Accuracy: %0.2f" % (scores[1]))
    f.write("Accuracy: %0.2f\n" % (scores[1]))

    return model