package integration.canaryprojects;

import integration.IntegrationBaseTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

//Test if filePath are fixed #180
//Test if ProcessMetricsCollector.java:190 throws a nullpointer exception
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
public class OGMFermatIntegrationTest extends IntegrationBaseTest {
	@Override
	protected String getRepo() {
		return "https://github.com/Fermat-ORG/fermat";
	}

	@Override
	protected String getFirstCommit() {
		return "e871af70c2940a72b9908407c81c072095508e72";
	}

	@Test
	void t1() {

	}
}
