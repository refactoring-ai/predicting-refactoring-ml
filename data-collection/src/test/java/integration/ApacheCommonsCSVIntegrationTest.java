package integration;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import refactoringml.App;
import refactoringml.db.Database;
import refactoringml.db.HibernateConfig;
import refactoringml.db.Project;
import refactoringml.db.Yes;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ApacheCommonsCSVIntegrationTest {

    private static Database db;
    private static String outputDir;
    private static SessionFactory sf;
    private static String tmpDir;
    private static Project project;

    @BeforeClass
    public static void before() throws Exception {
        sf = new HibernateConfig().getSessionFactory("jdbc:mysql://localhost/refactoringtest?useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", DataBaseInfo.pass, false);
        db = new Database(sf);
        outputDir = Files.createTempDir().getAbsolutePath();
        tmpDir = Files.createTempDir().getAbsolutePath();

        String repo1 = "https://www.github.com/apache/commons-csv.git";

        Session session = sf.openSession();
        List<Project> list = session.createQuery("from Project where gitUrl = :gitUrl").setParameter("gitUrl", repo1).list();
        if(list.isEmpty()) {
            App app = new App("integration-test",
                    repo1,
                    outputDir,
                    10,
                    db,
                    "70092bb303af69b09bf3978b24c1faa87c909e3c",
                    false, false);

            project = app.run();
        } else {
            project = list.get(0);
        }

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

}
