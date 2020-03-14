package integration.canaryprojects;

import integration.IntegrationBaseTest;
import org.junit.Ignore;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
public class GreenBitsIntegrationTest extends IntegrationBaseTest {


	@Override
	protected String getRepo() {
		return "https://www.github.com/greenaddress/GreenBits.git";
	}

	// debug commit 4af0169539667e66cff4fdd9a359a062508bb0f5
	// 2020-03-14 12:56:05 ERROR RefactoringAnalyzer:96 Refactoring miner found a refactoring for a newly introduced class file on commit: 4af0169539667e66cff4fdd9a359a062508bb0f5 for new class file: app/src/main/java/com/greenaddress/greenbits/ui/preferences/TwoFactorPreferenceFragment.java
	@Test @Ignore
	void t1() {

	}


}
