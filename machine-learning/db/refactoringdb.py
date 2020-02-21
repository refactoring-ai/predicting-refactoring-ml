from db.db_utils import execute_query


def get_refactoring_levels(dataset = ""):
    if dataset=="":
        sql = "SELECT refactoring, count(*) total from refactoringcommit group by refactoring order by count(*) desc"
    else:
        sql = "SELECT refactoring, count(*) total from refactoringcommit where project_id in (select id from project where datasetName = '" + dataset + "') group by refactoring order by count(*) desc";

    df = execute_query(sql)
    return df


def get_class_level_refactorings_count(dataset = ""):
    if dataset=="":
        sql = "SELECT refactoring, count(*) total from refactoringcommit where refactoringLevel = 1 group by refactoring order by count(*) desc"
    else:
        sql = "SELECT refactoring, count(*) total from refactoringcommit where refactoringLevel = 1 and project_id in (select id from project where datasetName = '" + dataset + "') group by refactoring order by count(*) desc"

    df = execute_query(sql)
    return df


def get_method_level_refactorings_count(dataset = ""):
    if dataset=="":
        sql = "SELECT refactoring, count(*) total from refactoringcommit where refactoringLevel = 2 group by refactoring order by count(*) desc"
    else:
        sql = "SELECT refactoring, count(*) total from refactoringcommit where refactoringLevel = 2 and project_id in (select id from project where datasetName = '" + dataset + "') group by refactoring order by count(*) desc"

    df = execute_query(sql)
    return df


def get_variable_level_refactorings_count(dataset = ""):
    if dataset=="":
        sql = "SELECT refactoring, count(*) total from refactoringcommit where refactoringLevel = 3 group by refactoring order by count(*) desc"
    else:
        sql = "SELECT refactoring, count(*) total from refactoringcommit where refactoringLevel = 3 and project_id in (select id from project where datasetName = '" + dataset + "') group by refactoring order by count(*) desc"

    df = execute_query(sql)
    return df


def get_field_level_refactorings_count(dataset = ""):
    if dataset=="":
        sql = "SELECT refactoring, count(*) total from refactoringcommit where refactoringLevel = 4 group by refactoring order by count(*) desc"
    else:
        sql = "SELECT refactoring, count(*) total from refactoringcommit where refactoringLevel = 4 and project_id in (select id from project where datasetName = '" + dataset + "') group by refactoring order by count(*) desc"

    df = execute_query(sql)
    return df


def get_method_level_refactorings(m_refactoring, dataset = ""):
    sql = (
        "select  " +
        "  classAnonymousClassesQty, " +
        "  classAssignmentsQty, " +
        "  classCbo, " +
        "  classComparisonsQty, " +
        "  classLambdasQty, " +
        "  classLcom, " +
        "  classLoc, " +
        "  classLoopQty, " +
        "  classMathOperationsQty, " +
        "  classMaxNestedBlocks, " +
        "  classNosi, " +
        "  classNumberOfAbstractMethods, " +
        "  classNumberOfDefaultFields, " +
        "  classNumberOfDefaultMethods, " +
        "  classNumberOfFields, " +
        "  classNumberOfFinalFields, " +
        "  classNumberOfFinalMethods, " +
        "  classNumberOfMethods, " +
        "  classNumberOfPrivateFields, " +
        "  classNumberOfPrivateMethods, " +
        "  classNumberOfProtectedFields, " +
        "  classNumberOfProtectedMethods, " +
        "  classNumberOfPublicFields, " +
        "  classNumberOfPublicMethods, " +
        "  classNumberOfStaticFields, " +
        "  classNumberOfStaticMethods, " +
        "  classNumberOfSynchronizedFields, " +
        "  classNumberOfSynchronizedMethods, " +
        "  classNumbersQty, " +
        "  classParenthesizedExpsQty, " +
        "  classReturnQty, " +
        "  classRfc, " +
        "  classStringLiteralsQty, " +
        "  classSubClassesQty, " +
        "  classTryCatchQty, " +
        "  classUniqueWordsQty, " +
        "  classVariablesQty, " +
        "  classWmc, " +

        "  methodAnonymousClassesQty, " +
        "  methodAssignmentsQty, " +
        "  methodCbo, " +
        "  methodComparisonsQty, " +
        "  methodLambdasQty, " +
        "  methodLoc, " +
        "  methodLoopQty, " +
        "  methodMathOperationsQty, " +
        "  methodMaxNestedBlocks, " +
        "  methodNumbersQty, " +
        "  methodParametersQty, " +
        "  methodParenthesizedExpsQty, " +
        "  methodReturnQty, " +
        "  methodRfc, " +
        "  methodStringLiteralsQty, " +
        "  methodSubClassesQty, " +
        "  methodTryCatchQty, " +
        "  methodUniqueWordsQty, " +
        "  methodVariablesQty, " +
        "  methodWmc, " +

        "  authorOwnership, " +
        "  bugFixCount, " +
        "  linesAdded, " +
        "  linesDeleted, " +
        "  qtyMajorAuthors, " +
        "  qtyMinorAuthors, " +
        "  qtyOfAuthors, " +
        "  qtyOfCommits, " +
        "  refactoringsInvolved " +
        " " +
        "from refactoringcommit " +
        "where refactoring = '" + m_refactoring + "'"

    )

    if not dataset == "":
        sql = sql + " and project_id in (select id from project where datasetName = '" + dataset + "')"

    sql = sql + " order by refactoringDate"

    df = execute_query(sql)
    return df


