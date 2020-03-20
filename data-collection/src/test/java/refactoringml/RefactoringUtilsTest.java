package refactoringml;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import refactoringml.util.RefactoringUtils;

public class RefactoringUtilsTest {

	@Test
	void identifyAnonymousClassesOrMethods() {
		Assertions.assertTrue(RefactoringUtils.isAnonymousClass("a.ColumnQueryImpl.execute.result.doExecute"));
		Assertions.assertTrue(RefactoringUtils.isAnonymousClass("a.b.c.d.ColumnQueryImpl.execute.result.doExecute"));
		Assertions.assertTrue(RefactoringUtils.isAnonymousClass("a.b.c.d.ColumnQueryImpl.A.execute.result.doExecute"));
	}

	@Test
	void identifyRegularClasses() {
		Assertions.assertFalse(RefactoringUtils.isAnonymousClass("a.ColumnQueryImpl"));
		Assertions.assertFalse(RefactoringUtils.isAnonymousClass("a.b.c.d.ColumnQueryImpl"));
		Assertions.assertFalse(RefactoringUtils.isAnonymousClass("a.b.c.d.ColumnQueryImpl.A"));
	}
}
