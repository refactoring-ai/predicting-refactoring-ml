package refactoringml.db;

import javax.persistence.*;
import java.util.Calendar;

import static refactoringml.util.Counter.*;

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
	private long javaLoc;

	private long numberOfProductionFiles;
	private long numberOfTestFiles;
	private long productionLoc;
	private long testLoc;

	private int exceptionsCount;
	private int cleanedRows;

	private String lastCommitHash;

	@Deprecated // hibernate purposes
	public Project() {}

	public Project(String datasetName, String gitUrl, String projectName, Calendar dateOfProcessing, int commits, int threshold, String lastCommitHash, CounterResult c) {
		this.datasetName = datasetName;
		this.gitUrl = gitUrl;
		this.projectName = projectName;
		this.dateOfProcessing = dateOfProcessing;
		this.commits = commits;
		this.threshold = threshold;
		this.lastCommitHash = lastCommitHash;

		this.numberOfProductionFiles = c.getQtyOfProductionFiles();
		this.numberOfTestFiles = c.getQtyOfTestFiles();
		this.productionLoc = c.getLocProductionFiles();
		this.testLoc = c.getLocTestFiles();
		this.javaLoc = this.productionLoc + this.testLoc;

	}

	public void setFinishedDate(Calendar finishedDate) {
		this.finishedDate = finishedDate;
	}

	public void setExceptions(int exceptionsCount) {
		this.exceptionsCount = exceptionsCount;
	}

	public void setCleanedRows(int cleanedRows) {
		this.cleanedRows = cleanedRows;
	}

	public long getId() {
		return id;
	}

	public long getJavaLoc() {
		return javaLoc;
	}

	public long getNumberOfProductionFiles() {
		return numberOfProductionFiles;
	}

	public long getNumberOfTestFiles() {
		return numberOfTestFiles;
	}

	public long getProductionLoc() {
		return productionLoc;
	}

	public long getTestLoc() {
		return testLoc;
	}

	@Override
	public String toString() {
		return "Project{" +
				"id=" + id +
				", datasetName='" + datasetName + '\'' +
				", gitUrl='" + gitUrl + '\'' +
				", projectName='" + projectName + '\'' +
				", dateOfProcessing=" + dateOfProcessing +
				", finishedDate=" + finishedDate +
				", commits=" + commits +
				", threshold=" + threshold +
				", javaLoc=" + javaLoc +
				", numberOfProductionFiles=" + numberOfProductionFiles +
				", numberOfTestFiles=" + numberOfTestFiles +
				", productionLoc=" + productionLoc +
				", testLoc=" + testLoc +
				", exceptionsCount=" + exceptionsCount +
				", cleanedRows=" + cleanedRows +
				", lastCommitHash='" + lastCommitHash + '\'' +
				'}';
	}
}
