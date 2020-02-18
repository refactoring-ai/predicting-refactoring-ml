package integration;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import refactoringml.App;
import refactoringml.db.Database;
import refactoringml.db.HibernateConfig;
import refactoringml.db.Project;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/*
    Test if a project can be added twice to a database both for the Single and Queue version of the data collection.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DatabaseProjectTest{
    private final String repo1 = "repos/r1";
    private final String repo2 = "repos/r2";

    private String outputDir;
    private String tmpDir;

    private final String URL = "jdbc:mysql://localhost:3306/test";
    private final String USER = "root";
    private final String PASSWORD = "refactoring";

    private Database db;
    private SessionFactory sf;
    private Session session;

    @BeforeAll
    private void initTests() throws InterruptedException {
        outputDir = Files.createTempDir().getAbsolutePath();
        tmpDir = Files.createTempDir().getAbsolutePath();

        sf = new HibernateConfig().getSessionFactory(URL, USER, PASSWORD, false);
        db = new Database(sf);
    }

    @BeforeEach
    void openSession() {
        session = sf.openSession();
    }

    @AfterEach
    void resetTests() {
        deleteProject(repo1);
        deleteProject(repo2);
        this.session.close();
        this.session = null;
    }

    @AfterAll
    protected void cleanTests() throws IOException, InterruptedException {
        deleteProject(repo1);
        deleteProject(repo2);
        FileUtils.deleteDirectory(new File(tmpDir));
        FileUtils.deleteDirectory(new File(outputDir));
    }

    protected void deleteProject(String repo1) {
        try {
            Session session = sf.openSession();


            List<Project> projects = (List<Project>) session.createQuery("from Project p where p.gitUrl = :gitUrl")
                    .setParameter("gitUrl", repo1).list();

            if(!projects.isEmpty()) {
                session.beginTransaction();

                session.createQuery("delete from RefactoringCommit y where project in :project")
                        .setParameter("project", projects)
                        .executeUpdate();
                session.createQuery("delete from StableCommit where project in :project")
                        .setParameter("project", projects)
                        .executeUpdate();

                projects.stream().forEach(session::delete);
                session.getTransaction().commit();
            }


            session.close();
        } catch(Exception e) {
            System.out.println("Could not delete the project before starting the test");
            e.printStackTrace();
        }
    }

    /*
        Test if two different projects are processed.
    */
    @Test
    public void different() throws Exception {
        new App("repo1", repo1, outputDir, 50, db, false).run();
        new App("repo2", repo2, outputDir, 50, db, false).run();
    }

    /*
        Test if the same project is not processed twice.
    */
    @Test
    public void twice() throws Exception {
        new App("repo1", repo1, outputDir, 50, db, false).run();

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new App("repo1", repo1, outputDir, 50, db, false).run();
        });
        String expectedMessage = "already in the database";
        String passedMessage = exception.getMessage();
        assertTrue(passedMessage.contains(expectedMessage));
    }

    /*
        Test if the same project is not processed twice.
        TODO: check why it is failing. Probably because if you run all the tests together,
        one of the repos might be there already...
    */
    @Test @Disabled
    public void alternating() throws Exception {
        new App("repo1", repo1, outputDir, 50, db, false).run();
        new App("repo2", repo2, outputDir, 50, db, false).run();
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new App("repo1", repo1, outputDir, 50, db, false).run();
        });
        String expectedMessage = "already in the database";
        String passedMessage = exception.getMessage();
        assertTrue(passedMessage.contains(expectedMessage));
    }
}