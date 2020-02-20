package refactoringml.util;

import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import refactoringml.db.ClassMetric;
import refactoringml.db.MethodMetric;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static refactoringml.util.RefactoringUtils.cleanMethodName;

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

	//Extract the class metrics from a CKClassResult
	public static ClassMetric extractClassMetrics(CKClassResult ck){
		return new ClassMetric(
				CKUtils.evaluateSubclass(ck.getType()),
				ck.getCbo(),
				ck.getWmc(),
				ck.getRfc(),
				ck.getLcom(),
				ck.getNumberOfMethods(),
				ck.getNumberOfStaticMethods(),
				ck.getNumberOfPublicMethods(),
				ck.getNumberOfPrivateMethods(),
				ck.getNumberOfProtectedMethods(),
				ck.getNumberOfDefaultMethods(),
				ck.getNumberOfAbstractMethods(),
				ck.getNumberOfFinalMethods(),
				ck.getNumberOfSynchronizedMethods(),
				ck.getNumberOfFields(),
				ck.getNumberOfStaticFields(),
				ck.getNumberOfPublicFields(),
				ck.getNumberOfPrivateFields(),
				ck.getNumberOfProtectedFields(),
				ck.getNumberOfDefaultFields(),
				ck.getNumberOfFinalFields(),
				ck.getNumberOfSynchronizedFields(),
				ck.getNosi(),
				ck.getLoc(),
				ck.getReturnQty(),
				ck.getLoopQty(),
				ck.getComparisonsQty(),
				ck.getTryCatchQty(),
				ck.getParenthesizedExpsQty(),
				ck.getStringLiteralsQty(),
				ck.getNumbersQty(),
				ck.getAssignmentsQty(),
				ck.getMathOperationsQty(),
				ck.getVariablesQty(),
				ck.getMaxNestedBlocks(),
				ck.getAnonymousClassesQty(),
				ck.getSubClassesQty(),
				ck.getLambdasQty(),
				ck.getUniqueWordsQty());
	}

	//Extract the method metrics from a CKMethodResult
	public static MethodMetric extractMethodMetrics(CKMethodResult ckMethodResult){
		return new MethodMetric(
				CKUtils.simplifyFullName(ckMethodResult.getMethodName()),
				cleanMethodName(ckMethodResult.getMethodName()),
				ckMethodResult.getStartLine(),
				ckMethodResult.getCbo(),
				ckMethodResult.getWmc(),
				ckMethodResult.getRfc(),
				ckMethodResult.getLoc(),
				ckMethodResult.getReturnQty(),
				ckMethodResult.getVariablesQty(),
				ckMethodResult.getParametersQty(),
				ckMethodResult.getLoopQty(),
				ckMethodResult.getComparisonsQty(),
				ckMethodResult.getTryCatchQty(),
				ckMethodResult.getParenthesizedExpsQty(),
				ckMethodResult.getStringLiteralsQty(),
				ckMethodResult.getNumbersQty(),
				ckMethodResult.getAssignmentsQty(),
				ckMethodResult.getMathOperationsQty(),
				ckMethodResult.getMaxNestedBlocks(),
				ckMethodResult.getAnonymousClassesQty(),
				ckMethodResult.getSubClassesQty(),
				ckMethodResult.getLambdasQty(),
				ckMethodResult.getUniqueWordsQty()
		);
	}
}