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
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import refactoringml.db.*;
import refactoringml.util.CKUtils;
import refactoringml.util.CSVUtils;
import refactoringml.util.FilePathUtils;
import refactoringml.util.RefactoringUtils;

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

	private PMDatabase pmDatabase;

	private static final Logger log = Logger.getLogger(ProcessMetricsCollector.class);
	private String branch;

	public ProcessMetricsCollector(Project project, Database db, Repository repository, String branch, int commitThreshold,
	                               String fileStoragePath) {
		this.project = project;
		this.db = db;
		this.repository = repository;
		this.branch = branch;
		this.fileStoragePath = FilePathUtils.lastSlashDir(fileStoragePath);
		todo = new HashMap<>();
		pmDatabase = new PMDatabase(commitThreshold);
		this.tempDir = FilePathUtils.lastSlashDir(Files.createTempDir().getAbsolutePath());
	}

	public void addToList (RevCommit commitData, Yes yes) {
		String id = commitData.getName();
		if(!todo.containsKey(id))
			todo.put(id, new HashSet<>());

		todo.get(id).add(yes.getId());
	}

	public void collect() throws IOException {

		RevWalk walk = new RevWalk(repository);
		walk.markStart(walk.parseCommit(repository.resolve(branch)));
		walk.sort( RevSort.REVERSE);

		RevCommit commit = walk.next();

		while(commit!=null) {
			log.debug("Commit ID " + commit.getName());
			RevCommit commitParent = commit.getParentCount() == 0 ? null : commit.getParent(0);

			Set<String> refactoredClasses = new HashSet<>();

			// if the class happened to be refactored, then, print its process metrics at that time
			if(todo.containsKey(commit.getName())) {
				try {
					db.openSession();
					refactoredClasses = collectProcessMetricsOfRefactoredCommit(commit);
					db.commit();
				} catch(Exception e) {
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
			} catch(Exception e) {
				db.close();
			}

			commit = walk.next();
		}
		walk.close();
	}

	private void updateAndPrintExamplesOfNonRefactoredClasses(RevCommit commit, Set<String> refactoredClasses) throws IOException {

		pmDatabase.updateNotRefactored(refactoredClasses);

		// if there are classes over the threshold, we output them as an examples of not refactored classes,
		// and we reset their counter.
		// note that we have a lot of failures here, as 500 commits later, the class might had been
		// renamed or moved, and thus the class (with the name before) "doesn't exist" anymore..
		// that is still ok as we are collecting thousands of examples.
		// TTV to mention: our sample never contains non refactored classes that were moved or renamed,
		// but that's not a big deal.
		for(ProcessMetric pm : pmDatabase.refactoredLongAgo()) {
			outputNonRefactoredClass(pm);

			// we then reset the counter, and start again.
			// it is ok to use the same class more than once, as metrics as well as
			// its source code will/may change, and thus, they are a different instance.
			pm.resetLastRefactoringStats(commit.getName());

		}

	}

	private void updateProcessMetrics(RevCommit commit, RevCommit commitParent) throws IOException {
		try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
			diffFormatter.setRepository(repository);
			diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
			diffFormatter.setDetectRenames(true);

			for (DiffEntry entry : diffFormatter.scan(commitParent, commit)) {
				String fileName = entry.getPath(null);

				boolean isAJavaFile = fileName.toLowerCase().endsWith("java");
				boolean refactoringIsInATestFile = RefactoringUtils.isTestFile(fileName);
				if (!isAJavaFile || refactoringIsInATestFile) {
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

				// add class to our in-memory pmDatabase (if it's not a test file)
				if(!pmDatabase.containsKey(fileName))
					pmDatabase.put(fileName, new ProcessMetric(fileName, commit.getName()));

				// collect number of lines deleted and added in that file
				int linesDeleted = 0;
				int linesAdded = 0;

				for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
					linesDeleted = edit.getEndA() - edit.getBeginA();
					linesAdded = edit.getEndB() - edit.getBeginB();
				}

				// update our pmDatabase entry with the information of the current commit
				ProcessMetric currentClazz = pmDatabase.get(fileName);
				currentClazz.existsIn(commit.getName(), commit.getFullMessage(), commit.getAuthorIdent().getName(), linesAdded, linesDeleted);
			}
		}
	}

	private Set<String> collectProcessMetricsOfRefactoredCommit(RevCommit commit) {

		Set<String> refactoredClasses = new HashSet<>();
		Set<Long> allYeses = todo.get(commit.getName());

		for (Long yesId : allYeses) {

			Yes yes = db.findYes(yesId);

			String fileName = yes.getFilePath();

			// we print the information BEFORE updating it with this commit, because we need the data from BEFORE this commit
			ProcessMetric currentProcessMetrics = pmDatabase.get(fileName);
			ProcessMetrics dbProcessMetrics = new ProcessMetrics(
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
			yes.setProcessMetrics(dbProcessMetrics);
			db.update(yes);

			// update counters
			currentProcessMetrics.increaseRefactoringCounter();
			currentProcessMetrics.resetLastRefactoringStats(commit.getName());

			refactoredClasses.add(fileName);
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
		String commitHashBackThen = clazz.getBaseCommitForNonRefactoring();
		String sourceCodeBackThen;

		log.info("Class " + clazz.getFileName() + " is an example of not refactored (original commit " + commitHashBackThen + ")");

		try {
			// we extract the source code from back then (as that's the one that never deserved a refactoring)
			sourceCodeBackThen = readFileFromGit(repository, commitHashBackThen, clazz.getFileName());
		} catch(Exception e) {
			log.error("Failed when getting source code of the class... The class was probably moved or deleted...");
			pmDatabase.remove(clazz);
			return;
		}

		try {
			saveFile(commitHashBackThen, sourceCodeBackThen, clazz.getFileName());
			List<No> nos = codeMetrics(commitHashBackThen, clazz);

			// print its process metrics in the same process metrics file
			// note that we print the process metrics back then (X commits ago)
			storeProcessMetric(clazz.getFileName(), nos);
		} catch(Exception e) {
			log.error("Failing when calculating metrics", e);
		}

	}

	private List<No> codeMetrics(String commitHashBackThen, ProcessMetric clazz) {

		List<No> nos = new ArrayList<>();

		new CK().calculate(tempDir, ck -> {

			if(ck.isError()) {
				log.error("CK failed: " + ck.getClassName());
				throw new RuntimeException("CK failed: " + ck.getFile());
			}

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
					ck.getFile().replace(tempDir, ""),
					ck.getClassName(),
					classMetric,
					null,
					null,
					null);

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
						ck.getFile().replace(tempDir, ""),
						ck.getClassName(),
						classMetric,
						methodMetrics,
						null,
						null);

				nos.add(noM);

				for (Map.Entry<String, Integer> entry : ckMethodResult.getVariablesUsage().entrySet()) {
					VariableMetric variableMetric = new VariableMetric(entry.getKey(), entry.getValue());

					No noV = new No(
							project,
							commitHashBackThen,
							ck.getFile().replace(tempDir, ""),
							ck.getClassName(),
							classMetric,
							methodMetrics,
							variableMetric,
							null);

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
						ck.getFile().replace(tempDir, ""),
						ck.getClassName(),
						classMetric,
						null,
						null,
						fieldMetrics);

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
		cleanTmpDir();
		new File(tempDir + FilePathUtils.dirsOnly(fileName)).mkdirs();
		ps = new PrintStream(tempDir + fileName);
		ps.print(sourceCodeBackThen);
		ps.close();
	}

	private void cleanTmpDir () throws IOException {
		FileUtils.deleteDirectory(new File(tempDir));
		tempDir = FilePathUtils.lastSlashDir(com.google.common.io.Files.createTempDir().getAbsolutePath());
	}

}