def get_all_method_level_refactorings(level, dataset = ""):
    sql = (
        "select refactoring," +
        "  classAnonymousClassesQty, " +
        "  classAssignmentsQty, " +
        "  classCbo, " +
        "  classComparisonsQty, " +
        "  classLambdasQty, " +
        "  classLcom, " +
        "  classLoc, " +
        "  classLoopQty, " +
        "  classMathOperationsQty, " +
        "  classMaxNestedBlocks, " +
        "  classNosi, " +
        "  classNumberOfAbstractMethods, " +
        "  classNumberOfDefaultFields, " +
        "  classNumberOfDefaultMethods, " +
        "  classNumberOfFields, " +
        "  classNumberOfFinalFields, " +
        "  classNumberOfFinalMethods, " +
        "  classNumberOfMethods, " +
        "  classNumberOfPrivateFields, " +
        "  classNumberOfPrivateMethods, " +
        "  classNumberOfProtectedFields, " +
        "  classNumberOfProtectedMethods, " +
        "  classNumberOfPublicFields, " +
        "  classNumberOfPublicMethods, " +
        "  classNumberOfStaticFields, " +
        "  classNumberOfStaticMethods, " +
        "  classNumberOfSynchronizedFields, " +
        "  classNumberOfSynchronizedMethods, " +
        "  classNumbersQty, " +
        "  classParenthesizedExpsQty, " +
        "  classReturnQty, " +
        "  classRfc, " +
        "  classStringLiteralsQty, " +
        "  classSubClassesQty, " +
        "  classTryCatchQty, " +
        "  classUniqueWordsQty, " +
        "  classVariablesQty, " +
        "  classWmc, " +

        "  methodAnonymousClassesQty, " +
        "  methodAssignmentsQty, " +
        "  methodCbo, " +
        "  methodComparisonsQty, " +
        "  methodLambdasQty, " +
        "  methodLoc, " +
        "  methodLoopQty, " +
        "  methodMathOperationsQty, " +
        "  methodMaxNestedBlocks, " +
        "  methodNumbersQty, " +
        "  methodParametersQty, " +
        "  methodParenthesizedExpsQty, " +
        "  methodReturnQty, " +
        "  methodRfc, " +
        "  methodStringLiteralsQty, " +
        "  methodSubClassesQty, " +
        "  methodTryCatchQty, " +
        "  methodUniqueWordsQty, " +
        "  methodVariablesQty, " +
        "  methodWmc, " +

        "  authorOwnership, " +
        "  bugFixCount, " +
        "  linesAdded, " +
        "  linesDeleted, " +
        "  qtyMajorAuthors, " +
        "  qtyMinorAuthors, " +
        "  qtyOfAuthors, " +
        "  qtyOfCommits, " +
        "  refactoringsInvolved " +
        " " +
        "from refactoringcommit " +
        "where refactoringLevel = " + level
    )

    if not dataset == "":
        sql = sql + " and project_id in (select id from project where datasetName = '" + dataset + "')"

    sql = sql + " order by refactoringDate"

    df = execute_query(sql)
    return df


