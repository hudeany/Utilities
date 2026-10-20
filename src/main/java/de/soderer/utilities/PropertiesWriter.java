package de.soderer.utilities;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.soderer.utilities.TextUtilities.LineBreak;

/**
 * Writer for .properties files, which are readable by java.util.Properties and {@link PropertiesReader}.
 * <p>
 * By default the files are written in ISO-8859-1 encoding, characters outside printable ASCII are written as unicode
 * escapes ("\\uXXXX"). Optionally the files are written in UTF-8 encoding without escaping non-ASCII characters, as
 * supported by java.util.PropertyResourceBundle since Java 9 and by java.util.Properties.load(Reader) with an UTF-8
 * reader.
 * </p>
 * Supports comments, empty lines, a configurable key value separator and line break type.
 */
public class PropertiesWriter implements Closeable {
	/**
	 * Allowed separators: '=' or ':' optionally surrounded by blanks, or blanks only (no line breaks)
	 */
	private static final Pattern VALID_KEY_VALUE_SEPARATOR_PATTERN = Pattern.compile("[ \\t\\f]*[=:][ \\t\\f]*|[ \\t\\f]+");

	/** Byte order mark, which would be removed by some readers at the beginning of a file */
	private static final char BYTE_ORDER_MARK = '\uFEFF';

	/** Text like a unicode escape ("\" followed by "u" and 4 hex digits), which PropertiesReader would decode in comments */
	private static final Pattern UNICODE_ESCAPE_TEXT_PATTERN = Pattern.compile("\\\\u[0-9A-Fa-f]{4}");

	private LineBreak linebreakType = LineBreak.Unix;
	private String keyValueSeparator = "=";

	private final boolean storeAsUtf8;

	private OutputStream outputStream;
	private int writtenProperties = 0;
	private BufferedWriter outputWriter = null;

	/**
	 * Creates a writer for ISO-8859-1 encoded files with unicode escapes for all non-ASCII characters.
	 *
	 * @param outputStream stream to write to, closed by {@link #close()}
	 */
	public PropertiesWriter(final OutputStream outputStream) {
		this(outputStream, false);
	}

