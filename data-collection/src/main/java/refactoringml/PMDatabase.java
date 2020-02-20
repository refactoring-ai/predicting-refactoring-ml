package refactoringml;

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

	public boolean containsKey (String fileName) {
		return database.containsKey(fileName);
	}

	public void put (String key, ProcessMetricTracker value) {
		database.put(key, value);
	}

	public ProcessMetricTracker get (String key) {
		return database.get(key);
	}

	public List<ProcessMetricTracker> refactoredLongAgo () {
		return database.values().stream()
				.filter(p -> p.getCommitCounter() >= commitThreshold)
				.collect(Collectors.toList());
	}

	public void remove (ProcessMetricTracker clazz) {
		remove(clazz.getFileName());
	}

	public void remove (String key) {
		database.remove(key);
	}

	public Map<String, ProcessMetricTracker> getDatabase() {
		return database;
	}
}
