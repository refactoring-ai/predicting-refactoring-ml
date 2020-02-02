package integration.realprojects;

import com.google.common.io.Files;
import integration.DataBaseInfo;
import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.*;
import refactoringml.App;
import refactoringml.TrackDebugMode;
import refactoringml.db.*;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Ignore
public class ApacheCommonsCSVIntegrationTest {

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

        //String repo1 = "https://www.github.com/apache/commons-csv.git";
        String repo1 = "/Users/mauricioaniche/Desktop/commons-csv";

        TrackDebugMode.ACTIVE = true;
        TrackDebugMode.FILE_TO_TRACK = "src/main/java/org/apache/commons/csv/CSVFormat.java";

        App app = new App("integration-test",
                repo1,
                outputDir,
                50,
                db,
                "70092bb303af69b09bf3978b24c1faa87c909e3c",
                false);

        project = app.run();

    }

    @AfterClass
    public static void after() throws IOException {
        db.close();
        sf.close();
        FileUtils.deleteDirectory(new File(tmpDir));
        FileUtils.deleteDirectory(new File(outputDir));
    }


    // this test checks the Rename Parameter that has happened in #b58168683d01149a568734df21568ffcc41105fe,
    // method isSet
    @Test
    public void t1() {


        Session session = sf.openSession();

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

        session.close();

    }

    // this test follows the src/main/java/org/apache/commons/csv/CSVFormat.java file
    // this class was committed 281 times:
    // was introduced on commit cb99634ab3d6143dffc90938fc68e15c7f9d25b8
    // was refactored on commit 67d150adc88b806e52470d110a438d9107e72ed5
    // was refactored on commit 322fad25ad96da607a3045a19702a55381a426e7
    @Test
    public void t2() {
        Session session = sf.openSession();

        List<No> noList = session.createQuery("From No where type = 1 and filePath = :filePath and project = :project")
                .setParameter("filePath", "src/main/java/org/apache/commons/csv/CSVFormat.java")
                .setParameter("project", project)
                .list();
        Assert.assertEquals(24, noList.size());

        List<Yes> yesList = session.createQuery("From Yes where filePath = :filePath and project = :project order by refactoringDate desc")
                .setParameter("filePath", "src/main/java/org/apache/commons/csv/CSVFormat.java")
                .setParameter("project", project)
                .list();
        Assert.assertEquals(56, yesList.size());
//
//        // the file stayed 15 commits without a refactoring, so that's an example for the no
        Assert.assertEquals("cb99634ab3d6143dffc90938fc68e15c7f9d25b8", noList.get(0).getCommit());
//
//        // then, it was refactored 22 times (in commit 347bb..., 6 different refactorings have happened)
        Assert.assertEquals("322fad25ad96da607a3045a19702a55381a426e7", yesList.get(0).getRefactorCommit());
        Assert.assertEquals("67d150adc88b806e52470d110a438d9107e72ed5", yesList.get(1).getRefactorCommit());
        Assert.assertEquals("a72c71f5cc6431890f82707a2782325be6747dd1", yesList.get(12).getRefactorCommit());
//
//        // then, 65 commits in a row without a refactoring
//        // so, it appears 24 times
        Assert.assertEquals("4f3ef66ce3f030c1f45b9426908da32e462e6bac", noList.get(1).getCommit());
        Assert.assertEquals("38741a48c692ae2fc13cd2445e77ace6ecea1156", noList.get(2).getCommit());
        Assert.assertEquals("87466459c0086004703341766df2609467ea0b89", noList.get(3).getCommit());
        Assert.assertEquals("50e2719bb646870dc08dd71f2bc2314ce46def76", noList.get(4).getCommit());
        Assert.assertEquals("7ac5dd3ec633d64603bb836d0576f34a51f93f08", noList.get(5).getCommit());
        Assert.assertEquals("71a657004e090a86c47593f624cdcc3bb2ea710e", noList.get(6).getCommit());
        Assert.assertEquals("e28d4e9c2e2f3db38b6b1939ee1fde819debed9b", noList.get(7).getCommit());
        Assert.assertEquals("193dfd79af01acc41536f6c46019100bdd45527a", noList.get(8).getCommit());
        Assert.assertEquals("75f39a81a77b3680c21cd3f810da62ebbe9944b8", noList.get(9).getCommit());
        Assert.assertEquals("fcc0d15c7b541191f14b0861d945cbbeba770d10", noList.get(10).getCommit());
        Assert.assertEquals("e01400d2c6b0d8a9c1569229be234155e6a280ae", noList.get(11).getCommit());
        Assert.assertEquals("c84328e64a226304d277be6164b85351502edd94", noList.get(12).getCommit());
        Assert.assertEquals("4b2a4caa305c2e309c199080d5710f6e1a935a7d", noList.get(13).getCommit());
        Assert.assertEquals("67d150adc88b806e52470d110a438d9107e72ed5", noList.get(14).getCommit());
        Assert.assertEquals("ef39a01a22faf8daa34f2fd0cba322c975c70559", noList.get(15).getCommit());
        Assert.assertEquals("453cfcbb593732bee980f4c83d06448152ac9887", noList.get(16).getCommit());
        Assert.assertEquals("20eac694a070838bfe37df293007cf1babd127b4", noList.get(17).getCommit());
        Assert.assertEquals("21cb8b4750ca9356644e2e64655c6463bed47509", noList.get(18).getCommit());
        Assert.assertEquals("eb5c332a72365777bb1a9c408463d64ed558cb1f", noList.get(19).getCommit());
        Assert.assertEquals("09cf3739df3b82b23bf508b4f98e999ee5b8d3c7", noList.get(20).getCommit());
        Assert.assertEquals("0c216e783cbff346c820cabb83486e4401b2c0a2", noList.get(21).getCommit());
        Assert.assertEquals("322fad25ad96da607a3045a19702a55381a426e7", noList.get(22).getCommit());
        Assert.assertEquals("89f81712154a7d510e0b0f9323b08421cb8a529a", noList.get(23).getCommit());


        session.close();
    }

    // check the number of test and production files as well as their LOC
    @Test
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
