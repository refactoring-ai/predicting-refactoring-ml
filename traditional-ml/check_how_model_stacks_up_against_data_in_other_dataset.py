from joblib import load
#from ml_utils import load_model #SEE BELOW
#from main import datasets, models

models = ['svm', 'decision-tree', 'random-forest','logistic-regression', 'svm-non-linear']
datasets = ['', 'apache', 'github', 'fdroid']

def check_model_performance(dataset, model, model_name, refactoring_name):
    if is_not_blank(dataset) and dataset in datasets: 
        #VALID DATASET, READY TO GO!
        #LOAD MODEL BUILT FROM SPECIFIC DATASET
        model_under_eval = load_model(model, model_name, dataset, refactoring_name)

#THIS METHOD SHOULD PROBABLY BE MOVED TO ml_utils
def load_model(model, model_name, dataset, refactoring_name):
    file_name = "results/model-" + model_name + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".joblib"
    return load(file_name)

def is_not_blank(s):
    return bool(s and s.strip())

check_model_performance('apache', 'model', 'model_name', 'class-level')

