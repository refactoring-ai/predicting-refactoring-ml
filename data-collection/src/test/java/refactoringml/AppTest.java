package refactoringml;

import com.google.common.io.Files;
import org.apache.commons.io.IOUtils;
import org.eclipse.jgit.api.Git;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.*;

public class AppTest {

	private String filesStoragePath;

	private ByteArrayOutputStream notRefactoredClassLevelFilePath;

	private ByteArrayOutputStream notRefactoredMethodLevelFilePath;
	private ByteArrayOutputStream notRefactoredVariableLevelFilePath;
	private ByteArrayOutputStream refactoredFilePath;
	private ByteArrayOutputStream refactoredClassLevelFilePath;
	private ByteArrayOutputStream refactoredMethodLevelFilePath;
	private ByteArrayOutputStream refactoredVariableLevelFilePath;
	private ByteArrayOutputStream processMetricsOutputFile;
	private ByteArrayOutputStream projectInfoFilePath;
	private ByteArrayOutputStream notRefactoredFieldLevelFilePath;
	private ByteArrayOutputStream refactoredFieldLevelFilePath;


	@Before
	public void setUp() {
		filesStoragePath = Files.createTempDir().getAbsolutePath();
		filesStoragePath = FilePathUtils.lastSlashDir(filesStoragePath);

		notRefactoredClassLevelFilePath = new ByteArrayOutputStream();
		notRefactoredMethodLevelFilePath = new ByteArrayOutputStream();
		notRefactoredVariableLevelFilePath = new ByteArrayOutputStream();
		notRefactoredFieldLevelFilePath = new ByteArrayOutputStream();

		refactoredFilePath = new ByteArrayOutputStream();
		refactoredClassLevelFilePath = new ByteArrayOutputStream();
		refactoredMethodLevelFilePath = new ByteArrayOutputStream();
		refactoredVariableLevelFilePath = new ByteArrayOutputStream();
		refactoredFieldLevelFilePath = new ByteArrayOutputStream();

		processMetricsOutputFile = new ByteArrayOutputStream();
		projectInfoFilePath = new ByteArrayOutputStream();
	}

