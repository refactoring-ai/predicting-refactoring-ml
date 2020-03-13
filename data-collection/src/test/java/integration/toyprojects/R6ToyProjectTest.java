package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.RefactoringCommit;
import refactoringml.db.StableCommit;

import java.util.List;

// tests related to PR #128: https://github.com/refactoring-ai/predicting-refactoring-ml/pull/128
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R6ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "https://github.com/refactoring-ai/toyrepo-r6.git";
	}

	// normal refactoring stores just one data point
	@Test
	void normalRefactoring() {

		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits();

		// 4125d9a212381d132e47c1d53b8dbb0b0a7eb7f3
		System.out.println(refactoringCommitList);
	}

}