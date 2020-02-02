package integration.realprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.No;
import refactoringml.db.Yes;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApacheCommonsCliIntegrationTest extends IntegrationBaseTest {

	@Override
	protected String getLastCommit() {
		return "b9ccc94008c78a59695f0c77ebe4ecf284370956";
	}

	@Override
	protected String getRepo() {
		return "https://github.com/apache/commons-cli.git";
	}

	@Override
	protected String track() {
		return "src/java/org/apache/commons/cli/Option.java";
	}

	// this test checks the Extract Method that has happened in #269eae18a911f792895d0402f5dd4e7913410523,
	// method getParsedOptionValue
	@Test
	public void t1() {


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

	}

	// this test follows the src/java/org/apache/commons/cli/Option.java file
	// This test helped us to understand that we should not delete
	// YES where variableAppearances = -1, as this happens in newly introduced variables.
	@Test
	public void t2() {

		List<No> noList = session.createQuery("From No where filePath = :filePath and project = :project")
				.setParameter("filePath", "src/java/org/apache/commons/cli/Option.java")
				.setParameter("project", project)
				.list();

		// it has been through 9 different refactorings
		List<Yes> yesList = session.createQuery("From Yes where filePath = :filePath and project = :project order by refactoringDate desc")
				.setParameter("filePath", "src/java/org/apache/commons/cli/Option.java")
				.setParameter("project", project)
				.list();

		Assert.assertEquals(9, yesList.size());
		assertRefactoring(yesList, "04490af06faa8fd1be15da88172beb32218dd336", "Extract Variable", 1);
		assertRefactoring(yesList, "347bbeb8f98a49744501ac50850457ba8751d545", "Extract Class", 1);
		assertRefactoring(yesList, "347bbeb8f98a49744501ac50850457ba8751d545", "Move Method", 3);
		assertRefactoring(yesList, "5470bcaa9d75d73fb9c687fa13e12d642c75984f", "Extract Method", 2);
		assertRefactoring(yesList, "97744806d59820b096fb502b1d51ca54b5d0921d", "Rename Method", 1);
		assertRefactoring(yesList, "bfe6bd8634895645aa71d6a6dc668545297d7413", "Rename Parameter", 1);

		// the file should appear twice as examples of 'no'
		assertNoRefactoring(noList, "aae50c585ec3ac33c6a9af792e80378904a73195", "5470bcaa9d75d73fb9c687fa13e12d642c75984f");
		// TODO: assertions related to the values of the No metrics

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
