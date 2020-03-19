package refactoringml;

import com.github.mauricioaniche.ck.CKMethodResult;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.util.io.DisabledOutputStream;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;
import refactoringml.db.*;
import refactoringml.util.CKUtils;
import refactoringml.util.JGitUtils;
import refactoringml.util.RefactoringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static refactoringml.util.CKUtils.*;
import static refactoringml.util.FilePathUtils.*;
import static refactoringml.util.FileUtils.*;
import static refactoringml.util.JGitUtils.getMapWithOldAndNewFiles;
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

		try {

			// get the map between new path -> old path
			HashMap<String, String> filesMap = getMapWithOldAndNewFiles(repository, commit);

			// get the map between class names
			HashMap<String, String> classAliases = getClassAliases(refactoringsToProcess);

			//Iterate over all Refactorings found for this commit
			for (Refactoring refactoring : refactoringsToProcess) {
				String refactoringSummary = refactoring.toString().trim();
				log.debug("Process Commit [" + commit.getId().getName() + "] with Refactoring: [" + refactoringSummary + "]");

				//loop over all refactored classes, multiple classes can be refactored by the same refactoring, e.g. Extract Interface Refactoring
				for (ImmutablePair<String, String> pair : refactoredFilesAndClasses(refactoring, refactoring.getInvolvedClassesBeforeRefactoring())) {
					// get the name of the file before the refactoring
					// if the one returned by RMiner exists in the map, we use the one in the map instead
					String refactoredClassFile = pair.getLeft();
					if(filesMap.containsKey(refactoredClassFile))
						refactoredClassFile = filesMap.get(refactoredClassFile);

					String refactoredClassName = pair.getRight();

					// build the full RefactoringCommit object
					String refactoredClassNameAlias = classAliases.get(refactoredClassName);
					RefactoringCommit refactoringCommit = buildRefactoringCommitObject(superCommitMetaData, refactoring, refactoringSummary, refactoredClassName, refactoredClassNameAlias, refactoredClassFile);

					if (refactoringCommit != null) {
						// mark it for the process metrics collection
						allRefactorings.add(refactoringCommit);

						if(storeFullSourceCode)
							storeSourceCode(refactoringCommit.getId(), refactoring, commit);
					} else {
						log.debug("RefactoringCommit instance was not created for the class: " + refactoredClassName + " and the refactoring type: " + refactoring.getName()  + " on commit " + commit.getName());
					}
				}

			}

		} catch (Exception e){
			log.error("Failed to collect commit data for refactored commit: "+ superCommitMetaData.getCommitId(), e);
		}

		return allRefactorings;
    }

	/**
	 * Get a map that contains classes that were renamed in this commit.
	 *
	 * Note that the map is name after -> name before. This is due to the fact that
	 * RMiner sometimes returns, for other refactorings, "the name before" = "the name after renaming".
	 */
	private HashMap<String, String> getClassAliases(List<Refactoring> refactoringsToProcess) {
		HashMap<String, String> aliases = new HashMap<>();

		for (Refactoring rename : possibleClassRenames(refactoringsToProcess)) {

			String nameBefore = rename.getInvolvedClassesBeforeRefactoring().iterator().next().getRight();
			String nameAfter = rename.getInvolvedClassesAfterRefactoring().iterator().next().getRight();

			aliases.put(nameAfter, nameBefore);
		}

		return aliases;
	}

	protected RefactoringCommit buildRefactoringCommitObject(CommitMetaData superCommitMetaData, Refactoring refactoring, String refactoringSummary, String refactoredClassName, String refactoredClassNameAlias, String fileName) {
		String parentCommitId = superCommitMetaData.getParentCommitId();

		try {
			/**
			 * Now, we get the contents of the file in the previous version,
			 * which we use to extract the features.
			 */
			String sourceCodeInPreviousVersion = readFileFromGit(repository, parentCommitId, fileName);
			tempDir = createTmpDir();
			writeFile(tempDir + "/" + fileName, sourceCodeInPreviousVersion);

			RefactoringCommit refactoringCommit = calculateCkMetrics(refactoredClassName, refactoredClassNameAlias, superCommitMetaData, refactoring, refactoringSummary);
			cleanTempDir(tempDir);

			return refactoringCommit;
		} catch(IOException e) {
			/**
			 * We could not open the file in the previous commit. This often happens when
			 * RMiner finds a refactoring in a new file. That can happen in corner cases.
			 * See example in https://github.com/tsantalis/RefactoringMiner/issues/89
			 */
			log.error("Could not find (previous) version of " + fileName + " in parent commit " + parentCommitId + " (commit " + superCommitMetaData.getCommitId() + "), commit url:" + superCommitMetaData.getCommitUrl(), e);

			return null;
		}
	}

	private void storeSourceCode(long id, Refactoring refactoring, RevCommit currentCommit) throws IOException {

		RevCommit commitParent = currentCommit.getParent(0);

		// for the before refactoring, we get its source code in the previous commit
		for (ImmutablePair<String, String> pair : refactoring.getInvolvedClassesBeforeRefactoring()) {
			String fileName = pair.getLeft();

			try {
				String sourceCode = readFileFromGit(repository, commitParent, fileName);
				writeFile(fileStorageDir + id + "/before/" + fileNameOnly(fileName), sourceCode);
			} catch(Exception e) {
				log.error("Could not write raw source code for file before refactoring, id=" + id + ", file name=" + fileName + ", commit=" + commitParent.getId().getName(), e);
			}
		}

		// for the after refactoring, we get its source code in the current commit
		for (ImmutablePair<String, String> pair : refactoring.getInvolvedClassesAfterRefactoring()) {
			String fileName = pair.getLeft();

			try {
				String sourceCode = readFileFromGit(repository, currentCommit, fileName);
				writeFile(fileStorageDir + id + "/after/" + fileNameOnly(fileName), sourceCode);
			} catch(Exception e) {
				log.error("Could not write raw source code for file after refactoring, id=" + id + ", file name=" + fileName + ", commit=" + currentCommit.getId().getName(), e);
			}
		}
	}

	private RefactoringCommit calculateCkMetrics(String refactoredClass, String refactoredClassAlias, CommitMetaData commitMetaData, Refactoring refactoring, String refactoringSummary) {
		final List<RefactoringCommit> refactorings = new ArrayList<>();
		CKUtils.calculate(tempDir, commitMetaData.getCommitId(), project.getGitUrl(), ck -> {
			String cleanedCkClassName = cleanCkClassName(ck.getClassName());

			//Ignore all subclass callbacks from CK, that are not relevant in this case
			if(!cleanedCkClassName.equals(refactoredClass) && !cleanedCkClassName.equals(refactoredClassAlias)){
				return;
			}
			// collect the class level metrics
			ClassMetric classMetric = extractClassMetrics(ck);
			MethodMetric methodMetrics = null;
			VariableMetric variableMetrics = null;

			// if it's a method or a variable-level refactoring, collect the data
			if(isMethodLevelRefactoring(refactoring) || isVariableLevelRefactoring(refactoring)) {
				String fullRefactoredMethod = CKUtils.simplifyFullMethodName(RefactoringUtils.fullMethodName(getRefactoredMethod(refactoring)));

				Optional<CKMethodResult> ckMethod = ck.getMethods().stream().filter(x -> CKUtils.simplifyFullMethodName(x.getMethodName().toLowerCase()).equals(fullRefactoredMethod.toLowerCase()))
						.findFirst();

				if(!ckMethod.isPresent()) {
					// for some reason we did not find the method, let's remove it from the refactorings.
					String methods = ck.getMethods().stream().map(x -> CKUtils.simplifyFullMethodName(x.getMethodName())).reduce("", (a, b) -> a + ", " + b);
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

		if(refactorings.isEmpty()) {
			log.error("We did not find class " + refactoredClass + " in CK's output (" + commitMetaData + ")");
		} else {
			for (RefactoringCommit refactoringCommit : refactorings) {
				db.persist(refactoringCommit);
			}
		}

		return refactorings.isEmpty()? null : refactorings.get(0);
	}
}