def get_class_level_refactorings(m_refactoring, dataset = ""):
    sql = (
            "select  " +
            "  classAnonymousClassesQty, " +
            "  classAssignmentsQty, " +
            "  classCbo, " +
            "  classComparisonsQty, " +
            "  classLambdasQty, " +
            "  classLcom, " +
            "  classLoc, " +
            "  classLoopQty, " +
            "  classMathOperationsQty, " +
            "  classMaxNestedBlocks, " +
            "  classNosi, " +
            "  classNumberOfAbstractMethods, " +
            "  classNumberOfDefaultFields, " +
            "  classNumberOfDefaultMethods, " +
            "  classNumberOfFields, " +
            "  classNumberOfFinalFields, " +
            "  classNumberOfFinalMethods, " +
            "  classNumberOfMethods, " +
            "  classNumberOfPrivateFields, " +
            "  classNumberOfPrivateMethods, " +
            "  classNumberOfProtectedFields, " +
            "  classNumberOfProtectedMethods, " +
            "  classNumberOfPublicFields, " +
            "  classNumberOfPublicMethods, " +
            "  classNumberOfStaticFields, " +
            "  classNumberOfStaticMethods, " +
            "  classNumberOfSynchronizedFields, " +
            "  classNumberOfSynchronizedMethods, " +
            "  classNumbersQty, " +
            "  classParenthesizedExpsQty, " +
            "  classReturnQty, " +
            "  classRfc, " +
            "  classStringLiteralsQty, " +
            "  classSubClassesQty, " +
            "  classTryCatchQty, " +
            "  classUniqueWordsQty, " +
            "  classVariablesQty, " +
            "  classWmc, " +

            "  authorOwnership, " +
            "  bugFixCount, " +
            "  linesAdded, " +
            "  linesDeleted, " +
            "  qtyMajorAuthors, " +
            "  qtyMinorAuthors, " +
            "  qtyOfAuthors, " +
            "  qtyOfCommits, " +
            "  refactoringsInvolved " +
            " " +
            "from refactoringcommit " +
            "where refactoring = '" + m_refactoring + "'"
            )

    if not dataset == "":
        sql = sql + " and project_id in (select id from project where datasetName = '" + dataset + "')"

    sql = sql + " order by refactoringDate"

    df = execute_query(sql)
    return df


def get_all_class_level_refactorings(level, dataset = ""):
    sql = (
            "select refactoring, " +
            "  classAnonymousClassesQty, " +
            "  classAssignmentsQty, " +
            "  classCbo, " +
            "  classComparisonsQty, " +
            "  classLambdasQty, " +
            "  classLcom, " +
            "  classLoc, " +
            "  classLoopQty, " +
            "  classMathOperationsQty, " +
            "  classMaxNestedBlocks, " +
            "  classNosi, " +
            "  classNumberOfAbstractMethods, " +
            "  classNumberOfDefaultFields, " +
            "  classNumberOfDefaultMethods, " +
            "  classNumberOfFields, " +
            "  classNumberOfFinalFields, " +
            "  classNumberOfFinalMethods, " +
            "  classNumberOfMethods, " +
            "  classNumberOfPrivateFields, " +
            "  classNumberOfPrivateMethods, " +
            "  classNumberOfProtectedFields, " +
            "  classNumberOfProtectedMethods, " +
            "  classNumberOfPublicFields, " +
            "  classNumberOfPublicMethods, " +
            "  classNumberOfStaticFields, " +
            "  classNumberOfStaticMethods, " +
            "  classNumberOfSynchronizedFields, " +
            "  classNumberOfSynchronizedMethods, " +
            "  classNumbersQty, " +
            "  classParenthesizedExpsQty, " +
            "  classReturnQty, " +
            "  classRfc, " +
            "  classStringLiteralsQty, " +
            "  classSubClassesQty, " +
            "  classTryCatchQty, " +
            "  classUniqueWordsQty, " +
            "  classVariablesQty, " +
            "  classWmc, " +

            "  authorOwnership, " +
            "  bugFixCount, " +
            "  linesAdded, " +
            "  linesDeleted, " +
            "  qtyMajorAuthors, " +
            "  qtyMinorAuthors, " +
            "  qtyOfAuthors, " +
            "  qtyOfCommits, " +
            "  refactoringsInvolved " +
            " " +
            "from refactoringcommit " +
            "where refactoringLevel = " + level
            )

    if not dataset == "":
        sql = sql + " and project_id in (select id from project where datasetName = '" + dataset + "')"

    sql = sql + " order by refactoringDate"

    df = execute_query(sql)
    return df


