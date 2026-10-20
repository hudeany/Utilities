package de.soderer.utilities;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.soderer.utilities.collection.CaseInsensitiveLinkedMap;

/**
 * Reader for .properties files, which, unlike java.util.Properties, keeps the order of the keys and also provides
 * the comments of the keys, the duplicate keys and the number of empty lines before each key (e.g. to rewrite a file
 * with the same structure). Keys are read case insensitive by default.
 * Escapes, continuation lines and separators ('=', ':' or whitespace) are handled like java.util.Properties does.
 * <p>
 * Without a given encoding the data is read as UTF-8, and as ISO-8859-1 if it is no valid UTF-8, like
 * java.util.PropertyResourceBundle does since Java 9. So files written with or without unicode escapes are both
 * readable. A leading byte order mark is ignored.
 * </p>
 */
public class PropertiesReader implements Closeable {
	private static final Pattern UNICODE_ESCAPE_PATTERN = Pattern.compile("\\\\u([0-9A-Fa-f]{4})");

	/** Byte order mark, which is ignored at the beginning of the data */
	private static final char BYTE_ORDER_MARK = '\uFEFF';

	private InputStream inputStream;
	private BufferedReader inputReader;

	/** Encoding used to decode the data */
	private final Charset encoding;

	private boolean readKeysCaseInsensitive = true;

	/**
	 * If a key occurs more than once, keep the value of its first occurrence.
	 * Default is false, which keeps the value of the last occurrence like java.util.Properties does.
	 */
	private boolean firstDuplicateValueWins = false;

	private String pendingCommentLines = null;

	/**
	 * Created in read(), so it uses the same key case sensitivity as the read properties
	 */
	private Map<String, String> comments = new LinkedHashMap<>();

	/**
	 * Keys that occurred more than once in the read data, in order of their first duplicate occurrence.
	 * Created in read(), so it uses the same key case sensitivity as the read properties
	 */
	private Set<String> duplicateKeys = new LinkedHashSet<>();

	/**
	 * Number of empty lines before each key (counted since the previous property line, including those before its comment).
	 * Only the first occurrence of a key is counted, because its position is also taken from there.
	 * Keys without preceding empty lines are not contained.
	 * Created in read(), so it uses the same key case sensitivity as the read properties
	 */
	private Map<String, Integer> emptyLinesBefore = new LinkedHashMap<>();

	/**
	 * Creates a reader, which detects the encoding: the data is read as UTF-8, and as ISO-8859-1 if it is no valid
	 * UTF-8 (like java.util.PropertyResourceBundle since Java 9). The whole data is read and decoded immediately,
	 * because invalid UTF-8 bytes may occur anywhere in the data.
	 *
	 * @param inputStream stream to read, closed by {@link #close()}
	 * @throws Exception if the stream is null or cannot be read
	 */
	public PropertiesReader(final InputStream inputStream) throws Exception {
		if (inputStream == null) {
			throw new Exception("Invalid empty inputStream");
		}

		this.inputStream = inputStream;
		final byte[] data;
		try {
			data = inputStream.readAllBytes();
		} catch (final IOException e) {
			// The caller gets no reader to close, so the stream is closed here
			closeQuietly(inputStream);
			throw e;
		}
		String text;
		Charset detectedEncoding;
		try {
			text = StandardCharsets.UTF_8.newDecoder()
					.onMalformedInput(CodingErrorAction.REPORT)
					.onUnmappableCharacter(CodingErrorAction.REPORT)
					.decode(ByteBuffer.wrap(data))
					.toString();
			detectedEncoding = StandardCharsets.UTF_8;
		} catch (@SuppressWarnings("unused") final CharacterCodingException e) {
			// Every byte sequence is valid ISO-8859-1
			text = new String(data, StandardCharsets.ISO_8859_1);
			detectedEncoding = StandardCharsets.ISO_8859_1;
		}
		encoding = detectedEncoding;

		if (!text.isEmpty() && text.charAt(0) == BYTE_ORDER_MARK) {
			text = text.substring(1);
		}
		inputReader = new BufferedReader(new StringReader(text));
	}

	/**
	 * Creates a reader for data in the given encoding. A leading byte order mark is ignored.
	 *
	 * @param inputStream stream to read, closed by {@link #close()}
	 * @param encoding encoding of the data
	 * @throws Exception if the stream is null or cannot be read
	 */
	public PropertiesReader(final InputStream inputStream, final Charset encoding) throws Exception {
		if (inputStream == null) {
			throw new Exception("Invalid empty inputStream");
		} else if (encoding == null) {
			throw new Exception("Invalid empty encoding");
		}

		this.inputStream = inputStream;
		this.encoding = encoding;
		inputReader = new BufferedReader(new InputStreamReader(inputStream, encoding));
		try {
			skipByteOrderMark(inputReader);
		} catch (final IOException e) {
			// The caller gets no reader to close, so the stream is closed here
			closeQuietly(inputReader);
			closeQuietly(inputStream);
			throw e;
		}
	}

