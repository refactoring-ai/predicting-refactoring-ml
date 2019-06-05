from keras.layers import Dense, Activation
from keras.models import Sequential


def run_deep_learning(x, columns, y, f):

    #network architecture
    model = Sequential()
    model.add(Dense(64, input_dim=x.shape[1]))
    model.add(Dropout(0.2))
    model.add(Dense(64))
    model.add(Dropout(0.2))
    model.add(Activation('relu'))
    model.add(Dense(y.shape[1]))
    model.add(Activation('softmax'))
    model.compile(optimizer='rmsprop', loss='categorical_crossentropy', metrics=['accuracy'])

    #fit model
    model.fit(x, y, epochs=100, batch_size=64)

    # validation
    scores = model.evaluate(x, y, batch_size=128)

    print("Accuracy: %0.2f" % (scores[1]))
    f.write("Accuracy: %0.2f\n" % (scores[1]))

    return model