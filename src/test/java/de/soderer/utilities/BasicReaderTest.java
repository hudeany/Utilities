package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BasicReader}. Located in the same package to access its protected methods.
 */
@SuppressWarnings("static-method")
public class BasicReaderTest {
	private static BasicReader createReader(final String text) throws Exception {
		return new BasicReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
	}

	private static BasicReader createReader(final byte[] data, final Charset encoding) throws Exception {
		return new BasicReader(new ByteArrayInputStream(data), encoding);
	}

	private static String readAll(final BasicReader reader) throws Exception {
		final StringBuilder result = new StringBuilder();
		Character nextChar;
		while ((nextChar = reader.readNextCharacter()) != null) {
			result.append(nextChar.charValue());
		}
		return result.toString();
	}

	/**
	 * Input stream that remembers whether it was closed.
	 */
	private static class CloseTrackingInputStream extends ByteArrayInputStream {
		boolean closed = false;

		CloseTrackingInputStream(final byte[] data) {
			super(data);
		}

		@Override
		public void close() throws IOException {
			closed = true;
			super.close();
		}
	}

	@Test
	public void testNullInputStream() {
		assertThrows(Exception.class, () -> new BasicReader(null));
		assertThrows(Exception.class, () -> new BasicReader(null, StandardCharsets.UTF_8));
	}

	@Test
	public void testReadCharacters() throws Exception {
		try (BasicReader reader = createReader("ab")) {
			assertFalse(reader.readWasInitialized());
			assertNull(reader.getCurrentChar());

			assertEquals(Character.valueOf('a'), reader.readNextCharacter());
			assertTrue(reader.readWasInitialized());
			assertEquals(Character.valueOf('a'), reader.getCurrentChar());
			assertEquals(Character.valueOf('b'), reader.readNextCharacter());
			assertNull(reader.readNextCharacter());
			assertNull(reader.getCurrentChar());
			assertNull(reader.readNextCharacter());
			assertEquals(2, reader.getReadCharacters());
		}
	}

	@Test
	public void testEmptyInput() throws Exception {
		try (BasicReader reader = createReader("")) {
			assertTrue(reader.isEOF());
			assertNull(reader.readNextCharacter());
			assertNull(reader.readNextLine());
			assertEquals("", reader.readUpToNext(false, null, ','));
		}
	}

	@Test
	public void testUtf8BomIsSkipped() throws Exception {
		final byte[] data = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', 'b' };
		try (BasicReader reader = createReader(data, StandardCharsets.UTF_8)) {
			assertEquals("ab", readAll(reader));
			assertEquals(2, reader.getReadCharacters());
		}
	}

	@Test
	public void testUtf16LeBomIsSkipped() throws Exception {
		final byte[] data = { (byte) 0xFF, (byte) 0xFE, 'a', 0, 'b', 0 };
		try (BasicReader reader = createReader(data, StandardCharsets.UTF_16LE)) {
			assertEquals("ab", readAll(reader));
		}
	}

	@Test
	public void testBomOnlyInput() throws Exception {
		final byte[] data = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
		try (BasicReader reader = createReader(data, StandardCharsets.UTF_8)) {
			assertNull(reader.readNextCharacter());
		}
	}

	@Test
	public void testBomCharacterLaterInDataIsKept() throws Exception {
		try (BasicReader reader = createReader("a\uFEFFb")) {
			assertEquals("a\uFEFFb", readAll(reader));
		}
	}

	@Test
	public void testUtf8BomWithLatinEncodingIsRejected() throws Exception {
		final byte[] data = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a' };
		try (BasicReader reader = createReader(data, StandardCharsets.ISO_8859_1)) {
			assertThrows(IOException.class, reader::readNextCharacter);
		}
		try (BasicReader reader = createReader(data, Charset.forName("windows-1252"))) {
			assertThrows(IOException.class, reader::readNextCharacter);
		}
	}

	@Test
	public void testSingleLatinSmallIDiaeresisIsValidText() throws Exception {
		// 0xEF is 'i with diaeresis' in ISO-8859-1, only the complete sequence EF BB BF is a misread BOM
		final byte[] data = { (byte) 0xEF, 'x', 'y' };
		try (BasicReader reader = createReader(data, StandardCharsets.ISO_8859_1)) {
			assertEquals("\u00EFxy", readAll(reader));
		}
		final byte[] shortData = { (byte) 0xEF, (byte) 0xBB };
		try (BasicReader reader = createReader(shortData, StandardCharsets.ISO_8859_1)) {
			assertEquals("\u00EF\u00BB", readAll(reader));
		}
	}

