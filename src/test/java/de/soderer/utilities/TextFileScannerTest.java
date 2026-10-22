package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link TextFileScanner}. As it is based on a copy of {@link java.util.Scanner}, most results are compared
 * with those of the JDK scanner for the same input.
 */
@SuppressWarnings("static-method")
public class TextFileScannerTest {
	private static final String[] COMPARISON_INPUTS = {
			"",
			"   ",
			"one two  three\tfour\nfive",
			"1 -2 +3 1,234 1.5 -0.25 1e3 true FALSE NaN Infinity 0x1F text 99999999999 128 32768",
			"line1\nline2\r\nline3\rline4\u2028line5\u2029line6\u0085line7",
			"\n\nempty lines\n\n",
			"  leading and trailing  \n",
			"1 2 3\n4 5 6\n",
			"123456789012345678901234567890 12.3456789012345678901234567890 -7",
	};

	/**
	 * Reads all tokens with the typed methods, preferring the most specific type, and logs the results. Used to compare
	 * TextFileScanner with java.util.Scanner.
	 */
	private static List<String> readTyped(final TextFileScanner scanner) {
		final List<String> log = new ArrayList<>();
		while (scanner.hasNext()) {
			if (scanner.hasNextByte()) {
				log.add("byte:" + scanner.nextByte());
			} else if (scanner.hasNextShort()) {
				log.add("short:" + scanner.nextShort());
			} else if (scanner.hasNextInt()) {
				log.add("int:" + scanner.nextInt());
			} else if (scanner.hasNextLong()) {
				log.add("long:" + scanner.nextLong());
			} else if (scanner.hasNextBigInteger()) {
				log.add("bigInteger:" + scanner.nextBigInteger());
			} else if (scanner.hasNextFloat()) {
				log.add("float:" + scanner.nextFloat());
			} else if (scanner.hasNextDouble()) {
				log.add("double:" + scanner.nextDouble());
			} else if (scanner.hasNextBigDecimal()) {
				log.add("bigDecimal:" + scanner.nextBigDecimal());
			} else if (scanner.hasNextBoolean()) {
				log.add("boolean:" + scanner.nextBoolean());
			} else {
				log.add("token:" + scanner.next());
			}
		}
		return log;
	}

	private static List<String> readTyped(final Scanner scanner) {
		final List<String> log = new ArrayList<>();
		while (scanner.hasNext()) {
			if (scanner.hasNextByte()) {
				log.add("byte:" + scanner.nextByte());
			} else if (scanner.hasNextShort()) {
				log.add("short:" + scanner.nextShort());
			} else if (scanner.hasNextInt()) {
				log.add("int:" + scanner.nextInt());
			} else if (scanner.hasNextLong()) {
				log.add("long:" + scanner.nextLong());
			} else if (scanner.hasNextBigInteger()) {
				log.add("bigInteger:" + scanner.nextBigInteger());
			} else if (scanner.hasNextFloat()) {
				log.add("float:" + scanner.nextFloat());
			} else if (scanner.hasNextDouble()) {
				log.add("double:" + scanner.nextDouble());
			} else if (scanner.hasNextBigDecimal()) {
				log.add("bigDecimal:" + scanner.nextBigDecimal());
			} else if (scanner.hasNextBoolean()) {
				log.add("boolean:" + scanner.nextBoolean());
			} else {
				log.add("token:" + scanner.next());
			}
		}
		return log;
	}

	private static List<String> readLines(final TextFileScanner scanner) {
		final List<String> lines = new ArrayList<>();
		while (scanner.hasNextLine()) {
			lines.add(scanner.nextLine());
		}
		return lines;
	}

	private static List<String> readLines(final Scanner scanner) {
		final List<String> lines = new ArrayList<>();
		while (scanner.hasNextLine()) {
			lines.add(scanner.nextLine());
		}
		return lines;
	}

	@Test
	public void testTypedTokensLikeJdkScanner() {
		for (final Locale locale : new Locale[] { Locale.US, Locale.GERMANY, Locale.FRANCE, Locale.ROOT }) {
			for (final String input : COMPARISON_INPUTS) {
				final List<String> expected = readTyped(new Scanner(input).useLocale(locale));
				final List<String> actual = readTyped(new TextFileScanner(input).useLocale(locale));
				assertEquals(expected, actual, locale + ": " + input);
			}
		}
	}

