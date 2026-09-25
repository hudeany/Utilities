package de.soderer.utilities;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.soderer.utilities.collection.CaseInsensitiveLinkedMap;

public class PropertiesReader implements Closeable {
	private static final Pattern UNICODE_ESCAPE_PATTERN = Pattern.compile("\\\\u([0-9A-Fa-f]{4})");

	private BufferedReader inputReader;

	private boolean readKeysCaseInsensitive = true;

	private String pendingCommentLines = "";
	private final Map<String, String> comments = new HashMap<>();

	public PropertiesReader(final InputStream inputStream) throws Exception {
		if (inputStream == null) {
			throw new Exception("Invalid empty inputStream");
		} else {
			inputReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.ISO_8859_1));
		}
	}

	public PropertiesReader(final InputStream inputStream, final Charset encoding) throws Exception {
		if (inputStream == null) {
			throw new Exception("Invalid empty inputStream");
		} else {
			inputReader = new BufferedReader(new InputStreamReader(inputStream, encoding));
		}
	}

	public boolean isReadKeysCaseInsensitive() {
		return readKeysCaseInsensitive;
	}

	public PropertiesReader setReadKeysCaseInsensitive(final boolean readKeysCaseInsensitive) {
		this.readKeysCaseInsensitive = readKeysCaseInsensitive;
		return this;
	}

	public Map<String, String> read() throws Exception {
		if (inputReader == null) {
			throw new IllegalStateException("PropertiesReader is already closed");
		}

		Map<String, String> properties;
		if (readKeysCaseInsensitive) {
			properties = new CaseInsensitiveLinkedMap<>();
		} else {
			properties = new LinkedHashMap<>();
		}

		String nextLine;
		while ((nextLine = inputReader.readLine()) != null) {
			final String trimmedNextLine = nextLine.trim();
			if (trimmedNextLine.startsWith("#")
					|| trimmedNextLine.startsWith("!")) {
				if (pendingCommentLines.length() > 0) {
					pendingCommentLines += "\n";
				}
				pendingCommentLines += unescapeComment(trimmedNextLine.substring(1).trim());
			} else if (trimmedNextLine.length() > 0) {
				final int separatorIndex = findKeyValueSeparator(nextLine);
				String key;
				String value;
				if (separatorIndex == -1) {
					key = nextLine.substring(0);
					value = "";
				} else {
					key = nextLine.substring(0, separatorIndex);
					value = nextLine.substring(separatorIndex + 1);
				}

				// Continuation lines only lose their leading whitespace, trailing whitespace is part of the value
				while (endsWithUnescapedBackslash(value) && (nextLine = inputReader.readLine()) != null) {
					value = value.substring(0, value.length() - 1) + Utilities.trimLeft(nextLine);
				}

				final String unescapedKey = unescapeKey(key);
				final String unescapedValue = unescapeValue(value);
				properties.put(unescapedKey, unescapedValue);

				if (pendingCommentLines.length() > 0) {
					comments.put(unescapedKey, pendingCommentLines);
					pendingCommentLines = "";
				}
			}
		}

		return properties;
	}

	private static int findKeyValueSeparator(final String nextLine) {
		boolean escapeNextChar = false;
		boolean keyStarted = false;
		boolean keyEnded = false;
		int charIndex = -1;
		for (final char nextChar : nextLine.toCharArray()) {
			charIndex++;

			if (!keyStarted && !Character.isWhitespace(nextChar)) {
				keyStarted = true;
			}

			if (escapeNextChar) {
				escapeNextChar = false;
			} else if (nextChar == '\\') {
				escapeNextChar = true;
			} else if (nextChar == '='
					|| nextChar == ':') {
				return charIndex;
			} else if (keyStarted && !keyEnded && Character.isWhitespace(nextChar)) {
				keyEnded = true;
			} else if (keyEnded && !Character.isWhitespace(nextChar)) {
				return charIndex - 1;
			}
		}

		return -1;
	}

	/**
	 * Unescape a key in its stored form (counterpart of PropertiesWriter.escapeKey()),
	 * including the whitespace trimming that is applied when reading a .properties file.
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
		final String key = Utilities.trimLeft(rawKey);
		int end = key.length();
		while (end > 0 && Character.isWhitespace(key.charAt(end - 1)) && !isEscapedAt(key, end - 1)) {
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
	 */
	public static String unescapeValue(final String rawValue) throws Exception {
		try {
			return Utilities.unescapePropertiesString(Utilities.trimLeft(rawValue));
		} catch (final Exception e) {
			throw new Exception("Invalid properties encoded value: " + rawValue, e);
		}
	}

	/**
	 * Decode unicode escapes in comment lines, which are written for chars not representable in ISO-8859-1.
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

	private static boolean endsWithUnescapedBackslash(final String value) {
		boolean escapeNextChar = false;
		for (final char nextChar : value.toCharArray()) {
			if (escapeNextChar) {
				escapeNextChar = false;
			} else if (nextChar == '\\') {
				escapeNextChar = true;
			}
		}
		return escapeNextChar;
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
	}

	public Map<String, String> getComments() {
		return comments;
	}

	public static Map<String, String> readPropertiesString(final String propertiesString) throws Exception {
		// Bytes and reader must use the same charset, otherwise non-ASCII chars become mojibake
		try (PropertiesReader reader = new PropertiesReader(new ByteArrayInputStream(propertiesString.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8)) {
			return reader.read();
		}
	}
}
