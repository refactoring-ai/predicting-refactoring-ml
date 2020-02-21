package refactoringml;

import org.junit.Assert;
import org.junit.Test;
import refactoringml.util.FilePathUtils;

public class UtilsTest {

	@Test
	public void classFromFileName() {
		Assert.assertEquals("File", FilePathUtils.classFromFileName("/some/dir/File.java"));
		Assert.assertEquals("File", FilePathUtils.classFromFileName("c:\\some\\dir\\File.java"));
		Assert.assertEquals("File", FilePathUtils.classFromFileName("/File.java"));

	}

	@Test
	public void classFromFullName() {
		Assert.assertEquals("File", FilePathUtils.classFromFullName(".some.pack.File"));
		Assert.assertEquals("File", FilePathUtils.classFromFullName("File"));
	}

	@Test
	public void calculateLinesAdded(){

	}

	@Test
	public void calculateLinesDeleted(){

	}
}
