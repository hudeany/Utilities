package de.soderer.utilities;


import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import de.soderer.utilities.crypto.OID;

@SuppressWarnings("static-method")
public class OidTest {
	@Test
	public void test1() {
		try {
			final byte[] oidBytes = new OID("1.3.132.0.34").getByteArrayEncoding();
			final String oidString = new OID(oidBytes).getStringEncoding();
			assertArrayEquals(new byte[] { 43, -127, 4, 0, 34 }, oidBytes);
			assertEquals("1.3.132.0.34", oidString);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}
}
