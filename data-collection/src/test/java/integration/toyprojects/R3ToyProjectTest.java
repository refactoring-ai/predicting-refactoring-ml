package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.No;
import refactoringml.db.Yes;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R3ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "repos/r3";
	}

	// This test helped to check if refactoring in subclasses are working.

	//Push Up Attribute not working see e3e605f2d76b5e8a4d85ba0d586103834822ea40
	//I tried to create a new class named Cat. Cat and Dog had the same field "region". Then I push it up to AnimalSuper.
	// However the Pull Up Attribute in commit 556cf904bc didnt work.
	// commit 892ffd8486daaedb5c92a548a23b87753393ce16 should show two refactoring -- Rename Class and Push Down Method
	@Test
	public void yes() {
		List<Yes> yesList = session.createQuery("From Yes where project = :project order by refactoringDate desc")
				.setParameter("project", project)
				.list();

		Assert.assertEquals(13, yesList.size());

		assertRefactoring(yesList, "074881da657ed0a11527cb8b14bba12e4111c704", "Rename Class", 1);
		assertRefactoring(yesList, "d025fed92a7253953a148f7264de28a85bc9af4e", "Rename Method", 2);
		assertRefactoring(yesList, "061febd820977f2b00c4926634f09908cc5b8b08", "Rename Parameter", 2);
		assertRefactoring(yesList, "376304b51193e5fade802be2cbd7523d6a5ba664", "Move And Rename Class", 1);
		assertRefactoring(yesList, "24b55774f386aefdc69f7753132310d53759e2e3", "Move Class", 1);
		assertRefactoring(yesList, "4881103961cfac2afb7139c29eb10536b42bc3cd", "Move Class", 1);
		assertRefactoring(yesList, "07cae36026215cefeac784c4213b2d46eb63de53", "Rename Parameter", 1);
		assertRefactoring(yesList, "8fc1ff2f53a617082767b8dd0af60b978dfc6e67", "Move Class", 1);
		assertRefactoring(yesList, "cf2ef5a3de59923ac000a4fe3ceeb8778229b293", "Pull Up Method", 2);
		assertRefactoring(yesList, "892ffd8486daaedb5c92a548a23b87753393ce16", "Rename Class", 1);

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
		Assert.assertEquals(23, project.getJavaLoc());
		//We dont have any Test file in this toy example.
		Assert.assertEquals(3, project.getNumberOfProductionFiles() + project.getNumberOfTestFiles());

		Assert.assertEquals(3, project.getNumberOfProductionFiles());

		Assert.assertEquals(23, project.getProductionLoc());


	}
}
