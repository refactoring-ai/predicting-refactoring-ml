package refactoringml.db;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class MethodMetric {


	@Column(nullable = true) private String fullMethodName;
	@Column(nullable = true) private String shortMethodName;

	@Column(nullable = true) private int startLine;
	@Column(nullable = true) private int methodCbo;
	@Column(nullable = true) private int methodWmc;
	@Column(nullable = true) private int methodRfc;
	@Column(nullable = true) private int methodLoc;
	@Column(nullable = true) private int methodReturnQty;
	@Column(nullable = true) private int methodVariablesQty;
	@Column(nullable = true) private int methodParametersQty;
	@Column(nullable = true) private int methodLoopQty;
	@Column(nullable = true) private int methodComparisonsQty;
	@Column(nullable = true) private int methodTryCatchQty;
	@Column(nullable = true) private int methodParenthesizedExpsQty;
	@Column(nullable = true) private int methodStringLiteralsQty;
	@Column(nullable = true) private int methodNumbersQty;
	@Column(nullable = true) private int methodAssignmentsQty;
	@Column(nullable = true) private int methodMathOperationsQty;
	@Column(nullable = true) private int methodMaxNestedBlocks;
	@Column(nullable = true) private int methodAnonymousClassesQty;
	@Column(nullable = true) private int methodSubClassesQty;
	@Column(nullable = true) private int methodLambdasQty;
	@Column(nullable = true) private int methodUniqueWordsQty;

	@Deprecated // hibernate purposes
	public MethodMetric() {}

	public MethodMetric(String fullMethodName, String shortMethodName, int startLine, int methodCbo, int methodWmc, int methodRfc, int loc, int methodReturnQty,
	                    int methodVariablesQty, int methodParametersQty, int methodLoopQty, int methodComparisonsQty, int methodTryCatchQty, int methodParenthesizedExpsQty, int methodStringLiteralsQty, int methodNumbersQty,
	                    int methodAssignmentsQty, int methodMathOperationsQty, int methodMaxNestedBlocks, int methodAnonymousClassesQty, int methodSubClassesQty, int methodLambdasQty, int methodUniqueWordsQty) {
		this.fullMethodName = fullMethodName;
		this.shortMethodName = shortMethodName;
		this.startLine = startLine;
		this.methodCbo = methodCbo;
		this.methodWmc = methodWmc;
		this.methodRfc = methodRfc;
		this.methodLoc = loc;
		this.methodReturnQty = methodReturnQty;
		this.methodVariablesQty = methodVariablesQty;
		this.methodParametersQty = methodParametersQty;
		this.methodLoopQty = methodLoopQty;
		this.methodComparisonsQty = methodComparisonsQty;
		this.methodTryCatchQty = methodTryCatchQty;
		this.methodParenthesizedExpsQty = methodParenthesizedExpsQty;
		this.methodStringLiteralsQty = methodStringLiteralsQty;
		this.methodNumbersQty = methodNumbersQty;
		this.methodAssignmentsQty = methodAssignmentsQty;
		this.methodMathOperationsQty = methodMathOperationsQty;
		this.methodMaxNestedBlocks = methodMaxNestedBlocks;
		this.methodAnonymousClassesQty = methodAnonymousClassesQty;
		this.methodSubClassesQty = methodSubClassesQty;
		this.methodLambdasQty = methodLambdasQty;
		this.methodUniqueWordsQty = methodUniqueWordsQty;
	}

	public int getStartLine() {
		return startLine;
	}

	public String getShortMethodName() {
		return shortMethodName == null ? "" : shortMethodName;
	}

	@Override
	public String toString() {
		return "MethodMetric{" +
				"fullMethodName='" + fullMethodName + '\'' +
				", shortMethodName='" + shortMethodName + '\'' +
				", startLine=" + startLine +
				", methodCbo=" + methodCbo +
				", methodWmc=" + methodWmc +
				", methodRfc=" + methodRfc +
				", methodLoc=" + methodLoc +
				", methodReturnQty=" + methodReturnQty +
				", methodVariablesQty=" + methodVariablesQty +
				", methodParametersQty=" + methodParametersQty +
				", methodLoopQty=" + methodLoopQty +
				", methodComparisonsQty=" + methodComparisonsQty +
				", methodTryCatchQty=" + methodTryCatchQty +
				", methodParenthesizedExpsQty=" + methodParenthesizedExpsQty +
				", methodStringLiteralsQty=" + methodStringLiteralsQty +
				", methodNumbersQty=" + methodNumbersQty +
				", methodAssignmentsQty=" + methodAssignmentsQty +
				", methodMathOperationsQty=" + methodMathOperationsQty +
				", methodMaxNestedBlocks=" + methodMaxNestedBlocks +
				", methodAnonymousClassesQty=" + methodAnonymousClassesQty +
				", methodSubClassesQty=" + methodSubClassesQty +
				", methodLambdasQty=" + methodLambdasQty +
				", methodUniqueWordsQty=" + methodUniqueWordsQty +
				'}';
	}
}
