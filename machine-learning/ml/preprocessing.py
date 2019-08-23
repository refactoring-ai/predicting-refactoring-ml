import pandas as pd
import sklearn
from sklearn.preprocessing import MinMaxScaler

from configs import SCALE_DATASET, TEST
from ml.sampling import perform_under_sampling
from refactoring import LowLevelRefactoring
from utils.log import log


def retrieve_labelled_instances(dataset, refactoring: LowLevelRefactoring):

    # get all refactoring examples we have in our dataset
    refactored_instances = refactoring.get_refactored_instances(dataset)

    # load non-refactoring examples
    non_refactored_instances = refactoring.get_non_refactored_instances(dataset)

    log("refactoring instances: {}".format(refactored_instances.shape[0]))
    log("not refactoring instances: {}".format(non_refactored_instances.shape[0]))

    # if there' still a row with NAs, drop it as it'll cause a failure later on.
    refactored_instances = refactored_instances.dropna()
    non_refactored_instances = non_refactored_instances.dropna()

    log("refactoring instance(after dropping NA)s: {}".format(refactored_instances.shape[0]))
    log("not refactoring instances: {}".format(non_refactored_instances.shape[0]))

    assert refactored_instances.shape[0] > 0, "No refactorings found"

    # set the prediction variable as true and false in the datasets
    refactored_instances["prediction"] = 1
    non_refactored_instances["prediction"] = 0

    # if it's a test run, we reduce the sample randomly
    if TEST:
        refactored_instances = refactored_instances.sample(frac=0.01)
        non_refactored_instances = non_refactored_instances.sample(frac=0.01)

    # now, combine both datasets (with both TRUE and FALSE predictions)
    assert non_refactored_instances.shape[1] == refactored_instances.shape[
        1], "number of columns differ from both datasets"
    merged_dataset = pd.concat([refactored_instances, non_refactored_instances])

    # shuffle the array
    merged_dataset = sklearn.utils.shuffle(merged_dataset)

    # separate the x from the y (as required by the scikit-learn API)
    x = merged_dataset.drop("prediction", axis=1)
    y = merged_dataset["prediction"]

    # class level refactoring is the only one with process and ownership metrics
    # TODO: best way would be to remove these fields from the queries
    if not refactoring.refactoring_level() == 'class':
        x = x.drop(["authorOwnership", "bugFixCount", "linesAdded", "linesDeleted", "qtyMajorAuthors",
                    "qtyMinorAuthors", "qtyOfAuthors", "qtyOfCommits", "refactoringsInvolved"], axis=1)

    # balance the datasets, as we have way more 'non refactored examples' rather than refactoring examples
    # for now, we basically perform under sampling
    balanced_x, balanced_y = perform_under_sampling(x, y)
    assert balanced_x.shape[0] == balanced_y.shape[0], "Undersampling did not work"

    # apply some scaling to speed up the algorithm
    scaler = None
    if SCALE_DATASET:
        scaler = MinMaxScaler()  # Default behavior is to scale to [0,1]
        balanced_x = scaler.fit_transform(balanced_x)

    return x.columns.values, balanced_x, balanced_y, scaler