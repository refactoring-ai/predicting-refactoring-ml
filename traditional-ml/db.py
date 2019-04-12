from db_utils import execute_query

not_test_expr = "not like '%/test/%'"

# get all the different types of refactoring we have in our entire dataset
# (**not** divided by project groups, e.g., apache, github)
def get_refactoring_types():
    sql = "SELECT refactoring, count(*) total from yes where lower(path) " + not_test_expr + " group by refactoring order by count(*)"
    df = execute_query(sql)
    return df


def get_class_level_refactorings_count():
    sql = "SELECT refactoring, count(*) total from yes where method = '' and variable = '' and lower(path) " + not_test_expr + " group by refactoring order by count(*)"
    df = execute_query(sql)
    return df

def get_method_level_refactorings_count():
    sql = "SELECT refactoring, count(*) total from yes where method <> '' and variable = '' and lower(path) " + not_test_expr + " group by refactoring order by count(*)"
    df = execute_query(sql)
    return df

def get_variable_level_refactorings_count():
    sql = "SELECT refactoring, count(*) total from yes where variable <> '' and lower(path) " + not_test_expr + " group by refactoring order by count(*)"
    df = execute_query(sql)
    return df


def get_method_level_refactorings(m_refactoring):
    sql = ("select distinct "+
    " yesmethod.cbo as method_cbo,"+
    " yesmethod.wmc as method_wmc,"+
    " yesmethod.rfc as method_rfc,"+
    " yesmethod.loc as method_loc,"+
    " yesmethod.returns as method_returns,"+
    " yesmethod.variables as method_variables,"+
    " yesmethod.parameters as method_parameters,"+
    " yesmethod.startLine as method_startLine,"+
    " yesmethod.loopQty as method_loopQty,"+
    " yesmethod.comparisonsQty as method_comparisonsQty,"+
    " yesmethod.tryCatchQty as method_tryCatchQty,"+
    " yesmethod.numbersQty as method_numbersQty,"+
    " yesmethod.assignmentsQty as method_assignmentsQty,"+
    " yesmethod.mathOperationsQty as method_mathOperationsQty,"+
    " yesmethod.subClassesQty as method_subClassesQty,"+
    " yesmethod.lambdasQty as method_lambdasQty,"+
    " yesmethod.uniqueWordsQty as method_uniqueWordsQty,"+
    " yesclass.cbo as class_cbo,"+
    " yesclass.wmc as class_wmc,"+
    " yesclass.rfc as class_rfc,"+
    " yesclass.lcom as class_lcom,"+
    " yesclass.totalMethods as class_totalMethods,"+
    " yesclass.staticMethods as class_staticMethods,"+
    " yesclass.publicMethods as class_publicMethods,"+
    " yesclass.privateMethods as class_privateMethods,"+
    " yesclass.protectedMethods as class_protectedMethods,"+
    " yesclass.defaultMethods as class_defaultMethods,"+
    " yesclass.abstractMethods as class_abstractMethods,"+
    " yesclass.finalMethods as class_finalMethods,"+
    " yesclass.synchronizedFields as class_synchronizedFields,"+
    " yesclass.totalFields as class_totalFields,"+
    " yesclass.staticFields as class_staticFields,"+
    " yesclass.publicFields as class_publicFields,"+
    " yesclass.privateFields as class_privateFields,"+
    " yesclass.protectedFields as class_protectedFields,"+
    " yesclass.defaultFields as class_defaultFields,"+
    " yesclass.finalFields as class_finalFields,"+
    " yesclass.nosi as class_nosi,"+
    " yesclass.loc as class_loc,"+
    " yesclass.returnQty as class_returnQty,"+
    " yesclass.loopQty as class_loopQty,"+
    " yesclass.comparisonsQty as class_comparisonsQty,"+
    " yesclass.tryCatchQty as class_tryCatchQty,"+
    " yesclass.parenthesizedExpsQty as class_parenthesizedExpsQty,"+
    " yesclass.stringLiteralsQty as class_stringLiteralsQty,"+
    " yesclass.numbersQty as class_numbersQty,"+
    " yesclass.assignmentsQty as class_assignmentsQty,"+
    " yesclass.mathOperationsQty as class_mathOperationsQty,"+
    " yesclass.variablesQty as class_variablesQty,"+
    " yesclass.maxNestedBlocks as class_maxNestedBlocks,"+
    " yesclass.anonymousClassesQty as class_anonymousClassesQty,"+
    " yesclass.subClassesQty as class_subClassesQty,"+
    " yesclass.lambdasQty as class_lambdasQty,"+
    " yesclass.uniqueWordsQty as class_uniqueWordsQty,"+
    " process.commits as process_commits,"+
    " process.linesAdded as process_linesAdded,"+
    " process.linesDeleted as process_linesDeleted,"+
    " process.authors as process_authors,"+
    " process.minorAuthors as process_minorAuthors,"+
    " process.majorAuthors as process_majorAuthors,"+
    " process.authorOwnership as process_authorOwnership,"+
    " process.bugs as process_bugs,"+
    " process.refactorings as process_refactorings"+
    " from yes " +
    " join yesclass on yes.dataset = yesclass.dataset and yes.project = yesclass.project and yes.class = yesclass.class and yes.parentCommit  = yesclass.parentCommit " +
    " join yesmethod on yes.method = yesmethod.method and yesclass.dataset = yesmethod.dataset and yesclass.project = yesmethod.project and yesclass.class = yesmethod.class and yesclass.parentCommit  = yesmethod.parentCommit " +
    " join process on process.dataset = yes.dataset and process.project = yes.project and process.file = yes.path and process.commit = yes.refactorCommit " +
    " where lower(yes.path) " + not_test_expr + " and yes.refactoring = '" + m_refactoring + "'")

    df = execute_query(sql)
    return df


