package refactoringml;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import refactoringml.db.Database;
import refactoringml.db.Project;
import refactoringml.util.Counter;
import refactoringml.util.Counter.CounterResult;
import refactoringml.util.JGitUtils;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import static refactoringml.util.FilePathUtils.lastSlashDir;
import static refactoringml.util.JGitUtils.extractProjectNameFromGitUrl;

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
	private int threshold;

	public App (String datasetName,
	            String gitUrl,
	            String filesStoragePath,
	            int threshold,
	            Database db, 
	            boolean storeFullSourceCode) {
		this(datasetName, gitUrl, filesStoragePath, threshold, db, null, storeFullSourceCode);

	}
	public App (String datasetName,
	            String gitUrl,
	            String filesStoragePath,
	            int threshold,
	            Database db,
	            String lastCommitToProcess,
	            boolean storeFullSourceCode
	            ) {

		this.datasetName = datasetName;
		this.gitUrl = gitUrl;
		this.filesStoragePath = filesStoragePath + extractProjectNameFromGitUrl(gitUrl); // add project as subfolder
		this.threshold = threshold;
		this.db = db;
		this.lastCommitToProcess = lastCommitToProcess;
		this.storeFullSourceCode = storeFullSourceCode;
	}

	public Project run () throws Exception {

		long start = System.currentTimeMillis();

		GitService gitService = new GitServiceImpl();
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

		// creates a temp dir to store the project
		String newTmpDir = Files.createTempDir().getAbsolutePath();
		String clonePath = (!gitUrl.startsWith("http") && !gitUrl.startsWith("git@") ? gitUrl : lastSlashDir(newTmpDir) + "repo").trim();

		try {

			// creates the directory in the storage
			if(storeFullSourceCode) {
				new File(filesStoragePath).mkdirs();
			}

			log.info("Refactoring analyzer");
			log.info("Starting project " + gitUrl + "(clone at " + clonePath + ")");
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
					numberOfCommits, threshold, lastCommitHash, counterResult, projectSize);

			db.openSession();
			db.persist(project);
			db.commit();


			final ProcessMetricsCollector processMetrics = new ProcessMetricsCollector(project, db, repo, mainBranch, threshold, filesStoragePath, lastCommitToProcess);
			final RefactoringAnalyzer refactoringAnalyzer = new RefactoringAnalyzer(project, db, repo, processMetrics, filesStoragePath, storeFullSourceCode);

			RefactoringHandler handler = getRefactoringHandler(git);

			// get all commits in the repo, and to each commit with a refactoring, extract the metrics
			RevWalk walk = JGitUtils.getReverseWalk(repo, mainBranch);
			RevCommit currentCommit = walk.next();

			boolean endFound = false;
			while (currentCommit!=null && !endFound) {

				log.debug("Visiting commit " + currentCommit.getId().getName());

				// did we find the last commit to process?
				// if so, process it and then stop
				if (currentCommit.equals(lastCommitToProcess))
					endFound = true;

				String commitHash = currentCommit.getId().getName();

				log.debug("Invoking refactoringminer for commit " + commitHash);

				refactoringsToProcess = null;
				commitIdToProcess = null;

				// we define a timeout of 20 seconds for RefactoringMiner to find a refactoring.
				miner.detectAtCommit(repo, commitHash, handler, 20);

				// if timeout has happened, refactoringsToProcess and commitIdToProcess will be null
				boolean thereIsRefactoringToProcess = refactoringsToProcess != null && commitIdToProcess != null;
				if (thereIsRefactoringToProcess) {
					for (Refactoring ref : refactoringsToProcess) {
						try {
							db.openSession();
							refactoringAnalyzer.collectCommitData(currentCommit, ref);
							db.commit();
						} catch (Exception e) {
							exceptionsCount++;
							log.error("Error when collecting commit data", e);
							db.rollback();
						} finally {
							db.close();
						}
					}
				} else {
					// timeout happened, so count it as an exception
					exceptionsCount++;
				}

				currentCommit = walk.next();
			}

			walk.close();

			// all refactorings were detected, now we start the second phase:
			// collecting process metrics and examples of non-refatored code
			log.info("Starting the collection of the process metrics and the non-refactored classes");
			processMetrics.collect();

			long end = System.currentTimeMillis();
			log.info(String.format("Finished in %.2f minutes", ( ( end - start ) / 1000.0 / 60.0 )));

			// set finished data
			// note that if this process crashes, finisheddate will be equals to null in the database
			// these projects must be deleted manually afterwards....
			db.openSession();
			project.setFinishedDate(Calendar.getInstance());
			project.setExceptions(exceptionsCount);
			db.update(project);
			db.commit();

			// we may have collected data from refactorings and non refactorings, but not able to collect
			// their process metric. We thus delete these data points as we can't really use them in training.
			// we also delete variable usages that were equals to -1 (which means, we failed to detect it for some reason)
			db.openSession();
			db.cleanProject(project);
			db.commit();

			return project;
		} finally {
			// delete the tmp dir that stores the project
			FileUtils.deleteDirectory(new File(newTmpDir));
		}
	}

	private String getHead(Git git) throws IOException {
		return git.getRepository().resolve(Constants.HEAD).getName();
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
					log.error("RefactoringMiner not handle commit Id " + commitId, e);
					resetGitRepo();

				}

				private void resetGitRepo() {
					try {
						git.reset().setMode(ResetCommand.ResetType.HARD).call();
					} catch (GitAPIException e1) {
						log.error("Reset failed", e1);
					}
				}
			};
	}

	private int numberOfCommits(Git git) throws GitAPIException {
		Iterable<RevCommit> commits = git.log().call();
		int count = 0;
		for(RevCommit ignored : commits) {
			count++;
		}

		return count;
	}


	private String discoverMainBranch(Git git) throws IOException {
		return git.getRepository().getBranch();
	}


}
