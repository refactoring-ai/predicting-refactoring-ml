package refactoringml;

import org.junit.Assert;
import org.junit.Test;
import refactoringml.db.CommitMetaData;
import java.util.HashMap;
import java.util.Map;

//Test the PMDatabase class
//Closely linked to the
public class PMDatabaseTest {
    @Test
    public void constructor(){
        Map<String, ProcessMetricTracker> database = new HashMap<>();
        PMDatabase pmDatabase = new PMDatabase(10);

        String expected = "PMDatabase{" +
                "database=" + database.toString() + ",\n" +
                "commitThreshold=" + 10 +
                "}";
        Assert.assertEquals(expected, pmDatabase.toString());
    }

    //Test the case sensitivity of class fileNames
    //Be aware: .Java and .java are equal for java
    @Test
    public void caseSensitivity(){
        PMDatabase pmDatabase = new PMDatabase(10);

        pmDatabase.reportChanges("a.Java", new CommitMetaData("1", "n", "n", "0"), "R", 1, 1);
        pmDatabase.reportChanges("A.Java", new CommitMetaData("1", "n", "n", "0"), "R", 1, 1);
        Assert.assertNotEquals(pmDatabase.find("a.Java"), pmDatabase.find("A.Java"));

        pmDatabase.reportChanges("a.java", new CommitMetaData("1", "n", "n", "0"), "R", 1, 1);
        Assert.assertEquals(pmDatabase.find("a.Java"), pmDatabase.find("a.java"));
    }

    @Test
    public void reportChanges(){
        PMDatabase pmDatabase = new PMDatabase(10);

        //test if a new pm tracker is created with the right values
        pmDatabase.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmDatabase.find("a.Java"));
        Assert.assertEquals(1, pmDatabase.find("a.Java").getCommitCounter());
        Assert.assertNotNull(pmDatabase.find("a.Java").getBaseProcessMetrics());
        Assert.assertNotNull(pmDatabase.find("a.Java").getCurrentProcessMetrics());

