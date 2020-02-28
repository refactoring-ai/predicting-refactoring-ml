package refactoringml;

import org.junit.Assert;
import org.junit.Test;
import refactoringml.db.CommitMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//Test the PMDatabase class
//Closely linked to the
public class PMTrackerDatabaseTest {
    @Test
    public void constructor(){
        Map<String, ProcessMetricTracker> database = new HashMap<>();
        PMTrackerDatabase pmTrackerDatabase = new PMTrackerDatabase(List.of(10, 25));

        String expected = "PMDatabase{" +
                "database=" + database.toString() + ",\n" +
                "commitThreshold=" + List.of(10, 25) +
                "}";
        Assert.assertEquals(expected, pmTrackerDatabase.toString());
    }

    //Test the case sensitivity of class fileNames
    //Be aware: .Java and .java are equal for java
    @Test
    public void caseSensitivity(){
        PMTrackerDatabase pmTrackerDatabase = new PMTrackerDatabase(List.of(10, 25));

        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#1", "n", "n", "0"), "R", 1, 1);
        pmTrackerDatabase.reportChanges("A.Java", new CommitMetaData("#1", "n", "n", "0"), "R", 1, 1);
        Assert.assertNotEquals(pmTrackerDatabase.find("a.Java"), pmTrackerDatabase.find("A.Java"));

        pmTrackerDatabase.reportChanges("a.java", new CommitMetaData("#2", "n", "n", "0"), "R", 1, 1);
        Assert.assertEquals(pmTrackerDatabase.find("a.Java"), pmTrackerDatabase.find("a.java"));
    }

    @Test
    public void reportChanges(){
        PMTrackerDatabase pmTrackerDatabase = new PMTrackerDatabase(List.of(10, 25));

        //test if a new pm tracker is created with the right values
        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmTrackerDatabase.find("a.Java"));
        Assert.assertEquals(1, pmTrackerDatabase.find("a.Java").getCommitCounter());
        Assert.assertNotNull(pmTrackerDatabase.find("a.Java").getBaseProcessMetrics());
        Assert.assertNotNull(pmTrackerDatabase.find("a.Java").getCurrentProcessMetrics());

        //test if another new pm tracker is created with the right values
        pmTrackerDatabase.reportChanges("A.Java", new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotEquals(pmTrackerDatabase.find("a.Java"), pmTrackerDatabase.find("A.Java"));
        Assert.assertNotNull(pmTrackerDatabase.find("A.Java"));
        Assert.assertEquals(1, pmTrackerDatabase.find("A.Java").getCommitCounter());
        Assert.assertNotNull(pmTrackerDatabase.find("A.Java").getBaseProcessMetrics());
        Assert.assertNotNull(pmTrackerDatabase.find("A.Java").getCurrentProcessMetrics());

        //test if an existing pmTracker is updated correctly
        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#2", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmTrackerDatabase.find("a.Java"));
        Assert.assertEquals(2, pmTrackerDatabase.find("a.Java").getCommitCounter());
        Assert.assertNotEquals(
                pmTrackerDatabase.find("a.Java").getBaseProcessMetrics().qtyOfCommits,
                pmTrackerDatabase.find("a.Java").getCurrentProcessMetrics().qtyOfCommits);
    }

    @Test
    public void reportRefactoring(){
        PMTrackerDatabase pmTrackerDatabase = new PMTrackerDatabase(List.of(10, 25));

        pmTrackerDatabase.reportRefactoring("a.Java", new CommitMetaData("#1", "null", "null", "0"));
        Assert.assertNotNull(pmTrackerDatabase.find("a.Java"));
        Assert.assertEquals(0, pmTrackerDatabase.find("a.Java").getCommitCounter());
        Assert.assertEquals(
                pmTrackerDatabase.find("a.Java").getCurrentProcessMetrics().toString(),
                pmTrackerDatabase.find("a.Java").getBaseProcessMetrics().toString());
        Assert.assertEquals(1,
                pmTrackerDatabase.find("a.Java").getCurrentProcessMetrics().refactoringsInvolved);
        Assert.assertEquals(1,
                pmTrackerDatabase.find("a.Java").getBaseProcessMetrics().refactoringsInvolved);

        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#2", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pmTrackerDatabase.find("a.Java").getCommitCounter());
        pmTrackerDatabase.reportChanges("A.Java", new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pmTrackerDatabase.find("a.Java").getCommitCounter());
        Assert.assertEquals(1, pmTrackerDatabase.find("A.Java").getCommitCounter());

        pmTrackerDatabase.reportRefactoring("a.Java", new CommitMetaData("#3", "null", "null", "0"));
        Assert.assertEquals(0, pmTrackerDatabase.find("a.Java").getCommitCounter());
        Assert.assertEquals(
                pmTrackerDatabase.find("a.Java").getCurrentProcessMetrics().toString(),
                pmTrackerDatabase.find("a.Java").getBaseProcessMetrics().toString());
        Assert.assertEquals(2,
                pmTrackerDatabase.find("a.Java").getCurrentProcessMetrics().refactoringsInvolved);
        Assert.assertEquals(2,
                pmTrackerDatabase.find("a.Java").getBaseProcessMetrics().refactoringsInvolved);
        Assert.assertEquals(1, pmTrackerDatabase.find("A.Java").getCommitCounter());
    }

