package refactoringml.db;

import javax.persistence.*;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

@Entity
@Table(name = "ProcessMetrics")
public class ProcessMetrics {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = true) public int qtyOfCommits = 0;
	@Column(nullable = true) public int linesAdded = 0;
	@Column(nullable = true) public int linesDeleted = 0;
	@Column(nullable = true) public int bugFixCount = 0;
	@Column(nullable = true) public int refactoringsInvolved = 0;

	//all authors with commits affecting this class file
	@Transient
	public Map<String, Integer> allAuthors = new HashMap<>();

	@Deprecated // hibernate purposes
	public ProcessMetrics() {}

	public ProcessMetrics(int qtyOfCommits, int linesAdded, int linesDeleted, int bugFixCount, int refactoringsInvolved) {
		this.qtyOfCommits = qtyOfCommits;
		this.linesAdded = linesAdded;
		this.linesDeleted = linesDeleted;
		this.bugFixCount = bugFixCount;
		this.refactoringsInvolved = refactoringsInvolved;
	}

	//Deep copy the ProcessMetrics collected by ProcessMetricTracker
	public ProcessMetrics(ProcessMetrics pm){
		this(pm.qtyOfCommits, pm.linesAdded, pm.linesDeleted, pm.bugFixCount, pm.refactoringsInvolved);
		this.allAuthors = new HashMap<>(pm.allAuthors);
	}

	//Properties
	@Column(nullable = true)
	public int qtyOfAuthors() { return allAuthors.size(); }

	public long qtyMinorAuthors() {
		return countAuthors(author -> allAuthors.get(author) < fivePercent());
	}

	public long qtyMajorAuthors() {
		return countAuthors(author -> allAuthors.get(author) >= fivePercent());
	}

	public double authorOwnership() {
		if(allAuthors.entrySet().isEmpty()) return 0;

		String mostRecurrentAuthor = Collections.max(allAuthors.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();

		return allAuthors.get(mostRecurrentAuthor) / (double) qtyOfCommits;
	}

	//utils
	private double fivePercent () {
		return qtyOfCommits * 0.05;
	}

	private long countAuthors (Predicate<String> predicate) {
		return allAuthors.keySet().stream()
				.filter(predicate)
				.count();
	}

	@Override
	public String toString() {
		return toString(qtyOfCommits, linesAdded, linesDeleted, qtyOfAuthors(), qtyMinorAuthors(),
				qtyMajorAuthors(), authorOwnership(), bugFixCount, refactoringsInvolved);
	}

	//for testing
	public static String toString(int qtyOfCommits, int linesAdded, int linesDeleted, int qtyOfAuthors, long qtyMinorAuthors,
						   long qtyMajorAuthors, double authorOwnership, int bugFixCount, int refactoringsInvolved){
		return "ProcessMetrics{" +
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
}