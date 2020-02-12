package refactoringml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PMDatabase {
	/*
	Maps class names onto their original process metrics.
	 */
	private Map<String, ProcessMetric> database;
	private int commitThreshold;

	public PMDatabase (int commitThreshold) {
		this.commitThreshold = commitThreshold;
		this.database = new HashMap<>();
	}

	public boolean containsKey (String fileName) {
		return database.containsKey(fileName);
	}

	public void put (String key, ProcessMetric value) {
		database.put(key, value);
	}

	public ProcessMetric get (String key) {
		return database.get(key);
	}

	public List<ProcessMetric> refactoredLongAgo () {
		return database.values().stream()
				.filter(p -> p.counter() >= commitThreshold)
				.collect(Collectors.toList());
	}

	public void remove (ProcessMetric clazz) {
		remove(clazz.getFileName());
	}

	public void remove (String key) {
		database.remove(key);
	}

	public Map<String, ProcessMetric> getDatabase() {
		return database;
	}

	/*
	Report the rename of a class in order to track its process metrics.
	In case of (various renames), the names are replaced, e.g.
	1. Rename People to Person: Person -> People_ProcessMetrics
	2. Rename Person to Human: Human -> People_ProcessMetrics
	 */
	public boolean rename(String oldClassName, String newClassName){
		ProcessMetric metric = database.remove(oldClassName);
		return database.put(newClassName, metric) != null;
	}
}
