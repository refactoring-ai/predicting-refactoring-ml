package refactoringml;

import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKMethodResult;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.log4j.Logger;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.refactoringminer.api.Refactoring;
import refactoringml.astconverter.ASTConverter;
import refactoringml.db.*;
import refactoringml.util.CKUtils;
import refactoringml.util.JGitUtils;
import refactoringml.util.RefactoringUtils;
import refactoringml.util.SourceCodeUtils;

import java.io.*;
import java.nio.InvalidMarkException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static refactoringml.util.CKUtils.cleanClassName;
import static refactoringml.util.FilePathUtils.*;
import static refactoringml.util.JGitUtils.readFileFromGit;
import static refactoringml.util.RefactoringUtils.*;


public class RefactoringAnalyzer {
	private String tempDir;
	private Project project;
	private Database db;
	private Repository repository;
	private ProcessMetricsCollector processMetrics;
	private boolean storeFullSourceCode;
	private String fileStorageDir;

	private static final Logger log = Logger.getLogger(RefactoringAnalyzer.class);

	public RefactoringAnalyzer (Project project, Database db, Repository repository, ProcessMetricsCollector processMetrics, String fileStorageDir, boolean storeFullSourceCode) {
		this.project = project;
		this.db = db;
		this.repository = repository;
		this.processMetrics = processMetrics;
		this.storeFullSourceCode = storeFullSourceCode;

		this.tempDir = null;
		this.fileStorageDir = lastSlashDir(fileStorageDir);
	}

	public void collectCommitData(RevCommit commit, Refactoring refactoring) throws IOException {

		if (commit.getParentCount() == 0 || !studied(refactoring)) {
			return ;
		}

		log.info("Process Commit [" + commit.getId().getName() + "] Refactoring: [" + refactoring.toString().trim() + "]");
		if(commit.getId().getName().equals(TrackDebugMode.COMMIT_TO_TRACK)) {
			log.info("[TRACK] Commit " + commit.getId().getName());
		}

		RevCommit commitParent = commit.getParent(0);

		for (ImmutablePair<String, String> pair : refactoring.getInvolvedClassesBeforeRefactoring()) {

			String refactoredClassFile = pair.getLeft();
			String refactoredClassName = pair.getRight();

			try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
				diffFormatter.setRepository(repository);
				diffFormatter.setDetectRenames(true);

				List<DiffEntry> entries = diffFormatter.scan(commitParent, commit);

				//TODO: Process metrics: Track renames #19
				// we try to match either the old or the new name of the file.
				// this is to help us in catching renames or moves
				Optional<DiffEntry> refactoredEntry = entries.stream()
						.filter(entry -> {
							String oldFileName = entry.getOldPath();
							String newFileName = entry.getNewPath();
							return refactoredClassFile.equals(oldFileName) ||
									refactoredClassFile.equals(newFileName);
						})
						.findFirst();

				// this should not happen...
				if(!refactoredEntry.isPresent()) {
					log.info("old classes in DiffEntry: " + entries.stream().map(x -> x.getOldPath()).collect(Collectors.toList()));
					log.info("new classes in DiffEntry: " + entries.stream().map(x -> x.getNewPath()).collect(Collectors.toList()));
					throw new RuntimeException("RefactoringMiner finds a refactoring for class '" + refactoredClassName + "', but we can't find it in DiffEntry: '" + refactoring.getRefactoringType() + "'. Check RefactoringAnalyzer.java for reasons why this can happen.");
				}

				// we found the file, let's get its metrics!
				DiffEntry entry = refactoredEntry.get();
				diffFormatter.toFileHeader(entry);

				String oldFileName = entry.getOldPath();
				String currentFileName = entry.getNewPath();

				if(TrackDebugMode.ACTIVE && (oldFileName.equals(TrackDebugMode.FILE_TO_TRACK) || currentFileName.equals(TrackDebugMode.FILE_TO_TRACK))) {
					log.info("[TRACK] Refactoring '" + refactoring.getName() +"' detected, commit " + commit.getId().getName());
				}

				// Now, we get the contents of the file before
				String sourceCodeBefore = SourceCodeUtils.removeComments(readFileFromGit(repository, commitParent, oldFileName));

				// save the old version of the file in a temp dir to execute the CK tool
				// Note: in older versions of the tool, we used to use the 'new name' for the file name. It does not make a lot of difference,
				// but later we notice it might do in cases of file renames and refactorings in the same commit.
				createTmpDir();
				createAllDirs(tempDir, oldFileName);
				try (PrintStream out = new PrintStream(new FileOutputStream(tempDir + oldFileName))) {
					out.print(sourceCodeBefore);
				}

				// generate metric for the refactored class
				Calendar commitTime = JGitUtils.getGregorianCalendar(commit);

				Yes yes = calculateCkMetrics(refactoredClassName, commit.getId().getName(), commitTime, refactoring, commitParent.getId().getName());

				if(yes!=null) {
					// mark it as To Do for the process metrics tool
					processMetrics.addToList(commit, yes);

					if(storeFullSourceCode) {
						// let's get the source code of the file after the refactoring
						// but only if not deleted
						String sourceCodeAfter = !wasDeleted(currentFileName) ? SourceCodeUtils.removeComments(readFileFromGit(repository, commit.getName(), currentFileName)) : "";

						// store the before and after versions for the deep learning training
						// note that we save the file before with the same name of the current file name,
						// as to help in finding it (from the SQL query to the file)
						saveSourceCode(commit.getId().getName(), oldFileName, sourceCodeBefore, currentFileName, sourceCodeAfter, yes);
					}
				} else {
					log.error("YES was not created. CK did not find the class, maybe?");

					if(TrackDebugMode.ACTIVE && (oldFileName.equals(TrackDebugMode.FILE_TO_TRACK) || currentFileName.equals(TrackDebugMode.FILE_TO_TRACK))) {
						log.info("[TRACK] YES instance not created!");
					}
				}

				cleanTmpDir();
			}//end if

		}

