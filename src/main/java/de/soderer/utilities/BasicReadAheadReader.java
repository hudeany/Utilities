package de.soderer.utilities;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

/**
 * Character based base reader for text formats, which offers a fixed size look ahead buffer.
 * <p>
 * The next characters can be inspected by the peek methods before they are consumed by
 * {@link #readChar()}. A leading UTF-8 BOM is skipped. Optionally all line breaks (CRLF, CR) are
 * normalized to LF (see {@link #setNormalizeLinebreaks(boolean)}); a CRLF line break can only be
 * normalized if the look ahead buffer has a size of at least 2 characters.
 * </p>
 */
public class BasicReadAheadReader implements Closeable {
	/** UTF-8 BOM (Byte Order Mark) character for readers. */
	public static final char BOM_UTF_8_CHAR = (char) 65279;

	/** UTF-8 BOM (Byte Order Mark) first character for wrong encoding ISO-8859. */
	public static final char BOM_UTF_8_CHAR_ISO_8859 = (char) 239;

	/** Default input encoding. */
	public static final Charset DEFAULT_ENCODING = StandardCharsets.UTF_8;

	/** Input encoding. */
	private final Charset encoding;

	/**
	 * Normalize linebreaks (\r\n, \n, \r) to unix style (\n). Use
	 * {@link #setNormalizeLinebreaks(boolean)} to change it, so that already buffered characters are
	 * normalized, too.
	 */
	public boolean normalizeLinebreaks = false;

	/** Input reader. */
	private Reader inputReader;

	private long charactersRead = 0;
	private CountingInputStream countingInputStream;

	private final Character[] nextChars;
	private long currentColumn = 1;
	private long currentLine = 1;

	/** The character consumed most recently, used to count a CRLF line break only once. */
	private Character lastReadChar = null;

	/** True if the last call of {@link #readUpToNext(boolean, Character, char...)} stopped at a limit character. */
	private boolean lastReadUpToNextFoundEndChar = false;

	/**
	 * Creates a new reader using the default encoding ({@link #DEFAULT_ENCODING}) and a look ahead
	 * buffer of 3 characters.
	 *
	 * @param inputStream
	 *            the stream to read from, must not be null
	 * @throws Exception
	 *             if the input stream is null or reading the first characters fails
	 */
	public BasicReadAheadReader(final InputStream inputStream) throws Exception {
		this(inputStream, null, 3);
	}

	/**
	 * Creates a new reader using the default encoding ({@link #DEFAULT_ENCODING}).
	 *
	 * @param inputStream
	 *            the stream to read from, must not be null
	 * @param numberOfReadAheadCharacters
	 *            the size of the look ahead buffer, at least 1
	 * @throws Exception
	 *             if the input stream is null, the buffer size is invalid or reading the first
	 *             characters fails
	 */
	public BasicReadAheadReader(final InputStream inputStream, final int numberOfReadAheadCharacters) throws Exception {
		this(inputStream, null, numberOfReadAheadCharacters);
	}

