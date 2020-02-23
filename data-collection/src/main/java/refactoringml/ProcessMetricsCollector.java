package refactoringml;

import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKMethodResult;
import org.apache.commons.io.FileUtils;
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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

import static refactoringml.util.CKUtils.*;
import static refactoringml.util.FilePathUtils.enforceUnixPaths;
import static refactoringml.util.FileUtils.createTmpDir;
import static refactoringml.util.JGitUtils.readFileFromGit;
import static refactoringml.util.RefactoringUtils.calculateLinesAdded;
import static refactoringml.util.RefactoringUtils.calculateLinesDeleted;

public class ProcessMetricsCollector {
	private String tempDir;
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

		int stableCommitThreshold = project.getCommitCountThresholds().get(0);
		pmDatabase = new PMDatabase(stableCommitThreshold);
	}

	public void collectMetrics(RevCommit commit, Set<Long> allRefactoringCommits, boolean isRefactoring) throws IOException {
		RevCommit commitParent = commit.getParentCount() == 0 ? null : commit.getParent(0);

		//if this commit contained a refactoring, then collect its process metrics,
		//otherwise only update the file process metrics
		if (isRefactoring) {
			try {
				db.openSession();
				collectProcessMetricsRefactorings(allRefactoringCommits);
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
		updateProcessMetricsDB(commit, commitParent, new CommitMetaData(commit, project));

		// update classes that were not refactored on this commit
		try {
			db.openSession();
			collectProcessMetricsStable(commit);
			db.commit();
		} catch (Exception e) {
			log.error("Error when collecting process metrics in commit " + commit.getName(), e);
			db.rollback();
		} finally {
			db.close();
		}
	}

	//Collect all process metrics for refactored classes and write them to the sql database
	//Update the refactored class files in the in-memory process metrics database
	private void collectProcessMetricsRefactorings(Set<Long> allRefactoringCommits) {
		for (Long refactoringCommitId : allRefactoringCommits) {
			RefactoringCommit refactoringCommit = db.findRefactoringCommit(refactoringCommitId);
			String fileName = refactoringCommit.getFilePath();

			// we print the information BEFORE updating it with this commit, because we need the data from BEFORE this commit
			// however, we might not be able to find the process metrics of that class.
			// this will happen in strange cases where we never tracked that class before...
			// for now, let's store it as -1, so that we can still use the data point for structural metrics
			ProcessMetricTracker currentProcessMetricsTracker = pmDatabase.find(fileName);
			ProcessMetrics dbProcessMetrics = currentProcessMetricsTracker != null ?
					new ProcessMetrics(currentProcessMetricsTracker.getCurrentProcessMetrics()) :
					new ProcessMetrics(-1, -1, -1, -1, -1);

			refactoringCommit.setProcessMetrics(dbProcessMetrics);
			db.update(refactoringCommit);

			pmDatabase.reportRefactoring(fileName, refactoringCommit.getCommitMetaData());

			if(TrackDebugMode.ACTIVE && (fileName.contains(TrackDebugMode.FILENAME_TO_TRACK) || refactoringCommit.getCommitMetaData().getCommit().contains(TrackDebugMode.COMMIT_TO_TRACK))) {
				log.debug("[TRACK] Collected process metrics at refactoring commit " + refactoringCommitId
						+ " for class: " + refactoringCommit.getCommitMetaData().getCommit() + "\n" +
						"\t\t\t\t\t\t\tRefactoringCommit ProcessMetrics: " + refactoringCommit.getProcessMetrics());
				log.debug("[TRACK] Number of refactorings involved increased to " + currentProcessMetricsTracker.getCurrentProcessMetrics().refactoringsInvolved
						+ " and class stability counter was reduced to: " + currentProcessMetricsTracker.getCommitCounter() + " for class: " + currentProcessMetricsTracker.getFileName());
			}
		}
	}

	//Update the in memory process metrics database with the changes introduced by the current commit, e.g. lines added or deleted
	//Track renames, deletions and moves of class files
	private void updateProcessMetricsDB(RevCommit commit, RevCommit commitParent, CommitMetaData commitMetaData) throws IOException {
		try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
			diffFormatter.setRepository(repository);
			diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
			diffFormatter.setDetectRenames(true);

			for (DiffEntry entry : diffFormatter.scan(commitParent, commit)) {
				String fileName = enforceUnixPaths(entry.getNewPath());

				// do not collect these numbers if not a java file (save some memory)
				if (!refactoringml.util.FileUtils.IsJavaFile(fileName))
					continue;

				if(TrackDebugMode.ACTIVE && (fileName.contains(TrackDebugMode.FILENAME_TO_TRACK) || commit.getName().contains(TrackDebugMode.COMMIT_TO_TRACK))) {
					log.debug("[TRACK] File was changed in commit " + commit.getId().getName() + ", thus the process metrics are updated.");
				}

				// if the class was either removed or deleted, we remove it from our pmDatabase, as to not mess
				// with the refactoring counter...
				// this is a TTV as we can't correctly trace all renames and etc. But this doesn't affect the overall result,
				// as this is basically exceptional when compared to thousands of commits and changes.
				//TODO: track moves e.g. src/java/org/apache/commons/cli/HelpFormatter.java to src/main/java/org/apache/commons/cli/HelpFormatter.java
				if(entry.getChangeType() == DiffEntry.ChangeType.DELETE || entry.getChangeType() == DiffEntry.ChangeType.RENAME) {
					String oldFileName = enforceUnixPaths(entry.getOldPath());
					pmDatabase.removeFile(oldFileName);

					if(entry.getChangeType() == DiffEntry.ChangeType.DELETE)
						continue;
				}

				// collect number of lines deleted and added in that file
				List<Edit> editList = diffFormatter.toFileHeader(entry).toEditList();
				int linesDeleted = calculateLinesDeleted(editList);
				int linesAdded = calculateLinesAdded(editList);

				// we increase the counter here. This means a class will go to the 'non refactored' bucket
				// only after we see it X times (and not involved in a refactoring, otherwise, counters are resetted).
				pmDatabase.reportChanges(fileName, commitMetaData, commit.getAuthorIdent().getName(), linesAdded, linesDeleted);

				if(TrackDebugMode.ACTIVE && (fileName.contains(TrackDebugMode.FILENAME_TO_TRACK) || commit.getName().contains(TrackDebugMode.COMMIT_TO_TRACK))) {
					log.debug("[TRACK] Reported commit " + commit.getName() + " to pmTracker affecting class file: " + fileName + "\n" +
							"\t\t\t\t\t\t\tlinesAdded: " + linesAdded + ", linesDeleted: " + linesDeleted + ", author: " + commit.getAuthorIdent().getName());
					ProcessMetricTracker currentClazz = pmDatabase.find(fileName);
					log.debug("[TRACK] Class stability counter increased to " + currentClazz.getCommitCounter() + " for class: " + fileName + "\n" +
							"\t\t\t\t\t\t\tcurrent ProcessMetrics: " + currentClazz.getCurrentProcessMetrics());
				}
			}
		}
	}

	//Collect the process metrics of stable instances and update the sql database, e.g. a class file that was not refactored in the last 10 commits
	//Furthermore, collect the source code for the stable commits
	private void collectProcessMetricsStable(RevCommit commit) throws IOException {
		// if there are classes over the threshold, we output them as an examples of not refactored classes,
		// and we reset their counter.
		for(ProcessMetricTracker pm : pmDatabase.findStableInstances()) {
			if(TrackDebugMode.ACTIVE && (pm.getFileName().contains(TrackDebugMode.FILENAME_TO_TRACK) || commit.getName().contains(TrackDebugMode.COMMIT_TO_TRACK))) {
				log.debug("[TRACK] Marking it as a non-refactoring instance, and resetting the counter");
				log.debug("[TRACK] " + pm.toString());
			}

			collectStableInstancesMetrics(pm);

			// Reset the counter, and start again. It is ok to use the same class more than once, as metrics as well as
			// its source code will/may change, and thus, they are a different instance.
			pm.resetCounter(new CommitMetaData(commit, project));
		}
	}

	//Collect all metrics for the stable instances by analyzing their source code with CK
	//Write those metrics to the sql database
	private void collectStableInstancesMetrics(ProcessMetricTracker pmTracker) throws IOException {
		String commitHashBackThen = pmTracker.getBaseCommitMetaData().getCommit();
		log.debug("Class " + pmTracker.getFileName() + " is an example of a not refactored instance with the original commit: " + commitHashBackThen);

		try {
			String sourceCodeBackThen = SourceCodeUtils.removeComments(readFileFromGit(repository, commitHashBackThen, pmTracker.getFileName()));
			// create a temp dir to store the source code files and run CK there
			tempDir = createTmpDir();
			saveFile(commitHashBackThen, sourceCodeBackThen, pmTracker.getFileName());

			// print its process metrics in the same process metrics file
			// note that we print the process metrics back then (X commits ago)
			CommitMetaData commitMetaData = pmTracker.getBaseCommitMetaData();
			for(StableCommit stableCommit :  codeMetrics(commitMetaData)) {
				stableCommit.setProcessMetrics(pmTracker.getBaseProcessMetrics());
				db.persist(stableCommit);
			}
		} catch(Exception e) {
			log.error(e.getClass().getCanonicalName() + " when getting or analyzing the source code of the class " + pmTracker.getFileName());
			pmDatabase.removeFile(pmTracker.getFileName());
		} finally {
			cleanTmpDir();
		}
	}

	private List<StableCommit> codeMetrics(CommitMetaData commitMetaData) {
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
					RefactoringUtils.TYPE_CLASS_LEVEL);

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
						RefactoringUtils.TYPE_METHOD_LEVEL);

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
							RefactoringUtils.TYPE_VARIABLE_LEVEL);

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
						RefactoringUtils.TYPE_ATTRIBUTE_LEVEL);

				stableCommits.add(stableCommitF);
			}
		});

		return stableCommits;
	}

	//TODO: move this to file utils
	private void saveFile (String commitBackThen, String sourceCodeBackThen, String fileName) throws IOException {
		// we save it in the permanent storage...
		new File(fileStoragePath + commitBackThen + "/" + "not-refactored/" + FilePathUtils.dirsOnly(fileName)).mkdirs();
		PrintStream ps = new PrintStream(fileStoragePath + commitBackThen + "/" + "not-refactored/" + fileName);
		ps.print(sourceCodeBackThen);
		ps.close();

		// ... as well as in the temp one, so that we can calculate the CK metrics

		new File(tempDir + FilePathUtils.dirsOnly(fileName)).mkdirs();
		ps = new PrintStream(tempDir + fileName);
		ps.print(sourceCodeBackThen);
		ps.close();
	}

	//TODO: move this to file utils
	private void cleanTmpDir () throws IOException {
		if(tempDir!=null) {
			FileUtils.deleteDirectory(new File(tempDir));
			tempDir = null;
		}
	}
}