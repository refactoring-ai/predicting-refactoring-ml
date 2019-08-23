import pandas as pd
from sklearn.preprocessing import MinMaxScaler


def perform_scaling(x):
    scaler = MinMaxScaler()  # Default behavior is to scale to [0,1]
    columns = x.columns
    x = scaler.fit_transform(x)
    x = pd.DataFrame(x, columns=columns) # keeping the column names

    return x