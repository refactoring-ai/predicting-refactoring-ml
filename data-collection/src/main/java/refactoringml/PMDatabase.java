package refactoringml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PMDatabase {
	/*
	Maps file names onto their original process metrics.
	TODO: Track both class and filename renames
	 I found that we currently only use filenames instead of classnames to track process metrics in the `PMDatabase.java`.
	 Thus, we use the same process metric for all classes in a file.
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

	public void remove (String key) { database.remove(key); }

	public Map<String, ProcessMetric> getDatabase() {
		return database;
	}

	/*
	Report the rename of a file in order to track its process metrics.
	In case of (various renames), the names are replaced, e.g.
	1. Rename People.java to Person.java: Person -> People_ProcessMetrics
	2. Rename Person.java to Human.java: Human -> People_ProcessMetrics
	 */
	public boolean rename(String oldFilename, String newFilename){
		ProcessMetric metric = database.remove(oldFilename);
		return database.put(newFilename, metric) != null;
	}
}
