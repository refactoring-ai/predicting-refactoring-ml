from sklearn.feature_selection import RFECV
from sklearn.svm import SVR

from configs import N_CV_FEATURE_REDUCTION
from utils.log import log


def perform_feature_reduction(x, y):
    """
    Performs feature reduction in the x, y

    For now, it uses linear SVR as estimator, and removes feature by feature.

    :param x: feature values
    :param y: labels
    :return: x, y, where x only contain the relevant features.
    """

    estimator = SVR(kernel="linear")
    selector = RFECV(estimator, step=1, cv=N_CV_FEATURE_REDUCTION)

    temp_x = x.drop(['id'], axis=1)
    log("Features before reduction (total of {}): {}".format(len(temp_x.columns.values), ', '.join(temp_x.columns.values)))
    selector.fit(temp_x, y)
    temp_x = temp_x[temp_x.columns[selector.get_support(indices=True)]] # keeping the column names

    log("Features after reduction (total of {}): {}".format(len(temp_x.columns.values), ', '.join(temp_x.columns.values)))
    log("Feature ranking: {}".format(', '.join(str(e) for e in selector.ranking_)))
    log("Feature grid scores: {}".format(', '.join(str(e) for e in selector.grid_scores_)))

    x = temp_x.assign(id=x['id'])
    return x
