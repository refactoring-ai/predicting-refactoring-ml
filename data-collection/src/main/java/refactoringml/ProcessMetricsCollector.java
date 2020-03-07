package refactoringml;

import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKMethodResult;
import org.apache.log4j.Logger;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import refactoringml.db.*;
import refactoringml.util.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import static refactoringml.util.CKUtils.cleanClassName;
import static refactoringml.util.FilePathUtils.enforceUnixPaths;
import static refactoringml.util.CKUtils.*;
import static refactoringml.util.FileUtils.*;
import static refactoringml.util.JGitUtils.getReverseWalk;
import static refactoringml.util.JGitUtils.readFileFromGit;
import static refactoringml.util.RefactoringUtils.*;
import static refactoringml.util.SourceCodeUtils.getCleanSourceCode;

public class ProcessMetricsCollector {
	private Project project;
	private Database db;
	private Repository repository;
	private String fileStoragePath;
	private PMDatabase pmDatabase;

	private static final Logger log = Logger.getLogger(ProcessMetricsCollector.class);

	public ProcessMetricsCollector(Project project, Database db, Repository repository, String fileStoragePath) {
		this.project = project;
		this.db = db;
		this.repository = repository;
		this.fileStoragePath = FilePathUtils.lastSlashDir(fileStoragePath);
		pmDatabase = new PMDatabase();
	}

	//if this commit contained a refactoring, then collect its process metrics for all affected class files,
	//otherwise only update the file process metrics
	public void collectMetrics(RevCommit commit, CommitMetaData superCommitMetaData, List<RefactoringCommit> allRefactoringCommits) throws IOException {
		collectProcessMetricsOfRefactoredCommit(commit, superCommitMetaData, allRefactoringCommits);

		// we go now change by change in the commit to update the process metrics there
		// Also if a stable instance is found it is stored with the metrics in the DB
		RevCommit commitParent = commit.getParentCount() == 0 ? null : commit.getParent(0);
		collectProcessMetricsOfStableCommits(commit, commitParent, superCommitMetaData);
	}

	//Collect the ProcessMetrics of the RefactoringCommit before this commit happened and update the database entry with it
	private void collectProcessMetricsOfRefactoredCommit(RevCommit commit, CommitMetaData superCommitMetaData, List<RefactoringCommit> allRefactoringCommits) {
		for (RefactoringCommit refactoringCommit : allRefactoringCommits) {
			String fileName = refactoringCommit.getFilePath();
			ProcessMetricTracker currentProcessMetricsTracker = pmDatabase.find(fileName);

			ProcessMetrics dbProcessMetrics  = currentProcessMetricsTracker != null ?
					new ProcessMetrics(currentProcessMetricsTracker.getCurrentProcessMetrics()) :
					new ProcessMetrics(-1, -1, -1, -1, -1);

			refactoringCommit.setProcessMetrics(dbProcessMetrics);
			db.update(refactoringCommit);

			pmDatabase.reportRefactoring(fileName, superCommitMetaData);

			if(TrackDebugMode.ACTIVE && (fileName.contains(TrackDebugMode.FILENAME_TO_TRACK) || commit.getName().contains(TrackDebugMode.COMMIT_TO_TRACK))) {
				log.debug("[TRACK] Collected process metrics at refactoring commit " + commit.getId().getName() + " for class: " + commit.getName()
								+ " and class stability counter was set to: " + currentProcessMetricsTracker.getCommitCounter() + "\n" +
						"\t\t\t\t\t\t\tRefactoringCommit ProcessMetrics: " + refactoringCommit.getProcessMetrics());
			}
		}
	}

