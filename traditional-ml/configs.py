# global configs

# number of iterations (hyperparameter tuning)
# number of cross-validations
# SVM has its own, given its slowliness
N_ITER = 100
N_ITER_SVM = 10

N_CV = 10
N_CV_SVM = 5

# how to balance the dataset
# options = [random, cluster_centroids, nearmiss]
BALANCE_DATASET = "random"

# models and datasets we have available
MODELS = ['svm', 'decision-tree', 'random-forest','logistic-regression', 'deep-learning', 'naive-bayes']
DATASETS = ['', 'apache', 'github', 'fdroid']