def get_variable_level_refactorings(m_refactoring, dataset = ""):
    sql = (
        "select  " +
        "  classAnonymousClassesQty, " +
        "  classAssignmentsQty, " +
        "  classCbo, " +
        "  classComparisonsQty, " +
        "  classLambdasQty, " +
        "  classLcom, " +
        "  classLoc, " +
        "  classLoopQty, " +
        "  classMathOperationsQty, " +
        "  classMaxNestedBlocks, " +
        "  classNosi, " +
        "  classNumberOfAbstractMethods, " +
        "  classNumberOfDefaultFields, " +
        "  classNumberOfDefaultMethods, " +
        "  classNumberOfFields, " +
        "  classNumberOfFinalFields, " +
        "  classNumberOfFinalMethods, " +
        "  classNumberOfMethods, " +
        "  classNumberOfPrivateFields, " +
        "  classNumberOfPrivateMethods, " +
        "  classNumberOfProtectedFields, " +
        "  classNumberOfProtectedMethods, " +
        "  classNumberOfPublicFields, " +
        "  classNumberOfPublicMethods, " +
        "  classNumberOfStaticFields, " +
        "  classNumberOfStaticMethods, " +
        "  classNumberOfSynchronizedFields, " +
        "  classNumberOfSynchronizedMethods, " +
        "  classNumbersQty, " +
        "  classParenthesizedExpsQty, " +
        "  classReturnQty, " +
        "  classRfc, " +
        "  classStringLiteralsQty, " +
        "  classSubClassesQty, " +
        "  classTryCatchQty, " +
        "  classUniqueWordsQty, " +
        "  classVariablesQty, " +
        "  classWmc, " +

        "  methodAnonymousClassesQty, " +
        "  methodAssignmentsQty, " +
        "  methodCbo, " +
        "  methodComparisonsQty, " +
        "  methodLambdasQty, " +
        "  methodLoc, " +
        "  methodLoopQty, " +
        "  methodMathOperationsQty, " +
        "  methodMaxNestedBlocks, " +
        "  methodNumbersQty, " +
        "  methodParametersQty, " +
        "  methodParenthesizedExpsQty, " +
        "  methodReturnQty, " +
        "  methodRfc, " +
        "  methodStringLiteralsQty, " +
        "  methodSubClassesQty, " +
        "  methodTryCatchQty, " +
        "  methodUniqueWordsQty, " +
        "  methodVariablesQty, " +
        "  methodWmc, " +

        "  variableAppearances, " +

        "  authorOwnership, " +
        "  bugFixCount, " +
        "  linesAdded, " +
        "  linesDeleted, " +
        "  qtyMajorAuthors, " +
        "  qtyMinorAuthors, " +
        "  qtyOfAuthors, " +
        "  qtyOfCommits, " +
        "  refactoringsInvolved " +
        " " +
        "from refactoringcommit " +
        "where refactoring = '" + m_refactoring + "'"
    )

    if not dataset == "":
        sql = sql + " and project_id in (select id from project where datasetName = '" + dataset + "')"

    sql = sql + " order by refactoringDate"

    df = execute_query(sql)
    return df


