package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.No;
import refactoringml.db.ProcessMetrics;
import refactoringml.db.Yes;
import java.util.List;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R2ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "https://github.com/jan-gerling/toyrepo-r2.git";
	}

	// This test helped us to realize (again) that when class name and file name don't match, we can't link the
	// refactoring. We opened a PR in RefactoringMiner; now it works!
	@Test
	public void yes() {
		List<Yes> yesList = session.createQuery("From Yes where project = :project order by commitMetaData.commitDate desc")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(2, yesList.size());

		String renameCommit = "bc15aee7cfaddde19ba6fefe0d12331fe98ddd46";
		assertRefactoring(yesList, renameCommit, "Rename Class", 1);

		Yes renameRefactoring = yesList.stream().filter(yes -> yes.getCommit().equals(renameCommit)).findFirst().get();
		//TODO: figure out what to expect here
		ProcessMetrics metrics = new ProcessMetrics(1, 5, 0, 1, 0, 1, 1.0, 0, 0);
		assertProcessMetrics(renameRefactoring, metrics);

		String extractCommit = "515365875143aa84b5bbb5c3191e7654a942912f";
		assertRefactoring(yesList, extractCommit, "Extract Class", 1);

		Yes extractClassRefactoring = (Yes) filterCommit(yesList, extractCommit).get(0);
		//TODO: figure out what to expect here
		metrics = new ProcessMetrics(0, 1, 3, 1, 0, 1, 0, 0, 1);
//		assertProcessMetrics(extractClassRefactoring, metrics);
	}

	@Test
	public void isSubclass() {
		List<Yes> yesList = session.createQuery("From Yes where project = :project order by commitMetaData.commitDate desc")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(2, yesList.size());

		List<Yes> areSubclasses = yesList.stream().filter(yes ->
				yes.getClassMetrics().isInnerClass()
						&& yes.getClassName().equals("org.apache.commons.cli.HelpFormatter.StringBufferComparator")).collect(Collectors.toList());
		List<Yes> areNoSubclasses = yesList.stream().filter(yes -> !yes.getClassMetrics().isInnerClass()).collect(Collectors.toList());

		Assert.assertEquals(0, areSubclasses.size());
		Assert.assertEquals(2, areNoSubclasses.size());
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
		String commit = "bc15aee7cfaddde19ba6fefe0d12331fe98ddd46";
		assertMetaDataYes(
				commit,
				"rename class",
				"Rename Class\tPerson renamed to People",
				"@local/repos/toyrepo-r2/" + commit);
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
