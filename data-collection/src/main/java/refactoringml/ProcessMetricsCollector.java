package refactoringml;

import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKMethodResult;
import com.google.common.io.Files;
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

import static refactoringml.util.CKUtils.cleanClassName;
import static refactoringml.util.JGitUtils.readFileFromGit;
import static refactoringml.util.RefactoringUtils.cleanMethodName;

public class ProcessMetricsCollector {

	private String tempDir;
	private Project project;
	private Database db;
	private Repository repository;
	private String fileStoragePath;
	private String lastCommitToProcess;

	private PMDatabase pmDatabase;

	private static final Logger log = Logger.getLogger(ProcessMetricsCollector.class);
	private String branch;

	public ProcessMetricsCollector(Project project, Database db, Repository repository, String branch, int commitThreshold,
	                               String fileStoragePath, String lastCommitToProcess) {
		this.project = project;
		this.db = db;
		this.repository = repository;
		this.branch = branch;
		this.fileStoragePath = FilePathUtils.lastSlashDir(fileStoragePath);
		this.lastCommitToProcess = lastCommitToProcess;
		pmDatabase = new PMDatabase(commitThreshold);
	}

	public void collectMetrics(RevCommit commit, Set<Long> allYeses, boolean isRefactoring) throws IOException {
		RevCommit commitParent = commit.getParentCount() == 0 ? null : commit.getParent(0);
		Set<String> refactoredClasses = new HashSet<>();

		//if this commit contained a refactoring, then collect its process metrics,
		//otherwise only update the file process metrics
		if (isRefactoring) {
			try {
				db.openSession();
				refactoredClasses = collectProcessMetricsOfRefactoredCommit(commit, allYeses);
				db.commit();
			} catch (Exception e) {
				log.error("Error when collecting process metrics in commit " + commit.getName(), e);
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
			updateAndPrintExamplesOfNonRefactoredClasses(commit, refactoredClasses);
			db.commit();
		} catch (Exception e) {
			log.error("Error when collecting process metrics in commit " + commit.getName(), e);
			db.rollback();
		} finally {
			db.close();
		}
	}

	private void updateAndPrintExamplesOfNonRefactoredClasses(RevCommit commit, Set<String> refactoredClasses) throws IOException {
		// if there are classes over the threshold, we output them as an examples of not refactored classes,
		// and we reset their counter.
		// note that we have a lot of failures here, as X commits later, the class might had been
		// renamed or moved, and thus the class (with the name before) "doesn't exist" anymore..
		// that is still ok as we are collecting thousands of examples.
		// TTV to mention: our sample never contains non refactored classes that were moved or renamed,
		// but that's not a big deal.
		for(ProcessMetric pm : pmDatabase.refactoredLongAgo()) {
			if(TrackDebugMode.ACTIVE && pm.getFileName().equals(TrackDebugMode.FILE_TO_TRACK)) {
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

			// do not collect these numbers if not a java file (save some memory)
			List<DiffEntry> javaDiffs = diffFormatter.scan(commitParent, commit).stream()
					.filter(entry -> entry.getNewPath().toLowerCase().endsWith("java")).collect(Collectors.toList());

			for (DiffEntry entry : javaDiffs) {
				String fileName = entry.getNewPath();

				if(TrackDebugMode.ACTIVE && fileName.equals(TrackDebugMode.FILE_TO_TRACK)) {
					log.debug("[TRACK] File was changed in commit " + commit.getId().getName() + ", updating process metrics");
				}

				// if the class was deleted, we remove it from our pmDatabase, as to not mess
				// with the refactoring counter...
				// this is a TTV as we can't correctly trace all renames and etc. But this doesn't affect the overall result,
				// as this is basically exceptional when compared to thousands of commits and changes.
				if(entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
					pmDatabase.remove(entry.getOldPath());
					log.debug("Removed " + entry.getOldPath() + " from PMDatabase.");
					continue;
				}
				// entry.getChangeType() returns "MODIFY" for commit: bc15aee7cfaddde19ba6fefe0d12331fe98ddd46 instead of a rename, it works only if the class file was renamed
				// Thus, we are not tracking class renames here, but that is also not necessary, because the PM metrics are computed for each java file anyways.
				else if(entry.getChangeType() == DiffEntry.ChangeType.RENAME){
					String oldFileName = entry.getOldPath();
					pmDatabase.rename(oldFileName, fileName);
					log.debug("Renamed " + oldFileName + " to " + fileName + " in PMDatabase.");
				}

				//TODO: track class names instead of filenames for the process metrics
				// add file () to our in-memory pmDatabase
				if(!pmDatabase.containsKey(fileName))
					pmDatabase.put(fileName, new ProcessMetric(fileName, commit.getName(), JGitUtils.getGregorianCalendar(commit)));

				// collect number of lines deleted and added in that file
				int linesDeleted = 0;
				int linesAdded = 0;

				//wrong, this always overrides previous results
				for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
					linesDeleted += edit.getEndA() - edit.getBeginA();
					linesAdded += edit.getEndB() - edit.getBeginB();
				}

				// update our pmDatabase entry with the information of the current commit
				ProcessMetric currentClazz = pmDatabase.get(fileName);
				currentClazz.existsIn(commit.getFullMessage(), commit.getAuthorIdent().getName(), linesAdded, linesDeleted);

				// we increase the counter here. This means a class will go to the 'non refactored' bucket
				// only after we see it X times (and not involved in a refactoring, otherwise, counters are resetted).
				currentClazz.increaseCounter();

				if(TrackDebugMode.ACTIVE && fileName.equals(TrackDebugMode.FILE_TO_TRACK)) {
					log.debug("[TRACK] Class stability counter increased to " + currentClazz.counter() + " for file: " + fileName);
				}
			}
		}
	}

	private Set<String> collectProcessMetricsOfRefactoredCommit(RevCommit commit, Set<Long> allYeses) {
		Set<String> refactoredClasses = new HashSet<>();

		for (Long yesId : allYeses) {
			Yes yes = db.findYes(yesId);
			//TODO: the filePath from yes and Diffentry do not always match, e.g.
			// C:\Users\jange\AppData\Local\Temp\1581607643389-0\a\Animal.java and a/Animal.java from toyrepo r4
			String fileName = yes.getFilePath();

			if(TrackDebugMode.ACTIVE && fileName.equals(TrackDebugMode.FILE_TO_TRACK)) {
				log.debug("[TRACK] Collecting process metrics at refactoring commit " + commit.getId().getName() + " for file: " + fileName);
			}

			ProcessMetric currentProcessMetrics = pmDatabase.get(fileName);
			ProcessMetrics dbProcessMetrics;

			// we print the information BEFORE updating it with this commit, because we need the data from BEFORE this commit
			// however, we might not be able to find the process metrics of that class.
			// this will happen in strange cases where we never tracked that class before...
			// for now, let's store it as -1, so that we can still use the data point for structural metrics
			// TODO: better track renames. As soon as a class is renamed, transfer its process metrics.
			if(currentProcessMetrics == null) {
				dbProcessMetrics = new ProcessMetrics(
						-1,
						-1,
						-1,
						-1,
						-1,
						-1,
						-1,
						-1,
						-1
				);

				log.error("Not able to find process metrics for file " + fileName + " (commit " + commit.getName() + ")");
				if(TrackDebugMode.ACTIVE && fileName.equals(TrackDebugMode.FILE_TO_TRACK)) {
					log.debug("[TRACK] Not able to find process metrics at " + commit.getId().getName() + " for file: " + fileName);
				}
			} else {

				dbProcessMetrics = new ProcessMetrics(
						currentProcessMetrics.qtyOfCommits(),
						currentProcessMetrics.getLinesAdded(),
						currentProcessMetrics.getLinesDeleted(),
						currentProcessMetrics.qtyOfAuthors(),
						currentProcessMetrics.qtyMinorAuthors(),
						currentProcessMetrics.qtyMajorAuthors(),
						currentProcessMetrics.authorOwnership(),
						currentProcessMetrics.getBugFixCount(),
						currentProcessMetrics.getRefactoringsInvolved()
				);
			}
			yes.setProcessMetrics(dbProcessMetrics);
			db.update(yes);

			// update counters
			if(currentProcessMetrics != null) {
				currentProcessMetrics.increaseRefactoringsInvolved();
				currentProcessMetrics.resetCounter(commit.getName(), commit.getFullMessage(), JGitUtils.getGregorianCalendar(commit));
			}

			refactoredClasses.add(fileName);

			if(TrackDebugMode.ACTIVE && fileName.equals(TrackDebugMode.FILE_TO_TRACK)) {
				log.debug("[TRACK] Number of refactorings involved increased to " + currentProcessMetrics.getRefactoringsInvolved()
						+ " and class stability counter was set to 0 for file: " + fileName);
			}

		}

		return refactoredClasses;

	}

	private void storeProcessMetric(String fileName, List<No> nos) {

		for(No no : nos) {

			ProcessMetric filePm = pmDatabase.get(fileName);
			ProcessMetrics dbProcessMetrics = new ProcessMetrics(
					filePm.getBaseCommits(),
					filePm.getBaseLinesAdded(),
					filePm.getBaseLinesDeleted(),
					filePm.getBaseAuthors(),
					filePm.getBaseMinorAuthors(),
					filePm.getBaseMajorAuthors(),
					filePm.getBaseAuthorOwnership(),
					filePm.getBaseBugFixCount(),
					filePm.getBaseRefactoringsInvolved());

			no.setProcessMetrics(dbProcessMetrics);
			db.persist(no);
		}

	}

	private void outputNonRefactoredClass (ProcessMetric clazz) throws IOException {
		CommitMetaData commitMetaData = new CommitMetaData(clazz, project);

		String commitHashBackThen = clazz.getBaseCommitForNonRefactoring();

		String sourceCodeBackThen;
		log.info("Class " + clazz.getFileName() + " is an example of not refactored (original commit " + commitHashBackThen + ")");

		try {
			// we extract the source code from back then (as that's the one that never deserved a refactoring)
			sourceCodeBackThen = SourceCodeUtils.removeComments(readFileFromGit(repository, commitHashBackThen, clazz.getFileName()));
		} catch(Exception e) {
			log.error("Failed when getting source code of the class... The class was probably moved or deleted...");
			pmDatabase.remove(clazz);
			return;
		}

		try {
			// create a temp dir to store the source code files and run CK there
			createTempDir();

			saveFile(commitHashBackThen, sourceCodeBackThen, clazz.getFileName());

			List<No> nos = codeMetrics(commitMetaData);

			// print its process metrics in the same process metrics file
			// note that we print the process metrics back then (X commits ago)
			storeProcessMetric(clazz.getFileName(), nos);
		} catch(Exception e) {
			log.error("Failing when calculating metrics", e);
		} finally {
			cleanTmpDir();
		}

	}

	private List<No> codeMetrics(CommitMetaData commitMetaData) {

		List<No> nos = new ArrayList<>();

		new CK().calculate(tempDir, ck -> {
			String cleanedCkClassName = cleanClassName(ck.getClassName());
			ClassMetric classMetric = new ClassMetric(
					CKUtils.evaluateSubclass(ck.getType()),
					ck.getCbo(),
					ck.getWmc(),
					ck.getRfc(),
					ck.getLcom(),
					ck.getNumberOfMethods(),
					ck.getNumberOfStaticMethods(),
					ck.getNumberOfPublicMethods(),
					ck.getNumberOfPrivateMethods(),
					ck.getNumberOfProtectedMethods(),
					ck.getNumberOfDefaultMethods(),
					ck.getNumberOfAbstractMethods(),
					ck.getNumberOfFinalMethods(),
					ck.getNumberOfSynchronizedMethods(),
					ck.getNumberOfFields(),
					ck.getNumberOfStaticFields(),
					ck.getNumberOfPublicFields(),
					ck.getNumberOfPrivateFields(),
					ck.getNumberOfProtectedFields(),
					ck.getNumberOfDefaultFields(),
					ck.getNumberOfFinalFields(),
					ck.getNumberOfSynchronizedFields(),
					ck.getNosi(),
					ck.getLoc(),
					ck.getReturnQty(),
					ck.getLoopQty(),
					ck.getComparisonsQty(),
					ck.getTryCatchQty(),
					ck.getParenthesizedExpsQty(),
					ck.getStringLiteralsQty(),
					ck.getNumbersQty(),
					ck.getAssignmentsQty(),
					ck.getMathOperationsQty(),
					ck.getVariablesQty(),
					ck.getMaxNestedBlocks(),
					ck.getAnonymousClassesQty(),
					ck.getSubClassesQty(),
					ck.getLambdasQty(),
					ck.getUniqueWordsQty());


			No no = new No(
					project,
					commitMetaData,
					ck.getFile().replace(tempDir, ""),
					cleanedCkClassName,
					classMetric,
					null,
					null,
					null,
					RefactoringUtils.TYPE_CLASS_LEVEL);

			nos.add(no);


			for(CKMethodResult ckMethodResult : ck.getMethods()) {
				MethodMetric methodMetrics = new MethodMetric(
						CKUtils.simplifyFullName(ckMethodResult.getMethodName()),
						cleanMethodName(ckMethodResult.getMethodName()),
						ckMethodResult.getStartLine(),
						ckMethodResult.getCbo(),
						ckMethodResult.getWmc(),
						ckMethodResult.getRfc(),
						ckMethodResult.getLoc(),
						ckMethodResult.getReturnQty(),
						ckMethodResult.getVariablesQty(),
						ckMethodResult.getParametersQty(),
						ckMethodResult.getLoopQty(),
						ckMethodResult.getComparisonsQty(),
						ckMethodResult.getTryCatchQty(),
						ckMethodResult.getParenthesizedExpsQty(),
						ckMethodResult.getStringLiteralsQty(),
						ckMethodResult.getNumbersQty(),
						ckMethodResult.getAssignmentsQty(),
						ckMethodResult.getMathOperationsQty(),
						ckMethodResult.getMaxNestedBlocks(),
						ckMethodResult.getAnonymousClassesQty(),
						ckMethodResult.getSubClassesQty(),
						ckMethodResult.getLambdasQty(),
						ckMethodResult.getUniqueWordsQty()
				);

				No noM = new No(
						project,
						commitMetaData,
						ck.getFile().replace(tempDir, ""),
						cleanedCkClassName,
						classMetric,
						methodMetrics,
						null,
						null,
						RefactoringUtils.TYPE_METHOD_LEVEL);

				nos.add(noM);

				for (Map.Entry<String, Integer> entry : ckMethodResult.getVariablesUsage().entrySet()) {
					VariableMetric variableMetric = new VariableMetric(entry.getKey(), entry.getValue());

					No noV = new No(
							project,
							commitMetaData,
							ck.getFile().replace(tempDir, ""),
							cleanedCkClassName,
							classMetric,
							methodMetrics,
							variableMetric,
							null,
							RefactoringUtils.TYPE_VARIABLE_LEVEL);

					nos.add(noV);

				}

			}

			Set<String> fields = ck.getMethods().stream().flatMap(x -> x.getFieldUsage().keySet().stream()).collect(Collectors.toSet());

			for(String field : fields) {
				int totalAppearances = ck.getMethods().stream()
						.map(x -> x.getFieldUsage().get(field) == null ? 0 : x.getFieldUsage().get(field))
						.mapToInt(Integer::intValue).sum();

				FieldMetric fieldMetrics = new FieldMetric(field, totalAppearances);

				No noF = new No(
						project,
						commitMetaData,
						ck.getFile().replace(tempDir, ""),
						cleanedCkClassName,
						classMetric,
						null,
						null,
						fieldMetrics,
						RefactoringUtils.TYPE_ATTRIBUTE_LEVEL);

				nos.add(noF);
			}
		});

		return nos;
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

	private void createTempDir() {
		tempDir = FilePathUtils.lastSlashDir(Files.createTempDir().getAbsolutePath());
	}

	private void cleanTmpDir () throws IOException {
		if(tempDir!=null) {
			FileUtils.deleteDirectory(new File(tempDir));
			tempDir = null;
		}
	}

}
