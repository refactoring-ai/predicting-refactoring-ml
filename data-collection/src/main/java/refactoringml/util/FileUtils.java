package refactoringml.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Scanner;

import static refactoringml.util.FilePathUtils.*;

public class FileUtils {

	public static String[] getAllDirs(String path) {
		return getAllDirs(path, null);
	}

	public static String[] getAllDirs(String path, String regex) {
		try {
			return Files.walk(Paths.get(path))
					.filter(Files::isDirectory)
					.filter(x -> !x.toAbsolutePath().toString().contains(".git"))
					.filter(x -> (regex!=null?x.toAbsolutePath().toString().contains(regex):true))
					.map(x -> x.toAbsolutePath().toString())
					.toArray(String[]::new);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static String[] getAllJavaFiles(String path) {
		return getAllJavaFiles(path, null);
	}

	public static String[] getAllJavaFiles(String path, String regex) {
		try {
			return Files.walk(Paths.get(path))
					.filter(Files::isRegularFile)
					.filter(x -> !x.toAbsolutePath().toString().contains(".git"))
					.filter(x -> IsJavaFile(x.toAbsolutePath().toString()))
					.filter(x -> (regex!=null?x.toAbsolutePath().toString().contains(regex):true))
					.map(x -> x.toAbsolutePath().toString())
					.toArray(String[]::new);
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	//The fileName and filePath, as both is important to be considered
	//src/test is a enforced convention for test files with gradle and maven build tools
	//every test file for ant has to end on Test, e.g. *Test.java
	public static boolean IsTestFile(String filePathName) {
		if(!FileUtils.IsJavaFile(filePathName))
			return false;

		String normalizedFilePath= enforceUnixPaths(filePathName.toLowerCase());
		return normalizedFilePath.contains("test") ||
				normalizedFilePath.contains("/test/");
	}

	//Returns true if a file is a java file.
	public static boolean IsJavaFile(String fileName){
		return fileName.toLowerCase().endsWith(".java");
	}

	public static String createTmpDir() {
		String rawTempDir = com.google.common.io.Files.createTempDir().getAbsolutePath();
		return lastSlashDir(rawTempDir);
	}

	public static void cleanTempDir (String tempDir) throws IOException {
		if(tempDir != null) {
			org.apache.commons.io.FileUtils.deleteDirectory(new File(tempDir));
		}
	}

	public static boolean newDir(String path){
		return new File(path).mkdirs();
	}

	//Write the content to a new file at the given path. Creates a new directory at the path if necessary.
	public static void writeFile(String filePath, Object content) throws FileNotFoundException {
		new File(dirsOnly(filePath)).mkdirs();
		PrintStream ps = new PrintStream(filePath);
		ps.print(content);
		ps.close();
	}

	//Write the content to a new file at the given path. Creates a new directory at the path if necessary.
	public static String readFile(String filePath) throws FileNotFoundException {
		File myObj = new File(filePath);
		Scanner myReader = new Scanner(myObj);
		String data = "";
		while (myReader.hasNextLine()) {
			data = myReader.nextLine();
		}
		myReader.close();
		return data;
	}

	public static  boolean nonClassFile(String fileName) {
		return fileName.equals("/dev/null");
	}

}