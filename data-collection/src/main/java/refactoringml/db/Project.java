package refactoringml.db;

import javax.persistence.*;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.stream.Collectors;

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

	//Collect instances of non-refactorings with different Ks e.g, 25,50,100 commits on a file without refactorings
	@ElementCollection
	private List<Integer> commitCountThresholds;

	private long javaLoc;

	private long numberOfProductionFiles;
	private long numberOfTestFiles;
	private long productionLoc;
	private long testLoc;
	private long projectSizeInBytes;

	private int exceptionsCount;

	private String lastCommitHash;
	//does the project have a remote origin, or is it a local one?
	private boolean isLocal;

	@Deprecated // hibernate purposes
	public Project() {}

	public Project(String datasetName, String gitUrl, String projectName, Calendar dateOfProcessing, int commits, String commitCountThresholds, String lastCommitHash, CounterResult c, long projectSizeInBytes) {
		this.datasetName = datasetName;
		this.gitUrl = gitUrl;
		this.projectName = projectName;
		this.dateOfProcessing = dateOfProcessing;
		this.commits = commits;
		this.lastCommitHash = lastCommitHash;

		this.numberOfProductionFiles = c.getQtyOfProductionFiles();
		this.numberOfTestFiles = c.getQtyOfTestFiles();
		this.productionLoc = c.getLocProductionFiles();
		this.testLoc = c.getLocTestFiles();
		this.projectSizeInBytes = projectSizeInBytes;
		this.javaLoc = this.productionLoc + this.testLoc;
		this.isLocal = isLocal(gitUrl);

		String[] rawCommitThresholds = commitCountThresholds.split(",");
		this.commitCountThresholds =  Arrays.stream(rawCommitThresholds).map(string -> Integer.valueOf(string)).collect(Collectors.toList());
	}

	public void setFinishedDate(Calendar finishedDate) {
		this.finishedDate = finishedDate;
	}

	public void setExceptions(int exceptionsCount) {
		this.exceptionsCount = exceptionsCount;
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

	public long getProjectSizeInBytes() {
		return projectSizeInBytes;
	}

	public String getGitUrl() {return gitUrl; }

	public boolean isLocal(){ return isLocal; }

	public static boolean isLocal(String gitUrl) {return !(gitUrl.startsWith("https") || gitUrl.startsWith("git")); }

	public List<Integer> getCommitCountThresholds() {return commitCountThresholds;}

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
				", commitCountThresholds=" + commitCountThresholds.toString() +
				", javaLoc=" + javaLoc +
				", numberOfProductionFiles=" + numberOfProductionFiles +
				", numberOfTestFiles=" + numberOfTestFiles +
				", productionLoc=" + productionLoc +
				", testLoc=" + testLoc +
				", exceptionsCount=" + exceptionsCount +
				", lastCommitHash='" + lastCommitHash + '\'' +
				'}';
	}
}
