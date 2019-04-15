package refactoringml.db;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class ProcessMetrics {

	@Column(nullable = true) private int qtyOfCommits;
	@Column(nullable = true) private int linesAdded;
	@Column(nullable = true) private int linesDeleted;
	@Column(nullable = true) private int qtyOfAuthors;
	@Column(nullable = true) private long qtyMinorAuthors;
	@Column(nullable = true) private long qtyMajorAuthors;
	@Column(nullable = true) private double authorOwnership;
	@Column(nullable = true) private int bugFixCount;
	@Column(nullable = true) private int refactoringsInvolved;

	private ProcessMetrics() {}

	public ProcessMetrics(int qtyOfCommits, int linesAdded, int linesDeleted, int qtyOfAuthors, long qtyMinorAuthors,
	                      long qtyMajorAuthors, double authorOwnership, int bugFixCount, int refactoringsInvolved) {
		this.qtyOfCommits = qtyOfCommits;
		this.linesAdded = linesAdded;
		this.linesDeleted = linesDeleted;
		this.qtyOfAuthors = qtyOfAuthors;
		this.qtyMinorAuthors = qtyMinorAuthors;
		this.qtyMajorAuthors = qtyMajorAuthors;
		this.authorOwnership = authorOwnership;
		this.bugFixCount = bugFixCount;
		this.refactoringsInvolved = refactoringsInvolved;
	}
}
