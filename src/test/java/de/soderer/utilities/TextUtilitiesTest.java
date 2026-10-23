package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.soderer.utilities.TextUtilities.LineBreak;

public class TextUtilitiesTest {
	@SuppressWarnings("static-method")
	@Test
	public void lineBreak_toString() {
		assertEquals("\n", LineBreak.Unix.toString());
		assertEquals("\r\n", LineBreak.Windows.toString());
		assertEquals("\r", LineBreak.Mac.toString());
		assertNull(LineBreak.Unknown.toString());
		assertNull(LineBreak.Mixed.toString());
	}

	@SuppressWarnings("static-method")
	@Test
	public void lineBreak_getLineBreakTypeByName() {
		assertEquals(LineBreak.Windows, LineBreak.getLineBreakTypeByName("windows"));
		assertEquals(LineBreak.Unix, LineBreak.getLineBreakTypeByName("UNIX"));
		assertEquals(LineBreak.Mac, LineBreak.getLineBreakTypeByName("Mac"));
		assertEquals(LineBreak.Unknown, LineBreak.getLineBreakTypeByName("Mixed"));
		assertEquals(LineBreak.Unknown, LineBreak.getLineBreakTypeByName(null));
	}

	@SuppressWarnings("static-method")
	@Test
	public void trimStringToLengthAlignedRight() {
		assertEquals("  abc", TextUtilities.trimStringToLengthAlignedRight("abc", 5));
		assertEquals("abc", TextUtilities.trimStringToLengthAlignedRight("abc", 3));
		assertEquals("def", TextUtilities.trimStringToLengthAlignedRight("abcdef", 3));
	}

	@SuppressWarnings("static-method")
	@Test
	public void trimStringToLengthAlignedLeft() {
		assertEquals("abc  ", TextUtilities.trimStringToLengthAlignedLeft("abc", 5));
		assertEquals("abc", TextUtilities.trimStringToLengthAlignedLeft("abc", 3));
		assertEquals("abc", TextUtilities.trimStringToLengthAlignedLeft("abcdef", 3));
	}

	@SuppressWarnings("static-method")
	@Test
	public void trimStringToLengthAligned_lengthZero() {
		assertEquals("", TextUtilities.trimStringToLengthAlignedLeft("", 0));
		assertEquals("", TextUtilities.trimStringToLengthAlignedRight("", 0));
		assertEquals("", TextUtilities.trimStringToLengthAlignedLeft("abc", 0));
		assertEquals("", TextUtilities.trimStringToLengthAlignedRight("abc", 0));
	}

	@SuppressWarnings("static-method")
	@Test
	public void chopToChunks() {
		assertEquals(List.of("abc", "def", "g"), TextUtilities.chopToChunks("abcdefg", 3));
		assertEquals(List.of("abc"), TextUtilities.chopToChunks("abc", 3));
		assertEquals(List.of(), TextUtilities.chopToChunks("", 3));
		assertThrows(IllegalArgumentException.class, () -> TextUtilities.chopToChunks("abc", 0));
	}

	@SuppressWarnings("static-method")
	@Test
	public void trimToMaxNumberOfLines() {
		assertEquals("a\nb\n...", TextUtilities.trimToMaxNumberOfLines("a\nb\nc", 2));
		assertEquals("a\nb", TextUtilities.trimToMaxNumberOfLines("a\nb", 2));
		assertEquals("a\nb\nc", TextUtilities.trimToMaxNumberOfLines("a\nb\nc", 5));
		// Nothing is cut off after the trailing linebreak
		assertEquals("a\n", TextUtilities.trimToMaxNumberOfLines("a\n", 1));
		// Empty first line
		assertEquals("\n...", TextUtilities.trimToMaxNumberOfLines("\na", 1));
	}

	@SuppressWarnings("static-method")
	@Test
	public void trimToMaxNumberOfLines_keepsLinebreakType() {
		assertEquals("a\r\n...", TextUtilities.trimToMaxNumberOfLines("a\r\nb\r\nc", 1));
		assertEquals("a\r...", TextUtilities.trimToMaxNumberOfLines("a\rb\rc", 1));
	}

