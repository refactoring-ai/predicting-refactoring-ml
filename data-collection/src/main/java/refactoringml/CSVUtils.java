package refactoringml;

public class CSVUtils {

	public static String escape(String toEscape) {
		return "\"" + toEscape.replace("\"", "\"\"") + "\"";
	}

}