        //test if another new pm tracker is created with the right values
        pmDatabase.reportChanges("A.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotEquals(pmDatabase.find("a.Java"), pmDatabase.find("A.Java"));
        Assert.assertNotNull(pmDatabase.find("A.Java"));
        Assert.assertEquals(1, pmDatabase.find("A.Java").getCommitCounter());
        Assert.assertNotNull(pmDatabase.find("A.Java").getBaseProcessMetrics());
        Assert.assertNotNull(pmDatabase.find("A.Java").getCurrentProcessMetrics());

        //test if an existing pmTracker is updated correctly
        pmDatabase.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmDatabase.find("a.Java"));
        Assert.assertEquals(2, pmDatabase.find("a.Java").getCommitCounter());
        Assert.assertNotEquals(
                pmDatabase.find("a.Java").getBaseProcessMetrics().qtyOfCommits,
                pmDatabase.find("a.Java").getCurrentProcessMetrics().qtyOfCommits);
    }

    @Test
    public void reportRefactoring(){
        PMDatabase pmDatabase = new PMDatabase(10);

        pmDatabase.reportRefactoring("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmDatabase.find("a.Java"));
        Assert.assertEquals(0, pmDatabase.find("a.Java").getCommitCounter());
        Assert.assertEquals(
                pmDatabase.find("a.Java").getCurrentProcessMetrics().toString(),
                pmDatabase.find("a.Java").getBaseProcessMetrics().toString());
        Assert.assertEquals(1,
                pmDatabase.find("a.Java").getCurrentProcessMetrics().refactoringsInvolved);
        Assert.assertEquals(1,
                pmDatabase.find("a.Java").getBaseProcessMetrics().refactoringsInvolved);

        pmDatabase.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pmDatabase.find("a.Java").getCommitCounter());
        pmDatabase.reportChanges("A.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pmDatabase.find("a.Java").getCommitCounter());
        Assert.assertEquals(1, pmDatabase.find("A.Java").getCommitCounter());

        pmDatabase.reportRefactoring("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(0, pmDatabase.find("a.Java").getCommitCounter());
        Assert.assertEquals(
                pmDatabase.find("a.Java").getCurrentProcessMetrics().toString(),
                pmDatabase.find("a.Java").getBaseProcessMetrics().toString());
        Assert.assertEquals(2,
                pmDatabase.find("a.Java").getCurrentProcessMetrics().refactoringsInvolved);
        Assert.assertEquals(2,
                pmDatabase.find("a.Java").getBaseProcessMetrics().refactoringsInvolved);
        Assert.assertEquals(1, pmDatabase.find("A.Java").getCommitCounter());
    }

    //take care of case sensitivity
    @Test
    public void removeFile1(){
        PMDatabase pmDatabase = new PMDatabase(10);
        Map<String, ProcessMetricTracker> database = new HashMap<>();

        pmDatabase.removeFile("a.Java");
        String expected = "PMDatabase{" +
                "database=" + database.toString() + ",\n" +
                "commitThreshold=" + 10 +
                "}";
        Assert.assertEquals(expected, pmDatabase.toString());

        pmDatabase.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        pmDatabase.removeFile("A.Java");
        Assert.assertNotNull(pmDatabase.find("a.Java"));

        pmDatabase.removeFile("a.Java");
        Assert.assertNull(pmDatabase.find("a.Java"));
    }

    //take care of case sensitivity
    @Test
    public void renameFile(){
        PMDatabase pmDatabase = new PMDatabase(10);

        ProcessMetricTracker oldPMTracker = pmDatabase.renameFile("a.Java", "A.Java",
                new CommitMetaData("1", "null", "null", "0"));
        Assert.assertNull(oldPMTracker);
        Assert.assertNotNull(pmDatabase.find("A.Java"));
        Assert.assertNull(pmDatabase.find("a.Java"));

        pmDatabase.reportChanges("A.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmDatabase.find("A.Java"));
        Assert.assertEquals(1, pmDatabase.find("A.Java").getCommitCounter());
        Assert.assertEquals(0, pmDatabase.find("A.Java").getBaseProcessMetrics().linesAdded);
        Assert.assertEquals(10, pmDatabase.find("A.Java").getCurrentProcessMetrics().linesAdded);

        oldPMTracker = pmDatabase.renameFile("A.Java", "B.Java",
                new CommitMetaData("1", "null", "null", "0"));
        Assert.assertNotNull(oldPMTracker);
        Assert.assertEquals(1, oldPMTracker.getCommitCounter());
        Assert.assertEquals(0, oldPMTracker.getBaseProcessMetrics().linesAdded);
        Assert.assertEquals(10, oldPMTracker.getCurrentProcessMetrics().linesAdded);

        pmDatabase.reportChanges("B.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmDatabase.find("B.Java"));
        Assert.assertEquals(2, pmDatabase.find("B.Java").getCommitCounter());
        Assert.assertEquals(2, pmDatabase.find("B.Java").getCurrentProcessMetrics().qtyOfCommits);
        Assert.assertEquals(40, pmDatabase.find("B.Java").getCurrentProcessMetrics().linesDeleted);

        pmDatabase.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmDatabase.find("a.Java"));
        Assert.assertEquals(1, pmDatabase.find("a.Java").getCommitCounter());
        Assert.assertEquals(1, pmDatabase.find("a.Java").getCurrentProcessMetrics().qtyOfCommits);
        Assert.assertEquals(20, pmDatabase.find("a.Java").getCurrentProcessMetrics().linesDeleted);
    }

    //take care of case sensitivity
    @Test
    public void renameFile2(){
        PMDatabase pmDatabase = new PMDatabase(10);
        pmDatabase.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        pmDatabase.renameFile("a.Java", "a.Java", new CommitMetaData("1", "null", "null", "0"));

        ProcessMetricTracker pmTracker = pmDatabase.find("a.Java");
        Assert.assertNotNull(pmTracker);
        Assert.assertEquals(10, pmTracker.getCurrentProcessMetrics().linesAdded);
        Assert.assertEquals(0, pmTracker.getBaseProcessMetrics().linesAdded);
        Assert.assertEquals("1", pmTracker.getBaseCommitMetaData().getCommit());
    }

    //take care of renamed files
    @Test
    public void removeFile2(){
        PMDatabase pmDatabase = new PMDatabase(10);
        pmDatabase.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        pmDatabase.renameFile("a.Java", "A.Java", new CommitMetaData("1", "null", "null", "0"));

        ProcessMetricTracker pmTracker = pmDatabase.removeFile("a.Java");
        Assert.assertNull(pmTracker);
        pmTracker = pmDatabase.removeFile("A.Java");
        Assert.assertNotNull(pmTracker);
    }

    @Test
    public void isStable1(){
        PMDatabase pm = new PMDatabase(10);

        for(int i = 0; i < 9; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
            pm.reportChanges("b.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(0, pm.findStableInstances().size());
            Assert.assertEquals(i + 1, pm.find("a.Java").getCommitCounter());
            Assert.assertEquals(i + 1, pm.find("b.Java").getCommitCounter());
        }

        pm.reportChanges("b.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(10, pm.find("b.Java").getCommitCounter());
        Assert.assertEquals(1, pm.findStableInstances().size());

        for(int i = 0; i < 12; i++) {
            pm.reportChanges("b.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(1, pm.findStableInstances().size());
        }

        pm.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(2, pm.findStableInstances().size());
    }

    @Test
    public void isStable2(){
        PMDatabase pm = new PMDatabase(10);
        for(int i = 0; i < 9; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
            pm.reportChanges("b.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(0, pm.findStableInstances().size());
        }

        pm.reportRefactoring("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(0, pm.find("a.Java").getCommitCounter());
        Assert.assertEquals(0, pm.findStableInstances().size());

        for(int i = 0; i < 2; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(0, pm.findStableInstances().size());
            Assert.assertEquals(i + 1, pm.find("a.Java").getCommitCounter());
        }

        pm.reportChanges("b.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pm.findStableInstances().size());

        for(int i = 0; i < 7; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(1, pm.findStableInstances().size());
        }

        pm.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(2, pm.findStableInstances().size());
    }
}