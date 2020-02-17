package refactoringml.util;

import java.nio.file.Files;
import java.nio.file.Paths;

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

	//Returns true if a file is a java class.
	public static boolean IsJavaFile(String fileName){
		return fileName.toLowerCase().endsWith("java");
	}
}