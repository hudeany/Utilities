package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import de.soderer.utilities.FileComparer.OutputMode;

@SuppressWarnings("static-method")
public class FileComparerTest {
	private static final String HEADER = "--- one\n+++ two\n";

	@TempDir
	Path tempDir;

	@Test
	public void compareExactly() throws Exception {
		final File file1 = createFile("1.txt", "abc");
		final File file2 = createFile("2.txt", "abc");
		final File file3 = createFile("3.txt", "abd");
		final File file4 = createFile("4.txt", "abcd");
		final File empty1 = createFile("e1.txt", "");
		final File empty2 = createFile("e2.txt", "");

		assertTrue(FileComparer.compareExactly(file1, file2));
		assertTrue(FileComparer.compareExactly(file1, file1));
		assertTrue(FileComparer.compareExactly(empty1, empty2));
		assertFalse(FileComparer.compareExactly(file1, file3));
		assertFalse(FileComparer.compareExactly(file1, file4));
		assertFalse(FileComparer.compareExactly(file4, file1));
		assertFalse(FileComparer.compareExactly(empty1, file1));
	}

	@Test
	public void compareExactly_largeFilesDifferingAtEnd() throws Exception {
		final byte[] data1 = new byte[100_000];
		final byte[] data2 = data1.clone();
		data2[data2.length - 1] = 1;

		assertFalse(FileComparer.compareExactly(createFile("1.bin", data1), createFile("2.bin", data2)));
		assertTrue(FileComparer.compareExactly(createFile("3.bin", data1), createFile("4.bin", data1.clone())));
	}

	@Test
	public void compareExactly_missingFile() throws Exception {
		final File file = createFile("1.txt", "abc");

		assertThrows(IOException.class, () -> FileComparer.compareExactly(file, tempDir.resolve("missing.txt").toFile()));
	}

	@Test
	public void generateUnifiedDiff_equalTexts() throws Exception {
		assertEquals(HEADER + " a\n b\n", diff("a\nb", "a\nb"));
		assertEquals(HEADER, diff("", ""));
	}

	@Test
	public void generateUnifiedDiff_changedLine() throws Exception {
		assertEquals(HEADER + " a\n-b\n+x\n c\n", diff("a\nb\nc", "a\nx\nc"));
	}

	@Test
	public void generateUnifiedDiff_addedAndRemovedTails() throws Exception {
		assertEquals(HEADER + " a\n+b\n", diff("a", "a\nb"));
		assertEquals(HEADER + " a\n-b\n", diff("a\nb", "a"));
		assertEquals(HEADER + "+x\n", diff("", "x"));
		assertEquals(HEADER + "-x\n", diff("x", ""));
	}

	@Test
	public void generateUnifiedDiff_nothingInCommon() throws Exception {
		assertEquals(HEADER + "-a\n-b\n+c\n", diff("a\nb", "c"));
	}

	@Test
	public void generateUnifiedDiff_insertedLines() throws Exception {
		assertEquals(HEADER + " a\n+x\n+y\n b\n", diff("a\nb", "a\nx\ny\nb"));
	}

	@Test
	public void generateUnifiedDiff_differentLinebreaks() throws Exception {
		assertEquals(HEADER + " a\n b\n", diff("a\r\nb\r\n", "a\nb"));
	}

	@Test
	public void generateUnifiedDiff_blockSensitive() throws Exception {
		// Synchronizes at the largest common block "b c d"
		assertEquals(HEADER + "-a\n+e\n b\n c\n d\n-e\n+a\n", FileComparer.generateUnifiedDiff("one", "a\nb\nc\nd\ne", "two", "e\nb\nc\nd\na", false, true, OutputMode.All));
	}

	@Test
	public void generateUnifiedDiff_straight() throws Exception {
		// Synchronizes at the nearest equal line "a"
		assertEquals(HEADER + "+e\n+b\n+c\n+d\n a\n-b\n-c\n-d\n-e\n", FileComparer.generateUnifiedDiff("one", "a\nb\nc\nd\ne", "two", "e\nb\nc\nd\na", false, false, OutputMode.All));
	}

	@Test
	public void generateUnifiedDiff_ignoreLeadingAndTrailingWhitespaces() throws Exception {
		// Common lines are output as in the first text
		assertEquals(HEADER + "   a\n b\n", FileComparer.generateUnifiedDiff("one", "  a\nb", "two", "a \nb", true, true, OutputMode.All));
		assertEquals(HEADER + "-  a\n+a \n b\n", FileComparer.generateUnifiedDiff("one", "  a\nb", "two", "a \nb", false, true, OutputMode.All));
	}

	@Test
	public void generateUnifiedDiff_outputModes() throws Exception {
		final String text1 = "a\nb\nc";
		final String text2 = "a\nx\nc";

		assertEquals(HEADER + "-b\n+x\n", FileComparer.generateUnifiedDiff("one", text1, "two", text2, false, true, OutputMode.DifferencesOnly));
		assertEquals(HEADER + " a\n c\n", FileComparer.generateUnifiedDiff("one", text1, "two", text2, false, true, OutputMode.CommonalitiesOnly));
		assertEquals(HEADER + " a\n-b\n+x\n c\n", FileComparer.generateUnifiedDiff("one", text1, "two", text2, false, true, null));
		// Tails are differences
		assertEquals(HEADER + " a\n", FileComparer.generateUnifiedDiff("one", "a", "two", "a\nb", false, true, OutputMode.CommonalitiesOnly));
		assertEquals(HEADER + "+b\n", FileComparer.generateUnifiedDiff("one", "a", "two", "a\nb", false, true, OutputMode.DifferencesOnly));
	}