	@Test
	public void testLinesLikeJdkScanner() {
		for (final String input : COMPARISON_INPUTS) {
			assertEquals(readLines(new Scanner(input)), readLines(new TextFileScanner(input)), input);
		}
	}

	@Test
	public void testRadixLikeJdkScanner() {
		final String input = "ff 101 z 7fffffff 80000000";
		for (final int radix : new int[] { 2, 16, 36 }) {
			assertEquals(readTyped(new Scanner(input).useLocale(Locale.US).useRadix(radix)), readTyped(new TextFileScanner(input).useLocale(Locale.US).useRadix(radix)), "radix " + radix);
		}
		final TextFileScanner scanner = new TextFileScanner("ff 11");
		assertEquals(255, scanner.nextInt(16));
		assertEquals(3, scanner.nextInt(2));
		assertEquals(10, scanner.radix());
		assertThrows(IllegalArgumentException.class, () -> scanner.useRadix(1));
		assertThrows(IllegalArgumentException.class, () -> scanner.useRadix(37));
	}

	@Test
	public void testLocaleNumbers() {
		final TextFileScanner germanScanner = new TextFileScanner("1.234,5 -7 1.000").useLocale(Locale.GERMANY);
		assertEquals(1234.5, germanScanner.nextDouble(), 0.0);
		assertEquals(-7, germanScanner.nextInt());
		assertEquals(1000L, germanScanner.nextLong());
		assertEquals(Locale.GERMANY, germanScanner.locale());

		final TextFileScanner usScanner = new TextFileScanner("1,234.5 12345678901234567890 0.1").useLocale(Locale.US);
		assertEquals(1234.5f, usScanner.nextFloat(), 0.0f);
		assertEquals(new BigInteger("12345678901234567890"), usScanner.nextBigInteger());
		assertEquals(new BigDecimal("0.1"), usScanner.nextBigDecimal());
	}

	@Test
	public void testNumberRanges() {
		final TextFileScanner scanner = new TextFileScanner("127 128 32767 32768 2147483647 2147483648").useLocale(Locale.US);
		assertEquals(127, scanner.nextByte());
		assertFalse(scanner.hasNextByte());
		assertEquals(128, scanner.nextShort());
		assertEquals(32767, scanner.nextShort());
		assertFalse(scanner.hasNextShort());
		assertEquals(32768, scanner.nextInt());
		assertEquals(Integer.MAX_VALUE, scanner.nextInt());
		assertFalse(scanner.hasNextInt());
		assertEquals(2147483648L, scanner.nextLong());
	}

	@Test
	public void testInputMismatchDoesNotConsumeToken() {
		final TextFileScanner scanner = new TextFileScanner("abc 1").useLocale(Locale.US);
		assertThrows(InputMismatchException.class, scanner::nextInt);
		assertThrows(InputMismatchException.class, scanner::nextBoolean);
		assertEquals("abc", scanner.next());
		assertEquals(1, scanner.nextInt());
		assertThrows(NoSuchElementException.class, scanner::next);
		assertThrows(NoSuchElementException.class, scanner::nextInt);
	}

	@Test
	public void testNextLine() {
		final TextFileScanner scanner = new TextFileScanner("first line\r\nsecond\n\nlast");
		assertTrue(scanner.hasNextLine());
		assertEquals("first line", scanner.nextLine());
		assertEquals("second", scanner.nextLine());
		assertEquals("", scanner.nextLine());
		assertEquals("last", scanner.nextLine());
		assertFalse(scanner.hasNextLine());
		assertThrows(NoSuchElementException.class, scanner::nextLine);
	}

	@Test
	public void testMixedTokensAndLines() {
		final TextFileScanner scanner = new TextFileScanner("42 rest of line\nnext line").useLocale(Locale.US);
		assertEquals(42, scanner.nextInt());
		assertEquals(" rest of line", scanner.nextLine());
		assertEquals("next", scanner.next());
		assertEquals(" line", scanner.nextLine());
	}

	@Test
	public void testDelimiter() {
		final TextFileScanner scanner = new TextFileScanner("a;b;;c").useDelimiter(";");
		assertEquals(";", scanner.delimiter().pattern());
		assertEquals("a", scanner.next());
		assertEquals("b", scanner.next());
		assertEquals("", scanner.next());
		assertEquals("c", scanner.next());
		assertFalse(scanner.hasNext());

		final Pattern commaPattern = Pattern.compile("\\s*,\\s*");
		final TextFileScanner patternScanner = new TextFileScanner("x , y,z").useDelimiter(commaPattern);
		assertSame(commaPattern, patternScanner.delimiter());
		assertEquals("x", patternScanner.next());
		assertEquals("y", patternScanner.next());
		assertEquals("z", patternScanner.next());
	}

