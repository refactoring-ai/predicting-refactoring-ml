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
import refactoringml.db.*;
import refactoringml.util.CKUtils;
import refactoringml.util.FilePathUtils;
import refactoringml.util.RefactoringUtils;
import refactoringml.util.SourceCodeUtils;

import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static refactoringml.util.CKUtils.*;
import static refactoringml.util.FilePathUtils.*;
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

	private static final Logger log = Logger.getLogger(RefactoringAnalyzer.class);

	public RefactoringAnalyzer (Project project, Database db, Repository repository, String fileStorageDir, boolean storeFullSourceCode) {
		this.project = project;
		this.db = db;
		this.repository = repository;
		this.storeFullSourceCode = storeFullSourceCode;

		this.tempDir = null;
		this.fileStorageDir = lastSlashDir(fileStorageDir);
	}

	public Set<Long> collectCommitData(RevCommit commit, Refactoring refactoring) throws IOException {
		String refactoringSummary = refactoring.toString().trim();
		//log.debug("Process Commit [" + commit.getId().getName() + "] Refactoring: [" + refactoringSummary + "]");

		RevCommit commitParent = commit.getParent(0);
		Set<Long> allRefactorings = new HashSet<Long>();

		for (ImmutablePair<String, String> pair : refactoring.getInvolvedClassesBeforeRefactoring()) {
			String refactoredClassFile = pair.getLeft();
			String refactoredClassName = pair.getRight();

			try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
				diffFormatter.setRepository(repository);
				diffFormatter.setDetectRenames(true);

				//TODO: move this diff entry part outside the for loop, in order to improve the performance
				List<DiffEntry> entries = diffFormatter.scan(commitParent, commit);
				Optional<DiffEntry> refactoredEntry = entries.stream()
						.filter(entry -> {
							String oldFile = enforceUnixPaths(entry.getOldPath());
							String newFile = enforceUnixPaths(entry.getNewPath());
							return refactoredClassFile.equals(oldFile) ||
									refactoredClassFile.equals(newFile);
						})
						.findFirst();

				// this should not happen...
				if(!refactoredEntry.isPresent()) {
					log.error("Old classes in DiffEntry: " + entries.stream().map(x -> enforceUnixPaths(x.getOldPath())).collect(Collectors.toList()));
					log.error("New classes in DiffEntry: " + entries.stream().map(x -> enforceUnixPaths(x.getNewPath())).collect(Collectors.toList()));
					throw new RuntimeException("RefactoringMiner finds a refactoring for class '" + refactoredClassName + "', but we can't find it in DiffEntry: '" + refactoring.getRefactoringType() + "'. Check RefactoringAnalyzer.java for reasons why this can happen.");
				}

				// we found the file, let's get its metrics!
				DiffEntry entry = refactoredEntry.get();
				diffFormatter.toFileHeader(entry);

				String oldFileName = enforceUnixPaths(entry.getOldPath());
				String currentFileName = enforceUnixPaths(entry.getNewPath());

				if(TrackDebugMode.ACTIVE && (oldFileName.contains(TrackDebugMode.FILENAME_TO_TRACK) || currentFileName.contains(TrackDebugMode.FILENAME_TO_TRACK))) {
					log.debug("[TRACK] Refactoring '" + refactoring.getName() +"' detected, commit " + commit.getId().getName());
				}

				// Now, we get the contents of the file before
				String sourceCodeBefore = SourceCodeUtils.removeComments(readFileFromGit(repository, commitParent, oldFileName));

				// save the old version of the file in a temp dir to execute the CK tool
				// Note: in older versions of the tool, we used to use the 'new name' for the file name. It does not make a lot of difference,
				// but later we notice it might do in cases of file renames and refactorings in the same commit.
				tempDir = createTmpDir();
				createAllDirs(tempDir, oldFileName);
				try (PrintStream out = new PrintStream(new FileOutputStream(tempDir + oldFileName))) {
					out.print(sourceCodeBefore);
				}

				RefactoringCommit refactoringCommit = calculateCkMetrics(refactoredClassName, new CommitMetaData(commit, project), refactoring, refactoringSummary);

				if(refactoringCommit !=null) {
					// mark it for the process metrics collection
					allRefactorings.add(refactoringCommit.getId());

					if(storeFullSourceCode) {
						// let's get the source code of the file after the refactoring
						// but only if not deleted
						String sourceCodeAfter = !wasDeleted(currentFileName) ? SourceCodeUtils.removeComments(readFileFromGit(repository, commit.getName(), currentFileName)) : "";

						// store the before and after versions for the deep learning training
						// note that we save the file before with the same name of the current file name,
						// as to help in finding it (from the SQL query to the file)
						saveSourceCode(commit.getId().getName(), oldFileName, sourceCodeBefore, currentFileName, sourceCodeAfter, refactoringCommit);
					}
				} else {
					//TODO: investigate this case to write a better log message
					log.error("RefactoringCommit instance was not created. CK did not find the class, maybe?");
				}

				cleanTmpDir();
			}//end if

		}

		if(commit.getId().getName().equals(TrackDebugMode.COMMIT_TO_TRACK)) {
			log.debug("[TRACK] End commit " + commit.getId().getName());
		}

		return allRefactorings;
    }

	private boolean wasDeleted(String fileName) {
		return fileName.equals("/dev/null");
	}

	private String getMethodAndOrVariableNameIfAny(RefactoringCommit refactoringCommit) {
		if(refactoringCommit.getLevel() == TYPE_METHOD_LEVEL) {
			return refactoringCommit.getMethodMetrics().getShortMethodName();
		}
		if(refactoringCommit.getLevel() == TYPE_VARIABLE_LEVEL) {
			return refactoringCommit.getMethodMetrics().getShortMethodName() + "-" + refactoringCommit.getVariableMetrics().getVariableName();
		}
		if(refactoringCommit.getLevel() == TYPE_ATTRIBUTE_LEVEL) {
			return refactoringCommit.getFieldMetrics().getFieldName();
		}

		// this is no method, variable, or attribute refactoring
		return "";
	}

	private void saveSourceCode(String commit, String fileNameBefore, String sourceCodeBefore, String fileNameAfter, String sourceCodeAfter, RefactoringCommit refactoringCommit) throws FileNotFoundException {

		createAllDirs(fileStorageDir + commit + "/before-refactoring/", fileNameBefore);

		String completeFileNameBefore = String.format("%s-%d-%s-%d-%s",
				fileNameBefore,
				refactoringCommit.getLevel(),
				refactoringCommit.getRefactoring(),
				(refactoringCommit.getLevel() == TYPE_METHOD_LEVEL
						|| refactoringCommit.getLevel() == TYPE_VARIABLE_LEVEL ? refactoringCommit.getMethodMetrics().getStartLine() : 0),
				getMethodAndOrVariableNameIfAny(refactoringCommit));

		PrintStream before = new PrintStream(fileStorageDir + commit + "/before-refactoring/" + completeFileNameBefore);
		before.print(sourceCodeBefore);
		before.close();

		if(!sourceCodeAfter.isEmpty()) {
			createAllDirs(fileStorageDir + commit + "/after-refactoring/", fileNameAfter);

			String completeFileNameAfter = String.format("%s-%d-%s-%d-%s",
					fileNameAfter,
					refactoringCommit.getLevel(),
					refactoringCommit.getRefactoring(),
					(refactoringCommit.getLevel() == TYPE_METHOD_LEVEL
							|| refactoringCommit.getLevel() == TYPE_VARIABLE_LEVEL ? refactoringCommit.getMethodMetrics().getStartLine() : 0),
					getMethodAndOrVariableNameIfAny(refactoringCommit));

			PrintStream after = new PrintStream(fileStorageDir + commit + "/after-refactoring/" + completeFileNameAfter);
			after.print(sourceCodeAfter);
			after.close();
		}
	}

	private RefactoringCommit calculateCkMetrics(String refactoredClass, CommitMetaData commitMetaData, Refactoring refactoring, String refactoringSummary) {
		final List<RefactoringCommit> list = new ArrayList<>();
		new CK().calculate(tempDir, ck -> {
			String cleanedCkClassName = cleanClassName(ck.getClassName());

			//Ignore all subclass callbacks from CK, that are not relevant in this case
			if(!cleanedCkClassName.equals(refactoredClass))
				return;

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
					// for some reason we did not find the method, let's remove it from the list.
					String methods = ck.getMethods().stream().map(x -> CKUtils.simplifyFullName(x.getMethodName())).reduce("", (a, b) -> a + ", " + b);
					log.error("CK did not find the refactored method: " + fullRefactoredMethod + "\n" +
							"All methods found by CK: " + methods);
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
			list.add(refactoringCommit);

			db.persist(refactoringCommit);

		});

		return list.isEmpty() ? null : list.get(0);
	}

	//TODO: on my Windows computer the tempDir is not always deleted
	private void cleanTmpDir() throws IOException {
		if(tempDir != null) {
			FileUtils.deleteDirectory(new File(tempDir));
			tempDir = null;
		}
	}
}