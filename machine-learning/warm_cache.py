from enum import Enum
from db.QueryBuilder import get_all_level_stable, get_level_refactorings_count, get_level_refactorings, get_refactoring_types
from db.DBConnector import execute_query


class Level(Enum):
    Class = 1
    Method = 2
    Variable = 3
    Field = 4
    Other = 5


datasets = ['', 'apache', 'github', 'fdroid']


print('begin cache warmup')


for dataset in datasets:
    print("dataset: " + dataset)

    print("-- non refactored methods")
    execute_query(get_all_level_stable(2, dataset))
    print("-- non refactored variables")
    execute_query(get_all_level_stable(3, dataset))
    print("-- non refactored classes")
    execute_query(get_all_level_stable(1, dataset))
    print("-- non refactored fields")
    execute_query(get_all_level_stable(4, dataset))

    print("-- refactoring types")
    execute_query(get_refactoring_types(dataset))

    for level in Level:
        print("-- " + str(level) + " level refactoring count")
        refactorings =  execute_query(get_level_refactorings_count(int(level), dataset))
        for refactoring_name in refactorings["refactoring"].values:
            print("---- " + refactoring_name)
            execute_query(get_level_refactorings(int(level), refactoring_name, dataset))


print('end cache warmup')