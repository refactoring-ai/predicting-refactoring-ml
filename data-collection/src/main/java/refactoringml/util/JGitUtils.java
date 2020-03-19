package refactoringml.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevSort;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.util.io.DisabledOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

public class JGitUtils {
	private static final Logger log = LogManager.getLogger(JGitUtils.class);

	public static int numberOfCommits(Git git) throws GitAPIException {
		Iterable<RevCommit> commits = git.log().call();
		int count = 0;
		for(RevCommit ignored : commits) {
			count++;
		}

		return count;
	}

	public static String getHead(Git git) throws IOException {
		return git.getRepository().resolve(Constants.HEAD).getName();
	}

	public static String discoverMainBranch(Git git) throws IOException {
		return git.getRepository().getBranch();
	}

	public static String readFileFromGit (Repository repo, RevCommit commit, String filepath) throws IOException {
		try (TreeWalk walk = TreeWalk.forPath(repo, filepath, commit.getTree())) {
			if (walk != null) {
				byte[] bytes = repo.open(walk.getObjectId(0)).getBytes();
				return new String(bytes, StandardCharsets.UTF_8);
			} else {
				throw new IllegalArgumentException("No path found in " + commit.getName() + ": " + filepath);
			}
		}
	}

	public static String readFileFromGit (Repository repo, String commit, String filepath) throws IOException {
		ObjectId commitId = ObjectId.fromString(commit);
		RevWalk revWalk = new RevWalk(repo);
		RevCommit revCommit = revWalk.parseCommit( commitId );

		return readFileFromGit(repo, revCommit, filepath);
	}

	public static String extractProjectNameFromGitUrl(String gitUrl) {
		String[] splittedGitUrl = gitUrl.split("/");
		return splittedGitUrl[splittedGitUrl.length - 1].replace(".git", "");
	}


	public static Calendar getGregorianCalendar(RevCommit commit) {
		GregorianCalendar commitTime = new GregorianCalendar();
		commitTime.setTime(commit.getAuthorIdent().getWhen());
		commitTime.setTimeZone(commit.getAuthorIdent().getTimeZone());
		return commitTime;
	}

	public static RevWalk getReverseWalk(Repository repo, String mainBranch) throws IOException {
		RevWalk walk = new RevWalk(repo);
		walk.markStart(walk.parseCommit(repo.resolve(mainBranch)));
		walk.sort(RevSort.REVERSE);
		return walk;
	}

	//Generate the commit url with repository url and the commit ID
	//Local repositories without remote are formatted as: @local/repository/commit Id
	//TODO: evaluate if this pattern works for other repo hosters as well, e.g. BitBucket
	public static String generateCommitUrl(String repositoryUrl, String commitId, boolean isLocal){
		if (isLocal){
			return String.format("@local/%s/%s", repositoryUrl, commitId);
		}
		String cleanRepositoryUrl = repositoryUrl.replace(".git", "");
		return String.format("%s/commit/%s", cleanRepositoryUrl, commitId);
	}

	/**
	 * In some cases, RMiner actually returns the 'new' path, even in its 'classesBeforeRefactoringMethod'.
	 * See issue https://github.com/refactoring-ai/predicting-refactoring-ml/issues/144
	 * To counteract this, we then get a map with all old and new files, so that we can
	 * always use the old file to retrieve the version we actually extract features from.
	 *
	 * Note that key is the new file, and value is the old file.
	 * So, if we don't find the file in this map, this means it's already the old file.
	 */
	public static HashMap<String, String> getMapWithOldAndNewFiles(Repository repository, RevCommit commit) throws IOException {
		HashMap<String, String> map = new HashMap<>();

		try (DiffFormatter diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE)) {
			diffFormatter.setRepository(repository);
			diffFormatter.setDetectRenames(true);
			RevCommit commitParent = commit.getParent(0);

			List<DiffEntry> entries = diffFormatter.scan(commitParent, commit);

			entries.stream().forEach(e -> map.put(e.getNewPath(), e.getOldPath()));
		}

		return map;
	}
}