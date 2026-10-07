package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reader for simple key value lists like {@code key1=value1, key2="value 2"; key3='value3'}.
 * <p>
 * Entries are separated by comma, semicolon, tab or line breaks, and optionally by blanks (see
 * {@link #setUseBlankAsSeparator(boolean)}). Keys and values are separated by "=". Keys and values
 * may be quoted by single or double quotes, a backslash escapes the following character. Unquoted
 * keys and values are trimmed. A key without "=" gets an empty value.
 * </p>
 */
public class MapStringReader extends BasicReader {
	private boolean useBlankAsSeparator = false;

	/**
	 * Creates a new reader using UTF-8 encoding.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @throws Exception
	 *             if the input stream is null
	 */
	public MapStringReader(final InputStream inputStream) throws Exception {
		super(inputStream, null);
	}

	/**
	 * Creates a new reader using the given encoding.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @param encodingCharset
	 *            the encoding of the data, or null for UTF-8
	 * @throws Exception
	 *             if the input stream is null
	 */
	public MapStringReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		super(inputStream, encodingCharset);
	}

	/**
	 * Returns whether blanks separate entries.
	 *
	 * @return true, if blanks separate entries
	 */
	public boolean isUseBlankAsSeparator() {
		return useBlankAsSeparator;
	}

	/**
	 * Sets whether blanks separate entries. If set, unquoted keys and values cannot contain blanks.
	 *
	 * @param useBlankAsSeparator
	 *            true to separate entries by blanks
	 */
	public void setUseBlankAsSeparator(final boolean useBlankAsSeparator) {
		this.useBlankAsSeparator = useBlankAsSeparator;
	}

	/**
	 * Reads all entries of the data.
	 *
	 * @return the entries in reading order, later entries replace earlier ones with the same key
	 * @throws Exception
	 *             if the data is invalid (e.g. it ends within an escape sequence or a quoted text) or
	 *             reading fails
	 */
	public Map<String, String> readMap() throws Exception {
		final Map<String, String> returnMap = new LinkedHashMap<>();

		boolean inValue = false;

		StringBuilder nextKey = new StringBuilder();
		boolean keyWasQuoted = false;
		StringBuilder nextValue = new StringBuilder();
		boolean valueWasQuoted = false;
		boolean escapeNextChar = false;

		Character currentChar = readNextNonWhitespace();
		while (currentChar != null) {
			if (escapeNextChar) {
				escapeNextChar = false;
				if (inValue) {
					nextValue.append(currentChar);
				} else {
					nextKey.append(currentChar);
				}
				currentChar = readNextCharacter();
			} else {
				switch (currentChar) {
					case '\\':
						escapeNextChar = true;
						currentChar = readNextCharacter();
						break;
					case ' ':
						if (useBlankAsSeparator) {
							if (nextKey.length() > 0 || nextValue.length() > 0) {
								returnMap.put(nextKey.toString(), nextValue.toString());
								inValue = false;
								nextKey = new StringBuilder();
								keyWasQuoted = false;
								nextValue = new StringBuilder();
								valueWasQuoted = false;
							}
							currentChar = readNextNonWhitespace();
							break;
						} else {
							// Item content, maybe quoted
							if (inValue) {
								if (!valueWasQuoted) {
									nextValue.append(currentChar);
								}
							} else {
								if (!keyWasQuoted) {
									nextKey.append(currentChar);
								}
							}
							currentChar = readNextCharacter();
							break;
						}
					case ',':
					case ';':
					case '\n':
					case '\r':
					case '\t':
						if (nextKey.length() > 0 || nextValue.length() > 0) {
							returnMap.put(keyWasQuoted ? nextKey.toString() : nextKey.toString().trim(),
									valueWasQuoted ? nextValue.toString() : nextValue.toString().trim());
							inValue = false;
							nextKey = new StringBuilder();
							keyWasQuoted = false;
							nextValue = new StringBuilder();
							valueWasQuoted = false;
						}
						currentChar = readNextNonWhitespace();
						break;
					case '\'':
					case '"':
						// Start quoted value
						final String quotedText = readQuotedText('\\');
						if (inValue) {
							nextValue = new StringBuilder(quotedText);
							valueWasQuoted = true;
						} else {
							nextKey = new StringBuilder(quotedText);
							keyWasQuoted = true;
						}
						currentChar = readNextCharacter();
						break;
					case '=':
						// Key value separator
						if (!inValue) {
							inValue = true;
						} else {
							nextValue.append(currentChar);
						}
						currentChar = readNextNonWhitespace();
						break;
					default:
						// Item content, maybe quoted
						if (inValue) {
							nextValue.append(currentChar);
						} else {
							nextKey.append(currentChar);
						}
						currentChar = readNextCharacter();
						break;
				}
			}
		}

		if (escapeNextChar) {
			throw new Exception("Unexpected start of escape sequence (\\)");
		}

		if (inValue || nextKey.length() > 0) {
			returnMap.put(keyWasQuoted ? nextKey.toString() : nextKey.toString().trim(),
					valueWasQuoted ? nextValue.toString() : nextValue.toString().trim());
			inValue = false;
			nextKey = new StringBuilder();
			keyWasQuoted = false;
			nextValue = new StringBuilder();
			valueWasQuoted = false;
		}

		return returnMap;
	}

	/**
	 * Reads all entries of a text.
	 *
	 * @param mapString
	 *            the text to parse
	 * @return the entries in reading order
	 * @throws Exception
	 *             if the text is invalid
	 */
	public static Map<String, String> readMap(final String mapString) throws Exception {
		try (MapStringReader mapStringReader = new MapStringReader(new ByteArrayInputStream(mapString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)) {
			return mapStringReader.readMap();
		}
	}

	/**
	 * Reads all entries of a text, which uses blanks as additional entry separators.
	 *
	 * @param mapString
	 *            the text to parse
	 * @return the entries in reading order
	 * @throws Exception
	 *             if the text is invalid
	 */
	public static Map<String, String> readMapWithBlankAsSeparator(final String mapString) throws Exception {
		try (MapStringReader mapStringReader = new MapStringReader(new ByteArrayInputStream(mapString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)) {
			mapStringReader.setUseBlankAsSeparator(true);
			return mapStringReader.readMap();
		}
	}
}
