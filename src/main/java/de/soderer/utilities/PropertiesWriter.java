package de.soderer.utilities;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import de.soderer.utilities.TextUtilities.LineBreak;

/**
 * Writer for .properties files in ISO-8859-1 encoding (characters outside printable ASCII are written as unicode
 * escapes), which is readable by java.util.Properties and {@link PropertiesReader}.
 * Supports comments, empty lines, a configurable key value separator and line break type.
 */
public class PropertiesWriter implements Closeable {
	/**
	 * Allowed separators: '=' or ':' optionally surrounded by blanks, or blanks only (no line breaks)
	 */
	private static final Pattern VALID_KEY_VALUE_SEPARATOR_PATTERN = Pattern.compile("[ \\t\\f]*[=:][ \\t\\f]*|[ \\t\\f]+");

	private LineBreak linebreakType = LineBreak.Unix;
	private String keyValueSeparator = "=";

	private OutputStream outputStream;
	private int writtenProperties = 0;
	private BufferedWriter outputWriter = null;

	/**
	 * Creates a writer.
	 *
	 * @param outputStream stream to write to, closed by {@link #close()}
	 */
	public PropertiesWriter(final OutputStream outputStream) {
		this.outputStream = outputStream;
		outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.ISO_8859_1));
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
	 * @param linebreakType line break type, must not be null
	 */
	public void setLinebreakType(final LineBreak linebreakType) {
		if (linebreakType == null) {
			throw new IllegalArgumentException("Linebreak type must not be null");
		}
		this.linebreakType = linebreakType;
	}

	/**
	 * Sets the line break written after each line.
	 *
	 * @param linebreakTypeParam line break type, must not be null
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
	 * @param key key, must not be empty
	 * @param value value, null is written as empty value
	 * @throws IllegalStateException if this writer is already closed
	 * @throws IllegalArgumentException if the key is empty
	 * @throws Exception if the data cannot be written
	 */
	public void writeProperty(final String key, final String value) throws Exception {
		checkNotClosed();

		if (key == null || key.isEmpty()) {
			throw new IllegalArgumentException("Property key must not be empty");
		}

		String escapedValue = escapeValue(value);
		if (keyValueSeparator.trim().isEmpty() && (escapedValue.startsWith("=") || escapedValue.startsWith(":"))) {
			// With a blanks-only separator, a leading '=' or ':' of the value would be read as the actual separator
			escapedValue = "\\" + escapedValue;
		}

		outputWriter.write(escapeKey(key));
		outputWriter.write(keyValueSeparator);
		outputWriter.write(escapedValue);
		outputWriter.write(linebreakType.toString());

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

		for (final Entry<String, String> propertyEntry : properties.entrySet()) {
			if (comments != null && comments.get(propertyEntry.getKey()) != null) {
				writeComment(comments.get(propertyEntry.getKey()));
			}
			writeProperty(propertyEntry.getKey(), propertyEntry.getValue());
		}
	}

	/**
	 * Escape a property key.
	 * Keys need escaping of separators ('=', ':'), comment chars ('#', '!') and all whitespace,
	 * because the first unescaped whitespace or separator ends the key.
	 *
	 * @param key key to escape
	 * @return escaped key
	 */
	public static String escapeKey(final String key) {
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
					appendEscapedCommonChar(result, c);
			}
		}
		return result.toString();
	}

	/**
	 * Escape a property value.
	 * Only characters that would change the meaning when loading are escaped.
	 * Double quotes, '=', ':', '#' and '!' have no special meaning inside a value and are written as they are.
	 * Line breaks are kept lossless ("\r\n" is written as "\r\n").
	 *
	 * @param value value to escape, may be null
	 * @return escaped value, empty for null
	 */
	public static String escapeValue(final String value) {
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
				appendEscapedCommonChar(result, c);
			}
		}
		return result.toString();
	}

	/**
	 * Escape characters that need escaping in keys and values alike:
	 * backslash, control characters and everything outside printable ASCII.
	 */
	private static void appendEscapedCommonChar(final StringBuilder result, final char c) {
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
				if (c < 0x20 || c > 0x7E) {
					appendUnicodeEscape(result, c);
				} else {
					result.append(c);
				}
		}
	}

	private static void appendUnicodeEscape(final StringBuilder result, final char c) {
		result.append("\\u").append(String.format("%04X", (int) c));
	}

	/**
	 * Escape a comment line.
	 * Only characters not representable in ISO-8859-1 are written as unicode escapes,
	 * so umlauts etc. stay readable. PropertiesReader decodes these escapes in comments.
	 */
	private static String escapeCommentLine(final String commentLine) {
		final StringBuilder result = new StringBuilder(commentLine.length() + 16);
		for (int i = 0; i < commentLine.length(); i++) {
			final char c = commentLine.charAt(i);
			if (c > 0xFF) {
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

		for (final String commentLine : comment.replace("\r\n", "\n").replace("\r", "\n").split("\n")) {
			outputWriter.write("# " + escapeCommentLine(commentLine) + linebreakType.toString());
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

		outputWriter.write(linebreakType.toString());
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
	 * Close this writer and its underlying stream.
	 */
	@Override
	public void close() {
		closeQuietly(outputWriter);
		outputWriter = null;
		closeQuietly(outputStream);
		outputStream = null;
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