# global configs

# number of iterations (hyperparameter tuning)
# number of cross-validations
# SVM has its own, given its slowliness
N_ITER = 10
N_ITER_SVM = 5

N_CV = 10
N_CV_SVM = 5

# how to balance the dataset
# options = [random, cluster_centroids, nearmiss]
BALANCE_DATASET = "random"

# models and datasets we have available
# MODELS options = ['svm', 'svm-non-linear', 'decision-tree', 'random-forest','logistic-regression', 'deep-learning', 'naive-bayes']
MODELS = ['svm', 'svm-non-linear', 'decision-tree', 'random-forest','logistic-regression', 'deep-learning', 'naive-bayes']
DATASETS = ['', 'apache', 'github', 'fdroid']