def get_all_variable_level_refactorings(level, dataset = ""):
    sql = (
        "select refactoring, " +
        "  classAnonymousClassesQty, " +
        "  classAssignmentsQty, " +
        "  classCbo, " +
        "  classComparisonsQty, " +
        "  classLambdasQty, " +
        "  classLcom, " +
        "  classLoc, " +
        "  classLoopQty, " +
        "  classMathOperationsQty, " +
        "  classMaxNestedBlocks, " +
        "  classNosi, " +
        "  classNumberOfAbstractMethods, " +
        "  classNumberOfDefaultFields, " +
        "  classNumberOfDefaultMethods, " +
        "  classNumberOfFields, " +
        "  classNumberOfFinalFields, " +
        "  classNumberOfFinalMethods, " +
        "  classNumberOfMethods, " +
        "  classNumberOfPrivateFields, " +
        "  classNumberOfPrivateMethods, " +
        "  classNumberOfProtectedFields, " +
        "  classNumberOfProtectedMethods, " +
        "  classNumberOfPublicFields, " +
        "  classNumberOfPublicMethods, " +
        "  classNumberOfStaticFields, " +
        "  classNumberOfStaticMethods, " +
        "  classNumberOfSynchronizedFields, " +
        "  classNumberOfSynchronizedMethods, " +
        "  classNumbersQty, " +
        "  classParenthesizedExpsQty, " +
        "  classReturnQty, " +
        "  classRfc, " +
        "  classStringLiteralsQty, " +
        "  classSubClassesQty, " +
        "  classTryCatchQty, " +
        "  classUniqueWordsQty, " +
        "  classVariablesQty, " +
        "  classWmc, " +

        "  methodAnonymousClassesQty, " +
        "  methodAssignmentsQty, " +
        "  methodCbo, " +
        "  methodComparisonsQty, " +
        "  methodLambdasQty, " +
        "  methodLoc, " +
        "  methodLoopQty, " +
        "  methodMathOperationsQty, " +
        "  methodMaxNestedBlocks, " +
        "  methodNumbersQty, " +
        "  methodParametersQty, " +
        "  methodParenthesizedExpsQty, " +
        "  methodReturnQty, " +
        "  methodRfc, " +
        "  methodStringLiteralsQty, " +
        "  methodSubClassesQty, " +
        "  methodTryCatchQty, " +
        "  methodUniqueWordsQty, " +
        "  methodVariablesQty, " +
        "  methodWmc, " +

        "  variableAppearances, " +

        "  authorOwnership, " +
        "  bugFixCount, " +
        "  linesAdded, " +
        "  linesDeleted, " +
        "  qtyMajorAuthors, " +
        "  qtyMinorAuthors, " +
        "  qtyOfAuthors, " +
        "  qtyOfCommits, " +
        "  refactoringsInvolved " +
        " " +
        "from refactoringcommit " +
        "where refactoringLevel = " + level
    )

    if not dataset == "":
        sql = sql + " and project_id in (select id from project where datasetName = '" + dataset + "')"

    sql = sql + " order by refactoringDate"

    df = execute_query(sql)
    return df


def get_field_level_refactorings(m_refactoring, dataset = ""):
    sql = (
        "select  " +
        "  classAnonymousClassesQty, " +
        "  classAssignmentsQty, " +
        "  classCbo, " +
        "  classComparisonsQty, " +
        "  classLambdasQty, " +
        "  classLcom, " +
        "  classLoc, " +
        "  classLoopQty, " +
        "  classMathOperationsQty, " +
        "  classMaxNestedBlocks, " +
        "  classNosi, " +
        "  classNumberOfAbstractMethods, " +
        "  classNumberOfDefaultFields, " +
        "  classNumberOfDefaultMethods, " +
        "  classNumberOfFields, " +
        "  classNumberOfFinalFields, " +
        "  classNumberOfFinalMethods, " +
        "  classNumberOfMethods, " +
        "  classNumberOfPrivateFields, " +
        "  classNumberOfPrivateMethods, " +
        "  classNumberOfProtectedFields, " +
        "  classNumberOfProtectedMethods, " +
        "  classNumberOfPublicFields, " +
        "  classNumberOfPublicMethods, " +
        "  classNumberOfStaticFields, " +
        "  classNumberOfStaticMethods, " +
        "  classNumberOfSynchronizedFields, " +
        "  classNumberOfSynchronizedMethods, " +
        "  classNumbersQty, " +
        "  classParenthesizedExpsQty, " +
        "  classReturnQty, " +
        "  classRfc, " +
        "  classStringLiteralsQty, " +
        "  classSubClassesQty, " +
        "  classTryCatchQty, " +
        "  classUniqueWordsQty, " +
        "  classVariablesQty, " +
        "  classWmc, " +

        "  fieldAppearances, " +

        "  authorOwnership, " +
        "  bugFixCount, " +
        "  linesAdded, " +
        "  linesDeleted, " +
        "  qtyMajorAuthors, " +
        "  qtyMinorAuthors, " +
        "  qtyOfAuthors, " +
        "  qtyOfCommits, " +
        "  refactoringsInvolved " +
        " " +
        "from refactoringcommit " +
        "where refactoring = '" + m_refactoring + "'"
    )

    if not dataset == "":
        sql = sql + " and project_id in (select id from project where datasetName = '" + dataset + "')"

    sql = sql + " order by refactoringDate"

    df = execute_query(sql)
    return df


