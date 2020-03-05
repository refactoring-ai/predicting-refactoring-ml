import unittest
from db.DatabaseLoader import project_filter, join_tables, get_metrics_level, get_instance_fields, \
    get_refactoring_levels, get_level_refactorings_count, get_all_level_refactorings, get_all_level_stable, \
    get_level_refactorings


class DatabaseLoaderUtilsTest(unittest.TestCase):
    def test_project_filter(self):
        sqlExpected: str = "refactoringcommit.project_id in (select id from project where datasetName = \"integration-test\")"
        sqlBuilt: str = project_filter("refactoringcommit", "integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)

        sqlExpected: str = "stablecommit.project_id in (select id from project where datasetName = \"integration-test\")"
        sqlBuilt: str = project_filter("stablecommit", "integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)


    def test_join_tables(self):
        self.assertEqual("refactoringcommit.commitmetadata_id = commitmetadata.id", join_tables("refactoringcommit", "commitmetadata"))
        self.assertEqual("refactoringcommit.processmetrics_id = processmetrics.id", join_tables("refactoringcommit", "processmetrics"))
        self.assertEqual("stablecommit.project_id = project.id", join_tables("stablecommit", "project"))


    def test_get_metrics_level(self):
        self.assertEqual(2, len(get_metrics_level(1)))
        self.assertEqual(3, len(get_metrics_level(2)))
        self.assertEqual(4, len(get_metrics_level(3)))
        self.assertEqual(3, len(get_metrics_level(4)))
        self.assertNotEqual(get_metrics_level(2), get_metrics_level(4))


    def test_get_instance_fields(self):
        sqlExpected: str = "SELECT refactoringcommit.className, commitmetadata.commitId FROM refactoringcommit, commitmetadata WHERE refactoringcommit.commitmetadata_id = commitmetadata.id "
        sqlBuilt: str = get_instance_fields("refactoringcommit",
                                            [("refactoringcommit", ["className"]), ("commitmetadata", ["commitId"])])
        self.assertEqual(sqlExpected, sqlBuilt)

        sqlExpected += "AND commitmetadata.parentCommitId != null "
        sqlBuilt: str = get_instance_fields("refactoringcommit",
                                            [("refactoringcommit", ["className"]), ("commitmetadata", ["commitId"])],
                                            "commitmetadata.parentCommitId != null")

        sqlExpected += "AND refactoringcommit.project_id in (select id from project where datasetName = \"integration-test\") "
        sqlBuilt: str = get_instance_fields("refactoringcommit",
                                            [("refactoringcommit", ["className"]), ("commitmetadata", ["commitId"])],
                                            "commitmetadata.parentCommitId != null",
                                            "integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)

        sqlExpected += "order by commitmetadata.commitDate"
        sqlBuilt: str = get_instance_fields("refactoringcommit",
                                            [("refactoringcommit", ["className"]), ("commitmetadata", ["commitId"])],
                                            "commitmetadata.parentCommitId != null",
                                            "integration-test",
                                            "order by commitmetadata.commitDate")
        self.assertEqual(sqlExpected, sqlBuilt)


    def test_get_refactoring_levels(self):
        sqlExpected: str = "SELECT refactoring, count(*) total from refactoringcommit where " \
                           "refactoringcommit.project_id in (select id from project where datasetName = \"integration-test\") " \
                           "group by refactoring order by count(*) desc"
        sqlBuilt: str = get_refactoring_levels("integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)


    def test_get_level_refactorings(self):
        sqlExpected: str = "SELECT refactoringcommit.className, refactoringcommit.filePath, refactoringcommit.isTest, refactoringcommit.level, refactoringcommit.refactoring, refactoringcommit.refactoringSummary, commitmetadata.commitDate, classmetric.classAnonymousClassesQty, classmetric.classAssignmentsQty, classmetric.classCbo, classmetric.classComparisonsQty, classmetric.classLambdasQty, classmetric.classLcom, classmetric.classLoc, classmetric.classLoopQty, classmetric.classMathOperationsQty, classmetric.classMaxNestedBlocks, classmetric.classNosi, classmetric.classNumberOfAbstractMethods, classmetric.classNumberOfDefaultFields, classmetric.classNumberOfDefaultMethods, classmetric.classNumberOfFields, classmetric.classNumberOfFinalFields, classmetric.classNumberOfFinalMethods, classmetric.classNumberOfMethods, classmetric.classNumberOfPrivateFields, classmetric.classNumberOfPrivateMethods, classmetric.classNumberOfProtectedFields, classmetric.classNumberOfProtectedMethods, classmetric.classNumberOfPublicFields, classmetric.classNumberOfPublicMethods, classmetric.classNumberOfStaticFields, classmetric.classNumberOfStaticMethods, classmetric.classNumberOfSynchronizedFields, classmetric.classNumberOfSynchronizedMethods, classmetric.classNumbersQty, classmetric.classParenthesizedExpsQty, classmetric.classReturnQty, classmetric.classRfc, classmetric.classStringLiteralsQty, classmetric.classSubClassesQty, classmetric.classTryCatchQty, classmetric.classUniqueWordsQty, classmetric.classVariablesQty, classmetric.classWmc, classmetric.isInnerClass, processmetrics.authorOwnership, processmetrics.bugFixCount, processmetrics.linesAdded, processmetrics.linesDeleted, processmetrics.qtyMajorAuthors, processmetrics.qtyMinorAuthors, processmetrics.qtyOfAuthors, processmetrics.qtyOfCommits, processmetrics.refactoringsInvolved FROM refactoringcommit, commitmetadata, classmetric, processmetrics WHERE refactoringcommit.commitmetadata_id = commitmetadata.id AND refactoringcommit.classmetrics_id = classmetric.id AND refactoringcommit.processmetrics_id = processmetrics.id AND refactoringcommit.level = 1 AND refactoringcommit.refactoring = \"Change Parameter Type\" AND refactoringcommit.project_id in (select id from project where datasetName = \"integration-test\")  order by commitmetadata.commitDate"
        sqlBuilt: str = get_level_refactorings(1, "Change Parameter Type", "integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)


    def test_get_all_level_refactorings(self):
        sqlExpected: str = "SELECT refactoringcommit.className, refactoringcommit.filePath, refactoringcommit.isTest, refactoringcommit.level, refactoringcommit.refactoring, refactoringcommit.refactoringSummary, commitmetadata.commitDate, classmetric.classAnonymousClassesQty, classmetric.classAssignmentsQty, classmetric.classCbo, classmetric.classComparisonsQty, classmetric.classLambdasQty, classmetric.classLcom, classmetric.classLoc, classmetric.classLoopQty, classmetric.classMathOperationsQty, classmetric.classMaxNestedBlocks, classmetric.classNosi, classmetric.classNumberOfAbstractMethods, classmetric.classNumberOfDefaultFields, classmetric.classNumberOfDefaultMethods, classmetric.classNumberOfFields, classmetric.classNumberOfFinalFields, classmetric.classNumberOfFinalMethods, classmetric.classNumberOfMethods, classmetric.classNumberOfPrivateFields, classmetric.classNumberOfPrivateMethods, classmetric.classNumberOfProtectedFields, classmetric.classNumberOfProtectedMethods, classmetric.classNumberOfPublicFields, classmetric.classNumberOfPublicMethods, classmetric.classNumberOfStaticFields, classmetric.classNumberOfStaticMethods, classmetric.classNumberOfSynchronizedFields, classmetric.classNumberOfSynchronizedMethods, classmetric.classNumbersQty, classmetric.classParenthesizedExpsQty, classmetric.classReturnQty, classmetric.classRfc, classmetric.classStringLiteralsQty, classmetric.classSubClassesQty, classmetric.classTryCatchQty, classmetric.classUniqueWordsQty, classmetric.classVariablesQty, classmetric.classWmc, classmetric.isInnerClass, processmetrics.authorOwnership, processmetrics.bugFixCount, processmetrics.linesAdded, processmetrics.linesDeleted, processmetrics.qtyMajorAuthors, processmetrics.qtyMinorAuthors, processmetrics.qtyOfAuthors, processmetrics.qtyOfCommits, processmetrics.refactoringsInvolved FROM refactoringcommit, commitmetadata, classmetric, processmetrics WHERE refactoringcommit.commitmetadata_id = commitmetadata.id AND refactoringcommit.classmetrics_id = classmetric.id AND refactoringcommit.processmetrics_id = processmetrics.id AND refactoringcommit.level = 1 AND refactoringcommit.project_id in (select id from project where datasetName = \"integration-test\")  order by commitmetadata.commitDate"
        sqlBuilt: str = get_all_level_refactorings(1, "integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)


    def test_get_all_level_stable(self):
        sqlExpected: str = "SELECT stablecommit.className, stablecommit.filePath, stablecommit.isTest, stablecommit.level, commitmetadata.commitDate, classmetric.classAnonymousClassesQty, classmetric.classAssignmentsQty, classmetric.classCbo, classmetric.classComparisonsQty, classmetric.classLambdasQty, classmetric.classLcom, classmetric.classLoc, classmetric.classLoopQty, classmetric.classMathOperationsQty, classmetric.classMaxNestedBlocks, classmetric.classNosi, classmetric.classNumberOfAbstractMethods, classmetric.classNumberOfDefaultFields, classmetric.classNumberOfDefaultMethods, classmetric.classNumberOfFields, classmetric.classNumberOfFinalFields, classmetric.classNumberOfFinalMethods, classmetric.classNumberOfMethods, classmetric.classNumberOfPrivateFields, classmetric.classNumberOfPrivateMethods, classmetric.classNumberOfProtectedFields, classmetric.classNumberOfProtectedMethods, classmetric.classNumberOfPublicFields, classmetric.classNumberOfPublicMethods, classmetric.classNumberOfStaticFields, classmetric.classNumberOfStaticMethods, classmetric.classNumberOfSynchronizedFields, classmetric.classNumberOfSynchronizedMethods, classmetric.classNumbersQty, classmetric.classParenthesizedExpsQty, classmetric.classReturnQty, classmetric.classRfc, classmetric.classStringLiteralsQty, classmetric.classSubClassesQty, classmetric.classTryCatchQty, classmetric.classUniqueWordsQty, classmetric.classVariablesQty, classmetric.classWmc, classmetric.isInnerClass, processmetrics.authorOwnership, processmetrics.bugFixCount, processmetrics.linesAdded, processmetrics.linesDeleted, processmetrics.qtyMajorAuthors, processmetrics.qtyMinorAuthors, processmetrics.qtyOfAuthors, processmetrics.qtyOfCommits, processmetrics.refactoringsInvolved FROM stablecommit, commitmetadata, classmetric, processmetrics WHERE stablecommit.commitmetadata_id = commitmetadata.id AND stablecommit.classmetrics_id = classmetric.id AND stablecommit.processmetrics_id = processmetrics.id AND stablecommit.level = 1 AND stablecommit.project_id in (select id from project where datasetName = \"integration-test\")  order by commitmetadata.commitDate"
        sqlBuilt: str = get_all_level_stable(1, "integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)


    # TODO: write test
    def test_get_level_refactorings_count(self):
        sqlExpected: str = ""
        sqlBuilt: str = get_level_refactorings_count(2, "integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)

        get_instance_fields(refactoringCommits, [(refactoringCommits, refactoringCommitFields), (comm)] +
                            get_metrics_level(level), refactoringCommits + ".level = " + str(level), dataset) + \
        " group by refactoring order by count(*) desc"


if __name__ == '__main__':
    unittest.main()
