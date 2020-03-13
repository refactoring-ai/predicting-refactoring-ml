package refactoringml;

import com.github.javaparser.utils.Log;
import com.github.mauricioaniche.ck.CKMethodResult;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.validator.CreditCardValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.refactoringminer.api.Refactoring;
import refactoringml.db.*;
import refactoringml.util.CKUtils;
import refactoringml.util.RefactoringUtils;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static refactoringml.util.CKUtils.*;
import static refactoringml.util.FilePathUtils.*;
import static refactoringml.util.FileUtils.*;
import static refactoringml.util.FileUtils.writeFile;
import static refactoringml.util.FileUtils.createTmpDir;
import static refactoringml.util.JGitUtils.readFileFromGit;
import static refactoringml.util.RefactoringUtils.*;

public class RefactoringAnalyzer {
	private String tempDir;
	private Project project;
	private Database db;
	private Repository repository;
	private boolean storeFullSourceCode;
	private String fileStorageDir;

	private static final Logger log = LogManager.getLogger(RefactoringAnalyzer.class);

	public RefactoringAnalyzer (Project project, Database db, Repository repository, String fileStorageDir, boolean storeFullSourceCode) {
		this.project = project;
		this.db = db;
		this.repository = repository;
		this.storeFullSourceCode = storeFullSourceCode;

		this.tempDir = null;
		this.fileStorageDir = lastSlashDir(fileStorageDir);
	}

	public List<RefactoringCommit> collectCommitData(RevCommit commit, CommitMetaData superCommitMetaData, List<Refactoring> refactoringsToProcess) throws IOException {
		List<RefactoringCommit> allRefactorings = new ArrayList<>();

		try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
			diffFormatter.setRepository(repository);
			diffFormatter.setDetectRenames(true);
			RevCommit commitParent = commit.getParent(0);
			List<DiffEntry> entries = diffFormatter.scan(commitParent, commit);

			//Iterate over all Refactorings found for this commit
			for (Refactoring refactoring : refactoringsToProcess) {
				String refactoringSummary = refactoring.toString().trim();
				log.debug("Process Commit [" + commit.getId().getName() + "] with Refactoring: [" + refactoringSummary + "]");

				//loop over all refactored classes, multiple classes can be refactored by the same refactoring, e.g. Extract Interface Refactoring
				for (ImmutablePair<String, String> pair : refactoring.getInvolvedClassesBeforeRefactoring()) {
					String refactoredClassFile = pair.getLeft();
					String refactoredClassName = pair.getRight();

					//filter the diff entries for class files affected by this refactoring
					Optional<DiffEntry> refactoredEntry = entries.stream()
							.filter(entry -> {
								String oldFile = enforceUnixPaths(entry.getOldPath());
								String newFile = enforceUnixPaths(entry.getNewPath());
								return refactoredClassFile.equals(oldFile) ||
										refactoredClassFile.equals(newFile);
							})
							.findFirst();

					// this should not happen...
					if (refactoredEntry.isEmpty()) {
						log.error("Old classes in DiffEntry: " + entries.stream().map(x -> enforceUnixPaths(x.getOldPath())).collect(Collectors.toList()) + " but " + refactoredClassFile + " is not there.");
						log.error("New classes in DiffEntry: " + entries.stream().map(x -> enforceUnixPaths(x.getNewPath())).collect(Collectors.toList()) + " but " + refactoredClassFile + " is not there.");
						throw new RuntimeException("RefactoringMiner finds a refactoring for class '" + refactoredClassName + "', but we can't find it in DiffEntry: '" + refactoring.getRefactoringType() + "'. Check RefactoringAnalyzer.java for reasons why this can happen.");
					}

					// we found the file, let's get its metrics!
					DiffEntry entry = refactoredEntry.get();
					diffFormatter.toFileHeader(entry);

					String oldFileName = enforceUnixPaths(entry.getOldPath());
					String currentFileName = enforceUnixPaths(entry.getNewPath());

					if(nonClassFile(oldFileName)){
						log.error("Refactoring miner found a refactoring for a newly introduced class file on commit: " + commit.getName() + " for new class file: " + currentFileName);
						continue;
					}

					// Now, we get the contents of the file before
					String sourceCodeBefore = readFileFromGit(repository, commitParent, oldFileName);
					// save the old version of the file in a temp dir to execute the CK tool
					// Note: in older versions of the tool, we used to use the 'new name' for the file name. It does not make a lot of difference,
					// but later we notice it might do in cases of file renames and refactorings in the same commit.
					tempDir = createTmpDir();
					writeFile(tempDir + "/" + oldFileName, sourceCodeBefore);

					RefactoringCommit refactoringCommit = calculateCkMetrics(refactoredClassName, superCommitMetaData, refactoring, refactoringSummary);
					cleanTempDir(tempDir);

					if (refactoringCommit != null) {
						// mark it for the process metrics collection
						allRefactorings.add(refactoringCommit);

						if (storeFullSourceCode) {
							// let's get the source code of the file after the refactoring
							// but only if not deleted
							String sourceCodeAfter = !nonClassFile(currentFileName) ? readFileFromGit(repository, commit, oldFileName) : "";

							// store the before and after versions for the deep learning training
							// note that we save the file before with the same name of the current file name,
							// as to help in finding it (from the SQL query to the file)
							saveSourceCode(commit.getId().getName(), oldFileName, sourceCodeBefore, currentFileName, sourceCodeAfter, refactoringCommit);
						}
					} else {
						log.debug("RefactoringCommit instance was not created for the class: " + refactoredClassName + " and the refactoring type: " + refactoring.getName()  + " on commit " + commit.getName());
					}
				}
			}
		} catch (Exception e){
			log.error("Failed to collect commit meta data for refactored commit: "+ commit.getId(), e);
		}

