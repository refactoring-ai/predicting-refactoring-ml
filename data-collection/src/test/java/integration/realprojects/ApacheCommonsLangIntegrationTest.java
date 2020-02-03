package integration.realprojects;

import integration.IntegrationBaseTest;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.No;
import refactoringml.db.Yes;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApacheCommonsLangIntegrationTest extends IntegrationBaseTest {


    @Override
    protected String getLastCommit() {
        return "2ea44b2adae8da8e3e7f55cc226479f9431feda9";
    }

    @Override
    protected String getRepo() {
        return "https://www.github.com/apache/commons-lang.git";
    }

    @Override
    protected String track() {
        return "src/java/org/apache/commons/lang/builder/HashCodeBuilder.java";
    }


    // this test checks the Rename Method that has happened in #5e7d64d6b2719afb1e5f4785d80d24ac5a19a782,
    // method isSet
    @org.junit.jupiter.api.Test
    public void t1() {


        Session session = sf.openSession();
//
//        // manually verified
        Yes instance1 = (Yes) session.createQuery("from Yes where refactoring = :refactoring and methodMetrics.fullMethodName = :method and refactorCommit = :refactorCommit and project = :project")
                .setParameter("refactoring", "Extract Method")
                .setParameter("method", "isSameDay/2[Date,Date]")
                .setParameter("refactorCommit", "5e7d64d6b2719afb1e5f4785d80d24ac5a19a782")
                .setParameter("project", project)
                .uniqueResult();

        Assert.assertNotNull(instance1);
//
        Assert.assertEquals("isSameDay/2[Date,Date]", instance1.getMethodMetrics().getFullMethodName());
        Assert.assertEquals(2, instance1.getMethodMetrics().getMethodVariablesQty());
        Assert.assertEquals(1, instance1.getMethodMetrics().getMethodMaxNestedBlocks());
        Assert.assertEquals(1, instance1.getMethodMetrics().getMethodReturnQty());
        Assert.assertEquals(0, instance1.getMethodMetrics().getMethodTryCatchQty());
//
        session.close();

    }

    // this test follows the src/java/org/apache/commons/lang/builder/HashCodeBuilder.java file

    @org.junit.jupiter.api.Test
    public void t2() {
        Session session = sf.openSession();

        List<No> noList = session.createQuery("From No where type = 1 and filePath = :filePath and project = :project")
                .setParameter("filePath", "src/java/org/apache/commons/lang/builder/HashCodeBuilder.java")
                .setParameter("project", project)
                .list();
        Assert.assertEquals(4, noList.size());

        List<Yes> yesList = session.createQuery("From Yes where filePath = :filePath and project = :project order by refactoringDate desc")
                .setParameter("filePath", "src/java/org/apache/commons/lang/builder/HashCodeBuilder.java")
                .setParameter("project", project)
                .list();
        Assert.assertEquals(8, yesList.size());


        Assert.assertEquals("5c40090fecdacd9366bba7e3e29d94f213cf2633", noList.get(0).getCommit());

        // then, it was refactored two times (in commit 5c40090fecdacd9366bba7e3e29d94f213cf2633)
        Assert.assertEquals("5c40090fecdacd9366bba7e3e29d94f213cf2633", yesList.get(0).getRefactorCommit());
        Assert.assertEquals("5c40090fecdacd9366bba7e3e29d94f213cf2633", yesList.get(1).getRefactorCommit());


        // It appears 3 times
        Assert.assertEquals("379d1bcac32d75e6c7f32661b2203f930f9989df", noList.get(1).getCommit());
        Assert.assertEquals("d3c425d6f1281d9387f5b80836ce855bc168453d", noList.get(2).getCommit());
        Assert.assertEquals("3ed99652c84339375f1e6b99bd9c7f71d565e023", noList.get(3).getCommit());



        session.close();
    }

    // check the number of test and production files as well as their LOC
    @org.junit.jupiter.api.Test
    public void t3() {

        // the next two assertions come directly from a 'cloc .' in the project
        Assert.assertEquals(78054L, project.getJavaLoc());
        Assert.assertEquals(340L, project.getNumberOfProductionFiles() + project.getNumberOfTestFiles());

        // find . -name "*.java" | grep "/test/" | wc
        Assert.assertEquals(179, project.getNumberOfTestFiles());

        // 340 - 179
        Assert.assertEquals(161, project.getNumberOfProductionFiles());

        // cloc . --by-file | grep "/test/"
        Assert.assertEquals(49632, project.getTestLoc());

        // 78054L - 49632
        Assert.assertEquals(28422, project.getProductionLoc());


//        Assert.assertEquals(33120617L, project.getProjectSizeInBytes());

    }

}
