# more info: https://imbalanced-learn.readthedocs.io/en/stable/under_sampling.html
from imblearn.under_sampling import RandomUnderSampler, ClusterCentroids, NearMiss

from configs import BALANCE_DATASET


def perform_under_sampling(x, y):

    if BALANCE_DATASET == 'random':
        rus = RandomUnderSampler(random_state=42)  # 42 is a random number, just to ensure our results are reproducible
        return rus.fit_resample(x, y)

    if BALANCE_DATASET == 'cluster_centroids':
        rus = ClusterCentroids(random_state=42)
        return rus.fit_resample(x, y)

    if BALANCE_DATASET == 'nearmiss':
        rus = NearMiss(version=1)
        return rus.fit_resample(x, y)

    raise Exception("algorithm not found")
