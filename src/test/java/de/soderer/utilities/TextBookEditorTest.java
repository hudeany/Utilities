package de.soderer.utilities;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.TextUtilities.LineBreak;
import de.soderer.utilities.textbook.TextBookEditor;
import de.soderer.utilities.textbook.TextPosition;

@SuppressWarnings("static-method")
public class TextBookEditorTest {
	@Test
	public void testDropEmpty() {
		File tempTextFile_src = null;
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
				Assert.assertEquals(17, droppedLines1);
				Assert.assertEquals(23, textBookEditor.getNumberOfLines());

				final long droppedLines2 = textBookEditor.dropEmptyLines();
				Assert.assertEquals(3, droppedLines2);
				Assert.assertEquals(20, textBookEditor.getNumberOfLines());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
			}
		}
	}

	@Test
	public void testDropBlank() {
		File tempTextFile_src = null;
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
				textBookEditor.write();

				Assert.assertEquals(20, textBookEditor.countBlankLines());

				final long droppedLines1 = textBookEditor.dropBlankLines(3, 37);
				textBookEditor.write();
				Assert.assertEquals(17, droppedLines1);
				Assert.assertEquals(23, textBookEditor.getNumberOfLines());

				final long droppedLines2 = textBookEditor.dropBlankLines();
				Assert.assertEquals(3, droppedLines2);
				Assert.assertEquals(20, textBookEditor.getNumberOfLines());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
			}
		}
	}

	@Test
	public void testSearch() {
		File tempTextFile_src = null;
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
		}
	}

	@Test
	public void testSearchWithWindowsLinebreak() {
		File tempTextFile_src = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				for (int i = 0; i < 20; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\r\n").getBytes(StandardCharsets.UTF_8));
				}
			}

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, 5, 128, StandardCharsets.UTF_8)) {
				textBookEditor.read();

				Assert.assertEquals(LineBreak.Windows, textBookEditor.getLineBreakType());

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
		}
	}

	@Test
	public void testChangeWithBOM() {
		File tempTextFile_src = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				outputStream.write(BOM.UTF_8.getBytes());
				for (int i = 0; i < 20; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\n").getBytes(StandardCharsets.UTF_8));
				}
			}

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, 5, 128, StandardCharsets.UTF_8)) {
				textBookEditor.read();

				Assert.assertEquals(LineBreak.Unix, textBookEditor.getLineBreakType());

				Assert.assertEquals(17, textBookEditor.search("Line 17").getLine());
				Assert.assertEquals(1, textBookEditor.countLinesContaining("Line 17"));
				Assert.assertEquals(1, textBookEditor.searchAndReplace("Line 17", "Replaced", true, true, true, null, null));
				Assert.assertEquals(0, textBookEditor.countLinesContaining("Line 17"));
				Assert.assertEquals(1, textBookEditor.countLinesContaining("Replaced"));
				Assert.assertEquals(20, textBookEditor.countLinesContaining(TextUtilities.GERMAN_TEST_STRING));
				Assert.assertEquals(61, textBookEditor.searchCount("e"));
				Assert.assertEquals(40, textBookEditor.searchAndReplace("abc", "def"));
				Assert.assertEquals(80, textBookEditor.searchCount("def"));

				textBookEditor.replaceLine(1, null);

				textBookEditor.insertLine(6, "NewLineX");

				textBookEditor.write();
			}

			final byte[] fileData = FileUtilities.readFileToByteArray(tempTextFile_src);
			Assert.assertTrue(fileData[0] == BOM.UTF_8.getBytes()[0] && fileData[1] == BOM.UTF_8.getBytes()[1] && fileData[2] == BOM.UTF_8.getBytes()[2]);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
			}
		}
	}

	@Test
	public void testSearchWithWindowsLinebreakWithBOM() {
		File tempTextFile_src = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				outputStream.write(BOM.UTF_8.getBytes());
				for (int i = 0; i < 20; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\r\n").getBytes(StandardCharsets.UTF_8));
				}
			}

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, 5, 128, StandardCharsets.UTF_8)) {
				textBookEditor.read();

				Assert.assertEquals(LineBreak.Windows, textBookEditor.getLineBreakType());

				Assert.assertEquals(17, textBookEditor.search("Line 17").getLine());
				Assert.assertEquals(1, textBookEditor.countLinesContaining("Line 17"));
				Assert.assertEquals(1, textBookEditor.searchAndReplace("Line 17", "Replaced", true, true, true, null, null));
				Assert.assertEquals(0, textBookEditor.countLinesContaining("Line 17"));
				Assert.assertEquals(1, textBookEditor.countLinesContaining("Replaced"));
				Assert.assertEquals(20, textBookEditor.countLinesContaining(TextUtilities.GERMAN_TEST_STRING));
				Assert.assertEquals(61, textBookEditor.searchCount("e"));
				Assert.assertEquals(40, textBookEditor.searchAndReplace("abc", "def"));
				Assert.assertEquals(80, textBookEditor.searchCount("def"));

				textBookEditor.replaceLine(1, null);

				textBookEditor.insertLine(6, "NewLineX");

				textBookEditor.write();
			}

			final byte[] fileData = FileUtilities.readFileToByteArray(tempTextFile_src);
			Assert.assertTrue(fileData[0] == BOM.UTF_8.getBytes()[0] && fileData[1] == BOM.UTF_8.getBytes()[1] && fileData[2] == BOM.UTF_8.getBytes()[2]);

			final String fileDataString = FileUtilities.readFileToString(tempTextFile_src, StandardCharsets.UTF_8);
			Assert.assertTrue(fileDataString.contains(LineBreak.Windows.toString()));
			Assert.assertFalse(fileDataString.replace(LineBreak.Windows.toString(), "").contains(LineBreak.Mac.toString()));
			Assert.assertFalse(fileDataString.replace(LineBreak.Windows.toString(), "").contains(LineBreak.Unix.toString()));
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
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
			tempTextFile_dest = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			tempTextFile_dest.delete();

			try (TextBookEditor textBookEditor = new TextBookEditor(StandardCharsets.UTF_8)) {
				textBookEditor.insertLine(0, TextUtilities.GERMAN_TEST_STRING);
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

	@Test
	public void testInsertMultipleLines() {
		File tempTextFile_src = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				for (int i = 0; i < 5; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\n").getBytes(StandardCharsets.UTF_8));
				}
			}

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, 5, 128, StandardCharsets.UTF_8)) {
				textBookEditor.read();

				textBookEditor.insertLines(2, Arrays.asList(new String[] { "InsertA", "InsertB", "InsertC" }));

				Assert.assertEquals(8, textBookEditor.getNumberOfLines());
				Assert.assertEquals(3, textBookEditor.search("InsertA").getLine());
				Assert.assertEquals(4, textBookEditor.search("InsertB").getLine());
				Assert.assertEquals(5, textBookEditor.search("InsertC").getLine());
				Assert.assertEquals(6, textBookEditor.search("Line 3:").getLine());
				Assert.assertEquals(8, textBookEditor.search("Line 5:").getLine());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
			}
		}
	}

	@Test
	public void testReplaceLinesAcrossMultiplePages() {
		File tempTextFile_src = null;
		File tempTextFile_dest = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				for (int i = 0; i < 30; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\n").getBytes(StandardCharsets.UTF_8));
				}
			}

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, 5, 128, StandardCharsets.UTF_8)) {
				textBookEditor.read();

				textBookEditor.replaceLines(7, 22, Arrays.asList(new String[] { "X1", "X2", "X3" }));

				Assert.assertEquals(17, textBookEditor.getNumberOfLines());
				Assert.assertEquals(6, textBookEditor.search("Line 6:").getLine());
				Assert.assertEquals(7, textBookEditor.search("X1").getLine());
				Assert.assertEquals(8, textBookEditor.search("X2").getLine());
				Assert.assertEquals(9, textBookEditor.search("X3").getLine());
				Assert.assertEquals(10, textBookEditor.search("Line 23:").getLine());
				Assert.assertEquals(17, textBookEditor.search("Line 30:").getLine());
				Assert.assertNull(textBookEditor.search("Line 7:"));
				Assert.assertNull(textBookEditor.search("Line 21:"));
				Assert.assertNull(textBookEditor.search("Line 22:"));

				tempTextFile_dest = File.createTempFile("TextBookEditorTest" + "_", ".txt");
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
	public void testDeleteLinesAcrossPages() {
		File tempTextFile_src = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				for (int i = 0; i < 20; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\n").getBytes(StandardCharsets.UTF_8));
				}
			}

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, 5, 128, StandardCharsets.UTF_8)) {
				textBookEditor.read();

				textBookEditor.replaceLines(3, 12, null);

				Assert.assertEquals(10, textBookEditor.getNumberOfLines());
				Assert.assertEquals(1, textBookEditor.search("Line 1:").getLine());
				Assert.assertEquals(2, textBookEditor.search("Line 2:").getLine());
				Assert.assertEquals(3, textBookEditor.search("Line 13:").getLine());
				Assert.assertEquals(10, textBookEditor.search("Line 20:").getLine());
				Assert.assertNull(textBookEditor.search("Line 3:"));
				Assert.assertNull(textBookEditor.search("Line 12:"));

				textBookEditor.write();
				Assert.assertEquals(10, textBookEditor.getNumberOfLines());
				Assert.assertEquals(1, textBookEditor.countLinesContaining("Line 20:"));
				Assert.assertEquals(0, textBookEditor.countLinesContaining("Line 5:"));
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
			}
		}
	}

	@Test
	public void testSearchAndCountWithinPageBoundary() {
		File tempTextFile_src = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				for (int i = 0; i < 20; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\n").getBytes(StandardCharsets.UTF_8));
				}
			}

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, 5, 128, StandardCharsets.UTF_8)) {
				textBookEditor.read();

				final TextPosition end = new TextPosition(5, 7);
				Assert.assertEquals(5, textBookEditor.search("Line 5:", false, false, null, end).getLine());
				Assert.assertNull(textBookEditor.search("Line 6:", false, false, null, end));
				Assert.assertEquals(5, textBookEditor.searchCount("Line ", false, false, null, end));

				Assert.assertNull(textBookEditor.search("Line 4:", false, false, null, new TextPosition(3, 7)));
				Assert.assertEquals(0, textBookEditor.searchCount("Line 4:", false, false, null, new TextPosition(3, 7)));

				Assert.assertEquals(1, textBookEditor.countLinesContaining("Line 2:", true, false, 1, 3));
				Assert.assertEquals(0, textBookEditor.countLinesContaining("Line 4:", true, false, 1, 3));

				Assert.assertEquals(1, textBookEditor.searchAndReplace("Line 4:", "REPL4:", false, true, false, null, new TextPosition(4, 7)));
				Assert.assertEquals(4, textBookEditor.search("REPL4:").getLine());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
			}
		}
	}

	@Test
	public void testMultipleWriteReadCycles() {
		File tempTextFile_src = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				for (int i = 0; i < 12; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\n").getBytes(StandardCharsets.UTF_8));
				}
			}

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, 3, 128, StandardCharsets.UTF_8)) {
				textBookEditor.read();

				textBookEditor.replaceLine(1, "Changed1");
				textBookEditor.write();

				Assert.assertEquals(1, textBookEditor.countLinesContaining("Line 5:"));

				textBookEditor.replaceLine(5, "Changed5");
				textBookEditor.write();

				Assert.assertEquals(12, textBookEditor.getNumberOfLines());
				Assert.assertEquals(1, textBookEditor.countLinesContaining("Changed1"));
				Assert.assertEquals(1, textBookEditor.countLinesContaining("Changed5"));
				Assert.assertEquals(0, textBookEditor.countLinesContaining("Line 1:"));
				Assert.assertEquals(0, textBookEditor.countLinesContaining("Line 5:"));
				Assert.assertEquals(1, textBookEditor.countLinesContaining("Line 2:"));
				Assert.assertEquals(1, textBookEditor.countLinesContaining("Line 12:"));
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
			}
		}
	}

	@Test
	public void testSaveAsDoesNotTamperOriginal() {
		File tempTextFile_src = null;
		File tempTextFile_dest = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				for (int i = 0; i < 10; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\n").getBytes(StandardCharsets.UTF_8));
				}
			}
			final long originalLength = tempTextFile_src.length();
			final long originalModified = tempTextFile_src.lastModified();

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, 5, 128, StandardCharsets.UTF_8)) {
				textBookEditor.read();

				textBookEditor.replaceLine(3, "Changed3");

				tempTextFile_dest = File.createTempFile("TextBookEditorTest" + "_", ".txt");
				tempTextFile_dest.delete();
				textBookEditor.write(tempTextFile_dest);
			}

			Assert.assertEquals(originalLength, tempTextFile_src.length());
			Assert.assertEquals(originalModified, tempTextFile_src.lastModified());

			final String destContent = FileUtilities.readFileToString(tempTextFile_dest, StandardCharsets.UTF_8);
			Assert.assertTrue(destContent.contains("Changed3"));
			Assert.assertFalse(destContent.contains("Line 3:"));

			final String srcContent = FileUtilities.readFileToString(tempTextFile_src, StandardCharsets.UTF_8);
			Assert.assertTrue(srcContent.contains("Line 3:"));
			Assert.assertFalse(srcContent.contains("Changed3"));
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
	public void testInterimStoragePaging() {
		File tempTextFile_src = null;
		try {
			tempTextFile_src = File.createTempFile("TextBookEditorTest" + "_", ".txt");
			try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempTextFile_src))) {
				for (int i = 0; i < 50; i++) {
					outputStream.write(("Line " + (i + 1) + ": " + TextUtilities.GERMAN_TEST_STRING + "\n").getBytes(StandardCharsets.UTF_8));
				}
			}

			try (TextBookEditor textBookEditor = new TextBookEditor(tempTextFile_src, 5, 3, StandardCharsets.UTF_8)) {
				textBookEditor.read();
				Assert.assertEquals(10, textBookEditor.getNumberOfPages());

				for (int page = 0; page < 10; page++) {
					textBookEditor.replaceLine((page * 5) + 1, "ChangedPage" + page);
				}

				Assert.assertEquals(50, textBookEditor.getNumberOfLines());
				for (int page = 0; page < 10; page++) {
					final long expectedLine = (page * 5) + 1;
					Assert.assertEquals(expectedLine, textBookEditor.search("ChangedPage" + page).getLine());
					Assert.assertEquals(0, textBookEditor.countLinesContaining("Line " + expectedLine + ":"));
				}
				Assert.assertEquals(1, textBookEditor.countLinesContaining("Line 2:"));

				textBookEditor.write();

				Assert.assertEquals(50, textBookEditor.getNumberOfLines());
				Assert.assertEquals(1, textBookEditor.countLinesContaining("ChangedPage9"));
				Assert.assertEquals(1, textBookEditor.countLinesContaining("Line 2:"));
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
			}
		}
	}
}
