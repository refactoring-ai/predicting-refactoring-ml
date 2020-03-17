package refactoringml;

import org.junit.Assert;
import org.junit.Test;
import refactoringml.util.CKUtils;

public class CKUtilsTest {

	@Test
	public void methodWithoutParams() {
		Assert.assertEquals("method/0", CKUtils.simplifyFullMethodName("method/0"));
	}

	@Test
	public void methodAlreadyClean() {
		Assert.assertEquals("method/2[int]", CKUtils.simplifyFullMethodName("method/2[int]"));
		Assert.assertEquals("method/2[int,double]", CKUtils.simplifyFullMethodName("method/2[int,double]"));
		Assert.assertEquals("method/2[A,B]", CKUtils.simplifyFullMethodName("method/2[A,B]"));

		Assert.assertEquals("CSVRecord/5[String[],Map,String,long,long]", CKUtils.simplifyFullMethodName("CSVRecord/5[String[],Map,String,long,long]"));
	}

	@Test
	public void methodNeedsCleaning() {
		Assert.assertEquals("method/2[int,ClassC,ClassD]", CKUtils.simplifyFullMethodName("method/2[int,a.b.ClassC,d.e.ClassD]"));
		Assert.assertEquals("method/2[ClassD]", CKUtils.simplifyFullMethodName("method/2[d.e.ClassD]"));
	}

	// for now, we clean arrays too, as RefactoringMiner seems to be removing arrays from method signatures
	@Test
	public void array() {
		Assert.assertEquals("method/2[int,ClassC,ClassD[]]", CKUtils.simplifyFullMethodName("method/2[int,a.b.ClassC,d.e.ClassD[]]"));
		Assert.assertEquals("method/2[int,ClassC,ClassD[][]]", CKUtils.simplifyFullMethodName("method/2[int,ClassC,ClassD[][]]"));
	}

	@Test
	public void mixOfArraysAndGenerics_exampleFromCommonsCsv() {
		String simplified = CKUtils.simplifyFullMethodName("CSVRecord/5[java.lang.String[],java.util.Map<java.lang.String,java.lang.Integer>,java.lang.String,long,long]");
		Assert.assertEquals("CSVRecord/5[String[],Map,String,long,long]", simplified);
	}

	@Test
	public void fullClassNamesAndGenerics() {
		String fullVersion = "doConnect/3[com.ning.http.client.providers.Request,com.ning.http.client.providers.AsyncHandler<T>,com.ning.http.client.providers.NettyResponseFuture<T>]";

		String simplified = CKUtils.simplifyFullMethodName(fullVersion);

		Assert.assertEquals("doConnect/3[Request,AsyncHandler,NettyResponseFuture]", simplified);
	}

	@Test
	public void genericInsideGenerics() {
		String fullVersion = "setParameters/1[Map<String, Collection<String>>]";

		String simplified = CKUtils.simplifyFullMethodName(fullVersion);

		Assert.assertEquals("setParameters/1[Map]", simplified);
	}

	@Test
	public void genericInsideGenerics_2() {
		String fullVersion = "setParameters/1[Map<String, Collection<String>, String>]";

		String simplified = CKUtils.simplifyFullMethodName(fullVersion);

		Assert.assertEquals("setParameters/1[Map]", simplified);
	}

	// that can happen in RMiner...
	// see https://github.com/refactoring-ai/predicting-refactoring-ml/issues/142
	@Test
	public void methodWithAnnotation() {
		String fullVersion = "contains/1[@NonNull Entry]";
		String simplified = CKUtils.simplifyFullMethodName(fullVersion);
		Assert.assertEquals("contains/1[Entry]", simplified);

		fullVersion = "contains/1[@a.b.NonNull Entry]";
		simplified = CKUtils.simplifyFullMethodName(fullVersion);
		Assert.assertEquals("contains/1[Entry]", simplified);

		fullVersion = "contains/1[ @a.b.NonNull Entry ]";
		simplified = CKUtils.simplifyFullMethodName(fullVersion);
		Assert.assertEquals("contains/1[Entry]", simplified);
	}
}
