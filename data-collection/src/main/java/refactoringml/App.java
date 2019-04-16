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

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import static refactoringml.util.FilePathUtils.lastSlashDir;
import static refactoringml.util.JGitUtils.*;

public class App {

	private String clonePath;
	private String gitUrl;
	private String filesStoragePath;
	private int commitThreshold;
	private Database db;

	private static final Logger log = Logger.getLogger(App.class);
	private String datasetName;

	public App (String datasetName,
	            String clonePath,
	            String gitUrl,
	            String filesStoragePath,
	            int commitThreshold,
	            Database db) {

		this.datasetName = datasetName;
		this.clonePath = clonePath;
		this.gitUrl = gitUrl;
		this.filesStoragePath = filesStoragePath;
		this.commitThreshold = commitThreshold;
		this.db = db;
	}

	public static void main(String[] args) throws Exception {

		// do we want to get data from the vars or not?
		// i.e., is this a local IDE test?
		boolean test = true;

		String gitUrl;
		String highLevelOutputPath;
		String datasetName;

		int commitThreshold;

		if(test) {
			gitUrl = "/Users/mauricioaniche/Desktop/commons-lang";
			highLevelOutputPath = "/Users/mauricioaniche/Desktop/results/";
			commitThreshold = 1000;
			datasetName = "test";
		} else {
			if (args == null || args.length != 4) {
				System.out.println("4 arguments: (dataset name) (git url or project directory) (output path) (not refactoring threshold)");
				System.exit(-1);
			}

			datasetName = args[0].trim();
			gitUrl = args[1].trim();
			highLevelOutputPath = lastSlashDir(args[2].trim());
			commitThreshold = Integer.parseInt(args[3].trim());
		}

		String clonePath = !gitUrl.startsWith("http") && !gitUrl.startsWith("git@") ? gitUrl : lastSlashDir(Files.createTempDir().getAbsolutePath()) + "/repo";
		String filesStoragePath = highLevelOutputPath + extractProjectNameFromGitUrl(gitUrl);
		new File(filesStoragePath).mkdirs();

		Database db = new Database(new HibernateConfig().getSessionFactory());

		// do not run if the project is already in the database
		if(db.projectExists(gitUrl)) {
			System.out.println(String.format("Project %s already in the database", gitUrl));
			System.exit(-1);
		}

		new App(datasetName, clonePath, gitUrl,
				filesStoragePath,
				commitThreshold, db).run();

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

		Project project = new Project(datasetName, gitUrl, extractProjectNameFromGitUrl(gitUrl), Calendar.getInstance());
		db.openSession();
		db.persist(project);
		db.commit();


		final ProcessMetricsCollector processMetrics = new ProcessMetricsCollector(project, db, repo, mainBranch, commitThreshold, filesStoragePath);
		final RefactoringAnalyzer refactoringAnalyzer = new RefactoringAnalyzer(project, db, repo, processMetrics, filesStoragePath);

		miner.detectAll(repo, mainBranch, new RefactoringHandler() {
			@Override
			public void handle(RevCommit commitData, List<Refactoring> refactorings) {
				for (Refactoring ref : refactorings) {
					try {
						db.openSession();
						refactoringAnalyzer.collectCommitData(commitData, ref);
						db.commit();
					} catch (Exception e) {
						log.error("Error", e);
						db.close();
					}
				}
			}

			@Override
			public void handleException(String commitId, Exception e) {
				log.error("RefactoringMiner not handle commit Id " + commitId, e);
				resetGitRepo();

			}

			private void resetGitRepo () {
				try {
					git.reset().setMode(ResetCommand.ResetType.HARD).call();
				} catch (GitAPIException e1) {
					log.error("Reset failed", e1);
				}
			}
		});

		log.info("Starting the collection of the process metrics and the non-refactored classes");
		processMetrics.collect();

		long end = System.currentTimeMillis();
		log.info(String.format("Finished in %.2f minutes", ((end-start)/1000.0/60.0)));

		db.openSession();
		project.setFinishedDate(Calendar.getInstance());
		db.update(project);
		db.commit();
	}


	private String discoverMainBranch(Git git) throws IOException {
		return git.getRepository().getBranch();
	}


}
