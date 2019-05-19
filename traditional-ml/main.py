import sys

import db


def build_model(dataset, model_name, refactoring_level, counts_function, refactoring_get_function, non_refactored_function):
    file_name = "results-" + refactoring_level + "-" + model_name + "-" + ("all" if dataset == "" else dataset) + ".txt"
    f = open(file_name, "w+")
    counts = counts_function(dataset)
    for refactoring_name in counts["refactoring"].values:
        try:
            print("Refactoring %s" % refactoring_name)

            # get all refactoring examples we have in our dataset
            refactorings = refactoring_get_function(refactoring_name, dataset)
            assert refactorings.shape[0] == counts.loc[counts['refactoring'] == refactoring_name].iloc[0][1]

            # load non-refactoring examples
            non_refactored_fields = non_refactored_function(dataset)

            method_name = 'run_' + model_name.replace("-", "_")
            possibles = globals().copy()
            possibles.update(locals())
            method = possibles.get(method_name)
            if not method:
                raise NotImplementedError("Method %s not implemented" % method_name)
            # build model
            method(refactoring_name, refactorings, non_refactored_fields, f)
        except Exception as e:
            print("An error occured while building " + model_name.replace('-', ' ') + " model")
            print(str(e))

        sys.stdout.flush()
    f.close()


print("[STARTING...]")
# models/algorithms we use so far
models = ['svm', 'decision-tree', 'random-forest', 'deep-learning']
datasets = ['', 'apache', 'github', 'fdroid']

for model in models:
    for dataset in datasets:
        print("Model: " + model + " dataset " + ("all" if dataset == "" else dataset))

        print("[BUILDING METHOD-LEVEL MODELS]")
        build_model(dataset, model, "method-level",
                    db.get_method_level_refactorings_count,
                    db.get_method_level_refactorings,
                    db.get_non_refactored_methods)
        print("[DONE BUILDING METHOD-LEVEL MODELS]")

        print("[BUILDING VARIABLE-LEVEL MODELS]")
        build_model(dataset, model, "variable-level",
                    db.get_variable_level_refactorings_count,
                    db.get_variable_level_refactorings,
                    db.get_non_refactored_variables)
        print("[DONE BUILDING VARIABLE-LEVEL MODELS]")

        print("[BUILDING CLASS-LEVEL MODELS]")
        build_model(dataset, model, "class-level",
                    db.get_class_level_refactorings_count,
                    db.get_class_level_refactorings,
                    db.get_non_refactored_classes)
        print("[DONE BUILDING CLASS-LEVEL MODELS]")

        print("[BUILDING FIELD-LEVEL MODELS]")
        build_model(dataset, model, "field-level",
                    db.get_field_level_refactorings_count,
                    db.get_field_level_refactorings,
                    db.get_non_refactored_fields)
        print("[DONE BUILDING FIELD-LEVEL MODELS]")

print("[DONE]")
