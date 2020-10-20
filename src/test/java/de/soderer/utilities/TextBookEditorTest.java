package de.soderer.utilities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.textbook.TextBookEditor;

public class TextBookEditorTest {
	@Test
	public void testDropEmpty() {
		File tempTextFile_src = null;
		final File tempTextFile_dest = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				for (int i = 0; i < 20; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\n").getBytes(StandardCharsets.UTF_8));
					outputStream.write(("\n").getBytes(StandardCharsets.UTF_8));
				}
			}

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, 5, 128, StandardCharsets.UTF_8)) {
				textBookEditor.read();

				Assert.assertEquals(20, textBookEditor.countEmptyLines());

				final long droppedLines1 = textBookEditor.dropEmptyLines(3, 37);
				Assert.assertEquals(16, droppedLines1);
				Assert.assertEquals(24, textBookEditor.getNumberOfLines());

				final long droppedLines2 = textBookEditor.dropEmptyLines();
				Assert.assertEquals(4, droppedLines2);
				Assert.assertEquals(20, textBookEditor.getNumberOfLines());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
			}
			if (tempTextFile_dest != null && tempTextFile_dest.exists()) {
				tempTextFile_dest.delete();
			}
		}
	}

	@Test
	public void testDropBlank() {
		File tempTextFile_src = null;
		final File tempTextFile_dest = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				for (int i = 0; i < 20; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\n").getBytes(StandardCharsets.UTF_8));
					outputStream.write((" \n").getBytes(StandardCharsets.UTF_8));
				}
			}

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, 5, 128, StandardCharsets.UTF_8)) {
				textBookEditor.read();

				Assert.assertEquals(20, textBookEditor.countBlankLines());

				final long droppedLines1 = textBookEditor.dropBlankLines(3, 37);
				Assert.assertEquals(16, droppedLines1);
				Assert.assertEquals(24, textBookEditor.getNumberOfLines());

				final long droppedLines2 = textBookEditor.dropBlankLines();
				Assert.assertEquals(4, droppedLines2);
				Assert.assertEquals(20, textBookEditor.getNumberOfLines());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
			}
			if (tempTextFile_dest != null && tempTextFile_dest.exists()) {
				tempTextFile_dest.delete();
			}
		}
	}

	@Test
	public void testSearch() {
		File tempTextFile_src = null;
		final File tempTextFile_dest = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				for (int i = 0; i < 20; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\n").getBytes(StandardCharsets.UTF_8));
				}
			}

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, 5, 128, StandardCharsets.UTF_8)) {
				textBookEditor.read();

				Assert.assertEquals(17, textBookEditor.search("Line 17").getLine());
				Assert.assertEquals(1, textBookEditor.countLinesContaining("Line 17"));
				Assert.assertEquals(1, textBookEditor.searchAndReplace("Line 17", "Replaced", true, true, true, null, null));
				Assert.assertEquals(0, textBookEditor.countLinesContaining("Line 17"));
				Assert.assertEquals(1, textBookEditor.countLinesContaining("Replaced"));
				Assert.assertEquals(20, textBookEditor.countLinesContaining(TextUtilities.GERMAN_TEST_STRING));
				Assert.assertEquals(61, textBookEditor.searchCount("e"));
				Assert.assertEquals(40, textBookEditor.searchAndReplace("abc", "def"));
				Assert.assertEquals(80, textBookEditor.searchCount("def"));
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
			}
			if (tempTextFile_dest != null && tempTextFile_dest.exists()) {
				tempTextFile_dest.delete();
			}
		}
	}

	@Test
	public void testLineEdit() {
		File tempTextFile_src = null;
		File tempTextFile_dest = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				for (int i = 0; i < 20; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\n").getBytes(StandardCharsets.UTF_8));
				}
			}

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, 5, 128, StandardCharsets.UTF_8)) {
				textBookEditor.read();

				textBookEditor.insertLine(6, "NewLineX");
				textBookEditor.replaceLines(8, 10, Arrays.asList(new String[] { "ReplacedLineX" }));
				Assert.assertEquals(7, textBookEditor.search("NewLineX").getLine());
				Assert.assertEquals(8, textBookEditor.search("ReplacedLineX").getLine());
				Assert.assertEquals(17, textBookEditor.keepOnlyLinesContaining("LineX", true, false, 0, 1000));
				Assert.assertEquals(2, textBookEditor.getNumberOfLines());

				tempTextFile_dest = File.createTempFile("TextBookEditorTest" + "_", ".txt.zip");
				tempTextFile_dest.delete();
				textBookEditor.write(tempTextFile_dest);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
			}
			if (tempTextFile_dest != null && tempTextFile_dest.exists()) {
				tempTextFile_dest.delete();
			}
		}
	}

	@Test
	public void testBigReplace() {
		File tempTextFile_src = null;
		File tempTextFile_dest = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				for (int i = 0; i < 100000; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\n").getBytes(StandardCharsets.UTF_8));
				}
			}

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, StandardCharsets.UTF_8)) {
				textBookEditor.read();

				Assert.assertEquals(200000, textBookEditor.searchAndReplace("abcde", "Replaced"));

				tempTextFile_dest = File.createTempFile("TextBookEditorTest" + "_", ".txt.zip");
				tempTextFile_dest.delete();
				textBookEditor.write(tempTextFile_dest);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
			}
			if (tempTextFile_dest != null && tempTextFile_dest.exists()) {
				tempTextFile_dest.delete();
			}
		}
	}

	@Test
	public void testNewEmptyTextBookEditor() {
		File tempTextFile_dest = null;
		try {
			try (TextBookEditor textBookEditor = new TextBookEditor(StandardCharsets.UTF_8)) {
				textBookEditor.insertLine(0, TextUtilities.GERMAN_TEST_STRING);

				tempTextFile_dest = File.createTempFile("TextBookEditorTest" + "_", ".txt");
				tempTextFile_dest.delete();
				textBookEditor.write(tempTextFile_dest);
			}

			Assert.assertEquals(TextUtilities.GERMAN_TEST_STRING + "\n", FileUtilities.readFileToString(tempTextFile_dest, StandardCharsets.UTF_8));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_dest != null && tempTextFile_dest.exists()) {
				tempTextFile_dest.delete();
			}
		}
	}
}
