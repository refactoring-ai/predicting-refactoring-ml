# global configs

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

# models and datasets we have available
# MODELS options = ['svm', 'svm-non-linear', 'decision-tree', 'random-forest','logistic-regression', 'deep-learning', 'naive-bayes']
MODELS = ['svm', 'svm-non-linear', 'decision-tree', 'random-forest','logistic-regression', 'deep-learning', 'naive-bayes']

# Empty dataset means 'all datasets'
DATASETS = ['', 'apache', 'github', 'fdroid']


# exclude refactoring
# a list of refactorings to exclude from the analysis
# useful when running the tool in batches
REFACTORING_TO_EXCLUDE = []