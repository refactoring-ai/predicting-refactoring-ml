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
public class R1ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "repos/r1";
	}

	@Test
	public void yes() {
		List<Yes> yesList = session.createQuery("From Yes where project = :project order by refactoringDate desc")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(3, yesList.size());

		assertRefactoring(yesList, "e8895b22847c7c54a9e187f9f674db274e6bc103", "Extract Method", 1);

		for (Yes yes : yesList){
			Assertions.assertFalse(yes.getClassMetrics().isSubclass());
		}
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
	public void commitMetaData(){
		String commit = "21151bf7e36da52b9305d99755eb6f0b7616e620";
		Yes yes = (Yes) session.createQuery("From Yes where project = :project and refactorCommit = :refactorCommit ")
				.setParameter("project", project)
				.setParameter("refactorCommit", commit)
				.list().get(0);

		Assert.assertEquals("Inline Method\tprivate convert(a int) : int inlined to public m1() : void in class a.Example1", yes.getRefactoringSummary());
		Assert.assertEquals("inline method", yes.getCommitMessage());
		Assert.assertEquals("@local/repos/r1/" + commit, yes.getCommitUrl());
	}

	@Test
	public void metrics() {
		// the next two assertions come directly from a 'cloc .' in the project
		Assert.assertEquals(9, project.getJavaLoc());

		Assert.assertEquals(1, project.getNumberOfProductionFiles() + project.getNumberOfTestFiles());

		Assert.assertEquals(1, project.getNumberOfProductionFiles());

		Assert.assertEquals(0, project.getNumberOfTestFiles());

		Assert.assertEquals(9, project.getProductionLoc());
	}
}
