package refactoringml.db;

import javax.persistence.Embeddable;

@Embeddable
public class ClassMetric {

	private boolean isSubclass;
	private int classCbo;
	private int classWmc;
	private int classRfc;
	private int classLcom;
	private int classNumberOfMethods;
	private int classNumberOfStaticMethods;

	private int classNumberOfPublicMethods;
	private int classNumberOfPrivateMethods;
	private int classNumberOfProtectedMethods;
	private int classNumberOfDefaultMethods;
	private int classNumberOfAbstractMethods;
	private int classNumberOfFinalMethods;
	private int classNumberOfSynchronizedMethods;
	private int classNumberOfFields;
	private int classNumberOfStaticFields;
	private int classNumberOfPublicFields;
	private int classNumberOfPrivateFields;
	private int classNumberOfProtectedFields;
	private int classNumberOfDefaultFields;
	private int classNumberOfFinalFields;
	private int classNumberOfSynchronizedFields;
	private int classNosi;
	private int classLoc;
	private int classReturnQty;
	private int classLoopQty;
	private int classComparisonsQty;
	private int classTryCatchQty;
	private int classParenthesizedExpsQty;
	private int classStringLiteralsQty;
	private int classNumbersQty;
	private int classAssignmentsQty;
	private int classMathOperationsQty;
	private int classVariablesQty;
	private int classMaxNestedBlocks;
	private int classAnonymousClassesQty;
	private int classSubClassesQty;
	private int classLambdasQty;
	private int classUniqueWordsQty;

	@Deprecated // hibernate purposes
	public ClassMetric(){}

	public ClassMetric(boolean isSubclass, int classCbo, int classWmc, int classRfc, int classLcom, int classNumberOfMethods, int classNumberOfStaticMethods, int classNumberOfPublicMethods, int classNumberOfPrivateMethods, int classNumberOfProtectedMethods,
	                   int classNumberOfDefaultMethods, int classNumberOfAbstractMethods, int classNumberOfFinalMethods, int classNumberOfSynchronizedMethods, int classNumberOfFields, int classNumberOfStaticFields,
	                   int classNumberOfPublicFields, int classNumberOfPrivateFields, int classNumberOfProtectedFields, int classNumberOfDefaultFields, int classNumberOfFinalFields, int classNumberOfSynchronizedFields,
	                   int classNosi, int classLoc, int classReturnQty, int classLoopQty, int classComparisonsQty, int classTryCatchQty, int classParenthesizedExpsQty, int classStringLiteralsQty, int classNumbersQty, int classAssignmentsQty,
	                   int classMathOperationsQty, int classVariablesQty, int classMaxNestedBlocks, int classAnonymousClassesQty, int classSubClassesQty, int classLambdasQty, int classUniqueWordsQty) {
		this.isSubclass = isSubclass;
		this.classCbo = classCbo;
		this.classWmc = classWmc;
		this.classRfc = classRfc;
		this.classLcom = classLcom;
		this.classNumberOfMethods = classNumberOfMethods;
		this.classNumberOfStaticMethods = classNumberOfStaticMethods;
		this.classNumberOfPublicMethods = classNumberOfPublicMethods;
		this.classNumberOfPrivateMethods = classNumberOfPrivateMethods;
		this.classNumberOfProtectedMethods = classNumberOfProtectedMethods;
		this.classNumberOfDefaultMethods = classNumberOfDefaultMethods;
		this.classNumberOfAbstractMethods = classNumberOfAbstractMethods;
		this.classNumberOfFinalMethods = classNumberOfFinalMethods;
		this.classNumberOfSynchronizedMethods = classNumberOfSynchronizedMethods;
		this.classNumberOfFields = classNumberOfFields;
		this.classNumberOfStaticFields = classNumberOfStaticFields;
		this.classNumberOfPublicFields = classNumberOfPublicFields;
		this.classNumberOfPrivateFields = classNumberOfPrivateFields;
		this.classNumberOfProtectedFields = classNumberOfProtectedFields;
		this.classNumberOfDefaultFields = classNumberOfDefaultFields;
		this.classNumberOfFinalFields = classNumberOfFinalFields;
		this.classNumberOfSynchronizedFields = classNumberOfSynchronizedFields;
		this.classNosi = classNosi;
		this.classLoc = classLoc;
		this.classReturnQty = classReturnQty;
		this.classLoopQty = classLoopQty;
		this.classComparisonsQty = classComparisonsQty;
		this.classTryCatchQty = classTryCatchQty;
		this.classParenthesizedExpsQty = classParenthesizedExpsQty;
		this.classStringLiteralsQty = classStringLiteralsQty;
		this.classNumbersQty = classNumbersQty;
		this.classAssignmentsQty = classAssignmentsQty;
		this.classMathOperationsQty = classMathOperationsQty;
		this.classVariablesQty = classVariablesQty;
		this.classMaxNestedBlocks = classMaxNestedBlocks;
		this.classAnonymousClassesQty = classAnonymousClassesQty;
		this.classSubClassesQty = classSubClassesQty;
		this.classLambdasQty = classLambdasQty;
		this.classUniqueWordsQty = classUniqueWordsQty;
	}

