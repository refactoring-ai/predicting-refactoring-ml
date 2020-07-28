from configs import DATASETS, Level
from db.QueryBuilder import get_all_level_stable, get_level_refactorings_count, get_level_refactorings
from db.DBConnector import execute_query
from utils.log import log_init, log_close
import time

log_init()
print('Begin cache warm-up')
start_time = time.time()

for dataset in DATASETS:
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

print('Cache warm-up took %s seconds.' % (time.time() - start_time))
log_close()
