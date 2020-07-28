import unittest
from db.QueryBuilder import project_filter, join_table, get_metrics_level, get_instance_fields, \
    get_refactoring_levels, get_level_refactorings_count, get_all_level_refactorings, get_all_level_stable, \
    get_level_refactorings, get_refactoring_types


class QueryBuilderUnitTest(unittest.TestCase):
    def test_project_filter(self):
        sqlExpected: str = "refactoringcommit.project_id in (select id from project where datasetName = \"integration-test\")"
        sqlBuilt: str = project_filter("refactoringcommit", "integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)

        sqlExpected: str = "stablecommit.project_id in (select id from project where datasetName = \"integration-test\")"
        sqlBuilt: str = project_filter("stablecommit", "integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)


    def test_join_tables(self):
        self.assertEqual(" INNER JOIN commitmetadata ON refactoringcommit.commitmetadata_id = commitmetadata.id",
                         join_table("refactoringcommit", "commitmetadata"))
        self.assertEqual(" INNER JOIN processmetrics ON refactoringcommit.processmetrics_id = processmetrics.id",
                         join_table("refactoringcommit", "processmetrics"))
        self.assertEqual(" INNER JOIN project ON stablecommit.project_id = project.id", join_table("stablecommit", "project"))


    def test_get_metrics_level(self):
        self.assertEqual(2, len(get_metrics_level(1)))
        self.assertEqual(3, len(get_metrics_level(2)))
        self.assertEqual(4, len(get_metrics_level(3)))
        self.assertEqual(3, len(get_metrics_level(4)))
        self.assertNotEqual(get_metrics_level(2), get_metrics_level(4))


    def test_get_instance_fields(self):
        sqlExpected: str = "SELECT refactoringcommit.className, commitmetadata.commitId FROM refactoringcommit INNER JOIN commitmetadata ON refactoringcommit.commitmetadata_id = commitmetadata.id"
        sqlBuilt: str = get_instance_fields("refactoringcommit",
                                            [("refactoringcommit", ["className"]), ("commitmetadata", ["commitId"])])
        self.assertEqual(sqlExpected, sqlBuilt)

        sqlExpected += " WHERE commitmetadata.parentCommitId != null"
        sqlBuilt: str = get_instance_fields("refactoringcommit",
                                            [("refactoringcommit", ["className"]), ("commitmetadata", ["commitId"])],
                                            "commitmetadata.parentCommitId != null")
        self.assertEqual(sqlExpected, sqlBuilt)

        sqlExpected += " AND refactoringcommit.project_id in (select id from project where datasetName = \"integration-test\")"
        sqlBuilt: str = get_instance_fields("refactoringcommit",
                                            [("refactoringcommit", ["className"]), ("commitmetadata", ["commitId"])],
                                            "commitmetadata.parentCommitId != null",
                                            "integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)

        sqlExpected += " order by commitmetadata.commitDate"
        sqlBuilt: str = get_instance_fields("refactoringcommit",
                                            [("refactoringcommit", ["className"]), ("commitmetadata", ["commitId"])],
                                            "commitmetadata.parentCommitId != null",
                                            "integration-test",
                                            "order by commitmetadata.commitDate")
        self.assertEqual(sqlExpected, sqlBuilt)


    def test_get_refactoring_levels(self):
        sqlExpected: str = "SELECT refactoring, count(*) total from refactoringcommit where " \
                           "refactoringcommit.project_id in (select id from project where datasetName = \"integration-test\") AND refactoringcommit.isValid = TRUE " \
                           "group by refactoring order by count(*) desc"
        sqlBuilt: str = get_refactoring_levels("integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)


    def test_get_level_refactorings(self):
        sqlExpected: str = "SELECT classmetric.classAnonymousClassesQty, classmetric.classAssignmentsQty, classmetric.classCbo, classmetric.classComparisonsQty, classmetric.classLambdasQty, classmetric.classLcom, classmetric.classLoc, classmetric.classLoopQty, classmetric.classMathOperationsQty, classmetric.classMaxNestedBlocks, classmetric.classNosi, classmetric.classNumberOfAbstractMethods, classmetric.classNumberOfDefaultFields, classmetric.classNumberOfDefaultMethods, classmetric.classNumberOfFields, classmetric.classNumberOfFinalFields, classmetric.classNumberOfFinalMethods, classmetric.classNumberOfMethods, classmetric.classNumberOfPrivateFields, classmetric.classNumberOfPrivateMethods, classmetric.classNumberOfProtectedFields, classmetric.classNumberOfProtectedMethods, classmetric.classNumberOfPublicFields, classmetric.classNumberOfPublicMethods, classmetric.classNumberOfStaticFields, classmetric.classNumberOfStaticMethods, classmetric.classNumberOfSynchronizedFields, classmetric.classNumberOfSynchronizedMethods, classmetric.classNumbersQty, classmetric.classParenthesizedExpsQty, classmetric.classReturnQty, classmetric.classRfc, classmetric.classStringLiteralsQty, classmetric.classSubClassesQty, classmetric.classTryCatchQty, classmetric.classUniqueWordsQty, classmetric.classVariablesQty, classmetric.classWmc, classmetric.isInnerClass, processmetrics.authorOwnership, processmetrics.bugFixCount, processmetrics.qtyMajorAuthors, processmetrics.qtyMinorAuthors, processmetrics.qtyOfAuthors, processmetrics.qtyOfCommits, processmetrics.refactoringsInvolved FROM refactoringcommit INNER JOIN commitmetadata ON refactoringcommit.commitmetadata_id = commitmetadata.id INNER JOIN classmetric ON refactoringcommit.classmetrics_id = classmetric.id INNER JOIN processmetrics ON refactoringcommit.processmetrics_id = processmetrics.id WHERE refactoringcommit.level = 1 AND refactoringcommit.isValid = TRUE AND refactoringcommit.refactoring = \"Change Parameter Type\" AND refactoringcommit.project_id in (select id from project where datasetName = \"integration-test\")  order by commitmetadata.commitDate"
        sqlBuilt: str = get_level_refactorings(1, "Change Parameter Type", "integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)


    def test_get_all_level_refactorings(self):
        sqlExpected: str = "SELECT classmetric.classAnonymousClassesQty, classmetric.classAssignmentsQty, classmetric.classCbo, classmetric.classComparisonsQty, classmetric.classLambdasQty, classmetric.classLcom, classmetric.classLoc, classmetric.classLoopQty, classmetric.classMathOperationsQty, classmetric.classMaxNestedBlocks, classmetric.classNosi, classmetric.classNumberOfAbstractMethods, classmetric.classNumberOfDefaultFields, classmetric.classNumberOfDefaultMethods, classmetric.classNumberOfFields, classmetric.classNumberOfFinalFields, classmetric.classNumberOfFinalMethods, classmetric.classNumberOfMethods, classmetric.classNumberOfPrivateFields, classmetric.classNumberOfPrivateMethods, classmetric.classNumberOfProtectedFields, classmetric.classNumberOfProtectedMethods, classmetric.classNumberOfPublicFields, classmetric.classNumberOfPublicMethods, classmetric.classNumberOfStaticFields, classmetric.classNumberOfStaticMethods, classmetric.classNumberOfSynchronizedFields, classmetric.classNumberOfSynchronizedMethods, classmetric.classNumbersQty, classmetric.classParenthesizedExpsQty, classmetric.classReturnQty, classmetric.classRfc, classmetric.classStringLiteralsQty, classmetric.classSubClassesQty, classmetric.classTryCatchQty, classmetric.classUniqueWordsQty, classmetric.classVariablesQty, classmetric.classWmc, classmetric.isInnerClass, processmetrics.authorOwnership, processmetrics.bugFixCount, processmetrics.qtyMajorAuthors, processmetrics.qtyMinorAuthors, processmetrics.qtyOfAuthors, processmetrics.qtyOfCommits, processmetrics.refactoringsInvolved FROM refactoringcommit INNER JOIN commitmetadata ON refactoringcommit.commitmetadata_id = commitmetadata.id INNER JOIN classmetric ON refactoringcommit.classmetrics_id = classmetric.id INNER JOIN processmetrics ON refactoringcommit.processmetrics_id = processmetrics.id WHERE refactoringcommit.level = 1 AND refactoringcommit.isValid = TRUE AND refactoringcommit.project_id in (select id from project where datasetName = \"integration-test\")  order by commitmetadata.commitDate"
        sqlBuilt: str = get_all_level_refactorings(1, "integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)


    def test_get_all_level_stable(self):
        sqlExpected: str = "SELECT classmetric.classAnonymousClassesQty, classmetric.classAssignmentsQty, classmetric.classCbo, classmetric.classComparisonsQty, classmetric.classLambdasQty, classmetric.classLcom, classmetric.classLoc, classmetric.classLoopQty, classmetric.classMathOperationsQty, classmetric.classMaxNestedBlocks, classmetric.classNosi, classmetric.classNumberOfAbstractMethods, classmetric.classNumberOfDefaultFields, classmetric.classNumberOfDefaultMethods, classmetric.classNumberOfFields, classmetric.classNumberOfFinalFields, classmetric.classNumberOfFinalMethods, classmetric.classNumberOfMethods, classmetric.classNumberOfPrivateFields, classmetric.classNumberOfPrivateMethods, classmetric.classNumberOfProtectedFields, classmetric.classNumberOfProtectedMethods, classmetric.classNumberOfPublicFields, classmetric.classNumberOfPublicMethods, classmetric.classNumberOfStaticFields, classmetric.classNumberOfStaticMethods, classmetric.classNumberOfSynchronizedFields, classmetric.classNumberOfSynchronizedMethods, classmetric.classNumbersQty, classmetric.classParenthesizedExpsQty, classmetric.classReturnQty, classmetric.classRfc, classmetric.classStringLiteralsQty, classmetric.classSubClassesQty, classmetric.classTryCatchQty, classmetric.classUniqueWordsQty, classmetric.classVariablesQty, classmetric.classWmc, classmetric.isInnerClass, processmetrics.authorOwnership, processmetrics.bugFixCount, processmetrics.qtyMajorAuthors, processmetrics.qtyMinorAuthors, processmetrics.qtyOfAuthors, processmetrics.qtyOfCommits, processmetrics.refactoringsInvolved FROM stablecommit INNER JOIN commitmetadata ON stablecommit.commitmetadata_id = commitmetadata.id INNER JOIN classmetric ON stablecommit.classmetrics_id = classmetric.id INNER JOIN processmetrics ON stablecommit.processmetrics_id = processmetrics.id WHERE stablecommit.level = 1 AND stablecommit.project_id in (select id from project where datasetName = \"integration-test\")  order by commitmetadata.commitDate"
        sqlBuilt: str = get_all_level_stable(1, "integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)


    def test_get_level_refactorings_count(self):
        sqlExpected: str = "SELECT refactoring, count(*) FROM (SELECT refactoringcommit.refactoring FROM refactoringcommit WHERE refactoringcommit.level = 2 AND refactoringcommit.project_id in (select id from project where datasetName = \"integration-test\") AND refactoringcommit.isValid = TRUE) t group by refactoring order by count(*) desc"
        sqlBuilt: str = get_level_refactorings_count(2, "integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)


    def test_get_refactoring_types(self):
        sqlExpected: str = "SELECT DISTINCT refactoring FROM (SELECT refactoringcommit.refactoring FROM refactoringcommit WHERE refactoringcommit.project_id in (select id from project where datasetName = \"integration-test\")) t"
        sqlBuilt: str = get_refactoring_types("integration-test")
        self.assertEqual(sqlExpected, sqlBuilt)

if __name__ == '__main__':
    unittest.main()
