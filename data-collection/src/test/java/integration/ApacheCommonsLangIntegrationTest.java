package integration;

import com.google.common.io.Files;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.BeforeClass;
import refactoringml.App;
import refactoringml.db.Database;
import refactoringml.db.HibernateConfig;
import refactoringml.db.Project;

import java.util.List;

public class ApacheCommonsLangIntegrationTest {

    private static Database db;
    private static String outputDir;
    private static SessionFactory sf;
    private static String tmpDir;
    private static Project project;

    @BeforeClass
    public static void before() throws Exception {
        sf = new HibernateConfig().getSessionFactory(DataBaseInfo.URL, "root", DataBaseInfo.PASSWORD, false);
        db = new Database(sf);
        outputDir = Files.createTempDir().getAbsolutePath();
        tmpDir = Files.createTempDir().getAbsolutePath();

        String repo1 = "https://www.github.com/apache/commons-lang.git";

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
}
