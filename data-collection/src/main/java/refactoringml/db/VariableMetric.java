package refactoringml.db;

import javax.persistence.*;

@Entity
@Table(name = "VariableMetric")
public class VariableMetric {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = true) private String variableName;
	@Column(nullable = true) private int variableAppearances;

	@Deprecated // hibernate purposes
	public VariableMetric(){}

	public VariableMetric(String variableName, int variableAppearances) {
		this.variableName = variableName;
		this.variableAppearances = variableAppearances;
	}

	public String getVariableName() {
		return variableName;
	}

	@Override
	public String toString() {
		return "VariableMetric{" +
				"variableName='" + variableName + '\'' +
				", variableAppearances=" + variableAppearances +
				'}';
	}
}