def get_non_refactored_methods():
    sql = ("select distinct "+
    " nomethod.cbo as method_cbo,"+
    " nomethod.wmc as method_wmc,"+
    " nomethod.rfc as method_rfc,"+
    " nomethod.loc as method_loc,"+
    " nomethod.returns as method_returns,"+
    " nomethod.variables as method_variables,"+
    " nomethod.parameters as method_parameters,"+
    " nomethod.startLine as method_startLine,"+
    " nomethod.loopQty as method_loopQty,"+
    " nomethod.comparisonsQty as method_comparisonsQty,"+
    " nomethod.tryCatchQty as method_tryCatchQty,"+
    " nomethod.numbersQty as method_numbersQty,"+
    " nomethod.assignmentsQty as method_assignmentsQty,"+
    " nomethod.mathOperationsQty as method_mathOperationsQty,"+
    " nomethod.subClassesQty as method_subClassesQty,"+
    " nomethod.lambdasQty as method_lambdasQty,"+
    " nomethod.uniqueWordsQty as method_uniqueWordsQty,"+
    " noclass.cbo as class_cbo,"+
    " noclass.wmc as class_wmc,"+
    " noclass.rfc as class_rfc,"+
    " noclass.lcom as class_lcom,"+
    " noclass.totalMethods as class_totalMethods,"+
    " noclass.staticMethods as class_staticMethods,"+
    " noclass.publicMethods as class_publicMethods,"+
    " noclass.privateMethods as class_privateMethods,"+
    " noclass.protectedMethods as class_protectedMethods,"+
    " noclass.defaultMethods as class_defaultMethods,"+
    " noclass.abstractMethods as class_abstractMethods,"+
    " noclass.finalMethods as class_finalMethods,"+
    " noclass.synchronizedFields as class_synchronizedFields,"+
    " noclass.totalFields as class_totalFields,"+
    " noclass.staticFields as class_staticFields,"+
    " noclass.publicFields as class_publicFields,"+
    " noclass.privateFields as class_privateFields,"+
    " noclass.protectedFields as class_protectedFields,"+
    " noclass.defaultFields as class_defaultFields,"+
    " noclass.finalFields as class_finalFields,"+
    " noclass.nosi as class_nosi,"+
    " noclass.loc as class_loc,"+
    " noclass.returnQty as class_returnQty,"+
    " noclass.loopQty as class_loopQty,"+
    " noclass.comparisonsQty as class_comparisonsQty,"+
    " noclass.tryCatchQty as class_tryCatchQty,"+
    " noclass.parenthesizedExpsQty as class_parenthesizedExpsQty,"+
    " noclass.stringLiteralsQty as class_stringLiteralsQty,"+
    " noclass.numbersQty as class_numbersQty,"+
    " noclass.assignmentsQty as class_assignmentsQty,"+
    " noclass.mathOperationsQty as class_mathOperationsQty,"+
    " noclass.variablesQty as class_variablesQty,"+
    " noclass.maxNestedBlocks as class_maxNestedBlocks,"+
    " noclass.anonymousClassesQty as class_anonymousClassesQty,"+
    " noclass.subClassesQty as class_subClassesQty,"+
    " noclass.lambdasQty as class_lambdasQty,"+
    " noclass.uniqueWordsQty as class_uniqueWordsQty,"+
    " process.commits as process_commits,"+
    " process.linesAdded as process_linesAdded,"+
    " process.linesDeleted as process_linesDeleted,"+
    " process.authors as process_authors,"+
    " process.minorAuthors as process_minorAuthors,"+
    " process.majorAuthors as process_majorAuthors,"+
    " process.authorOwnership as process_authorOwnership,"+
    " process.bugs as process_bugs,"+
    " process.refactorings as process_refactorings"+
    " from nomethod as nomethod"+
    " join noclass on "+
    " noclass.dataset = nomethod.dataset"+
    " and noclass.project = nomethod.project"+
    " and noclass.class = nomethod.class "+
    " and noclass.commit = nomethod.commit"+
    " join process on "+
    " process.dataset = nomethod.dataset"+
    " and process.project = nomethod.project"+
    " and process.file = nomethod.path"+
    " and process.commit  = nomethod.commit" +
    " where lower(noclass.path) " + not_test_expr
    )

    return execute_query(sql)



