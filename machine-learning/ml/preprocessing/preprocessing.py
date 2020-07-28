from collections import Counter

import pandas as pd

from configs import SCALE_DATASET, TEST, FEATURE_REDUCTION, BALANCE_DATASET, DROP_PROCESS_AND_AUTHORSHIP_METRICS
from ml.preprocessing.feature_reduction import perform_feature_reduction
from ml.preprocessing.sampling import perform_balancing
from ml.preprocessing.scaling import perform_scaling
from refactoring import LowLevelRefactoring
from utils.log import log


def retrieve_labelled_instances(dataset, refactoring: LowLevelRefactoring, is_training_data: bool):
    # get all refactoring examples we have in our dataset
    refactored_instances = refactoring.get_refactored_instances(dataset)
    # load non-refactoring examples
    non_refactored_instances = refactoring.get_non_refactored_instances(dataset)

    log("raw number of refactoring instances: {}".format(refactored_instances.shape[0]))
    log("raw number of non-refactoring instances: {}".format(non_refactored_instances.shape[0]))

    # if there' still a row with NAs, drop it as it'll cause a failure later on.
    refactored_instances = refactored_instances.dropna()
    non_refactored_instances = non_refactored_instances.dropna()

    # test if any refactorings were found for the given refactoring type
    if refactored_instances.shape[0] == 0:
        log("No refactorings found for refactoring type: " + refactoring.name())
        return None, None, None, None

    # test if any refactorings were found for the given refactoring type
    if non_refactored_instances.shape[0] == 0:
        log("No non-refactorings found for refactoring type: " + refactoring.name())
        return None, None, None, None

    log("refactoring instances (after dropping NA)s: {}".format(refactored_instances.shape[0]))
    log("non-refactoring instances (after dropping NA)s: {}".format(non_refactored_instances.shape[0]))

    assert non_refactored_instances.shape[0] > 0, "Found no non-refactoring instances for level: " + refactoring.refactoring_level()

    # set the prediction variable as true and false in the datasets
    refactored_instances["prediction"] = 1
    non_refactored_instances["prediction"] = 0

    # if it's a test run, we reduce the sample randomly
    if TEST:
        refactored_instances = refactored_instances.sample(frac=0.1)
        non_refactored_instances = non_refactored_instances.sample(frac=0.1)

    # now, combine both datasets (with both TRUE and FALSE predictions)
    if non_refactored_instances.shape[1] != refactored_instances.shape[1]:
        raise ImportError("Number of columns differ from both datasets.")
    merged_dataset = pd.concat([refactored_instances, non_refactored_instances])

    # just to be sure, shuffle the dataset
    merged_dataset = merged_dataset.sample(frac=1, random_state = 42)

    # do we want to try the models without process and authorship metrics?
    if DROP_PROCESS_AND_AUTHORSHIP_METRICS:
        merged_dataset = merged_dataset.drop(["authorOwnership", "bugFixCount", "qtyMajorAuthors",
                    "qtyMinorAuthors", "qtyOfAuthors", "qtyOfCommits", "refactoringsInvolved"], axis=1)

    # separate the x from the y (as required by the scikit-learn API)
    x = merged_dataset.drop("prediction", axis=1)
    y = merged_dataset["prediction"]

    # balance the datasets, as we have way more 'non refactored examples' rather than refactoring examples
    # for now, we basically perform under sampling
    if BALANCE_DATASET:
        log("instances before balancing: {}".format(Counter(y)))
        x, y = perform_balancing(x, y)
        assert x.shape[0] == y.shape[0], "Balancing did not work, x and y have different shapes."
        log("instances after balancing: {}".format(Counter(y)))

    # apply some scaling to speed up the algorithm
    scaler = None
    if is_training_data and SCALE_DATASET:
        x, scaler = perform_scaling(x)

    # let's reduce the number of features in the set
    if is_training_data and FEATURE_REDUCTION:
        x = perform_feature_reduction(x, y)

    return x.columns.values, x, y, scaler

