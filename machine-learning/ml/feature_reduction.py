from sklearn.feature_selection import RFECV
from sklearn.svm import SVR

from configs import N_CV_FEATURE_REDUCTION
from utils.log import log


def perform_feature_reduction(x, y):
    estimator = SVR(kernel="linear")
    selector = RFECV(estimator, step=1, cv=N_CV_FEATURE_REDUCTION)

    log("Features before reduction (total of {}): {}".format(len(x.columns.values), ', '.join(x.columns.values)))
    x = selector.fit_transform(x, y)
    log("Features after reduction (total of {}): {}".format(len(x.columns.values), ', '.join(x.columns.values)))

    return x
