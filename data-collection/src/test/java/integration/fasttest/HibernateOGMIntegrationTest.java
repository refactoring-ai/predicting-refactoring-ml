package integration.fasttest;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.util.List;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class HibernateOGMIntegrationTest extends IntegrationBaseTest {
	@Override
	protected String getStableCommitThreshold() {return "25,50";};

	@Override
	protected String getLastCommit() {
		return "52cc3cf6276d2929aaea904a27e24babb3a1056e";
	}

	@Override
	protected String getRepo() {
		return "https://github.com/hibernate/hibernate-ogm";
	}

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
		assertProjectMetrics(1602, 851, 751, 98286, 47007, 51279);
	}
}