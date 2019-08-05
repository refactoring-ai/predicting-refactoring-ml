import traceback

import pandas as pd
from sklearn import metrics
from sklearn.preprocessing import MinMaxScaler

import db
from configs import DATASETS, MODELS
from ml_utils import perform_under_sampling, load_model, load_scaler


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

                # drop process and ownership metrics, if not class level
                if not refactoring_level == 'class-level':
                    x = x.drop(["authorOwnership","bugFixCount","linesAdded","linesDeleted","qtyMajorAuthors",
                                "qtyMinorAuthors","qtyOfAuthors","qtyOfCommits","refactoringsInvolved"], axis=1)

                # balance the datasets
                balanced_x, balanced_y = perform_under_sampling(x, y)

                for model_name in MODELS:
                    try:
                        print("Refactoring %s, model %s, dataset 1 %s, dataset 2 %s" % (refactoring_name, model_name, d1, d2))

                        # scale it (as in the training of the model)
                        # this time, uses existing scaler
                        scaler = load_scaler("models/", model_name, d1, refactoring_name)
                        balanced_x_2 = scaler.transform(balanced_x)


                        model_under_eval = load_model("models/", model_name, d1, refactoring_name)

                        if model_name == 'deep-learning':
                            y_predicted = model_under_eval.predict_classes(balanced_x_2)
                        else:
                            y_predicted = model_under_eval.predict(balanced_x_2)

                        results = metrics.classification_report(balanced_y, y_predicted,output_dict=True)

                        print(results)
                        f.write(d1 + "," + d2 + "," + refactoring_name + "," + model_name + "," + str(results["macro avg"]["precision"]) + "," + str(results["macro avg"]["recall"]))
                        f.write("\n")
                        f.flush()
                    except Exception as e:
                        print("An error occurred while working on refactoring " + refactoring_name + " model " + model_name)
                        print(e)
                        print(traceback.format_exc())


file_name = "results/cross-validation.csv"
f = open(file_name, "w+")
f.write("dataset_loaded_model,dataset_test,refactoring,model,precision,recall\n")

print("[COMPARING METHOD-LEVEL MODELS]")
check_model_performance(f, "method-level",
            db.get_method_level_refactorings_count,
            db.get_method_level_refactorings,
            db.get_non_refactored_methods)

check_model_performance(f, "class-level",
            db.get_class_level_refactorings_count,
            db.get_class_level_refactorings,
            db.get_non_refactored_classes)

check_model_performance(f, "variable-level",
            db.get_variable_level_refactorings_count,
            db.get_variable_level_refactorings,
            db.get_non_refactored_variables)
