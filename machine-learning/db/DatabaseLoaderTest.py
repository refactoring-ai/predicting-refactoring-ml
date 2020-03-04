import unittest
from db.DatabaseLoader import project_filter, join_tables


class DatabaseLoaderUtilsTest(unittest.TestCase):
    def test_project_filter(self):
        self.assertEqual(project_filter("refactoringcommit", "test-project"),
                         "refactoringcommit.project_id in (select id from project where datasetName = test-project")
        self.assertEqual(project_filter("stablecommit", "test-project"),
                         "stablecommit.project_id in (select id from project where datasetName = test-project")

    def test_join_tables(self):
        self.assertEqual(join_tables("refactoringcommit", "commitmetadata"),
                         "refactoringcommit.commitmetadata_id = commitmetadata.id")
        self.assertEqual(join_tables("refactoringcommit", "processmetrics"),
                         "refactoringcommit.processmetrics_id = processmetrics.id")
        self.assertEqual(join_tables("stablecommit", "project"),
                         "refactoringcommit.project_id = project.id")

    def test_get_metrics_level(self):
        self.assertEqual(True, False)

    def test_get_instance_fields(self):
        self.assertEqual(True, False)


if __name__ == '__main__':
    unittest.main()
