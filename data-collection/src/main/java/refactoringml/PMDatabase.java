package refactoringml;

import refactoringml.db.CommitMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PMDatabase {
	private Map<String, ProcessMetricTracker> database;
	private int commitThreshold;

	public PMDatabase (int commitThreshold) {
		this.commitThreshold = commitThreshold;
		this.database = new HashMap<>();
	}

	//public interaction
	//Retrieve the process metrics tracker for the given fileName
	public ProcessMetricTracker find(String fileName) {
		return database.get(fileName);
	}

	//Find all stable instances in the database
	public List<ProcessMetricTracker> findStableInstances() {
		return database.values().stream()
				.filter(p -> p.getCommitCounter() >= commitThreshold)
				.collect(Collectors.toList());
	}

	//Remove the given fileName in the process metrics database to the new one, to keep track of renames
	//Returns the process metrics tracker before the rename, if any existed in the database
	public ProcessMetricTracker renameFile(String oldFileName, String newFileName, CommitMetaData commitMetaData){
		ProcessMetricTracker pmTracker = database.getOrDefault(oldFileName, new ProcessMetricTracker(newFileName, commitMetaData));
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
	public void reportChanges(String fileName, CommitMetaData commitMetaData, String authorName, int linesAdded, int linesDeleted) {
		ProcessMetricTracker pmTracker = database.getOrDefault(fileName, new ProcessMetricTracker(fileName, commitMetaData));
		pmTracker.reportCommit(commitMetaData.getCommitMessage(), authorName, linesAdded, linesDeleted);

		database.put(fileName, pmTracker);
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
				"database=" + database.toString() + ",\n" +
				"commitThreshold=" + commitThreshold +
				"}";
	}
}