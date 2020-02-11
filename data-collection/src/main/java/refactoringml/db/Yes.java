package refactoringml.db;

import refactoringml.ProcessMetric;
import refactoringml.util.RefactoringUtils;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "yes", indexes = {@Index(columnList = "project_id"), @Index(columnList = "refactoringType"), @Index(columnList = "refactoring"), @Index(columnList = "isTest")})
public class Yes {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne
	private Project project;

	private String refactorCommit;
	private String parentCommit;

	@Temporal(TemporalType.TIMESTAMP)
	private Calendar refactoringDate;

	private String filePath;
	private String className;
	private boolean isTest;


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

	@Deprecated // hibernate purposes
	public Yes() {}

	public Yes(Project project, String refactorCommit, Calendar refactoringDate, String parentCommit, String filePath, String className,String refactoring, int refactoringType,
	           ClassMetric classMetrics, MethodMetric methodMetrics, VariableMetric variableMetrics, FieldMetric fieldMetrics) {
		this.project = project;
		this.refactorCommit = refactorCommit;
		this.refactoringDate = refactoringDate;
		this.parentCommit = parentCommit;
		this.filePath = filePath;
		this.className = className;
		this.refactoring = refactoring;
		this.refactoringType = refactoringType;
		this.classMetrics = classMetrics;
		this.methodMetrics = methodMetrics;
		this.variableMetrics = variableMetrics;
		this.fieldMetrics = fieldMetrics;

		this.isTest = RefactoringUtils.isTestFile(this.filePath);
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

	public int getRefactoringType() {
		return refactoringType;
	}

	public String getRefactoring() {
		return refactoring;
	}

	public MethodMetric getMethodMetrics() {
		return methodMetrics;
	}

	public ProcessMetrics getProcessMetrics() { return processMetrics; }

	public VariableMetric getVariableMetrics() {
		return variableMetrics;
	}

	public FieldMetric getFieldMetrics() {
		return fieldMetrics;
	}

	public String getClassName() {
		return className;
	}

	@Override
	public String toString() {
		return "Yes{" +
				"id=" + id +
				", project=" + project +
				", refactorCommit='" + refactorCommit + '\'' +
				", parentCommit='" + parentCommit + '\'' +
				", refactoringDate=" + refactoringDate +
				", filePath='" + filePath + '\'' +
				", className='" + className + '\'' +
				", refactoring='" + refactoring + '\'' +
				", refactoringType=" + refactoringType +
				", classMetrics=" + classMetrics +
				", methodMetrics=" + methodMetrics +
				", variableMetrics=" + variableMetrics +
				", fieldMetrics=" + fieldMetrics +
				", processMetrics=" + processMetrics +
				'}';
	}

	public String getRefactorCommit() {
		return refactorCommit;
	}
}
