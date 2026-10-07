package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import de.soderer.utilities.remote.FilePermissions;

@SuppressWarnings("static-method")
public class FilePermissionsTest {
	@Test
	public void test() {
		try {
			assertEquals("600", new FilePermissions(true, true, false).getPermissionMapString());
			assertEquals("764", new FilePermissions(true, true, true, true, true, false, true, false, false).getPermissionMapString());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}
}
