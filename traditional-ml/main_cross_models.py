import pandas as pd
from sklearn.metrics import precision_recall_fscore_support
from sklearn.preprocessing import MinMaxScaler

import db
from configs import DATASETS, MODELS
from ml_utils import perform_under_sampling, load_model


def check_model_performance(f, refactoring_level, counts_function, get_refactored_function, get_non_refactored_function):

    print("Starting cross model analysis at " + refactoring_level)

    counts = counts_function("")

    for d1 in DATASETS: # d1 being the model we load
        for d2 in DATASETS: # d2 being the dataset we'll try to predict
            if d1 == d2 or d1 == '' or d2 == '':
                continue

            for refactoring_name in counts["refactoring"].values:
                refactored_instances = get_refactored_function(refactoring_name, d2)
                non_refactored_instances = get_non_refactored_function(d2)

                # if there' still a row with NAs, drop it as it'll cause a failure later on.
                refactored_instances = refactored_instances.dropna()
                non_refactored_instances = non_refactored_instances.dropna()

                # set the prediction variable as true and false in the datasets
                refactored_instances["prediction"] = 1
                non_refactored_instances["prediction"] = 0
                merged_dataset = pd.concat([refactored_instances, non_refactored_instances])

                # separate the x from the y (as required by the scikit-learn API)
                x = merged_dataset.drop("prediction", axis=1)
                y = merged_dataset["prediction"]

                # balance the datasets
                balanced_x, balanced_y = perform_under_sampling(x, y)

                # scale it (as in the training of the model)
                # Default behavior is to scale to [0,1]
                scaler = MinMaxScaler()
                balanced_x = scaler.fit_transform(balanced_x)

                for model_name in MODELS:
                    print("Refactoring %s, model %s, dataset 1 %s, dataset 2 %s" % (refactoring_name, model_name, d1, d2))
                    model_under_eval = load_model("models/", model_name, d1, refactoring_name)
                    y_score = model_under_eval.decision_function(balanced_x)

                    results = precision_recall_fscore_support(balanced_y, y_score)

                    f.write(d1 + "," + d2 + "," + refactoring_name + "," + model_name + "," + str(results["precision"]) + "," + str(results["recall"]))
                    f.write("\n")
                    f.flush()


file_name = "results/cross-validation.csv"
f = open(file_name, "w+")
f.write("dataset_loaded_model,dataset_test,refactoring,model,precision,recall\n")

print("[COMPARING METHOD-LEVEL MODELS]")
check_model_performance(f, "method-level",
            db.get_method_level_refactorings_count,
            db.get_method_level_refactorings,
            db.get_non_refactored_methods)

