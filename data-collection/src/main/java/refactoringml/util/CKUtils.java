package refactoringml.util;

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

		rightPart = cleanGenerics(rightPart);

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

	// we replace the $ that appears in the name of a class when there is a subclass, e.g., A$B becomes A.B
	// we remove generics, e.g., A<B, C> becomes A
	// Why? Because the way JDT resolves (and stringuifies) class names in TypeDeclarations
	// is different from the way it resolves (and stringuifies) in MethodBinding...
	private static String cleanGenerics(String clazzName) {
		return clazzName.replaceAll("\\$", "\\.").replaceAll("<.*>", "").trim();
	}

	public static String cleanClassName(String clazzName) {
		return clazzName.replaceAll("\\$", "\\.");
	}

	/*
	Only works with the class type from ck.
	 */
	public static boolean evaluateSubclass(String classType) { return classType.equals("subclass"); }
}
