package refactoringml;

import com.github.javaparser.utils.Log;
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
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import refactoringml.db.*;
import refactoringml.util.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;

import static refactoringml.util.JGitUtils.readFileFromGit;
import static refactoringml.util.RefactoringUtils.cleanMethodName;

public class ProcessMetricsCollector {

	private String tempDir;

	// commit hash, file name
	Map<String, Set<Long>> todo;

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
		todo = new HashMap<>();
		pmDatabase = new PMDatabase(commitThreshold);
	}

	public void addToList (RevCommit commitData, Yes yes) {
		String id = commitData.getName();
		if(!todo.containsKey(id))
			todo.put(id, new HashSet<>());

		todo.get(id).add(yes.getId());
	}

	public void collect() throws IOException {

		RevWalk walk = JGitUtils.getReverseWalk(repository, branch);

		RevCommit commit = walk.next();

		boolean lastFound = false;
		while(commit!=null && !lastFound) {
			// did we find the commit to stop?
			// if so, process it, and then stop
			if(commit.getName().equals(lastCommitToProcess))
				lastFound = true;

			if(commit.getParentCount() <= 1) {

				log.debug("Commit ID " + commit.getName());
				RevCommit commitParent = commit.getParentCount() == 0 ? null : commit.getParent(0);

				Set<String> refactoredClasses = new HashSet<>();

				// if the class happened to be refactored, then, print its process metrics at that time
				if (todo.containsKey(commit.getName())) {
					try {
						db.openSession();
						refactoredClasses = collectProcessMetricsOfRefactoredCommit(commit);
						db.commit();
					} catch (Exception e) {
						log.error("Error when collecting process metrics in commit " + commit.getName(), e);
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
				} finally {
					db.close();
				}
			}

			commit = walk.next();
		}
		walk.close();

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
				log.info("[TRACK] Marking it as a non-refactoring instance, and resetting the counter");
				log.info("[TRACK] " + pm.toString());
			}

			outputNonRefactoredClass(pm);

			// we then reset the counter, and start again.
			// it is ok to use the same class more than once, as metrics as well as
			// its source code will/may change, and thus, they are a different instance.
			pm.resetCounter(commit.getName(), JGitUtils.getGregorianCalendar(commit));

		}

	}

	private void updateProcessMetrics(RevCommit commit, RevCommit commitParent) throws IOException {
		try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
			diffFormatter.setRepository(repository);
			diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
			diffFormatter.setDetectRenames(true);

			for (DiffEntry entry : diffFormatter.scan(commitParent, commit)) {
				String fileName = entry.getPath(null);

				if(TrackDebugMode.ACTIVE && fileName.equals(TrackDebugMode.FILE_TO_TRACK)) {
					log.info("[TRACK] File was changed in commit " + commit.getId().getName() + ", updating process metrics");
				}

				// do not collect these numbers if not a java file (save some memory)
				boolean isAJavaFile = fileName.toLowerCase().endsWith("java");
				if (!isAJavaFile) {
					continue;
				}

				// if the class was either removed or deleted, we remove it from our pmDatabase, as to not mess
				// with the refactoring counter...
				// this is a TTV as we can't correctly trace all renames and etc. But this doesn't affect the overall result,
				// as this is basically exceptional when compared to thousands of commits and changes.
				if(entry.getChangeType() == DiffEntry.ChangeType.DELETE || entry.getChangeType() == DiffEntry.ChangeType.RENAME) {
					pmDatabase.remove(entry.getOldPath());

					if(entry.getChangeType() == DiffEntry.ChangeType.DELETE)
						continue;
				}

				// add class to our in-memory pmDatabase
				if(!pmDatabase.containsKey(fileName))
					pmDatabase.put(fileName, new ProcessMetric(fileName, commit.getName(), JGitUtils.getGregorianCalendar(commit)));

				// collect number of lines deleted and added in that file
				int linesDeleted = 0;
				int linesAdded = 0;

				for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
					linesDeleted = edit.getEndA() - edit.getBeginA();
					linesAdded = edit.getEndB() - edit.getBeginB();
				}

				// update our pmDatabase entry with the information of the current commit
				ProcessMetric currentClazz = pmDatabase.get(fileName);
				currentClazz.existsIn(commit.getFullMessage(), commit.getAuthorIdent().getName(), linesAdded, linesDeleted);

				// we increase the counter here. This means a class will go to the 'non refactored' bucket
				// only after we see it X times (and not involved in a refactoring, otherwise, counters are resetted).
				currentClazz.increaseCounter();

				if(TrackDebugMode.ACTIVE && fileName.equals(TrackDebugMode.FILE_TO_TRACK)) {
					log.info("[TRACK] Counter increased to " + currentClazz.counter());
				}

			}
		}
	}

	private Set<String> collectProcessMetricsOfRefactoredCommit(RevCommit commit) {

		Set<String> refactoredClasses = new HashSet<>();
		Set<Long> allYeses = todo.get(commit.getName());

		for (Long yesId : allYeses) {

			Yes yes = db.findYes(yesId);

			String fileName = yes.getFilePath();

			if(TrackDebugMode.ACTIVE && fileName.equals(TrackDebugMode.FILE_TO_TRACK)) {
				log.info("[TRACK] Collecting process metrics at refactoring commit " + commit.getId().getName());
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
						false,
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
					log.info("[TRACK] Not able to find process metrics at " + commit.getId().getName());
				}
			} else {

				dbProcessMetrics = new ProcessMetrics(
						true,
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
				currentProcessMetrics.resetCounter(commit.getName(), JGitUtils.getGregorianCalendar(commit));
			}

			refactoredClasses.add(fileName);

			if(TrackDebugMode.ACTIVE && fileName.equals(TrackDebugMode.FILE_TO_TRACK)) {
				log.info("[TRACK] Number of refactorings involved increased to " + currentProcessMetrics.getRefactoringsInvolved() + " and counter resetted");
			}

		}

		return refactoredClasses;

	}

	private void storeProcessMetric(String fileName, List<No> nos) {

		for(No no : nos) {

			ProcessMetric filePm = pmDatabase.get(fileName);
			ProcessMetrics dbProcessMetrics = new ProcessMetrics(
					//TODO: figure out if there is a smarter way to set the has metrics boolean for this case
					filePm != null,
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
		String commitHashBackThen = clazz.getBaseCommitForNonRefactoring();
		Calendar commitDate = clazz.getBaseCommitDateForNonRefactoring();

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
			List<No> nos = codeMetrics(commitHashBackThen, commitDate);

			// print its process metrics in the same process metrics file
			// note that we print the process metrics back then (X commits ago)
			storeProcessMetric(clazz.getFileName(), nos);
		} catch(Exception e) {
			log.error("Failing when calculating metrics", e);
		} finally {
			cleanTmpDir();
		}

	}

	private List<No> codeMetrics(String commitHashBackThen, Calendar commitDate) {

		List<No> nos = new ArrayList<>();

		new CK().calculate(tempDir, ck -> {

			ClassMetric classMetric = new ClassMetric(ck.getCbo(),
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
					commitHashBackThen,
					commitDate,
					ck.getFile().replace(tempDir, ""),
					ck.getClassName(),
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
						commitHashBackThen,
						commitDate,
						ck.getFile().replace(tempDir, ""),
						ck.getClassName(),
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
							commitHashBackThen,
							commitDate,
							ck.getFile().replace(tempDir, ""),
							ck.getClassName(),
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
						commitHashBackThen,
						commitDate,
						ck.getFile().replace(tempDir, ""),
						ck.getClassName(),
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
