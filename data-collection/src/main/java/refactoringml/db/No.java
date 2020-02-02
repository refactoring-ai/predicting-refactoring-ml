package refactoringml.db;

import refactoringml.util.RefactoringUtils;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "no", indexes = {@Index(columnList = "project_id"), @Index(columnList = "type"), @Index(columnList = "isTest")})
public class No {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne
	private Project project;

	private String commit;

	@Temporal(TemporalType.TIMESTAMP)
	private Calendar commitDate;

	private String filePath;
	private String className;
	private boolean isTest;


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

	private int type;

	@Deprecated // hibernate purposes
	public No() {}

	public No(Project project, String commit, Calendar commitDate, String filePath, String className,
	          ClassMetric classMetrics, MethodMetric methodMetrics, VariableMetric variableMetrics, FieldMetric fieldMetrics, int type) {
		this.project = project;
		this.commit = commit;
		this.commitDate = commitDate;
		this.filePath = filePath;
		this.className = className;
		this.classMetrics = classMetrics;
		this.methodMetrics = methodMetrics;
		this.variableMetrics = variableMetrics;
		this.fieldMetrics = fieldMetrics;
		this.type = type;

		this.isTest = RefactoringUtils.isTestFile(this.filePath);
	}

	public void setProcessMetrics(ProcessMetrics processMetrics) {
		this.processMetrics = processMetrics;
	}

	@Override
	public String toString() {
		return "No{" +
				"id=" + id +
				", project=" + project +
				", commit='" + commit + '\'' +
				", filePath='" + filePath + '\'' +
				", className='" + className + '\'' +
				", classMetrics=" + classMetrics +
				", methodMetrics=" + methodMetrics +
				", variableMetrics=" + variableMetrics +
				", fieldMetrics=" + fieldMetrics +
				", processMetrics=" + processMetrics +
				", type=" + type +
				'}';
	}

	public String getCommit() {
		return commit;
	}
}
