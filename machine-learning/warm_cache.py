from db.refactoringdb import get_non_refactored_methods, get_non_refactored_variables, get_non_refactored_classes, \
    get_non_refactored_fields, get_refactoring_types, get_class_level_refactorings_count, get_class_level_refactorings, \
    get_method_level_refactorings_count, get_method_level_refactorings, get_variable_level_refactorings_count, \
    get_variable_level_refactorings, get_field_level_refactorings_count, get_field_level_refactorings

datasets = ['', 'apache', 'github', 'fdroid']

print('begin cache warmup')


for dataset in datasets:
    print("dataset: " + dataset)

    print("-- non refactored methods")
    get_non_refactored_methods(dataset)
    print("-- non refactored variables")
    get_non_refactored_variables(dataset)
    print("-- non refactored classes")
    get_non_refactored_classes(dataset)
    print("-- non refactored fields")
    get_non_refactored_fields(dataset)

    print("-- refactoring types")
    get_refactoring_types(dataset)

    print("-- class level refactoring count")
    class_refactorings = get_class_level_refactorings_count(dataset)
    for refactoring_name in class_refactorings["refactoring"].values:
        print("---- " + refactoring_name)
        get_class_level_refactorings(refactoring_name, dataset)

    print("-- method level refactoring count")
    method_refactorings = get_method_level_refactorings_count(dataset)
    for refactoring_name in method_refactorings["refactoring"].values:
        print("---- " + refactoring_name)
        get_method_level_refactorings(refactoring_name, dataset)

    print("-- variable level refactoring count")
    variable_refactorings = get_variable_level_refactorings_count(dataset)
    for refactoring_name in variable_refactorings["refactoring"].values:
        print("---- " + refactoring_name)
        get_variable_level_refactorings(refactoring_name, dataset)

    print("-- field level refactoring count")
    field_refactorings = get_field_level_refactorings_count(dataset)
    for refactoring_name in field_refactorings["refactoring"].values:
        print("---- " + refactoring_name)
        get_field_level_refactorings(refactoring_name, dataset)


print('end cache warmup')