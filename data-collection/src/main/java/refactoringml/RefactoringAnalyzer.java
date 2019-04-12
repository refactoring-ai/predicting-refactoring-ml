package refactoringml;

import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKMethodResult;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.refactoringminer.api.Refactoring;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static refactoringml.FilePathUtils.*;
import static refactoringml.JGitUtils.readFileFromGit;
import static refactoringml.RefactoringUtils.*;


public class RefactoringAnalyzer {
	private String tempDir;
	private String datasetName;
	private String gitUrl;
	private String projectName;
	private Repository repository;
	private ProcessMetricsCollector processMetrics;
	private PrintStream refactoredOutputFile;
	private PrintStream classOutputFile;
	private PrintStream methodOutputFile;
	private PrintStream variableOutputFile;
	private PrintStream fieldOutputFile;
	private String fileStorageDir;

	// stores the latest <commit,file> processed
	// we may find more than one refactoring per file, and thus, we don't wanna print repeated metrics
	// e.g., if a file suffers 2 refactorings, we want the two entries there, but a single one for the code metrics
	private String lastCommit = "";
	private String lastFile = "";

	private static final Logger log = Logger.getLogger(RefactoringAnalyzer.class);

	public RefactoringAnalyzer (String datasetName, String gitUrl, String projectName, Repository repository, ProcessMetricsCollector processMetrics,
	                            PrintStream refactoredOutputFile, PrintStream classOutputFile,PrintStream methodOutputFile,PrintStream variableOutputFile,
	                            PrintStream fieldOutputFile,String fileStorageDir) {
		this.datasetName = datasetName;
		this.gitUrl = gitUrl;
		this.projectName = projectName;
		this.repository = repository;
		this.processMetrics = processMetrics;
		this.refactoredOutputFile = refactoredOutputFile;

		this.classOutputFile = classOutputFile;
		this.methodOutputFile = methodOutputFile;
		this.variableOutputFile = variableOutputFile;
		this.fieldOutputFile = fieldOutputFile;

		this.tempDir = "";
		this.fileStorageDir = lastSlashDir(fileStorageDir);

		this.refactoredOutputFile.println("dataset,gitUrl,project,refactorCommit,parentCommit,path,class,refactoring,method,variable");
		this.classOutputFile.println("dataset,gitUrl,project,refactorCommit,parentCommit,path,class,type,cbo,wmc,rfc,lcom,totalMethods,staticMethods,publicMethods,privateMethods,protectedMethods,defaultMethods,abstractMethods,finalMethods,synchronizedMethods,totalFields,staticFields,publicFields,privateFields,protectedFields,defaultFields,finalFields,synchronizedFields,nosi,loc,returnQty,loopQty,comparisonsQty,tryCatchQty,parenthesizedExpsQty,stringLiteralsQty,numbersQty,assignmentsQty,mathOperationsQty,variablesQty,maxNestedBlocks,anonymousClassesQty,subClassesQty,lambdasQty,uniqueWordsQty");
		this.methodOutputFile.println("dataset,gitUrl,project,refactorCommit,parentCommit,path,class,method,simplemethodname,line,cbo,wmc,rfc,loc,returns,variables,parameters,startLine,loopQty,comparisonsQty,tryCatchQty,parenthesizedExpsQty,stringLiteralsQty,numbersQty,assignmentsQty,mathOperationsQty,maxNestedBlocks,anonymousClassesQty,subClassesQty,lambdasQty,uniqueWordsQty");
		this.variableOutputFile.println("dataset,gitUrl,project,refactorCommit,parentCommit,path,class,method,simplemethodname,variable,qty");
		this.fieldOutputFile.println("dataset,gitUrl,project,refactorCommit,after,parentCommit,class,method,simplemethodname,variable,qty");

	}

