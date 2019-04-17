package refactoringml.util;

import java.io.File;

public class FilePathUtils {
	public static String classFromFileName (String fileName) {
		fileName = fileName.replace("\\", "/");
		String[] splittedFile = fileName.split("/");
		return splittedFile[splittedFile.length-1].replace(".java", "");
	}

	public static String classFromFullName (String refactoredClass) {
		String[] split = refactoredClass.split("\\.");
		return split[split.length-1];
	}

	public static String dirsOnly (String fileName) {
		return new File(fileName).getParent();
	}

	public static String fileNameOnly (String fileName) {
		return new File(fileName).getName();
	}

	public static boolean createAllDirs (String base, String fileName) {
		return new File(lastSlashDir(base) + dirsOnly(fileName)).mkdirs();
	}

	public static String lastSlashDir (String path) {
		return path + (path.endsWith("/")?"":"/");
	}
}
