package refactoringml.db;

import javax.persistence.*;

//TODO: Why do we have No at all?
// Can't we just add a reference to a yes refactoring to the No table and add the K?
// Then we know for how long that refactoring was stable, have the same data and reduce the table size.
@Entity
@Table(name = "no", indexes = {@Index(columnList = "project_id"), @Index(columnList = "level"), @Index(columnList = "isTest")})
public class No extends Commit{
	@Deprecated // hibernate purposes
	public No() {}

	public No(Project project, CommitMetaData commitMetaData, String filePath, String className, ClassMetric classMetrics,
			  MethodMetric methodMetrics, VariableMetric variableMetrics, FieldMetric fieldMetrics, int level) {
		super(project, commitMetaData, filePath, className, classMetrics, methodMetrics, variableMetrics, fieldMetrics, level);
	}

	@Override
	public String toString() {
		return "No{" + "id=" + id + super.toString() + '}';
	}
}