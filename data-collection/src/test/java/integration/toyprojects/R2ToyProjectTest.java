package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.RefactoringCommit;
import refactoringml.db.StableCommit;
import refactoringml.db.ProcessMetrics;

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
	public void refactorings() {
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits();
		Assert.assertEquals(2, refactoringCommitList.size());

		String renameCommit = "bc15aee7cfaddde19ba6fefe0d12331fe98ddd46";
		assertRefactoring(refactoringCommitList, renameCommit, "Rename Class", 1);

		RefactoringCommit renameRefactoring = refactoringCommitList.stream().filter(refactoringCommit ->
				refactoringCommit.getCommit().equals(renameCommit)).findFirst().get();

		//TODO: figure out what to expect here
		ProcessMetrics metrics = new ProcessMetrics(1, 5, 0, 1, 0, 1, 1.0, 0, 0);
		assertProcessMetrics(renameRefactoring, metrics);

		String extractCommit = "515365875143aa84b5bbb5c3191e7654a942912f";
		assertRefactoring(refactoringCommitList, extractCommit, "Extract Class", 1);

		RefactoringCommit extractClassRefactoring = (RefactoringCommit) filterCommit(refactoringCommitList, extractCommit).get(0);
		//TODO: figure out what to expect here
		metrics = new ProcessMetrics(0, 1, 3, 1, 0, 1, 0, 0, 1);
		//assertProcessMetrics(extractClassRefactoring, metrics);
	}

	@Test
	public void isSubclass() {
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits();
		Assert.assertEquals(2, refactoringCommitList.size());

		List<RefactoringCommit> areSubclasses = refactoringCommitList.stream().filter(refactoringCommit ->
				refactoringCommit.getClassMetrics().isInnerClass() &&
						refactoringCommit.getClassName().equals("org.apache.commons.cli.HelpFormatter.StringBufferComparator")).collect(Collectors.toList());
		List<RefactoringCommit> areNoSubclasses = refactoringCommitList.stream().filter(yes -> !yes.getClassMetrics().isInnerClass()).collect(Collectors.toList());

		Assert.assertEquals(0, areSubclasses.size());
		Assert.assertEquals(2, areNoSubclasses.size());
	}

	@Test
	public void stable() {
		// there are no instances of stable variables, as the repo is too small
		List<StableCommit> stableCommitList = getStableCommits();
		Assert.assertEquals(0, stableCommitList.size());
	}

	@Test
	public void commitMetaData(){
		String commit = "bc15aee7cfaddde19ba6fefe0d12331fe98ddd46";
		assertMetaDataRefactoring(
				commit,
				"rename class",
				"Rename Class\tPerson renamed to People",
				"@local/repos/toyrepo-r2/" + commit,
				"d56acf6b23d646528b4b04779b0fe64d74811052");
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
