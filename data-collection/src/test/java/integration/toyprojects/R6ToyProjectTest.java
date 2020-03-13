package integration.toyprojects;

import integration.IntegrationBaseTest;
import org.junit.Assert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import refactoringml.db.RefactoringCommit;
import refactoringml.db.StableCommit;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// tests related to PR #128: https://github.com/refactoring-ai/predicting-refactoring-ml/pull/128
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class R6ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "https://github.com/refactoring-ai/toyrepo-r6.git";
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

}