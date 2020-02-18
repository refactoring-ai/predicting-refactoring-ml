package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.No;
import refactoringml.db.ProcessMetrics;
import refactoringml.db.Yes;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R4ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "https://github.com/jan-gerling/toyrepo-r4.git";
	}

	@Test
	public void yes() {
		List<Yes> yesList = session.createQuery("From Yes where project = :project order by commitMetaData.commitDate desc")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(6, yesList.size());
	}

	@Test
	public void refactoringDetails(){
		String extractCommit = "dd9aa00b03c9456c69c5e6566040fb994d7c9d98";
		String renameCommit = "104e39574462f9e4bd6b1cdf388ecd0334a6f2c3";
		List<Yes> yesList = session.createQuery("From Yes where project = :project and (commitMetaData.commitId = :extractCommit or commitMetaData.commitId = :renameCommit)")
				.setParameter("project", project)
				.setParameter("extractCommit", extractCommit)
				.setParameter("renameCommit", renameCommit)
				.list();

		Yes extractYes = yesList.stream().filter(yes -> yes.getCommit().equals(extractCommit)).findFirst().get();
		Assert.assertEquals("a.Animal.Dog", extractYes.getClassName());
		assertRefactoring(yesList, extractCommit, "Extract Method", 1);

		Yes renameYes = yesList.stream().filter(yes -> yes.getCommit().equals(renameCommit)).findFirst().get();
		Assert.assertEquals("Rename Class", renameYes.getRefactoring());
		assertRefactoring(yesList, renameCommit, "Rename Class", 2);
	}

	@Test
	public void isSubclass(){
		List<Yes> yesList = session.createQuery("From Yes where project = :project order by commitMetaData.commitDate asc")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(6, yesList.size());

		assertRefactoring(yesList, "dd9aa00b03c9456c69c5e6566040fb994d7c9d98", "Extract Method", 1);
		Assertions.assertEquals("a.Animal.Dog", yesList.get(0).getClassName());
		Assertions.assertTrue(yesList.get(0).getClassMetrics().isInnerClass());

		assertRefactoring(yesList, "d3b912566712bdeda096c60a8887dd96b76ceb7b", "Rename Method", 1);
		Assertions.assertEquals("a.Pets.CanisLupusFamiliaris", yesList.get(5).getClassName());
	}

	@Test
	public void no() {
		// there are no instances of no variables, as the repo is too small
		List<No> noList = session.createQuery("From No where project = :project")
				.setParameter("project", project)
				.list();
		Assert.assertEquals(0, noList.size());
	}

	/*
	Test various cases of class and file renames, to check if they are detected correctly and metrics are tracked.
	Between the two commits: rename class and full rename, the file name of the affected class was changed as well in one commit.
	 */
	@Test
	public void classRenames(){
		List<Yes> yesList = session.createQuery("From Yes where project = :project and refactoring = :refactoring order by commitMetaData.commitDate desc")
				.setParameter("project", project)
				.setParameter("refactoring", "Rename Class")
				.list();
		Assert.assertEquals(4, yesList.size());

		//two renames of subclasses in one commits
		String doubleRenameCommit = "104e39574462f9e4bd6b1cdf388ecd0334a6f2c3";
		//renamed only the class name without the filename
		String renameClass = "96443c0c80919970071acfbb9f2af6a99b1f41ac";
		//renamed both class name and filename
		String renameClassFull = "d801d80c03ff1268010bbb43cec43da4be233dfd";

		assertRefactoring(yesList, doubleRenameCommit, "Rename Class",2);
		assertRefactoring(yesList, renameClass, "Rename Class",1);
		assertRefactoring(yesList, renameClassFull, "Rename Class",1);

		//no check if the class metrics were tracked and set correct
		//TODO: Should the qtyOfCommits not be 3, as it is the 4th commit changing this file?
		ProcessMetrics doubleRenameMetrics1 = new ProcessMetrics(
				3,
				34,
				4,
				1,
				0,
				1,
				1.0,
				0,
				1
		);
		//assertProcessMetrics(filterCommit(yesList, doubleRenameCommit).get(0), doubleRenameMetrics1);
		ProcessMetrics doubleRenameMetrics2 = new ProcessMetrics(
				3,
				34,
				4,
				1,
				0,
				1,
				1.0,
				0,
				2
		);
//		assertProcessMetrics(filterCommit(yesList, doubleRenameCommit).get(1), doubleRenameMetrics2);

		ProcessMetrics renameClassMetrics = new ProcessMetrics(
				5,
				36,
				6,
				2,
				0,
				2,
				0.5,
				0,
				3
		);
		//assertProcessMetrics(filterCommit(yesList, renameClass).get(0), renameClassMetrics);
		//TODO: The author owner ship metric is a bit weird with 0.4, I would expect something like 0.33
		ProcessMetrics renameFullMetrics = new ProcessMetrics(
				6,
				37,
				7,
				3,
				0,
				3,
				0.4,
				0,
				4
		);
		//assertProcessMetrics(filterCommit(yesList, renameClassFull).get(0), renameFullMetrics);
	}

	@Test
	public void commitMetaData(){
		String commit = "dd9aa00b03c9456c69c5e6566040fb994d7c9d98";
		assertMetaDataYes(
				commit,
				"extract method",
				"Extract Method\tprivate print(a int, b int, c int) : void extracted from public bark() : void in class a.Animal.Dog",
				"@local/repos/toyrepo-r4/" + commit);
	}

	@Test
	public void metrics() {
		List<Yes> yesList = session.createQuery("From Yes where project = :project order by commitMetaData.commitDate desc")
				.setParameter("project", project)
				.list();
		ProcessMetrics methodExtract = new ProcessMetrics(
				0,
				0,
				0,
				0,
				0,
				0,
				0.0,
				0,
				0
		);
		//assertProcessMetrics(filterCommit(yesList, "dd9aa00b03c9456c69c5e6566040fb994d7c9d98").get(0), methodExtract);

		//TODO: The author owner ship metric is a bit weird with 0.5, I would expect something like 0.25
		ProcessMetrics methodRename = new ProcessMetrics(
				7,
				38,
				8,
				4,
				0,
				4,
				0.25,
				0,
				5
		);
//		assertProcessMetrics(filterCommit(yesList, "d3b912566712bdeda096c60a8887dd96b76ceb7b").get(0), methodRename);

		// the next two assertions come directly from a 'cloc .' in the project
		Assert.assertEquals(29, project.getJavaLoc());

		Assert.assertEquals(2, project.getNumberOfProductionFiles() + project.getNumberOfTestFiles());

		Assert.assertEquals(1, project.getNumberOfProductionFiles());

		Assert.assertEquals(1, project.getNumberOfTestFiles());

		Assert.assertEquals(21, project.getProductionLoc());
	}
}