# -- variable level queries
def get_variable_level_refactorings(m_refactoring):
    sql = ("select distinct "+
    " yesvariable.qty as variable_qty," +
    " CHAR_LENGTH(yesvariable.qty) as variable_length," +
    " yesmethod.cbo as method_cbo,"+
    " yesmethod.wmc as method_wmc,"+
    " yesmethod.rfc as method_rfc,"+
    " yesmethod.loc as method_loc,"+
    " yesmethod.returns as method_returns,"+
    " yesmethod.variables as method_variables,"+
    " yesmethod.parameters as method_parameters,"+
    " yesmethod.startLine as method_startLine,"+
    " yesmethod.loopQty as method_loopQty,"+
    " yesmethod.comparisonsQty as method_comparisonsQty,"+
    " yesmethod.tryCatchQty as method_tryCatchQty,"+
    " yesmethod.numbersQty as method_numbersQty,"+
    " yesmethod.assignmentsQty as method_assignmentsQty,"+
    " yesmethod.mathOperationsQty as method_mathOperationsQty,"+
    " yesmethod.subClassesQty as method_subClassesQty,"+
    " yesmethod.lambdasQty as method_lambdasQty,"+
    " yesmethod.uniqueWordsQty as method_uniqueWordsQty,"+
    " yesclass.cbo as class_cbo,"+
    " yesclass.wmc as class_wmc,"+
    " yesclass.rfc as class_rfc,"+
    " yesclass.lcom as class_lcom,"+
    " yesclass.totalMethods as class_totalMethods,"+
    " yesclass.staticMethods as class_staticMethods,"+
    " yesclass.publicMethods as class_publicMethods,"+
    " yesclass.privateMethods as class_privateMethods,"+
    " yesclass.protectedMethods as class_protectedMethods,"+
    " yesclass.defaultMethods as class_defaultMethods,"+
    " yesclass.abstractMethods as class_abstractMethods,"+
    " yesclass.finalMethods as class_finalMethods,"+
    " yesclass.synchronizedFields as class_synchronizedFields,"+
    " yesclass.totalFields as class_totalFields,"+
    " yesclass.staticFields as class_staticFields,"+
    " yesclass.publicFields as class_publicFields,"+
    " yesclass.privateFields as class_privateFields,"+
    " yesclass.protectedFields as class_protectedFields,"+
    " yesclass.defaultFields as class_defaultFields,"+
    " yesclass.finalFields as class_finalFields,"+
    " yesclass.nosi as class_nosi,"+
    " yesclass.loc as class_loc,"+
    " yesclass.returnQty as class_returnQty,"+
    " yesclass.loopQty as class_loopQty,"+
    " yesclass.comparisonsQty as class_comparisonsQty,"+
    " yesclass.tryCatchQty as class_tryCatchQty,"+
    " yesclass.parenthesizedExpsQty as class_parenthesizedExpsQty,"+
    " yesclass.stringLiteralsQty as class_stringLiteralsQty,"+
    " yesclass.numbersQty as class_numbersQty,"+
    " yesclass.assignmentsQty as class_assignmentsQty,"+
    " yesclass.mathOperationsQty as class_mathOperationsQty,"+
    " yesclass.variablesQty as class_variablesQty,"+
    " yesclass.maxNestedBlocks as class_maxNestedBlocks,"+
    " yesclass.anonymousClassesQty as class_anonymousClassesQty,"+
    " yesclass.subClassesQty as class_subClassesQty,"+
    " yesclass.lambdasQty as class_lambdasQty,"+
    " yesclass.uniqueWordsQty as class_uniqueWordsQty,"+
    " process.commits as process_commits,"+
    " process.linesAdded as process_linesAdded,"+
    " process.linesDeleted as process_linesDeleted,"+
    " process.authors as process_authors,"+
    " process.minorAuthors as process_minorAuthors,"+
    " process.majorAuthors as process_majorAuthors,"+
    " process.authorOwnership as process_authorOwnership,"+
    " process.bugs as process_bugs,"+
    " process.refactorings as process_refactorings"+
    " from yes " +
    " join yesclass on yes.dataset = yesclass.dataset and yes.project = yesclass.project and yes.class = yesclass.class and yes.parentCommit  = yesclass.parentCommit " +
    " join yesmethod on yes.method = yesmethod.method and yes.dataset = yesmethod.dataset and yes.project = yesmethod.project and yes.class = yesmethod.class and yes.parentCommit  = yesmethod.parentCommit " +
    " join yesvariable on yes.variable = yesvariable.variable and yes.method = yesvariable.method and yes.dataset = yesvariable.dataset and yes.project = yesvariable.project and yes.class = yesvariable.class and yes.parentCommit  = yesvariable.parentCommit " +
    " join process on process.dataset = yes.dataset and process.project = yes.project and process.file = yes.path and process.commit = yes.refactorCommit " +
    " where lower(yes.path) " + not_test_expr + " and yes.refactoring = '" + m_refactoring + "'")

    df = execute_query(sql)
    return df


