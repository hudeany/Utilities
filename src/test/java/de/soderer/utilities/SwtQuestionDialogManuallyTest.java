package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.swt.QuestionDialog;

@SuppressWarnings("static-method")
public class SwtQuestionDialogManuallyTest {
	@Test
	public void test() {
		try {
			new QuestionDialog(null, "Test Title", "Very short text!", "OK").open();
			new QuestionDialog(null, "Test Title", "Long text in single line. Long text in single line. Long text in single line. Long text in single line. Long text in single line.", "OK").open();
			new QuestionDialog(null, "Test Title", "Short text\nin multiple lines", "OK").open();
			new QuestionDialog(null, "Test Title", "Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines.\nLong text multiple lines.\nLong text multiple lines.\nLong text multiple lines.\nLong text multiple lines.", "OK").open();

			String bigText = "";
			for (int i = 0; i < 100; i++) {
				bigText += "Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines.\n";
			}
			new QuestionDialog(null, "Test Title", bigText, "OK").open();
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
