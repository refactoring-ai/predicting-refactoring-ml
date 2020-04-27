package integration.canaryprojects;

import integration.IntegrationBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.RefactoringCommit;
import java.util.List;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FluentIntegrationTest extends IntegrationBaseTest {
	@Override
	protected String getRepo() {
		return "https://github.com/FluentLenium/FluentLenium.git";
	}

	@Test
	public void moveSourceRefactoring(){
		String moveSource = "85c68373dabe32334933bdf6e67091534fc1504a";
		assertRefactoring(getRefactoringCommits(), moveSource, "Move Class", 33);

		//test the refactorings for a specific file
		List<RefactoringCommit> moveRefactorings = (List<RefactoringCommit>) filterCommit(getRefactoringCommits(), moveSource).stream().filter(
				commit -> commit.getClassName().contains("/FluentTest.java"))
				.collect(Collectors.toList());
		assertRefactoring(moveRefactorings, moveSource, "Move Class", 1);
		assertRefactoring(moveRefactorings, moveSource, "Pull Up Method", 5);
	}
}