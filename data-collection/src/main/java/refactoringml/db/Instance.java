package refactoringml.db;

import javax.persistence.*;

import static refactoringml.util.FilePathUtils.enforceUnixPaths;
import static refactoringml.util.FileUtils.IsTestFile;

//Base class for all commits saved in the DB
@MappedSuperclass
public abstract class Instance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected long id;

    @ManyToOne
    //project id: referencing the project information, e.g. name or gitUrl
    protected Project project;

    @ManyToOne(cascade = CascadeType.ALL)
    protected CommitMetaData commitMetaData;

    //relative filepath to the java file of the class file
    @Column(columnDefinition="TEXT")
    protected String filePath;
    //name of the class, @Warning: might differ from the filename
    protected String className;
    //is this commit affecting a test?
    protected boolean isTest;

    //Describes the level of the class being affected, e.g. class level or method level refactoring
    //For a mapping see: RefactoringUtils
    //TODO: make this an enum, for better readibility
    private int level;

    @ManyToOne(cascade = CascadeType.ALL)
    protected ClassMetric classMetrics;

    @ManyToOne(cascade = CascadeType.ALL)
    protected MethodMetric methodMetrics;

    @ManyToOne(cascade = CascadeType.ALL)
    protected VariableMetric variableMetrics;

    @ManyToOne(cascade = CascadeType.ALL)
    protected FieldMetric fieldMetrics;

    @ManyToOne(cascade = CascadeType.ALL)
    protected ProcessMetrics processMetrics;

    @Deprecated
    public Instance(){}

    public Instance(Project project, CommitMetaData commitMetaData, String filePath, String className, ClassMetric classMetrics,
                    MethodMetric methodMetrics, VariableMetric variableMetrics, FieldMetric fieldMetrics, int level) {
        this.project = project;
        this.commitMetaData = commitMetaData;
        this.filePath = enforceUnixPaths(filePath);
        this.className = className;
        this.classMetrics = classMetrics;
        this.methodMetrics = methodMetrics;
        this.variableMetrics = variableMetrics;
        this.fieldMetrics = fieldMetrics;
        this.level = level;

        this.isTest = IsTestFile(this.filePath);
    }

    public void setProcessMetrics(ProcessMetrics processMetrics) { this.processMetrics = processMetrics; }

    public String getCommit() { return commitMetaData.getCommitId(); }

    public ProcessMetrics getProcessMetrics() { return processMetrics; }

    public MethodMetric getMethodMetrics() { return methodMetrics; }

    public ClassMetric getClassMetrics() { return classMetrics; }

    public FieldMetric getFieldMetrics() { return fieldMetrics; }

    public VariableMetric getVariableMetrics() { return variableMetrics; }

    public String getClassName() { return className; }

    public String getCommitMessage() {return commitMetaData.getCommitMessage();}

    public String getCommitUrl() {return commitMetaData.getCommitUrl();}

    public boolean getIsTest() { return isTest; }

    public String getFilePath() { return filePath; }

    public int getLevel() { return level; }

    public long getId() { return id; }

    public CommitMetaData getCommitMetaData() { return commitMetaData; }

    @Override
    public String toString() {
        return
                ", project=" + project +
                ", commitMetaData=" + commitMetaData +
                ", filePath='" + filePath + '\'' +
                ", className='" + className + '\'' +
                ", classMetrics=" + classMetrics +
                ", methodMetrics=" + methodMetrics +
                ", variableMetrics=" + variableMetrics +
                ", fieldMetrics=" + fieldMetrics +
                ", processMetrics=" + processMetrics +
                ", level=" + level;
    }
}