	@Override
	public String toString() {
		return "ClassMetric{" +
				"isSubclass=" + isSubclass +
				", classCbo=" + classCbo +
				", classWmc=" + classWmc +
				", classRfc=" + classRfc +
				", classLcom=" + classLcom +
				", classNumberOfMethods=" + classNumberOfMethods +
				", classNumberOfStaticMethods=" + classNumberOfStaticMethods +
				", classNumberOfPublicMethods=" + classNumberOfPublicMethods +
				", classNumberOfPrivateMethods=" + classNumberOfPrivateMethods +
				", classNumberOfProtectedMethods=" + classNumberOfProtectedMethods +
				", classNumberOfDefaultMethods=" + classNumberOfDefaultMethods +
				", classNumberOfAbstractMethods=" + classNumberOfAbstractMethods +
				", classNumberOfFinalMethods=" + classNumberOfFinalMethods +
				", classNumberOfSynchronizedMethods=" + classNumberOfSynchronizedMethods +
				", classNumberOfFields=" + classNumberOfFields +
				", classNumberOfStaticFields=" + classNumberOfStaticFields +
				", classNumberOfPublicFields=" + classNumberOfPublicFields +
				", classNumberOfPrivateFields=" + classNumberOfPrivateFields +
				", classNumberOfProtectedFields=" + classNumberOfProtectedFields +
				", classNumberOfDefaultFields=" + classNumberOfDefaultFields +
				", classNumberOfFinalFields=" + classNumberOfFinalFields +
				", classNumberOfSynchronizedFields=" + classNumberOfSynchronizedFields +
				", classNosi=" + classNosi +
				", classLoc=" + classLoc +
				", classReturnQty=" + classReturnQty +
				", classLoopQty=" + classLoopQty +
				", classComparisonsQty=" + classComparisonsQty +
				", classTryCatchQty=" + classTryCatchQty +
				", classParenthesizedExpsQty=" + classParenthesizedExpsQty +
				", classStringLiteralsQty=" + classStringLiteralsQty +
				", classNumbersQty=" + classNumbersQty +
				", classAssignmentsQty=" + classAssignmentsQty +
				", classMathOperationsQty=" + classMathOperationsQty +
				", classVariablesQty=" + classVariablesQty +
				", classMaxNestedBlocks=" + classMaxNestedBlocks +
				", classAnonymousClassesQty=" + classAnonymousClassesQty +
				", classSubClassesQty=" + classSubClassesQty +
				", classLambdasQty=" + classLambdasQty +
				", classUniqueWordsQty=" + classUniqueWordsQty +
				'}';
	}

	public boolean isSubclass(){ return isSubclass; }

	/*
	Only works with the class name from ck, because there subclasses are marked with a $ symbol.
	 */
	public static boolean evaluateSubclass(String className) { return className.contains("$"); }

	public int getClassCbo() {
		return classCbo;
	}

	public int getClassWmc() {
		return classWmc;
	}

	public int getClassRfc() {
		return classRfc;
	}

	public int getClassLcom() {
		return classLcom;
	}

	public int getClassNumberOfMethods() {
		return classNumberOfMethods;
	}

	public int getClassNumberOfStaticMethods() {
		return classNumberOfStaticMethods;
	}

	public int getClassNumberOfPublicMethods() {
		return classNumberOfPublicMethods;
	}

	public int getClassNumberOfPrivateMethods() {
		return classNumberOfPrivateMethods;
	}

	public int getClassNumberOfProtectedMethods() {
		return classNumberOfProtectedMethods;
	}

	public int getClassNumberOfDefaultMethods() {
		return classNumberOfDefaultMethods;
	}

	public int getClassNumberOfAbstractMethods() {
		return classNumberOfAbstractMethods;
	}

	public int getClassNumberOfFinalMethods() {
		return classNumberOfFinalMethods;
	}

	public int getClassNumberOfSynchronizedMethods() {
		return classNumberOfSynchronizedMethods;
	}

	public int getClassNumberOfFields() {
		return classNumberOfFields;
	}

	public int getClassNumberOfStaticFields() {
		return classNumberOfStaticFields;
	}

	public int getClassNumberOfPublicFields() {
		return classNumberOfPublicFields;
	}

	public int getClassNumberOfPrivateFields() {
		return classNumberOfPrivateFields;
	}

	public int getClassNumberOfProtectedFields() {
		return classNumberOfProtectedFields;
	}

	public int getClassNumberOfDefaultFields() {
		return classNumberOfDefaultFields;
	}

	public int getClassNumberOfFinalFields() {
		return classNumberOfFinalFields;
	}

	public int getClassNumberOfSynchronizedFields() {
		return classNumberOfSynchronizedFields;
	}

	public int getClassNosi() {
		return classNosi;
	}

	public int getClassLoc() {
		return classLoc;
	}

	public int getClassReturnQty() {
		return classReturnQty;
	}

	public int getClassLoopQty() {
		return classLoopQty;
	}

	public int getClassComparisonsQty() {
		return classComparisonsQty;
	}

	public int getClassTryCatchQty() {
		return classTryCatchQty;
	}

	public int getClassParenthesizedExpsQty() {
		return classParenthesizedExpsQty;
	}

	public int getClassStringLiteralsQty() {
		return classStringLiteralsQty;
	}

	public int getClassNumbersQty() {
		return classNumbersQty;
	}

	public int getClassAssignmentsQty() {
		return classAssignmentsQty;
	}

	public int getClassMathOperationsQty() {
		return classMathOperationsQty;
	}

	public int getClassVariablesQty() {
		return classVariablesQty;
	}

	public int getClassMaxNestedBlocks() {
		return classMaxNestedBlocks;
	}

	public int getClassAnonymousClassesQty() {
		return classAnonymousClassesQty;
	}

	public int getClassSubClassesQty() {
		return classSubClassesQty;
	}

	public int getClassLambdasQty() {
		return classLambdasQty;
	}

	public int getClassUniqueWordsQty() {
		return classUniqueWordsQty;
	}
}
