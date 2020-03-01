package refactoringml;

import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKMethodResult;
import org.apache.log4j.Logger;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.diff.RawTextComparator;
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
import static refactoringml.util.JGitUtils.readFileFromGit;
import static refactoringml.util.RefactoringUtils.*;

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
		List<Integer> stableCommitThresholds = project.getCommitCountThresholds();
		pmDatabase = new PMDatabase(stableCommitThresholds);
	}

	//if this commit contained a refactoring, then collect its process metrics for all affected class files,
	//otherwise only update the file process metrics
	public void collectMetrics(RevCommit commit, List<RefactoringCommit> allRefactoringCommits, boolean isRefactoring) throws IOException {
		if (isRefactoring) {
			try {
				db.openSession();
				collectProcessMetricsOfRefactoredCommit(commit, allRefactoringCommits);
				db.commit();
			} catch (Exception e) {
				log.error(e.getClass().getCanonicalName() + " when collecting process metrics for commit " + commit.getName(), e);
				db.rollback();
			} finally {
				db.close();
			}
		}

		// we go now change by change in the commit to update the process metrics there
		// (no need for db here, as this update happens only locally)
		RevCommit commitParent = commit.getParentCount() == 0 ? null : commit.getParent(0);
		updateProcessMetrics(commit, commitParent);

		try {
			db.openSession();
			updateAndPrintExamplesOfNonRefactoredClasses(commit);
			db.commit();
		} catch (Exception e) {
			log.error("Error when collecting process metrics in commit " + commit.getName(), e);
			db.rollback();
		} finally {
			db.close();
		}
	}

	//Collect the ProcessMetrics of the RefactoringCommit before this commit happened and update the database entry with it
	private void collectProcessMetricsOfRefactoredCommit(RevCommit commit, List<RefactoringCommit> allRefactoringCommits) {
		for (RefactoringCommit refactoringCommit : allRefactoringCommits) {
			String fileName = refactoringCommit.getFilePath();
			ProcessMetricTracker currentProcessMetricsTracker = pmDatabase.find(fileName);

			ProcessMetrics dbProcessMetrics  = currentProcessMetricsTracker != null ?
					new ProcessMetrics(currentProcessMetricsTracker.getCurrentProcessMetrics()) :
					new ProcessMetrics(-1, -1, -1, -1, -1);

			refactoringCommit.setProcessMetrics(dbProcessMetrics);
			db.update(refactoringCommit);

			pmDatabase.reportRefactoring(fileName, new CommitMetaData(commit, project));

			if(TrackDebugMode.ACTIVE && (fileName.contains(TrackDebugMode.FILENAME_TO_TRACK) || commit.getName().contains(TrackDebugMode.COMMIT_TO_TRACK))) {
				log.debug("[TRACK] Collected process metrics at refactoring commit " + commit.getId().getName() + " for class: " + commit.getName()
								+ " and class stability counter was set to: " + currentProcessMetricsTracker.getCommitCounter() + "\n" +
						"\t\t\t\t\t\t\tRefactoringCommit ProcessMetrics: " + refactoringCommit.getProcessMetrics());
			}
		}
	}

	// update classes that were not refactored on this commit
	private void updateAndPrintExamplesOfNonRefactoredClasses(RevCommit commit) throws IOException {
		// if there are classes over the threshold, we output them as an examples of not refactored classes,
		// and we reset their counter.
		// note that we have a lot of failures here, as X commits later, the class might had been
		// renamed or moved, and thus the class (with the name before) "doesn't exist" anymore..
		// that is still ok as we are collecting thousands of examples.
		// TTV to mention: our sample never contains non refactored classes that were moved or renamed,
		// but that's not a big deal.
		for(ProcessMetricTracker pmTracker : pmDatabase.findStableInstances()) {
			if(TrackDebugMode.ACTIVE && (pmTracker.getFileName().contains(TrackDebugMode.FILENAME_TO_TRACK) || commit.getName().contains(TrackDebugMode.COMMIT_TO_TRACK))) {
				log.debug("[TRACK] Marking class file " + pmTracker.getFileName() + " as a non-refactoring instance.\n" +
						pmTracker.toString());
			}

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

	//Update the process metrics of all affected class files:
	//Reset the PMTracker for all class files, that were refactored on this commit
	//Increase the PMTracker for all class files, that were not refactored but changed on this commit
	private void updateProcessMetrics(RevCommit commit, RevCommit commitParent) throws IOException {
		try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
			diffFormatter.setRepository(repository);
			diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
			diffFormatter.setDetectRenames(true);

			for (DiffEntry entry : diffFormatter.scan(commitParent, commit)) {
				String fileName = enforceUnixPaths(entry.getNewPath());

				// do not collect these numbers if not a java file (save some memory)
				if (!refactoringml.util.FileUtils.IsJavaFile(fileName))
					continue;

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
					pmDatabase.renameFile(oldFileName, fileName, new CommitMetaData(commit, project));
					log.debug("Renamed " + oldFileName + " to " + fileName + " in PMDatabase.");
				}

				// collect number of lines deleted and added in that file
				List<Edit> editList = diffFormatter.toFileHeader(entry).toEditList();
				int linesDeleted = calculateLinesDeleted(editList);
				int linesAdded = calculateLinesAdded(editList);

				// we increase the counter here. This means a class will go to the 'non refactored' bucket
				// only after we see it X times (and not involved in a refactoring, otherwise, counters are resetted).
				pmDatabase.reportChanges(fileName, new CommitMetaData(commit, project), commit.getAuthorIdent().getName(), linesAdded, linesDeleted);

				if(TrackDebugMode.ACTIVE && (fileName.contains(TrackDebugMode.FILENAME_TO_TRACK) || commit.getName().contains(TrackDebugMode.COMMIT_TO_TRACK))) {
					ProcessMetricTracker currentClazz = pmDatabase.find(fileName);
					log.debug("[TRACK] Reported commit " + commit.getName() + " to pmTracker affecting class file: " + fileName + "\n" +
							"\t\t\t\t\t\t\tlinesAdded: " + linesAdded + ", linesDeleted: " + linesDeleted + ", author: "
							+ commit.getAuthorIdent().getName() + " and class stability counter is " + currentClazz.getCommitCounter()  + "\n" +
							"\t\t\t\t\t\t\tcurrent ProcessMetrics: " + currentClazz.getCurrentProcessMetrics());
				}
			}
		}
	}

	//Store the refactoring instances in the DB
	private void outputNonRefactoredClass (ProcessMetricTracker pmTracker) throws IOException {
		String commitHashBackThen = pmTracker.getBaseCommitMetaData().getCommitId();
		log.debug("Class " + pmTracker.getFileName() + " is an example of a not refactored instance with the stable commit: " + commitHashBackThen);

		String tempDir = null;
		try {
			// we extract the source code from back then (as that's the one that never deserved a refactoring)
			String sourceCodeBackThen = SourceCodeUtils.removeComments(readFileFromGit(repository, commitHashBackThen, pmTracker.getFileName()));

			// create a temp dir to store the source code files and run CK there
			tempDir = createTmpDir();

			// we save it in the permanent storage...
			writeFile(fileStoragePath + commitHashBackThen + "/" + "not-refactored/" + pmTracker.getFileName(), sourceCodeBackThen);
			// ... as well as in the temp one, so that we can calculate the CK metrics
			writeFile(tempDir + pmTracker.getFileName(), sourceCodeBackThen);

			CommitMetaData commitMetaData = new CommitMetaData(pmTracker.getBaseCommitMetaData());
			List<StableCommit> stableCommits = codeMetrics(commitMetaData, tempDir, pmTracker.getCommitCountThreshold());

			// print its process metrics in the same process metrics file
			// note that we print the process metrics back then (X commits ago)
			for(StableCommit stableCommit : stableCommits) {
				stableCommit.setProcessMetrics(new ProcessMetrics(pmTracker.getBaseProcessMetrics()));
				db.persist(stableCommit);
			}
		} catch(Exception e) {
			log.error(e.getClass().getCanonicalName() + " when processing metrics for commit: " + commitHashBackThen, e);
		} finally {
			cleanTempDir(tempDir);
		}
	}

	private List<StableCommit> codeMetrics(CommitMetaData commitMetaData, String tempDir, int commitThreshold) {
		List<StableCommit> stableCommits = new ArrayList<>();
		new CK().calculate(tempDir, ck -> {
			String cleanedCkClassName = cleanClassName(ck.getClassName());
			ClassMetric classMetric = extractClassMetrics(ck);
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

			for(CKMethodResult ckMethodResult : ck.getMethods()) {
				MethodMetric methodMetrics = extractMethodMetrics(ckMethodResult);

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

				for (Map.Entry<String, Integer> entry : ckMethodResult.getVariablesUsage().entrySet()) {
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
						RefactoringUtils.Level.ATTRIBUTE.ordinal(),
						commitThreshold);

				stableCommits.add(stableCommitF);
			}
		});

		return stableCommits;
	}
}