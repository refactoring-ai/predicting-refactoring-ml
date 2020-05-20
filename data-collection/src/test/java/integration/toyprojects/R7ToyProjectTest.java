package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.RefactoringCommit;
import java.util.List;

// tests related to PR #144: https://github.com/refactoring-ai/predicting-refactoring-ml/issues/144
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R7ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "https://github.com/refactoring-ai-testing/toyrepo-r7.git";
	}

	@Test
	void t1() {
		List<RefactoringCommit> refactorings = getRefactoringCommits();
		Assertions.assertEquals(6, refactorings.size());

		Assertions.assertTrue(refactorings.stream().anyMatch(x -> x.getRefactoring().equals("Extract Interface")));
	}

	@Test
	public void MoveRefactoring(){
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits();

		String moveRefactoring1 = "dce3865b05fe0b6e1db8e23f17dec498018d3f2f";
		assertRefactoring(refactoringCommitList, moveRefactoring1, "Move Class", 1);

		String moveRefactoring2 = "9cc7c77cfd38210eb44a67adda0545c0b6655017";
		assertRefactoring(refactoringCommitList, moveRefactoring2, "Move Class", 1);

		String moveRefactoring3 = "a0f7fbbfb859a23027705d2fdb12291795752736";
		assertRefactoring(refactoringCommitList, moveRefactoring3, "Move Class", 1);
	}

	@Test
	public void methodInvocations(){
		String commit = "dce3865b05fe0b6e1db8e23f17dec498018d3f2f";
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits();
		RefactoringCommit lastCommit = refactoringCommitList.stream().filter(refactoringCommit ->
				refactoringCommit.getCommit().equals(commit) && refactoringCommit.getFilePath().endsWith("A.java") && refactoringCommit.getRefactoring().equals("Extract Method")).findFirst().get();
		Assert.assertEquals(1, lastCommit.getMethodMetrics().getMethodInvocationsQty());
		Assert.assertEquals(0, lastCommit.getMethodMetrics().getMethodInvocationsLocalQty());
		Assert.assertEquals(0, lastCommit.getMethodMetrics().getMethodInvocationsIndirectLocalQty());
	}

	@Test
	public void ProcessMetrics(){
		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits();
		String renameVariable = "dce3865b05fe0b6e1db8e23f17dec498018d3f2f";
		RefactoringCommit renameCommit = (RefactoringCommit) filterCommit(refactoringCommitList, renameVariable).get(0);
		assertProcessMetrics(renameCommit, "ProcessMetrics{qtyOfCommits=1, linesAdded=14, linesDeleted=0, qtyOfAuthors=1, qtyMinorAuthors=0, qtyMajorAuthors=1, authorOwnership=1.0, bugFixCount=0, refactoringsInvolved=0}");

		String extractInterface = "a0f7fbbfb859a23027705d2fdb12291795752736";
		RefactoringCommit extractCommit = (RefactoringCommit) filterCommit(refactoringCommitList, extractInterface).get(0);
		assertProcessMetrics(extractCommit, "ProcessMetrics{qtyOfCommits=4, linesAdded=55, linesDeleted=1, qtyOfAuthors=1, qtyMinorAuthors=0, qtyMajorAuthors=1, authorOwnership=1.0, bugFixCount=0, refactoringsInvolved=4}");
	}
}