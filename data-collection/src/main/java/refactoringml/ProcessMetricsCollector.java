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

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static refactoringml.JGitUtils.readFileFromGit;

public class ProcessMetricsCollector {

	private final PrintStream processMetricsOutput;
	private String tempDir;

	// commit hash, file name
	Map<String, Set<String>> todo;
	private String datasetName;
	private String gitUrl;
	private String projectName;
	private Repository repository;
	private PrintStream classOutputFile;
	private PrintStream methodOutputFile;
	private PrintStream variableOutputFile;
	private PrintStream fieldOutputFile;
	private String fileStoragePath;

	private PMDatabase database;

	private static final Logger log = Logger.getLogger(ProcessMetricsCollector.class);
	private String branch;

	public ProcessMetricsCollector(String datasetName, String gitUrl, String projectName, Repository repository, String branch, int commitThreshold, PrintStream processMetricsOutput,
	                               PrintStream classOutputFile,PrintStream methodOutputFile,PrintStream variableOutputFile,PrintStream fieldOutputFile,
	                               String fileStoragePath) {
		this.datasetName = datasetName;
		this.gitUrl = gitUrl;
		this.projectName = projectName;
		this.repository = repository;
		this.branch = branch;
		this.processMetricsOutput = processMetricsOutput;
		this.fileStoragePath = FilePathUtils.lastSlashDir(fileStoragePath);

		todo = new HashMap<>();
		database = new PMDatabase(commitThreshold);

		this.tempDir = FilePathUtils.lastSlashDir(Files.createTempDir().getAbsolutePath());

		this.classOutputFile = classOutputFile;
		this.methodOutputFile = methodOutputFile;
		this.variableOutputFile = variableOutputFile;
		this.fieldOutputFile = fieldOutputFile;

		this.processMetricsOutput.println("dataset,gitUrl,project,commit,file,commits,linesAdded,linesDeleted,authors,minorAuthors,majorAuthors,authorOwnership,bugs,refactorings");
		this.classOutputFile.println("dataset,gitUrl,project,commit,path,class,type,cbo,wmc,rfc,lcom,totalMethods,staticMethods,publicMethods,privateMethods,protectedMethods,defaultMethods,abstractMethods,finalMethods,synchronizedMethods,totalFields,staticFields,publicFields,privateFields,protectedFields,defaultFields,finalFields,synchronizedFields,nosi,loc,returnQty,loopQty,comparisonsQty,tryCatchQty,parenthesizedExpsQty,stringLiteralsQty,numbersQty,assignmentsQty,mathOperationsQty,variablesQty,maxNestedBlocks,anonymousClassesQty,subClassesQty,lambdasQty,uniqueWordsQty");
		this.methodOutputFile.println("dataset,gitUrl,project,commit,path,class,method,simplemethodname,line,cbo,wmc,rfc,loc,returns,variables,parameters,startLine,loopQty,comparisonsQty,tryCatchQty,parenthesizedExpsQty,stringLiteralsQty,numbersQty,assignmentsQty,mathOperationsQty,maxNestedBlocks,anonymousClassesQty,subClassesQty,lambdasQty,uniqueWordsQty");
		this.variableOutputFile.println("dataset,gitUrl,project,commit,path,class,method,simplemethodname,variable,qty");
		this.fieldOutputFile.println("dataset,gitUrl,project,commit,path,class,method,simplemethodname,variable,qty");
	}

