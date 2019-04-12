package refactoringml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class CKUtils {

	public static String simplifyFullName(String fullName) {

		if(!fullName.contains("["))
			return fullName;

		String leftPart = fullName.substring(0, fullName.indexOf("["));
		String rightPart = fullName.substring(fullName.indexOf("[") + 1, fullName.length()-1);

		String[] parameters = rightPart.split(",");
		String cleanParams = Arrays.stream(parameters).map(p -> {
			if (!p.contains("."))
				return p;
			String[] splitted = p.split("\\.");
			return splitted[splitted.length - 1];
		}).collect(Collectors.joining(","));

		return String.format("%s%s%s%s", leftPart,
				parameters.length > 0 ? "[" : "",
				parameters.length > 0 ? cleanParams : "",
				parameters.length > 0 ? "]" : "");

	}
}