	//Update the process metrics of all affected class files:
	//Reset the PMTracker for all class files, that were refactored on this commit
	//Increase the PMTracker for all class files, that were not refactored but changed on this commit
	private void collectProcessMetricsOfStableCommits(RevCommit commit, RevCommit commitParent, CommitMetaData superCommitMetaData) throws IOException {
		try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
			diffFormatter.setRepository(repository);
			diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
			diffFormatter.setDetectRenames(true);

			for (DiffEntry entry : diffFormatter.scan(commitParent, commit)) {
				String fileName = enforceUnixPaths(entry.getNewPath());

				// do not collect these numbers if not a java file (save some memory)
				if (!refactoringml.util.FileUtils.IsJavaFile(fileName))
					continue;

				//TODO: in case the metrics collection crashes earlier for a commit with class file renames, the PMDatabase is not updated

				// if the class was deleted, we remove it from our pmDatabase
				// this is a TTV as we can't correctly trace all renames and etc. But this doesn't affect the overall result,
				// as this is basically exceptional when compared to thousands of commits and changes.
				if(entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
					String oldFileName = enforceUnixPaths(entry.getOldPath());
					pmDatabase.removeFile(oldFileName);
					log.debug("Deleted " + oldFileName + " from PMDatabase.");
					continue;
				}
				// entry.getChangeType() returns "MODIFY" for commit: bc15aee7cfaddde19ba6fefe0d12331fe98ddd46 instead of a rename, it works only if the class file was renamed
				// Thus, we are not tracking class renames here, but that is also not necessary, because the PM metrics are computed for each java file anyways.
				else if(entry.getChangeType() == DiffEntry.ChangeType.RENAME){
					String oldFileName = enforceUnixPaths(entry.getOldPath());
					pmDatabase.renameFile(oldFileName, fileName, superCommitMetaData);
					log.debug("Renamed " + oldFileName + " to " + fileName + " in PMDatabase.");
				}

				// collect number of lines deleted and added in that file
				List<Edit> editList = diffFormatter.toFileHeader(entry).toEditList();
				int linesDeleted = calculateLinesDeleted(editList);
				int linesAdded = calculateLinesAdded(editList);

				// we increase the counter here. This means a class will go to the 'non refactored' bucket
				// only after we see it X times (and not involved in a refactoring, otherwise, counters are resetted).
				ProcessMetricTracker pmTracker = pmDatabase.reportChanges(fileName, superCommitMetaData, commit.getAuthorIdent().getName(), linesAdded, linesDeleted);

				if(TrackDebugMode.ACTIVE && (fileName.contains(TrackDebugMode.FILENAME_TO_TRACK) || commit.getName().contains(TrackDebugMode.COMMIT_TO_TRACK))) {
					ProcessMetricTracker currentClazz = pmDatabase.find(fileName);
					log.debug("[TRACK] Reported commit " + commit.getName() + " to pmTracker affecting class file: " + fileName + "\n" +
							"\t\t\t\t\t\t\tlinesAdded: " + linesAdded + ", linesDeleted: " + linesDeleted + ", author: "
							+ commit.getAuthorIdent().getName() + " and class stability counter is " + currentClazz.getCommitCounter()  + "\n" +
							"\t\t\t\t\t\t\tcurrent ProcessMetrics: " + currentClazz.getCurrentProcessMetrics());
				}

				//The last commit passed the stability threshold for this class file
				if(pmTracker.calculateStability(project.getCommitCountThresholds())){
					outputNonRefactoredClass(pmTracker);

					// we then reset the counter, and start again.
					// it is ok to use the same class more than once, as metrics as well as
					// its source code will/may change, and thus, they are a different instance.
					if(pmTracker.getCommitCountThreshold() == project.getMaxCommitThreshold()){
						log.debug("Reset pmTracker for class " + pmTracker.getFileName() + " with threshold: " + pmTracker.getCommitCountThreshold() +
								" because it is the max threshold(" + project.getMaxCommitThreshold() + ").");
						pmTracker.resetCounter(new CommitMetaData(commit, project));
					}
				}
			}
		}
	}

	//Store the refactoring instances in the DB
	private void outputNonRefactoredClass (ProcessMetricTracker pmTracker) throws IOException {
		RevCommit commitBackThen = getReverseWalk(repository, "master")
				.parseCommit(ObjectId.fromString(pmTracker.getBaseCommitMetaData().getCommitId()));
		log.debug("Class " + pmTracker.getFileName() + " is an example of a not refactored instance with the stable commit: " + commitBackThen.getId());

		String tempDir = null;
		try {
			// we extract the source code from back then (as that's the one that never deserved a refactoring)
			String sourceCodeBackThen = getCleanSourceCode(repository, commitBackThen, pmTracker.getFileName());
			// create a temp dir to store the source code files and run CK there
			tempDir = createTmpDir();

			// we save it in the permanent storage...
			writeFile(fileStoragePath + commitBackThen.getName() + "/" + "not-refactored/" + pmTracker.getFileName(), sourceCodeBackThen);
			// ... as well as in the temp one, so that we can calculate the CK metrics
			writeFile(tempDir + pmTracker.getFileName(), sourceCodeBackThen);

			if(pmTracker.getBaseCommitMetaData().getId() == 0)
				db.persist(pmTracker.getBaseCommitMetaData());

			CommitMetaData commitMetaData = db.loadCommitMetaData(pmTracker.getBaseCommitMetaData().getId());
			List<StableCommit> stableCommits = codeMetrics(commitMetaData, tempDir, pmTracker.getCommitCountThreshold());

			// print its process metrics in the same process metrics file
			// note that we print the process metrics back then (X commits ago)
			for(StableCommit stableCommit : stableCommits) {
				stableCommit.setProcessMetrics(new ProcessMetrics(pmTracker.getBaseProcessMetrics()));
				db.persist(stableCommit);
			}
		} catch(Exception e) {
			log.error(e.getClass().getCanonicalName() + " when processing metrics for commit: " + commitBackThen.getId(), e);
		} finally {
			cleanTempDir(tempDir);
		}
	}

	//TODO: Fix this, as it generates many duplicates
	private List<StableCommit> codeMetrics(CommitMetaData commitMetaData, String tempDir, int commitThreshold) {
		List<StableCommit> stableCommits = new ArrayList<>();
		new CK().calculate(tempDir, ck -> {
			String cleanedCkClassName = cleanClassName(ck.getClassName());
			ClassMetric classMetric = extractClassMetrics(ck);

			Set<CKMethodResult> methods = ck.getMethods();
			for(CKMethodResult ckMethodResult : methods) {
				MethodMetric methodMetrics = extractMethodMetrics(ckMethodResult);

				Set<Map.Entry<String, Integer>> variables = ckMethodResult.getVariablesUsage().entrySet();
				for (Map.Entry<String, Integer> entry : variables) {
					VariableMetric variableMetric = new VariableMetric(entry.getKey(), entry.getValue());

					StableCommit stableCommitV = new StableCommit(
							project,
							commitMetaData,
							enforceUnixPaths(ck.getFile()).replace(tempDir, ""),
							cleanedCkClassName,
							classMetric,
							methodMetrics,
							variableMetric,
							null,
							RefactoringUtils.Level.VARIABLE.ordinal(),
							commitThreshold);

					stableCommits.add(stableCommitV);
				}

				//only add this if there are no variable refactorings
				if(variables.isEmpty()){
					StableCommit stableCommitM = new StableCommit(
							project,
							commitMetaData,
							enforceUnixPaths(ck.getFile()).replace(tempDir, ""),
							cleanedCkClassName,
							classMetric,
							methodMetrics,
							null,
							null,
							RefactoringUtils.Level.METHOD.ordinal(),
							commitThreshold);

					stableCommits.add(stableCommitM);

				}
			}

			Set<String> fields = ck.getMethods().stream().flatMap(x -> x.getFieldUsage().keySet().stream()).collect(Collectors.toSet());
			for(String field : fields) {
				int totalAppearances = ck.getMethods().stream()
						.map(x -> x.getFieldUsage().get(field) == null ? 0 : x.getFieldUsage().get(field))
						.mapToInt(Integer::intValue).sum();

				FieldMetric fieldMetrics = new FieldMetric(field, totalAppearances);

				StableCommit stableCommitF = new StableCommit(
						project,
						commitMetaData,
						enforceUnixPaths(ck.getFile()).replace(tempDir, ""),
						cleanedCkClassName,
						classMetric,
						null,
						null,
						fieldMetrics,
						Level.ATTRIBUTE.ordinal(),
						commitThreshold);

				stableCommits.add(stableCommitF);
			}

			//only add this if there are not method- and field level refactorings
			if(methods.isEmpty() && fields.isEmpty()){
				StableCommit stableCommit = new StableCommit(
						project,
						commitMetaData,
						enforceUnixPaths(ck.getFile()).replace(tempDir, ""),
						cleanedCkClassName,
						classMetric,
						null,
						null,
						null,
						RefactoringUtils.Level.CLASS.ordinal(),
						commitThreshold);

				stableCommits.add(stableCommit);
			}
		});

		return stableCommits;
	}
}