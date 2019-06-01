import sys

import joblib
import pandas as pd
from sklearn.preprocessing import MinMaxScaler

import db
from ml_utils import perform_under_sampling

# all the models and datasets we have available
models = ['logistic_regression', 'svm', 'decision-tree', 'random-forest', 'deep-learning']
datasets = ['', 'apache', 'github', 'fdroid']


def find_model_method(model_name):
    method_name = 'run_' + model_name.replace("-", "_")
    possibles = globals().copy()
    possibles.update(locals())
    method = possibles.get(method_name)
    if not method:
        raise NotImplementedError("Method %s not implemented" % method_name)
    return method


def save_model(model, model_name, dataset, refactoring_name):
    joblib.dump(model, ("model-%s-%s-%s.joblib", model_name, dataset, refactoring_name))


def build_model(refactoring_level, counts_function, get_refactored_function, get_non_refactored_function):
    for dataset in datasets:

        file_name = "results-" + refactoring_level + "-" + ("all" if dataset == "" else dataset) + ".txt"
        f = open(file_name, "w+")

        counts = counts_function(dataset)
        for refactoring_name in counts["refactoring"].values:
            try:
                f.write("**** %s\n\n" % refactoring_name)
                print("Refactoring %s" % refactoring_name)

                # get all refactoring examples we have in our dataset
                refactored_instances = get_refactored_function(refactoring_name, dataset)

                # load non-refactoring examples
                non_refactored_instances = get_non_refactored_function(dataset)

                f.write("refactoring instances: " + refactored_instances.shape[0] + "\n")
                f.write("not refactoring instances: " + non_refactored_instances.shape[0] + "\n")

                # if there' still a row with NAs, drop it as it'll cause a failure later on.
                refactored_instances = refactored_instances.dropna()
                non_refactored_instances = non_refactored_instances.dropna()

                f.write("refactoring instance(after dropping NA)s (after dropping NA): " + refactored_instances.shape[
                    0] + "\n")
                f.write("not refactoring instances: " + non_refactored_instances.shape[0] + "\n\n")

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
                    f.write("\n\n- Model: %s\n\n" % model_name)
                    print("- %s" % model_name)

                    method = find_model_method(model_name)

                    # build model
                    try:
                        model = method(balanced_x, balanced_y, f)
                        save_model(model, model_name, dataset, refactoring_name)
                    except:
                        print("An error occured while building " + model_name.replace('-', ' ') + " model")
                        print(str(e))

            except Exception as e:
                print("An error occured while working on refactoring " + refactoring_name)
                print(str(e))

            sys.stdout.flush()
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
