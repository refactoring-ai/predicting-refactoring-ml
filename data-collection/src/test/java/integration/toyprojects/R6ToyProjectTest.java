package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.RefactoringCommit;
import refactoringml.util.FileUtils;
import refactoringml.util.JGitUtils;

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

// tests related to PR #128: https://github.com/refactoring-ai/predicting-refactoring-ml/pull/128
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R6ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "https://github.com/refactoring-ai-testing/toyrepo-r6.git";
	}

	protected boolean storeSourceCode() {
		return true;
	}

	// normal refactoring stores just one data point
	@Test
	void normalRefactoring() {

		List<RefactoringCommit> refs = getRefactoringCommits()
				.stream()
				.filter(x -> x.getCommitMetaData().getCommitId().equals("4125d9a212381d132e47c1d53b8dbb0b0a7eb7f3"))
				.collect(Collectors.toList());

		Assertions.assertEquals(1, refs.size());
		Assertions.assertEquals("Rename Variable", refs.get(0).getRefactoring());

	}

	// extract and move method returns, apparently, just one...
	@Test
	void extractAndMoveMethod() {

		List<RefactoringCommit> refs = getRefactoringCommits()
				.stream()
				.filter(x -> x.getCommitMetaData().getCommitId().equals("2da260f30c920609281c0a8a2bb019c99c9e4bf7"))
				.collect(Collectors.toList());

		Assertions.assertEquals(1, refs.size());
		Assertions.assertEquals("Extract And Move Method", refs.get(0).getRefactoring());

	}

	// move gets all the files
	@Test
	void moveMethod() {

		List<RefactoringCommit> refs = getRefactoringCommits()
				.stream()
				.filter(x -> x.getCommitMetaData().getCommitId().equals("ee94f196cf419fac2ff86fc6ae19742999ef03c2"))
				.collect(Collectors.toList());

		Assertions.assertEquals(2, refs.size());
		RefactoringCommit one = refs.get(0);
		RefactoringCommit two = refs.get(1);

		Assertions.assertEquals("Move Source Folder", one.getRefactoring());
		Assertions.assertEquals("Move Source Folder", two.getRefactoring());

		Assertions.assertEquals("a.Utils", one.getClassName());
		Assertions.assertEquals("a.A", two.getClassName());

	}

	// move and inline gets only the origin
	@Test
	void moveAndInline() {

		List<RefactoringCommit> refs = getRefactoringCommits()
				.stream()
				.filter(x -> x.getCommitMetaData().getCommitId().equals("a256a4d32479986b62d52839c18c4880e560f97b"))
				.collect(Collectors.toList());

		Assertions.assertEquals(1, refs.size());
		RefactoringCommit one = refs.get(0);

		Assertions.assertEquals("Move And Inline Method", one.getRefactoring());

		// Utils is where the method was!
		Assertions.assertEquals("a.Utils", one.getClassName());

	}

	@Test
	void assertStoreSourceCode() {

		String[] allJavaFiles = FileUtils.getAllJavaFiles(outputDir);
		Assertions.assertEquals(14, allJavaFiles.length);

		List<RefactoringCommit> rcs = getRefactoringCommits();
		for (RefactoringCommit rc : rcs) {

			List<String> files = Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + rc.getId() + "/")).collect(Collectors.toList());

			boolean found = files.stream().anyMatch(x -> x.contains("/after/"));
			Assertions.assertTrue(found);
		}

		long smallestId = rcs.stream().min(Comparator.comparingLong(x -> x.getId())).get().getId();

		// r1: rename variable
		String r1Before = Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + smallestId + "/before/A.java")).findFirst().get();
		String r1After = Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + smallestId + "/after/A.java")).findFirst().get();
		String r1BeforeSourceCode = sourceCode(r1Before);
		Assertions.assertTrue(r1BeforeSourceCode.contains("int a = 0;"));
		String r1AfterSourceCode = sourceCode(r1After);
		Assertions.assertTrue(r1AfterSourceCode.contains("int b = 0;"));

		// r2: move source folder
		// class A was isolated into its own class... the Utils class disappeared from A.java
		String r2Before = Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + (smallestId+1) + "/before/A.java")).findFirst().get();
		String r2After = Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + (smallestId+1) + "/after/A.java")).findFirst().get();
		String r2BeforeSourceCode = sourceCode(r2Before);
		Assertions.assertTrue(r2BeforeSourceCode.contains("class Utils"));
		String r2AfterSourceCode = sourceCode(r2After);
		Assertions.assertFalse(r2AfterSourceCode.contains("class Utils"));

		// r3: move source folder
		// connected to the previous one, but now it's the Utils class
		String r3Before = Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + (smallestId+2) + "/before/A.java")).findFirst().get();
		String r3After = Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + (smallestId+2) + "/after/Utils.java")).findFirst().get();
		String r3BeforeSourceCode = sourceCode(r3Before);
		Assertions.assertTrue(r3BeforeSourceCode.contains("class Utils"));
		String r3AfterSourceCode = sourceCode(r3After);
		Assertions.assertTrue(r3AfterSourceCode.contains("class Utils"));
		Assertions.assertEquals(0, Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + (smallestId+2) + "/before/Utils.java")).count());

		// r4: Extract and Move Method
		String r4Before = Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + (smallestId+3) + "/before/A.java")).findFirst().get();
		String r4After = Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + (smallestId+3) + "/after/A.java")).findFirst().get();
		String r4After2 = Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + (smallestId+3) + "/after/Utils.java")).findFirst().get();
		String r4BeforeSourceCode = sourceCode(r4Before);
		Assertions.assertTrue(r4BeforeSourceCode.contains("int c = a + b"));
		String r4AfterSourceCode = sourceCode(r4After);
		Assertions.assertTrue(r4AfterSourceCode.contains("new Utils().m2copy();"));
		String r4AfterSourceCode2 = sourceCode(r4After2);
		Assertions.assertTrue(r4AfterSourceCode2.contains("public void m2copy"));
		Assertions.assertEquals(0, Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + (smallestId+3) + "/before/Utils.java")).count());

		// r5: move and inline method
		String r5Before = Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + (smallestId+4) + "/before/A.java")).findFirst().get();
		String r5Before2 = Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + (smallestId+4) + "/before/Utils.java")).findFirst().get();
		String r5After = Arrays.stream(allJavaFiles).filter(x -> x.contains("/" + (smallestId+4) + "/after/A.java")).findFirst().get();

		String r5BeforeSourceCode = sourceCode(r5Before);
		Assertions.assertTrue(r5BeforeSourceCode.contains("new Utils().m2copy();"));
		String r5BeforeSourceCode2 = sourceCode(r5Before2);
		Assertions.assertTrue(r5BeforeSourceCode2.contains("void m2copy()"));
		String r5AfterSourceCode = sourceCode(r5After);
		Assertions.assertTrue(r5AfterSourceCode.contains("int c = a + b"));

	}

	private String sourceCode(String file) {
		try {
			return new String(Files.readAllBytes(Paths.get(file)), StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private Repository getRepository() throws IOException {
		String projectName = extractProjectNameFromGitUrl(getRepo());
		String repoLocalDir = "repos/" + projectName;
		Git git = Git.open(new File(repoLocalDir));
		return git.getRepository();
	}

}