from db_utils import execute_query

# get all the different types of refactoring we have in our entire dataset
# (**not** divided by project groups, e.g., apache, github)
def get_refactoring_types():
    sql = "SELECT refactoring, count(*) total from yes group by refactoring order by count(*)"
    df = execute_query(sql)
    return df


def get_class_level_refactorings_count():
    sql = "SELECT refactoring, count(*) total from yes where refactoringType = 1 group by refactoring order by count(*)"
    df = execute_query(sql)
    return df

def get_method_level_refactorings_count():
    sql = "SELECT refactoring, count(*) total from yes where refactoringType = 2 group by refactoring order by count(*)"
    df = execute_query(sql)
    return df

def get_variable_level_refactorings_count():
    sql = "SELECT refactoring, count(*) total from yes where refactoringType = 3 group by refactoring order by count(*)"
    df = execute_query(sql)
    return df

def get_field_level_refactorings_count():
    sql = "SELECT refactoring, count(*) total from yes where refactoringType = 4 group by refactoring order by count(*)"
    df = execute_query(sql)
    return df

def get_method_level_refactorings(m_refactoring):
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
        "from yes " +
        "where refactoring = '" + m_refactoring + "'"
    )

    df = execute_query(sql)
    return df


def get_class_level_refactorings(m_refactoring):
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
            "from yes " +
            "where refactoring = '" + m_refactoring + "'"
            )

    df = execute_query(sql)
    return df


def get_variable_level_refactorings(m_refactoring):
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
        "from yes " +
        "where refactoring = '" + m_refactoring + "'"
    )

    df = execute_query(sql)
    return df


def get_field_level_refactorings(m_refactoring):
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
        "from yes " +
        "where refactoring = '" + m_refactoring + "'"
    )

    df = execute_query(sql)
    return df



# --------
# queries related to non refactored data
# --------

def get_non_refactored_methods():
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
        "from no " +
        "where type = 2"
    )

    df = execute_query(sql)
    return df





def get_non_refactored_variables():
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
        "from no " +
        "where type = 3"
    )

    df = execute_query(sql)
    return df


# ----
# class-level refactorings




def get_non_refactored_classes():
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
        "from no " +
        "where type = 1"
    )

    df = execute_query(sql)
    return df



def get_non_refactored_fields():
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
        "from no " +
        "where type = 4"
    )

    df = execute_query(sql)
    return df
