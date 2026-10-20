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
 * {@link #setUseBlankAsSeparator(boolean)}). Keys and values are separated by "=", blanks around
 * the "=" are allowed. Keys and values may be quoted by single or double quotes. Within quotes,
 * a backslash starts an escape sequence like "\n" or "\"". Outside of quotes a backslash escapes
 * the following character literally. A quote character within an unquoted key or value (like in
 * {@code it's}) is a normal character. Unquoted keys and values are trimmed. A key without "="
 * gets an empty value.
 * </p>
 */
public class MapStringReader extends BasicReader {
	private boolean useBlankAsSeparator = false;

	/** State of the entry currently read */
	private boolean inValue;
	private StringBuilder nextKey;
	private boolean keyWasQuoted;
	private StringBuilder nextValue;
	private boolean valueWasQuoted;

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
	 * Blanks around the "=" are still allowed.
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
	 *             if the data is invalid (e.g. it ends within an escape sequence or a quoted text,
	 *             or a quoted key or value is followed by other text) or reading fails
	 */
	public Map<String, String> readMap() throws Exception {
		final Map<String, String> returnMap = new LinkedHashMap<>();
		resetEntry();

		Character currentChar = readNextNonWhitespace();
		while (currentChar != null) {
			switch (currentChar) {
				case '\\':
					// Escaped character is taken literally
					final Character escapedChar = readNextCharacter();
					if (escapedChar == null) {
						throw new Exception("Data ends within escape sequence (\\)" + getCurrentPositionText());
					}
					appendContent(escapedChar);
					currentChar = readNextCharacter();
					break;
				case ' ':
					int blankCount = 1;
					currentChar = readNextCharacter();
					while (currentChar != null && currentChar == ' ') {
						blankCount++;
						currentChar = readNextCharacter();
					}
					if (useBlankAsSeparator) {
						// Blanks before "=" do not separate the key from its value
						if (!(currentChar != null && currentChar == '=' && !inValue)) {
							storeEntry(returnMap);
						}
					} else if (currentChar != null && !isSeparator(currentChar) && !(currentChar == '=' && !inValue)) {
						// Inner blanks belong to an unquoted key or value, blanks after quoted text are ignored
						if (!isCurrentPartQuoted() && getCurrentPart().length() > 0) {
							getCurrentPart().append(" ".repeat(blankCount));
						}
					}
					break;
				case ',':
				case ';':
				case '\n':
				case '\r':
				case '\t':
					storeEntry(returnMap);
					currentChar = readNextNonWhitespace();
					break;
				case '\'':
				case '"':
					if (getCurrentPart().length() > 0 || isCurrentPartQuoted()) {
						// Quote character within an unquoted text (like "it's") is a normal character
						appendContent(currentChar);
					} else {
						final String quotedText = readQuotedText(currentChar, '\\');
						getCurrentPart().append(quotedText);
						if (inValue) {
							valueWasQuoted = true;
						} else {
							keyWasQuoted = true;
						}
					}
					currentChar = readNextCharacter();
					break;
				case '=':
					if (!inValue) {
						// Key value separator
						inValue = true;
						// Only blanks are skipped, so an empty value followed by a line break or tab ends the entry
						currentChar = skipBlanks();
					} else {
						appendContent(currentChar);
						currentChar = readNextCharacter();
					}
					break;
				default:
					appendContent(currentChar);
					currentChar = readNextCharacter();
					break;
			}
		}

		storeEntry(returnMap);

		return returnMap;
	}

	private static boolean isSeparator(final char character) {
		return character == ',' || character == ';' || character == '\n' || character == '\r' || character == '\t';
	}

	/**
	 * Skips blanks, starting with the character after the current one
	 *
	 * @return the first character, which is no blank, or null at the end of data
	 */
	private Character skipBlanks() throws Exception {
		Character nextChar = readNextCharacter();
		while (nextChar != null && nextChar == ' ') {
			nextChar = readNextCharacter();
		}
		return nextChar;
	}

	private StringBuilder getCurrentPart() {
		return inValue ? nextValue : nextKey;
	}

	private boolean isCurrentPartQuoted() {
		return inValue ? valueWasQuoted : keyWasQuoted;
	}

	/**
	 * Appends a character to the unquoted key or value currently read
	 *
	 * @throws Exception
	 *             if the key or value was quoted, because quoted text must not be followed by other
	 *             text
	 */
	private void appendContent(final char character) throws Exception {
		if (isCurrentPartQuoted()) {
			throw new Exception("Unexpected character '" + character + "' after quoted " + (inValue ? "value" : "key") + getCurrentPositionText());
		}
		getCurrentPart().append(character);
	}

	/**
	 * Stores the entry currently read, if there is one, and starts a new entry
	 */
	private void storeEntry(final Map<String, String> map) {
		if (inValue || keyWasQuoted || nextKey.length() > 0) {
			map.put(keyWasQuoted ? nextKey.toString() : nextKey.toString().trim(),
					valueWasQuoted ? nextValue.toString() : nextValue.toString().trim());
		}
		resetEntry();
	}

	private void resetEntry() {
		inValue = false;
		nextKey = new StringBuilder();
		keyWasQuoted = false;
		nextValue = new StringBuilder();
		valueWasQuoted = false;
	}

	/**
	 * Reads all entries of a text.
	 *
	 * @param mapString
	 *            the text to parse, not null
	 * @return the entries in reading order
	 * @throws Exception
	 *             if the text is invalid
	 */
	public static Map<String, String> readMap(final String mapString) throws Exception {
		if (mapString == null) {
			throw new IllegalArgumentException("Map string must not be null");
		}
		try (MapStringReader mapStringReader = new MapStringReader(new ByteArrayInputStream(mapString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)) {
			return mapStringReader.readMap();
		}
	}

	/**
	 * Reads all entries of a text, which uses blanks as additional entry separators.
	 *
	 * @param mapString
	 *            the text to parse, not null
	 * @return the entries in reading order
	 * @throws Exception
	 *             if the text is invalid
	 */
	public static Map<String, String> readMapWithBlankAsSeparator(final String mapString) throws Exception {
		if (mapString == null) {
			throw new IllegalArgumentException("Map string must not be null");
		}
		try (MapStringReader mapStringReader = new MapStringReader(new ByteArrayInputStream(mapString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)) {
			mapStringReader.setUseBlankAsSeparator(true);
			return mapStringReader.readMap();
		}
	}
}
