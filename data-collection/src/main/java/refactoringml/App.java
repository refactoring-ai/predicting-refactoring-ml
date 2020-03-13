package refactoringml;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
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
import java.util.*;
import java.util.stream.Collectors;
import static refactoringml.util.FilePathUtils.enforceUnixPaths;
import static refactoringml.util.FilePathUtils.lastSlashDir;
import static refactoringml.util.FileUtils.createTmpDir;
import static refactoringml.util.JGitUtils.*;
import static refactoringml.util.PropertiesUtils.getProperty;

public class App {
	private String gitUrl;
	private String filesStoragePath;
	private Database db;
	private String lastCommitToProcess;
	private boolean storeFullSourceCode;

	private static final Logger log = LogManager.getLogger(App.class);
	private String datasetName;
	private int exceptionsCount = 0;

	private int refactoringMinerTimeout;
	private String commitIdToProcess;
	private List<Refactoring> refactoringsToProcess;

	private String newTmpDir;
	private String clonePath;
	private String lastCommitHash;
	private String mainBranch;

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
		newTmpDir = createTmpDir();
		clonePath = (Project.isLocal(gitUrl) ? gitUrl : newTmpDir + "repo").trim();
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
			new File(filesStoragePath).mkdirs();
		}

		try {
			ImmutablePair pair = initGitRepository();
			Repository repo = (Repository) pair.getLeft();
			Git git = (Git) pair.getRight();

			Project project = initProject(git);
			log.debug("Created project for analysis: " + project.toString());
			db.persistComplete(project);

			//get all necessary objects
			final ProcessMetricsCollector processMetrics = new ProcessMetricsCollector(project, db, repo, filesStoragePath);
			final RefactoringAnalyzer refactoringAnalyzer = new RefactoringAnalyzer(project, db, repo, filesStoragePath, storeFullSourceCode);
			RefactoringHandler handler = getRefactoringHandler(git);
			GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

			// get all commits in the repo, and to each commit with a refactoring, extract the metrics
			RevWalk walk = JGitUtils.getReverseWalk(repo, mainBranch);
			RevCommit currentCommit = walk.next();
			log.info("Start mining project " + gitUrl + "(clone at " + clonePath + ")");
			// we only analyze commits that have one parent or the first commit with 0 parents
			for (boolean endFound = false; currentCommit!=null && !endFound; currentCommit = walk.next()) {
				long startCommitTime = System.currentTimeMillis();
				String commitHash = currentCommit.getId().getName();

				// i.e., ignore merge commits
				if (currentCommit.getParentCount() > 1)
					continue;

				// did we find the last commit to process?
				// if so, process it and then stop
				if (currentCommit.toString().equals(lastCommitToProcess))
					endFound = true;

				try {
					db.openSession();

					refactoringsToProcess = null;
					commitIdToProcess = null;
					//stores all the ck metrics for the current commit
					List<RefactoringCommit> allRefactoringCommits = new ArrayList<>();
					// stores the commit meta data
					CommitMetaData superCommitMetaData = new CommitMetaData(currentCommit, project);

					// Note that we only run it if the commit has a parent, i.e, skip the first commit of the repo
					if (!isFirst(currentCommit)){
						miner.detectAtCommit(repo, commitHash, handler, refactoringMinerTimeout);

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
							log.debug("Refactoring Miner did not find any refactorings for commit: " + commitHash);
						} else {
							// timeout happened, so count it as an exception
							log.error("Refactoring Miner returned null for " + commitHash + " due to a timeout after " + refactoringMinerTimeout + " seconds.");
							exceptionsCount++;
						}
					}

					//collect the process metrics for the current commit
					processMetrics.collectMetrics(currentCommit, superCommitMetaData, allRefactoringCommits);

					db.commit();
				} catch (Exception e) {
					exceptionsCount++;
					log.error("Unhandled exception when collecting commit data: ", e);
					db.rollback();
				} finally {
					db.close();
				}
				long elapsedCommitTime = System.currentTimeMillis() - startCommitTime;
				log.debug("Processing commit " + commitHash + " took " + elapsedCommitTime + " milliseconds.");
			}
			walk.close();

			// set finished data
			// note that if this process crashes, finished date will be equals to null in the database
			// these projects must be deleted manually afterwards....
			db.openSession();
			project.setFinishedDate(Calendar.getInstance());
			project.setExceptions(exceptionsCount);
			db.update(project);
			db.commit();
			db.close();

			double elapsedTime = (System.currentTimeMillis() - startProjectTime) / 1000.0 / 60.0;
			log.info("Finished mining " + gitUrl + " in " + elapsedTime + " minutes");
			log.info(getProjectStatistics(project));
			return project;
		} finally {
			// delete the tmp dir that stores the project
			FileUtils.deleteDirectory(new File(newTmpDir));
		}
	}

	private boolean isFirst(RevCommit commit) {return commit.getParentCount() == 0;}

	//Initialize the git repository for this run, by downloading it
	//Returns the jgit repository object and the git object
	private ImmutablePair initGitRepository() throws Exception {
		GitService gitService = new GitServiceImpl();
		final Repository repo = gitService.cloneIfNotExists(clonePath, gitUrl);
		final Git git = Git.open(new File(lastSlashDir(clonePath) + ".git"));

		// identifies the main branch of that repo
		mainBranch = discoverMainBranch(git);
		lastCommitHash = getHead(git);

		return new ImmutablePair(repo, git);
	}

	//Initialize the project object for this run
	private Project initProject(Git git) throws GitAPIException {
		CounterResult counterResult = Counter.countProductionAndTestFiles(clonePath);
		long projectSize = FileUtils.sizeOfDirectory(new File(clonePath));
		int numberOfCommits = numberOfCommits(git);

		return new Project(datasetName, gitUrl, extractProjectNameFromGitUrl(gitUrl), Calendar.getInstance(),
				numberOfCommits, getProperty("stableCommitThresholds"), lastCommitHash, counterResult, projectSize);
	}

	private String getProjectStatistics(Project project){
		long stableInstancesCount = db.findAllStableCommits(project.getId());
		long refactoringInstancesCount = db.findAllRefactoringCommits(project.getId());
		StringBuilder statistics = new StringBuilder("\nFound " + refactoringInstancesCount + " refactoring- and " + stableInstancesCount + " stable instances in the project.");
		for(int level: project.getCommitCountThresholds()){
			stableInstancesCount = db.findAllStableCommits(project.getId(), level);
			statistics.append("\n\t\tFound ").append(stableInstancesCount).append(" stable instances in the project with threshold: ").append(level);
		}
		return statistics + "\n" + project.toString();
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
				log.error("RefactoringMiner could not handle commit Id " + commitId, e);
				resetGitRepo();
			}

			private void resetGitRepo() {
				try {
					git.reset().setMode(ResetCommand.ResetType.HARD).call();
				} catch (GitAPIException e1) {
					log.error("Reset failed for repository: " + gitUrl + " after a commit couldn't be handled.", e1);
				}
			}
		};
	}
}