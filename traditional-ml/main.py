import sys
import db

from refactoring_models_svm import run_svm
from refactoring_models_decision_tree import run_decision_tree
from refactoring_models_random_forest import run_random_forest

def build_model(model_name, refactoring_level, counts_function, refactoring_get_function, non_refactored_function):
    file_name = "results-" + refactoring_level + "-" + model_name + ".txt"
    f = open(file_name, "w+")
    counts = counts_function()
    for refactoring_name in counts["refactoring"].values:
        try:
            print("Refactoring %s" % refactoring_name)

            #get all refactoring examples we have in our dataset
            refactorings = refactoring_get_function(refactoring_name)
            assert refactorings.shape[0] == counts.loc[counts['refactoring'] == refactoring_name].iloc[0][1]

            #load non-refactoring examples
            non_refactored_fields = non_refactored_function()

            method_name = 'run_' + model_name.replace("-", "_") 
            possibles = globals().copy()
            possibles.update(locals())
            method = possibles.get(method_name)
            if not method:
                 raise NotImplementedError("Method %s not implemented" % method_name)
            #build model
            method(refactoring_name, refactorings, non_refactored_fields, f)
        except Exception as e:
            print("An error occured while building " + model_name.replace('-', ' ') + " model")
            print(str(e))

        sys.stdout.flush()
    f.close()

print("[STARTING...]")
#models/algorithms we use so far
models = ['svm', 'decision-tree', 'random-forest', 'deep-learning']

#method level
print("[BUILDING METHOD-LEVEL MODELS]")
for model in models: 
    build_model(model, "method-level", 
            db.get_method_level_refactorings_count, 
            db.get_method_level_refactorings, 
            db.get_non_refactored_methods)
print("[DONE BUILDING METHOD-LEVEL MODELS]")

#variable level
print("[BUILDING VARIABLE-LEVEL MODELS]")
for model in models: 
    build_model(model, "variable-level", 
            db.get_variable_level_refactorings_count,
            db.get_variable_level_refactorings,
            db.get_non_refactored_variables)
print("[DONE BUILDING VARIABLE-LEVEL MODELS]")

#class level
print("[BUILDING CLASS-LEVEL MODELS]")
for model in models: 
    build_model(model, "class-level", 
            db.get_class_level_refactorings_count,
            db.get_class_level_refactorings,
            db.get_non_refactored_classes)
print("[DONE BUILDING CLASS-LEVEL MODELS]")

#field level
print("[BUILDING FIELD-LEVEL MODELS]")
for model in models: 
    build_model(model, "field-level", 
            db.get_field_level_refactorings_count,
            db.get_field_level_refactorings,
            db.get_non_refactored_fields)
print("[DONE BUILDING FIELD-LEVEL MODELS]")

print("[DONE]")

