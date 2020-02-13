package integration.toyprojects;

import com.jcabi.immutable.Array;
import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.No;
import refactoringml.db.ProcessMetrics;
import refactoringml.db.Yes;

import java.util.ArrayList;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R4ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "repos/r4";
	}

	@Test
	public void yes() {
		List<Yes> yesList = session.createQuery("From Yes where project = :project order by refactoringDate asc")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(5, yesList.size());

		assertRefactoring(yesList, "dd9aa00b03c9456c69c5e6566040fb994d7c9d98", "Extract Method", 1);
		Assertions.assertEquals("a.Animal.Dog", yesList.get(0).getClassName());
		Assertions.assertTrue(yesList.get(0).getClassMetrics().isSubclass());
	}

	@Test
	public void no() {
		// there are no instances of no variables, as the repo is too small
		List<No> noList = session.createQuery("From No where project = :project")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(0, noList.size());
	}

	/*
	Test various cases of class and file renames, to check if they are detected correctly and metrics are tracked.
	Between the two commits: rename class and full rename, the file name of the affected class was changed as well in one commit.
	 */
	@Test
	public void classRenames(){
		List<Yes> yesList = session.createQuery("From Yes where project = :project and refactoring = :refactoring order by refactoringDate desc")
				.setParameter("project", project)
				.setParameter("refactoring", "Rename Class")
				.list();
		Assert.assertEquals(4, yesList.size());

		//two renames of subclasses in one commits
		String doubleRenameCommit = "104e39574462f9e4bd6b1cdf388ecd0334a6f2c3";
		//renamed only the class name without the filename
		String renameClass = "96443c0c80919970071acfbb9f2af6a99b1f41ac";
		//renamed both class name and filename
		String renameClassFull = "d801d80c03ff1268010bbb43cec43da4be233dfd";

		assertRefactoring(yesList, doubleRenameCommit, "Rename Class",2);
		assertRefactoring(yesList, renameClass, "Rename Class",1);
		assertRefactoring(yesList, renameClassFull, "Rename Class",1);

		//no check if the class metrics were tracked and set correct
		ProcessMetrics doubleRenameMetrics = new ProcessMetrics();
		assertProcessMetrics(filterCommit(yesList, doubleRenameCommit).get(0), doubleRenameMetrics);
		assertProcessMetrics(filterCommit(yesList, doubleRenameCommit).get(1), doubleRenameMetrics);

		ProcessMetrics renameClassMetrics = new ProcessMetrics();
		assertProcessMetrics(filterCommit(yesList, renameClass).get(0), renameClassMetrics);

		ProcessMetrics renameFullMetrics = new ProcessMetrics();
		assertProcessMetrics(filterCommit(yesList, renameClassFull).get(0), renameFullMetrics);
	}

	@Test
	public void metrics() {
		// the next two assertions come directly from a 'cloc .' in the project
		Assert.assertEquals(29, project.getJavaLoc());

		Assert.assertEquals(2, project.getNumberOfProductionFiles() + project.getNumberOfTestFiles());

		Assert.assertEquals(1, project.getNumberOfProductionFiles());

		Assert.assertEquals(1, project.getNumberOfTestFiles());

		Assert.assertEquals(21, project.getProductionLoc());
	}
}
