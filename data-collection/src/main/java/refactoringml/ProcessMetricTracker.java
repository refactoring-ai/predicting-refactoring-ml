package refactoringml;

import refactoringml.db.CommitMetaData;
import refactoringml.db.ProcessMetrics;

import javax.persistence.*;
import java.util.*;

@Entity
public class ProcessMetricTracker {
	//filename of the class file, does not distinguish between subclasses
	@Id
	@GeneratedValue
	private int id;

	private String fileName;

	//Either: the last commit refactoring the class file or the first one creating the class file
	@ManyToOne(cascade=CascadeType.ALL)
	private CommitMetaData baseCommitMetaData;
	//Reference commit to be considered stable, if it passes a certain threshold
	@OneToOne(cascade=CascadeType.ALL)
	private ProcessMetrics baseProcessMetrics;
	//The process metrics till the latest commit affecting the class file, use this for refactorings
	@OneToOne(cascade=CascadeType.ALL)
	private ProcessMetrics currentProcessMetrics;

	//current highest commit stability threshold, this class file passed, used to avoid double instances when we use multiple thresholds
	private int currentCommitThreshold = 0;

	//TODO: move to utils
	public static String[] bugKeywords = {"bug", "error", "mistake", "fault", "wrong", "fail", "fix"};
	private static boolean isBugFix(String commitMsg) {
		String cleanCommitMsg = commitMsg.toLowerCase();
		return Arrays.stream(bugKeywords).filter(keyword -> cleanCommitMsg.contains(keyword))
				.count() > 0;
	}

	@Deprecated // hibernate purposes
	public ProcessMetricTracker(){}

	public ProcessMetricTracker(String fileName, CommitMetaData commitMetaData) {
		this.fileName = fileName;
		this.baseCommitMetaData = commitMetaData;
		this.baseProcessMetrics = new ProcessMetrics(0, 0, 0, 0, 0);
		this.currentProcessMetrics =  new ProcessMetrics(0, 0, 0, 0, 0);
	}

	//Deep copy the ProcessMetrics in order to have a new object after renames
	public ProcessMetricTracker(ProcessMetricTracker oldPMTracker) {
		this.fileName = oldPMTracker.getFileName();
		this.baseCommitMetaData = oldPMTracker.getBaseCommitMetaData();
		this.baseProcessMetrics = new ProcessMetrics(oldPMTracker.getBaseProcessMetrics());
		this.currentProcessMetrics = new ProcessMetrics(oldPMTracker.getCurrentProcessMetrics());
	}

	//public tracker interaction
	public void reportCommit(String commitId, String commitMsg, String authorName, int linesAdded, int linesDeleted) {
		currentProcessMetrics.qtyOfCommits++;
		currentProcessMetrics.updateAuthorCommits(authorName);

		currentProcessMetrics.linesAdded += linesAdded;
		currentProcessMetrics.linesDeleted += linesDeleted;

		if(isBugFix(commitMsg))
			currentProcessMetrics.bugFixCount++;
	}

	//Reset the tracker with latest refactoring and its commit meta data
	//the commitCounter will be zero again
	public void resetCounter(CommitMetaData commitMetaData) {
		currentProcessMetrics.refactoringsInvolved ++;
		currentCommitThreshold = 0;

		this.baseCommitMetaData = commitMetaData;
		this.baseProcessMetrics = new ProcessMetrics(currentProcessMetrics);
	}

	//current filename of the class file, does not distinguish between subclasses
	public String getFileName() { return fileName; }

	public void setFileName(String newFileName) {fileName = newFileName;}

	//Number of commits affecting this class since the last refactoring
	//Used to estimate if the class is stable
	public int getCommitCounter() { return currentProcessMetrics.qtyOfCommits - baseProcessMetrics.qtyOfCommits; }

	public CommitMetaData getBaseCommitMetaData() { return baseCommitMetaData; }

	public ProcessMetrics getBaseProcessMetrics() { return baseProcessMetrics; }

	public ProcessMetrics getCurrentProcessMetrics() { return currentProcessMetrics; }

	public int getCommitCountThreshold() { return currentCommitThreshold; }

	//Filter class files that were not refactored in the last K commits and not already found with a lower K.
	//TODO: If a class has an inner class only one instance is stored in the database
	// The fix (>= instead of > : currentCommitThreshold) leads to stable commit duplicates in the DB in case of multiple refactorings in the current commit
	public boolean calculateStability(List<Integer> commitThresholds){
		for (Integer threshold : commitThresholds){
			//1. Test if the class file was not refactored for the last K commits
			if(isStable(threshold) &&
					//2. Avoid duplicates:
					//2.1: a class file fulfills various K's -> only use the highest K and ignore the lower ones
					//2.2: the commit counter is higher than at least one K, but below another K -> consider it not stable
					(threshold > currentCommitThreshold || threshold == getCommitCounter())){
				currentCommitThreshold = threshold;
				return true;
			}
		}
		return false;
	}

	//Was this class file not refactored in the last K commits affecting this class file?
	public boolean isStable(int commitThreshold){ return getCommitCounter() >= commitThreshold; }

	@Override
	public String toString() {
		return "ProcessMetricTracker{" +
				"fileName='" + fileName + '\'' +
				", commitCounter=" + getCommitCounter() +
				", baseCommitMetaData=" + baseCommitMetaData.toString() +
				", baseProcessMetrics=" + baseProcessMetrics.toString() +
				", currentProcessMetrics=" + currentProcessMetrics.toString() +
				'}';
	}
}