	public void collectCommitData(RevCommit commit, Refactoring refactoring) {

		if(commit.getId().getName().equals("babb6c8e99746531174073ebd2bce291d18770f4")) {
			System.out.println("found it");
		}

		if (commit.getParentCount() == 0) {
			return ;
		}

		if(!studied(refactoring)) {
			return;
		}

		String refactoredClass = refactoring.getInvolvedClassesBeforeRefactoring().get(0);

		log.info("Process Commit [" + commit.getId().getName() + "] Refactoring: [" + refactoring.toString().trim() + "]");

		RevCommit commitParent = commit.getParent(0);

		try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
	        diffFormatter.setRepository(repository);
			diffFormatter.setDetectRenames(true);

			List<DiffEntry> entries = diffFormatter.scan(commitParent, commit);

			// we try to match either the old or the new name of the file.
			// this is to help us in catching renames or moves
			// we also ignore files that were actually removed... no need to use these refactorings (although returned by RefactoringMiner)
			Optional<DiffEntry> refactoredEntry = entries.stream()
					.filter(entry -> entry.getChangeType() != ChangeType.DELETE)
					.filter(entry -> {
						String oldFileName = entry.getOldPath();
						String newFileName = entry.getNewPath();
						return classFromFileName(oldFileName).equals(classFromFullName(refactoredClass)) ||
								classFromFileName(newFileName).equals(classFromFullName(refactoredClass));
					})
					.findFirst();

			// sometimes the class might not be found
			// 1) Probably a case of refactoring with multiple classes (e.g., rename package), and RefactoringMiner currently returns
			// a list of classes, and we do not treat it. TTV limitation: we do not predict these refactoring operations.
			// 2) The refactoring happened in an inner class. In these cases, we also do not detect. This is a TTV/limitation of the approach.
			// 3) It is a refactoring on a deleted file. We filtered them out (see refactoredEntry filters above).
			// 4) the name of the file doesn't match with the name of the class.
			if(!refactoredEntry.isPresent()) {
				log.error("RefactoringMiner finds a refactoring, but we can't find it in DiffEntry: a " + refactoring.getRefactoringType() + " at " + refactoredClass);
				return;
			}

			DiffEntry entry = refactoredEntry.get();
			diffFormatter.toFileHeader(entry);

			String oldFileName = entry.getOldPath();
			String currentFileName = entry.getNewPath();

            String fileBefore = readFileFromGit(repository, commitParent, oldFileName);
			String fileAfter = readFileFromGit(repository, commit.getName(), currentFileName);

			boolean sawThisFileBefore = lastCommit.equals(commit.getId().getName()) && lastFile.equals(currentFileName);
			if(!sawThisFileBefore) {
				// store the before and after versions for the deep learning training
				saveSourceCode(commit.getId().getName(), currentFileName, fileBefore, fileAfter);

				// save the current file in a temp dir to execute the CK tool
				cleanTmpDir();
				createAllDirs(tempDir, currentFileName);
				try (PrintStream out = new PrintStream(new FileOutputStream(tempDir + currentFileName))) {
					out.print(fileBefore);
				}

				// mark it as To Do for the process metrics tool
				processMetrics.addToList(commit, currentFileName);
			}

		    // generate metric for the refactored class
		    calculateCkMetrics(commit.getId().getName(), refactoring, commitParent.getId().getName(), sawThisFileBefore);

			// store the latest <commit, file> visited
			lastCommit = commit.getId().getName();
			lastFile = currentFileName;

		} catch(Exception e) {
			log.error("Failed when working on " + refactoring, e);
		}

    }

	private void saveSourceCode (String commit, String fileName, String fileBefore, String fileAfter) throws FileNotFoundException {

		createAllDirs(fileStorageDir + commit + "/before-refactoring/", fileName);
		createAllDirs(fileStorageDir + commit + "/after-refactoring/", fileName);

		PrintStream before = new PrintStream(fileStorageDir + commit + "/before-refactoring/" + fileName);
		before.print(fileBefore);
		before.close();

		PrintStream after = new PrintStream(fileStorageDir + commit + "/after-refactoring/" + fileName);
		after.print(fileAfter);
		after.close();
	}

	private void calculateCkMetrics(String commitId, Refactoring refactoring, String commitParent, boolean sawThisFileBefore) {
		new CK().calculate(tempDir, ck -> {

			if(ck.isError())
				throw new RuntimeException("CK failed: " + ck.getFile());

			String fullRefactoredMethod = isMethodLevelRefactoring(refactoring) || isVariableLevelRefactoring(refactoring) ?
					RefactoringUtils.fullMethodName(getRefactoredMethod(refactoring)) : "";

			String refactoredVariable = isVariableLevelRefactoring(refactoring) || isAttributeLevelRefactoring(refactoring) ?
					getRefactoredVariableOrAttribute(refactoring) : "";

			refactoredOutputFile.println(
				datasetName + "," +
				gitUrl + "," +
				projectName + "," +
				commitId + "," +
				commitParent + "," +
				ck.getFile().replace(tempDir, "") + "," +
				ck.getClassName() + "," +
				refactoring.getRefactoringType().getDisplayName() + "," +
				CSVUtils.escape(fullRefactoredMethod) + "," +
				refactoredVariable
			);

			if(!sawThisFileBefore) {
				classOutputFile.println(
					datasetName + "," +
					gitUrl + "," +
					projectName + "," +
					commitId + "," +
					commitParent + "," +
					ck.getFile().replace(tempDir, "") + "," +
					ck.getClassName() + "," +
					ck.getType() + "," +
					ck.getCbo() + "," +
					ck.getWmc() + "," +
					ck.getRfc() + "," +
					ck.getLcom() + "," +
					ck.getNumberOfMethods() + "," +
					ck.getNumberOfStaticMethods() + "," +
					ck.getNumberOfPublicMethods() + "," +
					ck.getNumberOfPrivateMethods() + "," +
					ck.getNumberOfProtectedMethods() + "," +
					ck.getNumberOfDefaultMethods() + "," +
					ck.getNumberOfAbstractMethods() + "," +
					ck.getNumberOfFinalMethods() + "," +
					ck.getNumberOfSynchronizedMethods() + "," +
					ck.getNumberOfFields() + "," +
					ck.getNumberOfStaticFields() + "," +
					ck.getNumberOfPublicFields() + "," +
					ck.getNumberOfPrivateFields() + "," +
					ck.getNumberOfProtectedFields() + "," +
					ck.getNumberOfDefaultFields() + "," +
					ck.getNumberOfFinalFields() + "," +
					ck.getNumberOfSynchronizedFields() + "," +
					ck.getNosi() + "," +
					ck.getLoc() + "," +
					ck.getReturnQty() + "," +
					ck.getLoopQty() + "," +
					ck.getComparisonsQty() + "," +
					ck.getTryCatchQty() + "," +
					ck.getParenthesizedExpsQty() + "," +
					ck.getStringLiteralsQty() + "," +
					ck.getNumbersQty() + "," +
					ck.getAssignmentsQty() + "," +
					ck.getMathOperationsQty() + "," +
					ck.getVariablesQty() + "," +
					ck.getMaxNestedBlocks() + "," +
					ck.getAnonymousClassesQty() + "," +
					ck.getSubClassesQty() + "," +
					ck.getLambdasQty() + "," +
					ck.getUniqueWordsQty());

				for (CKMethodResult method : ck.getMethods()) {
					methodOutputFile.println(
						datasetName + "," +
						gitUrl + "," +
						projectName + "," +
						commitId + "," +
						commitParent + "," +
						ck.getFile().replace(tempDir, "") + "," +
						ck.getClassName() + "," +
						CSVUtils.escape(CKUtils.simplifyFullName(method.getMethodName())) + "," +
						cleanMethodName(method.getMethodName()) + "," +
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
							commitId + "," +
							commitParent + "," +
							ck.getFile().replace(tempDir, "") + "," +
							ck.getClassName() + "," +
							CSVUtils.escape(CKUtils.simplifyFullName(method.getMethodName())) + "," +
							cleanMethodName(method.getMethodName()) + "," +
							entry.getKey() + "," +
							entry.getValue());
					}

					for (Map.Entry<String, Integer> entry : method.getFieldUsage().entrySet()) {
						fieldOutputFile.println(
							datasetName + "," +
							gitUrl + "," +
							projectName + "," +
							commitId + "," +
							commitParent + "," +
							ck.getFile().replace(tempDir, "") + "," +
							ck.getClassName() + "," +
							CSVUtils.escape(CKUtils.simplifyFullName(method.getMethodName())) + "," +
							cleanMethodName(method.getMethodName()) + "," +
							entry.getKey() + "," +
							entry.getValue());
					}
				}

			}

			refactoredOutputFile.flush();
			classOutputFile.flush();
			methodOutputFile.flush();
			variableOutputFile.flush();
			fieldOutputFile.flush();
		});
	}

	private void cleanTmpDir() throws IOException {
		FileUtils.deleteDirectory(new File(tempDir));
		tempDir = lastSlashDir(com.google.common.io.Files.createTempDir().getAbsolutePath());
	}


}
