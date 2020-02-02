package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.hibernate.Session;
import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.No;
import refactoringml.db.Yes;

import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R1ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "repos/r1";
	}

	@Test
	public void t1() {

		Session session = sf.openSession();

		List<No> noList = session.createQuery("From No where project = :project")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(0, noList.size());

		List<Yes> yesList = session.createQuery("From Yes where project = :project order by refactoringDate desc")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(3, yesList.size());

		Assert.assertEquals("Extract Method", yesList.stream().filter(x -> x.getRefactorCommit().equals("e8895b22847c7c54a9e187f9f674db274e6bc103")).findFirst().get().getRefactoring());
		Assert.assertEquals("Rename Variable", yesList.stream().filter(x -> x.getRefactorCommit().equals("04ae2289e4f788c9d53594f85262c0715b3e257b")).findFirst().get().getRefactoring());
		Assert.assertEquals("Inline Method", yesList.stream().filter(x -> x.getRefactorCommit().equals("21151bf7e36da52b9305d99755eb6f0b7616e620")).findFirst().get().getRefactoring());


	}
}
