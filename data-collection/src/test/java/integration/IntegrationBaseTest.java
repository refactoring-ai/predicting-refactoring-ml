package integration;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import refactoringml.App;
import refactoringml.db.Database;
import refactoringml.db.HibernateConfig;
import refactoringml.db.Project;

import java.io.File;
import java.io.IOException;

public abstract class IntegrationBaseTest {


	protected Database db;
	protected String outputDir;
	protected SessionFactory sf;
	protected String tmpDir;
	protected Project project;

	@BeforeAll
	public void before() throws Exception {
		sf = new HibernateConfig().getSessionFactory(DataBaseInfo.URL, "root", DataBaseInfo.PASSWORD, true);
		db = new Database(sf);
		outputDir = Files.createTempDir().getAbsolutePath();
		tmpDir = Files.createTempDir().getAbsolutePath();

		String repo1 = getRepo();

		App app = new App("toy-integration-test",
				repo1,
				outputDir,
				threshold(),
				db,
				false);

		project = app.run();

	}

	protected int threshold() {
		return 10;
	}

	protected abstract String getRepo();


	@AfterAll
	public void after() throws IOException {
		db.close();
		sf.close();
		FileUtils.deleteDirectory(new File(tmpDir));
		FileUtils.deleteDirectory(new File(outputDir));
	}
}
