import sys

import db
from refactoring_models_svm import run_svm


def build_model(file_name, counts_function, refactoring_get_function, non_refactored_function):
    f = open(file_name, "w+")
    counts = counts_function()
    for refactoring_name in counts["refactoring"].values:
        try:
            print("Field-level refactoring %s" % refactoring_name)

            # get all the refactoring examples we have in our dataset
            refactorings = refactoring_get_function(refactoring_name)
            assert refactorings.shape[0] == counts.loc[counts['refactoring'] == refactoring_name].iloc[0][1]

            # load the non-refactoring examples
            non_refactored_fields = non_refactored_function()

            run_svm(refactoring_name, refactorings, non_refactored_fields, f)
        except Exception as e:
            print("An error occured.")
            print(str(e))
            print("-------")

        sys.stdout.flush()
    f.close()

print("Starting")

# method level
build_model("results-method-level-svm.txt",
            db.get_method_level_refactorings_count,
            db.get_method_level_refactorings,
            db.get_non_refactored_methods)

# variable level
build_model("results-variable-level-svm.txt",
            db.get_variable_level_refactorings_count,
            db.get_variable_level_refactorings,
            db.get_non_refactored_variables)

# class level
build_model("results-class-level-svm.txt",
            db.get_class_level_refactorings_count,
            db.get_class_level_refactorings,
            db.get_non_refactored_classes)

# field level
build_model("results-field-level-svm.txt",
            db.get_field_level_refactorings_count,
            db.get_field_level_refactorings,
            db.get_non_refactored_fields)

print("Done")