	private static void skipByteOrderMark(final BufferedReader reader) throws IOException {
		reader.mark(1);
		if (reader.read() != BYTE_ORDER_MARK) {
			reader.reset();
		}
	}

	/**
	 * Encoding used to decode the data. With encoding detection this is UTF-8 for valid UTF-8 data (including pure
	 * ASCII data, e.g. files with unicode escapes only), and ISO-8859-1 otherwise.
	 *
	 * @return the encoding
	 */
	public Charset getEncoding() {
		return encoding;
	}

	/**
	 * Whether keys are read case insensitive.
	 *
	 * @return true for case insensitive keys (default)
	 */
	public boolean isReadKeysCaseInsensitive() {
		return readKeysCaseInsensitive;
	}

	/**
	 * Sets whether keys are read case insensitive. Must be set before {@link #read()}.
	 *
	 * @param readKeysCaseInsensitive true for case insensitive keys
	 */
	public void setReadKeysCaseInsensitive(final boolean readKeysCaseInsensitive) {
		this.readKeysCaseInsensitive = readKeysCaseInsensitive;
	}

	/**
	 * Sets whether keys are read case insensitive. Must be set before {@link #read()}.
	 *
	 * @param readKeysCaseInsensitiveParam true for case insensitive keys
	 * @return this instance for chaining
	 */
	public PropertiesReader withReadKeysCaseInsensitive(final boolean readKeysCaseInsensitiveParam) {
		setReadKeysCaseInsensitive(readKeysCaseInsensitiveParam);
		return this;
	}

	/**
	 * Whether the value of the first occurrence of a duplicate key is kept.
	 *
	 * @return true to keep the first value, false to keep the last value (default)
	 */
	public boolean isFirstDuplicateValueWins() {
		return firstDuplicateValueWins;
	}

	/**
	 * Sets whether the value of the first occurrence of a duplicate key is kept.
	 *
	 * @param firstDuplicateValueWins true to keep the first value, false to keep the last value (default)
	 */
	public void setFirstDuplicateValueWins(final boolean firstDuplicateValueWins) {
		this.firstDuplicateValueWins = firstDuplicateValueWins;
	}

	/**
	 * Sets whether the value of the first occurrence of a duplicate key is kept.
	 *
	 * @param firstDuplicateValueWinsParam true to keep the first value, false to keep the last value (default)
	 * @return this instance for chaining
	 */
	public PropertiesReader withFirstDuplicateValueWins(final boolean firstDuplicateValueWinsParam) {
		setFirstDuplicateValueWins(firstDuplicateValueWinsParam);
		return this;
	}

