package refactoringml;

import com.github.mauricioaniche.ck.CKMethodResult;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.Edit;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import refactoringml.db.*;
import refactoringml.util.*;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import static refactoringml.util.CKUtils.cleanCkClassName;
import static refactoringml.util.FilePathUtils.enforceUnixPaths;
import static refactoringml.util.CKUtils.*;
import static refactoringml.util.FileUtils.*;
import static refactoringml.util.JGitUtils.getDiffFormater;
import static refactoringml.util.JGitUtils.readFileFromGit;
import static refactoringml.util.LogUtils.createErrorState;
import static refactoringml.util.RefactoringUtils.*;

public class ProcessMetricsCollector {
	private Project project;
	private Database db;
	private Repository repository;
	private String fileStoragePath;
	private PMDatabase pmDatabase;

	private static final Logger log = LogManager.getLogger(ProcessMetricsCollector.class);

	public ProcessMetricsCollector(Project project, Database db, Repository repository, PMDatabase pmDatabase, String fileStoragePath) {
		this.project = project;
		this.db = db;
		this.repository = repository;
		this.fileStoragePath = FilePathUtils.lastSlashDir(fileStoragePath);
		this.pmDatabase = pmDatabase;
	}

	//if this commit contained a refactoring, then collect its process metrics for all affected class files,
	//otherwise only update the file process metrics
	public void collectMetrics(RevCommit commit, CommitMetaData superCommitMetaData, List<RefactoringCommit> allRefactoringCommits, List<DiffEntry> entries, Set<ImmutablePair<String, String>> refactoringRenames, Set<ImmutablePair<String, String>> jGitRenames) throws IOException {
		collectProcessMetricsOfRefactoredCommit(superCommitMetaData, allRefactoringCommits);

		processRenames(refactoringRenames, jGitRenames, superCommitMetaData);

		// we go now change by change in the commit to update the process metrics there
		// Also if a stable instance is found it is stored with the metrics in the DB
		collectProcessMetricsOfStableCommits(commit, superCommitMetaData, entries);
	}

	//Collect the ProcessMetrics of the RefactoringCommit before this commit happened and update the database entry with it
	private void collectProcessMetricsOfRefactoredCommit(CommitMetaData superCommitMetaData, List<RefactoringCommit> allRefactoringCommits) {
		for (RefactoringCommit refactoringCommit : allRefactoringCommits) {
			String fileName = refactoringCommit.getFilePath();
			ProcessMetricTracker currentProcessMetricsTracker = pmDatabase.find(fileName);

			ProcessMetrics dbProcessMetrics  = currentProcessMetricsTracker != null ?
					new ProcessMetrics(currentProcessMetricsTracker.getCurrentProcessMetrics()) :
					new ProcessMetrics(0, 0, 0, 0, 0);

			refactoringCommit.setProcessMetrics(dbProcessMetrics);
			db.update(refactoringCommit);

			pmDatabase.reportRefactoring(fileName, superCommitMetaData);
		}
	}

	//update the process metrics for all renames that were missed by RefactoringMiner
	private void processRenames(Set<ImmutablePair<String, String>> refactoringRenames, Set<ImmutablePair<String, String>> jGitRenames, CommitMetaData superCommitMetadata) {
		//get all renames detected by RefactoringMiner
		if(refactoringRenames != null){
			for(ImmutablePair<String, String> rename : refactoringRenames){
				//check if the class file name was changed, not only the class name
				if (!rename.left.equals(rename.right)){
					pmDatabase.renameFile(rename.left, rename.right, superCommitMetadata);
					log.debug("Renamed " + rename.left + " to " + rename.right + " in PMDatabase.");
				}
			}
		}

		//process the renames missed by refactoringminer
		if(jGitRenames != null){
			//get all renames missed by RefactoringMiner
			if(refactoringRenames != null)
				jGitRenames.removeAll(refactoringRenames);
			if(jGitRenames.size() > 0){
				log.debug("Refactoringminer missed these refactorings: " + jGitRenames + LogUtils.createErrorState(superCommitMetadata.getCommitId(), project));
				//update the missed renames in the PM database
				for(ImmutablePair<String, String> rename : jGitRenames){
					log.debug("Renamed " + rename.left + " to " + rename.right + " in PMDatabase.");
					pmDatabase.renameFile(rename.left, rename.right, superCommitMetadata);
					pmDatabase.reportRefactoring(rename.right, superCommitMetadata);
				}
			}
		}
	}

