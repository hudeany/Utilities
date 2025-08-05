package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.swt.CredentialsDialog;
import de.soderer.utilities.swt.MultiInputDialog;
import de.soderer.utilities.swt.SimpleInputDialog;

@SuppressWarnings("static-method")
public class SwtSimpleInputDialogManuallyTest {
	@Test
	public void test() {
		try {
			new MultiInputDialog(null, "Test Title", "Very short text!", "Text 1", "Text 2", "Text 3").open();
			new CredentialsDialog(null, "Test Title", "Very short text!", true, true).open();
			new SimpleInputDialog(null, "Test Title", "Very short text!").open();
			new SimpleInputDialog(null, "Test Title", "Long text in single line. Long text in single line. Long text in single line. Long text in single line. Long text in single line.").open();
			new SimpleInputDialog(null, "Test Title", "Short text\nin multiple lines").open();
			new SimpleInputDialog(null, "Test Title", "Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines.\nLong text multiple lines.\nLong text multiple lines.\nLong text multiple lines.\nLong text multiple lines.").open();

			String bigText = "";
			for (int i = 0; i < 100; i++) {
				bigText += "Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines.\n";
			}
			new SimpleInputDialog(null, "Test Title", bigText).open();
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
