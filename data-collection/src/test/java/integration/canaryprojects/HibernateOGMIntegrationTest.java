package integration.canaryprojects;

import integration.IntegrationBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

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
	public void projectMetrics() {
		assertProjectMetrics(1602, 851, 751, 98286, 47007, 51279);
	}
}