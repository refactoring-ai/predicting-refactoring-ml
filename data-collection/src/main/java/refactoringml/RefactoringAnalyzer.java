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
import refactoringml.astconverter.ASTConverter;
import refactoringml.db.*;
import refactoringml.util.CKUtils;
import refactoringml.util.RefactoringUtils;
import refactoringml.util.SourceCodeUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static refactoringml.util.FilePathUtils.*;
import static refactoringml.util.JGitUtils.readFileFromGit;
import static refactoringml.util.RefactoringUtils.*;


public class RefactoringAnalyzer {
	private String tempDir;
	private Project project;
	private Database db;
	private Repository repository;
	private ProcessMetricsCollector processMetrics;
	private String fileStorageDir;

	private static final Logger log = Logger.getLogger(RefactoringAnalyzer.class);

	public RefactoringAnalyzer (Project project, Database db, Repository repository, ProcessMetricsCollector processMetrics, String fileStorageDir) {
		this.project = project;
		this.db = db;
		this.repository = repository;
		this.processMetrics = processMetrics;

		this.tempDir = "";
		this.fileStorageDir = lastSlashDir(fileStorageDir);

	}

	public void collectCommitData(RevCommit commit, Refactoring refactoring) throws IOException {

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
				throw new RuntimeException("RefactoringMiner finds a refactoring, but we can't find it in DiffEntry: a " + refactoring.getRefactoringType() + " at " + refactoredClass);
			}

			DiffEntry entry = refactoredEntry.get();
			diffFormatter.toFileHeader(entry);

			String oldFileName = entry.getOldPath();
			String currentFileName = entry.getNewPath();

			boolean refactoringIsInATestFile = isTestFile(oldFileName) || isTestFile(currentFileName);
			if(refactoringIsInATestFile)
				return;

			String fileBefore = SourceCodeUtils.removeComments(readFileFromGit(repository, commitParent, oldFileName));
			String fileAfter = SourceCodeUtils.removeComments(readFileFromGit(repository, commit.getName(), currentFileName));

			// save the current file in a temp dir to execute the CK tool
			cleanTmpDir();
			createAllDirs(tempDir, currentFileName);
			try (PrintStream out = new PrintStream(new FileOutputStream(tempDir + currentFileName))) {
				out.print(fileBefore);
			}

			// generate metric for the refactored class
			Yes yes = calculateCkMetrics(commit.getId().getName(), refactoring, commitParent.getId().getName());

			if(yes!=null) {
				// mark it as To Do for the process metrics tool
				processMetrics.addToList(commit, yes);

				// store the before and after versions for the deep learning training
				// note that we save the file before with the same name of the current file name,
				// as to help in finding it (from the SQL query to the file)
				saveSourceCode(commit.getId().getName(), fileBefore, currentFileName, fileAfter, yes);

				// save also a cleaned version of the source code (using astc)
				// this is to facilitate the deep learning process
				cleanSourceCode(commit.getId().getName(), fileBefore, currentFileName, fileAfter, yes);
			}

		}

    }

	private void cleanSourceCode(String commit, String fileBefore, String fileNameAfter, String fileAfter, Yes yes) throws FileNotFoundException {

		// Run ast converter 1
		createAllDirs(fileStorageDir + commit + "/before-refactoring/", fileNameAfter);
		createAllDirs(fileStorageDir + commit + "/after-refactoring/", fileNameAfter);

		String completeFileNameAstC1 = String.format("%s-%d-%s-%d-%s-astc1",
				fileNameAfter,
				yes.getRefactoringType(),
				yes.getRefactoring(),
				(yes.getRefactoringType() == 2 ? yes.getMethodMetrics().getStartLine() : 0),
				getRefactoredElementNameIfAny(yes)
				);

		PrintStream before1 = new PrintStream(fileStorageDir + commit + "/before-refactoring/" + completeFileNameAstC1);
		before1.print(ASTConverter.converter(fileBefore, 1));
		before1.close();

		// Run ast converter 2
		String completeFileNameAstC2 = String.format("%s-%d-%s-%d-%s-astc2",
				fileNameAfter,
				yes.getRefactoringType(),
				yes.getRefactoring(),
				(yes.getRefactoringType() == 2 ? yes.getMethodMetrics().getStartLine() : 0),
				getRefactoredElementNameIfAny(yes));

		PrintStream before2 = new PrintStream(fileStorageDir + commit + "/before-refactoring/" + completeFileNameAstC2);
		before2.print(ASTConverter.converter(fileBefore, 2));
		before2.close();
	}

	private String getRefactoredElementNameIfAny(Yes yes) {
		if(yes.getRefactoringType() == TYPE_METHOD_LEVEL) {
			return yes.getMethodMetrics().getShortMethodName();
		}
		if(yes.getRefactoringType() == TYPE_VARIABLE_LEVEL) {
			return yes.getVariableMetrics().getVariableName();
		}
		if(yes.getRefactoringType() == TYPE_ATTRIBUTE_LEVEL) {
			return yes.getFieldMetrics().getFieldName();
		}

		// this is no method, variable, or attribute refactoring
		return "";
	}

	private void saveSourceCode(String commit, String fileBefore, String fileNameAfter, String fileAfter, Yes yes) throws FileNotFoundException {

		createAllDirs(fileStorageDir + commit + "/before-refactoring/", fileNameAfter);
		createAllDirs(fileStorageDir + commit + "/after-refactoring/", fileNameAfter);

		String completeFileName = String.format("%s-%d-%s-%d-%s",
						fileNameAfter,
						yes.getRefactoringType(),
						yes.getRefactoring(),
						(yes.getRefactoringType() == 2 ? yes.getMethodMetrics().getStartLine() : 0),
						getRefactoredElementNameIfAny(yes));

		PrintStream before = new PrintStream(fileStorageDir + commit + "/before-refactoring/" + completeFileName);
		before.print(fileBefore);
		before.close();

		PrintStream after = new PrintStream(fileStorageDir + commit + "/after-refactoring/" + completeFileName);
		after.print(fileAfter);
		after.close();
	}

	private Yes calculateCkMetrics(String refactorCommit, Refactoring refactoring, String parentCommit) {
		final List<Yes> list = new ArrayList<>();
		new CK().calculate(tempDir, ck -> {

			if(ck.isError())
				throw new RuntimeException("CK failed: " + ck.getFile());

			// collect the class level metrics
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


			MethodMetric methodMetrics = null;
			VariableMetric variableMetrics = null;

			// if it's a method or a variable-level refactoring, collect the data
			if(isMethodLevelRefactoring(refactoring) || isVariableLevelRefactoring(refactoring)) {
				String fullRefactoredMethod = RefactoringUtils.fullMethodName(getRefactoredMethod(refactoring));

				Optional<CKMethodResult> ckMethod = ck.getMethods().stream().filter(x -> CKUtils.simplifyFullName(x.getMethodName()).equals(fullRefactoredMethod))
						.findFirst();

				if(!ckMethod.isPresent()) {
					// for some reason we did not find the method, let's remove it from the list.
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
					parentCommit,
					ck.getFile().replace(tempDir, ""),
					ck.getClassName(),
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
		FileUtils.deleteDirectory(new File(tempDir));
		tempDir = lastSlashDir(com.google.common.io.Files.createTempDir().getAbsolutePath());
	}


}
