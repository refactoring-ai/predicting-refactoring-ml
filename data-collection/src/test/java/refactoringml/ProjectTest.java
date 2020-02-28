package refactoringml;

import org.junit.Assert;
import org.junit.Test;
import refactoringml.db.Project;
import refactoringml.util.Counter;

import java.util.Calendar;
import java.util.List;

public class ProjectTest {
    @Test
    public void constructor(){
        Counter.CounterResult counterResult = Counter.countProductionAndTestFiles("");
        Calendar time = Calendar.getInstance();

        Project projectTrueth = new Project("test", "test", "testName", time,
                0, List.of(3, 5, 6).toString(), "a", counterResult, 0);
        Assert.assertEquals(List.of(3, 5, 6), projectTrueth.getCommitCountThresholds());

        Project project = new Project("test", "test", "testName", time,
                0, List.of(6, 5, 3).toString(), "a", counterResult, 0);
        Assert.assertEquals(projectTrueth.toString(), project.toString());

        project = new Project("test", "test", "testName", time,
                0, "6, 3, 5   sf", "a", counterResult, 0);
        Assert.assertEquals(projectTrueth.toString(), project.toString());

        project = new Project("test", "test", "testName", time,
                0, "[6,3,5]", "a", counterResult, 0);
        Assert.assertEquals(projectTrueth.toString(), project.toString());
    }
}