package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link BasicReadAheadReader}. Located in the same package to access its protected methods.
 */
@SuppressWarnings("static-method")
public class BasicReadAheadReaderTest {
	private static BasicReadAheadReader createReader(final String text) throws Exception {
		return new BasicReadAheadReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)));
	}

	private static BasicReadAheadReader createReader(final String text, final int numberOfReadAheadCharacters) throws Exception {
		return new BasicReadAheadReader(new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)), numberOfReadAheadCharacters);
	}

	private static BasicReadAheadReader createReader(final byte[] data, final Charset encoding) throws Exception {
		return new BasicReadAheadReader(new ByteArrayInputStream(data), encoding);
	}

	private static String readAll(final BasicReadAheadReader reader) throws Exception {
		final StringBuilder result = new StringBuilder();
		Character nextChar;
		while ((nextChar = reader.readChar()) != null) {
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
	public void testInvalidConstructorArguments() {
		assertThrows(Exception.class, () -> new BasicReadAheadReader(null));
		assertThrows(Exception.class, () -> createReader("abc", 0));
		assertThrows(Exception.class, () -> createReader("abc", -1));
	}

	@Test
	public void testReadChars() throws Exception {
		try (BasicReadAheadReader reader = createReader("abc")) {
			assertTrue(reader.isNotEOF());
			assertEquals(Character.valueOf('a'), reader.readChar());
			assertEquals(Character.valueOf('b'), reader.readChar());
			assertEquals(Character.valueOf('c'), reader.readChar());
			assertTrue(reader.isEOF());
			assertFalse(reader.isNotEOF());
			assertNull(reader.readChar());
			assertNull(reader.readChar());
			assertEquals(3, reader.getCharactersRead());
		}
	}

	@Test
	public void testEmptyInput() throws Exception {
		try (BasicReadAheadReader reader = createReader("")) {
			assertTrue(reader.isEOF());
			assertNull(reader.peekChar());
			assertNull(reader.readChar());
			assertNull(reader.readLine());
			assertEquals("", reader.readUpToNext(false, null, ','));
			assertEquals(0, reader.getCharactersRead());
		}
	}

	@Test
	public void testPeekWithinReadAheadRange() throws Exception {
		try (BasicReadAheadReader reader = createReader("abcd")) {
			assertEquals(Character.valueOf('a'), reader.peekChar());
			assertEquals(Character.valueOf('a'), reader.peekNextChar(0));
			assertEquals(Character.valueOf('b'), reader.peekNextChar(1));
			assertEquals(Character.valueOf('c'), reader.peekNextChar(2));
			assertThrows(IllegalArgumentException.class, () -> reader.peekNextChar(3));
			assertThrows(IllegalArgumentException.class, () -> reader.peekNextChar(-1));

			reader.readChar();
			assertEquals(Character.valueOf('d'), reader.peekNextChar(2));
			reader.readChar();
			assertNull(reader.peekNextChar(2));
			assertEquals(2, reader.getCharactersRead());
		}
	}

	@Test
	public void testReadAheadRangeIsConfiguredSizeNotInternalBufferSize() throws Exception {
		// Internally at least 2 characters are buffered, but only the configured number may be peeked
		try (BasicReadAheadReader reader = createReader("abc", 1)) {
			assertEquals(Character.valueOf('a'), reader.peekNextChar(0));
			assertThrows(IllegalArgumentException.class, () -> reader.peekNextChar(1));
			assertThrows(IllegalArgumentException.class, () -> reader.peekNextCharMatch(1, 'b'));
			assertThrows(IllegalArgumentException.class, () -> reader.peekNextCharNotMatch(1, 'b'));
			assertThrows(IllegalArgumentException.class, () -> reader.peekNextCharMatchAny(1, "b"));
			assertThrows(IllegalArgumentException.class, () -> reader.peekNextCharNotMatchAny(1, "b"));
			assertEquals("abc", readAll(reader));
		}
	}

	@Test
	public void testLargeReadAhead() throws Exception {
		try (BasicReadAheadReader reader = createReader("0123456789ab", 10)) {
			assertEquals(Character.valueOf('9'), reader.peekNextChar(9));
			reader.readChar();
			assertEquals(Character.valueOf('a'), reader.peekNextChar(9));
			assertEquals("123456789ab", readAll(reader));
		}
	}

	@Test
	public void testMatchMethods() throws Exception {
		try (BasicReadAheadReader reader = createReader("a,b")) {
			assertTrue(reader.peekCharMatch('a'));
			assertFalse(reader.peekCharMatch('b'));
			assertTrue(reader.peekCharNotMatch('b'));
			assertFalse(reader.peekCharNotMatch('a'));
			assertTrue(reader.peekCharMatchAny("xa"));
			assertFalse(reader.peekCharMatchAny("xy"));
			assertFalse(reader.peekCharMatchAny(null));
			assertTrue(reader.peekCharNotMatchAny("xy"));
			assertFalse(reader.peekCharNotMatchAny("a"));

			assertTrue(reader.peekNextCharMatch(1, ','));
			assertTrue(reader.peekNextCharNotMatch(1, ';'));
			assertTrue(reader.peekNextCharMatchAny(2, "cb"));
			assertFalse(reader.peekNextCharNotMatchAny(2, "cb"));
		}
	}

	@Test
	public void testMatchMethodsAtEndOfData() throws Exception {
		try (BasicReadAheadReader reader = createReader("a")) {
			assertFalse(reader.peekNextCharMatch(1, 'a'));
			assertTrue(reader.peekNextCharNotMatch(1, 'a'));
			reader.readChar();
			assertFalse(reader.peekCharMatch('a'));
			assertTrue(reader.peekCharNotMatch('a'));
			assertFalse(reader.peekCharMatchAny("a"));
			assertTrue(reader.peekCharNotMatchAny("a"));
		}
	}

	@Test
	public void testUtf8BomIsSkipped() throws Exception {
		final byte[] data = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a', 'b' };
		try (BasicReadAheadReader reader = createReader(data, StandardCharsets.UTF_8)) {
			assertEquals(Character.valueOf('a'), reader.peekChar());
			assertEquals("ab", readAll(reader));
			assertEquals(2, reader.getCharactersRead());
		}
	}

	@Test
	public void testUtf16LeBomIsSkipped() throws Exception {
		final byte[] data = { (byte) 0xFF, (byte) 0xFE, 'a', 0, 'b', 0 };
		try (BasicReadAheadReader reader = createReader(data, StandardCharsets.UTF_16LE)) {
			assertEquals("ab", readAll(reader));
		}
	}

	@Test
	public void testBomOnlyInput() throws Exception {
		final byte[] data = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
		try (BasicReadAheadReader reader = createReader(data, StandardCharsets.UTF_8)) {
			assertTrue(reader.isEOF());
		}
	}

	@Test
	public void testUtf8BomWithLatinEncodingIsRejectedAndStreamClosed() {
		final CloseTrackingInputStream inputStream = new CloseTrackingInputStream(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a' });
		assertThrows(IOException.class, () -> new BasicReadAheadReader(inputStream, StandardCharsets.ISO_8859_1));
		// The caller gets no reader to close, so the constructor must close the stream itself
		assertTrue(inputStream.closed);

		final byte[] data = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF, 'a' };
		assertThrows(IOException.class, () -> createReader(data, Charset.forName("windows-1252")));
	}

	@Test
	public void testSingleLatinSmallIDiaeresisIsValidText() throws Exception {
		// 0xEF is 'i with diaeresis' in ISO-8859-1, only the complete sequence EF BB BF is a misread BOM
		try (BasicReadAheadReader reader = createReader(new byte[] { (byte) 0xEF, 'x', 'y' }, StandardCharsets.ISO_8859_1)) {
			assertEquals("\u00EFxy", readAll(reader));
		}
		try (BasicReadAheadReader reader = createReader(new byte[] { (byte) 0xEF, (byte) 0xBB }, StandardCharsets.ISO_8859_1)) {
			assertEquals("\u00EF\u00BB", readAll(reader));
		}
	}

	@Test
	public void testLineAndColumn() throws Exception {
		try (BasicReadAheadReader reader = createReader("ab\r\ncd\re\nf")) {
			assertEquals(1, reader.getCurrentLine());
			assertEquals(1, reader.getCurrentColumn());

			reader.readChar();
			reader.readChar();
			assertEquals(1, reader.getCurrentLine());
			assertEquals(3, reader.getCurrentColumn());

			reader.readChar();
			assertEquals(2, reader.getCurrentLine());
			assertEquals(1, reader.getCurrentColumn());

			// LF of the CRLF does not count as a further line break
			reader.readChar();
			assertEquals(2, reader.getCurrentLine());
			assertEquals(1, reader.getCurrentColumn());

			reader.readChar();
			reader.readChar();
			reader.readChar();
			assertEquals(3, reader.getCurrentLine());

			reader.readChar();
			reader.readChar();
			assertEquals(4, reader.getCurrentLine());

			reader.readChar();
			assertEquals(4, reader.getCurrentLine());
			assertEquals(2, reader.getCurrentColumn());
			assertEquals(10, reader.getCharactersRead());
		}
	}

	@Test
	public void testNormalizedLinebreaks() throws Exception {
		try (BasicReadAheadReader reader = createReader("a\r\nb\rc\nd")) {
			reader.setNormalizeLinebreaks(true);
			assertTrue(reader.isNormalizeLinebreaks());
			assertEquals("a\nb\nc\nd", readAll(reader));
			// A normalized CRLF counts as one character
			assertEquals(7, reader.getCharactersRead());
			assertEquals(4, reader.getCurrentLine());
			assertEquals(2, reader.getCurrentColumn());
		}
	}

	@Test
	public void testNormalizedConsecutiveLinebreaks() throws Exception {
		try (BasicReadAheadReader reader = createReader("\r\r\n\n\r")) {
			reader.setNormalizeLinebreaks(true);
			assertEquals("\n\n\n\n", readAll(reader));
			assertEquals(5, reader.getCurrentLine());
		}
	}

	@Test
	public void testNormalizedLinebreakMatchesCr() throws Exception {
		try (BasicReadAheadReader reader = createReader("\r\nx")) {
			reader.setNormalizeLinebreaks(true);
			assertEquals(Character.valueOf('\n'), reader.peekChar());
			assertTrue(reader.peekCharMatch('\n'));
			assertTrue(reader.peekCharMatch('\r'));
			assertTrue(reader.peekCharMatchAny("\r"));
			assertEquals(Character.valueOf('x'), reader.peekNextChar(1));
		}
		try (BasicReadAheadReader reader = createReader("\nx")) {
			// Without normalization a CR does not match an LF
			assertFalse(reader.peekCharMatch('\r'));
		}
	}

	@Test
	public void testCrAtLastBufferPositionIsPeekedAsLf() throws Exception {
		try (BasicReadAheadReader reader = createReader("ab\r")) {
			reader.setNormalizeLinebreaks(true);
			assertEquals(Character.valueOf('\n'), reader.peekNextChar(2));
			assertTrue(reader.peekNextCharMatch(2, '\r'));
			assertEquals("ab\n", readAll(reader));
		}
		try (BasicReadAheadReader reader = createReader("ab\r\nc")) {
			reader.setNormalizeLinebreaks(true);
			assertEquals(Character.valueOf('\n'), reader.peekNextChar(2));
			assertEquals("ab\nc", readAll(reader));
		}
	}

	@Test
	public void testSwitchingNormalizationOffRestoresLinebreaks() throws Exception {
		try (BasicReadAheadReader reader = createReader("a\r\nb")) {
			reader.setNormalizeLinebreaks(true);
			assertEquals(Character.valueOf('\n'), reader.peekNextChar(1));
			reader.setNormalizeLinebreaks(false);
			assertFalse(reader.isNormalizeLinebreaks());
			assertEquals(Character.valueOf('\r'), reader.peekNextChar(1));
			assertEquals("a\r\nb", readAll(reader));
			assertEquals(2, reader.getCurrentLine());
		}
	}

	@Test
	public void testSwitchingNormalizationOffRestoresMoreThanBufferSize() throws Exception {
		// Restored CRLFs need more room than the read ahead buffer, the rest is pushed back
		try (BasicReadAheadReader reader = createReader("\r\n\r\n\r\nx")) {
			reader.setNormalizeLinebreaks(true);
			reader.setNormalizeLinebreaks(false);
			assertEquals("\r\n\r\n\r\nx", readAll(reader));
			assertEquals(4, reader.getCurrentLine());
		}
	}

	@Test
	public void testSwitchingNormalizationRepeatedly() throws Exception {
		try (BasicReadAheadReader reader = createReader("a\r\nb\r\nc\rd")) {
			final StringBuilder result = new StringBuilder();
			boolean normalize = false;
			Character nextChar;
			while ((nextChar = reader.readChar()) != null) {
				result.append(nextChar.charValue());
				normalize = !normalize;
				reader.setNormalizeLinebreaks(normalize);
			}
			// Each character is delivered exactly once, line breaks either original or normalized
			final String normalizedResult = result.toString().replace("\r\n", "\n").replace('\r', '\n');
			assertEquals("a\nb\nc\nd", normalizedResult);
		}
	}

	@Test
	public void testReadLine() throws Exception {
		try (BasicReadAheadReader reader = createReader("l1\r\nl2\rl3\nl4")) {
			assertEquals("l1", reader.readLine());
			assertEquals("l2", reader.readLine());
			assertEquals("l3", reader.readLine());
			assertEquals("l4", reader.readLine());
			assertNull(reader.readLine());
			assertEquals(4, reader.getCurrentLine());
		}
	}

	@Test
	public void testReadLineEmptyLines() throws Exception {
		try (BasicReadAheadReader reader = createReader("\n\r\n\nx\n")) {
			assertEquals("", reader.readLine());
			assertEquals("", reader.readLine());
			assertEquals("", reader.readLine());
			assertEquals("x", reader.readLine());
			assertNull(reader.readLine());
		}
	}

	@Test
	public void testReadLineNormalized() throws Exception {
		try (BasicReadAheadReader reader = createReader("l1\r\nl2\rl3")) {
			reader.setNormalizeLinebreaks(true);
			assertEquals("l1", reader.readLine());
			assertEquals("l2", reader.readLine());
			assertEquals("l3", reader.readLine());
			assertNull(reader.readLine());
		}
	}

	@Test
	public void testSkipWhitespaces() throws Exception {
		try (BasicReadAheadReader reader = createReader(" \t \n\t x y")) {
			reader.skipWhitespacesInCurrentLine();
			assertEquals(Character.valueOf('\n'), reader.peekChar());
			reader.skipWhitespaces();
			assertEquals(Character.valueOf('x'), reader.peekChar());
			assertEquals(Character.valueOf('x'), reader.readNextNonWhitespace());
			assertEquals(Character.valueOf('y'), reader.readNextNonWhitespace());
			assertNull(reader.readNextNonWhitespace());
			reader.skipWhitespaces();
			reader.skipWhitespacesInCurrentLine();
		}
	}

	@Test
	public void testReadUpToNext() throws Exception {
		try (BasicReadAheadReader reader = createReader("abc,def;ghi")) {
			assertEquals("abc", reader.readUpToNext(false, null, ',', ';'));
			assertEquals(Character.valueOf(','), reader.peekChar());
			reader.readChar();
			assertEquals("def;", reader.readUpToNext(true, null, ';'));
			assertEquals("ghi", reader.readUpToNext(false, null, ','));
			assertTrue(reader.isEOF());
		}
	}

	@Test
	public void testReadUpToNextEscapes() throws Exception {
		final String input = "\\a\\b\\e\\f\\n\\r\\t\\v\\ \\\t\\_\\0\\N\\L\\P\\x41\\u0042\\U0001F600\\\\\\\"\\/\\',rest";
		final String expected = "\u0007\b\u001B\f\n\r\t\u000B \t\u00A0\u0000\u0085\u2028\u2029AB" + new String(Character.toChars(0x1F600)) + "\\\"/'";
		try (BasicReadAheadReader reader = createReader(input)) {
			assertEquals(expected, reader.readUpToNext(false, '\\', ','));
			assertEquals(Character.valueOf(','), reader.peekChar());
		}
	}

	@Test
	public void testReadUpToNextEscapedLimitChar() throws Exception {
		try (BasicReadAheadReader reader = createReader("a\\,b,c")) {
			// An escaped comma is no valid escape sequence in this reader
			assertThrows(Exception.class, () -> reader.readUpToNext(false, '\\', ','));
		}
		try (BasicReadAheadReader reader = createReader("a\\x2cb,c")) {
			assertEquals("a,b", reader.readUpToNext(false, '\\', ','));
		}
	}

	@Test
	public void testReadUpToNextSurrogatePairViaJavaEscapes() throws Exception {
		try (BasicReadAheadReader reader = createReader("\\uD83D\\uDE00")) {
			assertEquals(new String(Character.toChars(0x1F600)), reader.readUpToNext(false, '\\', ','));
		}
	}

	@Test
	public void testReadUpToNextInvalidEscapes() throws Exception {
		try (BasicReadAheadReader reader = createReader("ab\n\\q")) {
			final Exception exception = assertThrows(Exception.class, () -> reader.readUpToNext(false, '\\', ','));
			assertEquals("Invalid escape sequence at character index 2 in line 2 ('\\q')", exception.getMessage());
		}
		try (BasicReadAheadReader reader = createReader("ab\\")) {
			final Exception exception = assertThrows(Exception.class, () -> reader.readUpToNext(false, '\\', ','));
			assertTrue(exception.getMessage().startsWith("Incomplete escape sequence at end of data"), exception.getMessage());
		}
		for (final String invalid : new String[] { "\\x4", "\\xZZ", "\\u12", "\\u12G4", "\\U0011FFFF", "\\U00110000", "\\U0000D800", "\\U1", "\\u\uFF10\uFF10\uFF10\uFF10" }) {
			try (BasicReadAheadReader reader = createReader(invalid)) {
				assertThrows(Exception.class, () -> reader.readUpToNext(false, '\\', ','), invalid);
			}
		}
	}

	@Test
	public void testReadUpToNextInvalidLimitChars() throws Exception {
		try (BasicReadAheadReader reader = createReader("abc")) {
			assertThrows(Exception.class, () -> reader.readUpToNext(false, null, ',', ','));
			assertThrows(Exception.class, () -> reader.readUpToNext(false, ',', ','));
		}
	}

	@Test
	public void testReadUpToNextCrLimitWithNormalization() throws Exception {
		try (BasicReadAheadReader reader = createReader("ab\r\ncd")) {
			reader.setNormalizeLinebreaks(true);
			assertEquals("ab", reader.readUpToNext(false, null, '\r'));
			assertEquals(Character.valueOf('\n'), reader.peekChar());
		}
		try (BasicReadAheadReader reader = createReader("ab\ncd")) {
			assertEquals("ab\ncd", reader.readUpToNext(false, null, '\r'));
		}
	}

	@Test
	public void testReadQuotedText() throws Exception {
		try (BasicReadAheadReader reader = createReader("\"a\\\"b\\x41\" rest")) {
			assertEquals("a\"bA", reader.readQuotedText('\\'));
			assertEquals(Character.valueOf(' '), reader.peekChar());
		}
		try (BasicReadAheadReader reader = createReader("'a\\b'x")) {
			assertEquals("a\\b", reader.readQuotedText(null));
			assertEquals(Character.valueOf('x'), reader.peekChar());
		}
	}

	@Test
	public void testReadQuotedTextDoubledQuote() throws Exception {
		try (BasicReadAheadReader reader = createReader("'it''s','',x")) {
			assertEquals("it's", reader.readQuotedText('\''));
			assertEquals(Character.valueOf(','), reader.readChar());
			assertEquals("", reader.readQuotedText('\''));
			assertEquals(Character.valueOf(','), reader.peekChar());
		}
	}

	@Test
	public void testReadQuotedTextErrors() throws Exception {
		try (BasicReadAheadReader reader = createReader("")) {
			assertThrows(Exception.class, () -> reader.readQuotedText('\\'));
		}
		try (BasicReadAheadReader reader = createReader("\"abc")) {
			assertThrows(Exception.class, () -> reader.readQuotedText('\\'));
		}
		try (BasicReadAheadReader reader = createReader("'abc")) {
			assertThrows(Exception.class, () -> reader.readQuotedText('\''));
		}
		try (BasicReadAheadReader reader = createReader("'abc''")) {
			// The last quote is a doubled one, so the closing quote is missing
			assertThrows(Exception.class, () -> reader.readQuotedText('\''));
		}
	}

	@Test
	public void testReadQuotedTextKeepsOriginalLinebreaks() throws Exception {
		try (BasicReadAheadReader reader = createReader("\"a\r\nb\rc\"\r\nx")) {
			reader.setNormalizeLinebreaks(true);
			assertEquals("a\r\nb\rc", reader.readQuotedText('\\'));
			assertTrue(reader.isNormalizeLinebreaks());
			// Normalization applies again after the closing quote
			assertEquals("\nx", readAll(reader));
		}
	}

	@Test
	public void testReadQuotedTextRestoresNormalizationOnError() throws Exception {
		try (BasicReadAheadReader reader = createReader("\"abc")) {
			reader.setNormalizeLinebreaks(true);
			assertThrows(Exception.class, () -> reader.readQuotedText('\\'));
			assertTrue(reader.isNormalizeLinebreaks());
		}
	}

	@Test
	public void testReadDataSize() throws Exception {
		try (BasicReadAheadReader reader = createReader("\u00E4bc")) {
			// The read ahead buffer is filled on construction, the umlaut takes two bytes in UTF-8
			assertEquals(4, reader.getReadDataSize());
		}
	}

	@Test
	public void testClose() throws Exception {
		final CloseTrackingInputStream inputStream = new CloseTrackingInputStream("ab\ncd".getBytes(StandardCharsets.UTF_8));
		try (final BasicReadAheadReader reader = new BasicReadAheadReader(inputStream)) {
			reader.readChar();
			reader.readChar();
			reader.readChar();
			reader.close();
			assertTrue(inputStream.closed);
			assertEquals(0, reader.getReadDataSize());
			assertThrows(IllegalStateException.class, reader::readChar);
			assertTrue(reader.isEOF());
			assertNull(reader.peekChar());
			// Position information remains available
			assertEquals(2, reader.getCurrentLine());
			assertEquals(3, reader.getCharactersRead());
			// Closing twice is allowed
			reader.close();
		}
	}

	@Test
	public void testStaticHelpers() {
		assertTrue(BasicReadAheadReader.isBlank(null));
		assertTrue(BasicReadAheadReader.isBlank(" \t"));
		assertFalse(BasicReadAheadReader.isBlank(" x "));
		assertTrue(BasicReadAheadReader.isNotEmpty(" "));
		assertFalse(BasicReadAheadReader.isNotEmpty(""));
		assertFalse(BasicReadAheadReader.isNotEmpty(null));
	}
}
