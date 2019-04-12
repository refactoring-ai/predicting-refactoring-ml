package refactoringml;

import com.google.common.io.Files;
import org.apache.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.transport.RemoteConfig;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.GitService;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;
import org.refactoringminer.util.GitServiceImpl;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static refactoringml.FilePathUtils.lastSlashDir;

public class App {

	private String clonePath;
	private String gitUrl;
	private PrintStream notRefactoredFieldLevelOutputFile;
	private String filesStoragePath;

	private PrintStream notRefactoredClassLevelOutputFile;
	private PrintStream notRefactoredMethodLevelOutputFile;
	private PrintStream notRefactoredVariableLevelOutputFile;

	private PrintStream refactoredOutputFile;
	private PrintStream refactoredClassLevelOutputFile;
	private PrintStream refactoredMethodLevelOutputFile;
	private PrintStream refactoredVariableLevelOutputFile;

	private PrintStream refactoredFieldLevelOutputFile;
	private PrintStream processMetricsOutputFile;
	private PrintStream projectInfoOutputFile;
	private int commitThreshold;

	private static final Logger log = Logger.getLogger(App.class);
	private String datasetName;

	public App (String datasetName,
	            String clonePath,
	            String gitUrl,
	            PrintStream notRefactoredClassLevelOutputFile,
	            PrintStream notRefactoredMethodLevelOutputFile,
	            PrintStream notRefactoredVariableLevelOutputFile,
	            PrintStream notRefactoredFieldLevelOutputFile,
	            String filesStoragePath,
	            PrintStream refactoredOutputFile,
	            PrintStream refactoredClassLevelOutputFile,
	            PrintStream refactoredMethodLevelOutputFile,
	            PrintStream refactoredVariableLevelOutputFile,
	            PrintStream refactoredFieldLevelOutputFile,
	            PrintStream processMetricsOutputFile,
	            PrintStream projectInfoOutputFile,
	            int commitThreshold) {
		this.datasetName = datasetName;
		this.clonePath = clonePath;
		this.gitUrl = gitUrl;

		this.notRefactoredClassLevelOutputFile = notRefactoredClassLevelOutputFile;
		this.notRefactoredMethodLevelOutputFile = notRefactoredMethodLevelOutputFile;
		this.notRefactoredVariableLevelOutputFile = notRefactoredVariableLevelOutputFile;
		this.notRefactoredFieldLevelOutputFile = notRefactoredFieldLevelOutputFile;

		this.filesStoragePath = filesStoragePath;
		this.refactoredOutputFile = refactoredOutputFile;

		this.refactoredClassLevelOutputFile = refactoredClassLevelOutputFile;
		this.refactoredMethodLevelOutputFile = refactoredMethodLevelOutputFile;
		this.refactoredVariableLevelOutputFile = refactoredVariableLevelOutputFile;
		this.refactoredFieldLevelOutputFile = refactoredFieldLevelOutputFile;

		this.processMetricsOutputFile = processMetricsOutputFile;
		this.projectInfoOutputFile = projectInfoOutputFile;
		this.commitThreshold = commitThreshold;
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
			gitUrl = "/Users/mauricioaniche/Desktop/abdera";
			highLevelOutputPath = "/Users/mauricioaniche/Desktop/fse19/";
			commitThreshold = 500;
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
		String filesStoragePath = highLevelOutputPath + "/storage/";
		new File(filesStoragePath).mkdirs();

		String notRefactoredClassLevelFilePath = highLevelOutputPath + "no-class.csv";
		String notRefactoredMethodLevelFilePath = highLevelOutputPath + "no-method.csv";
		String notRefactoredVariableLevelFilePath = highLevelOutputPath + "no-variable.csv";
		String notRefactoredFieldLevelFilePath = highLevelOutputPath + "no-field.csv";

		String refactoredFilePath = highLevelOutputPath + "yes.csv";
		String refactoredClassLevelFilePath = highLevelOutputPath + "yes-class.csv";
		String refactoredMethodLevelFilePath = highLevelOutputPath + "yes-method.csv";
		String refactoredVariableLevelFilePath = highLevelOutputPath + "yes-variable.csv";
		String refactoredFieldLevelFilePath = highLevelOutputPath + "yes-field.csv";

		String processMetricsOutputFile = highLevelOutputPath + "process.csv";
		String projectInfoFilePath = highLevelOutputPath + "project-info.txt";

		new App(datasetName, clonePath, gitUrl,
				new PrintStream(notRefactoredClassLevelFilePath),
				new PrintStream(notRefactoredMethodLevelFilePath),
				new PrintStream(notRefactoredVariableLevelFilePath),
				new PrintStream(notRefactoredFieldLevelFilePath),

				filesStoragePath,

				new PrintStream(refactoredFilePath),
				new PrintStream(refactoredClassLevelFilePath),
				new PrintStream(refactoredMethodLevelFilePath),
				new PrintStream(refactoredVariableLevelFilePath),
				new PrintStream(refactoredFieldLevelFilePath),

				new PrintStream(processMetricsOutputFile),
				new PrintStream(projectInfoFilePath), commitThreshold).run();

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

		storeRepoInfo(git);

		String projectName = extractProjectName(gitUrl);

		final ProcessMetricsCollector processMetrics = new ProcessMetricsCollector(datasetName, gitUrl, projectName, repo, mainBranch, commitThreshold,processMetricsOutputFile, notRefactoredClassLevelOutputFile, notRefactoredMethodLevelOutputFile, notRefactoredVariableLevelOutputFile, notRefactoredFieldLevelOutputFile, filesStoragePath);
		final RefactoringAnalyzer refactoringAnalyzer = new RefactoringAnalyzer(datasetName, gitUrl, projectName, repo, processMetrics, refactoredOutputFile, refactoredClassLevelOutputFile, refactoredMethodLevelOutputFile, refactoredVariableLevelOutputFile, refactoredFieldLevelOutputFile, filesStoragePath);

//		miner.detectAll(repo, mainBranch, new RefactoringHandler() {
		miner.detectAtCommit(repo, mainBranch, "babb6c8e99746531174073ebd2bce291d18770f4", new RefactoringHandler() {
			@Override
			public void handle(RevCommit commitData, List<Refactoring> refactorings) {
				try {
					for (Refactoring ref : refactorings) {
						refactoringAnalyzer.collectCommitData(commitData, ref);
					}
				} catch (Exception e) {
					log.error("Error", e);
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

		closeAllOutputFiles();

		long end = System.currentTimeMillis();
		log.info(String.format("Finished in %.2f minutes", ((end-start)/1000.0/60.0)));
	}

	private String extractProjectName(String gitUrl) {
		String[] splittedGitUrl = gitUrl.split("/");
		return splittedGitUrl[splittedGitUrl.length - 1].replace("\\.git", "");
	}

	private String discoverMainBranch(Git git) throws IOException {
		return git.getRepository().getBranch();
	}

	private void closeAllOutputFiles() {
		notRefactoredClassLevelOutputFile.close();
		notRefactoredMethodLevelOutputFile.close();
		notRefactoredVariableLevelOutputFile.close();

		refactoredClassLevelOutputFile.close();
		refactoredMethodLevelOutputFile.close();
		refactoredVariableLevelOutputFile.close();

		processMetricsOutputFile.close();
		projectInfoOutputFile.close();
	}

	private void storeRepoInfo(Git git) throws GitAPIException {
		projectInfoOutputFile.println(new Date().toString());
		projectInfoOutputFile.println(gitUrl);
		for (RemoteConfig remoteConfig : git.remoteList().call()) {
			projectInfoOutputFile.println(remoteConfig.getName() + ": " + remoteConfig.getURIs().stream().map(x -> x.toString()).collect(Collectors.joining(",")));
		}
	}


}
