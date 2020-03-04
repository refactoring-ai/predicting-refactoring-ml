from db.db_utils import execute_query


# region database structure
# table names for reference:
commitMetaData: str = "commitmetadata"
methodMetrics: str = "methodmetric"
fieldMetrics: str = "fieldmetric"
variableMetrics: str = "variablemetric"
processMetrics: str = "processmetrics"
classMetrics: str = "classmetric"
processMetrics: str = "processmetric"
refactoringCommits: str = "refactoringcommit"
stableCommits: str = "stablecommits"

# the ids are not included as they are the same for every table: id : long
classMetricsFields = ["classAnonymousClassesQty",
                      "classAssignmentsQty",
                      "classCbo",
                      "classComparisonsQty",
                      "classLambdasQty",
                      "classLcom",
                      "classLoc",
                      "classLoopQty",
                      "classMathOperationsQty",
                      "classMaxNestedBlocks",
                      "classNosi",
                      "classNumberOfAbstractMethods",
                      "classNumberOfDefaultFields",
                      "classNumberOfDefaultMethods",
                      "classNumberOfFields",
                      "classNumberOfFinalFields",
                      "classNumberOfFinalMethods",
                      "classNumberOfMethods",
                      "classNumberOfPrivateFields",
                      "classNumberOfPrivateMethods",
                      "classNumberOfProtectedFields",
                      "classNumberOfProtectedMethods",
                      "classNumberOfPublicFields",
                      "classNumberOfPublicMethods",
                      "classNumberOfStaticFields",
                      "classNumberOfStaticMethods",
                      "classNumberOfSynchronizedFields",
                      "classNumberOfSynchronizedMethods",
                      "classNumbersQty",
                      "classParenthesizedExpsQty",
                      "classReturnQty",
                      "classRfc",
                      "classStringLiteralsQty",
                      "classSubClassesQty",
                      "classTryCatchQty",
                      "classUniqueWordsQty",
                      "classVariablesQty",
                      "classWmc",
                      "isInnerClass"]
methodMetricsFields = ["fullMethodName",
                       "methodAnonymousClassesQty",
                       "methodAssignmentsQty",
                       "methodCbo",
                       "methodComparisonsQty",
                       "methodLambdasQty",
                       "methodLoc",
                       "methodLoopQty",
                       "methodMathOperationsQty",
                       "methodMaxNestedBlocks",
                       "methodNumbersQty",
                       "methodParametersQty",
                       "methodParenthesizedExpsQty",
                       "methodReturnQty",
                       "methodRfc",
                       "methodStringLiteralsQty",
                       "methodSubClassesQty",
                       "methodTryCatchQty",
                       "methodUniqueWordsQty",
                       "methodVariablesQty",
                       "methodWmc",
                       "shortMethodName",
                       "startLine"]
variableMetricsFields = ["variableAppearances",
                         "variableName"]
fieldMetricsFields = ["fieldAppearances",
                      "fieldName"]
processMetricsFields = ["authorOwnership",
                        "bugFixCount",
                        "linesAdded",
                        "linesDeleted",
                        "qtyMajorAuthors",
                        "qtyMinorAuthors",
                        "qtyOfAuthors",
                        "qtyOfCommits",
                        "refactoringsInvolved"]
commitMetaDataFields = ["commitDate",
                        "commitId",
                        "commitMessage",
                        "commitUrl",
                        "parentCommitId"]
projectFields = ["commitCountThresholds",
                 "commits",
                 "datasetName",
                 "dateOfProcessing",
                 "exceptionsCount",
                 "finishedDate",
                 "gitUrl",
                 "isLocal",
                 "javaLoc",
                 "lastCommitHash",
                 "numberOfProductionFiles",
                 "numberOfTestFiles",
                 "productionLoc",
                 "projectName",
                 "projectSizeInBytes",
                 "testLoc"]
refactoringCommitFields = ["className",
                           "filePath",
                           "isTest",
                           "level",
                           "refactoring",
                           "refactoringSummary"]
stableCommitFields = ["className",
                      "filePath",
                      "isTest",
                      "level"]
# all tables referenced from the instance base class for refactoring commit and stable commit
instanceReferences = ["classMetrics_id",
                      "commitMetaData_id",
                      "fieldMetrics_id",
                      "methodMetrics_id",
                      "processMetrics_id",
                      "project_id",
                      "variableMetrics_id"]
# endregion


# region tables utils
# returns an sql condition as a string to filter based on the project name
def __project_filter(instance_name: str, project_name: str) -> str:
    return instance_name + ".project_id in (select id from project where datasetName = " + project_name + ")"


