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
        assert refactorings.shape[0] == counts.loc[counts['refactoring'] == refactoring_name]

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
        assert refactorings.shape[0] == counts.loc[counts['refactoring'] == refactoring_name]

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
        assert refactorings.shape[0] == counts.loc[counts['refactoring'] == refactoring_name]

        # load the non-refactoring examples
        non_refactored_classes = db.get_non_refactored_classes()

        run_svm(refactoring_name, refactorings, non_refactored_classes, f)

        sys.stdout.flush()
    f.close()


print("Starting")

all_method_level()
all_variable_level()
all_class_level()

print("Done")