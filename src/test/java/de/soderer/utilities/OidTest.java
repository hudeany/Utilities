package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

public class OidTest {
	@Test
	public void test1() {
		try {
			final byte[] oidBytes = new OID("1.3.132.0.34").getByteArrayEncoding();
			final String oidString = new OID(oidBytes).getStringEncoding();
			Assert.assertArrayEquals(new byte[] { 43, -127, 4, 0, 34 }, oidBytes);
			Assert.assertEquals("1.3.132.0.34", oidString);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