		return allRefactorings;
    }

	private String getMethodAndOrVariableNameIfAny(RefactoringCommit refactoringCommit) {
		if(refactoringCommit.getLevel() == Level.METHOD.ordinal()) {
			return refactoringCommit.getMethodMetrics().getShortMethodName();
		}
		if(refactoringCommit.getLevel() == Level.VARIABLE.ordinal()) {
			return refactoringCommit.getMethodMetrics().getShortMethodName() + "-" + refactoringCommit.getVariableMetrics().getVariableName();
		}
		if(refactoringCommit.getLevel() == Level.ATTRIBUTE.ordinal()) {
			return refactoringCommit.getFieldMetrics().getFieldName();
		}

		// this is no method, variable, or attribute refactoring
		return "";
	}

	private void saveSourceCode(String commit, String fileNameBefore, String sourceCodeBefore, String fileNameAfter, String sourceCodeAfter, RefactoringCommit refactoringCommit) throws FileNotFoundException {
		String completeFileNameBefore = createFileName(fileNameBefore, refactoringCommit);
		writeFile(fileStorageDir + commit + "/before-refactoring/" + completeFileNameBefore, sourceCodeBefore);

		if(!sourceCodeAfter.isEmpty()) {
			String completeFileNameAfter = createFileName(fileNameAfter, refactoringCommit);
			writeFile(fileStorageDir + commit + "/after-refactoring/" + completeFileNameAfter, sourceCodeAfter);
		}
	}

	private String createFileName(String fileName, RefactoringCommit refactoringCommit){
		return String.format("%s-%d-%s-%d-%s",
				fileName,
				refactoringCommit.getLevel(),
				refactoringCommit.getRefactoring(),
				(refactoringCommit.getLevel() == Level.METHOD.ordinal()
						|| refactoringCommit.getLevel() == Level.VARIABLE.ordinal() ? refactoringCommit.getMethodMetrics().getStartLine() : 0),
				getMethodAndOrVariableNameIfAny(refactoringCommit));
	}

	private RefactoringCommit calculateCkMetrics(String refactoredClass, CommitMetaData commitMetaData, Refactoring refactoring, String refactoringSummary) {
		final List<RefactoringCommit> refactorings = new ArrayList<>();
		CKUtils.calculate(tempDir, commitMetaData.getCommitId(), project.getGitUrl(), ck -> {
			String cleanedCkClassName = cleanClassName(ck.getClassName());

			//Ignore all subclass callbacks from CK, that are not relevant in this case
			if(!cleanedCkClassName.equals(refactoredClass)){
				log.debug("CK found a subclass callback for class " + refactoredClass + "and ignores it on commit " + commitMetaData.getCommitId());
				return;
			}
			// collect the class level metrics
			ClassMetric classMetric = extractClassMetrics(ck);
			MethodMetric methodMetrics = null;
			VariableMetric variableMetrics = null;

			// if it's a method or a variable-level refactoring, collect the data
			if(isMethodLevelRefactoring(refactoring) || isVariableLevelRefactoring(refactoring)) {
				String fullRefactoredMethod = CKUtils.simplifyFullName(RefactoringUtils.fullMethodName(getRefactoredMethod(refactoring)));

				Optional<CKMethodResult> ckMethod = ck.getMethods().stream().filter(x -> CKUtils.simplifyFullName(x.getMethodName().toLowerCase()).equals(fullRefactoredMethod.toLowerCase()))
						.findFirst();

				if(!ckMethod.isPresent()) {
					// for some reason we did not find the method, let's remove it from the refactorings.
					String methods = ck.getMethods().stream().map(x -> CKUtils.simplifyFullName(x.getMethodName())).reduce("", (a, b) -> a + ", " + b);
					log.error("CK did not find the refactored method: " + fullRefactoredMethod + " for the refactoring type: " + refactoring.getName() + " on commit " + commitMetaData.getCommitId() +
							" on class " + refactoredClass +
							"\nAll methods found by CK: " + methods);
					return;
				} else {
					CKMethodResult ckMethodResult = ckMethod.get();
					methodMetrics = extractMethodMetrics(ckMethodResult);

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
			RefactoringCommit refactoringCommit = new RefactoringCommit(
					project,
					commitMetaData,
					enforceUnixPaths(ck.getFile()).replace(tempDir, ""),
					cleanedCkClassName,
					refactoring.getRefactoringType().getDisplayName(),
					refactoringTypeInNumber(refactoring),
					refactoringSummary,
					classMetric,
					methodMetrics,
					variableMetrics,
					fieldMetrics);
			refactorings.add(refactoringCommit);
		});

		for (RefactoringCommit refactoringCommit : refactorings){
			db.persist(refactoringCommit);
		}

		return refactorings.isEmpty()? null : refactorings.get(0);
	}
}