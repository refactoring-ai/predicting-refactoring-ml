# --------------------------------
# Testing
# --------------------------------
# is it a test run?
# test runs reduce the dataset to 100 instances only
TEST = False


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

# decide whether to run the test as a ordered set of events
# if so, then, we don't do k-fold validation, but split it in train/test
ORDERED_DATA = False
ORDERED_DATA_TEST_SPLIT = 0.1

# --------------------------------
# Dataset scaling
# --------------------------------

# scale using MinMaxScaler?
SCALE_DATASET = True


# --------------------------------
# Feature reduction
# --------------------------------

# use (or drop) process and authorship metrics
USE_PROCESS_AND_AUTHORSHIP_METRICS = True

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

# number of folds for the final evaluation
N_CV = 10

# number of folds for the DNN
N_CV_DNN = 10


# --------------------------------
# Models and datasets
# --------------------------------

# models and datasets we have available
MODELS = ['svm', 'svm-non-linear', 'decision-tree', 'random-forest','logistic-regression', 'naive-bayes','extra-trees']
DEEP_MODELS = ['neural-network']

# Empty dataset means 'all datasets'
# options = ['', 'apache', 'github', 'fdroid']
DATASETS = ['', 'apache', 'github', 'fdroid']

# --------------------------------
# Refactorings
# --------------------------------

# TODO: extend with new refactorings
# Refactorings to study
CLASS_LEVEL_REFACTORINGS = ["Extract Class",
"Extract Interface",
"Extract Subclass",
"Extract Superclass",
"Move And Rename Class",
"Move Class",
"Rename Class"]

METHOD_LEVEL_REFACTORINGS = ["Extract And Move Method",
"Extract Method",
"Inline Method",
"Move Method",
"Pull Up Method",
"Push Down Method",
"Rename Method"]

VARIABLE_LEVEL_REFACTORINGS = ["Extract Variable",
"Inline Variable",
"Parameterize Variable",
"Rename Parameter",
"Rename Variable",
"Replace Variable With Attribute"]


# --------------------------------
# DO NOT CHANGE FROM HERE ON
# --------------------------------

# Let's change some parameters (i.e., make them smaller) if this is a test run
if TEST:
    N_ITER = 1
    N_CV = 2

    N_ITER_SVM = 1
    N_CV_SVM = 2

    N_CV_DNN = 2

    CV_FEATURE_REDUCTION = 2