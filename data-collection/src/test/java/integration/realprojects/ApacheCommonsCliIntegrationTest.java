package integration.realprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.RefactoringCommit;
import refactoringml.db.StableCommit;

import java.util.List;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApacheCommonsCliIntegrationTest extends IntegrationBaseTest {

	@Override
	protected String getLastCommit() {
		return "b9ccc94008c78a59695f0c77ebe4ecf284370956";
	}

	@Override
	protected String getRepo() {
		return "https://github.com/apache/commons-cli.git";
	}

	@Override
	protected String track() {
		return "src/java/org/apache/commons/cli/Option.java";
	}

	protected int threshold() {
		return 10;
	}

	/*
	Test the isInnerClass boolean for both refactoricommit and stablecommit.
	 */
	@Test
	public void isInnerClass() {
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits().stream().filter(commit -> commit.getClassName().equals("org.apache.commons.cli.HelpFormatter")||
				commit.getClassName().equals("org.apache.commons.cli.HelpFormatter.StringBufferComparator")).collect(Collectors.toList());

		Assert.assertEquals(10, refactoringCommitList.size());
		List<RefactoringCommit> areInnerClassesInRefactorings = refactoringCommitList.stream().filter(commit ->
				commit.getClassMetrics().isInnerClass()
						&& commit.getClassName().equals("org.apache.commons.cli.HelpFormatter.StringBufferComparator")).collect(Collectors.toList());
		List<RefactoringCommit> areNotInnerClassesInRefactorings = refactoringCommitList.stream().filter(commit -> !commit.getClassMetrics().isInnerClass()).collect(Collectors.toList());

		Assert.assertEquals(1, areInnerClassesInRefactorings.size());
		Assert.assertEquals(9, areNotInnerClassesInRefactorings.size());

//		List<StableCommit> stableCommits = getStableCommits().stream().filter(commit -> commit.getClassName().equals("org.apache.commons.cli.HelpFormatter")||
//				commit.getClassName().equals("org.apache.commons.cli.HelpFormatter.StringBufferComparator")).collect(Collectors.toList());
//		Assert.assertEquals(927, stableCommits.size());
//
//		List<StableCommit> areInnerClassesInStable = stableCommits.stream().filter(commit ->
//				commit.getClassMetrics().isInnerClass()
//				&& commit.getClassName().equals("org.apache.commons.cli.HelpFormatter.StringBufferComparator")).collect(Collectors.toList());
//		List<StableCommit> areNotInnerClassesInStable = stableCommits.stream().filter(commit -> !commit.getClassMetrics().isInnerClass()).collect(Collectors.toList());
//
//		Assert.assertEquals(13, areInnerClassesInStable.size());
//		Assert.assertEquals(914, areNotInnerClassesInStable.size());
	}

	@Test
	public void commitMetaData(){
		//TODO: How to check the commit url without changing IntegrationBasetest, as the
		String renameCommit = "04490af06faa8fd1be15da88172beb32218dd336";
		assertMetaDataRefactoring(
				renameCommit,
				"bug #11457: implemented fix, javadoc added to Option\n" +
						"\n" +
						"\n" +
						"git-svn-id: https://svn.apache.org/repos/asf/jakarta/commons/proper/cli/trunk@129803 13f79535-47bb-0310-9956-ffa450edef68",
				"Extract Variable\tkey : String in method package setOpt(opt Option) : void from class org.apache.commons.cli.CommandLine",
				"@local/repos/commons-cli/" + renameCommit);

		String moveCommit = "347bbeb8f98a49744501ac50850457ba8751d545";
		assertMetaDataRefactoring(
				moveCommit,
				"refactored the option string handling, added property support for options with an argument value\n" +
						"\n" +
						"\n" +
						"git-svn-id: https://svn.apache.org/repos/asf/jakarta/commons/proper/cli/trunk@129846 13f79535-47bb-0310-9956-ffa450edef68",
				"Rename Parameter\topts : Options to options : Options in method public parse(options Options, arguments String[], stopAtNonOption boolean) : CommandLine in class org.apache.commons.cli.Parser",
				"@local/repos/commons-cli/" + moveCommit);

		// TODO: this is wrong, the id of the commit in a 'No' is the base commit, i.e., where the class started to become 'stable' for X commits
//		String stableCommit1 = "aae50c585ec3ac33c6a9af792e80378904a73195";
//		assertMetaDataNo(
//				stableCommit1,
//				"@local/repos/commons-cli/" + renameCommit);
//
//		String stableCommit2 = "745d1a535c9cf45d24455afc150b808981c8e0df";
//		assertMetaDataNo(
//				stableCommit2,
//				"@local/repos/commons-cli/" + renameCommit);
	}

	// this test checks the Extract Method that has happened in #269eae18a911f792895d0402f5dd4e7913410523,
	// method getParsedOptionValue
	@Test
	public void t1() {
		RefactoringCommit instance1 = getRefactoringCommits().stream().filter(commit ->
				commit.getCommit().equals("269eae18a911f792895d0402f5dd4e7913410523") &&
						commit.getRefactoring().equals("Extract Method") &&
						commit.getMethodMetrics().getFullMethodName().equals("getParsedOptionValue/1[String]")
		).collect(Collectors.toList()).get(0);

		Assert.assertNotNull(instance1);

		Assert.assertEquals("getParsedOptionValue/1[String]", instance1.getMethodMetrics().getFullMethodName());
		Assert.assertEquals(2, instance1.getMethodMetrics().getMethodVariablesQty());
		Assert.assertEquals(1, instance1.getMethodMetrics().getMethodMaxNestedBlocks());
		Assert.assertEquals(2, instance1.getMethodMetrics().getMethodReturnQty());
		Assert.assertEquals(0, instance1.getMethodMetrics().getMethodTryCatchQty());
	}

	// this test follows the src/java/org/apache/commons/cli/Option.java file
	// This test helped us to understand that we should not delete
	// RefactoringCommit where variableAppearances = -1, as this happens in newly introduced variables.
	@Test
	public void t2() {
		List<StableCommit> stableCommitList = getStableCommits().stream().filter(commit ->
				commit.getFilePath().equals("src/java/org/apache/commons/cli/Option.java")).collect(Collectors.toList());

		// it has been through 9 different refactorings
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits().stream().filter(commit ->
				commit.getFilePath().equals("src/java/org/apache/commons/cli/Option.java")).collect(Collectors.toList());

		Assert.assertEquals(9, refactoringCommitList.size());
		assertRefactoring(refactoringCommitList, "04490af06faa8fd1be15da88172beb32218dd336", "Extract Variable", 1);
		assertRefactoring(refactoringCommitList, "347bbeb8f98a49744501ac50850457ba8751d545", "Extract Class", 1);
		assertRefactoring(refactoringCommitList, "347bbeb8f98a49744501ac50850457ba8751d545", "Move Method", 3);
		assertRefactoring(refactoringCommitList, "5470bcaa9d75d73fb9c687fa13e12d642c75984f", "Extract Method", 2);
		assertRefactoring(refactoringCommitList, "97744806d59820b096fb502b1d51ca54b5d0921d", "Rename Method", 1);
		assertRefactoring(refactoringCommitList, "bfe6bd8634895645aa71d6a6dc668545297d7413", "Rename Parameter", 1);

		// the file should appear twice as examples of 'no'
		assertStableRefactoring(stableCommitList, "aae50c585ec3ac33c6a9af792e80378904a73195", "5470bcaa9d75d73fb9c687fa13e12d642c75984f");
		// TODO: assertions related to the values of the No metrics
	}

	// check the number of test and production files as well as their LOC
	@Test
	public void t3() {

		// the next two assertions come directly from a 'cloc .' in the project
		Assert.assertEquals(7070L, project.getJavaLoc());
		Assert.assertEquals(52L, project.getNumberOfProductionFiles() + project.getNumberOfTestFiles());

		// find . -name "*.java" | grep "/test/" | wc
		Assert.assertEquals(29, project.getNumberOfTestFiles());

		// 52 - 29
		Assert.assertEquals(23, project.getNumberOfProductionFiles());

		// cloc . --by-file | grep "/test/"
		Assert.assertEquals(4280, project.getTestLoc());

		// 7070 - 4280
		Assert.assertEquals(2790, project.getProductionLoc());

	}
}
