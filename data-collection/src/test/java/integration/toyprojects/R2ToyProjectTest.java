package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.No;
import refactoringml.db.Yes;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R2ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "repos/r2";
	}

	// This test helped us to realize (again) that when class name and file name don't match, we can't link the
	// refactoring. We opened a PR in RefactoringMiner; waiting for merge.
	@Test
	public void yes() {

		List<Yes> yesList = session.createQuery("From Yes where project = :project order by refactoringDate desc")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(1, yesList.size());

		assertRefactoring(yesList, "bc15aee7cfaddde19ba6fefe0d12331fe98ddd46", "Rename Class", 1);

		// FIX: Uncomment this once RefactoringMiner returns file names
		//assertRefactoring(yesList, "a03a3d71f7838cf964551fdc2be22b37fe9a35e6", "Rename Attribute", 1);
		//assertRefactoring(yesList, "e5b28cec38af6d7f1564e19f90278b6d1d7037b2", "Rename Attribute", 1);
	}

	@Test
	public void no() {
		// there are no instances of no variables, as the repo is too small
		List<No> noList = session.createQuery("From No where project = :project")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(0, noList.size());
	}
}
