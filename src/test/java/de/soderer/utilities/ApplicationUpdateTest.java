package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.appupdate.ApplicationUpdateUtilities;

@SuppressWarnings("static-method")
public class ApplicationUpdateTest {
	@Test
	public void testGitHubtestSodererDe() {
		try {
			final boolean result = ApplicationUpdateUtilities.executeUpdate(
					null,
					"https://api.github.com/repos/hudeany/LanguagePropertiesManager/releases/latest",
					null,
					"LanguagePropertiesManager",
					new Version("1.0.0"),
					null,
					null,
					null,
					null,
					null,
					false,
					false);
			Assert.assertEquals(true, result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSodererDe() {
		try {
			final boolean result = ApplicationUpdateUtilities.executeUpdate(
					null,
					"https://soderer.de/index.php?download=Versions.json",
					null,
					"LanguagePropertiesManager",
					new Version("1.0.0"),
					null,
					null,
					null,
					null,
					null,
					false,
					false);
			Assert.assertEquals(true, result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