def get_non_refactored_variables():
    sql = ("select distinct "+
    " novariable.qty as variable_qty," +
    " CHAR_LENGTH(novariable.qty) as variable_length," +
    " nomethod.cbo as method_cbo,"+
    " nomethod.wmc as method_wmc,"+
    " nomethod.rfc as method_rfc,"+
    " nomethod.loc as method_loc,"+
    " nomethod.returns as method_returns,"+
    " nomethod.variables as method_variables,"+
    " nomethod.parameters as method_parameters,"+
    " nomethod.startLine as method_startLine,"+
    " nomethod.loopQty as method_loopQty,"+
    " nomethod.comparisonsQty as method_comparisonsQty,"+
    " nomethod.tryCatchQty as method_tryCatchQty,"+
    " nomethod.numbersQty as method_numbersQty,"+
    " nomethod.assignmentsQty as method_assignmentsQty,"+
    " nomethod.mathOperationsQty as method_mathOperationsQty,"+
    " nomethod.subClassesQty as method_subClassesQty,"+
    " nomethod.lambdasQty as method_lambdasQty,"+
    " nomethod.uniqueWordsQty as method_uniqueWordsQty,"+
    " noclass.cbo as class_cbo,"+
    " noclass.wmc as class_wmc,"+
    " noclass.rfc as class_rfc,"+
    " noclass.lcom as class_lcom,"+
    " noclass.totalMethods as class_totalMethods,"+
    " noclass.staticMethods as class_staticMethods,"+
    " noclass.publicMethods as class_publicMethods,"+
    " noclass.privateMethods as class_privateMethods,"+
    " noclass.protectedMethods as class_protectedMethods,"+
    " noclass.defaultMethods as class_defaultMethods,"+
    " noclass.abstractMethods as class_abstractMethods,"+
    " noclass.finalMethods as class_finalMethods,"+
    " noclass.synchronizedFields as class_synchronizedFields,"+
    " noclass.totalFields as class_totalFields,"+
    " noclass.staticFields as class_staticFields,"+
    " noclass.publicFields as class_publicFields,"+
    " noclass.privateFields as class_privateFields,"+
    " noclass.protectedFields as class_protectedFields,"+
    " noclass.defaultFields as class_defaultFields,"+
    " noclass.finalFields as class_finalFields,"+
    " noclass.nosi as class_nosi,"+
    " noclass.loc as class_loc,"+
    " noclass.returnQty as class_returnQty,"+
    " noclass.loopQty as class_loopQty,"+
    " noclass.comparisonsQty as class_comparisonsQty,"+
    " noclass.tryCatchQty as class_tryCatchQty,"+
    " noclass.parenthesizedExpsQty as class_parenthesizedExpsQty,"+
    " noclass.stringLiteralsQty as class_stringLiteralsQty,"+
    " noclass.numbersQty as class_numbersQty,"+
    " noclass.assignmentsQty as class_assignmentsQty,"+
    " noclass.mathOperationsQty as class_mathOperationsQty,"+
    " noclass.variablesQty as class_variablesQty,"+
    " noclass.maxNestedBlocks as class_maxNestedBlocks,"+
    " noclass.anonymousClassesQty as class_anonymousClassesQty,"+
    " noclass.subClassesQty as class_subClassesQty,"+
    " noclass.lambdasQty as class_lambdasQty,"+
    " noclass.uniqueWordsQty as class_uniqueWordsQty,"+
    " process.commits as process_commits,"+
    " process.linesAdded as process_linesAdded,"+
    " process.linesDeleted as process_linesDeleted,"+
    " process.authors as process_authors,"+
    " process.minorAuthors as process_minorAuthors,"+
    " process.majorAuthors as process_majorAuthors,"+
    " process.authorOwnership as process_authorOwnership,"+
    " process.bugs as process_bugs,"+
    " process.refactorings as process_refactorings"+
    " from nomethod as nomethod"+
    " join noclass on "+
    " noclass.dataset = nomethod.dataset"+
    " and noclass.project = nomethod.project"+
    " and noclass.class = nomethod.class "+
    " and noclass.commit = nomethod.commit"+
    " join process on "+
    " process.dataset = nomethod.dataset"+
    " and process.project = nomethod.project"+
    " and process.file = nomethod.path"+
    " and process.commit  = nomethod.commit" +
    " join novariable on noclass.dataset = novariable.dataset and noclass.project = novariable.project and noclass.class = novariable.class and noclass.commit = novariable.commit and nomethod.method = novariable.method"
    " where lower(noclass.path) " + not_test_expr
    )

    return execute_query(sql)


