package refactoringml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PMDatabase {
	private Map<String, ProcessMetric> database;
	private List<Integer> commitThresholds;

	public PMDatabase (List<Integer> commitThresholds) {
		this.commitThresholds = commitThresholds;
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

	//Returns all instances that were not refactored for a long time.
	//Assumes the commitThresholds are sorted asc
	public List<ProcessMetric> findStableInstances() {
		List<ProcessMetric> stableInstances = new ArrayList<>();
		for (Integer threshold : commitThresholds){
			List<ProcessMetric> currentStableInstances = database.values().stream()
					.filter(p -> p.isStable(threshold))
					.collect(Collectors.toList());
			currentStableInstances.forEach(p -> p.setCommitCounterThreshold(threshold));
			stableInstances.addAll(currentStableInstances);
		}
		return stableInstances;
	}

	public void remove (ProcessMetric clazz) { remove(clazz.getFileName()); }

	public void remove (String key) {
		database.remove(key);
	}

	public Map<String, ProcessMetric> getDatabase() {
		return database;
	}
}
