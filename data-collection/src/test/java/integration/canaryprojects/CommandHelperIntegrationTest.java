package integration.canaryprojects;

import integration.IntegrationBaseTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommandHelperIntegrationTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() { return "https://github.com/EngineHub/CommandHelper"; }

	@Override
	protected String getFirstCommit() {
		return "7df407fcf529316f0087ca77043ab37a7a3cf367";
	}

	@Test
	void t1() { }
}