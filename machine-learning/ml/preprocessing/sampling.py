
import pandas as pd
from imblearn.over_sampling import RandomOverSampler
from imblearn.under_sampling import RandomUnderSampler, ClusterCentroids, NearMiss

from configs import BALANCE_DATASET_STRATEGY


def perform_balancing(x, y, strategy=None):
    """
    Performs under/over sampling, according to the number of true and false instances of the x, y dataset.
    :param x: feature values
    :param y: labels
    :return: a balanced x, y
    """

    if strategy is None:
        strategy = BALANCE_DATASET_STRATEGY

    if strategy == 'random':
        # more info: https://imbalanced-learn.readthedocs.io/en/stable/under_sampling.html
        rus = RandomUnderSampler(random_state=42)  # 42 is a random number, just to ensure our results are reproducible
    elif strategy == 'oversampling':
        rus = RandomOverSampler(random_state=42)
    elif strategy == 'cluster_centroids':
        rus = ClusterCentroids(random_state=42)
    elif strategy == 'nearmiss':
        rus = NearMiss(version=1)
    else:
        raise Exception("algorithm not found")

    # keeping column names
    new_x, new_y = rus.fit_resample(x, y)
    new_x = pd.DataFrame(new_x, columns=x.columns)
    return new_x, new_y
