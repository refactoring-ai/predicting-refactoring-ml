package integration.canaryprojects;

import integration.IntegrationBaseTest;
import org.junit.Ignore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AsyncHttpClientIntegrationTest extends IntegrationBaseTest {
    @Override
    protected String getLastCommit() {
        return "492cb67b9b8f9efab3760fd1b4a83e7467d758e1";
    }

    @Override
    protected String getRepo() {
        return "https://github.com/AsyncHttpClient/async-http-client.git";
    }

    @Override
    protected String getStableCommitThreshold() {return "10";};

    @Test @Ignore
    public void t1() {



    }
}