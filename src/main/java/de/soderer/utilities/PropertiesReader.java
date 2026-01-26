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

import de.soderer.utilities.collection.CaseInsensitiveOrderedMap;

public class PropertiesReader implements Closeable {
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
		Map<String, String> properties;
		if (readKeysCaseInsensitive) {
			properties = new CaseInsensitiveOrderedMap<>();
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
				pendingCommentLines += trimmedNextLine.substring(1).trim();
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

				while (endsWithUnescapedBackslash(value) && (nextLine = inputReader.readLine()) != null) {
					value = value.substring(0, value.length() - 1) + (nextLine.trim());
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

	private static String unescapeKey(final String rawKey) throws Exception {
		String key;

		try {
			key = Utilities.unescapeJavaString(rawKey.trim());
		} catch (final Exception e) {
			throw new Exception("Invalid JAVA endoded text: " + rawKey, e);
		}

		return key;
	}

	private static String unescapeValue(final String rawValue) throws Exception {
		String value;

		try {
			value = Utilities.unescapeJavaString(Utilities.trimLeft(rawValue));
		} catch (final Exception e) {
			throw new Exception("Invalid JAVA endoded text: " + rawValue, e);
		}
		return value;
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
	 * Close this writer and its underlying stream.
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
		try (PropertiesReader reader = new PropertiesReader(new ByteArrayInputStream(propertiesString.getBytes(StandardCharsets.UTF_8)))) {
			return reader.read();
		}
	}
}
