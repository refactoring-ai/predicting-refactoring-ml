package refactoringml;

import org.hibernate.SessionFactory;
import refactoringml.db.CommitMetaData;
import refactoringml.db.Database;
import refactoringml.db.HibernateConfig;
import javax.persistence.*;
import java.util.List;
import java.util.stream.Collectors;

public class PMTrackerDatabase {
	@Transient
	private List<Integer> commitThresholds;

	@Transient
	private final String databasePath =	"jdbc:mysql://localhost/refactoringtest";
	@Transient
	private final String databaseUsername = "root";
	@Transient
	private final String databasePassword = "root";
	@Transient
	public Database db;

	public

	public PMTrackerDatabase(List<Integer> commitThresholds) {
		SessionFactory sf = new HibernateConfig().getSessionFactory(databasePath, databaseUsername, databasePassword);
		db = new Database(sf);

		this.commitThresholds = commitThresholds;
	}

	//Empty the database and close the hibernate SessionFactory
	public void destroy(){
		db.drop("ProcessMetricTracker");
		db.close();
	}

	//public interaction
	//Retrieve the process metrics tracker for the given fileName
	public ProcessMetricTracker find(String fileName) {
		return db.find(fileName, ProcessMetricTracker.class);
	}

	//Find all stable instances in the database
	public List<ProcessMetricTracker> findStableInstances() {
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

		ProcessMetricTracker oldPMTracker = db.find(oldFileName, ProcessMetricTracker.class);
		if(oldPMTracker == null)
			throw new IllegalStateException("A class file with this name is not known to the PMTrackerDatabase: " + oldFileName);

		ProcessMetricTracker pmTracker = new ProcessMetricTracker(oldPMTracker);
		pmTracker.setFileName(newFileName);

		removeFile(oldFileName);
		db.persist(pmTracker);

		return oldPMTracker;
	}

	//Remove the given fileName from the process metrics database
	//Returns the old process metrics tracker of the deleted class file, if any existed in the database
	public ProcessMetricTracker removeFile(String fileName){
		return db.remove(fileName, ProcessMetricTracker.class);
	}

	//Report a commit changing the given class file, the in memory database is updated accordingly
	public void reportChanges(String fileName, CommitMetaData commitMetaData, String authorName, int linesAdded, int linesDeleted) {
		ProcessMetricTracker pmTracker = db.find(fileName, ProcessMetricTracker.class);
		boolean exists = pmTracker != null;
		if(!exists)
			pmTracker = new ProcessMetricTracker(fileName, commitMetaData);

		pmTracker.reportCommit(commitMetaData.getCommitId(), commitMetaData.getCommitMessage(), authorName, linesAdded, linesDeleted);
		db.put(pmTracker, exists);
	}

	//Reset the tracker with latest refactoring and its commit meta data
	//the commitCounter will be zero again
	public void reportRefactoring(String fileName, CommitMetaData commitMetaData) {
		ProcessMetricTracker pmTracker = db.find(fileName, ProcessMetricTracker.class);
		boolean exists = pmTracker != null;
		if(!exists)
			pmTracker = new ProcessMetricTracker(fileName, commitMetaData);
		pmTracker.resetCounter(commitMetaData);

		db.put(pmTracker, exists);
	}

	//TODO: adapt this to the new SQL database
	@Deprecated
	public String toString(){
		return "PMDatabase{" +
				"database=" + db.mapToString() + ",\n" +
				"commitThreshold=" + commitThresholds.toString() +
				"}";
	}
}