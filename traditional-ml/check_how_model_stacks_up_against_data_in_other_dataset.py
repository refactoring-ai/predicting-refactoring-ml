from joblib import load
from configs import N_CV
#from ml_utils import load_model #SEE BELOW
#from main import datasets, models

models = ['svm', 'decision-tree', 'random-forest','logistic-regression', 'svm-non-linear']
datasets = ['', 'apache', 'github', 'fdroid']

def check_model_performance(dataset, refactoring_level, counts_function, get_refactored_function, get_non_refactored_function):
    if is_not_blank(dataset) and dataset in datasets: 
        #VALID DATASET, READY TO GO!
        #LOAD MODEL BUILT FROM SPECIFIC DATASET
        counts = counts_function(dataset)
        #FOR EACH REFACTORING
        for refactoring_name in counts["refactoring"].values:
                print("Refactoring %s" % refactoring_name)
                #EXTRACT REFACTORINGS FROM ALL OTHER DATASETS BUT THE ONE UNDER EXAMINATION
                for d in [a_dataset for a_dataset in datasets if a_dataset not in ['', 'apache']]:
                    #GET ALL REFACTORING EXAMPLES WE HAVE IN OUR DATASET
                    refactored_instances = get_refactored_function(refactoring_name, d)
                    #LOAD NON-REFACTORING EXAMPLES
                    non_refactored_instances = get_non_refactored_function(d)

                    #IF THERE' STILL A ROW WITH NAS, DROP IT AS IT'LL CAUSE A FAILURE LATER ON.
                    refactored_instances = refactored_instances.dropna()
                    non_refactored_instances = non_refactored_instances.dropna()

                    #SET THE PREDICTION VARIABLE AS TRUE AND FALSE IN THE DATASETS
                    refactored_instances["prediction"] = 1
                    non_refactored_instances["prediction"] = 0

                    #NOW, COMBINE BOTH DATASETS (WITH BOTH TRUE AND FALSE PREDICTIONS)
                    merged_dataset = pd.concat([refactored_instances, non_refactored_instances])

                    #SEPARATE THE X FROM THE Y (AS REQUIRED BY THE SCIKIT-LEARN API)
                    x = merged_dataset.drop("prediction", axis=1)
                    y = merged_dataset["prediction"]

                    #BALANCE THE DATASETS, AS WE HAVE WAY MORE 'NON REFACTORED EXAMPLES' RATHER THAN REFACTORING EXAMPLES
                    #FOR NOW, WE BASICALLY PERFORM UNDER SAMPLING
                    balanced_x, balanced_y = perform_under_sampling(x, y)
                    assert balanced_x.shape[0] == balanced_y.shape[0], "Undersampling did not work"

                    #APPLY SOME SCALING TO SPEED UP THE ALGORITHM
                    scaler = MinMaxScaler()  # Default behavior is to scale to [0,1]
                    balanced_x = scaler.fit_transform(balanced_x)

                    for model_name in models:
                        print("Model: %s" % model_name)

                        model = None

                        if model_name == 'svm':
                            model_under_eval = load_model(model, 'svm', dataset, refactoring_name)
                        elif model_name == 'random-forest':
                            model_under_eval = load_model(model, 'random-forest', dataset, refactoring_name)
                        elif model_name == 'decision-tree':
                            model_under_eval = load_model(model, 'decision-tree', dataset, refactoring_name)
                        elif model_name == 'logistic-regression':
                            model_under_eval = load_model(model, 'logistic-regression', dataset, refactoring_name)
                        elif model_name == 'svm-non-linear':
                            model_under_eval = load_model(model, 'svm-non-linear', dataset, refactoring_name)

                        result = cross_validate(model_under_eval, balanced_x, balanced_y, cv=N_CV, n_jobs=-1, 
                                scoring=['precision', 'recall'], verbose=2)

#THIS METHOD SHOULD PROBABLY BE MOVED TO ml_utils
def load_model(model, model_name, dataset, refactoring_name):
    file_name = "results/model-" + model_name + "-" + dataset + "-" + refactoring_name.replace(" ", "") + ".joblib"
    return load(file_name)

def is_not_blank(s):
    return bool(s and s.strip())

check_model_performance('apache', 'model', 'model_name', 'class-level')

