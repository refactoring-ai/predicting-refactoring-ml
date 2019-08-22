from db import refactoringdb

datasets = ['', 'apache', 'github', 'fdroid']

print('begin cache warmup')


for dataset in datasets:
    print("dataset: " + dataset)

    print("-- non refactored methods")
    refactoringdb.get_non_refactored_methods(dataset)
    print("-- non refactored variables")
    refactoringdb.get_non_refactored_variables(dataset)
    print("-- non refactored classes")
    refactoringdb.get_non_refactored_classes(dataset)
    print("-- non refactored fields")
    refactoringdb.get_non_refactored_fields(dataset)

    print("-- refactoring types")
    refactoringdb.get_refactoring_types(dataset)

    print("-- class level refactoring count")
    class_refactorings = refactoringdb.get_class_level_refactorings_count(dataset)
    for refactoring_name in class_refactorings["refactoring"].values:
        print("---- " + refactoring_name)
        refactoringdb.get_class_level_refactorings(refactoring_name, dataset)

    print("-- method level refactoring count")
    method_refactorings = refactoringdb.get_method_level_refactorings_count(dataset)
    for refactoring_name in method_refactorings["refactoring"].values:
        print("---- " + refactoring_name)
        refactoringdb.get_method_level_refactorings(refactoring_name, dataset)

    print("-- variable level refactoring count")
    variable_refactorings = refactoringdb.get_variable_level_refactorings_count(dataset)
    for refactoring_name in variable_refactorings["refactoring"].values:
        print("---- " + refactoring_name)
        refactoringdb.get_variable_level_refactorings(refactoring_name, dataset)

    print("-- field level refactoring count")
    field_refactorings = refactoringdb.get_field_level_refactorings_count(dataset)
    for refactoring_name in field_refactorings["refactoring"].values:
        print("---- " + refactoring_name)
        refactoringdb.get_field_level_refactorings(refactoring_name, dataset)


print('end cache warmup')