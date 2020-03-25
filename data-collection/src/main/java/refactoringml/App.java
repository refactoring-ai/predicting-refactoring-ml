package refactoringml;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import refactoringml.db.*;
import refactoringml.util.Counter;
import refactoringml.util.Counter.CounterResult;
import refactoringml.util.JGitUtils;
import refactoringml.util.RefactoringUtils;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import static refactoringml.util.FilePathUtils.enforceUnixPaths;
import static refactoringml.util.FilePathUtils.lastSlashDir;
import static refactoringml.util.FileUtils.createTmpDir;
import static refactoringml.util.FileUtils.newDir;
import static refactoringml.util.JGitUtils.*;
import static refactoringml.util.LogUtils.createErrorState;
import static refactoringml.util.PropertiesUtils.getProperty;

public class App {
	private static final Logger log = LogManager.getLogger(App.class);
	private String currentTempDir;

	//url of the project to analyze
	private String gitUrl;
	//if source code storage is activated it is stored here
	private String filesStoragePath;
	//handles the logic with the MYSQL db
	private Database db;
	//the last commit to process on the selected branch
	private String lastCommitToProcess;
	//Do you want to save the affected source code for each commit?
	private boolean storeFullSourceCode;
	//name of the dataset
	private String datasetName;
	//number of unhandled exceptions encountered during runtime, @WARN quite unreliable
	private int exceptionsCount = 0;
	//timeout in seconds for the refactoring miner
	private int refactoringMinerTimeout;
	//current commitId processed by the RefactoringMiner
	private String commitIdToProcess;
	//all by RefactoringMiner detected refactorings for the current commit
	private List<Refactoring> refactoringsToProcess;
	//the git repository is cloned to this path, to analyze it there
	private String clonePath;
	//main branch of the current repository, this one will be analyzed
	private String mainBranch;
	//Metrics about the current project
	private Project project;
	//JGit repository object for the current run
	private Repository repository;

	public App (String datasetName,
	            String gitUrl,
	            String filesStoragePath,
	            Database db, 
	            boolean storeFullSourceCode) {
		this(datasetName, gitUrl, filesStoragePath, db, null, storeFullSourceCode);
	}
	public App (String datasetName,
	            String gitUrl,
	            String filesStoragePath,
	            Database db,
	            String lastCommitToProcess,
	            boolean storeFullSourceCode) {

		this.datasetName = datasetName;
		this.gitUrl = gitUrl;
		this.filesStoragePath = enforceUnixPaths(filesStoragePath + extractProjectNameFromGitUrl(gitUrl)); // add project as subfolder
		this.db = db;
		this.lastCommitToProcess = lastCommitToProcess;
		this.storeFullSourceCode = storeFullSourceCode;

		// creates a temp dir to store the project
		currentTempDir = createTmpDir();
		clonePath = (Project.isLocal(gitUrl) ? gitUrl : currentTempDir + "repo").trim();
		this.refactoringMinerTimeout = Integer.parseInt(getProperty("timeoutRefactoringMiner"));
	}

	public Project run () throws Exception {
		// do not run if the project is already in the database
		if (db.projectExists(gitUrl)) {
			String message = String.format("Project %s already in the database", gitUrl);
			throw new IllegalArgumentException(message);
		}

		long startProjectTime = System.currentTimeMillis();
		// creates the directory in the storage
		if(storeFullSourceCode) {
			newDir(filesStoragePath);
		}

		try {
			Git git = initGitRepository();
			project = initProject(git);
			log.debug("Created project for analysis: " + project.toString());
			db.persistComplete(project);

			//get all necessary objects to analyze the commits
			GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
			RefactoringHandler handler = getRefactoringHandler(git);
			PMDatabase pmDatabase = new PMDatabase();
			final RefactoringAnalyzer refactoringAnalyzer = new RefactoringAnalyzer(project, db, repository, pmDatabase, filesStoragePath, storeFullSourceCode);
			final ProcessMetricsCollector processMetrics = new ProcessMetricsCollector(project, db, repository, pmDatabase, filesStoragePath);

			// get all commits in the repo, and to each commit with a refactoring, extract the metrics
			RevWalk walk = JGitUtils.getReverseWalk(repository, mainBranch);
			RevCommit currentCommit = walk.next();
			log.info("Start mining project " + gitUrl + "(clone at " + clonePath + ")");
			// we only analyze commits that have one parent or the first commit with 0 parents
			for (boolean endFound = false; currentCommit!=null && !endFound; currentCommit = walk.next()) {
				// did we find the last commit to process?
				// if so, process it and then stop
				if (currentCommit.toString().equals(lastCommitToProcess))
					endFound = true;

				// i.e., ignore merge commits
				if (currentCommit.getParentCount() > 1)
					continue;

				processCommit(currentCommit, miner, handler, refactoringAnalyzer, processMetrics);
			}
			walk.close();

			// set finished data
			// note that if this process crashes, finished date will be equals to null in the database
			project.setFinishedDate(Calendar.getInstance());
			project.setExceptions(exceptionsCount);
			db.updateComplete(project);

			logProjectStatistics(startProjectTime);
			return project;
		} finally {
			// delete the tmp dir that stores the project
			FileUtils.deleteDirectory(new File(currentTempDir));
		}
	}

	private boolean isFirst(RevCommit commit) {return commit.getParentCount() == 0;}

	//Initialize the git repository for this run, by downloading it
	//Returns the jgit repository object and the git object
	private Git initGitRepository() throws Exception {
		GitService gitService = new GitServiceImpl();
		repository = gitService.cloneIfNotExists(clonePath, gitUrl);
		final Git git = Git.open(new File(lastSlashDir(clonePath) + ".git"));

		// identifies the main branch of that repo
		mainBranch = discoverMainBranch(git);
		return git;
	}

