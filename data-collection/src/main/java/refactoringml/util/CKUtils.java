package refactoringml.util;

import com.github.mauricioaniche.ck.CK;
import com.github.mauricioaniche.ck.CKClassResult;
import com.github.mauricioaniche.ck.CKMethodResult;
import com.github.mauricioaniche.ck.CKNotifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import refactoringml.db.ClassMetric;
import refactoringml.db.MethodMetric;

import java.util.Arrays;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static refactoringml.util.PropertiesUtils.getProperty;
import static refactoringml.util.RefactoringUtils.cleanMethodName;

public class CKUtils {
	private static final Logger log = LogManager.getLogger(CKUtils.class);
	private static long timeout = Long.parseLong(getProperty("timeoutCK"));

	//TODO: figure out if we could parallelize the CK tool for various class files on the same commit
	//Calls the CK.calculate with a timeout.
	public static void calculate(String tempdir, String commitHash, String projectUrl, CKNotifier ckNotifier){
		ExecutorService executor = Executors.newFixedThreadPool(1);
		FutureTask timeoutTask = new FutureTask(() -> {
			long startTimeCK = System.currentTimeMillis();
			new CK().calculate(tempdir, ckNotifier);
			log.debug("CK miner took " + (System.currentTimeMillis() - startTimeCK) + " milliseconds to calculate the metrics for file: " + tempdir + " on commit: " + commitHash + " from project " + projectUrl);
			return null;
		});
		executor.submit(timeoutTask);

		try {
			timeoutTask.get(timeout, TimeUnit.SECONDS);
		} catch (TimeoutException e){
			log.error("CK failed to calculate metrics for " + tempdir + " on the commit " + commitHash
					+ " from the project: " + projectUrl + " with a timeout of " + timeout + " seconds.", e);
		} catch (InterruptedException | ExecutionException e){
			log.error("Failed to calculate CK metrics for " + tempdir + " on the commit " + commitHash
					+ " from the project: " + projectUrl, e);
		} finally {
			executor.shutdownNow();
		}
	}

	/**
	 * This method simplifies full method names, so that both
	 * CK's and RefactoringMiner's format match.
	 *
	 * Basically:
	 * 1) removes full names in parameters
	 *    e.g., a/1[a.b.C] --> a/1[C]
	 * 2) removes any generic type information
	 *    e.g., a/1[A<B>] --> a/1[A]
	 * 3) removes any annotation
	 *    e.g., a/1[@B A] --> a/1[A]
	 *
	 * Maybe the same behavior could had been achieved by
	 * RefactoringUtils#fullMethodName, as we get the full name of the method
	 * from the UMLOperation object. However, it's much harder to test and we know
	 * way less about that object...
	 */
	public static String simplifyFullMethodName(String fullName) {
		if(!fullName.contains("["))
			return fullName;

		String leftPart = fullName.substring(0, fullName.indexOf("["));
		String rightPart = fullName.substring(fullName.indexOf("[") + 1, fullName.length()-1);

		rightPart = cleanGenerics(rightPart);
		rightPart = removeAnnotations(rightPart);

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

	//Remove annotations from method names, e.g. @NonNull
	//Don't use this on method bodies, it could cause issues in some cases, e.g. an @ as part of a String.
	private static String removeAnnotations(String code) {
		return code.replaceAll("@.* ", "");
	}

	// we replace the $ that appears in the name of a class when there is a subclass, e.g., A$B becomes A.B
	// we remove generics, e.g., A<B, C> becomes A
	// Why? Because the way JDT resolves (and stringuifies) class names in TypeDeclarations
	// is different from the way it resolves (and stringuifies) in MethodBinding...
	// We also remove the generic types as RefactoringMiner doesn't return the generics.
	private static String cleanGenerics(String code) {
		code = code.replaceAll("\\$", "\\.");

		// while there's a < in the string, we then look for its corresponding >.
		// we then extract this part out of the string.
		// we repeat it until there's no more <
		while(code.contains("<")) {
			int openIndex = code.indexOf("<");
			int qty = 0;
			int closeIndex;
			for (closeIndex = openIndex + 1; closeIndex < code.length(); closeIndex++) {

				char ch = code.charAt(closeIndex);
				if (ch == '<')
					qty++;
				else if (ch == '>') {
					if (qty == 0)
						break;

					qty--;
				}
			}

			String leftPart = code.substring(0, openIndex);
			String rightPart = closeIndex + 1 == code.length() ? "" : code.substring(closeIndex + 1);
			code = leftPart + rightPart;
		}

		return code.trim();
	}

	/*
	Only works with the class type from ck.
	 */
	public static String cleanCkClassName(String clazzName) {
		return clazzName.replaceAll("\\$", "\\.");
	}

	/*
	Only works with the class type from ck.
	 */
	public static boolean evaluateSubclass(String classType) { return classType.equals("innerclass"); }

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
				ck.getInnerClassesQty(),
				ck.getLambdasQty(),
				ck.getUniqueWordsQty());
	}

	//Extract the method metrics from a CKMethodResult
	public static MethodMetric extractMethodMetrics(CKMethodResult ckMethodResult){
		return new MethodMetric(
				CKUtils.simplifyFullMethodName(ckMethodResult.getMethodName()),
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
				ckMethodResult.getInnerClassesQty(),
				ckMethodResult.getLambdasQty(),
				ckMethodResult.getUniqueWordsQty()
		);
	}
}