def get_all_field_level_refactorings(level, dataset = ""):
    sql = (
        "select refactoring, " +
        "  classAnonymousClassesQty, " +
        "  classAssignmentsQty, " +
        "  classCbo, " +
        "  classComparisonsQty, " +
        "  classLambdasQty, " +
        "  classLcom, " +
        "  classLoc, " +
        "  classLoopQty, " +
        "  classMathOperationsQty, " +
        "  classMaxNestedBlocks, " +
        "  classNosi, " +
        "  classNumberOfAbstractMethods, " +
        "  classNumberOfDefaultFields, " +
        "  classNumberOfDefaultMethods, " +
        "  classNumberOfFields, " +
        "  classNumberOfFinalFields, " +
        "  classNumberOfFinalMethods, " +
        "  classNumberOfMethods, " +
        "  classNumberOfPrivateFields, " +
        "  classNumberOfPrivateMethods, " +
        "  classNumberOfProtectedFields, " +
        "  classNumberOfProtectedMethods, " +
        "  classNumberOfPublicFields, " +
        "  classNumberOfPublicMethods, " +
        "  classNumberOfStaticFields, " +
        "  classNumberOfStaticMethods, " +
        "  classNumberOfSynchronizedFields, " +
        "  classNumberOfSynchronizedMethods, " +
        "  classNumbersQty, " +
        "  classParenthesizedExpsQty, " +
        "  classReturnQty, " +
        "  classRfc, " +
        "  classStringLiteralsQty, " +
        "  classSubClassesQty, " +
        "  classTryCatchQty, " +
        "  classUniqueWordsQty, " +
        "  classVariablesQty, " +
        "  classWmc, " +

        "  fieldAppearances, " +

        "  authorOwnership, " +
        "  bugFixCount, " +
        "  linesAdded, " +
        "  linesDeleted, " +
        "  qtyMajorAuthors, " +
        "  qtyMinorAuthors, " +
        "  qtyOfAuthors, " +
        "  qtyOfCommits, " +
        "  refactoringsInvolved " +
        " " +
        "from refactoringcommit " +
        "where refactoringLevel = " + level
    )

    if not dataset == "":
        sql = sql + " and project_id in (select id from project where datasetName = '" + dataset + "')"

    sql = sql + " order by refactoringDate"

    df = execute_query(sql)
    return df


# --------
# queries related to non refactored data
# --------
def get_non_refactored_methods(dataset = ""):
    sql = (
        "select  " +
        "  classAnonymousClassesQty, " +
        "  classAssignmentsQty, " +
        "  classCbo, " +
        "  classComparisonsQty, " +
        "  classLambdasQty, " +
        "  classLcom, " +
        "  classLoc, " +
        "  classLoopQty, " +
        "  classMathOperationsQty, " +
        "  classMaxNestedBlocks, " +
        "  classNosi, " +
        "  classNumberOfAbstractMethods, " +
        "  classNumberOfDefaultFields, " +
        "  classNumberOfDefaultMethods, " +
        "  classNumberOfFields, " +
        "  classNumberOfFinalFields, " +
        "  classNumberOfFinalMethods, " +
        "  classNumberOfMethods, " +
        "  classNumberOfPrivateFields, " +
        "  classNumberOfPrivateMethods, " +
        "  classNumberOfProtectedFields, " +
        "  classNumberOfProtectedMethods, " +
        "  classNumberOfPublicFields, " +
        "  classNumberOfPublicMethods, " +
        "  classNumberOfStaticFields, " +
        "  classNumberOfStaticMethods, " +
        "  classNumberOfSynchronizedFields, " +
        "  classNumberOfSynchronizedMethods, " +
        "  classNumbersQty, " +
        "  classParenthesizedExpsQty, " +
        "  classReturnQty, " +
        "  classRfc, " +
        "  classStringLiteralsQty, " +
        "  classSubClassesQty, " +
        "  classTryCatchQty, " +
        "  classUniqueWordsQty, " +
        "  classVariablesQty, " +
        "  classWmc, " +

        "  methodAnonymousClassesQty, " +
        "  methodAssignmentsQty, " +
        "  methodCbo, " +
        "  methodComparisonsQty, " +
        "  methodLambdasQty, " +
        "  methodLoc, " +
        "  methodLoopQty, " +
        "  methodMathOperationsQty, " +
        "  methodMaxNestedBlocks, " +
        "  methodNumbersQty, " +
        "  methodParametersQty, " +
        "  methodParenthesizedExpsQty, " +
        "  methodReturnQty, " +
        "  methodRfc, " +
        "  methodStringLiteralsQty, " +
        "  methodSubClassesQty, " +
        "  methodTryCatchQty, " +
        "  methodUniqueWordsQty, " +
        "  methodVariablesQty, " +
        "  methodWmc, " +

        "  authorOwnership, " +
        "  bugFixCount, " +
        "  linesAdded, " +
        "  linesDeleted, " +
        "  qtyMajorAuthors, " +
        "  qtyMinorAuthors, " +
        "  qtyOfAuthors, " +
        "  qtyOfCommits, " +
        "  refactoringsInvolved " +
        " " +
        "from stablecommit " +
        "where level = 2"
    )

    if not dataset == "":
        sql = sql + " and project_id in (select id from project where datasetName = '" + dataset + "')"

    sql = sql + " order by id"

    df = execute_query(sql)
    return df