	/**
	 * Creates a writer.
	 *
	 * @param outputStream stream to write to, closed by {@link #close()}
	 * @param storeAsUtf8 true to write an UTF-8 file (without BOM) with non-ASCII characters unescaped, false to write
	 *            an ISO-8859-1 file with unicode escapes for all non-ASCII characters
	 */
	public PropertiesWriter(final OutputStream outputStream, final boolean storeAsUtf8) {
		if (outputStream == null) {
			throw new IllegalArgumentException("OutputStream must not be null");
		}
		this.outputStream = outputStream;
		this.storeAsUtf8 = storeAsUtf8;
		outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, storeAsUtf8 ? StandardCharsets.UTF_8 : StandardCharsets.ISO_8859_1));
	}

	/**
	 * Whether the file is written in UTF-8 encoding without escaping non-ASCII characters.
	 *
	 * @return true for UTF-8, false for ISO-8859-1 with unicode escapes
	 */
	public boolean isStoreAsUtf8() {
		return storeAsUtf8;
	}

	/**
	 * Line break written after each line.
	 *
	 * @return line break type (default Unix)
	 */
	public LineBreak getLinebreakType() {
		return linebreakType;
	}

	/**
	 * Sets the line break written after each line.
	 *
	 * @param linebreakType line break type: Unix, Windows or Mac
	 * @throws IllegalArgumentException for null or a type that is no single line break (like Unknown or Mixed)
	 */
	public void setLinebreakType(final LineBreak linebreakType) {
		if (linebreakType == null) {
			throw new IllegalArgumentException("Linebreak type must not be null");
		}
		// Throws for types without a defined line break
		getLinebreakString(linebreakType);
		this.linebreakType = linebreakType;
	}

	/**
	 * Line break chars of a line break type. Defined here, so the written chars do not depend on LineBreak.toString().
	 */
	private static String getLinebreakString(final LineBreak linebreakType) {
		switch (linebreakType) {
			case Unix:
				return "\n";
			case Windows:
				return "\r\n";
			case Mac:
				return "\r";
			case Mixed:
			case Unknown:
			default:
				throw new IllegalArgumentException("Invalid linebreak type for writing: " + linebreakType);
		}
	}

	/**
	 * Sets the line break written after each line.
	 *
	 * @param linebreakTypeParam line break type: Unix, Windows or Mac
	 * @return this instance for chaining
	 */
	public PropertiesWriter withLinebreakType(final LineBreak linebreakTypeParam) {
		setLinebreakType(linebreakTypeParam);
		return this;
	}

	/**
	 * Separator written between key and value.
	 *
	 * @return separator (default "=")
	 */
	public String getKeyValueSeparator() {
		return keyValueSeparator;
	}

	/**
	 * Set the separator between key and value.
	 * Allowed are '=' or ':' optionally surrounded by blanks (e.g. " = "), or blanks only.
	 *
	 * @param keyValueSeparator separator
	 * @throws IllegalArgumentException if the separator is null or not allowed
	 */
	public void setKeyValueSeparator(final String keyValueSeparator) {
		if (keyValueSeparator == null || !VALID_KEY_VALUE_SEPARATOR_PATTERN.matcher(keyValueSeparator).matches()) {
			throw new IllegalArgumentException("Invalid key value separator: '" + keyValueSeparator + "'");
		}
		this.keyValueSeparator = keyValueSeparator;
	}

	/**
	 * Set the separator between key and value.
	 * Allowed are '=' or ':' optionally surrounded by blanks (e.g. " = "), or blanks only.
	 *
	 * @param keyValueSeparatorParam separator
	 * @return this instance for chaining
	 * @throws IllegalArgumentException if the separator is null or not allowed
	 */
	public PropertiesWriter withKeyValueSeparator(final String keyValueSeparatorParam) {
		setKeyValueSeparator(keyValueSeparatorParam);
		return this;
	}

	/**
	 * Writes a property line with escaped key and value.
	 *
	 * @param key key, must not be null. An empty key (valid in .properties files) needs a separator containing '=' or
	 *            ':', because with a blanks-only separator the value would be read as key.
	 * @param value value, null is written as empty value
	 * @throws IllegalStateException if this writer is already closed
	 * @throws IllegalArgumentException if the key is null, or empty with a blanks-only separator
	 * @throws Exception if the data cannot be written
	 */
	public void writeProperty(final String key, final String value) throws Exception {
		checkNotClosed();

		if (key == null) {
			throw new IllegalArgumentException("Property key must not be null");
		} else if (key.isEmpty() && keyValueSeparator.trim().isEmpty()) {
			throw new IllegalArgumentException("Empty property key needs a separator containing '=' or ':'");
		}

		String escapedValue = escapeValue(value, !storeAsUtf8);
		if (keyValueSeparator.trim().isEmpty() && (escapedValue.startsWith("=") || escapedValue.startsWith(":"))) {
			// With a blanks-only separator, a leading '=' or ':' of the value would be read as the actual separator
			escapedValue = "\\" + escapedValue;
		}

		outputWriter.write(escapeKey(key, !storeAsUtf8));
		outputWriter.write(keyValueSeparator);
		outputWriter.write(escapedValue);
		outputWriter.write(getLinebreakString(linebreakType));

		writtenProperties++;
	}

	/**
	 * Writes all properties in the iteration order of the map.
	 *
	 * @param properties keys and values
	 * @throws Exception if the data cannot be written
	 */
	public void writeProperties(final Map<String, String> properties) throws Exception {
		writeProperties(properties, null);
	}

	/**
	 * Writes all properties in the iteration order of the map, each preceded by its comment.
	 *
	 * @param properties keys and values
	 * @param comments comments by key (multi line comments are separated by line breaks), may be null
	 * @throws Exception if the data cannot be written
	 */
	public void writeProperties(final Map<String, String> properties, final Map<String, String> comments) throws Exception {
		checkNotClosed();
		if (properties == null) {
			throw new IllegalArgumentException("Properties must not be null");
		}

		for (final Entry<String, String> propertyEntry : properties.entrySet()) {
			if (comments != null && comments.get(propertyEntry.getKey()) != null) {
				writeComment(comments.get(propertyEntry.getKey()));
			}
			writeProperty(propertyEntry.getKey(), propertyEntry.getValue());
		}
	}

	/**
	 * Escape a property key for ISO-8859-1 files (all non-ASCII characters as unicode escapes).
	 *
	 * @param key key to escape
	 * @return escaped key
	 * @see #escapeKey(String, boolean)
	 */
	public static String escapeKey(final String key) {
		return escapeKey(key, true);
	}

	/**
	 * Escape a property key.
	 * Keys need escaping of separators ('=', ':'), comment chars ('#', '!') and all whitespace,
	 * because the first unescaped whitespace or separator ends the key.
	 *
	 * @param key key to escape
	 * @param escapeNonAscii true to write all non-ASCII characters as unicode escapes (ISO-8859-1 files), false to
	 *            keep them unescaped (UTF-8 files)
	 * @return escaped key
	 */
	public static String escapeKey(final String key, final boolean escapeNonAscii) {
		final StringBuilder result = new StringBuilder(key.length() + 16);
		for (int i = 0; i < key.length(); i++) {
			final char c = key.charAt(i);
			switch (c) {
				case ' ':
				case '=':
				case ':':
				case '#':
				case '!':
					result.append('\\').append(c);
					break;
				default:
					appendEscapedCommonChar(result, key, i, escapeNonAscii);
			}
		}
		return result.toString();
	}

	/**
	 * Escape a property value for ISO-8859-1 files (all non-ASCII characters as unicode escapes).
	 *
	 * @param value value to escape, may be null
	 * @return escaped value, empty for null
	 * @see #escapeValue(String, boolean)
	 */
	public static String escapeValue(final String value) {
		return escapeValue(value, true);
	}

	/**
	 * Escape a property value.
	 * Only characters that would change the meaning when loading are escaped.
	 * Double quotes, '=', ':', '#' and '!' have no special meaning inside a value and are written as they are.
	 * Line breaks are kept lossless ("\r\n" is written as "\r\n").
	 *
	 * @param value value to escape, may be null
	 * @param escapeNonAscii true to write all non-ASCII characters as unicode escapes (ISO-8859-1 files), false to
	 *            keep them unescaped (UTF-8 files)
	 * @return escaped value, empty for null
	 */
	public static String escapeValue(final String value, final boolean escapeNonAscii) {
		if (value == null) {
			return "";
		}

		final StringBuilder result = new StringBuilder(value.length() + 16);
		for (int i = 0; i < value.length(); i++) {
			final char c = value.charAt(i);
			if (c == ' ' && i == 0) {
				// Leading whitespace would be stripped when loading
				result.append("\\ ");
			} else {
				appendEscapedCommonChar(result, value, i, escapeNonAscii);
			}
		}
		return result.toString();
	}

	/**
	 * Escape characters that need escaping in keys and values alike:
	 * backslash and control characters, and depending on escapeNonAscii everything outside printable ASCII.
	 * Without escapeNonAscii only the byte order mark and unpaired surrogates are escaped, because they cannot be
	 * stored lossless as UTF-8 text.
	 */
	private static void appendEscapedCommonChar(final StringBuilder result, final String text, final int index, final boolean escapeNonAscii) {
		final char c = text.charAt(index);
		switch (c) {
			case '\\':
				result.append("\\\\");
				break;
			case '\n':
				result.append("\\n");
				break;
			case '\r':
				result.append("\\r");
				break;
			case '\t':
				result.append("\\t");
				break;
			case '\f':
				result.append("\\f");
				break;
			default:
				if (c < 0x20 || c == 0x7F) {
					appendUnicodeEscape(result, c);
				} else if (c > 0x7E && (escapeNonAscii || c == BYTE_ORDER_MARK || isUnpairedSurrogate(text, index))) {
					appendUnicodeEscape(result, c);
				} else {
					result.append(c);
				}
		}
	}

	/**
	 * Checks whether the char at the index is a surrogate without its matching partner, which cannot be encoded in
	 * UTF-8 and would be replaced by '?'.
	 */
	private static boolean isUnpairedSurrogate(final String text, final int index) {
		final char c = text.charAt(index);
		if (Character.isHighSurrogate(c)) {
			return index + 1 >= text.length() || !Character.isLowSurrogate(text.charAt(index + 1));
		} else if (Character.isLowSurrogate(c)) {
			return index == 0 || !Character.isHighSurrogate(text.charAt(index - 1));
		} else {
			return false;
		}
	}

	private static void appendUnicodeEscape(final StringBuilder result, final char c) {
		result.append("\\u").append(String.format("%04X", (int) c));
	}

	/**
	 * Escape a comment line.
	 * For ISO-8859-1 files only characters not representable in ISO-8859-1 are written as unicode escapes,
	 * so umlauts etc. stay readable. For UTF-8 files only the byte order mark and unpaired surrogates are escaped.
	 * PropertiesReader decodes these escapes in comments. A backslash starting a text like "A" is escaped
	 * itself, so that text is not decoded when reading.
	 */
	private static String escapeCommentLine(final String commentLine, final boolean utf8) {
		final StringBuilder result = new StringBuilder(commentLine.length() + 16);
		final Matcher unicodeEscapeTextMatcher = UNICODE_ESCAPE_TEXT_PATTERN.matcher(commentLine);
		for (int i = 0; i < commentLine.length(); i++) {
			final char c = commentLine.charAt(i);
			if (c == '\\' && unicodeEscapeTextMatcher.region(i, commentLine.length()).lookingAt()) {
				appendUnicodeEscape(result, c);
			} else if (utf8) {
				if (c == BYTE_ORDER_MARK || isUnpairedSurrogate(commentLine, i)) {
					appendUnicodeEscape(result, c);
				} else {
					result.append(c);
				}
			} else if (c > 0xFF) {
				appendUnicodeEscape(result, c);
			} else {
				result.append(c);
			}
		}
		return result.toString();
	}

	/**
	 * Writes a comment, each of its lines prefixed by "# ".
	 *
	 * @param comment comment text, may contain line breaks
	 * @throws IllegalStateException if this writer is already closed
	 * @throws Exception if the data cannot be written
	 */
	public void writeComment(final String comment) throws Exception {
		checkNotClosed();
		if (comment == null) {
			throw new IllegalArgumentException("Comment must not be null");
		}

		// Limit -1 keeps trailing empty lines of the comment, which split() would drop otherwise
		for (final String commentLine : comment.replace("\r\n", "\n").replace("\r", "\n").split("\n", -1)) {
			outputWriter.write("# " + escapeCommentLine(commentLine, storeAsUtf8) + getLinebreakString(linebreakType));
		}
	}

	/**
	 * Writes an empty line.
	 *
	 * @throws IllegalStateException if this writer is already closed
	 * @throws Exception if the data cannot be written
	 */
	public void writeEmptyLine() throws Exception {
		checkNotClosed();

		outputWriter.write(getLinebreakString(linebreakType));
	}

	private void checkNotClosed() {
		if (outputStream == null) {
			throw new IllegalStateException("PropertiesWriter is already closed");
		}
	}

	/**
	 * Get number of properties written until now.
	 *
	 * @return the written properties
	 */
	public int getWrittenProperties() {
		return writtenProperties;
	}

	/**
	 * Close this writer and its underlying stream. Closing an already closed writer has no effect.
	 *
	 * @throws UncheckedIOException if the buffered data cannot be written (e.g. disk full). The stream is closed
	 *             anyway.
	 */
	@Override
	public void close() {
		IOException flushException = null;
		if (outputWriter != null) {
			try {
				// Flushed separately, so a write error of the buffered data is not swallowed like other close errors
				outputWriter.flush();
			} catch (final IOException e) {
				flushException = e;
			}
		}
		closeQuietly(outputWriter);
		outputWriter = null;
		closeQuietly(outputStream);
		outputStream = null;
		if (flushException != null) {
			throw new UncheckedIOException("PropertiesWriter could not write all data", flushException);
		}
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
}
