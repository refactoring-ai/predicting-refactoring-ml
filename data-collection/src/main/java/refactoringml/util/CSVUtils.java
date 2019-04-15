package refactoringml.util;

public class CSVUtils {

	public static String escape(String toEscape) {
		return "\"" + toEscape.replace("\"", "\"\"") + "\"";
	}

}
