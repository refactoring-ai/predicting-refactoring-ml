package refactoringml.db;

import javax.persistence.Embeddable;

@Embeddable
public class ClassMetric {

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

	public ClassMetric(int classCbo, int classWmc, int classRfc, int classLcom, int classNumberOfMethods, int classNumberOfStaticMethods, int classNumberOfPublicMethods, int classNumberOfPrivateMethods, int classNumberOfProtectedMethods,
	                   int classNumberOfDefaultMethods, int classNumberOfAbstractMethods, int classNumberOfFinalMethods, int classNumberOfSynchronizedMethods, int classNumberOfFields, int classNumberOfStaticFields,
	                   int classNumberOfPublicFields, int classNumberOfPrivateFields, int classNumberOfProtectedFields, int classNumberOfDefaultFields, int classNumberOfFinalFields, int classNumberOfSynchronizedFields,
	                   int classNosi, int classLoc, int classReturnQty, int classLoopQty, int classComparisonsQty, int classTryCatchQty, int classParenthesizedExpsQty, int classStringLiteralsQty, int classNumbersQty, int classAssignmentsQty,
	                   int classMathOperationsQty, int classVariablesQty, int classMaxNestedBlocks, int classAnonymousClassesQty, int classSubClassesQty, int classLambdasQty, int classUniqueWordsQty) {
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
				"classCbo=" + classCbo +
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
}
