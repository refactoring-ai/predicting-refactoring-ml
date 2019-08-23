import random

from configs import TEST, USE_CACHE, BALANCE_DATASET, BALANCE_DATASET_STRATEGY, SCALE_DATASET, FEATURE_REDUCTION, \
    N_CV_FEATURE_REDUCTION, SEARCH, N_CV_SEARCH, N_ITER_RANDOM_SEARCH, N_CV, DATASETS, MODELS, DEEP_MODELS, \
    CLASS_LEVEL_REFACTORINGS, METHOD_LEVEL_REFACTORINGS, VARIABLE_LEVEL_REFACTORINGS, DB_AVAILABLE

_f = None


def print_config():
    global _f
    log(f"Test: {TEST}")
    log(f"Use cache? {USE_CACHE} DB available? {DB_AVAILABLE}")
    log(f"Balance dataset? {BALANCE_DATASET} {BALANCE_DATASET_STRATEGY}")
    log(f"Scale dataset? {SCALE_DATASET}")
    log(f"Feature reduction? {FEATURE_REDUCTION} {N_CV_FEATURE_REDUCTION}")
    log(f"CV for Hyper parameter search: {SEARCH} {N_CV_SEARCH} {N_ITER_RANDOM_SEARCH}")
    log(f"CV for evaluation: {N_CV}")
    log(f"Datasets: {DATASETS}")
    log(f"Models: {MODELS}")
    log(f"Deep Learning Models: {DEEP_MODELS}")
    log(f"Class-level refactorings: {CLASS_LEVEL_REFACTORINGS}")
    log(f"Method-level refactorings: {METHOD_LEVEL_REFACTORINGS}")
    log(f"Variable-level refactorings: {VARIABLE_LEVEL_REFACTORINGS}")


def log_init():
    global _f
    _f = open("results/{}-result.txt".format(random.randint(1, 999999)), "w+")
    print_config()


def log_close():
    global _f
    _f.close()


def log(msg):
    global _f
    _f.write(msg)
    _f.write("\n")
    _f.flush()