	@Test
	public void testLineCountingWithoutNormalization() throws Exception {
		try (BasicReader reader = createReader("a\r\nb\rc\nd")) {
			assertEquals("a\r\nb\rc\nd", readAll(reader));
			assertEquals(3, reader.getReadLines());
			assertEquals(1, reader.getReadCharactersInCurrentLine());
			assertEquals(8, reader.getReadCharacters());
		}
	}

	@Test
	public void testLineCountingStepByStep() throws Exception {
		try (BasicReader reader = createReader("ab\r\nc")) {
			reader.readNextCharacter();
			reader.readNextCharacter();
			assertEquals(0, reader.getReadLines());
			assertEquals(2, reader.getReadCharactersInCurrentLine());

			reader.readNextCharacter();
			assertEquals(1, reader.getReadLines());
			assertEquals(0, reader.getReadCharactersInCurrentLine());

			// LF of the CRLF does not count as a further line break
			reader.readNextCharacter();
			assertEquals(1, reader.getReadLines());
			assertEquals(0, reader.getReadCharactersInCurrentLine());

			reader.readNextCharacter();
			assertEquals(1, reader.getReadLines());
			assertEquals(1, reader.getReadCharactersInCurrentLine());
		}
	}

	@Test
	public void testNormalizedLinebreaks() throws Exception {
		try (BasicReader reader = createReader("a\r\nb\rc\nd")) {
			reader.setNormalizeLinebreaks(true);
			assertTrue(reader.isNormalizeLinebreaks());
			assertEquals("a\nb\nc\nd", readAll(reader));
			assertEquals(3, reader.getReadLines());
			// Raw characters: a normalized CRLF counts as two
			assertEquals(8, reader.getReadCharacters());
		}
	}

	@Test
	public void testNormalizedCrAtEndOfData() throws Exception {
		try (BasicReader reader = createReader("a\r")) {
			reader.setNormalizeLinebreaks(true);
			assertEquals("a\n", readAll(reader));
			assertEquals(1, reader.getReadLines());
		}
	}

	@Test
	public void testNormalizedConsecutiveLinebreaks() throws Exception {
		try (BasicReader reader = createReader("\r\r\n\n\r")) {
			reader.setNormalizeLinebreaks(true);
			assertEquals("\n\n\n\n", readAll(reader));
			assertEquals(4, reader.getReadLines());
		}
	}

	@Test
	public void testSwitchingNormalizationOnAppliesToPeekedCharacters() throws Exception {
		try (BasicReader reader = createReader("\r\nx")) {
			// Peeked raw CR is normalized together with its LF after switching normalization on
			assertEquals(Character.valueOf('\r'), reader.peek());
			reader.setNormalizeLinebreaks(true);
			assertEquals("\nx", readAll(reader));
			assertEquals(1, reader.getReadLines());
		}
	}

	@Test
	public void testSwitchingNormalizationOffRestoresPeekedLinebreaks() throws Exception {
		try (BasicReader reader = createReader("\r\nx")) {
			reader.setNormalizeLinebreaks(true);
			assertEquals(Character.valueOf('\n'), reader.peek());
			reader.setNormalizeLinebreaks(false);
			assertEquals("\r\nx", readAll(reader));
			assertEquals(1, reader.getReadLines());
		}
	}

	@Test
	public void testIndentationLevel() throws Exception {
		try (BasicReader reader = createReader("  \tx y\n z")) {
			reader.readNextCharacter();
			reader.readNextCharacter();
			reader.readNextCharacter();
			assertEquals(3, reader.getCurrentIndentationLevel());
			reader.readNextCharacter();
			reader.readNextCharacter();
			reader.readNextCharacter();
			// Spaces after the first non-whitespace do not count
			assertEquals(3, reader.getCurrentIndentationLevel());
			reader.readNextCharacter();
			assertEquals(0, reader.getCurrentIndentationLevel());
			reader.readNextCharacter();
			reader.readNextCharacter();
			assertEquals(1, reader.getCurrentIndentationLevel());
		}
	}

