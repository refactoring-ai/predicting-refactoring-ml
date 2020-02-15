package refactoringml.db;

import org.hibernate.annotations.Type;
import refactoringml.util.RefactoringUtils;
import javax.persistence.*;

//TODO: create a Baseclass for both Yes and No, as they share a lot of logic
@Entity
@Table(name = "yes", indexes = {@Index(columnList = "project_id"), @Index(columnList = "refactoringLevel"), @Index(columnList = "refactoring"), @Index(columnList = "isTest")})
public class Yes {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne(targetEntity = CommitMetaData.class)
	//project id: referencing the project information, e.g. name or gitUrl
	private Project project;

	@ManyToOne
	private CommitMetaData commitMetaData;

	//relative filepath to the java file of the refactored class
	private String filePath;
	//name of the refactored class, @Warning: might differ from the filename
	private String className;
	//is this refactoring in this commit affecting a test?
	private boolean isTest;

	//Describes the level of the refactoring, e.g. class level or method level refactoring
	//For a mapping see: RefactoringUtils
	//TODO: make this an enum, for better readibility
	private int refactoringLevel;
	//Describe the refactoring e.g. "Rename Class" or "Extract Method"
	private String refactoring;
	//Describe the content of the refactoring e.g. "Rename Pets to Cat"
	@Type(type="text")
	private String refactoringSummary;

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

	public Yes(Project project, CommitMetaData commitMetaData, String filePath, String className, String refactoring, int refactoringLevel,
	           String refactoringSummary, ClassMetric classMetrics, MethodMetric methodMetrics, VariableMetric variableMetrics, FieldMetric fieldMetrics) {
		this.project = project;
		this.filePath = filePath;
		this.className = className;
		this.refactoring = refactoring;
		this.refactoringLevel = refactoringLevel;
		this.classMetrics = classMetrics;
		this.methodMetrics = methodMetrics;
		this.variableMetrics = variableMetrics;
		this.fieldMetrics = fieldMetrics;
		this.commitMetaData = commitMetaData;
		this.refactoringSummary = refactoringSummary.trim();

		this.isTest = RefactoringUtils.isTestFile(this.filePath);
	}

	public void setProcessMetrics(ProcessMetrics processMetrics) {
		this.processMetrics = processMetrics;
	}

	public String getFilePath() { return filePath; }

	public long getId() { return id; }

	public int getRefactoringLevel() { return refactoringLevel; }

	public String getRefactoring() { return refactoring; }

	public MethodMetric getMethodMetrics() { return methodMetrics; }

	public VariableMetric getVariableMetrics() { return variableMetrics; }

	public FieldMetric getFieldMetrics() { return fieldMetrics; }

	public String getClassName() { return className; }

	public ClassMetric getClassMetrics() { return classMetrics; }

	public String getRefactorCommit() { return commitMetaData.getCommit(); }

	public String getCommitMessage (){return commitMetaData.getCommitMessage();}

	public String getRefactoringSummary (){return refactoringSummary;}

	public String getCommitUrl (){return commitMetaData.getCommitUrl();}

	public ProcessMetrics getProcessMetrics()  {
		return processMetrics;
	}

	@Override
	public String toString() {
		return "Yes{" +
				"id=" + id +
				", project=" + project +
				", commitMetaData=" + commitMetaData +
				", filePath='" + filePath + '\'' +
				", className='" + className + '\'' +
				", refactoringLevel=" + refactoringLevel +
				", refactoring='" + refactoring + '\'' +
				", refactoringSummary=" + refactoringSummary +
				", classMetrics=" + classMetrics +
				", methodMetrics=" + methodMetrics +
				", variableMetrics=" + variableMetrics +
				", fieldMetrics=" + fieldMetrics +
				", processMetrics=" + processMetrics +
				'}';
	}
}