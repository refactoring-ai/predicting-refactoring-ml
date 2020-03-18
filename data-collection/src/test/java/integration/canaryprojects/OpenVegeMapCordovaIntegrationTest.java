package integration.canaryprojects;

import integration.IntegrationBaseTest;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OpenVegeMapCordovaIntegrationTest extends IntegrationBaseTest {
    @Override
    protected String getRepo() {
        return "https://github.com/Rudloff/openvegemap-cordova.git";
    }
}