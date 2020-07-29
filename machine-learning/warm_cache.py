from configs import DATASETS, Level, VALIDATION_DATASETS
from db.QueryBuilder import get_all_level_stable, get_level_refactorings_count, get_level_refactorings
from db.DBConnector import execute_query
from utils.log import log_init, log_close, log
import time

log_init()
log('Begin cache warm-up')
start_time = time.time()

for dataset in (DATASETS + VALIDATION_DATASETS):
    log("\n**** dataset: " + dataset)
    for level in Level:
        log("-- non refactored instances for " + str(level))
        non_refactored = execute_query(get_all_level_stable(int(level), dataset))
        log(str(len(non_refactored)) + " non-refactored instances were found for level: " + str(level))

        log("-- " + str(level) + " refactoring types with count")
        refactorings = execute_query(get_level_refactorings_count(int(level), dataset))
        log(refactorings.to_string())
        for refactoring_name in refactorings['refactoring']:
            refactoring_instances = execute_query(get_level_refactorings(int(level), refactoring_name, dataset))

log('Cache warm-up took %s seconds.' % (time.time() - start_time))
log_close()
