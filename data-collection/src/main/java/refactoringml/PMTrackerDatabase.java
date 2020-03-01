package refactoringml;

import refactoringml.db.CommitMetaData;
import refactoringml.db.Database;
import java.util.List;
import java.util.stream.Collectors;

public class PMTrackerDatabase {
	public Database db;

	public PMTrackerDatabase(Database db) {
		this.db = db;
	}

	//Empty the database and close the hibernate SessionFactory
	public void empty(){
		db.truncate("ProcessMetricTracker");
	}

	//public interaction
	//Retrieve the process metrics tracker for the given fileName
	public ProcessMetricTracker find(String fileName) {
		return db.find(fileName, ProcessMetricTracker.class);
	}

	//Find all stable instances in the database
	public List<ProcessMetricTracker> findStableInstances(List<Integer> commitThresholds) {
		return db.getAll("ProcessMetricTracker", ProcessMetricTracker.class).stream()
				.filter(pmTracker -> pmTracker.calculateStability(commitThresholds))
				.collect(Collectors.toList());
	}

	/*
	Report the rename of a file in order to track its process metrics.
	In case of (various renames), the names are replaced, e.g.
	1. Rename People.java to Person.java: Person -> People_ProcessMetrics
	2. Rename Person.java to Human.java: Human -> People_ProcessMetrics
	 */
	public ProcessMetricTracker renameFile(String oldFileName, String newFileName){
		if(oldFileName.equals(newFileName))
			throw new IllegalStateException("A class file cannot be renamed to the same name: " + oldFileName);

		ProcessMetricTracker pmTracker = db.find(oldFileName, ProcessMetricTracker.class);
		if(pmTracker == null)
			throw new IllegalStateException("A class file with this name is not known to the PMTrackerDatabase: " + oldFileName);

		pmTracker.setFileName(newFileName);
		db.update(pmTracker);

		return pmTracker;
	}

	//Remove the given fileName from the process metrics database
	//Returns the old process metrics tracker of the deleted class file, if any existed in the database
	public ProcessMetricTracker removeFile(String fileName){
		ProcessMetricTracker result = db.remove(fileName, ProcessMetricTracker.class);
		//Commit the transaction can lead to data inconsistencies in case the db is rolled back, but otherwise this might lead to ConstraintViolationExceptions
		db.commit();
		db.openSession();

		return result;
	}

	//Report a commit changing the given class file, the in memory database is updated accordingly
	public void reportChanges(String fileName, CommitMetaData commitMetaData, String authorName, int linesAdded, int linesDeleted) {
		ProcessMetricTracker pmTracker = db.find(fileName, ProcessMetricTracker.class);

		if(pmTracker == null) {
			pmTracker = new ProcessMetricTracker(fileName, commitMetaData);
			pmTracker.reportCommit(commitMetaData.getCommitId(), commitMetaData.getCommitMessage(), authorName, linesAdded, linesDeleted);
			db.persistAndCommit(pmTracker);
			db.openSession();
		} else{
			pmTracker.reportCommit(commitMetaData.getCommitId(), commitMetaData.getCommitMessage(), authorName, linesAdded, linesDeleted);
			db.update(pmTracker);
		}
	}

	//Reset the tracker with latest refactoring and its commit meta data
	//the commitCounter will be zero again
	public void reportRefactoring(String fileName, CommitMetaData commitMetaData) {
		ProcessMetricTracker pmTracker = db.find(fileName, ProcessMetricTracker.class);

		if(pmTracker == null) {
			pmTracker = new ProcessMetricTracker(fileName, commitMetaData);
			pmTracker.resetCounter(commitMetaData);
			db.persistAndCommit(pmTracker);
			db.openSession();
		} else{
			pmTracker.resetCounter(commitMetaData);
			db.update(pmTracker);
		}
	}

	//TODO: adapt this to the new SQL database
	@Deprecated
	public String toString(){
		return "PMDatabase{" +
				"database=" + db.mapToString() + ",\n" +
				"}";
	}
}