# --------------------------------
# Testing
# --------------------------------
# is it a test run?
# test runs reduce the dataset to 100 instances only
from enum import IntEnum

TEST = False

# --------------------------------
# FileTypes
# --------------------------------
# Do we only look at production or test files or both?
# 0 = only_production, 1 = only_test, 2 = production_and_test
FILE_TYPE = 2

# --------------------------------
# Database related
# --------------------------------
# do we use the cached results? True=yes, False=no, go always to the db
USE_CACHE = True

# is the db available? sometimes it's not, but you have all the cache
DB_AVAILABLE = True

# --------------------------------
# Dataset balancing
# --------------------------------
BALANCE_DATASET = True

# how to balance the dataset
# options = [random, cluster_centroids, nearmiss]
BALANCE_DATASET_STRATEGY = "random"

# --------------------------------
# Dataset scaling
# --------------------------------

# scale using MinMaxScaler?
SCALE_DATASET = True

# --------------------------------
# Feature reduction
# --------------------------------

# use (or drop) process and authorship metrics
DROP_PROCESS_AND_AUTHORSHIP_METRICS = True
#
PROCESS_AND_AUTHORSHIP_METRICS = ["authorOwnership", "bugFixCount", "qtyMajorAuthors", "qtyMinorAuthors", "qtyOfAuthors", "qtyOfCommits", "refactoringsInvolved"]

# Drop these metrics as well
DROP_METRICS = []

# perform feature reduction?
FEATURE_REDUCTION = True

# number of folds for feature reduction
N_CV_FEATURE_REDUCTION = 5

# --------------------------------
# Hyperparameter search
# --------------------------------

# what type of search for the best hyper params?
# options = [randomized, grid]
SEARCH = "grid"

# number of iterations (if Randomized strategy is chosen)
N_ITER_RANDOM_SEARCH = 100

# number of folds in the search for best parameters
N_CV_SEARCH = 5

# --------------------------------
# Evaluation: Cross-validation configuration
# --------------------------------

# Specify either a train/ test split, e.g. 0.2 -> 80/ 20 split
TEST_SPLIT_SIZE = -1
# Or specify test data sets in the database
# NOTE: set TEST_SPLIT_SIZE value to < 0, in order to indicate to use the given datasets instead of a random train/ test split
VALIDATION_DATASETS = ["test set github", "validation set github"]

# number of folds for the final evaluation
N_CV = 10

# number of folds for the DNN
N_CV_DNN = 10

# --------------------------------
# Models and datasets
# --------------------------------

# models and datasets we have available
MODELS = ['svm', 'svm-non-linear', 'decision-tree', 'random-forest', 'logistic-regression', 'naive-bayes',
          'extra-trees']

# Empty dataset means 'all datasets'
DATASETS = ["github"]


# --------------------------------
# Refactorings
# --------------------------------

# refactoring levels
class Level(IntEnum):
    NONE = 0
    Class = 1
    Method = 2
    Variable = 3
    Field = 4
    Other = 5


# Refactorings to study
CLASS_LEVEL_REFACTORINGS = ["Extract Class",
                            "Extract Interface",
                            "Extract Subclass",
                            "Extract Superclass",
                            "Move And Rename Class",
                            "Move Class",
                            "Rename Class",
                            "Introduce Polymorphism",
                            "Move And Rename Class",
                            "Convert Anonymous Class To Type"]

METHOD_LEVEL_REFACTORINGS = ["Extract And Move Method",
                             "Extract Method",
                             "Inline Method",
                             "Move Method",
                             "Pull Up Method",
                             "Push Down Method",
                             "Rename Method",
                             "Extract And Move Method",
                             "Change Return Type",
                             "Move And Inline Method",
                             "Move And Rename Method",
                             "Change Parameter Type",
                             "Split Parameter",
                             "Merge Parameter"]

VARIABLE_LEVEL_REFACTORINGS = ["Extract Variable",
                               "Inline Variable",
                               "Parameterize Variable",
                               "Rename Parameter",
                               "Rename Variable",
                               "Replace Variable With Attribute",
                               "Change Variable Type",
                               "Split Variable",
                               "Merge Variable"]

FIELD_LEVEL_REFACTORINGS = ["Move Attribute",
                            "Pull Up Attribute",
                            "Move And Rename Attribute",
                            "Push Down Attribute",
                            "Replace Attribute",
                            "Rename Attribute",
                            "Extract Attribute",
                            "Change Attribute Type"]

OTHER_LEVEL_REFACTORINGS = ["Move Source Folder",
                            "Change Package"]

levelMap = {Level.NONE: [],
            Level.Class: CLASS_LEVEL_REFACTORINGS,
            Level.Method: METHOD_LEVEL_REFACTORINGS,
            Level.Field: FIELD_LEVEL_REFACTORINGS,
            Level.Variable: VARIABLE_LEVEL_REFACTORINGS,
            Level.Other: OTHER_LEVEL_REFACTORINGS}
# --------------------------------
# DO NOT CHANGE FROM HERE ON
# --------------------------------
if DROP_PROCESS_AND_AUTHORSHIP_METRICS:
    DROP_METRICS += PROCESS_AND_AUTHORSHIP_METRICS


# Let's change some parameters (i.e., make them smaller) if this is a test run
if TEST:
    N_ITER = 1
    N_CV = 2

    N_ITER_SVM = 1
    N_CV_SVM = 2

    N_CV_DNN = 2

    CV_FEATURE_REDUCTION = 2
