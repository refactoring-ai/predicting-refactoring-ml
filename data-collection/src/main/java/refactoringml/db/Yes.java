package refactoringml.db;

import javax.persistence.*;

@Entity
public class Yes {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne
	private Project project;

	private String refactorCommit;
	private String parentCommit;
	private String filePath;
	private String className;

	private String refactoring;
	private int refactoringType;

	@Embedded
	private ClassMetric classMetrics;

	@Embedded
	private MethodMetric methodMetrics;

	@Embedded
	private VariableMetric variableMetrics;

	@Embedded
	private FieldMetric fieldMetrics;

	@Embedded
	private ProcessMetrics processMetrics;

	public Yes() {}

	public Yes(Project project, String refactorCommit, String parentCommit, String filePath, String className,String refactoring, int refactoringType,
	           ClassMetric classMetrics, MethodMetric methodMetrics, VariableMetric variableMetrics, FieldMetric fieldMetrics) {
		this.project = project;
		this.refactorCommit = refactorCommit;
		this.parentCommit = parentCommit;
		this.filePath = filePath;
		this.className = className;
		this.refactoring = refactoring;
		this.refactoringType = refactoringType;
		this.classMetrics = classMetrics;
		this.methodMetrics = methodMetrics;
		this.variableMetrics = variableMetrics;
		this.fieldMetrics = fieldMetrics;
	}

	public void setProcessMetrics(ProcessMetrics processMetrics) {
		this.processMetrics = processMetrics;
	}

	public String getFilePath() {
		return filePath;
	}

	public long getId() {
		return id;
	}
}