		if(commit.getId().getName().equals(TrackDebugMode.COMMIT_TO_TRACK)) {
			log.info("[TRACK] End commit " + commit.getId().getName());
		}

    }

	private boolean wasDeleted(String fileName) {
		return fileName.equals("/dev/null");
	}

	private String getMethodAndOrVariableNameIfAny(Yes yes) {
		if(yes.getRefactoringType() == TYPE_METHOD_LEVEL) {
			return yes.getMethodMetrics().getShortMethodName();
		}
		if(yes.getRefactoringType() == TYPE_VARIABLE_LEVEL) {
			return yes.getMethodMetrics().getShortMethodName() + "-" + yes.getVariableMetrics().getVariableName();
		}
		if(yes.getRefactoringType() == TYPE_ATTRIBUTE_LEVEL) {
			return yes.getFieldMetrics().getFieldName();
		}

		// this is no method, variable, or attribute refactoring
		return "";
	}

	private void saveSourceCode(String commit, String fileNameBefore, String sourceCodeBefore, String fileNameAfter, String sourceCodeAfter, Yes yes) throws FileNotFoundException {

		createAllDirs(fileStorageDir + commit + "/before-refactoring/", fileNameBefore);

		String completeFileNameBefore = String.format("%s-%d-%s-%d-%s",
				fileNameBefore,
				yes.getRefactoringType(),
				yes.getRefactoring(),
				(yes.getRefactoringType() == TYPE_METHOD_LEVEL || yes.getRefactoringType() == TYPE_VARIABLE_LEVEL ? yes.getMethodMetrics().getStartLine() : 0),
				getMethodAndOrVariableNameIfAny(yes));

		PrintStream before = new PrintStream(fileStorageDir + commit + "/before-refactoring/" + completeFileNameBefore);
		before.print(sourceCodeBefore);
		before.close();

		if(!sourceCodeAfter.isEmpty()) {
			createAllDirs(fileStorageDir + commit + "/after-refactoring/", fileNameAfter);

			String completeFileNameAfter = String.format("%s-%d-%s-%d-%s",
					fileNameAfter,
					yes.getRefactoringType(),
					yes.getRefactoring(),
					(yes.getRefactoringType() == TYPE_METHOD_LEVEL || yes.getRefactoringType() == TYPE_VARIABLE_LEVEL ? yes.getMethodMetrics().getStartLine() : 0),
					getMethodAndOrVariableNameIfAny(yes));

			PrintStream after = new PrintStream(fileStorageDir + commit + "/after-refactoring/" + completeFileNameAfter);
			after.print(sourceCodeAfter);
			after.close();
		}

	}

	private Yes calculateCkMetrics(String refactoredClass, String refactorCommit, Calendar refactoringDate, Refactoring refactoring, String parentCommit) {
		final List<Yes> list = new ArrayList<>();
		new CK().calculate(tempDir, ck -> {
			String cleanedCkClassName = cleanClassName(ck.getClassName());

			//Ignore all subclass callbacks from CK, that are not relevant in this case
			if(!cleanedCkClassName.equals(refactoredClass))
				return;

			boolean isSubclass = CKUtils.evaluateSubclass(ck.getType());

			// collect the class level metrics
			ClassMetric classMetric = new ClassMetric(
					isSubclass,
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


			MethodMetric methodMetrics = null;
			VariableMetric variableMetrics = null;

			// if it's a method or a variable-level refactoring, collect the data
			if(isMethodLevelRefactoring(refactoring) || isVariableLevelRefactoring(refactoring)) {
				String fullRefactoredMethod = CKUtils.simplifyFullName(RefactoringUtils.fullMethodName(getRefactoredMethod(refactoring)));

				Optional<CKMethodResult> ckMethod = ck.getMethods().stream().filter(x -> CKUtils.simplifyFullName(x.getMethodName().toLowerCase()).equals(fullRefactoredMethod.toLowerCase()))
						.findFirst();

				if(!ckMethod.isPresent()) {
					// for some reason we did not find the method, let's remove it from the list.
					log.error("CK did not find the refactored method: " + fullRefactoredMethod);

					String methods = ck.getMethods().stream().map(x -> CKUtils.simplifyFullName(x.getMethodName())).reduce("", (a, b) -> a + ", " + b);
					log.error("All methods in CK: " + methods);
					return;
				} else {

					CKMethodResult ckMethodResult = ckMethod.get();

					methodMetrics = new MethodMetric(
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

					if(isVariableLevelRefactoring(refactoring)) {
						String refactoredVariable = getRefactoredVariableOrAttribute(refactoring);

						Integer appearances = ckMethodResult.getVariablesUsage().get(refactoredVariable);
						if(appearances == null) {
							// if we couldn't find the variable, for any reason, give it a -1, so we can filter it
							// out later
							appearances = -1;
						}
						variableMetrics = new VariableMetric(refactoredVariable, appearances);
					}
				}

			}

			// finally, if it's a field refactoring, we then only have class + field
			FieldMetric fieldMetrics = null;
			if(isAttributeLevelRefactoring(refactoring)) {
				String refactoredField = getRefactoredVariableOrAttribute(refactoring);

				int totalAppearances = ck.getMethods().stream()
						.map(x -> x.getFieldUsage().get(refactoredField) == null ? 0 : x.getFieldUsage().get(refactoredField))
						.mapToInt(Integer::intValue).sum();

				fieldMetrics = new FieldMetric(refactoredField, totalAppearances);

			}

			// assemble the final object
			Yes yes = new Yes(
					project,
					refactorCommit,
					refactoringDate,
					parentCommit,
					ck.getFile().replace(tempDir, ""),
					cleanedCkClassName,
					refactoring.getRefactoringType().getDisplayName(),
					refactoringTypeInNumber(refactoring),
					classMetric,
					methodMetrics,
					variableMetrics,
					fieldMetrics);
			list.add(yes);

			db.persist(yes);

		});

		return list.isEmpty() ? null : list.get(0);
	}

	private void cleanTmpDir() throws IOException {
		if(tempDir != null) {
			FileUtils.deleteDirectory(new File(tempDir));
			tempDir = null;
		}
	}

	private void createTmpDir() {
		tempDir = lastSlashDir(com.google.common.io.Files.createTempDir().getAbsolutePath());
	}


}
