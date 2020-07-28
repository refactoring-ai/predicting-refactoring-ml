import pandas as pd
from sklearn.preprocessing import MinMaxScaler


def perform_scaling(x):
    """
    Scales all the values between [0,1]. It often speeds up the learning process.

    :param x: the feature values
    :return: x, scaled
    """

    scaler = MinMaxScaler()  # Default behavior is to scale to [0,1]
    new_x = scaler.fit_transform(x)
    x = pd.DataFrame(new_x, index=x.index, columns=x.columns) # keeping the same indices and column names

    return x, scaler
