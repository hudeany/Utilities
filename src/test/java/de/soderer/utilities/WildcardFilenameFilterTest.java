package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class WildcardFilenameFilterTest {
	private static final File DIRECTORY = new File("anyDirectory");

	private static boolean matches(final String pattern, final String name) {
		return new WildcardFilenameFilter(pattern).accept(DIRECTORY, name);
	}

	private static boolean matchesIgnoreCase(final String pattern, final String name) {
		return new WildcardFilenameFilter(pattern, false).accept(DIRECTORY, name);
	}

	@Test
	public void testExactPattern() {
		assertTrue(matches("readme.txt", "readme.txt"));
		assertFalse(matches("readme.txt", "readme.txt2"));
		assertFalse(matches("readme.txt", "readme.tx"));
		assertFalse(matches("readme.txt", "xreadme.txt"));
		assertTrue(matches("", ""));
		assertFalse(matches("", "a"));
		assertFalse(matches("a", ""));
	}

	@Test
	public void testStar() {
		assertTrue(matches("*.txt", "a.txt"));
		assertTrue(matches("*.txt", ".txt"));
		assertTrue(matches("*.txt", "a.b.txt"));
		assertFalse(matches("*.txt", "a.txt.bak"));
		assertFalse(matches("*.txt", "a.csv"));
		assertTrue(matches("data*", "data"));
		assertTrue(matches("data*", "data_2024.csv"));
		assertFalse(matches("data*", "dat"));
		assertTrue(matches("*", ""));
		assertTrue(matches("*", "anything.at.all"));
		assertTrue(matches("**", "x"));
		assertTrue(matches("a*b*c", "abc"));
		assertTrue(matches("a*b*c", "aXbYbZc"));
		assertFalse(matches("a*b*c", "aXbYbZ"));
		assertTrue(matches("*a*", "banana"));
		assertFalse(matches("*a*", "berry"));
	}

	@Test
	public void testQuestionMark() {
		assertTrue(matches("a?c", "abc"));
		assertTrue(matches("a?c", "a.c"));
		assertFalse(matches("a?c", "ac"));
		assertFalse(matches("a?c", "abbc"));
		assertTrue(matches("???", "abc"));
		assertFalse(matches("???", "ab"));
		assertFalse(matches("?", ""));
	}

	@Test
	public void testStarFollowedByQuestionMark() {
		// "*?" needs at least one character
		assertFalse(matches("*?", ""));
		assertTrue(matches("*?", "a"));
		assertTrue(matches("*?", "abc"));
		assertTrue(matches("*??.log", "ab.log"));
		assertFalse(matches("*??.log", "a.log"));
		assertTrue(matches("?*.log", "a.log"));
		assertTrue(matches("file_*_?.csv", "file_2024_01_x.csv"));
	}

	@Test
	public void testBacktracking() {
		// The first match of "ab" is not the right one
		assertTrue(matches("*ab", "aab"));
		assertTrue(matches("*abc", "ababc"));
		assertTrue(matches("*.tar.gz", "backup.tar.tar.gz"));
		assertFalse(matches("*.tar.gz", "backup.tar.gz.part"));
		assertTrue(matches("a*a*a", "aaa"));
		assertFalse(matches("a*a*a", "aa"));
	}

	@Test
	public void testPathologicalPatternIsFast() {
		// A naive recursive implementation takes exponential time here
		final String name = "a".repeat(5000);
		final long start = System.nanoTime();
		assertFalse(matches("*a*a*a*a*a*a*a*a*a*a*b", name));
		assertTrue(matches("*a*a*a*a*a*a*a*a*a*a*", name));
		final long durationMillis = (System.nanoTime() - start) / 1_000_000;
		assertTrue(durationMillis < 5000, "Matching took " + durationMillis + " ms");
	}

	@Test
	public void testCaseSensitivity() {
		assertFalse(matches("*.TXT", "a.txt"));
		assertTrue(matchesIgnoreCase("*.TXT", "a.txt"));
		assertTrue(matchesIgnoreCase("README.*", "readme.md"));
		assertTrue(matchesIgnoreCase("\u00C4RGER.TXT", "\u00E4rger.txt"));
		assertFalse(matchesIgnoreCase("a?c", "abd"));
		// Default constructors are case sensitive
		assertFalse(new WildcardFilenameFilter("A").accept(DIRECTORY, "a"));
		assertFalse(new WildcardFilenameFilter("A", "B").accept(DIRECTORY, "a"));
		assertFalse(new WildcardFilenameFilter(Collections.singletonList("A")).accept(DIRECTORY, "a"));
	}

	@Test
	public void testCaseInsensitiveLikeRegionMatches() {
		// Kelvin sign and Greek final sigma have no simple uppercase partner, regionMatches compares the lowercase values too
		assertTrue(matchesIgnoreCase("\u212A", "k"));
		assertTrue(matchesIgnoreCase("k", "\u212A"));
		assertTrue("\u212A".regionMatches(true, 0, "k", 0, 1));
		assertTrue(matchesIgnoreCase("\u03C2", "\u03A3"));
		assertTrue(matchesIgnoreCase("\u03C2", "\u03C3"));
		assertFalse(matches("\u212A", "k"));
	}

	@Test
	public void testMultiplePatterns() {
		final WildcardFilenameFilter filter = new WildcardFilenameFilter("*.txt", "*.csv");
		assertTrue(filter.accept(DIRECTORY, "a.txt"));
		assertTrue(filter.accept(DIRECTORY, "b.csv"));
		assertFalse(filter.accept(DIRECTORY, "c.xml"));

		final WildcardFilenameFilter arrayFilter = new WildcardFilenameFilter(new String[] { "*.TXT", "*.CSV" }, false);
		assertTrue(arrayFilter.accept(DIRECTORY, "a.txt"));
		assertTrue(arrayFilter.accept(DIRECTORY, "b.csv"));
		assertFalse(arrayFilter.accept(DIRECTORY, "c.xml"));

		final WildcardFilenameFilter listFilter = new WildcardFilenameFilter(Arrays.asList("*.TXT", "*.CSV"), false);
		assertTrue(listFilter.accept(DIRECTORY, "a.txt"));
		assertTrue(listFilter.accept(DIRECTORY, "b.csv"));
		assertFalse(listFilter.accept(DIRECTORY, "c.xml"));
	}

	@Test
	public void testNoPatternsAcceptsNothing() {
		assertFalse(new WildcardFilenameFilter(new String[0]).accept(DIRECTORY, "a.txt"));
		assertFalse(new WildcardFilenameFilter(new ArrayList<>()).accept(DIRECTORY, ""));
	}

	@Test
	public void testNullPatternInArrayIsIgnored() {
		final WildcardFilenameFilter filter = new WildcardFilenameFilter(new String[] { null, "*.txt" }, true);
		assertTrue(filter.accept(DIRECTORY, "a.txt"));
		assertFalse(filter.accept(DIRECTORY, "a.csv"));
	}

	@Test
	public void testNullArguments() {
		assertThrows(IllegalArgumentException.class, () -> new WildcardFilenameFilter((String) null));
		assertThrows(IllegalArgumentException.class, () -> new WildcardFilenameFilter((String) null, false));
		assertThrows(IllegalArgumentException.class, () -> new WildcardFilenameFilter((String[]) null));
		assertThrows(IllegalArgumentException.class, () -> new WildcardFilenameFilter((String[]) null, false));
		assertThrows(IllegalArgumentException.class, () -> new WildcardFilenameFilter((List<String>) null));
		assertThrows(IllegalArgumentException.class, () -> new WildcardFilenameFilter((List<String>) null, false));
	}

	@Test
	public void testPatternsAreCopied() {
		final String[] patterns = { "*.txt" };
		final WildcardFilenameFilter arrayFilter = new WildcardFilenameFilter(patterns);
		patterns[0] = "*.csv";
		assertTrue(arrayFilter.accept(DIRECTORY, "a.txt"));
		assertFalse(arrayFilter.accept(DIRECTORY, "a.csv"));

		final List<String> patternList = new ArrayList<>(Arrays.asList("*.txt"));
		final WildcardFilenameFilter listFilter = new WildcardFilenameFilter(patternList);
		patternList.set(0, "*.csv");
		patternList.add("*.xml");
		assertTrue(listFilter.accept(DIRECTORY, "a.txt"));
		assertFalse(listFilter.accept(DIRECTORY, "a.csv"));
		assertFalse(listFilter.accept(DIRECTORY, "a.xml"));
	}

	@Test
	public void testAcceptFileMatchesNameOnly() {
		final WildcardFilenameFilter filter = new WildcardFilenameFilter("*.txt");
		assertTrue(filter.accept(new File("some" + File.separator + "dir" + File.separator + "a.txt")));
		assertFalse(filter.accept(new File("dir.txt" + File.separator + "readme")));
		// A path in the pattern never matches, because only the name is checked
		assertFalse(new WildcardFilenameFilter("dir" + File.separator + "*.txt").accept(new File("dir" + File.separator + "a.txt")));
		// The directory argument of accept(File, String) is irrelevant
		assertTrue(filter.accept(null, "a.txt"));
		assertTrue(filter.accept(new File("x.csv"), "a.txt"));
	}

	@Test
	public void testUsableWithFileListing() throws IOException {
		final Path directory = Files.createTempDirectory("WildcardFilenameFilterTest");
		try {
			Files.createFile(directory.resolve("a.txt"));
			Files.createFile(directory.resolve("b.TXT"));
			Files.createFile(directory.resolve("c.csv"));
			Files.createDirectory(directory.resolve("sub.txt"));

			final FilenameFilter filenameFilter = new WildcardFilenameFilter("*.txt", false);
			final String[] names = directory.toFile().list(filenameFilter);
			Arrays.sort(names);
			assertEquals(Arrays.asList("a.txt", "b.TXT", "sub.txt"), Arrays.asList(names));

			final FileFilter fileFilter = new WildcardFilenameFilter("*.csv");
			final File[] files = directory.toFile().listFiles(fileFilter);
			assertEquals(1, files.length);
			assertEquals("c.csv", files[0].getName());
		} finally {
			try (java.util.stream.Stream<Path> paths = Files.walk(directory)) {
				paths.sorted(Collections.reverseOrder()).forEach(path -> path.toFile().delete());
			}
		}
	}

	@Test
	public void testToString() {
		assertTrue(new WildcardFilenameFilter("*.txt").toString().endsWith("(*.txt)"));
		assertTrue(new WildcardFilenameFilter("*.txt", "*.csv").toString().endsWith("(*.txt,*.csv)"));
		assertTrue(new WildcardFilenameFilter(new String[0]).toString().endsWith("()"));
	}
}
