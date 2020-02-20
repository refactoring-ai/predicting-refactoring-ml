package refactoringml;

import java.util.*;
import java.util.function.Predicate;

//TODO: Refactor this class, e.g. by combining it with ProcessMetrics?
//TODO: Rename this class, as it is easily confused with ProcessMetrics and the name does not describe its purpose well
public class ProcessMetric {
	private String fileName;

	// updated info about the class
	private int commits = 0;
	private Map<String, Integer> authors = new HashMap<String, Integer>();
	private int linesAdded = 0;
	private int linesDeleted = 0;
	private int bugFixCount = 0;
	private int refactoringsInvolved = 0;

	//number of commits affecting this class since the last refactoring
	//Used to estimate if a class is stable
	private int commitCounter = 0;

	// counters at the time of the base commit
	private String baseCommitForNonRefactoring;
	private int baseLinesAdded = 0;
	private int baseLinesDeleted = 0;
	private int baseBugFixCount = 0;
	private int baseRefactoringsInvolved = 0;
	private long baseMinorAuthors = 0;
	private long baseMajorAuthors = 0;
	private double baseAuthorOwnership = 0;
	private int baseAuthors = 0;
	private int baseCommits = 0;

	public static String[] bugKeywords = {"bug", "error", "mistake", "fault", "wrong", "fail", "fix"};
	private Calendar baseCommitDateForNonRefactoring;

	public ProcessMetric (String fileName, String baseCommitForNonRefactoring, Calendar baseCommitDateForNonRefactoring) {
		this.fileName = fileName;
		this.baseCommitForNonRefactoring = baseCommitForNonRefactoring;
		this.baseCommitDateForNonRefactoring = baseCommitDateForNonRefactoring;
	}

	public void existsIn (String commitMsg, String authorName, int linesAdded, int linesDeleted) {
		commits++;

		if(!authors.containsKey(authorName)) {
			authors.put(authorName, 0);
		}
		authors.put(authorName, authors.get(authorName)+1);

		this.linesAdded += linesAdded;
		this.linesDeleted += linesDeleted;

		if(isBugFix(commitMsg))
			bugFixCount++;
	}

	private boolean isBugFix(String commitMsg) {
		String cleanCommitMsg = commitMsg.toLowerCase();
		return Arrays.stream(bugKeywords).filter(keyword -> cleanCommitMsg.contains(keyword))
				.count() > 0;
	}

	public int qtyOfAuthors() {
		return authors.size();
	}

	public void resetCounter(String commitHash, String baseCommitMessageForNonRefactoring, Calendar commitDate) {
		commitCounter = 0;
		this.baseCommitForNonRefactoring = commitHash;
		this.baseCommitDateForNonRefactoring = commitDate;

		baseLinesAdded = linesAdded;
		baseLinesDeleted = linesDeleted;
		baseBugFixCount = bugFixCount;
		baseRefactoringsInvolved = refactoringsInvolved;
		baseMinorAuthors = qtyMinorAuthors();
		baseMajorAuthors = qtyMajorAuthors();
		baseAuthorOwnership = authorOwnership();
		baseAuthors = qtyOfAuthors();
		baseCommits = commits;
	}

	public void increaseCounter() {
		commitCounter++;
	}

	public int counter() {
		return commitCounter;
	}

	public String getFileName () {
		return fileName;
	}

	public String getBaseCommitForNonRefactoring () { return baseCommitForNonRefactoring; }

	public Calendar getBaseCommitDateForNonRefactoring() { return baseCommitDateForNonRefactoring; }

	public long qtyMinorAuthors() {
		return countAuthors(author -> authors.get(author) < fivePercent());
	}

	public long qtyMajorAuthors() {
		return countAuthors(author -> authors.get(author) >= fivePercent());
	}

	public double authorOwnership() {
		if(authors.entrySet().isEmpty()) return 0;

		String mostRecurrentAuthor = Collections.max(authors.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();

		return authors.get(mostRecurrentAuthor) / (double) commits;
	}

	private double fivePercent () {
		return commits * 0.05;
	}

	private long countAuthors (Predicate<String> predicate) {
		return authors.keySet().stream()
				.filter(predicate)
				.count();
	}

	public int getBaseCommits() {
		return baseCommits;
	}

	public int getBaseAuthors() {
		return baseAuthors;
	}

	public double getBaseAuthorOwnership() {
		return baseAuthorOwnership;
	}

	public long getBaseMajorAuthors() {
		return baseMajorAuthors;
	}

	public long getBaseMinorAuthors() {
		return baseMinorAuthors;
	}

	public int getBaseRefactoringsInvolved() {
		return baseRefactoringsInvolved;
	}

	public int getBaseBugFixCount() {
		return baseBugFixCount;
	}

	public int getBaseLinesDeleted() {
		return baseLinesDeleted;
	}

	public int getBaseLinesAdded() {
		return baseLinesAdded;
	}

	public int getLinesDeleted() {
		return linesDeleted;
	}

	public int getLinesAdded() {
		return linesAdded;
	}

	public int getBugFixCount() {
		return bugFixCount;
	}

	public int getRefactoringsInvolved() {
		return refactoringsInvolved;
	}

	public void increaseRefactoringsInvolved() {
		refactoringsInvolved++;
	}

	//Was this class file not refactored in the last K commits affecting this class file?
	public boolean isStable(int commitThreshold){
		return commitCounter >= commitThreshold;
	}

	public int getCommitCounter() { return commitCounter; }

	@Override
	public String toString() {
		return "ProcessMetric{" +
				"fileName='" + fileName + '\'' +
				", commits=" + commits +
				", authors=" + authors +
				", linesAdded=" + linesAdded +
				", linesDeleted=" + linesDeleted +
				", bugFixCount=" + bugFixCount +
				", refactoringsInvolved=" + refactoringsInvolved +
				", commitCounter=" + commitCounter +
				", baseCommitForNonRefactoring='" + baseCommitForNonRefactoring + '\'' +
				", baseLinesAdded=" + baseLinesAdded +
				", baseLinesDeleted=" + baseLinesDeleted +
				", baseBugFixCount=" + baseBugFixCount +
				", baseRefactoringsInvolved=" + baseRefactoringsInvolved +
				", baseMinorAuthors=" + baseMinorAuthors +
				", baseMajorAuthors=" + baseMajorAuthors +
				", baseAuthorOwnership=" + baseAuthorOwnership +
				", baseAuthors=" + baseAuthors +
				", baseCommits=" + baseCommits +
				'}';
	}

	public int qtyOfCommits() {
		return this.commits;
	}
}
