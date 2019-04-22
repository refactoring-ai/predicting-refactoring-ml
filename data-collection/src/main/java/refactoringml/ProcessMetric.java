package refactoringml;

import java.util.*;
import java.util.function.Predicate;

public class ProcessMetric {

	private String fileName;

	// updated info about the class
	private Set<String> commits = new HashSet<>();
	private Map<String, Integer> authors = new HashMap<String, Integer>();
	private int linesAdded = 0;
	private int linesDeleted = 0;
	private int bugFixCount = 0;
	private int refactoringsInvolved = 0;


	private int lastRefactoring = 0;

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

	public ProcessMetric (String fileName, String baseCommitForNonRefactoring) {
		this.fileName = fileName;
		this.baseCommitForNonRefactoring = baseCommitForNonRefactoring;
	}


	public void existsIn (String commit, String commitMsg, String authorName, int linesAdded, int linesDeleted) {
		commits.add(commit);

		if(!authors.containsKey(authorName)) {
			authors.put(authorName, 0);
		}
		authors.put(authorName, authors.get(authorName)+1);

		this.linesAdded += linesAdded;
		this.linesDeleted += linesDeleted;

		if(isBugFix(commitMsg))
			bugFixCount++;

		// we increase the counter here. This means a class will go to the 'non refactored' bucket
		// only after we see it X times (and not involved in a refactoring, otherwise, counters are resetted).
		this.notRefactoredInThisCommit();

	}

	private boolean isBugFix(String commitMsg) {
		String cleanCommitMsg = commitMsg.toLowerCase();
		return Arrays.stream(bugKeywords).filter(keyword -> cleanCommitMsg.contains(keyword))
				.count() > 0;
	}

	public int qtyOfCommits() {
		return commits.size();
	}

	public int qtyOfAuthors() {
		return authors.size();
	}

	public void resetLastRefactoringStats(String commitHash) {
		lastRefactoring = 0;
		this.baseCommitForNonRefactoring = commitHash;

		baseLinesAdded = linesAdded;
		baseLinesDeleted = linesDeleted;
		baseBugFixCount = bugFixCount;
		baseRefactoringsInvolved = refactoringsInvolved;
		baseMinorAuthors = qtyMinorAuthors();
		baseMajorAuthors = qtyMajorAuthors();
		baseAuthorOwnership = authorOwnership();
		baseAuthors = qtyOfAuthors();
		baseCommits = qtyOfCommits();
	}

	public void notRefactoredInThisCommit () {
		lastRefactoring++;
	}

	public int lastRefactoring () {
		return lastRefactoring;
	}

	public String getFileName () {
		return fileName;
	}

	public String getBaseCommitForNonRefactoring () {
		return baseCommitForNonRefactoring;
	}

	public long qtyMinorAuthors() {
		return countAuthors(author -> authors.get(author) < fivePercent());
	}

	public long qtyMajorAuthors() {
		return countAuthors(author -> authors.get(author) >= fivePercent());
	}

	public double authorOwnership() {
		if(authors.entrySet().isEmpty()) return 0;

		String mostRecurrentAuthor = Collections.max(authors.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();

		return authors.get(mostRecurrentAuthor) / (double) qtyOfCommits();
	}

	private double fivePercent () {
		return qtyOfCommits() * 0.05;
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

	public void increaseRefactoringCounter() {
		refactoringsInvolved++;
	}
}