	//Initialize the project object for this run
	private Project initProject(Git git) throws GitAPIException, IOException {
		CounterResult counterResult = Counter.countProductionAndTestFiles(clonePath);
		long projectSize = -1;
		try{
			projectSize = FileUtils.sizeOfDirectory(new File(clonePath));
		} catch (IllegalArgumentException e){
			log.info("For project: " + gitUrl + " the project size could not be determined.", e);
		}
		int numberOfCommits = numberOfCommits(git);
		String lastCommitHash = getHead(git);
		String projectName = extractProjectNameFromGitUrl(gitUrl);
		return new Project(datasetName, gitUrl, projectName, Calendar.getInstance(),
				numberOfCommits, getProperty("stableCommitThresholds"), lastCommitHash, counterResult, projectSize);
	}

	private void processCommit(RevCommit currentCommit, GitHistoryRefactoringMiner miner, RefactoringHandler handler, RefactoringAnalyzer refactoringAnalyzer, ProcessMetricsCollector processMetrics){
		long startCommitTime = System.currentTimeMillis();
		String commitHash = currentCommit.getId().getName();
		try{
			db.openSession();

			refactoringsToProcess = null;
			commitIdToProcess = null;

			//stores all the ck metrics for the current commit
			List<RefactoringCommit> allRefactoringCommits = new ArrayList<>();
			// stores the commit meta data
			CommitMetaData superCommitMetaData = new CommitMetaData(currentCommit, project);

			// Note that we only run it if the commit has a parent, i.e, skip the first commit of the repo
			if (!isFirst(currentCommit)){
				long startTimeRMiner = System.currentTimeMillis();
				miner.detectAtCommit(repository, commitHash, handler, refactoringMinerTimeout);
				log.debug("Refactoring miner took " + (System.currentTimeMillis() - startTimeRMiner) + " milliseconds to mine the commit: " + commitHash);

				// if timeout has happened, refactoringsToProcess and commitIdToProcess will be null
				boolean thereIsRefactoringToProcess = refactoringsToProcess != null && commitIdToProcess != null;
				if(thereIsRefactoringToProcess)
					//remove all not studied refactorings from the list
					refactoringsToProcess = refactoringsToProcess.stream().filter(RefactoringUtils::isStudied).collect(Collectors.toList());

				//check if refactoring miner detected a refactoring we study
				if (thereIsRefactoringToProcess && !refactoringsToProcess.isEmpty()) {
					db.persist(superCommitMetaData);
					allRefactoringCommits = refactoringAnalyzer.collectCommitData(currentCommit, superCommitMetaData, refactoringsToProcess);
				} else if (thereIsRefactoringToProcess) {
					// timeout happened, so count it as an exception
					log.debug("Refactoring Miner did not find any refactorings for commit: " + commitHash + createErrorState(commitHash, project));
				} else {
					// timeout happened, so count it as an exception
					log.error("Refactoring Miner timed out for commit: " + commitHash + createErrorState(commitHash, project));
					exceptionsCount++;
				}
			}

			//collect the process metrics for the current commit
			processMetrics.collectMetrics(currentCommit, superCommitMetaData, allRefactoringCommits);
			long startTimeTransaction = System.currentTimeMillis();
			db.commit();
			log.debug("Committing the transaction for commit " + commitHash + " took " + (System.currentTimeMillis() - startTimeTransaction) + " milliseconds.");
		} catch (Exception e) {
			exceptionsCount++;
			log.error("Unhandled exception when collecting commit data for commit: " + commitHash + createErrorState(commitHash, project), e);
			db.rollback(createErrorState(commitHash, project));
		} finally {
			db.close();
		}
		long elapsedCommitTime = System.currentTimeMillis() - startCommitTime;
		log.debug("Processing commit " + commitHash + " took " + elapsedCommitTime + " milliseconds.");
	}

	//Log the project statistics after the run
	private void logProjectStatistics(long startProjectTime){
		double elapsedTime = (System.currentTimeMillis() - startProjectTime) / 1000.0 / 60.0;
		StringBuilder statistics = new StringBuilder("Finished mining " + gitUrl + " in " + elapsedTime + " minutes");

		long stableInstancesCount = db.findAllStableCommits(project.getId());
		long refactoringInstancesCount = db.findAllRefactoringCommits(project.getId());
		statistics.append("\nFound ").append(refactoringInstancesCount).append(" refactoring- and ").append(stableInstancesCount).append(" stable instances in the project.");
		for(int level: project.getCommitCountThresholds()){
			stableInstancesCount = db.findAllStableCommits(project.getId(), level);
			statistics.append("\n\t\tFound ").append(stableInstancesCount).append(" stable instances in the project with threshold: ").append(level);
		}
		statistics.append("\n").append(project.toString());
		log.info(statistics);
	}

	private RefactoringHandler getRefactoringHandler(Git git) {
		return new RefactoringHandler() {
			@Override
			public void handle(String commitId, List<Refactoring> refactorings) {
				commitIdToProcess = commitId;
				refactoringsToProcess = refactorings;
			}

			@Override
			public void handleException(String commitId, Exception e) {
				exceptionsCount++;
				log.error("RefactoringMiner could not handle commit: " + commitId + createErrorState(commitId, project), e);
				resetGitRepo();
			}

			private void resetGitRepo() {
				try {
					git.reset().setMode(ResetCommand.ResetType.HARD).call();
				} catch (GitAPIException e1) {
					log.error("Reset failed for repository: " + gitUrl + " after a commit couldn't be handled." + createErrorState("UNK", project), e1);
				}
			}
		};
	}
}