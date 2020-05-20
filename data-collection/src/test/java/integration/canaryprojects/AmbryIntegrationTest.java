package integration.canaryprojects;

import integration.IntegrationBaseTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
public class AmbryIntegrationTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "https://github.com/linkedin/ambry.git";
	}

	@Test
	void t1() {

	}
}