	/**
	 * repo 1 has 1 refactoring (a rename method), but no examples of no refactoring
	 */
	@Test
	public void repo1() throws Exception {

		run("repo-1");

		// assert the metrics at class and method level
		Assert.assertEquals("dataset,project,before,after,path,class,refactoring,method,variable\n" +
						"test,repo-1,d95b5900ad34499ad9e5c28aea8bf04565f5c863,393178e92b87b8e3e582bb84a8f16a4b5b77caec,org/Dog.java,org.Dog,Rename Method,barkbark,\n",
				output(refactoredFilePath));

		Assert.assertEquals("dataset,project,before,after,path,class,type,cbo,wmc,rfc,lcom,totalMethods,staticMethods,publicMethods,privateMethods,protectedMethods,defaultMethods,abstractMethods,finalMethods,synchronizedMethods,totalFields,staticFields,publicFields,privateFields,protectedFields,defaultFields,finalFields,synchronizedFields,nosi,loc,returnQty,loopQty,comparisonsQty,tryCatchQty,parenthesizedExpsQty,stringLiteralsQty,numbersQty,assignmentsQty,mathOperationsQty,variablesQty,maxNestedBlocks,anonymousClassesQty,subClassesQty,lambdasQty,uniqueWordsQty\n" +
				"test,repo-1,d95b5900ad34499ad9e5c28aea8bf04565f5c863,393178e92b87b8e3e582bb84a8f16a4b5b77caec,org/Dog.java,org.Dog,class,0,1,1,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,2\n", output(refactoredClassLevelFilePath));
		Assert.assertEquals("dataset,project,before,after,path,class,method,simplemethodname,line,cbo,wmc,rfc,loc,returns,variables,parameters,startLine,loopQty,comparisonsQty,tryCatchQty,parenthesizedExpsQty,stringLiteralsQty,numbersQty,assignmentsQty,mathOperationsQty,maxNestedBlocks,anonymousClassesQty,subClassesQty,lambdasQty,uniqueWordsQty\n" +
				"test,repo-1,d95b5900ad34499ad9e5c28aea8bf04565f5c863,393178e92b87b8e3e582bb84a8f16a4b5b77caec,org/Dog.java,org.Dog,barkbark/0,barkbark,5,0,1,1,3,0,0,0,5,0,0,0,0,1,0,0,0,0,0,0,0,1\n", output(refactoredMethodLevelFilePath));

		Assert.assertTrue(new File(filesStoragePath + "d95b5900ad34499ad9e5c28aea8bf04565f5c863/before-refactoring/org/Dog.java").exists());
		Assert.assertTrue(new File(filesStoragePath + "d95b5900ad34499ad9e5c28aea8bf04565f5c863/after-refactoring/org/Dog.java").exists());

		// assert java files that will be used by the DL later
		String expectedBefore = "package org;\n" +
				"\n" +
				"class Dog {\n" +
				"\n" +
				"  public void barkbark() {\n" +
				"\tSystem.out.println(\"au\");\n" +
				"  }\n" +
				"\n" +
				"\n" +
				"}\n\n";

		Assert.assertEquals(expectedBefore, sourceCodeIn(filesStoragePath + "d95b5900ad34499ad9e5c28aea8bf04565f5c863/before/org/Dog.java"));

		String expectedAfter = "package org;\n" +
				"\n" +
				"class Dog {\n" +
				"\n" +
				"  public void bark() {\n" +
				"\tSystem.out.println(\"au\");\n" +
				"  }\n" +
				"\n" +
				"\n" +
				"}\n\n";

		Assert.assertEquals(expectedAfter, sourceCodeIn(filesStoragePath + "d95b5900ad34499ad9e5c28aea8bf04565f5c863/after/org/Dog.java"));

		// assert that there are no non refactored methods here
		Assert.assertEquals("dataset,project,commit,path,class,type,cbo,wmc,rfc,lcom,totalMethods,staticMethods,publicMethods,privateMethods,protectedMethods,defaultMethods,abstractMethods,finalMethods,synchronizedMethods,totalFields,staticFields,publicFields,privateFields,protectedFields,defaultFields,finalFields,synchronizedFields,nosi,loc,returnQty,loopQty,comparisonsQty,tryCatchQty,parenthesizedExpsQty,stringLiteralsQty,numbersQty,assignmentsQty,mathOperationsQty,variablesQty,maxNestedBlocks,anonymousClassesQty,subClassesQty,lambdasQty,uniqueWordsQty\n",
				output(notRefactoredClassLevelFilePath));

	}

	/**
	 * repo2 has no refactorings, but has two classes that serve as an example of non refactoring
	 */
	@Test
	public void repo2() throws Exception {
		run("repo-2");

		Assert.assertEquals("dataset,project,commit,path,class,type,cbo,wmc,rfc,lcom,totalMethods,staticMethods,publicMethods,privateMethods,protectedMethods,defaultMethods,abstractMethods,finalMethods,synchronizedMethods,totalFields,staticFields,publicFields,privateFields,protectedFields,defaultFields,finalFields,synchronizedFields,nosi,loc,returnQty,loopQty,comparisonsQty,tryCatchQty,parenthesizedExpsQty,stringLiteralsQty,numbersQty,assignmentsQty,mathOperationsQty,variablesQty,maxNestedBlocks,anonymousClassesQty,subClassesQty,lambdasQty,uniqueWordsQty\n" +
						"test,repo-2,cce2070ffb64bf7fe8adcc8c228fb30d6b053efe,org/Dog.java,org.Dog,class,0,1,1,0,1,0,1,0,0,0,0,0,0,1,0,1,0,0,0,0,0,0,7,0,0,0,0,0,1,0,0,0,0,1,0,0,0,0,4\n" +
						"test,repo-2,cce2070ffb64bf7fe8adcc8c228fb30d6b053efe,org/Cat.java,org.Cat,class,0,1,1,0,1,0,1,0,0,0,0,0,0,2,0,2,0,0,0,0,0,0,8,0,0,0,0,0,1,0,0,0,0,2,0,0,0,0,7\n",
				output(notRefactoredClassLevelFilePath));

		Assert.assertEquals("dataset,project,before,after,path,class,refactoring,method,variable\n",
				output(refactoredFilePath));


		String dog = "package org;\n" +
				"\n" +
				"class Dog {\n" +
				"\t\n" +
				"\n" +
				"\tpublic int age;\n" +
				"\n" +
				"\tpublic void bark() {\n" +
				"\t\tSystem.out.println(\"au au!\");\n" +
				"\t}\n" +
				"}";
		Assert.assertEquals(dog, sourceCodeIn(filesStoragePath + "cce2070ffb64bf7fe8adcc8c228fb30d6b053efe/not-refactored/org/Dog.java"));

		String cat = "package org;\n" +
				"\n" +
				"class Cat {\n" +
				"\t\n" +
				"\n" +
				"\tpublic int age;\n" +
				"\tpublic int lifes;\n" +
				"\n" +
				"\tpublic void destroyTheCouch() {\n" +
				"\t\tSystem.out.println(\"crack crack!\");\n" +
				"\t}\n" +
				"}";
		Assert.assertEquals(cat, sourceCodeIn(filesStoragePath + "cce2070ffb64bf7fe8adcc8c228fb30d6b053efe/not-refactored/org/Cat.java"));

	}

