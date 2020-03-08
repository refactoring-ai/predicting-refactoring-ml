package integration;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.refactoringminer.util.GitServiceImpl;
import refactoringml.App;
import refactoringml.TrackDebugMode;
import refactoringml.db.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static refactoringml.util.FileUtils.createTmpDir;
import static refactoringml.util.JGitUtils.extractProjectNameFromGitUrl;
import static refactoringml.util.PropertiesUtils.setProperty;

public abstract class IntegrationBaseTest {

	protected Database db;
	protected String outputDir;
	protected SessionFactory sf;
	protected String tmpDir;
	protected Project project;
	protected Session session;

	private List<RefactoringCommit> refactoringCommits;
	private List<StableCommit> stableCommits;

	protected String trackCommit() {
		return "null";
	}

	protected String trackFileName() {
		return "null";
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
		sf = new HibernateConfig().getSessionFactory(DataBaseInfo.URL, DataBaseInfo.USER, DataBaseInfo.PASSWORD, drop());
		db = new Database(sf);
		outputDir = createTmpDir();
		tmpDir = createTmpDir();

		String repoLocalDir = "repos/" + extractProjectNameFromGitUrl(getRepo());
		boolean projectAlreadyCloned = new File(repoLocalDir).exists();
		if(!projectAlreadyCloned)
			new GitServiceImpl().cloneIfNotExists(repoLocalDir, getRepo());

		deleteProject(extractProjectNameFromGitUrl(getRepo()));

		if(!trackFileName().equals("null") || !trackCommit().equals("null")) {
			TrackDebugMode.ACTIVE = true;
			TrackDebugMode.FILENAME_TO_TRACK = trackFileName();
			TrackDebugMode.COMMIT_TO_TRACK = trackCommit();
		}

		//set the stableCommitThreshold in the PMDatabase to test various configs
		setProperty("stableCommitThresholds", getStableCommitThreshold());

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

	private List<Long> getIds(String metricsName, List<Long> projectIds){
		List<Long> ids = (List<Long>) session.createQuery(String.format("SELECT r.%s.id FROM RefactoringCommit r WHERE r.project.id IN :projectIds", metricsName))
				.setParameter("projectIds", projectIds).list();
		ids.addAll((List<Long>) session.createQuery(String.format("SELECT s.%s.id FROM StableCommit s WHERE s.project.id IN :projectIds", metricsName))
				.setParameter("projectIds", projectIds).list());
		return ids;
	}

	private void deleteMetrics(String tableName, List<Long> ids){
		if(!ids.isEmpty()){
			session.createQuery(String.format("DELETE FROM %s WHERE id IN :ids", tableName))
					.setParameter("ids", ids)
					.executeUpdate();
		}
	}

	protected void deleteProject(String repository) {
		try {
			session = sf.openSession();

			List<Project> projects = (List<Project>) session.createQuery("FROM Project WHERE projectName = :projectName")
					.setParameter("projectName", repository).list();
			List<Long> projectIds = projects.stream().map(Project::getId).collect(Collectors.toList());

			if(!projectIds.isEmpty()) {
				List<Long> metaData = getIds("commitMetaData", projectIds);
				List<Long> classMetrics = getIds("classMetrics", projectIds);
				List<Long> methodMetrics = getIds("methodMetrics", projectIds);
				List<Long> variableMetrics = getIds("variableMetrics", projectIds);
				List<Long> fieldMetrics = getIds("fieldMetrics", projectIds);
				List<Long> processMetrics = getIds("processMetrics", projectIds);

				session.beginTransaction();
				session.createQuery("DELETE FROM RefactoringCommit WHERE project.id IN :projectIds")
						.setParameter("projectIds", projectIds)
						.executeUpdate();
				session.createQuery("DELETE FROM StableCommit WHERE project.id IN :projectIds")
						.setParameter("projectIds", projectIds)
						.executeUpdate();

				deleteMetrics("ClassMetric", classMetrics);
				deleteMetrics("MethodMetric", methodMetrics);
				deleteMetrics("CommitMetaData", metaData);
				deleteMetrics("VariableMetric", variableMetrics);
				deleteMetrics("FieldMetric", fieldMetrics);
				deleteMetrics("ProcessMetrics", processMetrics);

				projects.stream().forEach(session::delete);
				session.getTransaction().commit();
			}

			session.close();
		} catch(Exception e) {
			System.out.println("Could not delete the project before starting the test");
			e.printStackTrace();
		}
	}

	//Get all RefactoringCommits from the DB as a List, use this instead of a custom query
	protected List<RefactoringCommit> getRefactoringCommits(){
		if(refactoringCommits != null)
			return refactoringCommits;

		this.session = sf.openSession();
		refactoringCommits = session.createQuery("From RefactoringCommit where project = :project order by id asc")
				.setParameter("project", project)
				.list();
		this.session.close();
		this.session = null;
		return refactoringCommits;
	}

	//Get all StableCommits from the DB as a List, use this instead of a custom query
	protected List<StableCommit> getStableCommits(){
		if(stableCommits != null)
			return stableCommits;

		this.session = sf.openSession();
		stableCommits = session.createQuery("From StableCommit where project = :project order by id asc")
				.setParameter("project", project)
				.list();
		this.session.close();
		this.session = null;
		return stableCommits;
	}

	//Filter all commitInstances with the given commitHash
	protected List<? extends Instance> filterCommit(List<? extends Instance> commitList, String commitId){
		return commitList.stream().filter(commit -> commit.getCommit().equals(commitId)).collect(Collectors.toList());
	}

	//Test if all refactoring commits where found
	protected void assertRefactoring(List<RefactoringCommit> refactoringCommitList, String commit, String refactoring, int qty) {
		List<RefactoringCommit> inCommit = (List<RefactoringCommit>) filterCommit(refactoringCommitList, commit);

		long count = inCommit.stream().filter(x -> x.getRefactoring().equals(refactoring)).count();
		Assert.assertEquals(qty, count);
	}

	//Test if all stable commits where detected
	protected void assertStableCommit(List<StableCommit> stableCommitList, String... commits) {
		Set<String> stableCommits = stableCommitList.stream().map(x -> x.getCommit()).collect(Collectors.toSet());
		Set<String> assertCommits = Set.of(commits);

		Assert.assertEquals(commits.length, stableCommits.size());
		Assert.assertEquals(stableCommits, assertCommits);
	}

	protected void assertMetaDataRefactoring(String commit, String commitMessage, String refactoringSummary, String commitUrl, String parentCommit){
		RefactoringCommit refactoringCommit = (RefactoringCommit) filterCommit(getRefactoringCommits(), commit).get(0);

		Assert.assertEquals(refactoringSummary, refactoringCommit.getRefactoringSummary());
		assertMetaData(refactoringCommit.getCommitMetaData(), commitUrl, parentCommit, commitMessage);
	}

	protected void assertMetaDataStable(String commit, String commitUrl, String parentCommit, String commitMessage) {
		StableCommit stableCommit = (StableCommit) filterCommit(getStableCommits(), commit).get(0);

		assertMetaData(stableCommit.getCommitMetaData(), commitUrl, parentCommit, commitMessage);
	}

	private void assertMetaData(CommitMetaData commitMetaData, String commitUrl, String parentCommit, String commitMessage){
		Assert.assertEquals(commitUrl, commitMetaData.getCommitUrl());
		Assert.assertEquals(parentCommit, commitMetaData.getParentCommitId());
		Assert.assertEquals(commitMessage, commitMetaData.getCommitMessage());
	}

	protected void assertProcessMetrics(Instance instance, String truth) {
		Assert.assertEquals(truth, instance.getProcessMetrics().toString());
	}

	protected void assertInnerClass(List<? extends Instance> commitList, String commitId, String className, int qty){
		List<? extends Instance> filteredList = filterCommit(commitList, commitId).stream().filter(commit ->
				commit.getClassMetrics().isInnerClass() &&
						commit.getClassName().contains(className)).collect(Collectors.toList());
		Assert.assertEquals(qty, filteredList.size());
	}

	//Test if the project metrics are computed correctly
	protected void assertProjectMetrics(int javaFilesCount, int productionFilesCount, int testFilesCount,
										int javaLocCount, int productionLocCount, int testLocCount){
		Assert.assertEquals(productionFilesCount, project.getNumberOfProductionFiles());
		Assert.assertEquals(testFilesCount, project.getNumberOfTestFiles());
		Assert.assertEquals(javaFilesCount, project.getNumberOfProductionFiles() + project.getNumberOfTestFiles());

		Assert.assertEquals(testLocCount, project.getTestLoc());
		Assert.assertEquals(productionLocCount, project.getProductionLoc());
		Assert.assertEquals(javaLocCount, project.getJavaLoc());
	}

	//Test if all Refactorings were classified
	@Test
	public void refactoringLevel(){
		List<RefactoringCommit> refactoringCommitsNoLevel = getRefactoringCommits().stream().filter(refactoringCommit ->
				refactoringCommit.getLevel() < 0).collect(Collectors.toList());
		Assert.assertEquals(0, refactoringCommitsNoLevel.size());
	}

	@Test
	public void checkExceptions() throws FileNotFoundException {
		Assert.assertFalse(refactoringml.util.FileUtils.readFile("./logs_test/data-collection_ERROR.log").contains("Exception: "));
	}

	@Test
	public void checkCKMethodNotFound() throws FileNotFoundException {
		Assert.assertFalse(refactoringml.util.FileUtils.readFile("./logs_test/data-collection_ERROR.log").contains("CK did not find the refactored method:"));
	}

	@Test
	public void relevantCommitMetaData(){
		session = sf.openSession();
		List<String> allRelevantCommitIds = session.createQuery("SELECT DISTINCT r.commitMetaData.commitId FROM RefactoringCommit r").list();
		allRelevantCommitIds.addAll(session.createQuery("SELECT DISTINCT s.commitMetaData.commitId FROM StableCommit s").list());
		allRelevantCommitIds = allRelevantCommitIds.stream().distinct().collect(Collectors.toList());
		List<String> allCommitMetaDatas = session.createQuery("SELECT DISTINCT c.commitId From CommitMetaData c").list();
		session.close();
		session = null;

		Assert.assertEquals(allRelevantCommitIds.size(), allCommitMetaDatas.size());
	}
}