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

public class ProcessMetricsCollector {

	private String tempDir;
	private Project project;
	private Database db;
	private Repository repository;
	private String fileStoragePath;
	//TODO: remove lastCommitToProcess
	private String lastCommitToProcess;

	private PMDatabase pmDatabase;

	private static final Logger log = Logger.getLogger(ProcessMetricsCollector.class);
	private String branch;

	public ProcessMetricsCollector(Project project, Database db, Repository repository, String branch,
	                               String fileStoragePath, String lastCommitToProcess) {
		this.project = project;
		this.db = db;
		this.repository = repository;
		this.branch = branch;
		this.fileStoragePath = FilePathUtils.lastSlashDir(fileStoragePath);
		this.lastCommitToProcess = lastCommitToProcess;

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
		updateProcessMetrics(commit, commitParent);

		// update classes that were not refactored on this commit
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

	private void updateAndPrintExamplesOfNonRefactoredClasses(RevCommit commit) throws IOException {
		// if there are classes over the threshold, we output them as an examples of not refactored classes,
		// and we reset their counter.
		// note that we have a lot of failures here, as X commits later, the class might had been
		// renamed or moved, and thus the class (with the name before) "doesn't exist" anymore..
		// that is still ok as we are collecting thousands of examples.
		// TTV to mention: our sample never contains non refactored classes that were moved or renamed,
		// but that's not a big deal.
		for(ProcessMetricTracker pm : pmDatabase.refactoredLongAgo()) {

			if(TrackDebugMode.ACTIVE && pm.getFileName().contains(TrackDebugMode.FILENAME_TO_TRACK)) {
				log.debug("[TRACK] Marking it as a non-refactoring instance, and resetting the counter");
				log.debug("[TRACK] " + pm.toString());
			}

			outputNonRefactoredClass(pm);

			// we then reset the counter, and start again.
			// it is ok to use the same class more than once, as metrics as well as
			// its source code will/may change, and thus, they are a different instance.
			pm.resetCounter(commit.getName(), commit.getFullMessage(), JGitUtils.getGregorianCalendar(commit));
		}

	}

	private void updateProcessMetrics(RevCommit commit, RevCommit commitParent) throws IOException {
		try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
			diffFormatter.setRepository(repository);
			diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
			diffFormatter.setDetectRenames(true);

			for (DiffEntry entry : diffFormatter.scan(commitParent, commit)) {
				String fileName = enforceUnixPaths(entry.getNewPath());

				if(TrackDebugMode.ACTIVE && fileName.contains(TrackDebugMode.FILENAME_TO_TRACK)) {
					log.debug("[TRACK] File was changed in commit " + commit.getId().getName() + ", thus the process metrics are updated.");
				}

				// do not collect these numbers if not a java file (save some memory)
				if (!refactoringml.util.FileUtils.IsJavaFile(fileName))
					continue;

				// if the class was either removed or deleted, we remove it from our pmDatabase, as to not mess
				// with the refactoring counter...
				// this is a TTV as we can't correctly trace all renames and etc. But this doesn't affect the overall result,
				// as this is basically exceptional when compared to thousands of commits and changes.
				//TODO: track moves e.g. src/java/org/apache/commons/cli/HelpFormatter.java to src/main/java/org/apache/commons/cli/HelpFormatter.java
				if(entry.getChangeType() == DiffEntry.ChangeType.DELETE || entry.getChangeType() == DiffEntry.ChangeType.RENAME) {
					String oldFileName = enforceUnixPaths(entry.getOldPath());
					pmDatabase.remove(oldFileName);

					if(entry.getChangeType() == DiffEntry.ChangeType.DELETE)
						continue;
				}

				// add class to our in-memory pmDatabase
				if(!pmDatabase.containsKey(fileName))
					pmDatabase.put(fileName, new ProcessMetricTracker(fileName, commit.getName(), JGitUtils.getGregorianCalendar(commit)));

				// collect number of lines deleted and added in that file
				int linesDeleted = 0;
				int linesAdded = 0;

				for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
					linesDeleted = edit.getEndA() - edit.getBeginA();
					linesAdded = edit.getEndB() - edit.getBeginB();
				}

				// update our pmDatabase entry with the information of the current commit
				ProcessMetricTracker currentClazz = pmDatabase.get(fileName);
				currentClazz.existsIn(commit.getFullMessage(), commit.getAuthorIdent().getName(), linesAdded, linesDeleted);

				// we increase the counter here. This means a class will go to the 'non refactored' bucket
				// only after we see it X times (and not involved in a refactoring, otherwise, counters are resetted).
				currentClazz.increaseCommitCounter();

				if(TrackDebugMode.ACTIVE && fileName.contains(TrackDebugMode.FILENAME_TO_TRACK)) {
					log.debug("[TRACK] Class stability counter increased to " + currentClazz.getCommitCounter()
							+ " for class: " + currentClazz.getFileName());
				}
			}
		}
	}

	private void collectProcessMetricsOfRefactoredCommit(RevCommit commit, Set<Long> allRefactoringCommits) {
		for (Long refactoringCommitId : allRefactoringCommits) {
			RefactoringCommit refactoringCommit = db.findRefactoringCommit(refactoringCommitId);
			String fileName = refactoringCommit.getFilePath();

			if(TrackDebugMode.ACTIVE && fileName.contains(TrackDebugMode.FILENAME_TO_TRACK)) {
				log.debug("[TRACK] Collecting process metrics at refactoring commit " + commit.getId().getName()
						+ " for class: " + commit.getName());
			}

			ProcessMetricTracker currentProcessMetricsTracker = pmDatabase.get(fileName);

			// we print the information BEFORE updating it with this commit, because we need the data from BEFORE this commit
			// however, we might not be able to find the process metrics of that class.
			// this will happen in strange cases where we never tracked that class before...
			// for now, let's store it as -1, so that we can still use the data point for structural metrics
			ProcessMetrics dbProcessMetrics  = new ProcessMetrics(currentProcessMetricsTracker);

			refactoringCommit.setProcessMetrics(dbProcessMetrics);
			db.update(refactoringCommit);

			// update counters
			if(currentProcessMetricsTracker != null) {
				currentProcessMetricsTracker.increaseRefactoringsInvolved();
				currentProcessMetricsTracker.resetCounter(commit.getName(), commit.getFullMessage(), JGitUtils.getGregorianCalendar(commit));
			}

			if(TrackDebugMode.ACTIVE && fileName.contains(TrackDebugMode.FILENAME_TO_TRACK)) {
				log.debug("[TRACK] Number of refactorings involved increased to " + currentProcessMetricsTracker.getRefactoringsInvolved()
						+ " and class stability counter set to: " + currentProcessMetricsTracker.getCommitCounter() + " for class: " + commit.getName());
			}
		}
	}

	private void storeProcessMetric(String fileName, List<StableCommit> stableCommits) {
		for(StableCommit stableCommit : stableCommits) {
			ProcessMetricTracker filePm = pmDatabase.get(fileName);
			ProcessMetrics dbProcessMetrics = new ProcessMetrics(filePm);

			stableCommit.setProcessMetrics(dbProcessMetrics);
			db.persist(stableCommit);
		}
	}

	private void outputNonRefactoredClass (ProcessMetricTracker clazz) throws IOException {
		CommitMetaData commitMetaData = new CommitMetaData(clazz, project);
		String commitHashBackThen = clazz.getBaseCommitForNonRefactoring();

		String sourceCodeBackThen;
		log.debug("Class " + clazz.getFileName() + " is an example of a not refactored instance with the original commit: " + commitHashBackThen);

		try {
			// we extract the source code from back then (as that's the one that never deserved a refactoring)
			sourceCodeBackThen = SourceCodeUtils.removeComments(readFileFromGit(repository, commitHashBackThen, clazz.getFileName()));
		} catch(Exception e) {
			log.error(e.getClass().getCanonicalName() + " when getting source code of the class " + clazz.getFileName());
			pmDatabase.remove(clazz);
			return;
		}

		try {
			// create a temp dir to store the source code files and run CK there
			tempDir = createTmpDir();
			saveFile(commitHashBackThen, sourceCodeBackThen, clazz.getFileName());

			List<StableCommit> stableCommits = codeMetrics(commitMetaData);

			// print its process metrics in the same process metrics file
			// note that we print the process metrics back then (X commits ago)
			storeProcessMetric(clazz.getFileName(), stableCommits);
		} catch(Exception e) {
			log.error(e.getClass().getCanonicalName() + " when processing metrics for commit: " + commitHashBackThen, e);
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

	private void cleanTmpDir () throws IOException {
		if(tempDir!=null) {
			FileUtils.deleteDirectory(new File(tempDir));
			tempDir = null;
		}
	}

}
