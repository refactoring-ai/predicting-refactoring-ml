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
	}

	@Test
	public void methodNeedsCleaning() {
		Assert.assertEquals("method/2[int,ClassC,ClassD]", CKUtils.simplifyFullName("method/2[int,a.b.ClassC,d.e.ClassD]"));
		Assert.assertEquals("method/2[ClassD]", CKUtils.simplifyFullName("method/2[d.e.ClassD]"));
	}

	@Test
	public void array() {
		Assert.assertEquals("method/2[int,ClassC,ClassD[]]", CKUtils.simplifyFullName("method/2[int,a.b.ClassC,d.e.ClassD[]]"));
		Assert.assertEquals("method/2[int,ClassC,ClassD[]]", CKUtils.simplifyFullName("method/2[int,ClassC,ClassD[]]"));
	}
}