    //take care of case sensitivity
    @Test
    public void removeFile1(){
        PMTrackerDatabase pmTrackerDatabase = new PMTrackerDatabase(List.of(10, 25));
        Map<String, ProcessMetricTracker> database = new HashMap<>();

        pmTrackerDatabase.removeFile("a.Java");
        String expected = "PMDatabase{" +
                "database=" + database.toString() + ",\n" +
                "commitThreshold=" + List.of(10, 25) +
                "}";
        Assert.assertEquals(expected, pmTrackerDatabase.toString());

        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        pmTrackerDatabase.removeFile("A.Java");
        Assert.assertNotNull(pmTrackerDatabase.find("a.Java"));

        pmTrackerDatabase.removeFile("a.Java");
        Assert.assertNull(pmTrackerDatabase.find("a.Java"));
    }

    //take care of case sensitivity
    @Test
    public void renameFile(){
        PMTrackerDatabase pmTrackerDatabase = new PMTrackerDatabase(List.of(10, 25));

        ProcessMetricTracker oldPMTracker = pmTrackerDatabase.renameFile("a.Java", "A.Java",
                new CommitMetaData("1", "null", "null", "0"));
        Assert.assertNull(oldPMTracker);
        Assert.assertNotNull(pmTrackerDatabase.find("A.Java"));
        Assert.assertNull(pmTrackerDatabase.find("a.Java"));
        Assert.assertEquals("A.Java", pmTrackerDatabase.find("A.Java").getFileName());

        pmTrackerDatabase.reportChanges("A.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmTrackerDatabase.find("A.Java"));
        Assert.assertEquals(1, pmTrackerDatabase.find("A.Java").getCommitCounter());
        Assert.assertEquals(0, pmTrackerDatabase.find("A.Java").getBaseProcessMetrics().linesAdded);
        Assert.assertEquals(10, pmTrackerDatabase.find("A.Java").getCurrentProcessMetrics().linesAdded);

        oldPMTracker = pmTrackerDatabase.renameFile("A.Java", "B.Java",
                new CommitMetaData("1", "null", "null", "0"));
        Assert.assertNotNull(oldPMTracker);
        Assert.assertEquals("A.Java", oldPMTracker.getFileName());
        Assert.assertEquals(1, oldPMTracker.getCommitCounter());
        Assert.assertEquals(0, oldPMTracker.getBaseProcessMetrics().linesAdded);
        Assert.assertEquals(10, oldPMTracker.getCurrentProcessMetrics().linesAdded);

        pmTrackerDatabase.reportChanges("B.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmTrackerDatabase.find("B.Java"));
        Assert.assertEquals("B.Java", pmTrackerDatabase.find("B.Java").getFileName());
        Assert.assertEquals(2, pmTrackerDatabase.find("B.Java").getCommitCounter());
        Assert.assertEquals(2, pmTrackerDatabase.find("B.Java").getCurrentProcessMetrics().qtyOfCommits);
        Assert.assertEquals(40, pmTrackerDatabase.find("B.Java").getCurrentProcessMetrics().linesDeleted);

        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmTrackerDatabase.find("a.Java"));
        Assert.assertEquals(1, pmTrackerDatabase.find("a.Java").getCommitCounter());
        Assert.assertEquals(1, pmTrackerDatabase.find("a.Java").getCurrentProcessMetrics().qtyOfCommits);
        Assert.assertEquals(20, pmTrackerDatabase.find("a.Java").getCurrentProcessMetrics().linesDeleted);
    }

    //take care of case sensitivity
    @Test
    public void renameFile2(){
        PMTrackerDatabase pmTrackerDatabase = new PMTrackerDatabase(List.of(10, 25));
        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        pmTrackerDatabase.renameFile("a.Java", "a.Java", new CommitMetaData("1", "null", "null", "0"));

        ProcessMetricTracker pmTracker = pmTrackerDatabase.find("a.Java");
        Assert.assertNotNull(pmTracker);
        Assert.assertEquals(10, pmTracker.getCurrentProcessMetrics().linesAdded);
        Assert.assertEquals(0, pmTracker.getBaseProcessMetrics().linesAdded);
        Assert.assertEquals("#1", pmTracker.getBaseCommitMetaData().getCommitId());
    }

    //take care of renamed files
    @Test
    public void removeFile2(){
        PMTrackerDatabase pmTrackerDatabase = new PMTrackerDatabase(List.of(10, 25));
        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        pmTrackerDatabase.renameFile("a.Java", "A.Java", new CommitMetaData("1", "null", "null", "0"));

        ProcessMetricTracker pmTracker = pmTrackerDatabase.removeFile("a.Java");
        Assert.assertNull(pmTracker);
        pmTracker = pmTrackerDatabase.removeFile("A.Java");
        Assert.assertNotNull(pmTracker);
    }

    @Test
    public void isStable1(){
        PMTrackerDatabase pm = new PMTrackerDatabase(List.of(10, 25));

        for(int i = 0; i < 9; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i), "null", "null", "0"), "Rafael", 10, 20);
            pm.reportChanges("b.Java", new CommitMetaData("#" + (i), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(0, pm.findStableInstances().size());
            Assert.assertEquals(i + 1, pm.find("a.Java").getCommitCounter());
            Assert.assertEquals(i + 1, pm.find("b.Java").getCommitCounter());
        }

        pm.reportChanges("b.Java", new CommitMetaData("#10", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(10, pm.find("b.Java").getCommitCounter());
        Assert.assertEquals(1, pm.findStableInstances().size());

        for(int i = 0; i < 14; i++) {
            pm.reportChanges("b.Java", new CommitMetaData("#" + (i + 11), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(0, pm.findStableInstances().size());
        }

        pm.reportChanges("a.Java", new CommitMetaData("#10", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pm.findStableInstances().size());
        Assert.assertEquals(10, pm.findStableInstances().get(0).getCommitCountThreshold());

        pm.reportChanges("b.Java", new CommitMetaData("#25", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(2, pm.findStableInstances().size());
        Assert.assertEquals(25, pm.findStableInstances().stream().filter(pmTracker -> pmTracker.getFileName().equals("b.Java")).collect(Collectors.toList()).get(0).getCommitCountThreshold());

        pm.reportRefactoring("b.Java", new CommitMetaData("#26", "null", "null", "0"));
        Assert.assertEquals(1, pm.findStableInstances().size());

        Assert.assertEquals(1, pm.findStableInstances().size());
        Assert.assertEquals(10, pm.findStableInstances().get(0).getCommitCountThreshold());
    }

    @Test
    public void isStable2(){
        PMTrackerDatabase pm = new PMTrackerDatabase(List.of(10, 25));
        for(int i = 0; i < 9; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i + 0), "null", "null", "0"), "Rafael", 10, 20);
            pm.reportChanges("b.Java", new CommitMetaData("#" + (i + 0), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(0, pm.findStableInstances().size());
        }

        pm.reportRefactoring("a.Java", new CommitMetaData("#" + (10), "null", "null", "0"));
        Assert.assertEquals(0, pm.find("a.Java").getCommitCounter());
        Assert.assertEquals(0, pm.findStableInstances().size());

        for(int i = 0; i < 2; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i + 10), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(0, pm.findStableInstances().size());
            Assert.assertEquals(i + 1, pm.find("a.Java").getCommitCounter());
        }

        pm.reportChanges("b.Java", new CommitMetaData("#" + (9), "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pm.findStableInstances().size());

        for(int i = 0; i < 7; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i + 12), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(1, pm.findStableInstances().size());
        }

        pm.reportChanges("a.Java", new CommitMetaData("#" + (20), "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(2, pm.findStableInstances().size());
    }

    @Test
    public void multipleKs1(){
        PMTrackerDatabase pm = new PMTrackerDatabase(List.of(10, 25));

        for(int i = 0; i < 9; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i), "null", "null", "0"), "Rafael", 10, 20);
        }
        pm.reportChanges("a.Java", new CommitMetaData("#" + (10), "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pm.findStableInstances().size());
        Assert.assertEquals(10, pm.findStableInstances().get(0).getCommitCountThreshold());

        pm.reportChanges("a.Java", new CommitMetaData("#" + (11), "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(0, pm.findStableInstances().size());

        for(int i = 0; i < 14; i++) {
            pm.reportChanges("a.Java", new CommitMetaData("#" + (i + 12), "null", "null", "0"), "Rafael", 10, 20);
        }
        Assert.assertEquals(1, pm.findStableInstances().size());
        Assert.assertEquals(25, pm.findStableInstances().get(0).getCommitCountThreshold());

        pm.reportRefactoring("a.Java", new CommitMetaData("#" + (26), "null", "null", "0"));
        Assert.assertEquals(0, pm.findStableInstances().size());
    }
}