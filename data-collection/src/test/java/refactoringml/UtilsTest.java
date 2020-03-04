package refactoringml;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Assert;
import org.junit.Test;
import refactoringml.util.FilePathUtils;

import java.util.Random;

import static refactoringml.util.FileUtils.IsJavaFile;
import static refactoringml.util.FileUtils.IsTestFile;

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
	public void isJavaFile(){
		for(int i = 0; i < 100; i++){
			String randomString = generateRandomString();

			Assert.assertTrue(IsJavaFile(randomString + ".Java"));
			Assert.assertTrue(IsJavaFile(randomString + ".java"));

			Assert.assertFalse(IsJavaFile(randomString + "Java"));
			Assert.assertFalse(IsJavaFile(randomString + ".Jav"));
			Assert.assertFalse(IsJavaFile(randomString + ".Java.a"));
			Assert.assertFalse(IsJavaFile(randomString + "-Java"));
		}
	}

	@Test
	public void isTest(){
		for(int i = 0; i < 100; i++){
			String randomString = generateRandomString();

			//assert ant test conventions
			Assert.assertTrue(IsTestFile(randomString + "test.java"));
			Assert.assertTrue(IsTestFile(randomString + "Test.java"));

			Assert.assertFalse(IsTestFile(randomString + "*est.java"));
			Assert.assertFalse(IsTestFile(randomString + "Test.jav"));

			//assert maven and gradle test conventions
			Assert.assertTrue(IsTestFile("/test/" + randomString + ".java"));

			Assert.assertFalse(IsTestFile("/test/" + randomString + ".jav"));
			Assert.assertFalse(IsTestFile("src/" + randomString + ".java"));
		}
	}

	private String generateRandomString(){
		Random rnd = new Random();

		int length = rnd.ints(0, 10).findFirst().getAsInt();
		boolean useLetters = rnd.nextBoolean();
		boolean useNumbers = rnd.nextBoolean();
		return RandomStringUtils.random(length, useLetters, useNumbers);
	}
}