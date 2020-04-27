package refactoringml;

import refactoringml.db.CommitMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PMDatabase {
	//Map class files onto their original process metrics.
	private Map<String, ProcessMetricTracker> database;

	public PMDatabase () {
		this.database = new HashMap<>();
	}

	//public interaction
	//Retrieve the process metrics tracker for the given fileName
	public ProcessMetricTracker find(String fileName) {
		return database.get(fileName);
	}

	//Find all stable instances in the database
	//Don't use these, because it is very inefficient
	@Deprecated
	public List<ProcessMetricTracker> findStableInstances(List<Integer> commitThresholds) {
		return database.values().stream()
				.filter(pmTracker -> pmTracker.calculateStability(commitThresholds))
				.collect(Collectors.toList());
	}

	/*
	Report the rename of a file in order to track its process metrics. Every rename is considered a refactoring.
	In case of (various renames), the names are replaced, e.g.
	1. Rename People.java to Person.java: Person -> People_ProcessMetrics
	2. Rename Person.java to Human.java: Human -> People_ProcessMetrics
	Sometimes renames or move source folder refactorings are not detected by Refactoring-Miner, then the metrics are increased manually here.
	 */
	public ProcessMetricTracker renameFile(String oldFileName, String newFileName, CommitMetaData commitMetaData){
		ProcessMetricTracker pmTracker = new ProcessMetricTracker(database.getOrDefault(oldFileName, new ProcessMetricTracker(newFileName, commitMetaData)));
		if(oldFileName.equals(newFileName)){
			throw new IllegalArgumentException("The old and new file name for a rename refactoring are both: " + oldFileName);
		}
		//Check if the new file name maps onto an already tracked file name and thus,
		if (database.get(newFileName) != null){
			throw new IllegalStateException("The java class " + oldFileName + " was renamed into " + newFileName + ", but this file is already tracked. Maybe we missed a class file deletion?");
		}

		pmTracker.setFileName(newFileName);
		ProcessMetricTracker oldPMTracker = removeFile(oldFileName);

		database.put(newFileName, pmTracker);
		return oldPMTracker;
	}

	//Remove the given fileName from the process metrics database
	//Returns the old process metrics tracker of the deleted class file, if any existed in the database
	public ProcessMetricTracker removeFile(String fileName){
		return database.remove(fileName);
	}

	//Report a commit changing the given class file, the in memory database is updated accordingly
	//Returns the ProcessMetricsTracker if it is stable
	public ProcessMetricTracker reportChanges(String fileName, CommitMetaData commitMetaData, String authorName, int linesAdded, int linesDeleted) {
		ProcessMetricTracker pmTracker = database.getOrDefault(fileName, new ProcessMetricTracker(fileName, commitMetaData));
		pmTracker.reportCommit(commitMetaData.getCommitMessage(), authorName, linesAdded, linesDeleted);

		database.put(fileName, pmTracker);
		return pmTracker;
	}

	//Reset the tracker with latest refactoring and its commit meta data
	//the commitCounter will be zero again
	public void reportRefactoring(String fileName, CommitMetaData commitMetaData) {
		ProcessMetricTracker pmTracker = database.getOrDefault(fileName, new ProcessMetricTracker(fileName, commitMetaData));
		pmTracker.resetCounter(commitMetaData);

		database.put(fileName, pmTracker);
	}

	public String toString(){
		return "PMDatabase{" +
				"database=" + database.toString() + "}";
	}
}