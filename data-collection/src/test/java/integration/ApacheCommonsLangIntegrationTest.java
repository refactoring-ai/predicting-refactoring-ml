package integration;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.*;
import refactoringml.App;
import refactoringml.db.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Ignore
public class ApacheCommonsLangIntegrationTest {

    private static Database db;
    private static String outputDir;
    private static SessionFactory sf;
    private static String tmpDir;
    private static Project project;

    @BeforeClass
    public static void before() throws Exception {
        sf = new HibernateConfig().getSessionFactory(DataBaseInfo.URL, "root", DataBaseInfo.PASSWORD, true);
        db = new Database(sf);
        outputDir = Files.createTempDir().getAbsolutePath();
        tmpDir = Files.createTempDir().getAbsolutePath();

        String repo1 = "https://www.github.com/apache/commons-lang.git";

        App app = new App("integration-test",
                repo1,
                outputDir,
                10,
                db,
                "2ea44b2adae8da8e3e7f55cc226479f9431feda9",
                false, false);

        project = app.run();

    }

    @AfterClass
    public static void after() throws IOException {
        db.close();
        sf.close();
        FileUtils.deleteDirectory(new File(tmpDir));
        FileUtils.deleteDirectory(new File(outputDir));
    }

    // this test checks the Rename Method that has happened in #5e7d64d6b2719afb1e5f4785d80d24ac5a19a782,
    // method isSet
    @Test
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

    @Test
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
        Assert.assertEquals(3, yesList.size());


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
    @Test
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
