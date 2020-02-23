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
	protected String getStableCommitThreshold() {return "10";};

	@Override
	protected String getLastCommit() {
		return "b9ccc94008c78a59695f0c77ebe4ecf284370956";
	}

	@Override
	protected String getRepo() {
		return "https://github.com/apache/commons-cli.git";
	}

	@Override
	protected String trackFileName() { return "src/java/org/apache/commons/cli/HelpFormatter.java"; }

	//Test the isInnerClass boolean for both RefactoringCommit and StableCommit .
	@Test
	public void isInnerClassRefactoring(){
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits().stream().filter(commit -> commit.getClassName().equals("org.apache.commons.cli.HelpFormatter")||
				commit.getClassName().equals("org.apache.commons.cli.HelpFormatter.StringBufferComparator")).collect(Collectors.toList());

		Assert.assertEquals(10, refactoringCommitList.size());
		List<RefactoringCommit> areInnerClassesInRefactorings = refactoringCommitList.stream().filter(commit ->
				commit.getClassMetrics().isInnerClass()
						&& commit.getClassName().equals("org.apache.commons.cli.HelpFormatter.StringBufferComparator")).collect(Collectors.toList());
		List<RefactoringCommit> areNotInnerClassesInRefactorings = refactoringCommitList.stream().filter(commit -> !commit.getClassMetrics().isInnerClass()).collect(Collectors.toList());

		Assert.assertEquals(1, areInnerClassesInRefactorings.size());
		Assert.assertEquals(9, areNotInnerClassesInRefactorings.size());
	}

	@Test
	public void isInnerClassStable(){
		//TODO:What to expect here?
		List<StableCommit> areInnerClassesInStable = getStableCommits().stream().filter(commit ->
				commit.getClassMetrics().isInnerClass()).collect(Collectors.toList());
		List<StableCommit> areNotInnerClassesInStable = getStableCommits().stream().filter(commit ->
				!commit.getClassMetrics().isInnerClass()).collect(Collectors.toList());

		Assert.assertEquals(67, areInnerClassesInStable.size());
		Assert.assertEquals(2484, areNotInnerClassesInStable.size());
		Assert.assertEquals(2527, getStableCommits().size());
	}

	//Test if the inner classes are tracked and marked correctly, with all details
	//This test requires multipleKs to work
	@Test
	public void isInnerClassDetails() {
		//org.apache.commons.cli.HelpFormatter:
		//Refactorings at (date asc):
		// c7127329dad2c5d6284532da09ddc0fdefd67436
		// 6bcbf153c4497c38c4c36c3a04c1eac1f4cc153b
		//
		// first commit afterwards: 9b2b8038b52be022e42c8c7fdf26e236888f94c5

		//ApacheCommonsCli - Stable Innerclasses - Validated on (36016e4023cb74cd1076dbd33e0d7a73a6a61993):
		assertInnerClass(
				getStableCommits(),
				"cd745ecf52fb2fe8fed1c67fc9149e4be11a73f0",
				"org.apache.commons.cli.HelpFormatter.OptionComparator",
				4);
		assertInnerClass(
				getStableCommits(),
				"6f972cf56d7a3054bac902fecb6d3dd5ee310dea",
				"org.apache.commons.cli.HelpFormatter.OptionComparator",
				4);
		assertInnerClass(
				getStableCommits(),
				"d1121d3ad3154e3564e150555dcedb368ad3aa94",
				"org.apache.commons.cli.HelpFormatter.OptionComparator",
				5);
		assertInnerClass(
				getStableCommits(),
				"1596f3bbe57986361da4ac1a23634dd5b00d10df",
				"org.apache.commons.cli.HelpFormatter.OptionComparator",
				4);
		assertInnerClass(
				getStableCommits(),
				"a955324468d45eb845e05107d5b0013285c3bc0a",
				"org.apache.commons.cli.HelpFormatter.OptionComparator",
				2);
		assertInnerClass(
				getStableCommits(),
				"faa6455a9a0bccf29d049f0b0958eb9b2e804fc3",
				"org.apache.commons.cli.Option.Builder",
				20);
		assertInnerClass(
				getStableCommits(),
				"cd745ecf52fb2fe8fed1c67fc9149e4be11a73f0",
				"org.apache.commons.cli.OptionTest.TestOption",
				5);
		assertInnerClass(
				getStableCommits(),
				"cd745ecf52fb2fe8fed1c67fc9149e4be11a73f0",
				"org.apache.commons.cli.OptionTest.DefaultOption",
				4);
		assertInnerClass(
				getStableCommits(),
				"aae50c585ec3ac33c6a9af792e80378904a73195",
				"org.apache.commons.cli.HelpFormatter.StringBufferComparator",
				1);
		//Not sure if this one should show up, as it is right before a refactoring commit: 6bcbf153c4497c38c4c36c3a04c1eac1f4cc153b
		assertInnerClass(
				getStableCommits(),
				"4c34483ea18c6a4f259f19a6d18637bb1fbae1e8",
				"org.apache.commons.cli.HelpFormatter.StringBufferComparator",
				1);
	}

	@Test
	public void commitMetaDataRefactoring(){
		String renameCommit = "04490af06faa8fd1be15da88172beb32218dd336";
		assertMetaDataRefactoring(
				renameCommit,
				"bug #11457: implemented fix, javadoc added to Option\n" +
						"\n" +
						"\n" +
						"git-svn-id: https://svn.apache.org/repos/asf/jakarta/commons/proper/cli/trunk@129803 13f79535-47bb-0310-9956-ffa450edef68",
				"Extract Variable\tkey : String in method package setOpt(opt Option) : void from class org.apache.commons.cli.CommandLine",
				"@local/repos/commons-cli/" + renameCommit,
				"469e71799a438ccb2d0925e50d4bb9dce37cdba2");

		String moveCommit = "347bbeb8f98a49744501ac50850457ba8751d545";
		assertMetaDataRefactoring(
				moveCommit,
				"refactored the option string handling, added property support for options with an argument value\n" +
						"\n" +
						"\n" +
						"git-svn-id: https://svn.apache.org/repos/asf/jakarta/commons/proper/cli/trunk@129846 13f79535-47bb-0310-9956-ffa450edef68",
				"Rename Parameter\topts : Options to options : Options in method public parse(options Options, arguments String[], stopAtNonOption boolean) : CommandLine in class org.apache.commons.cli.Parser",
				"@local/repos/commons-cli/" + moveCommit,
				"3b8e3de5b7599a6165d48103f94f3a830361188d");
	}

	@Test
	public void commitMetaDataStable(){
		String stableCommit1 = "aae50c585ec3ac33c6a9af792e80378904a73195";
		assertMetaDataStable(
				stableCommit1,
				"@local/repos/commons-cli/" + stableCommit1,
				"4868ac5e7c2afd428de74a6dcbec07dc6541a1ea",
				"moved cli over from the sandbox to commons proper\n" +
						"\n" +
						"\n" +
						"git-svn-id: https://svn.apache.org/repos/asf/jakarta/commons/proper/cli/trunk@129767 13f79535-47bb-0310-9956-ffa450edef68");

		String stableCommit2 = "745d1a535c9cf45d24455afc150b808981c8e0df";
		assertMetaDataStable(
				stableCommit2,
				"@local/repos/commons-cli/" + stableCommit2,
				"dde69934d7f0bee13e4cd1fc99a7d60ce95a0c78",
				"javadoc updates\n" +
						"\n" +
						"\n" +
						"git-svn-id: https://svn.apache.org/repos/asf/jakarta/commons/proper/cli/trunk@129805 13f79535-47bb-0310-9956-ffa450edef68");
	}

	// this test checks the Extract Method that has happened in #269eae18a911f792895d0402f5dd4e7913410523,
	// method getParsedOptionValue
	@Test
	public void refactoringDetected1() {
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
		List<StableCommit> allStableCommits = getStableCommits();

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

		// the file should appear twice as examples of 'stableCommit'
		assertStableCommit(stableCommitList, "5470bcaa9d75d73fb9c687fa13e12d642c75984f", "aae50c585ec3ac33c6a9af792e80378904a73195");
		// TODO: assertions related to the values of the StableCommit metrics, What to expect here?
		Assert.assertEquals(2599, allStableCommits.size());
	}

	@Test
	public void stableCommitsClasses(){
		List<String> noListUnique = getStableCommits().stream().map(instance -> instance.getClassName()).distinct().collect(Collectors.toList());
		Assert.assertEquals(65, noListUnique.size());

		/*
		TODO: evaluate of these files are rightfully missing in the stable commits?:
			"org.apache.commons.cli.AmbiguousOptionException"
			"org.apache.commons.cli.bug.BugCLI162Test"
			"org.apache.commons.cli.CommandLineParser"
			"org.apache.commons.cli.GnuParserTest"
			"org.apache.commons.cli.HelpFormatter.StringBufferComparator"
			"org.apache.commons.cli.OptionGroupTest"
			"org.apache.commons.cli.ParseException"
			"org.apache.commons.cli.PosixParserTest"
			"org.apache.commons.cli.ValueTest"
			"org.apache.commons.cli2.builder.SwitchBuilder"
			"org.apache.commons.cli2.commandline.WriteableCommandLineImpl"
			"org.apache.commons.cli2.CommandLineTestCase
			"org.apache.commons.cli2.option.OptionImpl"
			"org.apache.commons.cli2.validation.DateValidator"
			"org.apache.commons.cli2.validation.DateValidatorTest"
		 */
	}

	// test if test files are marked as tests, and production files are not
	@Test
	public void isTest() {
		// it has been through 9 different refactorings
		List<RefactoringCommit> yesListTests = getRefactoringCommits().stream().filter(refactoring -> refactoring.getIsTest()).collect(Collectors.toList());
		List<RefactoringCommit> yesListNoTests = getRefactoringCommits().stream().filter(refactoring -> !refactoring.getIsTest()).collect(Collectors.toList());
		Assert.assertEquals(0, yesListTests.size());
		Assert.assertEquals(0, yesListNoTests.size());

		List<StableCommit> stableCommitListTests =  getStableCommits().stream().filter(stable -> stable.getIsTest()).collect(Collectors.toList());
		List<StableCommit> stableCommitListNoTests =  getStableCommits().stream().filter(stable -> !stable.getIsTest()).collect(Collectors.toList());
		Assert.assertEquals(0, stableCommitListTests.size());
		Assert.assertEquals(0, stableCommitListNoTests.size());
	}

	@Test
	public void projectMetrics() {
		assertProjectMetrics(52, 23, 29, 7070, 2790, 4280);
	}
}