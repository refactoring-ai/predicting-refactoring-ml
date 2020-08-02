from db.QueryBuilder import get_level_refactorings_count
from db.DBConnector import execute_query
from utils.log import log_init, log_close, log
import time
from configs import Level

log_init("Refactorings per commit statistics")
log('Begin cache warm-up')
start_time = time.time()

for level in Level:
    log("-- " + str(level) + " refactoring types with count")
    refactorings = execute_query(get_level_refactorings_count(int(level), ""))
    log(refactorings.to_string())
    for refactoring_name in refactorings['refactoring']:
        refactoring_instances = execute_query(
            "SELECT \
                SUM(\`Change Attribute Type count\`), SUM(\`Change Package count\`), SUM(\`Change Parameter Type count\`), SUM(\`Change Return Type count\`), SUM(\`Change Variable Type count\`), SUM(\`Extract And Move Method count\`), SUM(\`Extract Attribute count\`), SUM(\`Extract Class count\`), SUM(\`Extract Interface count\`), SUM(\`Extract Method count\`), SUM(\`Extract Subclass count\`), SUM(\`Extract Superclass count\`), SUM(\`Extract Variable count\`), SUM(\`Inline Method count\`), SUM(\`Inline Variable count\`), SUM(\`Merge Parameter count\`), SUM(\`Merge Variable count\`), SUM(\`Move And Inline Method count\`), SUM(\`Move And Rename Attribute count\`), SUM(\`Move And Rename Class count\`), SUM(\`Move And Rename Method count\`), SUM(\`Move Attribute count\`), SUM(\`Move Class count\`), SUM(\`Move Method count\`), SUM(\`Move Source Folder count\`), SUM(\`Parameterize Variable count\`), SUM(\`Pull Up Attribute count\`), SUM(\`Pull Up Method count\`), SUM(\`Push Down Attribute count\`), SUM(\`Push Down Method count\`), SUM(\`Rename Attribute count\`), SUM(\`Rename Class count\`), SUM(\`Rename Method count\`), SUM(\`Rename Parameter count\`), SUM(\`Rename Variable count\`), SUM(\`Replace Attribute count\`), SUM(\`Replace Variable With Attribute count\`), SUM(\`Split Parameter count\`), SUM(\`Split Variable count\`) \
            FROM RefactoringsPerCommit \
            WHERE \
                \`%s count\` > 0" % refactoring_name)
        log(refactoring_instances)

log('Processing statistics took %s seconds.' % (time.time() - start_time))
log_close()