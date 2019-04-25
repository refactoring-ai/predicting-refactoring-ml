package refactoringml.db;

import javax.persistence.*;
import java.util.Calendar;

@Entity
@Table(name = "project", indexes = {@Index(columnList = "datasetName"), @Index(columnList = "projectName")})
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

	private int commits;

	private int threshold;

	@Deprecated // hibernate purposes
	public Project() {}

	public Project(String datasetName, String gitUrl, String projectName, Calendar dateOfProcessing, int commits, int threshold) {
		this.datasetName = datasetName;
		this.gitUrl = gitUrl;
		this.projectName = projectName;
		this.dateOfProcessing = dateOfProcessing;
		this.commits = commits;
		this.threshold = threshold;
	}

	public void setFinishedDate(Calendar finishedDate) {
		this.finishedDate = finishedDate;
	}
}
