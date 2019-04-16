package refactoringml.db;

import javax.persistence.*;

@Entity
@Table(indexes = {@Index(columnList = "project_id")})
public class No {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne
	private Project project;

	private String commit;
	private String filePath;
	private String className;

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

	@Deprecated // hibernate purposes
	public No() {}

	public No(Project project, String commit, String filePath, String className,
	          ClassMetric classMetrics, MethodMetric methodMetrics, VariableMetric variableMetrics, FieldMetric fieldMetrics) {
		this.project = project;
		this.commit = commit;
		this.filePath = filePath;
		this.className = className;
		this.classMetrics = classMetrics;
		this.methodMetrics = methodMetrics;
		this.variableMetrics = variableMetrics;
		this.fieldMetrics = fieldMetrics;
	}

	public void setProcessMetrics(ProcessMetrics processMetrics) {
		this.processMetrics = processMetrics;
	}

}
