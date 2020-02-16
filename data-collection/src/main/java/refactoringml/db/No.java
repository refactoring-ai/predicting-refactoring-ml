package refactoringml.db;

import refactoringml.util.RefactoringUtils;
import javax.persistence.*;

//TODO: create a Baseclass for both Yes and No, as they share a lot of logic
@Entity
@Table(name = "no", indexes = {@Index(columnList = "project_id"), @Index(columnList = "type"), @Index(columnList = "isTest")})
public class No {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne
	//project id: referencing the project information, e.g. name or gitUrl
	private Project project;

	@ManyToOne(cascade = CascadeType.ALL)
	private CommitMetaData commitMetaData;

	//relative filepath to the java file of the refactored class
	private String filePath;
	//name of the refactored class, @Warning: might differ from the filename
	private String className;
	//is this commit affecting a test?
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

	//TODO: what exactly describes this field?
	private int type;

	@Deprecated // hibernate purposes
	public No() {}

	public No(Project project, CommitMetaData commitMetaData, String filePath, String className, ClassMetric classMetrics,
			  MethodMetric methodMetrics, VariableMetric variableMetrics, FieldMetric fieldMetrics, int type) {
		this.project = project;
		this.commitMetaData = commitMetaData;
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

	public String getCommit() {
		return commitMetaData.getCommit();
	}

	public ProcessMetrics getProcessMetrics() { return processMetrics; }

	public ClassMetric getClassMetrics() {
		return classMetrics;
	}

	public String getClassName() { return className; }

	public String getCommitMessage (){return commitMetaData.getCommitMessage();}

	public String getCommitUrl (){return commitMetaData.getCommitUrl();}

	@Override
	public String toString() {
		return "No{" +
				"id=" + id +
				", project=" + project +
				", commitMetaData=" + commitMetaData +
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
}