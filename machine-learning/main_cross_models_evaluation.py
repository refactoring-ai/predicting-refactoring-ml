import traceback
from collections import Counter

import pandas as pd
from sklearn import metrics
from sklearn.utils import shuffle

from db import refactoringdb
from configs import DATASETS, MODELS
from ml.preprocessing.sampling import perform_balancing
from utils.log import log, log_init
from utils.ml_utils import load_model, load_scaler


# This only works with models built _without_ feature reduction
def check_model_performance(refactoring_level, counts_function, get_refactored_function, get_non_refactored_function):

    log("Starting cross model analysis at " + refactoring_level)

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

                # shuffle the array
                # (not really necessary, though, as this dataset is entirely for test)
                merged_dataset = shuffle(merged_dataset)

                # separate the x from the y (as required by the scikit-learn API)
                x = merged_dataset.drop("prediction", axis=1)
                y = merged_dataset["prediction"]

                # drop process and ownership metrics, if not class level
                if not refactoring_level == 'class-level':
                    x = x.drop(["authorOwnership","bugFixCount","linesAdded","linesDeleted","qtyMajorAuthors",
                                "qtyMinorAuthors","qtyOfAuthors","qtyOfCommits","refactoringsInvolved"], axis=1)

                # drop 'default fields' and 'default methods' as
                # they were not properly collected during the collection phase
                x = x.drop(["classNumberOfDefaultFields", "classNumberOfDefaultMethods"], axis=1)

                # balance the datasets
                balanced_x, balanced_y = perform_balancing(x, y)
                log("instances after balancing: {}".format(Counter(balanced_y)))

                for model_name in MODELS:
                    try:
                        log("Refactoring %s, model %s, dataset 1 %s, dataset 2 %s" % (refactoring_name, model_name, d1, d2))

                        # scale it (as in the training of the model)
                        # using the scaler that was generated during training time
                        scaler = load_scaler("models", model_name, d1, refactoring_name)
                        balanced_x_2 = scaler.transform(balanced_x)

                        model_under_eval = load_model("models", model_name, d1, refactoring_name)

                        if model_name == 'deep-learning':
                            y_predicted = model_under_eval.predict_classes(balanced_x_2)
                        else:
                            y_predicted = model_under_eval.predict(balanced_x_2)

                        results = metrics.classification_report(balanced_y, y_predicted,output_dict=True)

                        log(results)
                        log("CSV," + d1 + "," + d2 + "," + refactoring_name + "," + model_name + "," + str(results["macro avg"]["precision"]) + "," + str(results["macro avg"]["recall"]))
                        # TODO: log more info, like the entire confusion matrix

                    except Exception as e:
                        log("An error occurred while working on refactoring " + refactoring_name + " model " + model_name)
                        log(e)
                        log(traceback.format_exc())


log_init()
log("ML4Refactoring: Cross-project validation")

log("CSV format: dataset_loaded_model,dataset_test,refactoring,model,precision,recall\n")

log("[COMPARING METHOD-LEVEL MODELS]")
check_model_performance("method-level",
                        refactoringdb.get_method_level_refactorings_count,
                        refactoringdb.get_method_level_refactorings,
                        refactoringdb.get_non_refactored_methods)

log("[COMPARING CLASS-LEVEL MODELS]")
check_model_performance("class-level",
                        refactoringdb.get_class_level_refactorings_count,
                        refactoringdb.get_class_level_refactorings,
                        refactoringdb.get_non_refactored_classes)

log("[COMPARING VARIABLE-LEVEL MODELS]")
check_model_performance("variable-level",
                        refactoringdb.get_variable_level_refactorings_count,
                        refactoringdb.get_variable_level_refactorings,
                        refactoringdb.get_non_refactored_variables)