	@Test
	public void testPeekDoesNotChangeState() throws Exception {
		try (BasicReader reader = createReader("abc")) {
			reader.readNextCharacter();
			assertEquals(Character.valueOf('b'), reader.peek());
			assertEquals(Character.valueOf('c'), reader.peekAhead());
			assertEquals(Character.valueOf('a'), reader.getCurrentChar());
			assertEquals(1, reader.getReadCharacters());
			assertEquals(1, reader.getReadCharactersInCurrentLine());
			assertEquals(Character.valueOf('b'), reader.readNextCharacter());
			assertNull(reader.peekAhead());
			assertEquals(Character.valueOf('c'), reader.peek());
			assertFalse(reader.isEOF());
			assertEquals(Character.valueOf('c'), reader.readNextCharacter());
			assertTrue(reader.isEOF());
			assertNull(reader.peek());
		}
	}

	@Test
	public void testReuseCurrentChar() throws Exception {
		try (BasicReader reader = createReader("ab\nc")) {
			reader.readNextCharacter();
			reader.readNextCharacter();
			reader.readNextCharacter();
			assertEquals(1, reader.getReadLines());

			reader.reuseCurrentChar();
			// The current character stays, but all counters are restored
			assertEquals(Character.valueOf('\n'), reader.getCurrentChar());
			assertEquals(0, reader.getReadLines());
			assertEquals(2, reader.getReadCharactersInCurrentLine());
			assertEquals(2, reader.getReadCharacters());

			// A second call does nothing, because the current character is already pending
			reader.reuseCurrentChar();
			assertEquals(Character.valueOf('\n'), reader.readNextCharacter());
			assertEquals(Character.valueOf('c'), reader.readNextCharacter());
		}
	}

	@Test
	public void testReuseCharacters() throws Exception {
		try (BasicReader reader = createReader("abc")) {
			reader.readNextCharacter();
			reader.readNextCharacter();
			reader.readNextCharacter();
			assertThrows(IllegalArgumentException.class, () -> reader.reuse(new char[] { 'a', 'c' }));
			assertThrows(IllegalStateException.class, () -> reader.reuse(new char[] { 'x', 'a', 'b', 'c' }));
			reader.reuse(new char[] { 'b', 'c' });
			assertEquals(1, reader.getReadCharacters());
			assertEquals("bc", readAll(reader));

			// Null does nothing
			reader.reuse((Character) null);
			reader.reuse((char[]) null);
		}
	}

	@Test
	public void testUnreadRestoresPositionAcrossLines() throws Exception {
		try (BasicReader reader = createReader("  ab\r\n  cd")) {
			final String expected = readAll(reader);
			assertEquals("  ab\r\n  cd", expected);
			assertEquals(1, reader.getReadLines());
			assertEquals(2, reader.getCurrentIndentationLevel());

			// Back to the position after "  a"
			reader.unread(7);
			assertEquals(0, reader.getReadLines());
			assertEquals(3, reader.getReadCharactersInCurrentLine());
			assertEquals(2, reader.getCurrentIndentationLevel());
			assertEquals(3, reader.getReadCharacters());

			assertEquals("b\r\n  cd", readAll(reader));
			assertEquals(1, reader.getReadLines());
			assertEquals(4, reader.getReadCharactersInCurrentLine());
		}
	}

	@Test
	public void testUnreadMaximum() throws Exception {
		final StringBuilder text = new StringBuilder();
		for (int i = 0; i < 100; i++) {
			text.append((char) ('a' + i % 26));
		}
		try (BasicReader reader = createReader(text.toString())) {
			assertEquals(text.toString(), readAll(reader));
			reader.unread(BasicReader.MAX_UNREAD_CHARACTERS);
			assertEquals(100 - BasicReader.MAX_UNREAD_CHARACTERS, reader.getReadCharacters());
			assertEquals(text.substring(100 - BasicReader.MAX_UNREAD_CHARACTERS), readAll(reader));
		}
	}

	@Test
	public void testUnreadInvalidCount() throws Exception {
		try (BasicReader reader = createReader("ab")) {
			reader.readNextCharacter();
			assertThrows(IllegalStateException.class, () -> reader.unread(2));
			assertThrows(IllegalStateException.class, () -> reader.unread(-1));
			reader.unread(0);
			assertEquals(Character.valueOf('b'), reader.readNextCharacter());
		}
	}