	public void addToList (RevCommit commitData, String fileName) {
		String id = commitData.getName();
		if(!todo.containsKey(id))
			todo.put(id, new HashSet<>());

		todo.get(id).add(fileName);
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

			try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
				diffFormatter.setRepository(repository);
				diffFormatter.setDiffComparator(RawTextComparator.DEFAULT);
				diffFormatter.setDetectRenames(true);

				for (DiffEntry entry : diffFormatter.scan(commitParent, commit)) {
					String fileName = entry.getPath(null);

					if (!fileName.toLowerCase().endsWith("java")) {
						continue;
					}

					// if the class was either removed or deleted, we remove it from our database, as to not mess
					// with the refactoring counter...
					// this is a TTV as we can't correctly trace all renames and etc. But this doesn't affect the overall result,
					// as this is basically exceptional when compared to thousands of commits and changes.
					if(entry.getChangeType() == DiffEntry.ChangeType.DELETE || entry.getChangeType() == DiffEntry.ChangeType.RENAME) {
						database.remove(entry.getOldPath());

						if(entry.getChangeType() == DiffEntry.ChangeType.DELETE)
							continue;
					}

					// add class to our in-memory database
					if(!database.containsKey(fileName))
						database.put(fileName, new ProcessMetric(fileName, commit.getName()));

					// collect number of lines deleted and added in that file
					int linesDeleted = 0;
					int linesAdded = 0;

					for (Edit edit : diffFormatter.toFileHeader(entry).toEditList()) {
						linesDeleted = edit.getEndA() - edit.getBeginA();
						linesAdded = edit.getEndB() - edit.getBeginB();
					}

					// if the class happened to be refactored, then, print its process metrics at that time
					ProcessMetric currentClazz = database.get(fileName);
					if(refactoring(commit.getName(), fileName)) {

						log.info("Printing process metrics for the refactored class " + fileName + " in commit " + commit.getName());

						// we print the information BEFORE updating it with this commit, because we need the data from BEFORE this commit
						printProcessMetric(commit.getName(), fileName, true);

						currentClazz.increaseRefactoringCounter();
						currentClazz.resetRefactoringCounter(commit.getName());
						refactoredClasses.add(fileName);
					}

					// update our database entry with the information of the current commit
					currentClazz.existsIn(commit.getName(), commit.getFullMessage(), commit.getAuthorIdent().getName(), linesAdded, linesDeleted);
				}
			}

			// update classes that were not refactored on this commit
			database.updateNotRefactored(refactoredClasses);

			// if there are classes over the threshold, we output them as an examples of not refactored classes,
			// and we reset their counter.
			// note that we have a lot of failures here, as 500 commits later, the class might had been
			// renamed or moved, and thus the class (with the name before) "doesn't exist" anymore..
			// that is still ok as we are collecting thousands of examples.
			// TTV to mention: our sample never contains non refactored classes that were moved or renamed,
			// but that's not a big deal.
			for(ProcessMetric pm : database.refactoredLongAgo()) {
				outputNonRefactoredClass(pm);

				// we then reset the counter, and start again.
				// it is ok to use the same class more than once, as metrics as well as
				// its source code will/may change, and thus, they are a different instance.
				pm.resetRefactoringCounter(commit.getName());

			}

