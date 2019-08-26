import pandas as pd
from sklearn.preprocessing import MinMaxScaler


def perform_scaling(x):
    """
    Scales all the values between [0,1]. It often speeds up the learning process.

    :param x: the feature values
    :return: x, scaled
    """

    scaler = MinMaxScaler()  # Default behavior is to scale to [0,1]
    columns = x.columns
    x = scaler.fit_transform(x)
    x = pd.DataFrame(x, columns=columns) # keeping the column names

    return x