	@Test
	public void testUnreadNormalizedCrlfAndSwitchNormalizationOff() throws Exception {
		try (BasicReader reader = createReader("a\r\nb")) {
			reader.setNormalizeLinebreaks(true);
			assertEquals("a\nb", readAll(reader));
			reader.unread(2);
			reader.setNormalizeLinebreaks(false);
			assertEquals("\r\nb", readAll(reader));
			assertEquals(1, reader.getReadLines());
			assertEquals(4, reader.getReadCharacters());
		}
	}

	@Test
	public void testReadNextLine() throws Exception {
		try (BasicReader reader = createReader("l1\r\nl2\rl3\nl4")) {
			assertEquals("l1", reader.readNextLine());
			assertEquals("l2", reader.readNextLine());
			assertEquals("l3", reader.readNextLine());
			assertEquals("l4", reader.readNextLine());
			assertNull(reader.readNextLine());
			assertEquals(3, reader.getReadLines());
		}
	}

	@Test
	public void testReadNextLineEmptyLines() throws Exception {
		try (BasicReader reader = createReader("\n\r\n\nx\n")) {
			assertEquals("", reader.readNextLine());
			assertEquals("", reader.readNextLine());
			assertEquals("", reader.readNextLine());
			assertEquals("x", reader.readNextLine());
			assertNull(reader.readNextLine());
		}
	}

	@Test
	public void testReadNextNonWhitespace() throws Exception {
		try (BasicReader reader = createReader(" \t \n x")) {
			assertEquals(Character.valueOf('\n'), reader.readNextNonWhitespaceWithinLine());
			assertEquals(Character.valueOf('x'), reader.readNextNonWhitespace());
			assertNull(reader.readNextNonWhitespace());
		}
	}

	@Test
	public void testReadUpToNext() throws Exception {
		try (BasicReader reader = createReader("abc,def;ghi")) {
			assertEquals("abc", reader.readUpToNext(false, null, ',', ';'));
			assertEquals(Character.valueOf(','), reader.readNextCharacter());
			// The current character ',' is part of the result
			assertEquals(",def;", reader.readUpToNext(true, null, ';'));
			reader.readNextCharacter();
			assertEquals("ghi", reader.readUpToNext(false, null, ','));
		}
	}

	@Test
	public void testReadUpToNextDoesNotUsePendingCurrentChar() throws Exception {
		try (BasicReader reader = createReader("abc,")) {
			reader.readNextCharacter();
			reader.reuseCurrentChar();
			assertEquals("abc", reader.readUpToNext(false, null, ','));
		}
	}

	@Test
	public void testReadUpToNextEscapes() throws Exception {
		try (BasicReader reader = createReader("\\\\\\\"\\/\\'\\b\\f\\n\\r\\t\\u00e4\\u00C4,rest")) {
			assertEquals("\\\"/'\b\f\n\r\t\u00E4\u00C4", reader.readUpToNext(false, '\\', ','));
			assertEquals(Character.valueOf(','), reader.readNextCharacter());
		}
	}

	@Test
	public void testReadUpToNextEscapedLimitChar() throws Exception {
		// An escaped limit character does not end the text
		try (BasicReader reader = createReader("a\\\"b\"c")) {
			assertEquals("a\"b", reader.readUpToNext(false, '\\', '"'));
			assertEquals(Character.valueOf('"'), reader.readNextCharacter());
		}
		// Only the JSON escape sequences are supported, so an escaped comma is invalid
		try (BasicReader reader = createReader("a\\,b")) {
			assertThrows(Exception.class, () -> reader.readUpToNext(false, '\\', ','));
		}
	}

	@Test
	public void testReadUpToNextInvalidEscapes() throws Exception {
		try (BasicReader reader = createReader("ab\\q")) {
			final Exception exception = assertThrows(Exception.class, () -> reader.readUpToNext(false, '\\', ','));
			assertTrue(exception.getMessage().startsWith("Invalid escape sequence"), exception.getMessage());
		}
		try (BasicReader reader = createReader("ab\\")) {
			final Exception exception = assertThrows(Exception.class, () -> reader.readUpToNext(false, '\\', ','));
			assertTrue(exception.getMessage().startsWith("Incomplete escape sequence"), exception.getMessage());
		}
		try (BasicReader reader = createReader("\\u00g1")) {
			final Exception exception = assertThrows(Exception.class, () -> reader.readUpToNext(false, '\\', ','));
			assertTrue(exception.getMessage().contains("'00g'"), exception.getMessage());
		}
		try (BasicReader reader = createReader("\\u00")) {
			assertThrows(Exception.class, () -> reader.readUpToNext(false, '\\', ','));
		}
		try (BasicReader reader = createReader("\\u\uFF10\uFF10\uFF10\uFF10")) {
			// Fullwidth digits are no ASCII hex digits
			assertThrows(Exception.class, () -> reader.readUpToNext(false, '\\', ','));
		}
	}