	//Update the process metrics of all affected class files:
	//Reset the PMTracker for all class files, that were refactored on this commit
	//Increase the PMTracker for all class files, that were not refactored but changed on this commit
	private void collectProcessMetricsOfStableCommits(RevCommit commit, CommitMetaData superCommitMetaData, List<DiffEntry> entries) throws IOException {
			for (DiffEntry entry : entries) {
				String fileName = enforceUnixPaths(entry.getNewPath());

				// do not collect these numbers if not a java file (save some memory)
				if (!refactoringml.util.FileUtils.IsJavaFile(fileName))
					continue;

				// if the class was deleted, we remove it from our pmDatabase
				// this is a TTV as we can't correctly trace all renames and etc. But this doesn't affect the overall result,
				// as this is basically exceptional when compared to thousands of commits and changes.
				if(entry.getChangeType() == DiffEntry.ChangeType.DELETE) {
					String oldFileName = enforceUnixPaths(entry.getOldPath());
					pmDatabase.removeFile(oldFileName);
					log.debug("Deleted " + oldFileName + " from PMDatabase.");
					continue;
				}

				// collect number of lines deleted and added in that file
				List<Edit> editList = getDiffFormater().toFileHeader(entry).toEditList();
				int linesDeleted = calculateLinesDeleted(editList);
				int linesAdded = calculateLinesAdded(editList);

				// we increase the counter here. This means a class will go to the 'non refactored' bucket
				// only after we see it X times (and not involved in a refactoring, otherwise, counters are resetted).
				ProcessMetricTracker pmTracker = pmDatabase.reportChanges(fileName, superCommitMetaData, commit.getAuthorIdent().getName(), linesAdded, linesDeleted);

				//The last commit passed the stability threshold for this class file
				if(pmTracker.calculateStability(project.getCommitCountThresholds())){
					outputNonRefactoredClass(pmTracker);

					// we then reset the counter, and start again.
					// it is ok to use the same class more than once, as metrics as well as
					// its source code will/may change, and thus, they are a different instance.
					if(pmTracker.getCommitCountThreshold() == project.getMaxCommitThreshold()){
						log.debug("Reset pmTracker for class " + pmTracker.getFileName() + " with threshold: " + pmTracker.getCommitCountThreshold() +
								" because it is the max threshold(" + project.getMaxCommitThreshold() + ").");
						pmTracker.resetCounter(new CommitMetaData(commit, project));
					}
				}
			}
	}

	//Store the refactoring instances in the DB
	private void outputNonRefactoredClass (ProcessMetricTracker pmTracker) throws IOException {
		String tempDir = null;
		try {
			String commitBackThen = pmTracker.getBaseCommitMetaData().getCommitId();
			log.debug("Class " + pmTracker.getFileName() + " is an example of a not refactored instance with the stable commit: " + commitBackThen);

			// we extract the source code from back then (as that's the one that never deserved a refactoring)
			String sourceCodeBackThen = readFileFromGit(repository, commitBackThen, pmTracker.getFileName());
			// create a temp dir to store the source code files and run CK there
			tempDir = createTmpDir();

			// we save it in the permanent storage...
			writeFile(fileStoragePath +  pmTracker.getFileName() + "/" + "not-refactored/" + pmTracker.getFileName(), sourceCodeBackThen);
			// ... as well as in the temp one, so that we can calculate the CK metrics
			writeFile(tempDir + pmTracker.getFileName(), sourceCodeBackThen);

			if(pmTracker.getBaseCommitMetaData().getId() == 0)
				db.persist(pmTracker.getBaseCommitMetaData());

			CommitMetaData commitMetaData = db.loadCommitMetaData(pmTracker.getBaseCommitMetaData().getId());
			List<StableCommit> stableCommits = codeMetrics(commitMetaData, tempDir, pmTracker.getCommitCountThreshold());

			// print its process metrics in the same process metrics file
			// note that we print the process metrics back then (X commits ago)
			for(StableCommit stableCommit : stableCommits) {
				stableCommit.setProcessMetrics(new ProcessMetrics(pmTracker.getBaseProcessMetrics()));
				db.persist(stableCommit);
			}
		} catch(Exception e) {
			log.error(e.getClass().getCanonicalName() + " while processing stable process metrics." + createErrorState(pmTracker.getBaseCommitMetaData().getCommitId(), project), e);
		} finally {
			cleanTempDir(tempDir);
		}
	}

