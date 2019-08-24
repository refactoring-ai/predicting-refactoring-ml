package integration;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import refactoringml.App;
import refactoringml.db.Database;
import refactoringml.db.HibernateConfig;
import refactoringml.db.Project;
import refactoringml.db.Yes;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static refactoringml.util.FilePathUtils.lastSlashDir;

public class IntegrationTest {

	private Database db;
	private String outputDir;
	private SessionFactory sf;
	private String tmpDir;

	@Before
	public void before() {
		sf = new HibernateConfig().getSessionFactory("jdbc:mysql://localhost/ml4se-tests?useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC", "root", "", true);
		db = new Database(sf);
		outputDir = Files.createTempDir().getAbsolutePath();
		tmpDir = Files.createTempDir().getAbsolutePath();
	}

	@After
	public void after() throws IOException {
		db.close();
		sf.close();
		FileUtils.deleteDirectory(new File(tmpDir));
		FileUtils.deleteDirectory(new File(outputDir));
	}

	@Test
	public void t1() throws Exception {
		String repo1 = "git@github.com:apache/commons-cli.git";

		App app = new App("integration-test",
				lastSlashDir(tmpDir)+"repo",
				repo1,
				outputDir,
				10,
				db,
				"b9ccc94008c78a59695f0c77ebe4ecf284370956");

		app.run();

		Session session = sf.openSession();
		List<Project> projects = session.createQuery("from Project").list();
		System.out.println(projects);


	}
}