	/**
	 * Creates a new reader using a look ahead buffer of 3 characters.
	 *
	 * @param inputStream
	 *            the stream to read from, must not be null
	 * @param encodingCharset
	 *            the encoding of the input data, or null to use {@link #DEFAULT_ENCODING}
	 * @throws Exception
	 *             if the input stream is null or reading the first characters fails
	 */
	public BasicReadAheadReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		this(inputStream, encodingCharset, 3);
	}

	/**
	 * Creates a new reader.
	 *
	 * @param inputStream
	 *            the stream to read from, must not be null
	 * @param encodingCharset
	 *            the encoding of the input data, or null to use {@link #DEFAULT_ENCODING}
	 * @param numberOfReadAheadCharacters
	 *            the size of the look ahead buffer, at least 1
	 * @throws Exception
	 *             if the input stream is null, the buffer size is invalid or reading the first
	 *             characters fails (also if a UTF-8 BOM is found while reading with an ISO-8859
	 *             encoding)
	 */
	public BasicReadAheadReader(final InputStream inputStream, final Charset encodingCharset, final int numberOfReadAheadCharacters) throws Exception {
		if (numberOfReadAheadCharacters < 1) {
			throw new Exception("Invalid size of read ahead buffer. Minimum 1 was: " + numberOfReadAheadCharacters);
		}

		if (inputStream == null) {
			throw new Exception("Invalid empty inputStream");
		} else {
			countingInputStream = new CountingInputStream(inputStream);
			encoding = encodingCharset == null ? DEFAULT_ENCODING : encodingCharset;
			inputReader = new BufferedReader(new InputStreamReader(countingInputStream, encoding));
		}

		nextChars = new Character[numberOfReadAheadCharacters];

		initializeBuffers();
	}

	/**
	 * Returns whether line breaks are normalized to unix style (LF).
	 *
	 * @return true, if line breaks are normalized
	 */
	protected boolean isNormalizeLinebreaks() {
		return normalizeLinebreaks;
	}

	/**
	 * Sets whether line breaks (CRLF, CR) are normalized to unix style (LF). Characters already in
	 * the look ahead buffer are normalized immediately.
	 *
	 * @param normalizeLinebreaks
	 *            true to normalize line breaks
	 * @throws IOException
	 *             if reading further characters into the look ahead buffer fails
	 */
	protected void setNormalizeLinebreaks(final boolean normalizeLinebreaks) throws IOException {
		this.normalizeLinebreaks = normalizeLinebreaks;
		normalizeLinebreaks();
	}

	private void normalizeLinebreaks() throws IOException {
		if (normalizeLinebreaks) {
			// The last buffer position is not normalized, because its following character is not known yet.
			// It is normalized when it moves forward in the buffer.
			for (int i = 0; i < nextChars.length - 1; i++) {
				if (nextChars[i] != null && nextChars[i] == '\r') {
					if (nextChars[i + 1] != null && nextChars[i + 1] == '\n') {
						nextChars[i] = '\n';
						readNextChars(i + 1);
					} else {
						nextChars[i] = '\n';
					}
				}
			}
		}
	}

	/**
	 * Returns the column of the next character to be read.
	 *
	 * @return the one based column number
	 */
	protected long getCurrentColumn() {
		return currentColumn;
	}

	/**
	 * Returns the line of the next character to be read.
	 *
	 * @return the one based line number
	 */
	protected long getCurrentLine() {
		return currentLine;
	}

	/**
	 * Initially fill the read ahead buffers.
	 * Check for UTF-8 BOM at data start.
	 *
	 * @throws IOException
	 *             if reading fails or a UTF-8 BOM is found while reading with an ISO-8859 encoding
	 */
	private void initializeBuffers() throws IOException {
		int currentCharInt = inputReader.read();
		if (currentCharInt != -1) {
			if (currentCharInt == BOM_UTF_8_CHAR && StandardCharsets.UTF_8.equals(encoding)) {
				currentCharInt = inputReader.read();
				if (currentCharInt != -1) {
					nextChars[0] = (char) currentCharInt;
				}
			} else if (currentCharInt == BOM_UTF_8_CHAR_ISO_8859 && encoding.name().toUpperCase(Locale.ROOT).startsWith("ISO-8859-")) {
				throw new IOException("Data encoding \"" + encoding + "\" is invalid: UTF-8 BOM detected");
			} else {
				nextChars[0] = (char) currentCharInt;
			}
		}

		for (int i = 1; i < nextChars.length; i++) {
			readNextChars(1);
		}
	}

	/**
	 * Consumes the next character.
	 *
	 * @return the next character, or null at the end of data
	 * @throws IOException
	 *             if reading further characters into the look ahead buffer fails
	 */
	protected Character readChar() throws IOException {
		final Character returnChar = nextChars[0];
		readNextChars(0);

		if (returnChar != null) {
			if (returnChar == '\r' || (returnChar == '\n' && (lastReadChar == null || lastReadChar != '\r'))) {
				currentColumn = 1;
				currentLine++;
			} else if (returnChar != '\n') {
				// A LF directly after CR is part of an already counted CRLF line break
				currentColumn++;
			}
			charactersRead++;
		}

		lastReadChar = returnChar;
		return returnChar;
	}

	private void readNextChars(final int startIndex) throws IOException {
		for (int i = startIndex; i < nextChars.length - 1; i++) {
			nextChars[i] = nextChars[i + 1];
		}

		final int newNextCharInt = inputReader.read();
		if (newNextCharInt == -1) {
			nextChars[nextChars.length - 1] = null;
		} else {
			nextChars[nextChars.length - 1] = (char) newNextCharInt;
		}

		normalizeLinebreaks();
	}

	/**
	 * Returns the next character without consuming it.
	 *
	 * @return the next character, or null at the end of data
	 */
	protected Character peekChar() {
		return nextChars[0];
	}

	/**
	 * Checks whether the next character equals the given character.
	 *
	 * @param otherChar
	 *            the character to compare with
	 * @return true, if the next character equals the given character, false at the end of data
	 */
	protected boolean peekCharMatch(final char otherChar) {
		if (nextChars[0] == null) {
			return false;
		} else {
			return nextChars[0] == otherChar;
		}
	}

	/**
	 * Checks whether the next character differs from the given character.
	 *
	 * @param otherChar
	 *            the character to compare with
	 * @return true, if the next character differs from the given character or the end of data was
	 *         reached
	 */
	protected boolean peekCharNotMatch(final char otherChar) {
		return !peekCharMatch(otherChar);
	}

	/**
	 * Checks whether the next character is one of the given characters.
	 *
	 * @param charSequence
	 *            the characters to compare with, may be null
	 * @return true, if the next character is contained in the given characters, false at the end of
	 *         data
	 */
	protected boolean peekCharMatchAny(final String charSequence) {
		if (nextChars[0] == null || charSequence == null) {
			return false;
		} else {
			return charSequence.contains(nextChars[0].toString());
		}
	}

	/**
	 * Checks whether the next character is none of the given characters.
	 *
	 * @param charSequence
	 *            the characters to compare with, may be null
	 * @return true, if the next character is not contained in the given characters or the end of
	 *         data was reached
	 */
	protected boolean peekCharNotMatchAny(final String charSequence) {
		return !peekCharMatchAny(charSequence);
	}

	/**
	 * Returns a character of the look ahead buffer without consuming it.
	 *
	 * @param indexOfNextChar
	 *            the index in the look ahead buffer, 0 for the next character
	 * @return the character, or null if the end of data is reached before
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the index is not within the look ahead buffer
	 */
	protected Character peekNextChar(final int indexOfNextChar) {
		return nextChars[indexOfNextChar];
	}

	/**
	 * Checks whether a character of the look ahead buffer equals the given character. If line breaks
	 * are normalized, a not yet normalized CR also matches LF.
	 *
	 * @param indexOfNextChar
	 *            the index in the look ahead buffer, 0 for the next character
	 * @param otherChar
	 *            the character to compare with
	 * @return true, if the character equals the given character, false if the end of data is reached
	 *         before
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the index is not within the look ahead buffer
	 */
	protected boolean peekNextCharMatch(final int indexOfNextChar, final char otherChar) {
		if (nextChars[indexOfNextChar] == null) {
			return false;
		} else if (otherChar == '\n') {
			return nextChars[indexOfNextChar] == '\n' || (normalizeLinebreaks && nextChars[indexOfNextChar] == '\r');
		} else {
			return nextChars[indexOfNextChar] == otherChar;
		}
	}

	/**
	 * Checks whether a character of the look ahead buffer differs from the given character.
	 *
	 * @param indexOfNextChar
	 *            the index in the look ahead buffer, 0 for the next character
	 * @param otherChar
	 *            the character to compare with
	 * @return true, if the character differs from the given character or the end of data is reached
	 *         before
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the index is not within the look ahead buffer
	 */
	protected boolean peekNextCharNotMatch(final int indexOfNextChar, final char otherChar) {
		return !peekNextCharMatch(indexOfNextChar, otherChar);
	}

	/**
	 * Checks whether a character of the look ahead buffer is one of the given characters. If line
	 * breaks are normalized, a not yet normalized CR also matches LF.
	 *
	 * @param indexOfNextChar
	 *            the index in the look ahead buffer, 0 for the next character
	 * @param charSequence
	 *            the characters to compare with, may be null
	 * @return true, if the character is contained in the given characters, false if the end of data
	 *         is reached before
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the index is not within the look ahead buffer
	 */
	protected boolean peekNextCharMatchAny(final int indexOfNextChar, final String charSequence) {
		if (nextChars[indexOfNextChar] == null || charSequence == null) {
			return false;
		} else if (normalizeLinebreaks) {
			return charSequence.replace("\n",  "\n\r").contains(nextChars[indexOfNextChar].toString());
		} else {
			return charSequence.contains(nextChars[indexOfNextChar].toString());
		}
	}

	/**
	 * Checks whether a character of the look ahead buffer is none of the given characters.
	 *
	 * @param indexOfNextChar
	 *            the index in the look ahead buffer, 0 for the next character
	 * @param charSequence
	 *            the characters to compare with, may be null
	 * @return true, if the character is not contained in the given characters or the end of data is
	 *         reached before
	 * @throws ArrayIndexOutOfBoundsException
	 *             if the index is not within the look ahead buffer
	 */
	protected boolean peekNextCharNotMatchAny(final int indexOfNextChar, final String charSequence) {
		return !peekNextCharMatchAny(indexOfNextChar, charSequence);
	}

	/**
	 * Checks whether the end of data was reached.
	 *
	 * @return true, if there are no more characters to read
	 */
	protected boolean isEOF() {
		return nextChars[0] == null;
	}

	/**
	 * Checks whether there are more characters to read.
	 *
	 * @return true, if the end of data was not reached yet
	 */
	protected boolean isNotEOF() {
		return nextChars[0] != null;
	}

	/**
	 * Consumes all whitespace characters (including line breaks) up to the next non-whitespace
	 * character.
	 *
	 * @throws Exception
	 *             if reading fails
	 */
	protected void skipWhitespaces() throws Exception {
		while (nextChars[0] != null && Character.isWhitespace(nextChars[0])) {
			readChar();
		}
	}

	/**
	 * Consumes all whitespace characters except line breaks up to the next non-whitespace character
	 * or line break.
	 *
	 * @throws Exception
	 *             if reading fails
	 */
	protected void skipWhitespacesInCurrentLine() throws Exception {
		while (nextChars[0] != null && nextChars[0] != '\r' && nextChars[0] != '\n' && Character.isWhitespace(nextChars[0])) {
			readChar();
		}
	}

	/**
	 * Reads the rest of the current line. The terminating line break (LF, CRLF or CR) is consumed but
	 * not included.
	 *
	 * @return the line, or null if the end of data was reached before
	 * @throws Exception
	 *             if reading fails
	 */
	protected String readLine() throws Exception {
		if (isEOF()) {
			return null;
		} else {
			final StringBuilder nextLine = new StringBuilder();
			while (isNotEOF() && nextChars[0] != '\r' && nextChars[0] != '\n') {
				nextLine.append(readChar());
			}
			if (isNotEOF()) {
				// Consume the line break, a not normalized CRLF consists of two characters
				final Character lineBreakChar = readChar();
				if (lineBreakChar == '\r' && peekCharMatch('\n')) {
					readChar();
				}
			}
			return nextLine.toString();
		}
	}

	/**
	 * Reads text up to the next occurrence of one of the given limit characters, starting with the
	 * next character.
	 * <p>
	 * Escape sequences introduced by the escape character are resolved: the escape character itself,
	 * double quote, single quote, slash, blank, tab, a (bell), b, e (escape), f, n, r, t, v (vertical
	 * tab), _ (non-breaking space), 0 (null), N (next line), L (line separator), P (paragraph
	 * separator), xHH, uHHHH and UHHHHHHHH (unicode code point). An escaped limit character does not
	 * end the text.
	 * </p>
	 *
	 * @param includeEndChar
	 *            true to consume and append the found limit character, false to leave it as the next
	 *            character
	 * @param escapeCharacter
	 *            the escape character, or null if no escaping is used
	 * @param endChars
	 *            the limit characters ending the text
	 * @return the text read, or the text up to the end of data if no limit character was found
	 * @throws Exception
	 *             if the limit characters are not distinct, the escape character is one of the limit
	 *             characters, an invalid escape sequence is found or reading fails
	 */
	protected String readUpToNext(final boolean includeEndChar, final Character escapeCharacter, final char... endChars) throws Exception {
		lastReadUpToNextFoundEndChar = false;
		if (anyCharsAreEqual(endChars)) {
			throw new Exception("Invalid set of limit characters, includes duplicates: '" + Utilities.join(endChars, "', '") + "'");
		} else if (escapeCharacter != null && contains(endChars, escapeCharacter)) {
			throw new Exception("Invalid escape character is included in limit characters: '" + escapeCharacter + "'");
		}

		final StringBuilder returnValue = new StringBuilder();
		boolean unescapeNextCharacter = false;
		while (isNotEOF()) {
			if (unescapeNextCharacter) {
				unescapeNextCharacter = false;
				char unescapedChar;
				if (escapeCharacter != null && escapeCharacter.equals(peekChar())) {
					unescapedChar = readChar();
				} else if ('"' == peekChar()) {
					unescapedChar = readChar();
				} else if ('\'' == peekChar()) {
					// Single quotes should not be escaped, but they are allowed here for user convenience
					unescapedChar = readChar();
				} else if ('/' == peekChar()) {
					unescapedChar = readChar();
				} else if ('a' == peekChar()) {
					readChar();
					unescapedChar = '\u0007'; // ASCII Alert/Bell
				} else if ('b' == peekChar()) {
					readChar();
					unescapedChar = '\b';
				} else if ('e' == peekChar()) {
					readChar();
					unescapedChar = '\u001B'; // ESC
				} else if ('f' == peekChar()) {
					readChar();
					unescapedChar = '\f';
				} else if ('n' == peekChar()) {
					readChar();
					unescapedChar = '\n';
				} else if ('r' == peekChar()) {
					readChar();
					unescapedChar = '\r';
				} else if ('t' == peekChar()) {
					readChar();
					unescapedChar = '\t';
				} else if ('v' == peekChar()) {
					readChar();
					unescapedChar = '\u000B'; // Vertical Tab
				} else if (' ' == peekChar()) {
					readChar();
					unescapedChar = ' ';
				} else if ('\t' == peekChar()) {
					readChar();
					unescapedChar = '\t';
				} else if ('_' == peekChar()) {
					readChar();
					unescapedChar = ' '; // NBSP/non-breaking space
				} else if ('0' == peekChar()) {
					readChar();
					unescapedChar = '\0';
				} else if ('N' == peekChar()) {
					readChar();
					unescapedChar = '\u0085'; // Next Line
				} else if ('L' == peekChar()) {
					readChar();
					unescapedChar = ' '; // Unicode LINE SEPARATOR (LS or LSEP)
				} else if ('P' == peekChar()) {
					readChar();
					unescapedChar = ' '; // Unicode PARAGRAPH SEPARATOR (PS or PSEP)
				} else if ('x' == peekChar()) {
					readChar();
					// hexadecimal encoded character
					unescapedChar = (char) readHexCodePoint(2);
				} else if ('u' == peekChar()) {
					readChar();
					// Java encoded character
					unescapedChar = (char) readHexCodePoint(4);
				} else if ('U' == peekChar()) {
					readChar();
					// Unicode encoded code point, which may need a surrogate pair
					final int codePoint = readHexCodePoint(8);
					if (!Character.isValidCodePoint(codePoint)) {
						throw new Exception("Invalid unicode code point at character index " + getCurrentColumn() + " in line " + getCurrentLine() + " ('" + Integer.toHexString(codePoint) + "')");
					}
					returnValue.appendCodePoint(codePoint);
					continue;
				} else {
					throw new Exception("Invalid escape sequence at character index " + getCurrentColumn() + " in line " + getCurrentLine() + " ('" + escapeCharacter + nextChars[0] + "')");
				}
				returnValue.append(unescapedChar);
			} else if (escapeCharacter != null && escapeCharacter.equals(peekChar())) {
				readChar();
				unescapeNextCharacter = true;
			} else if (contains(endChars, peekChar())) {
				if (includeEndChar) {
					returnValue.append(readChar());
				}
				lastReadUpToNextFoundEndChar = true;
				return returnValue.toString();
			} else {
				returnValue.append(readChar());
			}
		}
		return returnValue.toString();
	}

	/**
	 * Reads a fixed number of hexadecimal digits.
	 *
	 * @param numberOfDigits
	 *            the number of digits to read, at most 8
	 * @return the value of the digits
	 * @throws Exception
	 *             if a character is no hexadecimal digit or the end of data is reached before
	 */
	private int readHexCodePoint(final int numberOfDigits) throws Exception {
		final StringBuilder hexcode = new StringBuilder();
		for (int i = 0; i < numberOfDigits && isNotEOF(); i++) {
			final Character hexDigit = readChar();
			hexcode.append(hexDigit);
			if (Character.digit(hexDigit, 16) == -1) {
				throw new Exception("Invalid unicode sequence at character index " + (getCurrentColumn() - hexcode.length()) + " in line " + getCurrentLine() + " ('" + hexcode + "')");
			}
		}
		if (hexcode.length() != numberOfDigits) {
			throw new Exception("Invalid unicode sequence at character index " + (getCurrentColumn() - hexcode.length()) + " in line " + getCurrentLine() + " ('" + hexcode + "')");
		}
		return (int) Long.parseLong(hexcode.toString(), 16);
	}

	/**
	 * Reads a quoted text. The next character must be the opening quote character, which is also used
	 * as closing quote character. Line breaks are not normalized within the quoted text.
	 *
	 * @param escapeCharacter
	 *            the escape character, or null if no escaping is used. If it equals the quote
	 *            character, a doubled quote character represents a single one, otherwise escape
	 *            sequences are resolved as described in
	 *            {@link #readUpToNext(boolean, Character, char...)}.
	 * @return the unquoted text with resolved escape sequences
	 * @throws Exception
	 *             if the end of data was reached before the opening or closing quote, an invalid
	 *             escape sequence is found or reading fails
	 */
	protected String readQuotedText(final Character escapeCharacter) throws Exception {
		final long columnStart = currentColumn;
		final long lineStart = currentLine;
		final boolean previousSettingNormalizeLinebreaks = normalizeLinebreaks;
		try {
			normalizeLinebreaks = false;
			final Character quoteCharacter = readChar();
			if (quoteCharacter == null) {
				throw new Exception("Missing opening quote character at line " + lineStart + " column " + columnStart);
			}
			final char quoteChar = quoteCharacter;
			if (escapeCharacter != null && escapeCharacter == quoteChar) {
				final StringBuilder returnValue = new StringBuilder();
				boolean closed = false;
				while (isNotEOF()) {
					if (peekCharNotMatch(quoteChar)) {
						returnValue.append(readChar());
					} else if (nextChars.length > 1 && peekNextCharMatch(1, quoteChar)) {
						// Doubled quote character represents a single one
						returnValue.append(readChar());
						readChar();
					} else {
						// Closing quote
						readChar();
						closed = true;
						break;
					}
				}
				if (!closed) {
					throw new Exception("Missing closing quote character for quoted string beginning at line " + lineStart + " column " + columnStart);
				} else {
					return returnValue.toString();
				}
			} else {
				final String returnValue = readUpToNext(true, escapeCharacter, quoteChar);
				if (!lastReadUpToNextFoundEndChar) {
					throw new Exception("Missing closing quote character for quoted string beginning at line " + lineStart + " column " + columnStart);
				} else {
					return returnValue.substring(0, returnValue.length() - 1);
				}
			}
		} finally {
			setNormalizeLinebreaks(previousSettingNormalizeLinebreaks);
		}
	}

	/**
	 * Closes this reader and its underlying stream.
	 */
	@Override
	public void close() {
		closeQuietly(inputReader);
		inputReader = null;
		closeQuietly(countingInputStream);
		countingInputStream = null;

		Arrays.fill(nextChars, null);
		currentColumn = 1;
		currentLine = 1;
	}

	/**
	 * Check if String value is null or contains only whitespace characters.
	 *
	 * @param value
	 *            the value
	 * @return true, if is blank
	 */
	protected static boolean isBlank(final String value) {
		return value == null || value.trim().length() == 0;
	}

	/**
	 * Check if String value is not null and has a length greater than 0.
	 *
	 * @param value
	 *            the value
	 * @return true, if is not empty
	 */
	protected static boolean isNotEmpty(final String value) {
		return value != null && value.length() > 0;
	}

	/**
	 * Close a Closable item and ignore any Exception thrown by its close method.
	 *
	 * @param closeableItem
	 *            the closeable item
	 */
	private static void closeQuietly(final Closeable closeableItem) {
		if (closeableItem != null) {
			try {
				closeableItem.close();
			} catch (@SuppressWarnings("unused") final IOException e) {
				// Do nothing
			}
		}
	}

	/**
	 * Check if any characters in a list are equal
	 *
	 * @param values
	 *            the characters to check
	 * @return true, if at least two characters are equal
	 */
	private static boolean anyCharsAreEqual(final char... values) {
		for (int i = 0; i < values.length; i++) {
			for (int j = i + 1; j < values.length; j++) {
				if (values[i] == values[j]) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check if character array contains specific character
	 *
	 * @param characterArray
	 *            the characters to search in, may be null
	 * @param searchCharacter
	 *            the character to search for, may be null
	 * @return true, if the character is contained in the array
	 */
	private static boolean contains(final char[] characterArray, final Character searchCharacter) {
		if (characterArray == null || searchCharacter == null) {
			return false;
		} else {
			for (final char character : characterArray) {
				if (character == searchCharacter) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Returns the number of bytes read from the underlying input stream so far. Because of internal
	 * buffering this may be ahead of the characters actually consumed.
	 *
	 * @return the number of bytes read, or 0 if this reader was closed
	 */
	public long getReadDataSize() {
		if (countingInputStream != null) {
			return countingInputStream.getByteCount();
		} else {
			return 0;
		}
	}

	/**
	 * Returns the number of characters consumed so far. A normalized CRLF line break counts as one
	 * character.
	 *
	 * @return the number of characters consumed
	 */
	public long getCharactersRead() {
		return charactersRead;
	}
}