			commit = walk.next();
		}
		walk.close();
	}

	private void printProcessMetric (String commit, String fileName, boolean recent) {

		if(recent){
			processMetricsOutput.println(
				datasetName + "," +
				gitUrl + "," +
				projectName + "," +
               commit + "," +
               fileName  + "," +
               database.get(fileName).qtyOfCommits() + "," +
               database.get(fileName).getLinesAdded() + "," +
               database.get(fileName).getLinesDeleted() + "," +
               database.get(fileName).qtyOfAuthors() + "," +
               database.get(fileName).qtyMinorAuthors() + "," +
               database.get(fileName).qtyMajorAuthors() + "," +
               database.get(fileName).authorOwnership() + "," +
               database.get(fileName).getBugFixCount() + "," +
               database.get(fileName).getRefactoringsInvolved()
			);
		} else {
			processMetricsOutput.println(
				datasetName + "," +
				gitUrl + "," +
				projectName + "," +
				commit + "," +
				fileName  + "," +
				database.get(fileName).getBaseCommits() + "," +
				database.get(fileName).getBaseLinesAdded() + "," +
				database.get(fileName).getBaseLinesDeleted() + "," +
				database.get(fileName).getBaseAuthors() + "," +
				database.get(fileName).getBaseMinorAuthors() + "," +
				database.get(fileName).getBaseMajorAuthors() + "," +
				database.get(fileName).getBaseAuthorOwnership() + "," +
				database.get(fileName).getBaseBugFixCount() + "," +
				database.get(fileName).getBaseRefactoringsInvolved()
			);
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
			database.remove(clazz);
			return;
		}

		try {
			saveFile(commitHashBackThen, sourceCodeBackThen, clazz.getFileName());
			codeMetrics(commitHashBackThen, clazz);

			// print its process metrics in the same process metrics file
			// note that we print the process metrics back then (X commits ago)
			printProcessMetric(commitHashBackThen, clazz.getFileName(), false);
		} catch(Exception e) {
			log.error("Failing when calculating metrics", e);
		}

	}

	private void codeMetrics(String commitHashBackThen, ProcessMetric clazz) {

		new CK().calculate(tempDir, number -> {

			if(number.isError()) {
				log.error("CK failed: " + number.getClassName());
				throw new RuntimeException("CK failed: " + number.getFile());
			}

			classOutputFile.println(
				datasetName + "," +
				gitUrl + "," +
				projectName + "," +
				commitHashBackThen + "," +
				clazz.getFileName() + "," +
				number.getClassName() + "," +
				number.getType() + "," +
				number.getCbo() + "," +
				number.getWmc() + "," +
				number.getRfc() + "," +
				number.getLcom() + "," +
				number.getNumberOfMethods() + "," +
				number.getNumberOfStaticMethods() + "," +
				number.getNumberOfPublicMethods() + "," +
				number.getNumberOfPrivateMethods() + "," +
				number.getNumberOfProtectedMethods() + "," +
				number.getNumberOfDefaultMethods() + "," +
				number.getNumberOfAbstractMethods() + "," +
				number.getNumberOfFinalMethods() + "," +
				number.getNumberOfSynchronizedMethods() + "," +
				number.getNumberOfFields() + "," +
				number.getNumberOfStaticFields() + "," +
				number.getNumberOfPublicFields() + "," +
				number.getNumberOfPrivateFields() + "," +
				number.getNumberOfProtectedFields() + "," +
				number.getNumberOfDefaultFields() + "," +
				number.getNumberOfFinalFields() + "," +
				number.getNumberOfSynchronizedFields() + "," +
				number.getNosi() + "," +
				number.getLoc() + "," +
				number.getReturnQty() + "," +
				number.getLoopQty() + "," +
				number.getComparisonsQty() + "," +
				number.getTryCatchQty() + "," +
				number.getParenthesizedExpsQty() + "," +
				number.getStringLiteralsQty() + "," +
				number.getNumbersQty() + "," +
				number.getAssignmentsQty() + "," +
				number.getMathOperationsQty() + "," +
				number.getVariablesQty() + "," +
				number.getMaxNestedBlocks() + "," +
				number.getAnonymousClassesQty() + "," +
				number.getSubClassesQty() + "," +
				number.getLambdasQty() + "," +
				number.getUniqueWordsQty());

			for(CKMethodResult method : number.getMethods()) {
				methodOutputFile.println(
					datasetName + "," +
					gitUrl + "," +
					projectName + "," +
					commitHashBackThen + "," +
					clazz.getFileName() + "," +
					number.getClassName() + "," +
					CSVUtils.escape(method.getMethodName()) + "," +
					RefactoringUtils.cleanMethodName(method.getMethodName()) + "," +
					method.getStartLine() + "," +
					method.getCbo() + "," +
					method.getWmc() + "," +
					method.getRfc() + "," +
					method.getLoc() + "," +
					method.getReturnQty() + "," +
					method.getVariablesQty() + "," +
					method.getParametersQty() + "," +
					method.getStartLine() + "," +
					method.getLoopQty() + "," +
					method.getComparisonsQty() + "," +
					method.getTryCatchQty() + "," +
					method.getParenthesizedExpsQty() + "," +
					method.getStringLiteralsQty() + "," +
					method.getNumbersQty() + "," +
					method.getAssignmentsQty() + "," +
					method.getMathOperationsQty() + "," +
					method.getMaxNestedBlocks() + "," +
					method.getAnonymousClassesQty() + "," +
					method.getSubClassesQty() + "," +
					method.getLambdasQty() + "," +
					method.getUniqueWordsQty()
				);

				for (Map.Entry<String, Integer> entry : method.getVariablesUsage().entrySet()) {
					variableOutputFile.println(
						datasetName + "," +
						gitUrl + "," +
						projectName + "," +
						commitHashBackThen + "," +
						clazz.getFileName() + "," +
						number.getClassName() + "," +
						CSVUtils.escape(method.getMethodName()) + "," +
						RefactoringUtils.cleanMethodName(method.getMethodName()) + "," +
						entry.getKey() + "," +
						entry.getValue());
				}

				for (Map.Entry<String, Integer> entry : method.getFieldUsage().entrySet()) {
					fieldOutputFile.println(
						datasetName + "," +
						gitUrl + "," +
						projectName + "," +
						commitHashBackThen + "," +
						clazz.getFileName() + "," +
						number.getClassName() + "," +
						CSVUtils.escape(method.getMethodName()) + "," +
						RefactoringUtils.cleanMethodName(method.getMethodName()) + "," +
						entry.getKey() + "," +
						entry.getValue());
				}
			}

			classOutputFile.flush();
			methodOutputFile.flush();
			variableOutputFile.flush();
			fieldOutputFile.flush();
		});

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

	private boolean refactoring (String commit, String fileName) {
		return todo.containsKey(commit) && todo.get(commit).contains(fileName);
	}
}
