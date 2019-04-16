package refactoringml;

import org.junit.Assert;
import org.junit.Test;
import refactoringml.util.JGitUtils;

public class JGitUtilsTest {

	@Test
	public void extractProjectNameFromHttpUrl() {
		Assert.assertEquals("commons-collections", JGitUtils.extractProjectNameFromGitUrl("https://www.github.com/apache/commons-collections.git"));
	}

	@Test
	public void extractProjectNameFromGitSSHUrl() {
		Assert.assertEquals("commons-collections", JGitUtils.extractProjectNameFromGitUrl("git@github.com:apache/commons-collections.git"));
	}
}