# ----
# class-level refactorings


def get_class_level_refactorings(m_refactoring):
    sql = ("select distinct " +
           " yesclass.cbo as class_cbo," +
           " yesclass.wmc as class_wmc," +
           " yesclass.rfc as class_rfc," +
           " yesclass.lcom as class_lcom," +
           " yesclass.totalMethods as class_totalMethods," +
           " yesclass.staticMethods as class_staticMethods," +
           " yesclass.publicMethods as class_publicMethods," +
           " yesclass.privateMethods as class_privateMethods," +
           " yesclass.protectedMethods as class_protectedMethods," +
           " yesclass.defaultMethods as class_defaultMethods," +
           " yesclass.abstractMethods as class_abstractMethods," +
           " yesclass.finalMethods as class_finalMethods," +
           " yesclass.synchronizedFields as class_synchronizedFields," +
           " yesclass.totalFields as class_totalFields," +
           " yesclass.staticFields as class_staticFields," +
           " yesclass.publicFields as class_publicFields," +
           " yesclass.privateFields as class_privateFields," +
           " yesclass.protectedFields as class_protectedFields," +
           " yesclass.defaultFields as class_defaultFields," +
           " yesclass.finalFields as class_finalFields," +
           " yesclass.nosi as class_nosi," +
           " yesclass.loc as class_loc," +
           " yesclass.returnQty as class_returnQty," +
           " yesclass.loopQty as class_loopQty," +
           " yesclass.comparisonsQty as class_comparisonsQty," +
           " yesclass.tryCatchQty as class_tryCatchQty," +
           " yesclass.parenthesizedExpsQty as class_parenthesizedExpsQty," +
           " yesclass.stringLiteralsQty as class_stringLiteralsQty," +
           " yesclass.numbersQty as class_numbersQty," +
           " yesclass.assignmentsQty as class_assignmentsQty," +
           " yesclass.mathOperationsQty as class_mathOperationsQty," +
           " yesclass.variablesQty as class_variablesQty," +
           " yesclass.maxNestedBlocks as class_maxNestedBlocks," +
           " yesclass.anonymousClassesQty as class_anonymousClassesQty," +
           " yesclass.subClassesQty as class_subClassesQty," +
           " yesclass.lambdasQty as class_lambdasQty," +
           " yesclass.uniqueWordsQty as class_uniqueWordsQty," +
           " process.commits as process_commits," +
           " process.linesAdded as process_linesAdded," +
           " process.linesDeleted as process_linesDeleted," +
           " process.authors as process_authors," +
           " process.minorAuthors as process_minorAuthors," +
           " process.majorAuthors as process_majorAuthors," +
           " process.authorOwnership as process_authorOwnership," +
           " process.bugs as process_bugs," +
           " process.refactorings as process_refactorings" +
           " from yes " +
           " join yesclass on yes.dataset = yesclass.dataset and yes.project = yesclass.project and yes.class = yesclass.class and yes.parentCommit  = yesclass.parentCommit " +
           " join process on process.dataset = yes.dataset and process.project = yes.project and process.file = yes.path and process.commit = yes.refactorCommit " +
           " where lower(yes.path) " + not_test_expr + " and yes.refactoring = '" + m_refactoring + "'")

    df = execute_query(sql)
    return df