	/**
	 * Reads all properties of the stream. Comments, duplicate keys and empty lines are available afterwards via
	 * {@link #getComments()}, {@link #getDuplicateKeys()} and {@link #getEmptyLinesBefore()}.
	 *
	 * @return keys and unescaped values in the order of their first occurrence
	 * @throws IllegalStateException if this reader is already closed
	 * @throws Exception if the data cannot be read or contains invalid escapes
	 */
	public Map<String, String> read() throws Exception {
		if (inputReader == null) {
			throw new IllegalStateException("PropertiesReader is already closed");
		}

		Map<String, String> properties;
		if (readKeysCaseInsensitive) {
			properties = new CaseInsensitiveLinkedMap<>();
			comments = new CaseInsensitiveLinkedMap<>();
			duplicateKeys = Collections.newSetFromMap(new CaseInsensitiveLinkedMap<>());
			emptyLinesBefore = new CaseInsensitiveLinkedMap<>();
		} else {
			properties = new LinkedHashMap<>();
			comments = new LinkedHashMap<>();
			duplicateKeys = new LinkedHashSet<>();
			emptyLinesBefore = new LinkedHashMap<>();
		}
		// Null as long as no comment line is pending, so comments consisting of empty lines are kept, too
		pendingCommentLines = null;
		int pendingEmptyLines = 0;

		String nextLine;
		while ((nextLine = inputReader.readLine()) != null) {
			final String leftTrimmedNextLine = trimLeft(nextLine);
			if (leftTrimmedNextLine.startsWith("#")
					|| leftTrimmedNextLine.startsWith("!")) {
				if (pendingCommentLines == null) {
					pendingCommentLines = "";
				} else {
					pendingCommentLines += "\n";
				}
				// Only the single blank written by PropertiesWriter after the comment char is removed, so indentation within comments is kept
				String commentText = leftTrimmedNextLine.substring(1);
				if (commentText.startsWith(" ")) {
					commentText = commentText.substring(1);
				}
				pendingCommentLines += unescapeComment(commentText.stripTrailing());
			} else if (leftTrimmedNextLine.equals("\\") && !isAtEndOfData()) {
				// A line consisting of a continuation backslash only adds nothing, so java.util.Properties treats the
				// following line as a new line (which may be a comment or an empty line)
			} else if (leftTrimmedNextLine.length() > 0) {
				// Join continuation lines to the logical line before splitting it into key and value, because a line break
				// may also occur within the key (like java.util.Properties allows).
				// Continuation lines only lose their leading whitespace, trailing whitespace is kept.
				final StringBuilder logicalLine = new StringBuilder(nextLine);
				while (endsWithUnescapedBackslash(logicalLine)) {
					logicalLine.setLength(logicalLine.length() - 1);
					final String continuationLine = inputReader.readLine();
					if (continuationLine == null) {
						// A backslash at the end of data is dropped, like java.util.Properties does
						break;
					}
					logicalLine.append(trimLeft(continuationLine));
				}

				final String line = logicalLine.toString();
				final int separatorIndex = findKeyValueSeparator(line);
				final String key;
				final String value;
				if (separatorIndex == -1) {
					key = line;
					value = "";
				} else {
					key = line.substring(0, separatorIndex);
					value = line.substring(separatorIndex + 1);
				}

				final String unescapedKey = unescapeKey(key);
				final String unescapedValue = unescapeValue(value);

				// The pending comment belongs to this line, also if the line itself is skipped as duplicate
				final String commentOfLine = pendingCommentLines;
				pendingCommentLines = null;

				// The pending empty lines belong to this line, also if they were followed by a comment
				final int emptyLinesOfLine = pendingEmptyLines;
				pendingEmptyLines = 0;

				final boolean isDuplicateKey = properties.containsKey(unescapedKey);
				if (isDuplicateKey) {
					duplicateKeys.add(unescapedKey);
				} else if (emptyLinesOfLine > 0) {
					emptyLinesBefore.put(unescapedKey, emptyLinesOfLine);
				}

				if (firstDuplicateValueWins && isDuplicateKey) {
					// Later occurrence of a known key: keep the first value, only adopt a comment the first occurrence is missing
					if (commentOfLine != null && !comments.containsKey(unescapedKey)) {
						comments.put(unescapedKey, commentOfLine);
					}
				} else {
					properties.put(unescapedKey, unescapedValue);
					if (commentOfLine != null) {
						comments.put(unescapedKey, commentOfLine);
					}
				}
			} else {
				// Empty or whitespace-only line, used to structure blocks of properties.
				// Empty lines inside multi-line values never get here, because they are consumed by the continuation loop.
				pendingEmptyLines++;
			}
		}

		return properties;
	}

	/**
	 * Finds the end of the key like java.util.Properties: the key ends at the first unescaped '=', ':' or whitespace.
	 * Whitespace after the key may be followed by one '=' or ':', which is then the separator.
	 *
	 * @return index of the separator char ('=', ':' or the whitespace ending the key), or -1 if the line contains
	 *         only a key
	 */
	private static int findKeyValueSeparator(final String line) {
		final int length = line.length();
		int index = 0;
		while (index < length && isPropertiesWhitespace(line.charAt(index))) {
			index++;
		}
		while (index < length) {
			final char nextChar = line.charAt(index);
			if (nextChar == '\\') {
				// Escaped char is part of the key
				index += 2;
			} else if (nextChar == '=' || nextChar == ':') {
				return index;
			} else if (isPropertiesWhitespace(nextChar)) {
				break;
			} else {
				index++;
			}
		}
		if (index >= length) {
			return -1;
		}

		// Whitespace ended the key. An unescaped '=' or ':' after it is the actual separator, anything else starts the value.
		final int whitespaceIndex = index;
		while (index < length && isPropertiesWhitespace(line.charAt(index))) {
			index++;
		}
		if (index < length && (line.charAt(index) == '=' || line.charAt(index) == ':')) {
			return index;
		} else {
			return whitespaceIndex;
		}
	}

	/**
	 * Checks without consuming anything, whether there is no more data to read
	 */
	private boolean isAtEndOfData() throws IOException {
		inputReader.mark(1);
		final boolean atEnd = inputReader.read() == -1;
		inputReader.reset();
		return atEnd;
	}

	/**
	 * Whitespace as defined by java.util.Properties: blank, tab and form feed. Other Unicode whitespace (like an em
	 * space) is part of keys and values, as PropertiesWriter does not escape it in UTF-8 files.
	 */
	private static boolean isPropertiesWhitespace(final char character) {
		return character == ' ' || character == '\t' || character == '\f';
	}