# returns an sql condition to join instances with method metrics
# join an instance table with the given table
def __join_tables(instance_name: str, table_name: str) -> str:
    if table_name == commitMetaData:
        return instance_name + ".commitMetaData_id = " + table_name + ".id"
    elif table_name == methodMetrics:
        return instance_name + ".methodMetrics_id = " + table_name + ".id"
    elif table_name == fieldMetrics:
        return instance_name + ".methodMetrics_id = " + table_name + ".id"
    elif table_name == variableMetrics:
        return instance_name + ".methodMetrics_id = " + table_name + ".id"
    elif table_name == processMetrics:
        return instance_name + ".methodMetrics_id = " + table_name + ".id"
    elif table_name == classMetrics:
        return instance_name + ".methodMetrics_id = " + table_name + ".id"
    elif table_name == processMetrics:
        return instance_name + ".project_id = " + table_name + ".id"


# returns a list of all metrics in regard to the given level
def __get_metrics_level(level: str):
    # class level
    if level == 1:
        return [(classMetrics, classMetricsFields), (processMetrics, processMetricsFields)]
    # method level
    elif level == 2:
        return [(classMetrics, classMetricsFields), (methodMetrics, methodMetricsFields),
                (processMetrics, processMetricsFields)]
    # variable level
    elif level == 3:
        return [(classMetrics, classMetricsFields), (methodMetrics, methodMetricsFields),
                (variableMetrics, variableMetricsFields), (processMetrics, processMetricsFields)]
    # method level
    elif level == 4:
        return [(classMetrics, classMetricsFields), (fieldMetrics, fieldMetricsFields),
                (processMetrics, processMetricsFields)]


# instance name: name of the instance table you are querying, e.g. refactoringcommit or stablecommit, this is also given in the fields
# fields: a list containing the table name as string and all required fields from the table as a list of strings e.g [commitMetaData, commitMetaDataFields]
# an instance has to be part of fields together with at least one field, either refactoring commit or stablecommit
# Optional conditions: a string with additional conditions for the instances, e.g. cm.isInnerClass = 1
# Optional dataset: filter the instances based on their project name, e.g. toyproject-1
# Optional order: order by command, e.g. order by cm.commitDate
def __get_instance_fields(instance_name: str, fields, conditions: str = "", dataset: str = "", order: str = ""):
    # combine the required fields with their table names
    required_fields: str = ""
    required_tables: str = ""
    join_conditions: str = ""
    for table_name, field_names in fields:
        required_tables += table_name + ", "
        join_conditions += __join_tables(instance_name, table_name) + " AND "
        for field_name in field_names:
            required_fields += table_name + ", " + field_name
    # remove the last chars because it is either a ", " or an " AND "
    required_fields = required_fields[:-2]
    required_tables = required_tables[:-2]
    join_conditions = join_conditions[:-5]

    sql: str = "SELECT " + required_fields + " FROM " + required_tables + " WHERE " + join_conditions
    if conditions != "":
        sql += " AND " + conditions
    if dataset != "":
        sql += " AND " + __project_filter(instance_name, dataset)

    return sql + " " + order
# endregion


# region Public interaction
# get the count of all refactoring levels
def get_refactoring_levels(dataset=""):
    sql: str = "SELECT refactoring, count(*) total from refactoringcommit where " + __project_filter(
        dataset) + " group by refactoring order by count(*) desc"
    return execute_query(sql)


# get the count of all refactorings for the given level
def get_level_refactorings_count(level: str, dataset: str = ""):
    sql: str = "SELECT refactoring, count(*) FROM " + \
               __get_instance_fields(refactoringCommits, [(refactoringCommits, refactoringCommitFields)] +
                                     __get_metrics_level(level), refactoringCommits + ".level = " + level, dataset) +\
               " group by refactoring order by count(*) desc"
    return execute_query(sql)


# get all refactoring instances with the given refactoring type and metrics in regard to the level
def get_level_refactorings(m_refactoring: str, level: str, dataset: str = ""):
    sql: str = __get_instance_fields(refactoringCommits, [(refactoringCommits, refactoringCommitFields)] + __get_metrics_level(level),
                                     refactoringCommits + ".refactoring = " + m_refactoring, dataset, " order by " + commitMetaData + ".commitDate")
    return execute_query(sql)


# get all refactoring instances with the given level and the corresponding metrics
def get_all_level_refactorings(level: str, dataset: str = ""):
    sql: str = __get_instance_fields(refactoringCommits, [(refactoringCommits, refactoringCommitFields)] + __get_metrics_level(level),
                                     refactoringCommits + ".level = " + level, dataset, " order by " + commitMetaData + ".commitDate")
    return execute_query(sql)


# get all stable instances with the given level and the corresponding metrics
def get_level_stable(level: str, dataset: str = ""):
    sql: str = __get_instance_fields(stableCommits, [(stableCommits, stableCommitFields)] + __get_metrics_level(level),
                                     stableCommits + ".level = " + level, dataset, " order by " + commitMetaData + ".commitDate")
    return execute_query(sql)
# endregion
