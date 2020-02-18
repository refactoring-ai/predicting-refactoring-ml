package refactoringml.db;

import javax.persistence.*;

@Entity
@Table(name = "StableCommit", indexes = {@Index(columnList = "project_id"), @Index(columnList = "level"), @Index(columnList = "isTest")})
public class StableCommit extends Instance{
	@Deprecated // hibernate purposes
	public StableCommit() {}

	public StableCommit(Project project, CommitMetaData commitMetaData, String filePath, String className, ClassMetric classMetrics,
			  MethodMetric methodMetrics, VariableMetric variableMetrics, FieldMetric fieldMetrics, int level) {
		super(project, commitMetaData, filePath, className, classMetrics, methodMetrics, variableMetrics, fieldMetrics, level);
	}

	@Override
	public String toString() {
		return "No{" + "id=" + id + super.toString() + '}';
	}
}