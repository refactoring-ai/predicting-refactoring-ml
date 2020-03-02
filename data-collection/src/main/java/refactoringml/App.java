package refactoringml;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
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
import refactoringml.db.CommitMetaData;
import refactoringml.db.Database;
import refactoringml.db.Project;
import refactoringml.db.RefactoringCommit;
import refactoringml.util.Counter;
import refactoringml.util.Counter.CounterResult;
import refactoringml.util.JGitUtils;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static refactoringml.util.FilePathUtils.enforceUnixPaths;
import static refactoringml.util.FilePathUtils.lastSlashDir;
import static refactoringml.util.FileUtils.createTmpDir;
import static refactoringml.util.JGitUtils.*;
import static refactoringml.util.PropertiesUtils.getProperty;
import static refactoringml.util.RefactoringUtils.isStudied;

public class App {
	private String gitUrl;
	private String filesStoragePath;
	private Database db;
	private String lastCommitToProcess;
	private boolean storeFullSourceCode;

	private static final Logger log = Logger.getLogger(App.class);
	private String datasetName;
	private int exceptionsCount = 0;

	String commitIdToProcess;
	List<Refactoring> refactoringsToProcess;

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
	}

	public Project run () throws Exception {
		// do not run if the project is already in the database
		if (db.projectExists(gitUrl)) {
			String message = String.format("Project %s already in the database", gitUrl);
			throw new IllegalArgumentException(message);
		}

		long start = System.currentTimeMillis();

		GitService gitService = new GitServiceImpl();
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

		// creates a temp dir to store the project
		String newTmpDir = createTmpDir();
		String clonePath = (Project.isLocal(gitUrl) ? gitUrl : newTmpDir + "repo").trim();

		try {
			// creates the directory in the storage
			if(storeFullSourceCode) {
				new File(filesStoragePath).mkdirs();
			}

			log.info("REFACTORING ANALYZER");
			log.info("Start mining project " + gitUrl + "(clone at " + clonePath + ")");
			final Repository repo = gitService.cloneIfNotExists(clonePath, gitUrl);
			final Git git = Git.open(new File(lastSlashDir(clonePath) + ".git"));

			// identifies the main branch of that repo
			String mainBranch = discoverMainBranch(git);
			log.debug("main branch: " + mainBranch);

			String lastCommitHash = getHead(git);

			CounterResult counterResult = Counter.countProductionAndTestFiles(clonePath);
			long projectSize = FileUtils.sizeOfDirectory(new File(clonePath));
			int numberOfCommits = numberOfCommits(git);

			Project project = new Project(datasetName, gitUrl, extractProjectNameFromGitUrl(gitUrl), Calendar.getInstance(),
					numberOfCommits, getProperty("stableCommitThresholds"), lastCommitHash, counterResult, projectSize);
			log.debug("Set project stable commit threshold(s) to: " + project.getCommitCountThresholds());

			db.openSession();
			db.persist(project);
			db.commit();
			db.close();

			final ProcessMetricsCollector processMetrics = new ProcessMetricsCollector(project, db, repo, filesStoragePath);
			final RefactoringAnalyzer refactoringAnalyzer = new RefactoringAnalyzer(project, db, repo, filesStoragePath, storeFullSourceCode);

			RefactoringHandler handler = getRefactoringHandler(git);

			// get all commits in the repo, and to each commit with a refactoring, extract the metrics
			RevWalk walk = JGitUtils.getReverseWalk(repo, mainBranch);
			RevCommit currentCommit = walk.next();

			int refactoringMinerTimeout = Integer.valueOf(getProperty("timeout"));
			log.debug("Set Refactoring Miner timeout to " + refactoringMinerTimeout + " seconds.");

			// we only analyze commits that have one parent or the first commit with 0 parents
			for (boolean endFound = false; currentCommit!=null && !endFound; currentCommit = walk.next()) {

				try {
					db.openSession();

					// i.e., ignore merge commits
					if (currentCommit.getParentCount() > 1)
						continue;

					// did we find the last commit to process?
					// if so, process it and then stop
					if (currentCommit.equals(lastCommitToProcess))
						endFound = true;

					String commitHash = currentCommit.getId().getName();
					if (TrackDebugMode.ACTIVE && commitHash.contains(TrackDebugMode.COMMIT_TO_TRACK)) {
						log.debug("[Track] Visiting commit " + commitHash);
					}

					refactoringsToProcess = null;
					commitIdToProcess = null;

					// Note that we only run it if the commit has a parent, i.e, skip the first commit of the repo
					if (currentCommit.getParentCount() == 1)
						miner.detectAtCommit(repo, commitHash, handler, refactoringMinerTimeout);

					//stores all the ck metrics for the current commit
					List<RefactoringCommit> allRefactoringCommits = new ArrayList<>();

					// stores the commit meta data
					CommitMetaData superCommitMetaData = new CommitMetaData(currentCommit, project);
					// if timeout has happened, refactoringsToProcess and commitIdToProcess will be null
					boolean thereIsRefactoringToProcess = refactoringsToProcess != null && commitIdToProcess != null;
					if(thereIsRefactoringToProcess)
						//remove all not studied refactorings from the list
						refactoringsToProcess = refactoringsToProcess.stream().filter(Refactoring -> isStudied(Refactoring)).collect(Collectors.toList());

					//check if refactoring miner detected a refactoring we study
					if (thereIsRefactoringToProcess && !refactoringsToProcess.isEmpty()) {
						db.persist(superCommitMetaData);
						superCommitMetaData = db.loadCommitMetaData(superCommitMetaData.getId());
						for (Refactoring ref : refactoringsToProcess) {
							allRefactoringCommits.addAll(refactoringAnalyzer.collectCommitData(currentCommit, superCommitMetaData, ref));
						}
					} else if (currentCommit.getParentCount() == 1 && thereIsRefactoringToProcess) {
						// timeout happened, so count it as an exception
						log.debug("Refactoring Miner did not find any refactorings for commit: " + commitHash);
					}else if (currentCommit.getParentCount() == 1) {
						// timeout happened, so count it as an exception
						log.error("Refactoring Miner returned null for " + commitHash + " due to a timeout after " + refactoringMinerTimeout + " seconds.");
						exceptionsCount++;
					}

					//collect the process metrics for the current commit
					processMetrics.collectMetrics(currentCommit, superCommitMetaData, allRefactoringCommits, thereIsRefactoringToProcess);

					db.commit();
				} catch (Exception e) {
					exceptionsCount++;
					log.error("Exception when collecting commit data: ", e);
					db.rollback();
				} finally {
					db.close();
				}
			}

			walk.close();

			long end = System.currentTimeMillis();
			log.info(String.format("Finished mining %s in %.2f minutes", gitUrl,( ( end - start ) / 1000.0 / 60.0 )));

			// set finished data
			// note that if this process crashes, finished date will be equals to null in the database
			// these projects must be deleted manually afterwards....
			db.openSession();
			project.setFinishedDate(Calendar.getInstance());
			project.setExceptions(exceptionsCount);
			db.update(project);
			db.commit();
			db.close();

			return project;
		} finally {
			// delete the tmp dir that stores the project
			FileUtils.deleteDirectory(new File(newTmpDir));
		}
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