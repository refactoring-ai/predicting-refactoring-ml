package refactoringml.db;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class VariableMetric {

	@Column(nullable = true) private String variableName;
	@Column(nullable = true) private int variableAppearances;

	@Deprecated // hibernate purposes
	public VariableMetric(){}

	public VariableMetric(String variableName, int variableAppearances) {
		this.variableName = variableName;
		this.variableAppearances = variableAppearances;
	}
}
