package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class VersionTest {
	@Test
	public void test() throws Exception {
		final Version versionOld = new Version("19.0.9");
		final Version versionNew = new Version("19.0.10");

		Assert.assertEquals(0, versionOld.compareTo(versionOld));
		Assert.assertEquals(-1, versionOld.compareTo(versionNew));
		Assert.assertEquals(1, versionNew.compareTo(versionOld));
	}
}
