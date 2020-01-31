package refactoringml;

import org.junit.Assert;
import org.junit.Test;
import refactoringml.util.CKUtils;

public class CKUtilsTest {

	@Test
	public void methodWithoutParams() {
		Assert.assertEquals("method/0", CKUtils.simplifyFullName("method/0"));
	}

	@Test
	public void methodAlreadyClean() {
		Assert.assertEquals("method/2[int]", CKUtils.simplifyFullName("method/2[int]"));
		Assert.assertEquals("method/2[int,double]", CKUtils.simplifyFullName("method/2[int,double]"));

		Assert.assertEquals("CSVRecord/5[String[],Map,String,long,long]", CKUtils.simplifyFullName("CSVRecord/5[String[],Map,String,long,long]"));
	}

	@Test
	public void methodNeedsCleaning() {
		Assert.assertEquals("method/2[int,ClassC,ClassD]", CKUtils.simplifyFullName("method/2[int,a.b.ClassC,d.e.ClassD]"));
		Assert.assertEquals("method/2[ClassD]", CKUtils.simplifyFullName("method/2[d.e.ClassD]"));
	}

	// for now, we clean arrays too, as RefactoringMiner seems to be removing arrays from method signatures
	@Test
	public void array() {
		Assert.assertEquals("method/2[int,ClassC,ClassD[]]", CKUtils.simplifyFullName("method/2[int,a.b.ClassC,d.e.ClassD[]]"));
		Assert.assertEquals("method/2[int,ClassC,ClassD[][]]", CKUtils.simplifyFullName("method/2[int,ClassC,ClassD[][]]"));
	}

	@Test
	public void mixOfArraysAndGenerics_exampleFromCommonsCsv() {
		String simplified = CKUtils.simplifyFullName("CSVRecord/5[java.lang.String[],java.util.Map<java.lang.String,java.lang.Integer>,java.lang.String,long,long]");
		Assert.assertEquals("CSVRecord/5[String[],Map,String,long,long]", simplified);
	}
}
