package integration.canaryprojects;

import integration.IntegrationBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.ProcessMetrics;
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
		assertRefactoring(getRefactoringCommits(), moveSource, "Move Source Dir", 46);
	}

	/*@Test
	public void moveSourceRefactoringPMs(){
		String moveSource = "85c68373dabe32334933bdf6e67091534fc1504a";
		List<RefactoringCommit> moveSourceCommit = filterCommit(getRefactoringCommits(), moveSource).stream().filter(commit ->
				commit.getClassName().contains("FluentTest.java")).collect(Collectors.toList());
		//assertProcessMetrics(moveSourceCommit, new ProcessMetrics().toString(10, 10, 10, 1, 1, 1, 1.0, 10, 10));
	}*/
}