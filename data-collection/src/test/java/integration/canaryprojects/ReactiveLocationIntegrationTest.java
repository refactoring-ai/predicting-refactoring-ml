package integration.canaryprojects;

import integration.IntegrationBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

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
	public void projectMetrics() {
		assertProjectMetrics(42, 42, 0, 2125, 2125, 0);
	}
}