	@Test
	public void testPatternMethods() {
		final TextFileScanner scanner = new TextFileScanner("abc 123 def");
		assertTrue(scanner.hasNext("[a-z]+"));
		assertFalse(scanner.hasNext("\\d+"));
		assertEquals("abc", scanner.next("[a-z]+"));
		assertThrows(InputMismatchException.class, () -> scanner.next("[a-z]+"));
		assertEquals("123", scanner.next(Pattern.compile("\\d+")));
		assertTrue(scanner.hasNext(Pattern.compile("d.f")));
		assertEquals("def", scanner.next(Pattern.compile("d.f")));
	}

	@Test
	public void testFindAndSkip() {
		final TextFileScanner scanner = new TextFileScanner("key=value; other=1\nsecond line key=2");
		assertEquals("key=value", scanner.findInLine("\\w+=\\w+"));
		assertEquals("other=1", scanner.findInLine(Pattern.compile("\\w+=\\w+")));
		// Not found in the rest of the current line
		assertNull(scanner.findInLine("key"));
		assertEquals("key=2", scanner.findWithinHorizon("key=\\d", 0));
		assertThrows(IllegalArgumentException.class, () -> scanner.findWithinHorizon("x", -1));

		final TextFileScanner horizonScanner = new TextFileScanner("aaaaab");
		assertNull(horizonScanner.findWithinHorizon("b", 3));
		assertEquals("b", horizonScanner.findWithinHorizon("b", 10));

		final TextFileScanner skipScanner = new TextFileScanner("   # comment\nvalue");
		skipScanner.skip("\\s*#[^\n]*\n");
		assertEquals("value", skipScanner.next());
		assertThrows(NoSuchElementException.class, () -> new TextFileScanner("abc").skip("x"));
		assertEquals("abc", new TextFileScanner("abc").skip(Pattern.compile("[ \t]*")).next());
	}

	@Test
	public void testMatchResultAndPositions() {
		final TextFileScanner scanner = new TextFileScanner("ab cde fg");
		assertThrows(IllegalStateException.class, scanner::match);
		assertThrows(IllegalStateException.class, scanner::getLastMatchStart);

		assertEquals("ab", scanner.next());
		assertEquals(0, scanner.getLastMatchStart());
		assertEquals(2, scanner.getLastMatchEnd());
		assertEquals("cde", scanner.next());
		assertEquals(3, scanner.getLastMatchStart());
		assertEquals(6, scanner.getLastMatchEnd());
		assertEquals("cde", scanner.match().group());
		assertEquals(9, scanner.getDataRead());
	}

	@Test
	public void testPositionsInLargeInput() {
		// Larger than the internal buffer, so positions must be correct after compacting and growing the buffer
		final StringBuilder text = new StringBuilder();
		final List<Long> expectedPositions = new ArrayList<>();
		for (int i = 0; i < 5000; i++) {
			if (i % 97 == 0) {
				expectedPositions.add((long) text.length());
				text.append("MARK");
			} else {
				text.append(i % 10 == 0 ? '\n' : 'a');
			}
		}

		final TextFileScanner scanner = new TextFileScanner(new StringReader(text.toString()));
		final List<Long> foundPositions = new ArrayList<>();
		while (scanner.findWithinHorizon("MARK", 0) != null) {
			foundPositions.add(scanner.getLastMatchStart());
			assertEquals(scanner.getLastMatchStart() + 4, scanner.getLastMatchEnd());
		}
		assertEquals(expectedPositions, foundPositions);
		assertEquals(text.length(), scanner.getDataRead());
	}

	@Test
	public void testLongLineLargerThanBuffer() {
		final StringBuilder longLine = new StringBuilder();
		for (int i = 0; i < 3000; i++) {
			longLine.append((char) ('a' + i % 26));
		}
		final TextFileScanner scanner = new TextFileScanner(longLine + "\nshort");
		assertEquals(longLine.toString(), scanner.nextLine());
		assertEquals("short", scanner.nextLine());
	}

