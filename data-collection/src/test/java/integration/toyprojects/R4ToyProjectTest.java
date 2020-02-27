package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.Instance;
import refactoringml.db.StableCommit;
import refactoringml.db.ProcessMetrics;
import refactoringml.db.RefactoringCommit;
import java.util.List;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R4ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "https://github.com/jan-gerling/toyrepo-r4.git";
	}

	@Test
	public void refactorings() {
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits();
		Assert.assertEquals(6, refactoringCommitList.size());
	}

	@Test
	public void refactoringDetails(){
		String extractCommit = "dd9aa00b03c9456c69c5e6566040fb994d7c9d98";
		String renameCommit = "104e39574462f9e4bd6b1cdf388ecd0334a6f2c3";
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits().stream().filter(commit ->
						commit.getCommit().equals(extractCommit) ||
						commit.getCommit().equals(renameCommit)).collect(Collectors.toList());

		RefactoringCommit extractRefactoringCommit = refactoringCommitList.stream().filter(commit -> commit.getCommit().equals(extractCommit)).findFirst().get();
		Assert.assertEquals("a.Animal.Dog", extractRefactoringCommit.getClassName());
		assertRefactoring(refactoringCommitList, extractCommit, "Extract Method", 1);

		RefactoringCommit renameRefactoringCommit = refactoringCommitList.stream().filter(commit -> commit.getCommit().equals(renameCommit)).findFirst().get();
		Assert.assertEquals("Rename Class", renameRefactoringCommit.getRefactoring());
		assertRefactoring(refactoringCommitList, renameCommit, "Rename Class", 2);
	}

	@Test
	public void isSubclass(){
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits();
		Assert.assertEquals(6, refactoringCommitList.size());

		assertRefactoring(refactoringCommitList, "d3b912566712bdeda096c60a8887dd96b76ceb7b", "Rename Method", 1);
		Assertions.assertEquals("a.Pets.CanisLupusFamiliaris", filterCommit(refactoringCommitList, "d3b912566712bdeda096c60a8887dd96b76ceb7b").get(0).getClassName());

		assertRefactoring(refactoringCommitList, "dd9aa00b03c9456c69c5e6566040fb994d7c9d98", "Extract Method", 1);
		Assertions.assertEquals("a.Animal.Dog", refactoringCommitList.get(0).getClassName());
		Assertions.assertTrue(filterCommit(refactoringCommitList, "dd9aa00b03c9456c69c5e6566040fb994d7c9d98").get(0).getClassMetrics().isInnerClass());
	}

	@Test
	public void stable() {
		// there are no instances of stable variables, as the repo is too small
		Assert.assertEquals(0, getStableCommits().size());
	}

	/*
	Test various cases of class and file renames, to check if they are detected correctly and metrics are tracked.
	Between the two commits: rename class and full rename, the file name of the affected class was changed as well in one commit.
	 */
	@Test
	public void classRenames(){
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits().stream().filter(commit -> commit.getRefactoring().equals("Rename Class")).collect(Collectors.toList());
		Assert.assertEquals(4, refactoringCommitList.size());

		//two renames of subclasses in one commits
		String doubleRenameCommit = "104e39574462f9e4bd6b1cdf388ecd0334a6f2c3";
		//renamed only the class name without the filename
		String renameClass = "96443c0c80919970071acfbb9f2af6a99b1f41ac";
		//renamed both class name and filename
		String renameClassFull = "d801d80c03ff1268010bbb43cec43da4be233dfd";

		assertRefactoring(refactoringCommitList, doubleRenameCommit, "Rename Class",2);
		assertRefactoring(refactoringCommitList, renameClass, "Rename Class",1);
		assertRefactoring(refactoringCommitList, renameClassFull, "Rename Class",1);

		//no check if the class metrics were tracked and set correct
		String doubleRenameMetrics1 = ProcessMetrics.toString(
				2,
				34,
				4,
				1,
				0,
				1,
				1.0,
				0,
				1
		);
		Instance renameRefactoring1 = filterCommit(refactoringCommitList, doubleRenameCommit).stream()
				.filter(instance -> instance.getProcessMetrics().refactoringsInvolved == 1).collect(Collectors.toList()).get(0);
		assertProcessMetrics(renameRefactoring1, doubleRenameMetrics1);

		String doubleRenameMetrics2 = ProcessMetrics.toString(
				2,
				34,
				4,
				1,
				0,
				1,
				1.0,
				0,
				2
		);
		Instance renameRefactoring2 = filterCommit(refactoringCommitList, doubleRenameCommit).stream()
				.filter(instance -> instance.getProcessMetrics().refactoringsInvolved == 2).collect(Collectors.toList()).get(0);
		assertProcessMetrics(renameRefactoring2, doubleRenameMetrics2);

		String renameClassMetrics = ProcessMetrics.toString(
				4,
				36,
				6,
				2,
				0,
				2,
				0.5,
				0,
				3
		);
		assertProcessMetrics(filterCommit(refactoringCommitList, renameClass).get(0), renameClassMetrics);
		String renameFullMetrics = ProcessMetrics.toString(
				5,
				37,
				7,
				3,
				0,
				3,
				0.4,
				0,
				4
		);
		assertProcessMetrics(filterCommit(refactoringCommitList, renameClassFull).get(0), renameFullMetrics);
	}

	@Test
	public void commitMetaData(){
		String commit = "dd9aa00b03c9456c69c5e6566040fb994d7c9d98";
		assertMetaDataRefactoring(
				commit,
				"extract method",
				"Extract Method\tprivate print(a int, b int, c int) : void extracted from public bark() : void in class a.Animal.Dog",
				"@local/repos/toyrepo-r4/" + commit,
				"4bab290609c8d60e96ad2fa094793edc2cba023a");
	}

	@Test
	public void renames(){
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits();
		String methodExtract = ProcessMetrics.toString(
				1,
				26,
				0,
				1,
				0,
				1,
				1.0,
				0,
				0
		);
		assertProcessMetrics(filterCommit(refactoringCommitList, "dd9aa00b03c9456c69c5e6566040fb994d7c9d98").get(0), methodExtract);

		String methodRename = ProcessMetrics.toString(
				6,
				38,
				8,
				3,
				0,
				3,
				0.5,
				0,
				5
		);
		assertProcessMetrics(filterCommit(refactoringCommitList, "d3b912566712bdeda096c60a8887dd96b76ceb7b").get(0), methodRename);
	}

	@Test
	public void projectMetrics() {
		assertProjectMetrics(2, 1, 1, 29, 21, 8);
	}
}