	/**
	 * This repo3 contains 3 refactorings (2 happening at the same time)
	 * and two classes (that suffered refactoring in the past) that also serve as an example of non refactoring later on
	 */
	@Test
	public void repo3() throws Exception {
		run("repo-3");

		Assert.assertEquals("dataset,project,before,after,path,class,refactoring,method,variable\n" +
				"test,repo-3,289c883b93b3e16ef41a40ce0d67fd1f6ecc5c58,6f9b6a3b83ea6b8b009cb3083c54d72a34228cee,org/org2/Bird.java,org.org2.Bird,Rename Method,fly,\n" +
				"test,repo-3,a199c471529081160de58a447772568d685265ef,ff578595f4109128e3ca81f4a7f98819e628a54a,org/org2/Cat.java,org.org2.Cat,Rename Method,miau,\n" +
				"test,repo-3,a199c471529081160de58a447772568d685265ef,ff578595f4109128e3ca81f4a7f98819e628a54a,org/org2/Dog.java,org.org2.Dog,Rename Method,bark,\n", output(refactoredFilePath));

		Assert.assertEquals("dataset,project,before,after,path,class,type,cbo,wmc,rfc,lcom,totalMethods,staticMethods,publicMethods,privateMethods,protectedMethods,defaultMethods,abstractMethods,finalMethods,synchronizedMethods,totalFields,staticFields,publicFields,privateFields,protectedFields,defaultFields,finalFields,synchronizedFields,nosi,loc,returnQty,loopQty,comparisonsQty,tryCatchQty,parenthesizedExpsQty,stringLiteralsQty,numbersQty,assignmentsQty,mathOperationsQty,variablesQty,maxNestedBlocks,anonymousClassesQty,subClassesQty,lambdasQty,uniqueWordsQty\n" +
				"test,repo-3,289c883b93b3e16ef41a40ce0d67fd1f6ecc5c58,6f9b6a3b83ea6b8b009cb3083c54d72a34228cee,org/org2/Bird.java,org.org2.Bird,class,1,2,2,1,2,0,2,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,10,0,0,0,0,0,2,0,0,0,0,0,0,0,0,0,6\n" +
				"test,repo-3,a199c471529081160de58a447772568d685265ef,ff578595f4109128e3ca81f4a7f98819e628a54a,org/org2/Cat.java,org.org2.Cat,class,0,1,1,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,2\n" +
				"test,repo-3,a199c471529081160de58a447772568d685265ef,ff578595f4109128e3ca81f4a7f98819e628a54a,org/org2/Dog.java,org.org2.Dog,class,0,1,1,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,3\n", output(refactoredClassLevelFilePath));

		Assert.assertEquals("dataset,project,before,after,path,class,method,simplemethodname,line,cbo,wmc,rfc,loc,returns,variables,parameters,startLine,loopQty,comparisonsQty,tryCatchQty,parenthesizedExpsQty,stringLiteralsQty,numbersQty,assignmentsQty,mathOperationsQty,maxNestedBlocks,anonymousClassesQty,subClassesQty,lambdasQty,uniqueWordsQty\n" +
				"test,repo-3,289c883b93b3e16ef41a40ce0d67fd1f6ecc5c58,6f9b6a3b83ea6b8b009cb3083c54d72a34228cee,org/org2/Bird.java,org.org2.Bird,fly/0,fly,5,0,1,1,3,0,0,0,5,0,0,0,0,1,0,0,0,0,0,0,0,1\n" +
				"test,repo-3,289c883b93b3e16ef41a40ce0d67fd1f6ecc5c58,6f9b6a3b83ea6b8b009cb3083c54d72a34228cee,org/org2/Bird.java,org.org2.Bird,die/0,die,9,1,1,1,4,0,0,0,9,0,0,0,0,1,0,0,0,0,0,0,0,5\n" +
				"test,repo-3,a199c471529081160de58a447772568d685265ef,ff578595f4109128e3ca81f4a7f98819e628a54a,org/org2/Cat.java,org.org2.Cat,miau/0,miau,5,0,1,1,3,0,0,0,5,0,0,0,0,1,0,0,0,0,0,0,0,1\n" +
				"test,repo-3,a199c471529081160de58a447772568d685265ef,ff578595f4109128e3ca81f4a7f98819e628a54a,org/org2/Dog.java,org.org2.Dog,bark/0,bark,5,0,1,1,3,0,0,0,5,0,0,0,0,1,0,0,0,0,0,0,0,2\n", output(refactoredMethodLevelFilePath));

		String notRefactored = "dataset,project,commit,path,class,type,cbo,wmc,rfc,lcom,totalMethods,staticMethods,publicMethods,privateMethods,protectedMethods,defaultMethods,abstractMethods,finalMethods,synchronizedMethods,totalFields,staticFields,publicFields,privateFields,protectedFields,defaultFields,finalFields,synchronizedFields,nosi,loc,returnQty,loopQty,comparisonsQty,tryCatchQty,parenthesizedExpsQty,stringLiteralsQty,numbersQty,assignmentsQty,mathOperationsQty,variablesQty,maxNestedBlocks,anonymousClassesQty,subClassesQty,lambdasQty,uniqueWordsQty\n" +
				"test,repo-3,a199c471529081160de58a447772568d685265ef,org/org2/Cat.java,org.org2.Cat,class,0,1,1,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,7,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,3\n" +
				"test,repo-3,a199c471529081160de58a447772568d685265ef,org/org2/Dog.java,org.org2.Dog,class,0,1,1,0,1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,6,0,0,0,0,0,1,0,0,0,0,0,0,0,0,0,3\n";
		Assert.assertEquals(notRefactored, output(notRefactoredClassLevelFilePath));

		Assert.assertEquals("dataset,project,commit,path,class,method,simplemethodname,line,cbo,wmc,rfc,loc,returns,variables,parameters,startLine,loopQty,comparisonsQty,tryCatchQty,parenthesizedExpsQty,stringLiteralsQty,numbersQty,assignmentsQty,mathOperationsQty,maxNestedBlocks,anonymousClassesQty,subClassesQty,lambdasQty,uniqueWordsQty\n" +
				"test,repo-3,a199c471529081160de58a447772568d685265ef,org/org2/Cat.java,org.org2.Cat,miaumiau/0,miaumiau,5,0,1,1,3,0,0,0,5,0,0,0,0,1,0,0,0,0,0,0,0,2\n" +
				"test,repo-3,a199c471529081160de58a447772568d685265ef,org/org2/Dog.java,org.org2.Dog,barkbark/0,barkbark,5,0,1,1,3,0,0,0,5,0,0,0,0,1,0,0,0,0,0,0,0,2\n", output(notRefactoredMethodLevelFilePath));

		String birdBefore = "package org.org2;\n" +
				"\n" +
				"class Bird {\n" +
				"\t\n" +
				"\tpublic void fly() {\n" +
				"\t\tSystem.out.println(\"flyyyyy!\");\n" +
				"\t}\n" +
				"\n" +
				"\tpublic void die() {\n" +
				"\t\tthrow new RuntimeException(\"that's mean!\");\n" +
				"\t\tfly();\n" +
				"\t\t\n" +
				"\t}\n" +
				"}";


		Assert.assertEquals(birdBefore, sourceCodeIn(filesStoragePath + "289c883b93b3e16ef41a40ce0d67fd1f6ecc5c58/before-refactoring/org/org2/Bird.java"));

		String birdAfter = "package org.org2;\n" +
				"\n" +
				"class Bird {\n" +
				"\t\n" +
				"\tpublic void flyfly() {\n" +
				"\t\tSystem.out.println(\"flyyyyy!\");\n" +
				"\t}\n" +
				"\n" +
				"\tpublic void die() {\n" +
				"\t\tthrow new RuntimeException(\"that's mean!\");\n" +
				"\t\tflyfly();\n" +
				"\n" +
				"\t}\n" +
				"}";
		Assert.assertEquals(birdAfter, sourceCodeIn(filesStoragePath + "289c883b93b3e16ef41a40ce0d67fd1f6ecc5c58/after-refactoring/org/org2/Bird.java"));

		String nrDog = "package org.org2;\n" +
				"\n" +
				"class Dog {\n" +
				"\t\n" +
				"\tpublic void barkbark() {\n" +
				"\t\tSystem.out.println(\"au au!\");\n" +
				"\t}\n" +
				"}";
		Assert.assertEquals(nrDog, sourceCodeIn(filesStoragePath + "a199c471529081160de58a447772568d685265ef/not-refactored/org/org2/Dog.java"));

		String nrCat = "package org.org2;\n" +
				"\n" +
				"class Cat {\n" +
				"\t\n" +
				"\tpublic void miaumiau() {\n" +
				"\t\tSystem.out.println(\"miau miau!\");\n" +
				"\n" +
				"\t\t// comment\n" +
				"\t}\n" +
				"}";
		Assert.assertEquals(nrCat, sourceCodeIn(filesStoragePath + "a199c471529081160de58a447772568d685265ef/not-refactored/org/org2/Cat.java"));
	}