	@SuppressWarnings("static-method")
	@Test
	public void detectLinebreakType() {
		assertEquals(LineBreak.Unix, TextUtilities.detectLinebreakType("a\nb"));
		assertEquals(LineBreak.Windows, TextUtilities.detectLinebreakType("a\r\nb"));
		assertEquals(LineBreak.Mac, TextUtilities.detectLinebreakType("a\rb"));
		assertEquals(LineBreak.Mixed, TextUtilities.detectLinebreakType("a\nb\r\nc"));
		assertEquals(LineBreak.Unknown, TextUtilities.detectLinebreakType("abc"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void normalizeLineBreaks() {
		final String mixed = "a\r\nb\rc\nd";

		assertNull(TextUtilities.normalizeLineBreaks(null, LineBreak.Unix));
		assertEquals("a\nb\nc\nd", TextUtilities.normalizeLineBreaks(mixed, LineBreak.Unix));
		assertEquals("a\r\nb\r\nc\r\nd", TextUtilities.normalizeLineBreaks(mixed, LineBreak.Windows));
		assertEquals("a\rb\rc\rd", TextUtilities.normalizeLineBreaks(mixed, LineBreak.Mac));
		assertEquals("a\nb\nc\nd", TextUtilities.normalizeLineBreaks(mixed, LineBreak.Unknown));
		assertEquals("a\nb\nc\nd", TextUtilities.normalizeLineBreaks(mixed, LineBreak.Mixed));
		// LF followed by CR are two linebreaks
		assertEquals("a\n\nb", TextUtilities.normalizeLineBreaks("a\n\rb", LineBreak.Unix));
	}

	@SuppressWarnings("static-method")
	@Test
	public void normalizeLineBreaksForCurrentSystem() {
		final LineBreak expectedType = "\r\n".equals(System.lineSeparator()) ? LineBreak.Windows : LineBreak.Unix;

		assertEquals(TextUtilities.normalizeLineBreaks("a\rb\nc", expectedType), TextUtilities.normalizeLineBreaksForCurrentSystem("a\rb\nc"));
		assertNull(TextUtilities.normalizeLineBreaksForCurrentSystem(null));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getLines() throws IOException {
		assertEquals(List.of("a", "b", "c", "d"), TextUtilities.getLines("a\r\nb\rc\nd\n"));
		assertEquals(List.of("a", "", "b"), TextUtilities.getLines("a\n\nb"));
		assertEquals(List.of(), TextUtilities.getLines(""));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getLineCount() {
		assertEquals(0, TextUtilities.getLineCount(null));
		assertEquals(1, TextUtilities.getLineCount(""));
		assertEquals(1, TextUtilities.getLineCount("a"));
		assertEquals(2, TextUtilities.getLineCount("a\nb"));
		assertEquals(2, TextUtilities.getLineCount("a\nb\n"));
		assertEquals(2, TextUtilities.getLineCount("a\r\nb"));
		assertEquals(3, TextUtilities.getLineCount("a\rb\nc"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void countWords() {
		assertEquals(0, TextUtilities.countWords(""));
		assertEquals(0, TextUtilities.countWords("   "));
		assertEquals(1, TextUtilities.countWords("a"));
		assertEquals(1, TextUtilities.countWords("word"));
		assertEquals(2, TextUtilities.countWords("hello world"));
		assertEquals(2, TextUtilities.countWords("  hello   world  "));
		assertEquals(3, TextUtilities.countWords("hello\tworld\nfoo"));
		assertEquals(2, TextUtilities.countWords("ab c"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getStartIndexOfLine() {
		assertEquals(-1, TextUtilities.getStartIndexOfLine(null, 1));
		assertEquals(-1, TextUtilities.getStartIndexOfLine("a", -1));
		assertEquals(0, TextUtilities.getStartIndexOfLine("a\nbc\nd", 1));
		assertEquals(2, TextUtilities.getStartIndexOfLine("a\nbc\nd", 2));
		assertEquals(5, TextUtilities.getStartIndexOfLine("a\nbc\nd", 3));
		// Beyond the last line the start of the last line is returned
		assertEquals(5, TextUtilities.getStartIndexOfLine("a\nbc\nd", 10));
		assertEquals(3, TextUtilities.getStartIndexOfLine("a\r\nb", 2));
		assertEquals(2, TextUtilities.getStartIndexOfLine("a\rb", 2));
		assertEquals(7, TextUtilities.getStartIndexOfLine("a\r\nb\nc\rd", 4));
		// Empty line after a trailing linebreak
		assertEquals(2, TextUtilities.getStartIndexOfLine("a\n", 2));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getStartIndexOfLineAtIndex() {
		assertEquals(-1, TextUtilities.getStartIndexOfLineAtIndex(null, 0));
		assertEquals(-1, TextUtilities.getStartIndexOfLineAtIndex("a", -1));
		assertEquals(0, TextUtilities.getStartIndexOfLineAtIndex("ab\ncd", 0));
		assertEquals(0, TextUtilities.getStartIndexOfLineAtIndex("ab\ncd", 1));
		// The linebreak belongs to the line it ends
		assertEquals(0, TextUtilities.getStartIndexOfLineAtIndex("ab\ncd", 2));
		assertEquals(3, TextUtilities.getStartIndexOfLineAtIndex("ab\ncd", 3));
		assertEquals(3, TextUtilities.getStartIndexOfLineAtIndex("ab\ncd", 4));
		assertEquals(3, TextUtilities.getStartIndexOfLineAtIndex("ab\ncd", 99));
		assertEquals(4, TextUtilities.getStartIndexOfLineAtIndex("ab\r\ncd", 5));
		assertEquals(5, TextUtilities.getStartIndexOfLineAtIndex("a\r\nb\rc", 5));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getEndIndexOfLineAtIndex() {
		assertEquals(-1, TextUtilities.getEndIndexOfLineAtIndex(null, 0));
		assertEquals(-1, TextUtilities.getEndIndexOfLineAtIndex("a", -1));
		assertEquals(2, TextUtilities.getEndIndexOfLineAtIndex("ab\ncd", 0));
		assertEquals(2, TextUtilities.getEndIndexOfLineAtIndex("ab\ncd", 2));
		assertEquals(5, TextUtilities.getEndIndexOfLineAtIndex("ab\ncd", 3));
		assertEquals(5, TextUtilities.getEndIndexOfLineAtIndex("ab\ncd", 99));
		assertEquals(2, TextUtilities.getEndIndexOfLineAtIndex("ab\r\ncd", 1));
		assertEquals(3, TextUtilities.getEndIndexOfLineAtIndex("a\nb\rc", 2));
	}

	@SuppressWarnings("static-method")
	@Test
	public void searchNextPosition_forward() {
		final String text = "abc ABC abc";

		assertPosition(0, 3, TextUtilities.searchNextPosition(text, 0, "abc", false, true, false));
		assertPosition(8, 3, TextUtilities.searchNextPosition(text, 1, "abc", false, true, false));
		assertPosition(4, 3, TextUtilities.searchNextPosition(text, 1, "abc", false, false, false));
		// Wrap around to the start of the text
		assertPosition(0, 3, TextUtilities.searchNextPosition(text, 9, "abc", false, true, false));
		assertPosition(0, 3, TextUtilities.searchNextPosition(text, text.length(), "abc", false, true, false));
	}

	@SuppressWarnings("static-method")
	@Test
	public void searchNextPosition_reverse() {
		final String text = "abc ABC abc";

		assertPosition(0, 3, TextUtilities.searchNextPosition(text, 4, "abc", false, true, true));
		assertPosition(4, 3, TextUtilities.searchNextPosition(text, 8, "abc", false, false, true));
		assertPosition(8, 3, TextUtilities.searchNextPosition(text, text.length(), "abc", false, true, true));
		// Wrap around to the last match of the text
		assertPosition(8, 3, TextUtilities.searchNextPosition(text, 0, "abc", false, true, true));
		// Single match at index 0
		assertPosition(0, 3, TextUtilities.searchNextPosition("abc", 0, "abc", false, true, true));
		assertNull(TextUtilities.searchNextPosition(text, 5, "xyz", false, true, true));
	}

	@SuppressWarnings("static-method")
	@Test
	public void searchNextPosition_regularExpression() {
		assertPosition(3, 3, TextUtilities.searchNextPosition("a1 b22 c333", 1, "[a-z]\\d{2}", true, true, false));
		assertPosition(7, 3, TextUtilities.searchNextPosition("a1 b22 c333", 4, "[a-z]\\d{2}", true, true, false));
		assertPosition(3, 3, TextUtilities.searchNextPosition("a1 b22 c333", 1, "B\\d+", true, false, false));
		assertNull(TextUtilities.searchNextPosition("a1 b22 c333", 1, "B\\d+", true, true, false));
		assertPosition(3, 4, TextUtilities.searchNextPosition("ab\ncd e", 0, "^cd e$", true, true, false));
	}

	@SuppressWarnings("static-method")
	@Test
	public void searchNextPosition_literalIgnoresRegularExpressionCharacters() {
		assertPosition(4, 3, TextUtilities.searchNextPosition("abc a.c", 0, "a.c", false, true, false));
		assertPosition(1, 2, TextUtilities.searchNextPosition("a(*b", 0, "(*", false, true, false));
	}

	@SuppressWarnings("static-method")
	@Test
	public void searchNextPosition_invalidParameters() {
		assertNull(TextUtilities.searchNextPosition(null, 0, "a", false, true, false));
		assertNull(TextUtilities.searchNextPosition("  ", 0, "a", false, true, false));
		assertNull(TextUtilities.searchNextPosition("abc", -1, "a", false, true, false));
		assertNull(TextUtilities.searchNextPosition("abc", 4, "a", false, true, false));
		assertNull(TextUtilities.searchNextPosition("abc", 0, "", false, true, false));
		assertNull(TextUtilities.searchNextPosition("abc", 0, "x", false, true, false));
	}

	@SuppressWarnings("static-method")
	@Test
	public void countOccurences() {
		assertEquals(3, TextUtilities.countOccurences("aXaxa", "a", false, true));
		assertEquals(2, TextUtilities.countOccurences("aXaxa", "x", false, false));
		assertEquals(1, TextUtilities.countOccurences("aXaxa", "x", false, true));
		assertEquals(2, TextUtilities.countOccurences("a.b.c", ".", false, true));
		assertEquals(3, TextUtilities.countOccurences("abacad", "a.", true, true));
		assertEquals(2, TextUtilities.countOccurences("one\nTwo\nthree", "^t", true, false));
		// Non-overlapping
		assertEquals(1, TextUtilities.countOccurences("aaa", "aa", false, true));
		assertEquals(-1, TextUtilities.countOccurences(null, "a", false, true));
		assertEquals(-1, TextUtilities.countOccurences("a", null, false, true));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getLineNumberOfTextposition() {
		assertEquals(-1, TextUtilities.getLineNumberOfTextposition(null, 0));
		assertEquals(1, TextUtilities.getLineNumberOfTextposition("a\nb\nc", 0));
		assertEquals(1, TextUtilities.getLineNumberOfTextposition("a\nb\nc", 1));
		assertEquals(2, TextUtilities.getLineNumberOfTextposition("a\nb\nc", 2));
		assertEquals(2, TextUtilities.getLineNumberOfTextposition("a\nb\nc", 3));
		assertEquals(3, TextUtilities.getLineNumberOfTextposition("a\nb\nc", 100));
		assertEquals(2, TextUtilities.getLineNumberOfTextposition("a\r\nb", 3));
		assertEquals(2, TextUtilities.getLineNumberOfTextposition("a\rb", 2));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getNumberOfLineContainingText() throws IOException {
		final String text = "one\ntwo\nthree";

		assertEquals(2, TextUtilities.getNumberOfLineContainingText(text, "TWO", false, false));
		assertEquals(-1, TextUtilities.getNumberOfLineContainingText(text, "TWO", true, false));
		assertEquals(3, TextUtilities.getNumberOfLineContainingText(text, "^t.r", true, true));
		assertEquals(-1, TextUtilities.getNumberOfLineContainingText(text, ".", true, false));
		assertEquals(1, TextUtilities.getNumberOfLineContainingText(text, "O", false, false));
	}

	@SuppressWarnings("static-method")
	@Test
	public void insertTextBeforeLine() {
		assertEquals("a\nX\nb\nc", TextUtilities.insertTextBeforeLine("a\nb\nc", "X\n", 2));
		assertEquals("X\na\nb", TextUtilities.insertTextBeforeLine("a\nb", "X\n", 1));
		assertEquals("a\nb", TextUtilities.insertTextBeforeLine("a\nb", "X\n", -1));
	}

	@SuppressWarnings("static-method")
	@Test
	public void insertTextAfterLine() {
		assertEquals("a\nX\nb\nc", TextUtilities.insertTextAfterLine("a\nb\nc", "X\n", 1));
		assertEquals("a\r\nb\r\nX\r\nc", TextUtilities.insertTextAfterLine("a\r\nb\r\nc", "X\r\n", 2));
		assertEquals("X\na", TextUtilities.insertTextAfterLine("a", "X\n", 0));
		assertEquals("a\nb", TextUtilities.insertTextAfterLine("a\nb", "X\n", -1));
	}

	@SuppressWarnings("static-method")
	@Test
	public void insertTextAfterLine_lastLine() {
		// No linebreak is added before the inserted text
		assertEquals("a\nbX", TextUtilities.insertTextAfterLine("a\nb", "X", 2));
		assertEquals("aX", TextUtilities.insertTextAfterLine("a", "X", 1));
		assertEquals("a\r\nbX", TextUtilities.insertTextAfterLine("a\r\nb", "X", 2));
		// After a trailing linebreak the text starts the new line
		assertEquals("a\nX", TextUtilities.insertTextAfterLine("a\n", "X", 1));
		assertEquals("X", TextUtilities.insertTextAfterLine("", "X", 1));
	}

	@SuppressWarnings("static-method")
	@Test
	public void insertTextAfterLine_beyondLastLine() {
		assertEquals("a\nbX", TextUtilities.insertTextAfterLine("a\nb", "X", 5));
	}

	@SuppressWarnings("static-method")
	@Test
	public void insertTextAfterLine_emptyLines() {
		assertEquals("a\nX\nb", TextUtilities.insertTextAfterLine("a\n\nb", "X", 1));
		assertEquals("a\n\nXb", TextUtilities.insertTextAfterLine("a\n\nb", "X", 2));
	}

	@SuppressWarnings("static-method")
	@Test
	public void detectEncoding_withBom() {
		final byte[] data = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a' };

		final Tuple<String, Boolean> result = TextUtilities.detectEncoding(data);

		assertEquals("UTF-8", result.getFirst());
		assertTrue(result.getSecond());
	}

	@SuppressWarnings("static-method")
	@Test
	public void detectEncoding_fallbackForInvalidUtf8() {
		final byte[] data = "Grüße".getBytes(StandardCharsets.ISO_8859_1);

		final Tuple<String, Boolean> result = TextUtilities.detectEncoding(data, StandardCharsets.ISO_8859_1);

		assertEquals("ISO-8859-1", result.getFirst());
		assertFalse(result.getSecond());
	}

	@SuppressWarnings("static-method")
	@Test
	public void replaceIndentingBlanks() {
		assertNull(TextUtilities.replaceIndentingBlanks(null, 4, "\t"));
		assertEquals("\ta\n  b\n\t  c\nd", TextUtilities.replaceIndentingBlanks("    a\n  b\n      c\nd", 4, "\t"));
		assertEquals("\t\ta", TextUtilities.replaceIndentingBlanks("        a", 4, "\t"));
		assertEquals("  a", TextUtilities.replaceIndentingBlanks("  a", 0, "\t"));
		// Blanks within the line are kept
		assertEquals("\ta  b", TextUtilities.replaceIndentingBlanks("  a  b", 2, "\t"));
		// No endless loop for a replacement starting with blanks
		assertEquals("    a", TextUtilities.replaceIndentingBlanks("    a", 2, "  "));
		// Replacement is used literally
		assertEquals("$a", TextUtilities.replaceIndentingBlanks("  a", 2, "$"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void removeFirstLeadingWhitespace() {
		assertEquals("\ta\nb\nc", TextUtilities.removeFirstLeadingWhitespace("\t\ta\n b\nc"));
		assertEquals(" a", TextUtilities.removeFirstLeadingWhitespace("  a"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void addLeadingTabs() {
		assertEquals("\ta\n\tb", TextUtilities.addLeadingTab("a\nb"));
		assertEquals("\t\ta\n\t\tb", TextUtilities.addLeadingTabs("a\nb", 2));
		// No tab after a trailing linebreak
		assertEquals("\ta\n", TextUtilities.addLeadingTab("a\n"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void truncate_string() {
		assertEquals(List.of("abcd", "ef"), TextUtilities.truncate("abcdef", 4));
		assertEquals(List.of("ab"), TextUtilities.truncate("ab", 4));
		assertThrows(IllegalArgumentException.class, () -> TextUtilities.truncate("abc", 0));
	}

	@SuppressWarnings("static-method")
	@Test
	public void truncate_lines() {
		assertEquals(List.of("ab", "c", "de"), TextUtilities.truncate(new String[] { "abc", "de" }, 2));
		assertEquals(List.of("ab", "c", "de"), TextUtilities.truncate(Arrays.asList("abc", "de"), 2));
	}

	@SuppressWarnings("static-method")
	@Test
	public void removeLeadingAndTrailingWhitespaces() {
		assertEquals("a\nb\nc", TextUtilities.removeLeadingAndTrailingWhitespaces("  a  \n\t b\t\n\n c "));
		assertEquals("a b", TextUtilities.removeLeadingAndTrailingWhitespaces("  a b  "));
	}

	@SuppressWarnings("static-method")
	@Test
	public void removeTrailingWhitespaces() {
		assertEquals("a\nb\nc", TextUtilities.removeTrailingWhitespaces("a  \nb\t\n\nc "));
		assertEquals("  a", TextUtilities.removeTrailingWhitespaces("  a  "));
	}

	@SuppressWarnings("static-method")
	@Test
	public void scanLineStartIndexes() throws IOException {
		assertEquals(List.of(0L, 3L, 7L, 10L), TextUtilities.scanLineStartIndexes(stream("ab\ncd\r\nef\rgh")));
		assertEquals(List.of(0L, 2L), TextUtilities.scanLineStartIndexes(stream("a\r")));
		assertEquals(List.of(0L, 2L), TextUtilities.scanLineStartIndexes(stream("a\n")));
		assertEquals(List.of(0L), TextUtilities.scanLineStartIndexes(stream("")));
	}

	@SuppressWarnings("static-method")
	@Test
	public void scanLineStartIndexes_withStartIndex() throws IOException {
		final String data = "ab\ncd\r\nef\rgh";

		assertEquals(List.of(0L, 3L, 7L, 10L), TextUtilities.scanLineStartIndexes(stream(data), 0));
		assertEquals(List.of(3L, 7L, 10L), TextUtilities.scanLineStartIndexes(stream(data), 3));
		assertEquals(List.of(7L, 10L), TextUtilities.scanLineStartIndexes(stream(data), 4));
		assertEquals(List.of(7L, 10L), TextUtilities.scanLineStartIndexes(stream(data), 7));
		assertEquals(List.of(10L), TextUtilities.scanLineStartIndexes(stream(data), 10));
		assertEquals(List.of(), TextUtilities.scanLineStartIndexes(stream(data), 100));
	}

	@SuppressWarnings("static-method")
	@Test
	public void scanLinebreakIndexes() throws IOException {
		assertEquals(List.of(2L, 5L, 9L), TextUtilities.scanLinebreakIndexes(stream("ab\ncd\r\nef\rgh")));
		assertEquals(List.of(), TextUtilities.scanLinebreakIndexes(stream("abc")));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getUsedChars() {
		assertEquals(List.of('a', 'b', 'c'), TextUtilities.getUsedChars("abcab"));
		assertEquals(List.of(), TextUtilities.getUsedChars(""));
	}

	@SuppressWarnings("static-method")
	@Test
	public void containsIgnoreCase() {
		assertTrue(TextUtilities.containsIgnoreCase("Hello World", "WORLD"));
		assertTrue(TextUtilities.containsIgnoreCase("Hello World", "lo w"));
		assertTrue(TextUtilities.containsIgnoreCase("Hello", "HELLO"));
		assertFalse(TextUtilities.containsIgnoreCase("Hello", "xyz"));
		assertFalse(TextUtilities.containsIgnoreCase("ab", "abc"));
		assertTrue(TextUtilities.containsIgnoreCase(null, null));
		assertFalse(TextUtilities.containsIgnoreCase(null, "a"));
		assertTrue(TextUtilities.containsIgnoreCase("a", null));
	}

	@SuppressWarnings("static-method")
	@Test
	public void mapToString() {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("a", 1);
		map.put(null, null);
		map.put("b", "x");

		assertEquals("a: 1\n: \nb: x", TextUtilities.mapToString(map));
		assertEquals("", TextUtilities.mapToString(new LinkedHashMap<>()));
	}

	@SuppressWarnings("static-method")
	@Test
	public void mapToStringWithSortedKeys() {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("b", 2);
		map.put("a", null);

		assertEquals("a: \nb: 2", TextUtilities.mapToStringWithSortedKeys(map));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getColumnNumberFromColumnChars() {
		assertEquals(1, TextUtilities.getColumnNumberFromColumnChars("A"));
		assertEquals(26, TextUtilities.getColumnNumberFromColumnChars("Z"));
		assertEquals(27, TextUtilities.getColumnNumberFromColumnChars("AA"));
		assertEquals(52, TextUtilities.getColumnNumberFromColumnChars("az"));
		assertEquals(16384, TextUtilities.getColumnNumberFromColumnChars("XFD"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getColumnNumberFromColumnChars_turkishLocale() {
		final Locale defaultLocale = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag("tr-TR"));
			assertEquals(9, TextUtilities.getColumnNumberFromColumnChars("I"));
		} finally {
			Locale.setDefault(defaultLocale);
		}
	}

	@SuppressWarnings("static-method")
	@Test
	public void isValidBase64() {
		assertTrue(TextUtilities.isValidBase64(""));
		assertTrue(TextUtilities.isValidBase64("QQ=="));
		assertTrue(TextUtilities.isValidBase64("QUI="));
		assertTrue(TextUtilities.isValidBase64("QUJD"));
		assertTrue(TextUtilities.isValidBase64("a+/9QUJD"));
		assertFalse(TextUtilities.isValidBase64("QQ="));
		assertFalse(TextUtilities.isValidBase64("Q"));
		assertFalse(TextUtilities.isValidBase64("QUJD\n"));
		assertFalse(TextUtilities.isValidBase64("QU*D"));
		assertFalse(TextUtilities.isValidBase64("QQ==QUJD"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getLineStartingWith() throws Exception {
		assertEquals("b=2", TextUtilities.getLineStartingWith("a=1\nb=2\nb=3", "b"));
		assertNull(TextUtilities.getLineStartingWith("a=1\nb=2", "c"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getProsaParameter() {
		final String text = "Name: Hans\n  Age : 42 \nEmpty:\n";

		assertEquals("Hans", TextUtilities.getProsaParameter(text, "Name"));
		assertEquals("42", TextUtilities.getProsaParameter(text, "Age"));
		assertEquals("", TextUtilities.getProsaParameter(text, "Empty"));
		assertNull(TextUtilities.getProsaParameter(text, "Missing"));
		assertNull(TextUtilities.getProsaParameter(" ", "Name"));
		assertNull(TextUtilities.getProsaParameter(null, "Name"));
		// The parameter name is no regular expression
		assertNull(TextUtilities.getProsaParameter("axb: 1", "a.b"));
		assertEquals("1", TextUtilities.getProsaParameter("a.b: 1", "a.b"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void removeLinesContainingText() {
		assertNull(TextUtilities.removeLinesContainingText(null, "x"));
		assertEquals("keep\n\nkeep2", TextUtilities.removeLinesContainingText("keep\nremove me\n\nkeep2", "remove"));
		assertEquals("a\r\nb", TextUtilities.removeLinesContainingText("a\r\nx\r\nb", "x"));
		assertEquals("a\rb", TextUtilities.removeLinesContainingText("a\rx\rb", "x"));
		assertEquals("a\n", TextUtilities.removeLinesContainingText("a\nx", "x"));
		assertEquals("", TextUtilities.removeLinesContainingText("x\nxx\n", "x"));
		assertEquals("a\n\nb", TextUtilities.removeLinesContainingText("a\n\nb", "x"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void containsNonAsciiCharacters() {
		assertFalse(TextUtilities.containsNonAsciiCharacters("a\tb\r\n~".toCharArray()));
		assertTrue(TextUtilities.containsNonAsciiCharacters("aä".toCharArray()));
		assertFalse(TextUtilities.containsNonAsciiCharacters(new char[0]));
	}

	@SuppressWarnings("static-method")
	@Test
	public void fixEncodingErrorUTF8AsISO8859() {
		final String original = "Grüße Ä è ° €";
		final String wronglyDecoded = new String(original.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

		assertEquals(original, TextUtilities.fixEncodingErrorUTF8AsISO8859(wronglyDecoded));
		assertEquals("abc", TextUtilities.fixEncodingErrorUTF8AsISO8859("abc"));
		// Correct texts stay unchanged
		assertEquals(original, TextUtilities.fixEncodingErrorUTF8AsISO8859(original));
		assertEquals("Ã", TextUtilities.fixEncodingErrorUTF8AsISO8859("Ã"));
		assertEquals("Ãx", TextUtilities.fixEncodingErrorUTF8AsISO8859("Ãx"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void replaceLast() {
		assertEquals("a.b-c", TextUtilities.replaceLast("a.b.c", ".", "-"));
		assertEquals("abc", TextUtilities.replaceLast("abc", "x", "-"));
		assertEquals("a$1", TextUtilities.replaceLast("ab", "b", "$1"));
		assertEquals("x\ny", TextUtilities.replaceLast("x\nx", "x", "y"));
		assertEquals("y", TextUtilities.replaceLast("x", "x", "y"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void removeHtmlTags() {
		assertEquals("Hello World", TextUtilities.removeHtmlTags("<p>Hello <b>World</b></p>"));
		assertEquals("a < b", TextUtilities.removeHtmlTags("a < b"));
		assertEquals("&amp;", TextUtilities.removeHtmlTags("<i>&amp;</i>"));
		assertEquals("text", TextUtilities.removeHtmlTags("text"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void decodeUnicodeEncoding() {
		assertEquals("café $ \\ x", TextUtilities.decodeUnicodeEncoding("caf\\u00e9 \\u0024 \\u005C x"));
		assertEquals("\\u12", TextUtilities.decodeUnicodeEncoding("\\u12"));
		assertEquals("plain", TextUtilities.decodeUnicodeEncoding("plain"));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getUnicodeStringLength() {
		final String withSurrogatePair = "a😀";

		assertEquals(3, withSurrogatePair.length());
		assertEquals(2, TextUtilities.getUnicodeStringLength(withSurrogatePair));
		assertEquals(0, TextUtilities.getUnicodeStringLength(""));
	}

	@SuppressWarnings("static-method")
	@Test
	public void getMaximumLineLength() throws IOException {
		assertEquals(4, TextUtilities.getMaximumLineLength("ab\nabcd\r\nc"));
		assertEquals(0, TextUtilities.getMaximumLineLength(""));
	}

	private static ByteArrayInputStream stream(final String data) {
		return new ByteArrayInputStream(data.getBytes(StandardCharsets.US_ASCII));
	}

	private static void assertPosition(final int expectedStart, final int expectedLength, final Tuple<Integer, Integer> position) {
		assertNotNull(position, "position");
		assertEquals(Integer.valueOf(expectedStart), position.getFirst(), "start");
		assertEquals(Integer.valueOf(expectedLength), position.getSecond(), "length");
	}
}