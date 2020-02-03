package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.No;
import refactoringml.db.Yes;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R2ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "repos/r2";
	}

	@Test
	public void yes() {

		List<Yes> yesList = session.createQuery("From Yes where project = :project order by refactoringDate desc")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(1, yesList.size());

		Assert.assertEquals("Rename Class", yesList.stream().filter(x -> x.getRefactorCommit().equals("bc15aee7cfaddde19ba6fefe0d12331fe98ddd46")).findFirst().get().getRefactoring());

//		Assert.assertEquals("Extract Method", yesList.stream().filter(x -> x.getRefactorCommit().equals("e8895b22847c7c54a9e187f9f674db274e6bc103")).findFirst().get().getRefactoring());
//		Assert.assertEquals("Rename Variable", yesList.stream().filter(x -> x.getRefactorCommit().equals("04ae2289e4f788c9d53594f85262c0715b3e257b")).findFirst().get().getRefactoring());
//		Assert.assertEquals("Inline Method", yesList.stream().filter(x -> x.getRefactorCommit().equals("21151bf7e36da52b9305d99755eb6f0b7616e620")).findFirst().get().getRefactoring());

	}

	@Test
	public void no() {
		// there are no instances of no variables, as the repo is too small
		List<No> noList = session.createQuery("From No where project = :project")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(0, noList.size());
	}
}
