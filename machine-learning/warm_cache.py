from configs import DATASETS, Level
from db.QueryBuilder import get_all_level_stable, get_level_refactorings_count, get_level_refactorings, get_refactoring_types
from db.DBConnector import execute_query
from utils.log import log_init, log_close

datasets = DATASETS

log_init()
print('begin cache warm-up')

for dataset in datasets:
    print("dataset: " + dataset)
    for level in Level:
        print("-- non refactored instances for " + str(level))
        non_refactored = execute_query(get_all_level_stable(int(level), dataset))
        print(str(len(non_refactored)) + " non-refactored instances were found for level: " + str(level))

        print("-- " + str(level) + " refactoring types with count")
        refactorings = execute_query(get_level_refactorings_count(int(level), dataset))
        print(refactorings)
        for refactoring_name in refactorings['refactoring']:
            refactoring_instances = execute_query(get_level_refactorings(int(level), refactoring_name, dataset))

print('end cache warm-up')
log_close()