def get_non_refactored_variables(dataset = ""):
    sql = (
        "select  " +
        "  classAnonymousClassesQty, " +
        "  classAssignmentsQty, " +
        "  classCbo, " +
        "  classComparisonsQty, " +
        "  classLambdasQty, " +
        "  classLcom, " +
        "  classLoc, " +
        "  classLoopQty, " +
        "  classMathOperationsQty, " +
        "  classMaxNestedBlocks, " +
        "  classNosi, " +
        "  classNumberOfAbstractMethods, " +
        "  classNumberOfDefaultFields, " +
        "  classNumberOfDefaultMethods, " +
        "  classNumberOfFields, " +
        "  classNumberOfFinalFields, " +
        "  classNumberOfFinalMethods, " +
        "  classNumberOfMethods, " +
        "  classNumberOfPrivateFields, " +
        "  classNumberOfPrivateMethods, " +
        "  classNumberOfProtectedFields, " +
        "  classNumberOfProtectedMethods, " +
        "  classNumberOfPublicFields, " +
        "  classNumberOfPublicMethods, " +
        "  classNumberOfStaticFields, " +
        "  classNumberOfStaticMethods, " +
        "  classNumberOfSynchronizedFields, " +
        "  classNumberOfSynchronizedMethods, " +
        "  classNumbersQty, " +
        "  classParenthesizedExpsQty, " +
        "  classReturnQty, " +
        "  classRfc, " +
        "  classStringLiteralsQty, " +
        "  classSubClassesQty, " +
        "  classTryCatchQty, " +
        "  classUniqueWordsQty, " +
        "  classVariablesQty, " +
        "  classWmc, " +

        "  methodAnonymousClassesQty, " +
        "  methodAssignmentsQty, " +
        "  methodCbo, " +
        "  methodComparisonsQty, " +
        "  methodLambdasQty, " +
        "  methodLoc, " +
        "  methodLoopQty, " +
        "  methodMathOperationsQty, " +
        "  methodMaxNestedBlocks, " +
        "  methodNumbersQty, " +
        "  methodParametersQty, " +
        "  methodParenthesizedExpsQty, " +
        "  methodReturnQty, " +
        "  methodRfc, " +
        "  methodStringLiteralsQty, " +
        "  methodSubClassesQty, " +
        "  methodTryCatchQty, " +
        "  methodUniqueWordsQty, " +
        "  methodVariablesQty, " +
        "  methodWmc, " +

        "  variableAppearances, " +

        "  authorOwnership, " +
        "  bugFixCount, " +
        "  linesAdded, " +
        "  linesDeleted, " +
        "  qtyMajorAuthors, " +
        "  qtyMinorAuthors, " +
        "  qtyOfAuthors, " +
        "  qtyOfCommits, " +
        "  refactoringsInvolved " +
        " " +
        "from stablecommit " +
        "where level = 3"
    )

    if not dataset == "":
        sql = sql + " and project_id in (select id from project where datasetName = '" + dataset + "')"

    sql = sql + " order by id"

    df = execute_query(sql)
    return df


