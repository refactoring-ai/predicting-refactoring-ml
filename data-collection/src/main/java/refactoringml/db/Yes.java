package refactoringml.db;

import refactoringml.util.JGitUtils;
import refactoringml.util.RefactoringUtils;

import javax.persistence.*;
import java.util.Calendar;

//TODO: create a Baseclass for both Yes and No, as they share a lot of logic
@Entity
@Table(name = "yes", indexes = {@Index(columnList = "project_id"), @Index(columnList = "refactoringLevel"), @Index(columnList = "refactoring"), @Index(columnList = "refactoringSummary"), @Index(columnList = "isTest")})
public class Yes {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	@ManyToOne
	//project id: referencing the project information, e.g. name or gitUrl
	private Project project;

	//id of this refactoring commit
	private String refactorCommit;
	//original commit message
	private String commitMessage;
	//url to the commit on its remote repository, e.g. https://github.com/mauricioaniche/predicting-refactoring-ml/commit/36016e4023cb74cd1076dbd33e0d7a73a6a61993
	private String commitUrl;
	@Temporal(TemporalType.TIMESTAMP)
	private Calendar refactoringDate;

	//id of the parent commit
	private String parentCommit;

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

	public Yes(Project project, String refactorCommit, String commitMessage, Calendar refactoringDate, String parentCommit, String filePath, String className, String refactoring, int refactoringLevel,
	           String refactoringSummary, ClassMetric classMetrics, MethodMetric methodMetrics, VariableMetric variableMetrics, FieldMetric fieldMetrics) {
		this.project = project;
		this.refactorCommit = refactorCommit;
		this.refactoringDate = refactoringDate;
		this.parentCommit = parentCommit;
		this.filePath = filePath;
		this.className = className;
		this.refactoring = refactoring;
		this.refactoringLevel = refactoringLevel;
		this.classMetrics = classMetrics;
		this.methodMetrics = methodMetrics;
		this.variableMetrics = variableMetrics;
		this.fieldMetrics = fieldMetrics;
		this.commitMessage = commitMessage.trim();
		this.commitUrl = JGitUtils.generateCommitUrl(project.getGitUrl(), refactorCommit, project.isLocal());
		this.refactoringSummary = refactoringSummary;

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

	public String getRefactorCommit() { return refactorCommit; }

	public String getCommitMessage (){return commitMessage;}

	public String getRefactoringSummary (){return refactoringSummary;}

	public String getCommitUrl (){return commitUrl;}

	@Override
	public String toString() {
		return "Yes{" +
				"id=" + id +
				", project=" + project +
				", refactorCommit='" + refactorCommit + '\'' +
				", commitMessage=" + commitMessage +
				", commitUrl=" + commitUrl +
				", refactoringDate=" + refactoringDate +
				", parentCommit='" + parentCommit + '\'' +
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