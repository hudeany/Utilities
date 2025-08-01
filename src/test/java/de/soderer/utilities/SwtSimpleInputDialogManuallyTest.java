package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.swt.SimpleInputDialog;

@SuppressWarnings("static-method")
public class SwtSimpleInputDialogManuallyTest {
	@Test
	public void test() {
		try {
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
