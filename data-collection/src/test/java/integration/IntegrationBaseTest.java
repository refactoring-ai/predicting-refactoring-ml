package integration;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.refactoringminer.util.GitServiceImpl;
import refactoringml.App;
import refactoringml.TrackDebugMode;
import refactoringml.db.*;
import refactoringml.util.FilePathUtils;

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

	private List<RefactoringCommit> refactoringCommits;
	private List<StableCommit> stableCommits;

	protected String commitTrack() {
		return null;
	}

	protected String track() {
		return null;
	}

	protected final boolean drop() {
		return false;
	}

	protected String getLastCommit() {
		return null;
	}

	protected abstract String getRepo();

	protected String getStableCommitThreshold() {return "50";};

	/*
	Test Behavior
	 */
	@BeforeAll
	protected void runApp() throws Exception {
		sf = new HibernateConfig().getSessionFactory(DataBaseInfo.URL, "root", DataBaseInfo.PASSWORD, drop());
		db = new Database(sf);
		outputDir = FilePathUtils.enforceUnixPaths(Files.createTempDir().getAbsolutePath());
		tmpDir = FilePathUtils.enforceUnixPaths(Files.createTempDir().getAbsolutePath());

		String repoLocalDir = "repos/" + extractProjectNameFromGitUrl(getRepo());
		boolean projectAlreadyCloned = new File(repoLocalDir).exists();
		if(!projectAlreadyCloned)
			new GitServiceImpl().cloneIfNotExists(repoLocalDir, getRepo());

		deleteProject(extractProjectNameFromGitUrl(getRepo()));

		if(track()!=null || commitTrack() != null) {
			TrackDebugMode.ACTIVE = false;
			TrackDebugMode.FILE_TO_TRACK = track();
			TrackDebugMode.COMMIT_TO_TRACK = commitTrack();
		}

		//set the stableCommitThreshold in the PMDatabase to test various configs
		App.setProperty("stableCommitThreshold", getStableCommitThreshold());

		App app = new App("integration-test",
				repoLocalDir,
				outputDir,
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

	protected void deleteProject(String repository) {
		try {
			Session session = sf.openSession();

			List<Project> projects = (List<Project>) session.createQuery("from Project p where p.projectName = :projectName")
					.setParameter("projectName", repository).list();

			if(!projects.isEmpty()) {
				session.beginTransaction();

				session.createQuery("delete from RefactoringCommit where project in :project")
						.setParameter("project", projects)
						.executeUpdate();
				session.createQuery("delete from StableCommit where project in :project")
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

	protected List<RefactoringCommit> getRefactoringCommits(){
		if(refactoringCommits != null)
			return refactoringCommits;

		this.session = sf.openSession();
		refactoringCommits = session.createQuery("From RefactoringCommit where project = :project order by commitMetaData.commitDate desc")
				.setParameter("project", project)
				.list();
		this.session.close();
		this.session = null;
		return refactoringCommits;
	}

	protected List<StableCommit> getStableCommits(){
		if(stableCommits != null)
			return stableCommits;

		this.session = sf.openSession();
		stableCommits = session.createQuery("From StableCommit where project = :project order by commitMetaData.commitDate desc")
				.setParameter("project", project)
				.list();
		this.session.close();
		this.session = null;
		return stableCommits;
	}

	protected List<? extends Instance> filterCommit(List<? extends Instance> commitList, String commitId){
		return commitList.stream().filter(commit -> commit.getCommit().equals(commitId)).collect(Collectors.toList());
	}

	protected void assertRefactoring(List<RefactoringCommit> refactoringCommitList, String commit, String refactoring, int qty) {
		List<RefactoringCommit> inCommit = (List<RefactoringCommit>) filterCommit(refactoringCommitList, commit);

		long count = inCommit.stream().filter(x -> x.getRefactoring().equals(refactoring)).count();
		Assert.assertEquals(qty, count);
	}

	protected void assertStableRefactoring(List<StableCommit> stableCommitList, String... commits) {
		Set<String> stableCommits = stableCommitList.stream().map(x -> x.getCommit()).collect(Collectors.toSet());
		Set<String> assertCommits = Set.of(commits);

		Assert.assertEquals(stableCommits, assertCommits);
	}

	protected void assertMetaDataRefactoring(String commit, String commitMessage, String refactoringSummary, String commitUrl){
		RefactoringCommit refactoringCommit = (RefactoringCommit) filterCommit(getRefactoringCommits(), commit).get(0);

		Assert.assertEquals(refactoringSummary, refactoringCommit.getRefactoringSummary());
		Assert.assertEquals(commitMessage, refactoringCommit.getCommitMessage());
		Assert.assertEquals(commitUrl, refactoringCommit.getCommitUrl());
	}

	protected void assertMetaDataStable(String commit, String commitUrl) {
		StableCommit stableCommit = (StableCommit) filterCommit(getStableCommits(), commit).get(0);

		Assert.assertEquals(commitUrl, stableCommit.getCommitUrl());
	}

	protected void assertProcessMetrics(Instance instance, ProcessMetrics truth) {
		Assert.assertEquals(truth.toString(), instance.getProcessMetrics().toString());
	}
}