package integration.realprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.RefactoringCommit;
import refactoringml.db.StableCommit;

import java.util.List;
import java.util.stream.Collectors;

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

    @Test
    public void t1() {



    }
}