	@Test
	public void testReadUpToNextInvalidLimitChars() throws Exception {
		try (BasicReader reader = createReader("abc")) {
			assertThrows(Exception.class, () -> reader.readUpToNext(false, null, ',', ','));
			assertThrows(Exception.class, () -> reader.readUpToNext(false, ',', ','));
		}
	}

	@Test
	public void testReadUpToNextString() throws Exception {
		try (BasicReader reader = createReader("abc-->def")) {
			assertEquals("abc", reader.readUpToNextString(false, null, "-->"));
			assertEquals(Character.valueOf('-'), reader.readNextCharacter());
		}
		try (BasicReader reader = createReader("abc-->def")) {
			assertEquals("abc-->", reader.readUpToNextString(true, null, "-->"));
			assertEquals(Character.valueOf('d'), reader.readNextCharacter());
		}
		try (BasicReader reader = createReader("abc")) {
			assertEquals("abc", reader.readUpToNextString(false, null, "-->"));
		}
	}

	@Test
	public void testReadUpToNextStringFirstMatchWins() throws Exception {
		try (BasicReader reader = createReader("a]]b}}c")) {
			assertEquals("a", reader.readUpToNextString(false, null, "}}", "]]"));
			assertEquals("]]", reader.readUpToNextString(true, null, "]]"));
		}
	}

	@Test
	public void testReadUpToNextStringEscapedCharactersDoNotMatch() throws Exception {
		// The escaped quote must not form the limit string "" together with the following quote
		try (BasicReader reader = createReader("a\\\"\"b\"\"c")) {
			assertEquals("a\"\"b", reader.readUpToNextString(false, '\\', "\"\""));
			assertEquals(Character.valueOf('"'), reader.readNextCharacter());
		}
	}

	@Test
	public void testReadUpToNextStringInvalidLimitStrings() throws Exception {
		final StringBuilder tooLong = new StringBuilder();
		for (int i = 0; i <= BasicReader.MAX_UNREAD_CHARACTERS; i++) {
			tooLong.append('x');
		}
		try (BasicReader reader = createReader("abc")) {
			assertThrows(Exception.class, () -> reader.readUpToNextString(false, null));
			assertThrows(Exception.class, () -> reader.readUpToNextString(false, null, ""));
			assertThrows(Exception.class, () -> reader.readUpToNextString(false, null, (String) null));
			assertThrows(Exception.class, () -> reader.readUpToNextString(false, null, tooLong.toString()));
			assertThrows(Exception.class, () -> reader.readUpToNextString(false, null, "ab", "ab"));
			assertThrows(Exception.class, () -> reader.readUpToNextString(false, '\\', "a\\b"));
		}
	}

	@Test
	public void testReadQuotedText() throws Exception {
		try (BasicReader reader = createReader("\"a\\\"b\\nc\" rest")) {
			reader.readNextCharacter();
			assertEquals("a\"b\nc", reader.readQuotedText('\\'));
			assertEquals(Character.valueOf('"'), reader.getCurrentChar());
			assertEquals(Character.valueOf(' '), reader.readNextCharacter());
		}
	}

	@Test
	public void testReadQuotedTextWithoutEscapeCharacter() throws Exception {
		try (BasicReader reader = createReader("'a\\b'x")) {
			reader.readNextCharacter();
			assertEquals("a\\b", reader.readQuotedText((Character) null));
			assertEquals(Character.valueOf('x'), reader.readNextCharacter());
		}
	}

	@Test
	public void testReadQuotedTextDoubledQuote() throws Exception {
		try (BasicReader reader = createReader("'it''s','',x")) {
			reader.readNextCharacter();
			assertEquals("it's", reader.readQuotedText('\''));
			assertEquals(Character.valueOf(','), reader.readNextCharacter());
			reader.readNextCharacter();
			assertEquals("", reader.readQuotedText('\''));
			assertEquals(Character.valueOf(','), reader.readNextCharacter());
		}
	}

