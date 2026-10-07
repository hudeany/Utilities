package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class VersionTest {
	@Test
	public void test() throws Exception {
		final Version versionOld = new Version("19.0.9");
		final Version versionNew = new Version("19.0.10");

		assertEquals(0, versionOld.compareTo(versionOld));
		assertEquals(-1, versionOld.compareTo(versionNew));
		assertEquals(1, versionNew.compareTo(versionOld));
	}
}
