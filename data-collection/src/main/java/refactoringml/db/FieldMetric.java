package refactoringml.db;

import javax.persistence.*;

@Entity
@Table(name = "FieldMetric")
public class FieldMetric {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@Column(nullable = true)
	private String fieldName;
	@Column(nullable = true)
	private int fieldAppearances;

	@Deprecated // hibernate purposes
	public FieldMetric(){}

	public FieldMetric(String fieldName, int fieldAppearances) {
		this.fieldName = fieldName;
		this.fieldAppearances = fieldAppearances;
	}

	public String getFieldName() {
		return fieldName;
	}

	@Override
	public String toString() {
		return "FieldMetric{" +
				"fieldName='" + fieldName + '\'' +
				", fieldAppearances=" + fieldAppearances +
				'}';
	}
}
