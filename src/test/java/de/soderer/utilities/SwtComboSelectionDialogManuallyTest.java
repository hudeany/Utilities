package de.soderer.utilities;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.swt.ComboSelectionDialog;

@SuppressWarnings("static-method")
public class SwtComboSelectionDialogManuallyTest {
	@Test
	public void test() {
		try {
			final List<String> selectionTexts = new ArrayList<>();
			selectionTexts.add("Selection number 1");
			selectionTexts.add("Selection number 2");
			selectionTexts.add("Selection number 3");
			new ComboSelectionDialog(null, "Test Title", "Very short text!", selectionTexts).open();
			new ComboSelectionDialog(null, "Test Title", "Long text in single line. Long text in single line. Long text in single line. Long text in single line. Long text in single line.", selectionTexts).open();
			new ComboSelectionDialog(null, "Test Title", "Short text\nin multiple lines", selectionTexts).open();
			new ComboSelectionDialog(null, "Test Title", "Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines.\nLong text multiple lines.\nLong text multiple lines.\nLong text multiple lines.\nLong text multiple lines.", selectionTexts).open();

			String bigText = "";
			for (int i = 0; i < 100; i++) {
				bigText += "Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines. Long text multiple lines.\n";
			}
			new ComboSelectionDialog(null, "Test Title", bigText, selectionTexts).open();
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
