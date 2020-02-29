package refactoringml;

import org.hibernate.exception.SQLGrammarException;
import org.junit.*;
import refactoringml.db.CommitMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

//Test the PMDatabase class
//Closely linked to the
public class PMTrackerDatabaseTest {
    private PMTrackerDatabase pmTrackerDatabase;

    @Before
    public void initPMDB(){
        pmTrackerDatabase = new PMTrackerDatabase(List.of(10, 25));
        pmTrackerDatabase.db.openSession();
    }

    @After
    public void cleanPMDB(){
        pmTrackerDatabase.destroy();
    }

    @Test
    @Ignore
    public void constructor(){
        Map<String, ProcessMetricTracker> database = new HashMap<>();

        String expected = "PMDatabase{" +
                "database=" + database.toString() + ",\n" +
                "commitThreshold=" + List.of(10, 25) +
                "}";
        Assert.assertEquals(expected, pmTrackerDatabase.toString());
    }

    @Test
    public void dropTable(){
        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#1", "n", "n", "0"), "R", 1, 1);
        pmTrackerDatabase.db.drop("ProcessMetricTracker");

        try{
            pmTrackerDatabase.find("a.Java");
        } catch (SQLGrammarException ex){
            Assert.assertEquals("could not extract ResultSet", ex.getMessage());
        }
        //open a new session, for cleanPMDB()
        pmTrackerDatabase.db.openSession();
    }

    //Test the case sensitivity of class fileNames
    //Be aware: .Java and .java are equal for java
    @Test
    public void caseSensitivity1(){
        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#1", "n", "n", "0"), "R", 1, 1);
        pmTrackerDatabase.reportChanges("A.Java", new CommitMetaData("#1", "n", "n", "0"), "R", 1, 1);
        Assert.assertNotEquals(pmTrackerDatabase.find("a.Java"), pmTrackerDatabase.find("A.Java"));
    }

    //Test the case sensitivity of class fileNames
    //Be aware: .Java and .java are equal for java
    @Test
    public void caseSensitivity2(){
        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#1", "n", "n", "0"), "R", 1, 1);
        pmTrackerDatabase.reportChanges("A.Java", new CommitMetaData("#1", "n", "n", "0"), "R", 1, 1);
        Assert.assertNotEquals(pmTrackerDatabase.find("a.Java"), pmTrackerDatabase.find("A.Java"));

        pmTrackerDatabase.reportChanges("a.java", new CommitMetaData("#2", "n", "n", "0"), "R", 1, 1);
        Assert.assertEquals(pmTrackerDatabase.find("a.Java"), pmTrackerDatabase.find("a.java"));
    }


    @Test
    public void reportChanges(){
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
        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        pmTrackerDatabase.removeFile("A.Java");
        Assert.assertNotNull(pmTrackerDatabase.find("a.Java"));

        pmTrackerDatabase.removeFile("a.Java");
        Assert.assertNull(pmTrackerDatabase.find("a.Java"));
    }

