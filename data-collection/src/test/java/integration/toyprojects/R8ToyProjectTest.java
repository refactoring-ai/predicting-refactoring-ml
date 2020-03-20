package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.RefactoringCommit;

import java.util.List;

// tests related to PR #144: https://github.com/refactoring-ai/predicting-refactoring-ml/issues/144
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R8ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "https://github.com/refactoring-ai-testing/toyrepo-r8.git";
	}

	@Test
	void t1() {
		List<RefactoringCommit> refactorings = getRefactoringCommits();
		Assertions.assertEquals(1, refactorings.size());

		Assertions.assertEquals("Rename Variable", refactorings.get(0).getRefactoring());

	}
}