package integration.realprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.No;
import refactoringml.db.Yes;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApacheCommonsCSVIntegrationTest extends IntegrationBaseTest {

    @Override
    protected String getLastCommit() {
        return "70092bb303af69b09bf3978b24c1faa87c909e3c";
    }

    @Override
    protected String getRepo() {
        return "https://www.github.com/apache/commons-csv.git";
    }

    @Override
    protected String track() {
        return "src/main/java/org/apache/commons/csv/CSVFormat.java";
    }



    // this test checks the Rename Parameter that has happened in #b58168683d01149a568734df21568ffcc41105fe,
    // method isSet
    @org.junit.jupiter.api.Test
    public void t1() {

        // manually verified
        Yes instance1 = (Yes) session.createQuery("from Yes where refactoring = :refactoring and methodMetrics.fullMethodName = :method and refactorCommit = :refactorCommit and project = :project")
                .setParameter("refactoring", "Rename Parameter")
                .setParameter("method", "isSet/1[int]")
                .setParameter("refactorCommit", "b58168683d01149a568734df21568ffcc41105fe")
                .setParameter("project", project)
                .uniqueResult();

        Assert.assertNotNull(instance1);

        Assert.assertEquals("isSet/1[int]", instance1.getMethodMetrics().getFullMethodName());
        Assert.assertEquals(0, instance1.getMethodMetrics().getMethodVariablesQty());
        Assert.assertEquals(0, instance1.getMethodMetrics().getMethodMaxNestedBlocks());
        Assert.assertEquals(1, instance1.getMethodMetrics().getMethodReturnQty());
        Assert.assertEquals(0, instance1.getMethodMetrics().getMethodTryCatchQty());

    }

    // this test follows the src/main/java/org/apache/commons/csv/CSVFormat.java file
    // this class was committed 281 times:
    // was introduced on commit cb99634ab3d6143dffc90938fc68e15c7f9d25b8
    // was refactored on commit 67d150adc88b806e52470d110a438d9107e72ed5
    // was refactored on commit 322fad25ad96da607a3045a19702a55381a426e7
    @org.junit.jupiter.api.Test
    public void t2() {


        List<No> noList = session.createQuery("From No where type = 1 and filePath = :filePath and project = :project")
                .setParameter("filePath", track())
                .setParameter("project", project)
                .list();
        Assert.assertEquals(35, noList.size());

        List<Yes> yesList = session.createQuery("From Yes where filePath = :filePath and project = :project order by refactoringDate desc")
                .setParameter("filePath", track())
                .setParameter("project", project)
                .list();
        Assert.assertEquals(65, yesList.size());
//
//        // the file stayed 15 commits without a refactoring, so that's an example for the no
        Assert.assertEquals("cb99634ab3d6143dffc90938fc68e15c7f9d25b8", noList.get(0).getCommit());
//
//        // then, it was refactored 22 times (in commit 347bb..., 6 different refactorings have happened)
        Assert.assertEquals("322fad25ad96da607a3045a19702a55381a426e7", yesList.get(0).getRefactorCommit());
        Assert.assertEquals("67d150adc88b806e52470d110a438d9107e72ed5", yesList.get(1).getRefactorCommit());
        Assert.assertEquals("4695d73e3b1974454d55a30be2b1c3a4bddbf3db", yesList.get(12).getRefactorCommit());
//
//        // then, 65 commits in a row without a refactoring
//        // so, it appears 24 times
        Assert.assertEquals("4f3ef66ce3f030c1f45b9426908da32e462e6bac", noList.get(1).getCommit());
        Assert.assertEquals("38741a48c692ae2fc13cd2445e77ace6ecea1156", noList.get(2).getCommit());
        Assert.assertEquals("87466459c0086004703341766df2609467ea0b89", noList.get(3).getCommit());
        Assert.assertEquals("50e2719bb646870dc08dd71f2bc2314ce46def76", noList.get(4).getCommit());
        Assert.assertEquals("7ac5dd3ec633d64603bb836d0576f34a51f93f08", noList.get(5).getCommit());
        Assert.assertEquals("7ac5dd3ec633d64603bb836d0576f34a51f93f08", noList.get(6).getCommit());


    }

    // check the number of test and production files as well as their LOC
    @org.junit.jupiter.api.Test
    public void t3() {

        // the next two assertions come directly from a 'cloc .' in the project
        Assert.assertEquals(6994L, project.getJavaLoc());
        Assert.assertEquals(35L, project.getNumberOfProductionFiles() + project.getNumberOfTestFiles());

        // find . -name "*.java" | grep "/test/" | wc
        Assert.assertEquals(23, project.getNumberOfTestFiles());

        // 35 - 23
        Assert.assertEquals(12, project.getNumberOfProductionFiles());

        // cloc . --by-file | grep "/test/"
        Assert.assertEquals(5114, project.getTestLoc());

        // 6994 - 5114
        Assert.assertEquals(1880, project.getProductionLoc());


    }


}