	@Test
	public void testReadQuotedTextWithPendingOpeningQuote() throws Exception {
		try (BasicReader reader = createReader("\"abc\"")) {
			reader.readNextCharacter();
			reader.reuseCurrentChar();
			assertEquals("abc", reader.readQuotedText('"', '\\'));
		}
	}

	@Test
	public void testReadQuotedTextErrors() throws Exception {
		try (BasicReader reader = createReader("\"abc")) {
			reader.readNextCharacter();
			assertThrows(Exception.class, () -> reader.readQuotedText('\\'));
		}
		try (BasicReader reader = createReader("'abc")) {
			reader.readNextCharacter();
			assertThrows(Exception.class, () -> reader.readQuotedText('\''));
		}
		try (BasicReader reader = createReader("abc")) {
			// Nothing read yet
			assertThrows(Exception.class, () -> reader.readQuotedText('\\'));
			reader.readNextCharacter();
			// Current character is no quote
			assertThrows(Exception.class, () -> reader.readQuotedText('"', '\\'));
		}
	}

	@Test
	public void testReadQuotedTextKeepsOriginalLinebreaks() throws Exception {
		try (BasicReader reader = createReader("\"a\r\nb\rc\"\r\nx")) {
			reader.setNormalizeLinebreaks(true);
			reader.readNextCharacter();
			assertEquals("a\r\nb\rc", reader.readQuotedText('\\'));
			assertTrue(reader.isNormalizeLinebreaks());
			// Normalization applies again after the closing quote
			assertEquals("\nx", readAll(reader));
		}
	}

	@Test
	public void testReadQuotedTextRestoresNormalizationOnError() throws Exception {
		try (BasicReader reader = createReader("\"abc")) {
			reader.setNormalizeLinebreaks(true);
			reader.readNextCharacter();
			assertThrows(Exception.class, () -> reader.readQuotedText('\\'));
			assertTrue(reader.isNormalizeLinebreaks());
		}
	}

	@Test
	public void testPositionText() throws Exception {
		try (BasicReader reader = createReader("ab\ncd")) {
			assertEquals(" at character index 0 in line 1", reader.getCurrentPositionText());
			readAll(reader);
			assertEquals(" at character index 1 in line 2", reader.getCurrentPositionText());
		}
		try (BasicReader reader = createReader("ab\n\\q")) {
			final Exception exception = assertThrows(Exception.class, () -> reader.readUpToNext(false, '\\', ','));
			assertEquals("Invalid escape sequence at character index 1 in line 2 ('q')", exception.getMessage());
		}
	}

	@Test
	public void testReadDataSize() throws Exception {
		try (BasicReader reader = createReader("\u00E4bc")) {
			assertEquals(0, reader.getReadDataSize());
			readAll(reader);
			// The umlaut takes two bytes in UTF-8
			assertEquals(4, reader.getReadDataSize());
			assertEquals(3, reader.getReadCharacters());
		}
	}

	@Test
	public void testClose() throws Exception {
		final CloseTrackingInputStream inputStream = new CloseTrackingInputStream("abc".getBytes(StandardCharsets.UTF_8));
		try (final BasicReader reader = new BasicReader(inputStream)) {
			reader.readNextCharacter();
			reader.close();
			assertTrue(inputStream.closed);
			assertEquals(0, reader.getReadDataSize());
			assertThrows(IllegalStateException.class, reader::readNextCharacter);
			// Position information remains available
			assertEquals(1, reader.getReadCharacters());
			// Closing twice is allowed
			reader.close();
		}
	}

	@Test
	public void testStaticHelpers() {
		assertTrue(BasicReader.isBlank(null));
		assertTrue(BasicReader.isBlank(" \t"));
		assertFalse(BasicReader.isBlank(" x "));
		assertTrue(BasicReader.isNotEmpty(" "));
		assertFalse(BasicReader.isNotEmpty(""));
		assertFalse(BasicReader.isNotEmpty(null));
	}

	@Test
	public void testDefaultEncodingIsUtf8() throws Exception {
		try (InputStream inputStream = new ByteArrayInputStream("\u00E4\u20AC".getBytes(StandardCharsets.UTF_8)); BasicReader reader = new BasicReader(inputStream, null)) {
			assertEquals("\u00E4\u20AC", readAll(reader));
		}
	}
}
