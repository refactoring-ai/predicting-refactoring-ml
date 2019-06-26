import sys
import traceback

import pandas as pd
from sklearn.preprocessing import MinMaxScaler

import db
from date_utils import now
from ml_utils import perform_under_sampling, save_model
from refactoring_models_decision_tree import run_decision_tree
from refactoring_models_deep_learning import run_deep_learning
from refactoring_models_logistic_regression import run_logistic_regression
from refactoring_models_random_forest import run_random_forest
from refactoring_models_svm import run_svm

# all the models and datasets we have available
models = ['logistic-regression', 'svm', 'decision-tree', 'random-forest']
datasets = ['', 'apache', 'github', 'fdroid']


def build_model(refactoring_level, counts_function, get_refactored_function, get_non_refactored_function):
    for dataset in datasets:

        # testing in a single refactoring and model
        # if not dataset == '':
        #     continue

        file_name = "results/results-" + refactoring_level + "-" + ("all" if dataset == "" else dataset) + ".txt"
        f = open(file_name, "w+")
        f.write(dataset + "\n\n")

        counts = counts_function(dataset)
        for refactoring_name in counts["refactoring"].values:

            # testing in a single refactoring and model
            # if not refactoring_name == "Extract And Move Method":
            #     continue

            try:
                f.write("\n\n**** %s\n\n" % refactoring_name)
                print("Refactoring %s" % refactoring_name)

                # get all refactoring examples we have in our dataset
                refactored_instances = get_refactored_function(refactoring_name, dataset)

                # load non-refactoring examples
                non_refactored_instances = get_non_refactored_function(dataset)

                f.write("refactoring instances: " + str(refactored_instances.shape[0]) + "\n")
                f.write("not refactoring instances: " + str(non_refactored_instances.shape[0]) + "\n")

                # if there' still a row with NAs, drop it as it'll cause a failure later on.
                refactored_instances = refactored_instances.dropna()
                non_refactored_instances = non_refactored_instances.dropna()

                f.write("refactoring instance(after dropping NA)s: " + str(refactored_instances.shape[
                    0]) + "\n")
                f.write("not refactoring instances: " + str(non_refactored_instances.shape[0]) + "\n\n")

                assert refactored_instances.shape[0] > 0, "No refactorings found"

                # set the prediction variable as true and false in the datasets
                refactored_instances["prediction"] = 1
                non_refactored_instances["prediction"] = 0

                # now, combine both datasets (with both TRUE and FALSE predictions)
                assert non_refactored_instances.shape[1] == refactored_instances.shape[
                    1], "number of columns differ from both datasets"
                merged_dataset = pd.concat([refactored_instances, non_refactored_instances])

                # separate the x from the y (as required by the scikit-learn API)
                x = merged_dataset.drop("prediction", axis=1)
                y = merged_dataset["prediction"]

                # balance the datasets, as we have way more 'non refactored examples' rather than refactoring examples
                # for now, we basically perform under sampling
                balanced_x, balanced_y = perform_under_sampling(x, y)
                assert balanced_x.shape[0] == balanced_y.shape[0], "Undersampling did not work"

                # apply some scaling to speed up the algorithm
                scaler = MinMaxScaler()  # Default behavior is to scale to [0,1]
                balanced_x = scaler.fit_transform(balanced_x)

                for model_name in models:
                    try:
                        f.write("\n\n- Model: %s\n\n" % model_name)
                        print("Model: %s" % model_name)

                        model = None

                        print("Started at %s\n" % now())
                        f.write("Started at %s\n" % now())

                        if model_name == 'svm':
                            model = run_svm(balanced_x, x.columns.values, balanced_y, f)
                        elif model_name == 'random-forest':
                            model = run_random_forest(balanced_x, x.columns.values, balanced_y, f)
                        elif model_name == 'decision-tree':
                            model = run_decision_tree(balanced_x, x.columns.values, balanced_y, f)
                        elif model_name == 'deep-learning':
                            model = run_deep_learning(balanced_x, x.columns.values, balanced_y, f, refactoring_name)
                        elif model_name == 'logistic-regression':
                            model = run_logistic_regression(balanced_x, x.columns.values, balanced_y, f)

                        print("Finished at %s\n" % now())
                        f.write("Finished at %s\n" % now())

                        save_model(model, model_name, dataset, refactoring_name)
                        sys.stdout.flush()
                        f.flush()
                    except Exception as e:
                        print("An error occurred while working on refactoring " + refactoring_name + " model " + model_name)
                        print(e)
                        print(traceback.format_exc())

            except Exception as e:
                print("An error occured while working on refactoring " + refactoring_name)
                print(e)
                print(traceback.format_exc())

            sys.stdout.flush()
            f.flush()
        f.close()


# Time for action!
print("[STARTING...]")
print("[BUILDING METHOD-LEVEL MODELS]")
build_model("method-level",
            db.get_method_level_refactorings_count,
            db.get_method_level_refactorings,
            db.get_non_refactored_methods)
print("[DONE BUILDING METHOD-LEVEL MODELS]")

print("[BUILDING VARIABLE-LEVEL MODELS]")
build_model("variable-level",
            db.get_variable_level_refactorings_count,
            db.get_variable_level_refactorings,
            db.get_non_refactored_variables)
print("[DONE BUILDING VARIABLE-LEVEL MODELS]")

print("[BUILDING CLASS-LEVEL MODELS]")
build_model("class-level",
            db.get_class_level_refactorings_count,
            db.get_class_level_refactorings,
            db.get_non_refactored_classes)
print("[DONE BUILDING CLASS-LEVEL MODELS]")

print("[BUILDING FIELD-LEVEL MODELS]")
build_model("field-level",
            db.get_field_level_refactorings_count,
            db.get_field_level_refactorings,
            db.get_non_refactored_fields)
print("[DONE BUILDING FIELD-LEVEL MODELS]")
print("[DONE]")
