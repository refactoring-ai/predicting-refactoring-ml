package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.No;
import refactoringml.db.Yes;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R4ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "repos/r4";
	}

	@Test
	public void yes() {
		List<Yes> yesList = session.createQuery("From Yes where project = :project order by refactoringDate desc")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(2, yesList.size());

		String extractCommit = "dd9aa00b03c9456c69c5e6566040fb994d7c9d98";
		Yes extractYes = yesList.stream().filter(yes -> yes.getRefactorCommit().equals(extractCommit)).findFirst().get();
		Assert.assertEquals("a.Animal.Dog", extractYes.getClassName());
		assertRefactoring(yesList, extractCommit, "Extract Method", 1);

		String renameCommit = "104e39574462f9e4bd6b1cdf388ecd0334a6f2c3";
		Yes renameYes = yesList.stream().filter(yes -> yes.getRefactorCommit().equals(renameCommit)).findFirst().get();
		Assert.assertEquals("Rename Class", renameYes.getRefactoring());
		assertRefactoring(yesList, renameCommit, "Rename Class", 2);
	}

	@Test
	public void no() {
		// there are no instances of no variables, as the repo is too small
		List<No> noList = session.createQuery("From No where project = :project")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(0, noList.size());
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
