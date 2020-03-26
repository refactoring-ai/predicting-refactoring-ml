package refactoringml.util;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
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
import org.refactoringminer.api.Refactoring;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static refactoringml.util.RefactoringUtils.refactoredFilesAndClasses;

public class JGitUtils {
	private static final Logger log = LogManager.getLogger(JGitUtils.class);
	private static DiffFormatter diffFormatter;

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
	public static HashMap<String, String> getMapWithOldAndNewFiles(List<DiffEntry> entries) {
		HashMap<String, String> map = new HashMap<>();
		entries.forEach(e -> map.put(e.getNewPath(), e.getOldPath()));
		return map;
	}

	//get a diff-formater for the given repository
	public static DiffFormatter getDiffFormater(){
		return diffFormatter;
	}

	public static void createDiffFormatter(Repository repository){
		diffFormatter = new DiffFormatter(DisabledOutputStream.INSTANCE);
		diffFormatter.setRepository(repository);
		diffFormatter.setDetectRenames(true);
	}

	//Calculate the JGit diff-entries for the given repository and commit
	public static List<DiffEntry> calculateDiffEntries(RevCommit commit) throws IOException {
		RevCommit commitParent = commit.getParentCount() == 0 ? null : commit.getParent(0);
		return getDiffFormater().scan(commitParent, commit);
	}

	//all renames detected by JGit
	// returns the filename before and after the rename
	public static Set<ImmutablePair<String, String>> getJGitRenames(List<DiffEntry> entries){
		Set<ImmutablePair<String, String>> renamedClassesJGit = null;
		if(entries  != null) {
			Set<DiffEntry> renameEntries = entries.stream()
					.filter(diffEntry -> diffEntry.getChangeType() == DiffEntry.ChangeType.RENAME)
					.collect(Collectors.toSet());
			renamedClassesJGit = renameEntries.stream()
					.map(JGitUtils::getClassNames)
					.collect(Collectors.toSet());
		}
		return renamedClassesJGit;
	}

	//all rename class files detected by refactoring miner
	// returns the filename before and after the rename
	public static Set<ImmutablePair<String, String>> getRefactoringMinerRenames(List<Refactoring> refactoringsToProcess){
		Set<ImmutablePair<String, String>> renamedClassesRMiner = null;
		if(refactoringsToProcess != null){
			Set<Refactoring> renameRefactorings = refactoringsToProcess.stream()
					.filter(RefactoringUtils::isClassRename)
					.collect(Collectors.toSet());
			renamedClassesRMiner = renameRefactorings.stream()
					.map(JGitUtils::getClassNames)
					.flatMap(Collection::stream)
					.collect(Collectors.toSet());
		}
		return renamedClassesRMiner;
	}

	//Get the file names of all refactored classes before and after the refactoring
	private static Set<ImmutablePair<String, String>> getClassNames(Refactoring refactoring){
		Set<ImmutablePair<String, String>> before = refactoredFilesAndClasses(refactoring, refactoring.getInvolvedClassesBeforeRefactoring());
		Set<ImmutablePair<String, String>> after = refactoredFilesAndClasses(refactoring, refactoring.getInvolvedClassesAfterRefactoring());
		if(before.size() != after.size())
			throw new IllegalStateException();

		Set<ImmutablePair<String, String>> results = new HashSet<>();
		for(int i = 0; i < before.size(); i++){
			results.add(new ImmutablePair<>(before.iterator().next().getLeft(), after.iterator().next().getLeft()));
		}
		return results;
	}

	//get the file names of the affected class from the diffentry
	private static ImmutablePair<String, String> getClassNames(DiffEntry entry){
		return new ImmutablePair<>(entry.getOldPath(), entry.getNewPath());
	}
}