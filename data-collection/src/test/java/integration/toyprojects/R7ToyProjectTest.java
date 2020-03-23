package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.ProcessMetrics;
import refactoringml.db.RefactoringCommit;
import refactoringml.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static refactoringml.util.JGitUtils.extractProjectNameFromGitUrl;

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
	void processMetrics(){
		String moveRefactoring = "dce3865b05fe0b6e1db8e23f17dec498018d3f2f";
		RefactoringCommit moveRefactoringCommit = (RefactoringCommit) filterCommit(getRefactoringCommits(), moveRefactoring).get(0);
		assertProcessMetrics(moveRefactoringCommit, ProcessMetrics.toString(1, 14, 0, 1, 0, 1, 1.0, 0, 0));

		String moveRefactoring02 = "9cc7c77cfd38210eb44a67adda0545c0b6655017";
		RefactoringCommit moveRefactoringCommit02 = (RefactoringCommit) filterCommit(getRefactoringCommits(), moveRefactoring02).get(0);
		assertProcessMetrics(moveRefactoringCommit02, ProcessMetrics.toString(2, 28, 14, 1, 0, 1, 1.0, 0, 1));

		String lastRefactoring = "a0f7fbbfb859a23027705d2fdb12291795752736";
		RefactoringCommit lastRefactoringCommit = (RefactoringCommit) filterCommit(getRefactoringCommits(), lastRefactoring).get(0);
		assertProcessMetrics(lastRefactoringCommit, ProcessMetrics.toString(4, 34, 17, 1, 0, 1, 1.0, 0, 5));
	}
}