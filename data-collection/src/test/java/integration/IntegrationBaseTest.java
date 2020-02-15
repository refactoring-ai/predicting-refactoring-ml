package integration;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.refactoringminer.util.GitServiceImpl;
import refactoringml.App;
import refactoringml.TrackDebugMode;
import refactoringml.db.*;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static refactoringml.util.JGitUtils.extractProjectNameFromGitUrl;

public abstract class IntegrationBaseTest {

	protected Database db;
	protected String outputDir;
	protected SessionFactory sf;
	protected String tmpDir;
	protected Project project;
	protected Session session;

	@BeforeAll
	protected void runApp() throws Exception {
		sf = new HibernateConfig().getSessionFactory(DataBaseInfo.URL, "root", DataBaseInfo.PASSWORD, drop());
		db = new Database(sf);
		outputDir = Files.createTempDir().getAbsolutePath();
		tmpDir = Files.createTempDir().getAbsolutePath();

		String repoLocalDir = "repos/" + extractProjectNameFromGitUrl(getRepo());
		boolean projectAlreadyCloned = new File(repoLocalDir).exists();
		if(!projectAlreadyCloned)
			new GitServiceImpl().cloneIfNotExists(repoLocalDir, getRepo());

		deleteProject(getRepo());

		if(track()!=null || commitTrack() != null) {
			TrackDebugMode.ACTIVE = true;
			TrackDebugMode.FILE_TO_TRACK = track();
			TrackDebugMode.COMMIT_TO_TRACK = commitTrack();
		}

		App app = new App("integration-test",
				repoLocalDir,
				outputDir,
				threshold(),
				db,
				getLastCommit(),
				false);

		project = app.run();
	}

	@AfterAll
	protected void afterApp() throws IOException {
		db.close();
		FileUtils.deleteDirectory(new File(tmpDir));
		FileUtils.deleteDirectory(new File(outputDir));
	}

	@BeforeEach
	void openSession() {
		this.session = sf.openSession();
	}

	@AfterEach
	void closeSession() {
		this.session.close();
		this.session = null;
	}

	protected void deleteProject(String repo1) {
		try {
			Session session = sf.openSession();


			List<Project> projects = (List<Project>) session.createQuery("from Project p where p.gitUrl = :gitUrl")
					.setParameter("gitUrl", repo1).list();

			if(!projects.isEmpty()) {
				session.beginTransaction();

				session.createQuery("delete from Yes y where project in :project")
						.setParameter("project", projects)
						.executeUpdate();
				session.createQuery("delete from No where project in :project")
						.setParameter("project", projects)
						.executeUpdate();

				projects.stream().forEach(session::delete);
				session.getTransaction().commit();
			}


			session.close();
		} catch(Exception e) {
			System.out.println("Could not delete the project before starting the test");
			e.printStackTrace();
		}
	}

	protected String commitTrack() {
		return null;
	}

	protected String track() {
		return null;
	}

	protected boolean drop() {
		return false;
	}

	protected String getLastCommit() {
		return null;
	}

	protected int threshold() {
		return 10;
	}

	protected abstract String getRepo();

	protected List<Yes> filterCommit(List<Yes> yesList, String commit){
		return yesList.stream().filter(yes -> yes.getRefactorCommit().equals(commit)).collect(Collectors.toList());
	}

	protected List<No> filterNoCommit(List<No> noList, String commit){
		return noList.stream().filter(no -> no.getCommit().equals(commit)).collect(Collectors.toList());
	}

	protected void assertRefactoring(List<Yes> yesList, String commit, String refactoring, int qty) {
		List<Yes> inCommit = filterCommit(yesList, commit);

		long count = inCommit.stream().filter(x -> x.getRefactoring().equals(refactoring)).count();
		Assert.assertEquals(qty, count);
	}

	protected void assertNoRefactoring(List<No> noList, String... commits) {
		Set<String> noCommits = noList.stream().map(x -> x.getCommit()).collect(Collectors.toSet());
		Set<String> assertCommits = Set.of(commits);

		Assert.assertEquals(noCommits, assertCommits);
	}

	protected void assertMetaDataYes (String commit, String commitMessage, String refactoringSummary, String commitUrl){
		Yes yes = (Yes) session.createQuery("From Yes where project = :project and refactorCommit = :refactorCommit ")
				.setParameter("project", project)
				.setParameter("refactorCommit", commit)
				.list().get(0);

		Assert.assertEquals(refactoringSummary, yes.getRefactoringSummary());
		Assert.assertEquals(commitMessage, yes.getCommitMessage());
		Assert.assertEquals(commitUrl, yes.getCommitUrl());
	}

	protected void assertMetaDataNo(String commit, String commitMessage, String commitUrl) {
		No no = (No) session.createQuery("From No where project = :project and commit = :refactorCommit ")
				.setParameter("project", project)
				.setParameter("commit", commit)
				.list().get(0);

		Assert.assertEquals(commitMessage, no.getCommitMessage());
		Assert.assertEquals(commitUrl, no.getCommitUrl());
	}


	protected void assertProcessMetrics(Yes yes, ProcessMetrics truth) {
		assertProcessMetrics(yes.getProcessMetrics(), truth);
	}

	protected void assertProcessMetrics(No no, ProcessMetrics truth) {
		assertProcessMetrics(no.getProcessMetrics(), truth);
	}

	private void assertProcessMetrics(ProcessMetrics found, ProcessMetrics truth){
		Assert.assertEquals(truth.toString(), found.toString());
	}
}