	//TODO: Fix this, as it generates many duplicates
	private List<StableCommit> codeMetrics(CommitMetaData commitMetaData, String tempDir, int commitThreshold) {
		List<StableCommit> stableCommits = new ArrayList<>();

		CKUtils.calculate(tempDir, commitMetaData.getCommitId(), project.getGitUrl(), ck -> {
			String cleanedCkClassName = cleanCkClassName(ck.getClassName());
			ClassMetric classMetric = extractClassMetrics(ck);

			Set<CKMethodResult> methods = ck.getMethods();
			for(CKMethodResult ckMethodResult : methods) {
				MethodMetric methodMetrics = extractMethodMetrics(ckMethodResult);

				Set<Map.Entry<String, Integer>> variables = ckMethodResult.getVariablesUsage().entrySet();
				for (Map.Entry<String, Integer> entry : variables) {
					VariableMetric variableMetric = new VariableMetric(entry.getKey(), entry.getValue());

					StableCommit stableCommitV = new StableCommit(
							project,
							commitMetaData,
							enforceUnixPaths(ck.getFile()).replace(tempDir, ""),
							cleanedCkClassName,
							classMetric,
							methodMetrics,
							variableMetric,
							null,
							RefactoringUtils.Level.VARIABLE.ordinal(),
							commitThreshold);

					stableCommits.add(stableCommitV);
				}

				//only add this if there are no variable refactorings
				if(variables.isEmpty()){
					StableCommit stableCommitM = new StableCommit(
							project,
							commitMetaData,
							enforceUnixPaths(ck.getFile()).replace(tempDir, ""),
							cleanedCkClassName,
							classMetric,
							methodMetrics,
							null,
							null,
							RefactoringUtils.Level.METHOD.ordinal(),
							commitThreshold);

					stableCommits.add(stableCommitM);

				}
			}

			Set<String> fields = ck.getMethods().stream().flatMap(x -> x.getFieldUsage().keySet().stream()).collect(Collectors.toSet());
			for(String field : fields) {
				int totalAppearances = ck.getMethods().stream()
						.map(x -> x.getFieldUsage().get(field) == null ? 0 : x.getFieldUsage().get(field))
						.mapToInt(Integer::intValue).sum();

				FieldMetric fieldMetrics = new FieldMetric(field, totalAppearances);

				StableCommit stableCommitF = new StableCommit(
						project,
						commitMetaData,
						enforceUnixPaths(ck.getFile()).replace(tempDir, ""),
						cleanedCkClassName,
						classMetric,
						null,
						null,
						fieldMetrics,
						Level.ATTRIBUTE.ordinal(),
						commitThreshold);

				stableCommits.add(stableCommitF);
			}

			//only add this if there are not method- and field level refactorings
			if(methods.isEmpty() && fields.isEmpty()){
				StableCommit stableCommit = new StableCommit(
						project,
						commitMetaData,
						enforceUnixPaths(ck.getFile()).replace(tempDir, ""),
						cleanedCkClassName,
						classMetric,
						null,
						null,
						null,
						RefactoringUtils.Level.CLASS.ordinal(),
						commitThreshold);

				stableCommits.add(stableCommit);
			}
		});

		return stableCommits;
	}
}