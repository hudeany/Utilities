package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.swing.QuestionDialog;

@SuppressWarnings("static-method")
public class SwingQuestionDialogManuallyTest {
	@Test
	public void test() {
		try {
			de.soderer.utilities.swing.SwingUtilities.setSystemLookAndFeel();
			new QuestionDialog(null, "Test Title", "Very short text!").open();
			new QuestionDialog(null, "Test Title", "Long text in single line. Long text in single line. Long text in single line. Long text in single line. Long text in single line.").open();
			new QuestionDialog(null, "Test Title", "Short text\nin multiple lines").open();
			new QuestionDialog(null, "Test Title", "Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines.\nLong text multiple lines.\nLong text multiple lines.\nLong text multiple lines.\nLong text multiple lines.").open();

			String bigText = "";
			for (int i = 0; i < 100; i++) {
				bigText += "Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines.\n";
			}
			new QuestionDialog(null, "Test Title", bigText).open();
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
