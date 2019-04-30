package refactoringml.db;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class FieldMetric {

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

}