# ----
# class-level refactorings
def get_non_refactored_classes(dataset = ""):
    sql = (
        "select  " +
        "  classAnonymousClassesQty, " +
        "  classAssignmentsQty, " +
        "  classCbo, " +
        "  classComparisonsQty, " +
        "  classLambdasQty, " +
        "  classLcom, " +
        "  classLoc, " +
        "  classLoopQty, " +
        "  classMathOperationsQty, " +
        "  classMaxNestedBlocks, " +
        "  classNosi, " +
        "  classNumberOfAbstractMethods, " +
        "  classNumberOfDefaultFields, " +
        "  classNumberOfDefaultMethods, " +
        "  classNumberOfFields, " +
        "  classNumberOfFinalFields, " +
        "  classNumberOfFinalMethods, " +
        "  classNumberOfMethods, " +
        "  classNumberOfPrivateFields, " +
        "  classNumberOfPrivateMethods, " +
        "  classNumberOfProtectedFields, " +
        "  classNumberOfProtectedMethods, " +
        "  classNumberOfPublicFields, " +
        "  classNumberOfPublicMethods, " +
        "  classNumberOfStaticFields, " +
        "  classNumberOfStaticMethods, " +
        "  classNumberOfSynchronizedFields, " +
        "  classNumberOfSynchronizedMethods, " +
        "  classNumbersQty, " +
        "  classParenthesizedExpsQty, " +
        "  classReturnQty, " +
        "  classRfc, " +
        "  classStringLiteralsQty, " +
        "  classSubClassesQty, " +
        "  classTryCatchQty, " +
        "  classUniqueWordsQty, " +
        "  classVariablesQty, " +
        "  classWmc, " +

        "  authorOwnership, " +
        "  bugFixCount, " +
        "  linesAdded, " +
        "  linesDeleted, " +
        "  qtyMajorAuthors, " +
        "  qtyMinorAuthors, " +
        "  qtyOfAuthors, " +
        "  qtyOfCommits, " +
        "  refactoringsInvolved " +
        " " +
        "from stablecommit " +
        "where level = 1"
    )

    if not dataset == "":
        sql = sql + " and project_id in (select id from project where datasetName = '" + dataset + "')"

    sql = sql + " order by id"

    df = execute_query(sql)
    return df


def get_non_refactored_fields(dataset = ""):
    sql = (
        "select  " +
        "  classAnonymousClassesQty, " +
        "  classAssignmentsQty, " +
        "  classCbo, " +
        "  classComparisonsQty, " +
        "  classLambdasQty, " +
        "  classLcom, " +
        "  classLoc, " +
        "  classLoopQty, " +
        "  classMathOperationsQty, " +
        "  classMaxNestedBlocks, " +
        "  classNosi, " +
        "  classNumberOfAbstractMethods, " +
        "  classNumberOfDefaultFields, " +
        "  classNumberOfDefaultMethods, " +
        "  classNumberOfFields, " +
        "  classNumberOfFinalFields, " +
        "  classNumberOfFinalMethods, " +
        "  classNumberOfMethods, " +
        "  classNumberOfPrivateFields, " +
        "  classNumberOfPrivateMethods, " +
        "  classNumberOfProtectedFields, " +
        "  classNumberOfProtectedMethods, " +
        "  classNumberOfPublicFields, " +
        "  classNumberOfPublicMethods, " +
        "  classNumberOfStaticFields, " +
        "  classNumberOfStaticMethods, " +
        "  classNumberOfSynchronizedFields, " +
        "  classNumberOfSynchronizedMethods, " +
        "  classNumbersQty, " +
        "  classParenthesizedExpsQty, " +
        "  classReturnQty, " +
        "  classRfc, " +
        "  classStringLiteralsQty, " +
        "  classSubClassesQty, " +
        "  classTryCatchQty, " +
        "  classUniqueWordsQty, " +
        "  classVariablesQty, " +
        "  classWmc, " +

        "  fieldAppearances, " +

        "  authorOwnership, " +
        "  bugFixCount, " +
        "  linesAdded, " +
        "  linesDeleted, " +
        "  qtyMajorAuthors, " +
        "  qtyMinorAuthors, " +
        "  qtyOfAuthors, " +
        "  qtyOfCommits, " +
        "  refactoringsInvolved " +
        " " +
        "from stablecommit " +
        "where level = 4"
    )

    if not dataset == "":
        sql = sql + " and project_id in (select id from project where datasetName = '" + dataset + "')"

    sql = sql + " order by id"

    df = execute_query(sql)
    return df