	@Test
	public void generateUnifiedDiff_nullText() {
		assertThrows(Exception.class, () -> FileComparer.generateUnifiedDiff("one", null, "two", "", false, true, OutputMode.All));
		assertThrows(Exception.class, () -> FileComparer.generateUnifiedDiff("one", "", "two", null, false, true, OutputMode.All));
	}

	@Test
	public void generateUnifiedDiff_files() throws Exception {
		final File file1 = createFile("1.txt", "a\nb\nc");
		final File file2 = createFile("2.txt", "a\nx\nc\n");

		final List<String> lines = TextUtilities.getLines(FileComparer.generateUnifiedDiff(file1, file2));

		assertTrue(lines.get(0).startsWith("--- " + file1.getAbsolutePath() + " "), lines.get(0));
		assertTrue(lines.get(1).startsWith("+++ " + file2.getAbsolutePath() + " "), lines.get(1));
		assertEquals(List.of(" a", "-b", "+x", " c"), lines.subList(2, lines.size()));
	}

	@Test
	public void generateUnifiedDiff_filesWithOptions() throws Exception {
		final File file1 = createFile("1.txt", "a\nb\nc");
		final File file2 = createFile("2.txt", "a\nx\nc");

		final List<String> lines = TextUtilities.getLines(FileComparer.generateUnifiedDiff(file1, file2, false, false, OutputMode.DifferencesOnly));

		assertEquals(List.of("-b", "+x"), lines.subList(2, lines.size()));
	}

	@Test
	public void generateUnifiedDiff_missingFile() throws Exception {
		final File file = createFile("1.txt", "a");
		final File missing = tempDir.resolve("missing.txt").toFile();

		assertThrows(Exception.class, () -> FileComparer.generateUnifiedDiff(missing, file));
		assertThrows(Exception.class, () -> FileComparer.generateUnifiedDiff(file, missing));
	}

	@Test
	public void findNextEquationStraigth() {
		// Equal sums of positions, the search along the first list wins
		assertTuple(1, 2, FileComparer.findNextEquationStraigth(0, List.of("a", "b", "c"), 0, List.of("x", "c", "b")));
		assertTuple(0, 1, FileComparer.findNextEquationStraigth(0, List.of("a", "b"), 0, List.of("x", "a")));
		assertTuple(3, 0, FileComparer.findNextEquationStraigth(0, List.of("x", "y", "z", "a"), 0, List.of("a", "q", "r", "s", "t", "x")));
		assertTuple(2, 2, FileComparer.findNextEquationStraigth(1, List.of("a", "b", "a"), 1, List.of("a", "c", "a")));
		assertNull(FileComparer.findNextEquationStraigth(0, List.of("a"), 0, List.of("b")));
		assertNull(FileComparer.findNextEquationStraigth(0, List.<String> of(), 0, List.of("b")));
	}

	@Test
	public void findLargestBlockEquation() {
		assertTuple(1, 0, FileComparer.findLargestBlockEquation(0, -1, List.of("x", "a", "b", "c", "y", "a", "b"), 0, -1, List.of("a", "b", "c", "z", "a", "b")));
		// Blocks of equal size: first one by index in the first list
		assertTuple(0, 2, FileComparer.findLargestBlockEquation(0, -1, List.of("a", "x", "b"), 0, -1, List.of("b", "y", "a")));
		// Start positions limit the ranges
		assertTuple(2, 0, FileComparer.findLargestBlockEquation(2, -1, List.of("a", "b", "a", "b"), 0, -1, List.of("a", "b")));
		// End positions limit the block length
		assertTuple(0, 0, FileComparer.findLargestBlockEquation(0, 0, List.of("a", "b", "c", "x", "y"), 0, 99, List.of("a", "b", "c", "x", "y")));
		assertTuple(2, 2, FileComparer.findLargestBlockEquation(0, 4, List.of("a", "q", "c", "x", "y"), 0, 4, List.of("a", "r", "c", "x", "y")));
		// Repeated items within a block
		assertTuple(1, 0, FileComparer.findLargestBlockEquation(0, -1, List.of("b", "a", "a", "a"), 0, -1, List.of("a", "a", "a", "b")));
		assertNull(FileComparer.findLargestBlockEquation(0, -1, List.of("a"), 0, -1, List.of("b")));
	}

	@Test
	public void findNextEquationByBlock() {
		assertTuple(1, 0, FileComparer.findNextEquationByBlock(0, List.of("x", "a", "b", "c"), 0, List.of("a", "b", "c", "x")));
		assertTuple(1, 1, FileComparer.findNextEquationByBlock(0, List.of("a", "b", "c", "d", "e"), 0, List.of("e", "b", "c", "d", "a")));
		assertNull(FileComparer.findNextEquationByBlock(0, List.of("a"), 0, List.of("b")));
	}

	private static String diff(final String text1, final String text2) throws Exception {
		return FileComparer.generateUnifiedDiff("one", text1, "two", text2, false, true, OutputMode.All);
	}

	private File createFile(final String name, final String content) throws IOException {
		return createFile(name, content.getBytes(StandardCharsets.UTF_8));
	}

	private File createFile(final String name, final byte[] content) throws IOException {
		final Path path = tempDir.resolve(name);
		Files.write(path, content);
		return path.toFile();
	}

	private static void assertTuple(final int expectedFirst, final int expectedSecond, final Tuple<Integer, Integer> tuple) {
		assertNotNull(tuple, "tuple");
		assertEquals(Integer.valueOf(expectedFirst), tuple.getFirst(), "first");
		assertEquals(Integer.valueOf(expectedSecond), tuple.getSecond(), "second");
	}
}
