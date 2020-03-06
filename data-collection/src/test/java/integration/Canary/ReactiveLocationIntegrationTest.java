package integration.canary;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import java.util.List;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReactiveLocationIntegrationTest extends IntegrationBaseTest {
	@Override
	protected String getStableCommitThreshold() {return "10";};

	@Override
	protected String getLastCommit() {
		return "8dce277e49540767965b70502567af93593de994";
	}

	@Override
	protected String getRepo() {
		return "https://github.com/mcharmas/Android-ReactiveLocation.git";
	}

	@Override
	protected String trackFileName() { return "src/java/org/apache/commons/cli/HelpFormatter.java"; }

	@Test
	public void relevantCommitMetaData(){
		session = sf.openSession();
		List<String> allRelevantCommitIds = session.createQuery("SELECT DISTINCT r.commitMetaData.commitId FROM RefactoringCommit r").list();
		allRelevantCommitIds.addAll(session.createQuery("SELECT DISTINCT s.commitMetaData.commitId FROM StableCommit s").list());
		allRelevantCommitIds = allRelevantCommitIds.stream().distinct().collect(Collectors.toList());
		List<String> allCommitMetaDatas = session.createQuery("SELECT DISTINCT c.commitId From CommitMetaData c").list();
		session.close();
		session = null;

		Assert.assertEquals(allRelevantCommitIds.size(), allCommitMetaDatas.size());
	}

	@Test
	public void projectMetrics() {
		assertProjectMetrics(42, 42, 0, 2125, 2125, 0);
	}
}