	/**
	 * this repo contains two refactoring on the same commit, but in different folders.
	 *
	 * this test catches a bug we had before: when two refactored files were in different dirs, our tool couldn't create all required dirs,
	 * and then, an exception was happening
	 * @throws Exception
	 */
	@Test
	public void repo4() throws Exception {
		run("repo-4");

		Assert.assertTrue(sourceCodeIn(filesStoragePath + "f9278326ddc87849cbc17d02caca5b009252de80/before-refactoring/org/p1/Dog.java").length() > 0);
		Assert.assertTrue(sourceCodeIn(filesStoragePath + "f9278326ddc87849cbc17d02caca5b009252de80/after-refactoring/org/p1/Dog.java").length() > 0);
		Assert.assertTrue(sourceCodeIn(filesStoragePath + "f9278326ddc87849cbc17d02caca5b009252de80/before-refactoring/org/p2/Cat.java").length() > 0);
		Assert.assertTrue(sourceCodeIn(filesStoragePath + "f9278326ddc87849cbc17d02caca5b009252de80/after-refactoring/org/p2/Cat.java").length() > 0);
	}

	private String sourceCodeIn (String file) throws IOException {
		return IOUtils.toString(new FileInputStream(new File(file)), "UTF-8");
	}

	private void run (String repo) throws Exception {
		String gitUrl = AppTest.class.getResource("/").getPath() + "../../repos/" + repo;


		Git.open(new File(gitUrl)).checkout().setName("master").call();

		new App("test", gitUrl, gitUrl,
				new PrintStream(notRefactoredClassLevelFilePath),
				new PrintStream(notRefactoredMethodLevelFilePath),
				new PrintStream(notRefactoredVariableLevelFilePath),
				new PrintStream(notRefactoredFieldLevelFilePath),

				filesStoragePath,

				new PrintStream(refactoredFilePath),
				new PrintStream(refactoredClassLevelFilePath),
				new PrintStream(refactoredMethodLevelFilePath),
				new PrintStream(refactoredVariableLevelFilePath),
				new PrintStream(refactoredFieldLevelFilePath),

				new PrintStream(processMetricsOutputFile),
				new PrintStream(projectInfoFilePath), 10).run();

	}

	private String output(ByteArrayOutputStream output) {
		return new String(output.toByteArray());
	}

}
