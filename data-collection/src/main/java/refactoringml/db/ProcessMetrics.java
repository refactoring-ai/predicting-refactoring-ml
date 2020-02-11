package refactoringml.db;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ProcessMetrics {


	@Column(nullable = true) private boolean processMetricsAvailable;
	@Column(nullable = true) private int qtyOfCommits;
	@Column(nullable = true) private int linesAdded;
	@Column(nullable = true) private int linesDeleted;
	@Column(nullable = true) private int qtyOfAuthors;
	@Column(nullable = true) private long qtyMinorAuthors;
	@Column(nullable = true) private long qtyMajorAuthors;
	@Column(nullable = true) private double authorOwnership;
	@Column(nullable = true) private int bugFixCount;
	@Column(nullable = true) private int refactoringsInvolved;

	@Deprecated // hibernate purposes
	public ProcessMetrics() {}

	public ProcessMetrics(boolean processMetricsAvailable, int qtyOfCommits, int linesAdded, int linesDeleted, int qtyOfAuthors, long qtyMinorAuthors,
						  long qtyMajorAuthors, double authorOwnership, int bugFixCount, int refactoringsInvolved) {
		this.refactoringsInvolved = refactoringsInvolved;
		this.processMetricsAvailable = processMetricsAvailable;
		this.qtyOfCommits = qtyOfCommits;
		this.linesAdded = linesAdded;
		this.linesDeleted = linesDeleted;
		this.qtyOfAuthors = qtyOfAuthors;
		this.qtyMinorAuthors = qtyMinorAuthors;
		this.qtyMajorAuthors = qtyMajorAuthors;
		this.authorOwnership = authorOwnership;
		this.bugFixCount = bugFixCount;
	}

	@Override
	public String toString() {
		return "ProcessMetrics{" +
				"processMetricsAvailable=" + processMetricsAvailable +
				"qtyOfCommits=" + qtyOfCommits +
				", linesAdded=" + linesAdded +
				", linesDeleted=" + linesDeleted +
				", qtyOfAuthors=" + qtyOfAuthors +
				", qtyMinorAuthors=" + qtyMinorAuthors +
				", qtyMajorAuthors=" + qtyMajorAuthors +
				", authorOwnership=" + authorOwnership +
				", bugFixCount=" + bugFixCount +
				", refactoringsInvolved=" + refactoringsInvolved +
				'}';
	}

	public boolean hasProcessMetrics() { return processMetricsAvailable; }

	public int getQtyOfCommits() {
		return qtyOfCommits;
	}

	public int getLinesAdded() {
		return linesAdded;
	}

	public int getLinesDeleted() {
		return linesDeleted;
	}

	public int getQtyOfAuthors() {
		return qtyOfAuthors;
	}

	public long getQtyMinorAuthors() {
		return qtyMinorAuthors;
	}

	public long getQtyMajorAuthors() {
		return qtyMajorAuthors;
	}

	public double getAuthorOwnership() {
		return authorOwnership;
	}

	public int getBugFixCount() {
		return bugFixCount;
	}

	public int getRefactoringsInvolved() {
		return refactoringsInvolved;
	}
}
