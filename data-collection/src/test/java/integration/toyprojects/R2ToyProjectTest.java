package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.ProcessMetric;
import refactoringml.db.No;
import refactoringml.db.ProcessMetrics;
import refactoringml.db.Yes;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R2ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "repos/r2";
	}

	// This test helped us to realize (again) that when class name and file name don't match, we can't link the
	// refactoring. We opened a PR in RefactoringMiner; now it works!
	@Test
	public void yes() {
		List<Yes> yesList = session.createQuery("From Yes where project = :project order by refactoringDate desc")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(2, yesList.size());

		String renameCommit = "bc15aee7cfaddde19ba6fefe0d12331fe98ddd46";
		assertRefactoring(yesList, renameCommit, "Rename Class", 1);

		Yes renameRefactoring = yesList.stream().filter(yes -> yes.getRefactorCommit().equals(renameCommit)).findFirst().get();
		//TODO: figure out what to expect here
		ProcessMetrics metrics = new ProcessMetrics(0, 1, 3, 1, 0, 1, 0, 0, 1);
		assertProcessMetrics(renameRefactoring, metrics);

		String nextCommit = "515365875143aa84b5bbb5c3191e7654a942912f";
		assertRefactoring(yesList, nextCommit, "Extract Class", 1);

		Yes extractClassRefactoring = yesList.stream().filter(yes -> yes.getRefactorCommit().equals(renameCommit)).findFirst().get();
		//TODO: figure out what to expect here
		metrics = new ProcessMetrics(0, 1, 3, 1, 0, 1, 0, 0, 1);
		assertProcessMetrics(extractClassRefactoring, metrics);
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
		Assert.assertEquals(64, project.getJavaLoc());

		Assert.assertEquals(4, project.getNumberOfProductionFiles() + project.getNumberOfTestFiles());

		Assert.assertEquals(3, project.getNumberOfProductionFiles());

		Assert.assertEquals(1, project.getNumberOfTestFiles());

		Assert.assertEquals(56, project.getProductionLoc());

	}
}
