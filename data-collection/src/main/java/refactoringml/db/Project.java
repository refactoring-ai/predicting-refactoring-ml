package refactoringml.db;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(indexes = @Index(columnList = "datasetName"))
public class Project {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;

	private String datasetName;
	private String gitUrl;
	private String projectName;
	@Temporal(TemporalType.TIMESTAMP)
	private Calendar dateOfProcessing;

	@Temporal(TemporalType.TIMESTAMP)
	private Calendar finishedDate;

	@Deprecated // hibernate purposes
	public Project() {}

	public Project(String datasetName, String gitUrl, String projectName, Calendar dateOfProcessing) {
		this.datasetName = datasetName;
		this.gitUrl = gitUrl;
		this.projectName = projectName;
		this.dateOfProcessing = dateOfProcessing;
	}

	public void setFinishedDate(Calendar finishedDate) {
		this.finishedDate = finishedDate;
	}
}
