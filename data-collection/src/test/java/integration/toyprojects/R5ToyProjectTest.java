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
public class R5ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "repos/r5";
	}

	// this test was to see whether the tool would collect classes with generics
	@Test
	public void yes() {

		List<Yes> yesList = session.createQuery("From Yes where project = :project order by refactoringDate desc")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(1, yesList.size());

		assertRefactoring(yesList, "31820e9d172ba571d93de14733101f8cb81853e8", "Extract Method", 1);
		Assertions.assertEquals("a.Test", yesList.get(0).getClassName());

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
	public void metrics() {

		// the next two assertions come directly from a 'cloc .' in the project
		Assert.assertEquals(19, project.getJavaLoc());

		Assert.assertEquals(1, project.getNumberOfProductionFiles() + project.getNumberOfTestFiles());

		Assert.assertEquals(0, project.getNumberOfProductionFiles());

		Assert.assertEquals(1, project.getNumberOfTestFiles());

		Assert.assertEquals(0, project.getProductionLoc());

		Assert.assertEquals(19, project.getTestLoc());

	}


}
