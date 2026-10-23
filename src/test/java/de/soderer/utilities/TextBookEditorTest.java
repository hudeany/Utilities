package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

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

				assertEquals(20, textBookEditor.countEmptyLines());

				final long droppedLines1 = textBookEditor.dropEmptyLines(3, 37);
				assertEquals(17, droppedLines1);
				assertEquals(23, textBookEditor.getNumberOfLines());

				final long droppedLines2 = textBookEditor.dropEmptyLines();
				assertEquals(3, droppedLines2);
				assertEquals(20, textBookEditor.getNumberOfLines());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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

				assertEquals(20, textBookEditor.countBlankLines());

				final long droppedLines1 = textBookEditor.dropBlankLines(3, 37);
				textBookEditor.write();
				assertEquals(17, droppedLines1);
				assertEquals(23, textBookEditor.getNumberOfLines());

				final long droppedLines2 = textBookEditor.dropBlankLines();
				assertEquals(3, droppedLines2);
				assertEquals(20, textBookEditor.getNumberOfLines());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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

				assertEquals(17, textBookEditor.search("Line 17").getLine());
				assertEquals(1, textBookEditor.countLinesContaining("Line 17"));
				assertEquals(1, textBookEditor.searchAndReplace("Line 17", "Replaced", true, true, true, null, null));
				assertEquals(0, textBookEditor.countLinesContaining("Line 17"));
				assertEquals(1, textBookEditor.countLinesContaining("Replaced"));
				assertEquals(20, textBookEditor.countLinesContaining(TextUtilities.GERMAN_TEST_STRING));
				assertEquals(61, textBookEditor.searchCount("e"));
				assertEquals(40, textBookEditor.searchAndReplace("abc", "def"));
				assertEquals(80, textBookEditor.searchCount("def"));
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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

				assertEquals(LineBreak.Windows, textBookEditor.getLineBreakType());

				assertEquals(17, textBookEditor.search("Line 17").getLine());
				assertEquals(1, textBookEditor.countLinesContaining("Line 17"));
				assertEquals(1, textBookEditor.searchAndReplace("Line 17", "Replaced", true, true, true, null, null));
				assertEquals(0, textBookEditor.countLinesContaining("Line 17"));
				assertEquals(1, textBookEditor.countLinesContaining("Replaced"));
				assertEquals(20, textBookEditor.countLinesContaining(TextUtilities.GERMAN_TEST_STRING));
				assertEquals(61, textBookEditor.searchCount("e"));
				assertEquals(40, textBookEditor.searchAndReplace("abc", "def"));
				assertEquals(80, textBookEditor.searchCount("def"));
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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

				assertEquals(LineBreak.Unix, textBookEditor.getLineBreakType());

				assertEquals(17, textBookEditor.search("Line 17").getLine());
				assertEquals(1, textBookEditor.countLinesContaining("Line 17"));
				assertEquals(1, textBookEditor.searchAndReplace("Line 17", "Replaced", true, true, true, null, null));
				assertEquals(0, textBookEditor.countLinesContaining("Line 17"));
				assertEquals(1, textBookEditor.countLinesContaining("Replaced"));
				assertEquals(20, textBookEditor.countLinesContaining(TextUtilities.GERMAN_TEST_STRING));
				assertEquals(61, textBookEditor.searchCount("e"));
				assertEquals(40, textBookEditor.searchAndReplace("abc", "def"));
				assertEquals(80, textBookEditor.searchCount("def"));

				textBookEditor.replaceLine(1, null);

				textBookEditor.insertLine(6, "NewLineX");

				textBookEditor.write();
			}

			final byte[] fileData = FileUtilities.readFileToByteArray(tempTextFile_src);
			assertTrue(fileData[0] == BOM.UTF_8.getBytes()[0] && fileData[1] == BOM.UTF_8.getBytes()[1] && fileData[2] == BOM.UTF_8.getBytes()[2]);
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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

				assertEquals(LineBreak.Windows, textBookEditor.getLineBreakType());

				assertEquals(17, textBookEditor.search("Line 17").getLine());
				assertEquals(1, textBookEditor.countLinesContaining("Line 17"));
				assertEquals(1, textBookEditor.searchAndReplace("Line 17", "Replaced", true, true, true, null, null));
				assertEquals(0, textBookEditor.countLinesContaining("Line 17"));
				assertEquals(1, textBookEditor.countLinesContaining("Replaced"));
				assertEquals(20, textBookEditor.countLinesContaining(TextUtilities.GERMAN_TEST_STRING));
				assertEquals(61, textBookEditor.searchCount("e"));
				assertEquals(40, textBookEditor.searchAndReplace("abc", "def"));
				assertEquals(80, textBookEditor.searchCount("def"));

				textBookEditor.replaceLine(1, null);

				textBookEditor.insertLine(6, "NewLineX");

				textBookEditor.write();
			}

			final byte[] fileData = FileUtilities.readFileToByteArray(tempTextFile_src);
			assertTrue(fileData[0] == BOM.UTF_8.getBytes()[0] && fileData[1] == BOM.UTF_8.getBytes()[1] && fileData[2] == BOM.UTF_8.getBytes()[2]);

			final String fileDataString = FileUtilities.readFileToString(tempTextFile_src, StandardCharsets.UTF_8);
			assertTrue(fileDataString.contains(LineBreak.Windows.toString()));
			assertFalse(fileDataString.replace(LineBreak.Windows.toString(), "").contains(LineBreak.Mac.toString()));
			assertFalse(fileDataString.replace(LineBreak.Windows.toString(), "").contains(LineBreak.Unix.toString()));
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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
				assertEquals(7, textBookEditor.search("NewLineX").getLine());
				assertEquals(8, textBookEditor.search("ReplacedLineX").getLine());
				assertEquals(17, textBookEditor.keepOnlyLinesContaining("LineX", true, false, 0, 1000));
				assertEquals(2, textBookEditor.getNumberOfLines());

				tempTextFile_dest = File.createTempFile("TextBookEditorTest" + "_", ".txt.zip");
				tempTextFile_dest.delete();
				textBookEditor.write(tempTextFile_dest);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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

				assertEquals(200000, textBookEditor.searchAndReplace("abcde", "Replaced"));

				tempTextFile_dest = File.createTempFile("TextBookEditorTest" + "_", ".txt.zip");
				tempTextFile_dest.delete();
				textBookEditor.write(tempTextFile_dest);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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

			assertEquals(TextUtilities.GERMAN_TEST_STRING + "\n", FileUtilities.readFileToString(tempTextFile_dest, StandardCharsets.UTF_8));
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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

				assertEquals(8, textBookEditor.getNumberOfLines());
				assertEquals(3, textBookEditor.search("InsertA").getLine());
				assertEquals(4, textBookEditor.search("InsertB").getLine());
				assertEquals(5, textBookEditor.search("InsertC").getLine());
				assertEquals(6, textBookEditor.search("Line 3:").getLine());
				assertEquals(8, textBookEditor.search("Line 5:").getLine());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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

				assertEquals(17, textBookEditor.getNumberOfLines());
				assertEquals(6, textBookEditor.search("Line 6:").getLine());
				assertEquals(7, textBookEditor.search("X1").getLine());
				assertEquals(8, textBookEditor.search("X2").getLine());
				assertEquals(9, textBookEditor.search("X3").getLine());
				assertEquals(10, textBookEditor.search("Line 23:").getLine());
				assertEquals(17, textBookEditor.search("Line 30:").getLine());
				assertNull(textBookEditor.search("Line 7:"));
				assertNull(textBookEditor.search("Line 21:"));
				assertNull(textBookEditor.search("Line 22:"));

				tempTextFile_dest = File.createTempFile("TextBookEditorTest" + "_", ".txt");
				tempTextFile_dest.delete();
				textBookEditor.write(tempTextFile_dest);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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

				assertEquals(10, textBookEditor.getNumberOfLines());
				assertEquals(1, textBookEditor.search("Line 1:").getLine());
				assertEquals(2, textBookEditor.search("Line 2:").getLine());
				assertEquals(3, textBookEditor.search("Line 13:").getLine());
				assertEquals(10, textBookEditor.search("Line 20:").getLine());
				assertNull(textBookEditor.search("Line 3:"));
				assertNull(textBookEditor.search("Line 12:"));

				textBookEditor.write();
				assertEquals(10, textBookEditor.getNumberOfLines());
				assertEquals(1, textBookEditor.countLinesContaining("Line 20:"));
				assertEquals(0, textBookEditor.countLinesContaining("Line 5:"));
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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
				assertEquals(5, textBookEditor.search("Line 5:", false, false, null, end).getLine());
				assertNull(textBookEditor.search("Line 6:", false, false, null, end));
				assertEquals(5, textBookEditor.searchCount("Line ", false, false, null, end));

				assertNull(textBookEditor.search("Line 4:", false, false, null, new TextPosition(3, 7)));
				assertEquals(0, textBookEditor.searchCount("Line 4:", false, false, null, new TextPosition(3, 7)));

				assertEquals(1, textBookEditor.countLinesContaining("Line 2:", true, false, 1, 3));
				assertEquals(0, textBookEditor.countLinesContaining("Line 4:", true, false, 1, 3));

				assertEquals(1, textBookEditor.searchAndReplace("Line 4:", "REPL4:", false, true, false, null, new TextPosition(4, 7)));
				assertEquals(4, textBookEditor.search("REPL4:").getLine());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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

				assertEquals(1, textBookEditor.countLinesContaining("Line 5:"));

				textBookEditor.replaceLine(5, "Changed5");
				textBookEditor.write();

				assertEquals(12, textBookEditor.getNumberOfLines());
				assertEquals(1, textBookEditor.countLinesContaining("Changed1"));
				assertEquals(1, textBookEditor.countLinesContaining("Changed5"));
				assertEquals(0, textBookEditor.countLinesContaining("Line 1:"));
				assertEquals(0, textBookEditor.countLinesContaining("Line 5:"));
				assertEquals(1, textBookEditor.countLinesContaining("Line 2:"));
				assertEquals(1, textBookEditor.countLinesContaining("Line 12:"));
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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

			assertEquals(originalLength, tempTextFile_src.length());
			assertEquals(originalModified, tempTextFile_src.lastModified());

			final String destContent = FileUtilities.readFileToString(tempTextFile_dest, StandardCharsets.UTF_8);
			assertTrue(destContent.contains("Changed3"));
			assertFalse(destContent.contains("Line 3:"));

			final String srcContent = FileUtilities.readFileToString(tempTextFile_src, StandardCharsets.UTF_8);
			assertTrue(srcContent.contains("Line 3:"));
			assertFalse(srcContent.contains("Changed3"));
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
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
				assertEquals(10, textBookEditor.getNumberOfPages());

				for (int page = 0; page < 10; page++) {
					textBookEditor.replaceLine((page * 5) + 1, "ChangedPage" + page);
				}

				assertEquals(50, textBookEditor.getNumberOfLines());
				for (int page = 0; page < 10; page++) {
					final long expectedLine = (page * 5) + 1;
					assertEquals(expectedLine, textBookEditor.search("ChangedPage" + page).getLine());
					assertEquals(0, textBookEditor.countLinesContaining("Line " + expectedLine + ":"));
				}
				assertEquals(1, textBookEditor.countLinesContaining("Line 2:"));

				textBookEditor.write();

				assertEquals(50, textBookEditor.getNumberOfLines());
				assertEquals(1, textBookEditor.countLinesContaining("ChangedPage9"));
				assertEquals(1, textBookEditor.countLinesContaining("Line 2:"));
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		} finally {
			if (tempTextFile_src != null && tempTextFile_src.exists()) {
				tempTextFile_src.delete();
			}
		}
	}
}
