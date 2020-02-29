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

	//number of commits making changes to this class
	@Column(nullable = true) public int qtyOfCommits = 0;
	//total lines added to this class file
	@Column(nullable = true) public int linesAdded = 0;
	//total lines deleted from this class file
	@Column(nullable = true) public int linesDeleted = 0;
	//total count of (detected) bug fixes on this class file
	@Column(nullable = true) public int bugFixCount = 0;
	//total count of (detected) refactorings on this class file
	@Column(nullable = true) public int refactoringsInvolved = 0;

	@Column(nullable = true) private int qtyOfAuthors;
	@Column(nullable = true) private int qtyMinorAuthors;
	@Column(nullable = true) private int qtyMajorAuthors;
	@Column(nullable = true) private double authorOwnership;

	//all authors with commits affecting this class file
	@ElementCollection
	@CollectionTable(name = "authorName",
			joinColumns = {@JoinColumn(name = "processmetrics_id", referencedColumnName = "id")})
	@MapKeyColumn(name = "authorName")
	@Column(name = "commitCount")
	private Map<String, Integer> allAuthors = new HashMap<>();

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
		this.allAuthors = new HashMap<>(pm.getAllAuthors());
		updateAuthors();
	}

	public void updateAuthorCommits(String authorName){
		if(!allAuthors.containsKey(authorName)) {
			allAuthors.put(authorName, 1);
		} else{
			allAuthors.put(authorName, allAuthors.get(authorName) + 1);
		}
		updateAuthors();
	}

	//Properties
	public int qtyOfAuthors() { return allAuthors.size(); }

	public int qtyMinorAuthors() {
		return countAuthors(author -> allAuthors.get(author) < fivePercent());
	}

	public int qtyMajorAuthors() {
		return countAuthors(author -> allAuthors.get(author) >= fivePercent());
	}

	public double authorOwnership() {
		if(allAuthors.entrySet().isEmpty()) return 0;

		String mostRecurrentAuthor = Collections.max(allAuthors.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();

		return allAuthors.get(mostRecurrentAuthor) / (double) qtyOfCommits;
	}

	public Map<String, Integer> getAllAuthors(){return allAuthors;}

	//utils
	private double fivePercent () {
		return qtyOfCommits * 0.05;
	}

	private int countAuthors (Predicate<String> predicate) {
		return (int) allAuthors.keySet().stream()
				.filter(predicate)
				.count();
	}

	private void updateAuthors(){
		this.qtyOfAuthors = qtyOfAuthors();
		this.qtyMinorAuthors = qtyMinorAuthors();
		this.qtyMajorAuthors = qtyMajorAuthors();
		this.authorOwnership = authorOwnership();
	}

	@Override
	public String toString() {
		return toString(qtyOfCommits, linesAdded, linesDeleted, qtyOfAuthors, qtyMinorAuthors,
				qtyMajorAuthors, authorOwnership, bugFixCount, refactoringsInvolved);
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