    //take care of case sensitivity
    @Test
    public void renameFile(){
        ProcessMetricTracker oldPMTracker = pmTrackerDatabase.renameFile("a.Java", "A.Java");
        Assert.assertNull(oldPMTracker);
        Assert.assertNull(pmTrackerDatabase.find("A.Java"));
        Assert.assertNull(pmTrackerDatabase.find("a.Java"));

        pmTrackerDatabase.reportChanges("A.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNull(pmTrackerDatabase.find("a.Java"));
        Assert.assertNotNull(pmTrackerDatabase.find("A.Java"));
        Assert.assertEquals(1, pmTrackerDatabase.find("A.Java").getCommitCounter());
        Assert.assertEquals(0, pmTrackerDatabase.find("A.Java").getBaseProcessMetrics().linesAdded);
        Assert.assertEquals(10, pmTrackerDatabase.find("A.Java").getCurrentProcessMetrics().linesAdded);

        oldPMTracker = pmTrackerDatabase.renameFile("A.Java", "B.Java");
        Assert.assertNotNull(oldPMTracker);
        Assert.assertNull(pmTrackerDatabase.find("A.Java"));
        Assert.assertNull(pmTrackerDatabase.find("a.Java"));
        Assert.assertEquals("A.Java", oldPMTracker.getFileName());
        Assert.assertEquals(1, oldPMTracker.getCommitCounter());
        Assert.assertEquals(0, oldPMTracker.getBaseProcessMetrics().linesAdded);
        Assert.assertEquals(10, oldPMTracker.getCurrentProcessMetrics().linesAdded);

        pmTrackerDatabase.reportChanges("B.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmTrackerDatabase.find("B.Java"));
        Assert.assertNull(pmTrackerDatabase.find("a.Java"));
        Assert.assertEquals("B.Java", pmTrackerDatabase.find("B.Java").getFileName());
        Assert.assertEquals(2, pmTrackerDatabase.find("B.Java").getCommitCounter());
        Assert.assertEquals(2, pmTrackerDatabase.find("B.Java").getCurrentProcessMetrics().qtyOfCommits);
        Assert.assertEquals(40, pmTrackerDatabase.find("B.Java").getCurrentProcessMetrics().linesDeleted);

        Assert.assertNull(pmTrackerDatabase.find("a.Java"));
        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("1", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertNotNull(pmTrackerDatabase.find("a.Java"));
        Assert.assertEquals(1, pmTrackerDatabase.find("a.Java").getCommitCounter());
        Assert.assertEquals(1, pmTrackerDatabase.find("a.Java").getCurrentProcessMetrics().qtyOfCommits);
        Assert.assertEquals(20, pmTrackerDatabase.find("a.Java").getCurrentProcessMetrics().linesDeleted);
    }

    //take care of case sensitivity
    @Test
    public void renameFile2(){
        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        pmTrackerDatabase.renameFile("a.Java", "a.Java");

        ProcessMetricTracker pmTracker = pmTrackerDatabase.find("a.Java");
        Assert.assertNotNull(pmTracker);
        Assert.assertEquals(10, pmTracker.getCurrentProcessMetrics().linesAdded);
        Assert.assertEquals(0, pmTracker.getBaseProcessMetrics().linesAdded);
        Assert.assertEquals("#1", pmTracker.getBaseCommitMetaData().getCommitId());
    }

    //take care of renamed files
    @Test
    public void removeFile2(){
        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#1", "null", "null", "0"), "Rafael", 10, 20);
        pmTrackerDatabase.renameFile("a.Java", "A.Java");

        ProcessMetricTracker pmTracker = pmTrackerDatabase.removeFile("a.Java");
        Assert.assertNull(pmTracker);

        pmTracker = pmTrackerDatabase.removeFile("A.Java");
        Assert.assertNotNull(pmTracker);
    }

    @Test
    public void isStable1(){
        for(int i = 0; i < 9; i++) {
            pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#" + (i), "null", "null", "0"), "Rafael", 10, 20);
            pmTrackerDatabase.reportChanges("b.Java", new CommitMetaData("#" + (i), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(0, pmTrackerDatabase.findStableInstances().size());
            Assert.assertEquals(i + 1, pmTrackerDatabase.find("a.Java").getCommitCounter());
            Assert.assertEquals(i + 1, pmTrackerDatabase.find("b.Java").getCommitCounter());
        }

        pmTrackerDatabase.reportChanges("b.Java", new CommitMetaData("#10", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(10, pmTrackerDatabase.find("b.Java").getCommitCounter());
        Assert.assertEquals(1, pmTrackerDatabase.findStableInstances().size());

        for(int i = 0; i < 14; i++) {
            pmTrackerDatabase.reportChanges("b.Java", new CommitMetaData("#" + (i + 11), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(0, pmTrackerDatabase.findStableInstances().size());
        }

        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#10", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pmTrackerDatabase.findStableInstances().size());
        Assert.assertEquals(10, pmTrackerDatabase.findStableInstances().get(0).getCommitCountThreshold());

        pmTrackerDatabase.reportChanges("b.Java", new CommitMetaData("#25", "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(2, pmTrackerDatabase.findStableInstances().size());
        Assert.assertEquals(25, pmTrackerDatabase.findStableInstances().stream().filter(pmTracker -> pmTracker.getFileName().equals("b.Java")).collect(Collectors.toList()).get(0).getCommitCountThreshold());

        pmTrackerDatabase.reportRefactoring("b.Java", new CommitMetaData("#26", "null", "null", "0"));
        Assert.assertEquals(1, pmTrackerDatabase.findStableInstances().size());

        Assert.assertEquals(1, pmTrackerDatabase.findStableInstances().size());
        Assert.assertEquals(10, pmTrackerDatabase.findStableInstances().get(0).getCommitCountThreshold());
    }

    @Test
    public void isStable2(){
        for(int i = 0; i < 9; i++) {
            pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#" + (i + 0), "null", "null", "0"), "Rafael", 10, 20);
            pmTrackerDatabase.reportChanges("b.Java", new CommitMetaData("#" + (i + 0), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(0, pmTrackerDatabase.findStableInstances().size());
        }

        pmTrackerDatabase.reportRefactoring("a.Java", new CommitMetaData("#" + (10), "null", "null", "0"));
        Assert.assertEquals(0, pmTrackerDatabase.find("a.Java").getCommitCounter());
        Assert.assertEquals(0, pmTrackerDatabase.findStableInstances().size());

        for(int i = 0; i < 2; i++) {
            pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#" + (i + 10), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(0, pmTrackerDatabase.findStableInstances().size());
            Assert.assertEquals(i + 1, pmTrackerDatabase.find("a.Java").getCommitCounter());
        }

        pmTrackerDatabase.reportChanges("b.Java", new CommitMetaData("#" + (9), "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pmTrackerDatabase.findStableInstances().size());

        for(int i = 0; i < 7; i++) {
            pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#" + (i + 12), "null", "null", "0"), "Rafael", 10, 20);
            Assert.assertEquals(1, pmTrackerDatabase.findStableInstances().size());
        }

        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#" + (20), "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(2, pmTrackerDatabase.findStableInstances().size());
    }

    @Test
    public void multipleKs1(){
        for(int i = 0; i < 9; i++) {
            pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#" + (i), "null", "null", "0"), "Rafael", 10, 20);
        }
        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#" + (10), "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(1, pmTrackerDatabase.findStableInstances().size());
        Assert.assertEquals(10, pmTrackerDatabase.findStableInstances().get(0).getCommitCountThreshold());

        pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#" + (11), "null", "null", "0"), "Rafael", 10, 20);
        Assert.assertEquals(0, pmTrackerDatabase.findStableInstances().size());

        for(int i = 0; i < 14; i++) {
            pmTrackerDatabase.reportChanges("a.Java", new CommitMetaData("#" + (i + 12), "null", "null", "0"), "Rafael", 10, 20);
        }
        Assert.assertEquals(1, pmTrackerDatabase.findStableInstances().size());
        Assert.assertEquals(25, pmTrackerDatabase.findStableInstances().get(0).getCommitCountThreshold());

        pmTrackerDatabase.reportRefactoring("a.Java", new CommitMetaData("#" + (26), "null", "null", "0"));
        Assert.assertEquals(0, pmTrackerDatabase.findStableInstances().size());
    }
}