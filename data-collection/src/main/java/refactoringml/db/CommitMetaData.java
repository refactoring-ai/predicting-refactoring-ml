package refactoringml.db;

import org.eclipse.jgit.revwalk.RevCommit;
import org.hibernate.annotations.Type;
import refactoringml.ProcessMetric;
import refactoringml.util.JGitUtils;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Calendar;

public class CommitMetaData {
    //use the unique commit hash to relate from Yes and No to this one
    @Id
    private String commitId;

    @Type(type="text")
    //original commit message
    private String commitMessage;
    //url to the commit on its remote repository, e.g. https://github.com/mauricioaniche/predicting-refactoring-ml/commit/36016e4023cb74cd1076dbd33e0d7a73a6a61993
    private String commitUrl;
    //Date this commit was made
    @Temporal(TemporalType.TIMESTAMP)
    private Calendar commitDate;
    //id of the parent commit, if none exists:
    private String parentCommit;

    @Deprecated // hibernate purposes
    public CommitMetaData() {}

    public CommitMetaData(RevCommit commit, Project project){
        this.commitId = commit.getName();
        this.commitDate = JGitUtils.getGregorianCalendar(commit);
        this.commitMessage = commit.getFullMessage().trim();
        this.commitUrl = JGitUtils.generateCommitUrl(project.getGitUrl(), commitId, project.isLocal());
        this.parentCommit = commit.getParent(0).getName();
    }

    public CommitMetaData(ProcessMetric clazz, Project project){
        this.commitId = clazz.getBaseCommitForNonRefactoring();
        this.commitDate = clazz.getBaseCommitDateForNonRefactoring();
        this.commitMessage =  clazz.getBaseCommitMessageForNonRefactoring().trim();
        this.commitUrl = JGitUtils.generateCommitUrl(project.getGitUrl(), commitId, project.isLocal());
        //TODO, change this when integrating the one visit refactoring
        this.parentCommit = "";
    }

    public String getCommitUrl (){return commitUrl;}

    public String getCommit() {return commitId; }

    public String getCommitMessage (){return commitMessage;}

    @Override
    public String toString() {
        return "CommitMetaData{" +
                "commit=" + commitId +
                ", commitDate=" + commitDate +
                ", commitMessage=" + commitMessage +
                ", commitUrl=" + commitUrl +
                ", parentCommit='" + parentCommit + '\'' +
                '}';
    }
}