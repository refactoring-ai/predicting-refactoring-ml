package refactoringml.util;

import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.UMLType;
import gr.uom.java.xmi.diff.*;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RefactoringUtils {

	private static Set<RefactoringType> methodLevelRefactorings;
	private static Set<RefactoringType> classLevelRefactorings;
	private static Set<RefactoringType> variableLevelRefactorings;
	private static final Set<RefactoringType> attributeLevelRefactorings;

	static {
		classLevelRefactorings = new HashSet<>() {{
			add(RefactoringType.MOVE_CLASS);
			add(RefactoringType.RENAME_CLASS);
			add(RefactoringType.EXTRACT_CLASS);
			add(RefactoringType.EXTRACT_SUBCLASS);
			add(RefactoringType.EXTRACT_SUPERCLASS);
			add(RefactoringType.EXTRACT_INTERFACE);
			add(RefactoringType.MOVE_RENAME_CLASS);
			add(RefactoringType.CONVERT_ANONYMOUS_CLASS_TO_TYPE);
			add(RefactoringType.INTRODUCE_POLYMORPHISM);
		}};

		// note that we do not have 'change method signature' and 'merge operation' here
		// there's no detection for merge operation (i suppose it's still under development)
		// there's also no change method signature refactoring (this is a 'relationship', i need to understand it better)
		methodLevelRefactorings = new HashSet<>() {{
			add(RefactoringType.RENAME_METHOD);
			add(RefactoringType.MOVE_OPERATION);
			add(RefactoringType.EXTRACT_AND_MOVE_OPERATION);
			add(RefactoringType.EXTRACT_OPERATION);
			add(RefactoringType.PULL_UP_OPERATION);
			add(RefactoringType.PUSH_DOWN_OPERATION);
			add(RefactoringType.INLINE_OPERATION);
		}};

		variableLevelRefactorings = new HashSet<>() {{
			add(RefactoringType.EXTRACT_VARIABLE);
			add(RefactoringType.INLINE_VARIABLE);
			add(RefactoringType.PARAMETERIZE_VARIABLE);
			add(RefactoringType.RENAME_VARIABLE);
			add(RefactoringType.REPLACE_VARIABLE_WITH_ATTRIBUTE);
			add(RefactoringType.RENAME_PARAMETER);
		}};

		attributeLevelRefactorings = new HashSet<>() {{
			add(RefactoringType.MOVE_ATTRIBUTE);
			add(RefactoringType.PULL_UP_ATTRIBUTE);
			add(RefactoringType.MOVE_RENAME_ATTRIBUTE);
			add(RefactoringType.PUSH_DOWN_ATTRIBUTE);
			add(RefactoringType.REPLACE_ATTRIBUTE);
			add(RefactoringType.RENAME_ATTRIBUTE);
		}};
	}


	public static boolean isAttributeLevelRefactoring(Refactoring refactoring) {
		return attributeLevelRefactorings.contains(refactoring.getRefactoringType());
	}

	public static boolean isMethodLevelRefactoring(Refactoring refactoring) {
		return methodLevelRefactorings.contains(refactoring.getRefactoringType());
	}

	public static boolean isClassLevelRefactoring(Refactoring refactoring) {
		return classLevelRefactorings.contains(refactoring.getRefactoringType());
	}

	public static boolean isVariableLevelRefactoring(Refactoring refactoring) {
		return variableLevelRefactorings.contains(refactoring.getRefactoringType());
	}

	public static boolean studied(Refactoring refactoring) {
		return isMethodLevelRefactoring(refactoring) ||
				isClassLevelRefactoring(refactoring) ||
				isVariableLevelRefactoring(refactoring);
	}

	public static String fullMethodName(UMLOperation operation) {

		String methodName = operation.getName();
		List<UMLType> parameters = operation.getParameterTypeList();

		int parameterCount  = parameters.size();
		List<String> parameterTypes = new ArrayList<>();
		parameters.forEach(param -> parameterTypes.add(param.getClassType()));

		return String.format("%s/%d%s%s%s",
				methodName,
				parameterCount,
				(parameterCount > 0 ? "[" : ""),
				(parameterCount > 0 ? String.join(",", parameterTypes) : ""),
				(parameterCount > 0 ? "]" : "")
		);
	}

	public static UMLOperation getRefactoredMethod(Refactoring refactoring) {
		if(refactoring instanceof RenameOperationRefactoring) {
			RenameOperationRefactoring convertedRefactoring = (RenameOperationRefactoring) refactoring;
			return convertedRefactoring.getOriginalOperation();
		}

		if(refactoring instanceof MoveOperationRefactoring) {
			MoveOperationRefactoring convertedRefactoring = (MoveOperationRefactoring) refactoring;
			return convertedRefactoring.getOriginalOperation();
		}

		if(refactoring instanceof ExtractAndMoveOperationRefactoring) {
			ExtractAndMoveOperationRefactoring convertedRefactoring = (ExtractAndMoveOperationRefactoring) refactoring;
			return convertedRefactoring.getExtractedOperation();
		}

		if(refactoring instanceof ExtractOperationRefactoring) {
			ExtractOperationRefactoring convertedRefactoring = (ExtractOperationRefactoring) refactoring;
			return convertedRefactoring.getSourceOperationBeforeExtraction();
		}

		if(refactoring instanceof PullUpOperationRefactoring) {
			PullUpOperationRefactoring convertedRefactoring = (PullUpOperationRefactoring) refactoring;
			return convertedRefactoring.getOriginalOperation();
		}

		if(refactoring instanceof PushDownOperationRefactoring) {
			PushDownOperationRefactoring convertedRefactoring = (PushDownOperationRefactoring) refactoring;
			return convertedRefactoring.getOriginalOperation();
		}

		if(refactoring instanceof InlineOperationRefactoring) {
			InlineOperationRefactoring convertedRefactoring = (InlineOperationRefactoring) refactoring;
			return convertedRefactoring.getInlinedOperation();
		}

		// now, if it's a variable refactoring, it happens inside of a method, which we get it
		if(refactoring instanceof ExtractVariableRefactoring) {
			ExtractVariableRefactoring convertedRefactoring = (ExtractVariableRefactoring) refactoring;
			return convertedRefactoring.getOperation();
		}

		if(refactoring instanceof InlineVariableRefactoring) {
			InlineVariableRefactoring convertedRefactoring = (InlineVariableRefactoring) refactoring;
			return convertedRefactoring.getOperation();
		}

		if(refactoring instanceof RenameVariableRefactoring) {
			RenameVariableRefactoring convertedRefactoring = (RenameVariableRefactoring) refactoring;
			return convertedRefactoring.getOperationBefore();
		}

		throw new RuntimeException("This is a method-level refactoring, but it seems I can't get the refactored method: " + refactoring.getRefactoringType());
	}

	public static String getRefactoredVariableOrAttribute(Refactoring refactoring) {

		if(refactoring instanceof ExtractVariableRefactoring) {
			ExtractVariableRefactoring convertedRefactoring = (ExtractVariableRefactoring) refactoring;
			return convertedRefactoring.getVariableDeclaration().getVariableName();
		}

		if(refactoring instanceof InlineVariableRefactoring) {
			InlineVariableRefactoring convertedRefactoring = (InlineVariableRefactoring) refactoring;
			return convertedRefactoring.getVariableDeclaration().getVariableName();
		}

		if(refactoring instanceof MoveAttributeRefactoring) {
			MoveAttributeRefactoring convertedRefactoring = (MoveAttributeRefactoring) refactoring;
			return convertedRefactoring.getOriginalAttribute().getName();
		}

		if(refactoring instanceof MoveAndRenameAttributeRefactoring) {
			MoveAndRenameAttributeRefactoring convertedRefactoring = (MoveAndRenameAttributeRefactoring) refactoring;
			return convertedRefactoring.getOriginalAttribute().getName();
		}

		if(refactoring instanceof RenameAttributeRefactoring) {
			RenameAttributeRefactoring convertedRefactoring = (RenameAttributeRefactoring) refactoring;
			return convertedRefactoring.getOriginalAttribute().getVariableName();
		}

		if(refactoring instanceof RenameVariableRefactoring) {
			RenameVariableRefactoring convertedRefactoring = (RenameVariableRefactoring) refactoring;
			return convertedRefactoring.getOriginalVariable().getVariableName();
		}

		if(refactoring instanceof ReplaceAttributeRefactoring) {
			ReplaceAttributeRefactoring convertedRefactoring = (ReplaceAttributeRefactoring) refactoring;
			return convertedRefactoring.getOriginalAttribute().getName();
		}

		if(refactoring instanceof PullUpAttributeRefactoring) {
			PullUpAttributeRefactoring convertedRefactoring = (PullUpAttributeRefactoring) refactoring;
			return convertedRefactoring.getOriginalAttribute().getName();
		}

		if(refactoring instanceof PushDownAttributeRefactoring) {
			PushDownAttributeRefactoring convertedRefactoring = (PushDownAttributeRefactoring) refactoring;
			return convertedRefactoring.getOriginalAttribute().getName();
		}

		throw new RuntimeException("This is a variable-level refactoring, but it seems I can't get the refactored variable");
	}

	public static String cleanMethodName(String methodName) {
		return methodName.contains("/") ? methodName.substring(0, methodName.indexOf("/")) : methodName;
	}

	public static int refactoringTypeInNumber(Refactoring refactoring) {
		if(isClassLevelRefactoring(refactoring)) return 1;
		if(isMethodLevelRefactoring(refactoring)) return 2;
		if(isVariableLevelRefactoring(refactoring)) return 3;
		if(isAttributeLevelRefactoring(refactoring)) return 4;
		return -1;
	}
}
