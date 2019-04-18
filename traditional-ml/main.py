import sys

import db
from refactoring_models_svm import run_svm


def all_method_level():
    f = open("results-method-level-svm.txt", "w+")
    counts = db.get_method_level_refactorings_count()
    for refactoring_name in counts["refactoring"].values:
        print("Method-level refactoring %s" % refactoring_name)

        # get all the refactoring examples we have in our dataset
        refactorings = db.get_method_level_refactorings(refactoring_name)
        assert refactorings.shape[0] == counts.loc[counts['refactoring'] == refactoring_name].iloc[0][1]

        # load the non-refactoring examples
        non_refactored_methods = db.get_non_refactored_methods()

        run_svm(refactoring_name, refactorings, non_refactored_methods, f)
    f.close()


def all_variable_level():
    f = open("results-variable-level-svm.txt", "w+")
    counts = db.get_variable_level_refactorings_count()
    for refactoring_name in counts["refactoring"].values:
        print("Variable-level refactoring %s" % refactoring_name)

        # get all the refactoring examples we have in our dataset
        refactorings = db.get_variable_level_refactorings(refactoring_name)
        assert refactorings.shape[0] == counts.loc[counts['refactoring'] == refactoring_name].iloc[0][1]

        # load the non-refactoring examples
        non_refactored_variables = db.get_non_refactored_variables()

        run_svm(refactoring_name, refactorings, non_refactored_variables, f)

        sys.stdout.flush()
    f.close()


def all_class_level():
    f = open("results-class-level-svm.txt", "w+")
    counts = db.get_class_level_refactorings_count()
    for refactoring_name in counts["refactoring"].values:
        print("Class-level refactoring %s" % refactoring_name)

        # get all the refactoring examples we have in our dataset
        refactorings = db.get_class_level_refactorings(refactoring_name)
        assert refactorings.shape[0] == counts.loc[counts['refactoring'] == refactoring_name].iloc[0][1]

        # load the non-refactoring examples
        non_refactored_classes = db.get_non_refactored_classes()

        run_svm(refactoring_name, refactorings, non_refactored_classes, f)

        sys.stdout.flush()
    f.close()


def all_field_level():
    f = open("results-field-level-svm.txt", "w+")
    counts = db.get_field_level_refactorings_count()
    for refactoring_name in counts["refactoring"].values:
        print("Field-level refactoring %s" % refactoring_name)

        # get all the refactoring examples we have in our dataset
        refactorings = db.get_field_level_refactorings(refactoring_name)
        assert refactorings.shape[0] == counts.loc[counts['refactoring'] == refactoring_name].iloc[0][1]

        # load the non-refactoring examples
        non_refactored_fields = db.get_non_refactored_fields()

        run_svm(refactoring_name, refactorings, non_refactored_fields, f)

        sys.stdout.flush()
    f.close()


def build_model(file_name, counts_function, refactoring_get_function, non_refactored_function):
    f = open(file_name, "w+")
    counts = counts_function()
    for refactoring_name in counts["refactoring"].values:
        print("Field-level refactoring %s" % refactoring_name)

        # get all the refactoring examples we have in our dataset
        refactorings = refactoring_get_function(refactoring_name)
        assert refactorings.shape[0] == counts.loc[counts['refactoring'] == refactoring_name].iloc[0][1]

        # load the non-refactoring examples
        non_refactored_fields = non_refactored_function()

        run_svm(refactoring_name, refactorings, non_refactored_fields, f)

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
