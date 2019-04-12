package refactoringml;

import org.junit.Assert;
import org.junit.Test;

public class CSVUtilsTest {

	@Test
	public void escape() {
		Assert.assertEquals("\"method[a,b,c,d]\"", CSVUtils.escape("method[a,b,c,d]"));
		Assert.assertEquals("\"a,b,c,d,\"\"e\"\"\"", CSVUtils.escape("a,b,c,d,\"e\""));
	}
}