def get_non_refactored_classes():
    sql = ("select distinct " +
           " noclass.cbo as class_cbo," +
           " noclass.wmc as class_wmc," +
           " noclass.rfc as class_rfc," +
           " noclass.lcom as class_lcom," +
           " noclass.totalMethods as class_totalMethods," +
           " noclass.staticMethods as class_staticMethods," +
           " noclass.publicMethods as class_publicMethods," +
           " noclass.privateMethods as class_privateMethods," +
           " noclass.protectedMethods as class_protectedMethods," +
           " noclass.defaultMethods as class_defaultMethods," +
           " noclass.abstractMethods as class_abstractMethods," +
           " noclass.finalMethods as class_finalMethods," +
           " noclass.synchronizedFields as class_synchronizedFields," +
           " noclass.totalFields as class_totalFields," +
           " noclass.staticFields as class_staticFields," +
           " noclass.publicFields as class_publicFields," +
           " noclass.privateFields as class_privateFields," +
           " noclass.protectedFields as class_protectedFields," +
           " noclass.defaultFields as class_defaultFields," +
           " noclass.finalFields as class_finalFields," +
           " noclass.nosi as class_nosi," +
           " noclass.loc as class_loc," +
           " noclass.returnQty as class_returnQty," +
           " noclass.loopQty as class_loopQty," +
           " noclass.comparisonsQty as class_comparisonsQty," +
           " noclass.tryCatchQty as class_tryCatchQty," +
           " noclass.parenthesizedExpsQty as class_parenthesizedExpsQty," +
           " noclass.stringLiteralsQty as class_stringLiteralsQty," +
           " noclass.numbersQty as class_numbersQty," +
           " noclass.assignmentsQty as class_assignmentsQty," +
           " noclass.mathOperationsQty as class_mathOperationsQty," +
           " noclass.variablesQty as class_variablesQty," +
           " noclass.maxNestedBlocks as class_maxNestedBlocks," +
           " noclass.anonymousClassesQty as class_anonymousClassesQty," +
           " noclass.subClassesQty as class_subClassesQty," +
           " noclass.lambdasQty as class_lambdasQty," +
           " noclass.uniqueWordsQty as class_uniqueWordsQty," +
           " process.commits as process_commits," +
           " process.linesAdded as process_linesAdded," +
           " process.linesDeleted as process_linesDeleted," +
           " process.authors as process_authors," +
           " process.minorAuthors as process_minorAuthors," +
           " process.majorAuthors as process_majorAuthors," +
           " process.authorOwnership as process_authorOwnership," +
           " process.bugs as process_bugs," +
           " process.refactorings as process_refactorings" +
           " from noclass" +
           " join process on " +
           " process.dataset = noclass.dataset" +
           " and process.project = noclass.project" +
           " and process.file = noclass.path" +
           " and process.commit  = noclass.commit" +
           " where lower(noclass.path) " + not_test_expr
           )

    return execute_query(sql)