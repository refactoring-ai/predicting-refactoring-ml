# global configs

# is it a test run?
# test runs reduce the dataset to 100 instances only
TEST = False

# do we use the cached results? True=yes, False=no, go always to the db
USE_CACHE = True

# is the db available? sometimes it's not, but you have all the cache
DB_AVALIABLE = True

# number of iterations (hyperparameter tuning)
# number of cross-validations
# SVM has its own, given its slowliness
N_ITER = 100
N_CV = 10

N_ITER_SVM = 10
N_CV_SVM = 5

N_CV_DNN = 5

# how to balance the dataset
# options = [random, cluster_centroids, nearmiss]
BALANCE_DATASET = "random"

# scale?
SCALE_DATASET = True

# what type of search for the best hyper params?
# options = [randomized]
SEARCH = "randomized"


# models and datasets we have available
MODELS = ['svm', 'svm-non-linear', 'decision-tree', 'random-forest','logistic-regression', 'naive-bayes','extra-trees']
DEEP_MODELS = ['neural-network']

# Empty dataset means 'all datasets'
# options = ['', 'apache', 'github', 'fdroid']
DATASETS = ['', 'apache', 'github', 'fdroid']

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