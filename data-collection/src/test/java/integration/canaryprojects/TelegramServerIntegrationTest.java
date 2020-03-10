package integration.canaryprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TelegramServerIntegrationTest extends IntegrationBaseTest {
	@Override
	protected String getStableCommitThreshold() {return "15,25,50";}

	@Override
	protected String getLastCommit() {
		return "52cc3cf6276d2929aaea904a27e24babb3a1056e";
	}

	@Override
	protected String getRepo() {
		return "https://github.com/aykutalparslan/Telegram-Server.git";
	}

	@Test
	public void uniqueRefactorings(){
		int uniqueRefactorings = getRefactoringCommits().stream().map(commit -> commit.getRefactoringSummary()).distinct().collect(Collectors.toList()).size();
		Assert.assertEquals(729, uniqueRefactorings);
	}

	@Test
	public void projectMetrics() {
		assertProjectMetrics(1391, 1379, 12, 56002, 55488, 514);
	}
}