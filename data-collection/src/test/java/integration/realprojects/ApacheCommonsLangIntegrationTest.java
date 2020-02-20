package integration.realprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.StableCommit;
import refactoringml.db.RefactoringCommit;

import java.util.List;
import java.util.stream.Collectors;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled // still need to manually validate this one
public class ApacheCommonsLangIntegrationTest extends IntegrationBaseTest {
    @Override
    protected String getLastCommit() {
        return "2ea44b2adae8da8e3e7f55cc226479f9431feda9";
    }

    @Override
    protected String getRepo() {
        return "https://www.github.com/apache/commons-lang.git";
    }

    @Override
    protected String track() {
        return "src/java/org/apache/commons/lang/builder/HashCodeBuilder.java";
    }

    @Override
    protected String getStableCommitThreshold() {return "10";};

    // this test checks the Rename Method that has happened in #5e7d64d6b2719afb1e5f4785d80d24ac5a19a782,
    // method isSet
    @Test
    public void t1() {
        // manually verified
        RefactoringCommit instance1 = getRefactoringCommits().stream().filter(commit ->
                commit.getCommit().equals("5e7d64d6b2719afb1e5f4785d80d24ac5a19a782") &&
                        commit.getRefactoring().equals("Extract Method") &&
                        commit.getMethodMetrics().getFullMethodName().equals("isSameDay/2[Date,Date]")
        ).collect(Collectors.toList()).get(0);

        Assert.assertNotNull(instance1);

        Assert.assertEquals("isSameDay/2[Date,Date]", instance1.getMethodMetrics().getFullMethodName());
        Assert.assertEquals(2, instance1.getMethodMetrics().getMethodVariablesQty());
        Assert.assertEquals(1, instance1.getMethodMetrics().getMethodMaxNestedBlocks());
        Assert.assertEquals(1, instance1.getMethodMetrics().getMethodReturnQty());
        Assert.assertEquals(0, instance1.getMethodMetrics().getMethodTryCatchQty());
    }

    // this test follows the src/java/org/apache/commons/lang/builder/HashCodeBuilder.java file

    @Test
    public void t2() {
        List<StableCommit> stableCommitList = getStableCommits().stream().filter(commit ->
                commit.getFilePath().equals("src/java/org/apache/commons/lang/builder/HashCodeBuilder.java") && commit.getLevel() == 1).collect(Collectors.toList());
        // it has been through 9 different refactorings
        List<RefactoringCommit> refactoringCommitList = getRefactoringCommits().stream().filter(commit ->
                commit.getFilePath().equals("src/java/org/apache/commons/lang/builder/HashCodeBuilder.java")).collect(Collectors.toList());

        Assert.assertEquals(4, stableCommitList.size());
        Assert.assertEquals(8, refactoringCommitList.size());

        Assert.assertEquals("5c40090fecdacd9366bba7e3e29d94f213cf2633", stableCommitList.get(0).getCommit());

        // then, it was refactored two times (in commit 5c40090fecdacd9366bba7e3e29d94f213cf2633)
        Assert.assertEquals("5c40090fecdacd9366bba7e3e29d94f213cf2633", refactoringCommitList.get(0).getCommit());
        Assert.assertEquals("5c40090fecdacd9366bba7e3e29d94f213cf2633", refactoringCommitList.get(1).getCommit());

        // It appears 3 times
        Assert.assertEquals("379d1bcac32d75e6c7f32661b2203f930f9989df", stableCommitList.get(1).getCommit());
        Assert.assertEquals("d3c425d6f1281d9387f5b80836ce855bc168453d", stableCommitList.get(2).getCommit());
        Assert.assertEquals("3ed99652c84339375f1e6b99bd9c7f71d565e023", stableCommitList.get(3).getCommit());
    }

    // check the number of test and production files as well as their LOC
    @Test
    public void projectMetrics() {
        assertProjectMetrics(340, 161, 179, 78054, 28422, 49632);
    }
}