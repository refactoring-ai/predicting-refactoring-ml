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
public class ApacheCommonsCliIntegrationTest {

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

		String repo1 = "https://github.com/apache/commons-cli.git";

		App app = new App("integration-test",
				repo1,
				outputDir,
				10,
				db,
				"b9ccc94008c78a59695f0c77ebe4ecf284370956",
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

	// this test checks the Extract Method that has happened in #269eae18a911f792895d0402f5dd4e7913410523,
	// method getParsedOptionValue
	@Test
	public void t1() {


		Session session = sf.openSession();

		// manually verified
		Yes instance1 = (Yes) session.createQuery("from Yes where refactoring = :refactoring and methodMetrics.fullMethodName = :method and refactorCommit = :refactorCommit and project = :project")
				.setParameter("refactoring", "Extract Method")
				.setParameter("method", "getParsedOptionValue/1[String]")
				.setParameter("refactorCommit", "269eae18a911f792895d0402f5dd4e7913410523")
				.setParameter("project", project)
				.uniqueResult();

		Assert.assertNotNull(instance1);

		Assert.assertEquals("getParsedOptionValue/1[String]", instance1.getMethodMetrics().getFullMethodName());
		Assert.assertEquals(2, instance1.getMethodMetrics().getMethodVariablesQty());
		Assert.assertEquals(1, instance1.getMethodMetrics().getMethodMaxNestedBlocks());
		Assert.assertEquals(2, instance1.getMethodMetrics().getMethodReturnQty());
		Assert.assertEquals(0, instance1.getMethodMetrics().getMethodTryCatchQty());

		session.close();

	}

	// this test follows the src/java/org/apache/commons/cli/Option.java file
	// this class was committed 41 times:
	// was introduced on commit aae50c585ec3ac33c6a9af792e80378904a73195 (commit 1)
	// was refactored on commit 347bbeb8f98a49744501ac50850457ba8751d545 (commit 16)
	// was refactored on commit 5470bcaa9d75d73fb9c687fa13e12d642c75984f (commit 18)
	@Test
	public void t2() {
		Session session = sf.openSession();

		List<No> noList = session.createQuery("From No where type = 1 and filePath = :filePath and project = :project")
				.setParameter("filePath", "src/java/org/apache/commons/cli/Option.java")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(3, noList.size());

		List<Yes> yesList = session.createQuery("From Yes where filePath = :filePath and project = :project order by refactoringDate desc")
				.setParameter("filePath", "src/java/org/apache/commons/cli/Option.java")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(5, yesList.size());

		// the file stayed 15 commits without a refactoring, so that's an example for the no
		Assert.assertEquals("aae50c585ec3ac33c6a9af792e80378904a73195", noList.get(0).getCommit());

		// then, it was refactored two times (in commit 347bb..., 4 different refactorings have happened)
		Assert.assertEquals("5470bcaa9d75d73fb9c687fa13e12d642c75984f", yesList.get(0).getRefactorCommit());
		Assert.assertEquals("347bbeb8f98a49744501ac50850457ba8751d545", yesList.get(1).getRefactorCommit());

		// then, 23 commits in a row without a refactoring
		// so, it appears two times
		// the first time, note how it matches with the refactoring commit; after all, it was when it started to become stable
		Assert.assertEquals("5470bcaa9d75d73fb9c687fa13e12d642c75984f", noList.get(1).getCommit());
		Assert.assertEquals("b0e1b80b6d4a10a9c9f46539bc4c7a3cce55886e", noList.get(2).getCommit());

		session.close();
	}

	// check the number of test and production files as well as their LOC
	@Test
	public void t3() {

		// the next two assertions come directly from a 'cloc .' in the project
		Assert.assertEquals(7070L, project.getJavaLoc());
		Assert.assertEquals(52L, project.getNumberOfProductionFiles() + project.getNumberOfTestFiles());

		// find . -name "*.java" | grep "/test/" | wc
		Assert.assertEquals(29, project.getNumberOfTestFiles());

		// 52 - 29
		Assert.assertEquals(23, project.getNumberOfProductionFiles());

		// cloc . --by-file | grep "/test/"
		Assert.assertEquals(4280, project.getTestLoc());

		// 7070 - 4280
		Assert.assertEquals(2790, project.getProductionLoc());

	}
}
