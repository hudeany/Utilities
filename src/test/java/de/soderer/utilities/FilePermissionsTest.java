package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.remote.FilePermissions;

@SuppressWarnings("static-method")
public class FilePermissionsTest {
	@Test
	public void test() {
		try {
			Assert.assertEquals("600", new FilePermissions(true, true, false).getPermissionMapString());
			Assert.assertEquals("764", new FilePermissions(true, true, true, true, true, false, true, false, false).getPermissionMapString());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
