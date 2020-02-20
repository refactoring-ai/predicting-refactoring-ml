package refactoringml;

import refactoringml.db.CommitMetaData;
import refactoringml.db.ProcessMetrics;
import java.util.*;

public class ProcessMetricTracker {
	//filename of the class file, does not distinguish between subclasses
	private String fileName;
	//Either: the last commit refactoring the class file or the first one creating the class file
	private CommitMetaData baseCommitMetaData;
	//Reference commit to be considered stable, if it passes a certain threshold
	private ProcessMetrics baseProcessMetrics;
	//The process metrics till the latest commit affecting the class file, use this for refactorings
	private ProcessMetrics currentProcessMetrics;

	//TODO: move to utils
	public static String[] bugKeywords = {"bug", "error", "mistake", "fault", "wrong", "fail", "fix"};
	private static boolean isBugFix(String commitMsg) {
		String cleanCommitMsg = commitMsg.toLowerCase();
		return Arrays.stream(bugKeywords).filter(keyword -> cleanCommitMsg.contains(keyword))
				.count() > 0;
	}

	public ProcessMetricTracker(String fileName, CommitMetaData commitMetaData) {
		this.fileName = fileName;
		this.baseCommitMetaData = commitMetaData;
		this.baseProcessMetrics = new ProcessMetrics(0, 0, 0, 0, 0);
		this.currentProcessMetrics =  new ProcessMetrics(0, 0, 0, 0, 0);
	}

	//public tracker interaction
	public void reportCommit(String commitMsg, String authorName, int linesAdded, int linesDeleted) {
		currentProcessMetrics.qtyOfCommits++;

		if(!currentProcessMetrics.allAuthors.containsKey(authorName)) {
			currentProcessMetrics.allAuthors.put(authorName, 0);
		}
		currentProcessMetrics.allAuthors.put(authorName, currentProcessMetrics.allAuthors.get(authorName)+1);

		currentProcessMetrics.linesAdded += linesAdded;
		currentProcessMetrics.linesDeleted += linesDeleted;

		if(isBugFix(commitMsg))
			currentProcessMetrics.bugFixCount++;
	}

	//Reset the tracker with latest refactoring and its commit meta data
	//the commitCounter will be zero again
	public void resetCounter(CommitMetaData commitMetaData) {
		currentProcessMetrics.refactoringsInvolved ++;

		this.baseCommitMetaData = commitMetaData;
		this.baseProcessMetrics = new ProcessMetrics(currentProcessMetrics);
	}

	//filename of the class file, does not distinguish between subclasses
	public String getFileName () { return fileName; }

	//Number of commits affecting this class since the last refactoring
	//Used to estimate if the class is stable
	public int getCommitCounter() { return currentProcessMetrics.qtyOfCommits - baseProcessMetrics.qtyOfCommits; }

	public CommitMetaData getBaseCommitMetaData() { return baseCommitMetaData; }

	public ProcessMetrics getBaseProcessMetrics() { return baseProcessMetrics; }

	public ProcessMetrics getCurrentProcessMetrics() { return currentProcessMetrics; }

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