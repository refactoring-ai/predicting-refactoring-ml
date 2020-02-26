package refactoringml.db;

import javax.persistence.*;

@Entity
@Table(name = "StableCommit", indexes = {@Index(columnList = "project_id"), @Index(columnList = "level"), @Index(columnList = "isTest"), @Index(columnList = "commitThreshold")})
public class StableCommit extends Instance{
	//The commit threshold for which this class is considered as stable,
	private int commitThreshold;

	@Deprecated // hibernate purposes
	public StableCommit() {}

	public StableCommit(Project project, CommitMetaData commitMetaData, String filePath, String className, ClassMetric classMetrics,
			  MethodMetric methodMetrics, VariableMetric variableMetrics, FieldMetric fieldMetrics, int level, int commitThreshold) {
		super(project, commitMetaData, filePath, className, classMetrics, methodMetrics, variableMetrics, fieldMetrics, level);
		this.commitThreshold = commitThreshold;
	}

	public int getCommitThreshold(){ return commitThreshold; }

	@Override
	public String toString() {
		return "StableCommit{" + "id=" + id +
				super.toString() +
				", commitThreshold=" + commitThreshold + '}';
	}
}