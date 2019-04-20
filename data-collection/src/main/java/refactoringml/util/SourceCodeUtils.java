package refactoringml.util;

public class SourceCodeUtils {
	public static String removeComments(String sourceCode) {
		final JavaCommentRemover jcr = new JavaCommentRemover();

		return jcr.removeComment(sourceCode);
	}
}
