package refactoringml;

import com.google.common.io.Files;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;
import refactoringml.db.Database;
import refactoringml.db.HibernateConfig;
import refactoringml.db.Project;
import refactoringml.util.LOCUtils;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import static refactoringml.util.FilePathUtils.lastSlashDir;
import static refactoringml.util.JGitUtils.*;

public class App {

	private String clonePath;
	private String gitUrl;
	private String filesStoragePath;
	private Database db;

	private static final Logger log = Logger.getLogger(App.class);
	private String datasetName;
	private int exceptionsCount = 0;

	public App (String datasetName,
	            String clonePath,
	            String gitUrl,
	            String filesStoragePath,
	            Database db) {

		this.datasetName = datasetName;
		this.clonePath = clonePath;
		this.gitUrl = gitUrl;
		this.filesStoragePath = filesStoragePath;
		this.db = db;
	}

	public static void main(String[] args) throws Exception {

		// do we want to get data from the vars or not?
		// i.e., is this a local IDE test?
		boolean test = (args == null || args.length == 0);

		String gitUrl;
		String highLevelOutputPath;
		String datasetName;
		String url;
		String user;
		String pwd;

		if(test) {
			gitUrl = "/Users/mauricioaniche/Desktop/commons-lang";
			highLevelOutputPath = "/Users/mauricioaniche/Desktop/results/";
			datasetName = "test";

			url = "jdbc:mysql://localhost:3306/refactoring2?useSSL=false";
			user = "root";
			pwd = "";

		} else {
			if (args == null || args.length != 6) {
				System.out.println("6 arguments: (dataset name) (git url or project directory) (output path) (database url) (database user) (database pwd)");
				System.exit(-1);
			}

			datasetName = args[0].trim();
			gitUrl = args[1].trim();
			highLevelOutputPath = lastSlashDir(args[2].trim());

			url = args[3];
			user = args[4];
			pwd = args[5];
		}

		String clonePath = !gitUrl.startsWith("http") && !gitUrl.startsWith("git@") ? gitUrl : lastSlashDir(Files.createTempDir().getAbsolutePath()) + "repo";
		String filesStoragePath = highLevelOutputPath; // No need for the name of the project, as the run.sh creates a folder for it already
		new File(filesStoragePath).mkdirs();

		Database db = new Database(new HibernateConfig().getSessionFactory(url, user, pwd));

		// do not run if the project is already in the database
		if(db.projectExists(gitUrl)) {
			System.out.println(String.format("Project %s already in the database", gitUrl));
			System.exit(-1);
		}

		new App(datasetName, clonePath, gitUrl,
				filesStoragePath, db).run();

    }

	public void run () throws Exception {

		long start = System.currentTimeMillis();

		GitService gitService = new GitServiceImpl();
		GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

		log.info("Refactoring analyzer");
		log.info("Starting project " + gitUrl + "(clone at " + clonePath + ")");
		final Repository repo = gitService.cloneIfNotExists(clonePath, gitUrl);
		final Git git = Git.open(new File(lastSlashDir(clonePath) + ".git"));

		// identifies the main branch of that repo
		String mainBranch = discoverMainBranch(git);
		log.debug("main branch: " + mainBranch);

		// we define the threshold to consider a file as a non-refactored data point,
		// if it is changed by 10% of the commits without being refactored.
		int numberOfCommits = numberOfCommits(git);
		int commitThreshold = (int) ( numberOfCommits * 0.02);

		int loc = LOCUtils.countJavaFiles(clonePath);

		Project project = new Project(datasetName, gitUrl, extractProjectNameFromGitUrl(gitUrl), Calendar.getInstance(), numberOfCommits, commitThreshold, loc);
		db.openSession();
		db.persist(project);
		db.commit();


		final ProcessMetricsCollector processMetrics = new ProcessMetricsCollector(project, db, repo, mainBranch, commitThreshold, filesStoragePath);
		final RefactoringAnalyzer refactoringAnalyzer = new RefactoringAnalyzer(project, db, repo, processMetrics, filesStoragePath);

		// get all commits in the repo, and to each commit with a refactoring, extract the metrics
		Iterator<RevCommit> it = getAllCommits(repo);
		RefactoringHandler handler = getRefactoringHandler(git, refactoringAnalyzer);


		while(it.hasNext()) {
			try {
				RevCommit currentCommit = it.next();
				String commitHash = currentCommit.getId().getName();

				// we define a timeout of 20 seconds for RefactoringMiner to find a refactoring.
				miner.detectAtCommit(repo, null, commitHash, handler, 20);
			} catch(Exception e) {
				exceptionsCount++;
				log.error("Unexpected exception, but moving on", e);
			}
		}

		// all refactorings were detected, now we start the second phase:
		// collecting process metrics and examples of non-refatored code
		log.info("Starting the collection of the process metrics and the non-refactored classes");
		processMetrics.collect();

		long end = System.currentTimeMillis();
		log.info(String.format("Finished in %.2f minutes", ((end-start)/1000.0/60.0)));

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
		// we also delete variable usages that was equals to -1 (which means, we failed to detect it for some reason)
		db.openSession();
		db.cleanProject(project);
		db.commit();
	}

	private RefactoringHandler getRefactoringHandler(Git git, RefactoringAnalyzer refactoringAnalyzer) {
		return new RefactoringHandler() {
				@Override
				public void handle(RevCommit commitData, List<Refactoring> refactorings) {
					for (Refactoring ref : refactorings) {
						try {
							db.openSession();
							refactoringAnalyzer.collectCommitData(commitData, ref);
							db.commit();
						} catch (Exception e) {
							log.error("Error", e);
							db.rollback();
						} finally {
							db.close();
						}
					}
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

	private Iterator<RevCommit> getAllCommits(Repository repo) throws Exception {
		GitServiceImpl gs = new GitServiceImpl();
		return gs.createAllRevsWalk(repo).iterator();
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
