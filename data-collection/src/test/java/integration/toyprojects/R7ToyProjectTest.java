package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.RefactoringCommit;
import refactoringml.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static refactoringml.util.JGitUtils.extractProjectNameFromGitUrl;

// tests related to PR #144: https://github.com/refactoring-ai/predicting-refactoring-ml/issues/144
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R7ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "https://github.com/refactoring-ai/toyrepo-r7.git";
	}


	@Test
	void t1() {
		getRefactoringCommits().forEach(x -> System.out.println(x.getRefactoringSummary()));
	}
}