	@SuppressWarnings("unused")
	@Test
	public void testSourcesAndEncodings() throws Exception {
		final String text = "Gr\u00FC\u00DFe \u20AC";
		final File file = File.createTempFile("TextFileScannerTest", ".txt");
		file.deleteOnExit();
		Files.write(file.toPath(), text.getBytes(StandardCharsets.UTF_8));

		assertEquals(text, readAllLines(new TextFileScanner(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)))));
		assertEquals(text, readAllLines(new TextFileScanner(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_16LE)), StandardCharsets.UTF_16LE)));
		assertEquals(text, readAllLines(new TextFileScanner(file, "UTF-8")));
		try (FileInputStream inputStream = new FileInputStream(file)) {
			assertEquals(text, readAllLines(new TextFileScanner(inputStream.getChannel(), "UTF-8")));
		}
		final String windowsText = "Gr\u00FC\u00DFe";
		Files.write(file.toPath(), windowsText.getBytes(Charset.forName("windows-1252")));
		assertEquals(windowsText, readAllLines(new TextFileScanner(file, "windows-1252")));

		assertThrows(IllegalArgumentException.class, () -> {
			try (FileInputStream inputStream = new FileInputStream(file)) {
				new TextFileScanner(inputStream.getChannel(), "NO-SUCH-ENCODING");
			}
		});
		assertThrows(java.io.FileNotFoundException.class, () -> new TextFileScanner(new File(file.getParentFile(), "does-not-exist-" + System.nanoTime())));
		assertThrows(NullPointerException.class, () -> new TextFileScanner((java.io.InputStream) null, StandardCharsets.UTF_8));
	}

	private static String readAllLines(final TextFileScanner scanner) {
		final StringBuilder result = new StringBuilder();
		while (scanner.hasNextLine()) {
			if (result.length() > 0) {
				result.append('\n');
			}
			result.append(scanner.nextLine());
		}
		scanner.close();
		return result.toString();
	}

	@Test
	public void testClose() {
		final List<String> closeCalls = new ArrayList<>();
		final StringReader reader = new StringReader("a b") {
			@Override
			public void close() {
				closeCalls.add("closed");
				super.close();
			}
		};
		final TextFileScanner scanner = new TextFileScanner(reader);
		assertEquals("a", scanner.next());
		scanner.close();
		assertEquals(1, closeCalls.size());
		// Closing again does nothing
		scanner.close();
		assertEquals(1, closeCalls.size());

		assertThrows(IllegalStateException.class, scanner::hasNext);
		assertThrows(IllegalStateException.class, scanner::next);
		assertThrows(IllegalStateException.class, () -> scanner.findInLine("a"));
		assertThrows(IllegalStateException.class, () -> scanner.findWithinHorizon("a", 0));
		assertThrows(IllegalStateException.class, () -> scanner.skip("a"));
	}

	@Test
	public void testIoExceptionOfSource() {
		final Readable failingSource = buffer -> {
			throw new IOException("read failed");
		};
		final TextFileScanner scanner = new TextFileScanner(failingSource);
		assertNull(scanner.ioException());
		// A read error is treated as end of data
		assertFalse(scanner.hasNext());
		assertEquals("read failed", scanner.ioException().getMessage());
	}

	@Test
	public void testResetAndToString() {
		final TextFileScanner scanner = new TextFileScanner("a;b c").useDelimiter(";").useRadix(16);
		scanner.reset();
		assertEquals("\\p{javaWhitespace}+", scanner.delimiter().pattern());
		assertEquals(10, scanner.radix());
		assertEquals("a;b", scanner.next());
		assertTrue(scanner.toString().contains("[delimiters="), scanner.toString());
	}

	@Test
	public void testIteratorRemoveIsUnsupported() {
		assertThrows(UnsupportedOperationException.class, () -> new TextFileScanner("a").remove());
	}

	@Test
	public void testNullArguments() {
		final TextFileScanner scanner = new TextFileScanner("a");
		assertThrows(NullPointerException.class, () -> scanner.hasNext((Pattern) null));
		assertThrows(NullPointerException.class, () -> scanner.next((Pattern) null));
		assertThrows(NullPointerException.class, () -> scanner.findInLine((Pattern) null));
		assertThrows(NullPointerException.class, () -> scanner.findWithinHorizon((Pattern) null, 0));
		assertThrows(NullPointerException.class, () -> scanner.skip((Pattern) null));
		assertThrows(NullPointerException.class, () -> new TextFileScanner((Readable) null));
	}
}