	/**
	 * Removes leading whitespace as defined by java.util.Properties
	 */
	private static String trimLeft(final String text) {
		int start = 0;
		while (start < text.length() && isPropertiesWhitespace(text.charAt(start))) {
			start++;
		}
		return text.substring(start);
	}

	/**
	 * Unescape a key in its stored form (counterpart of PropertiesWriter.escapeKey()),
	 * including the whitespace trimming that is applied when reading a .properties file.
	 *
	 * @param rawKey key as stored in the file
	 * @return unescaped key
	 * @throws Exception if the key contains an invalid escape sequence
	 */
	public static String unescapeKey(final String rawKey) throws Exception {
		try {
			return Utilities.unescapePropertiesString(trimKey(rawKey));
		} catch (final Exception e) {
			throw new Exception("Invalid properties encoded key: " + rawKey, e);
		}
	}

	/**
	 * Trim leading whitespace and trailing whitespace, but keep trailing whitespace that is escaped by a backslash.
	 */
	private static String trimKey(final String rawKey) {
		final String key = trimLeft(rawKey);
		int end = key.length();
		while (end > 0 && isPropertiesWhitespace(key.charAt(end - 1)) && !isEscapedAt(key, end - 1)) {
			end--;
		}
		return key.substring(0, end);
	}

	/**
	 * Check whether the char at the given index is escaped by an odd number of preceding backslashes.
	 */
	private static boolean isEscapedAt(final String text, final int index) {
		int backslashCount = 0;
		for (int i = index - 1; i >= 0 && text.charAt(i) == '\\'; i--) {
			backslashCount++;
		}
		return backslashCount % 2 == 1;
	}

	/**
	 * Unescape a value in its stored form (counterpart of PropertiesWriter.escapeValue()),
	 * including the leading whitespace trimming that is applied when reading a .properties file.
	 *
	 * @param rawValue value as stored in the file
	 * @return unescaped value
	 * @throws Exception if the value contains an invalid escape sequence
	 */
	public static String unescapeValue(final String rawValue) throws Exception {
		try {
			return Utilities.unescapePropertiesString(trimLeft(rawValue));
		} catch (final Exception e) {
			throw new Exception("Invalid properties encoded value: " + rawValue, e);
		}
	}

	/**
	 * Decode unicode escapes in comment lines, which are written for chars not representable in the file encoding
	 * (and for a byte order mark and unpaired surrogates).
	 */
	private static String unescapeComment(final String commentLine) {
		final Matcher matcher = UNICODE_ESCAPE_PATTERN.matcher(commentLine);
		final StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			matcher.appendReplacement(result, Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(matcher.group(1), 16))));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	/**
	 * Whether the text ends with an odd number of backslashes, which marks a continuation line
	 */
	private static boolean endsWithUnescapedBackslash(final CharSequence text) {
		int backslashCount = 0;
		for (int i = text.length() - 1; i >= 0 && text.charAt(i) == '\\'; i--) {
			backslashCount++;
		}
		return backslashCount % 2 == 1;
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
	 * Close this reader and its underlying stream.
	 */
	@Override
	public void close() {
		closeQuietly(inputReader);
		inputReader = null;
		closeQuietly(inputStream);
		inputStream = null;
	}

	/**
	 * Comments of the keys read by {@link #read()} (lines starting with '#' or '!' directly or indirectly before a key,
	 * without the comment character, multiple lines joined by "\n").
	 *
	 * @return comments by key
	 */
	public Map<String, String> getComments() {
		return comments;
	}

	/**
	 * Keys that occurred more than once in the data read by read().
	 * Which value was kept depends on isFirstDuplicateValueWins().
	 *
	 * @return duplicate keys in order of their first duplicate occurrence
	 */
	public Set<String> getDuplicateKeys() {
		return duplicateKeys;
	}

	/**
	 * Number of empty lines before the keys read by read() (including those before a key's comment).
	 * Keys without preceding empty lines are not contained.
	 *
	 * @return number of empty lines by key
	 */
	public Map<String, Integer> getEmptyLinesBefore() {
		return emptyLinesBefore;
	}

	/**
	 * Reads properties from a string (with case insensitive keys).
	 *
	 * @param propertiesString properties data
	 * @return keys and unescaped values in the order of their first occurrence
	 * @throws Exception if the data contains invalid escapes
	 */
	public static Map<String, String> readPropertiesString(final String propertiesString) throws Exception {
		if (propertiesString == null) {
			throw new Exception("Invalid empty propertiesString");
		}
		// Bytes and reader must use the same charset, otherwise non-ASCII chars become mojibake
		try (PropertiesReader reader = new PropertiesReader(new ByteArrayInputStream(propertiesString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)) {
			return reader.read();
		}
	}
}
