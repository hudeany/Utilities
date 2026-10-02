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

	public PropertiesWriter(final OutputStream outputStream) {
		this.outputStream = outputStream;
		outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.ISO_8859_1));
	}

	public LineBreak getLinebreakType() {
		return linebreakType;
	}

	public void setLinebreakType(final LineBreak linebreakType) {
		if (linebreakType == null) {
			throw new IllegalArgumentException("Linebreak type must not be null");
		}
		this.linebreakType = linebreakType;
	}

	public PropertiesWriter withLinebreakType(final LineBreak linebreakTypeParam) {
		setLinebreakType(linebreakTypeParam);
		return this;
	}

	public String getKeyValueSeparator() {
		return keyValueSeparator;
	}

	/**
	 * Set the separator between key and value.
	 * Allowed are '=' or ':' optionally surrounded by blanks (e.g. " = "), or blanks only.
	 */
	public void setKeyValueSeparator(final String keyValueSeparator) {
		if (keyValueSeparator == null || !VALID_KEY_VALUE_SEPARATOR_PATTERN.matcher(keyValueSeparator).matches()) {
			throw new IllegalArgumentException("Invalid key value separator: '" + keyValueSeparator + "'");
		}
		this.keyValueSeparator = keyValueSeparator;
	}

	public PropertiesWriter withKeyValueSeparator(final String keyValueSeparatorParam) {
		setKeyValueSeparator(keyValueSeparatorParam);
		return this;
	}

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

	public void writeProperties(final Map<String, String> properties) throws Exception {
		writeProperties(properties, null);
	}

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

	public void writeComment(final String comment) throws Exception {
		checkNotClosed();

		for (final String commentLine : comment.replace("\r\n", "\n").replace("\r", "\n").split("\n")) {
			outputWriter.write("# " + escapeCommentLine(commentLine) + linebreakType.toString());
		}
	}

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