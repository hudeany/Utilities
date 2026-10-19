package de.soderer.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import de.soderer.utilities.collection.UniqueFifoQueuedList;

/**
 * Application configuration stored as properties file "~/.&lt;applicationName&gt;.config" (or
 * "~/.&lt;applicationName&gt;/.&lt;applicationName&gt;.config" when using a sub directory).
 * Supports typed values, lists (stored as csv line) and recent lists with a maximum size.
 * Changes are only persisted by {@link #save()} or {@link #saveOrThrow()}.
 * Application specific settings like the proxy configuration are added by subclasses (e.g. in ApplicationBase).
 * <p>
 * The file is written atomically and on POSIX file systems readable by the owner only, because it may contain
 * credentials. Like all properties files it is stored in ISO-8859-1 with unicode escapes, so it should not be edited
 * with an editor saving UTF-8.
 * </p>
 * <p>
 * All public methods are synchronized, so an instance may be shared between threads.
 * </p>
 */
public class ConfigurationProperties {
	/** Key of the application version */
	public static final String CONFIG_KEY_VERSION = "Application.Version";
	/** Key of the application language (language code like "en") */
	public static final String CONFIG_KEY_LANGUAGE = "Application.Language";

	/**
	 * Internal key: version of the configuration file format. Files without this key were written by older versions,
	 * whose list values may still be stored in the legacy format with backslash escaping.
	 */
	public static final String CONFIG_KEY_FORMAT_VERSION = "Application.ConfigFormat";

	/**
	 * Internal key: names of all list keys, which are stored in the current list format
	 * (plain RFC 4180 csv without backslash escaping). It is still maintained, so older application versions sharing
	 * this file keep reading these lists correctly.
	 */
	public static final String CONFIG_KEY_PLAIN_LIST_KEYS = "Application.PlainListKeys";

	/**
	 * Internal key: names of the keys whose list values may still be stored in the legacy format with backslash
	 * escaping. They are migrated on first read. Keys not contained here are never interpreted as legacy values, so
	 * backslashes in current list values (like Windows paths) are never mistaken for escape sequences.
	 */
	public static final String CONFIG_KEY_LEGACY_LIST_KEYS = "Application.LegacyListKeys";

	/** Current version of the configuration file format */
	private static final int CURRENT_FORMAT_VERSION = 2;

	/** Key extension for the maximum size of a recent list ("&lt;name&gt;.maxSize") */
	public static final String MAXSIZE_EXTENSION = ".maxSize";

	/** Maximum size of a recent list, if no valid "&lt;name&gt;.maxSize" is configured */
	public static final int DEFAULT_RECENT_LIST_MAX_SIZE = 10;
	/** Key extension for the comma separated allowed values of a key ("&lt;name&gt;.enum"), used by configuration dialogs */
	public static final String ENUM_EXTENSION = ".enum";

	private final Properties properties = new Properties();
	private final String applicationName;
	private final boolean useSubDirectory;

	/** List keys already stored in the current (plain) list format */
	private final Set<String> plainListKeys = new TreeSet<>();

	/** Keys whose list values may still be stored in the legacy format */
	private final Set<String> legacyListKeys = new TreeSet<>();

	/** Keys holding a {@link YesNoAsk} value, registered at runtime, used by configuration dialogs to offer a selection */
	private final Set<String> yesNoAskKeys = new TreeSet<>();

	/** Separator of list values */
	private static final char LIST_SEPARATOR = ';';

	/** String quote of list values, doubled within quoted values */
	private static final char LIST_STRING_QUOTE = '"';

	/**
	 * Internal keys are managed by this class and are not meant to be edited by the user
	 * (e.g. hidden in configuration dialogs)
	 *
	 * @param name configuration key
	 * @return true for internal keys
	 */
	public static boolean isInternalKey(final String name) {
		return CONFIG_KEY_PLAIN_LIST_KEYS.equals(name) || CONFIG_KEY_LEGACY_LIST_KEYS.equals(name) || CONFIG_KEY_FORMAT_VERSION.equals(name);
	}

	/**
	 * Loads the configuration of an application from "~/.&lt;applicationName&gt;.config", if it exists.
	 *
	 * @param applicationName name of the application
	 * @throws Exception if the existing configuration file cannot be read
	 */
	public ConfigurationProperties(final String applicationName) throws Exception {
		this(applicationName, false);
	}

	/**
	 * Loads the configuration of an application, if it exists.
	 *
	 * @param applicationName name of the application
	 * @param useSubDirectory true to store the file in the sub directory "~/.&lt;applicationName&gt;"
	 * @throws Exception if the existing configuration file cannot be read
	 */
	public ConfigurationProperties(final String applicationName, final boolean useSubDirectory) throws Exception {
		this.applicationName = applicationName;
		this.useSubDirectory = useSubDirectory;
		final File configFile = getConfigFile();
		if (configFile.exists()) {
			try (FileInputStream fileInputStream = new FileInputStream(configFile)) {
				final Properties propertiesLoaded = new Properties();
				propertiesLoaded.load(fileInputStream);
				for (final Entry<Object, Object> entry : propertiesLoaded.entrySet()) {
					properties.setProperty((String) entry.getKey(), (String) entry.getValue());
				}
			} catch (final IOException e) {
				throw new Exception("Cannot load configuration file '" + configFile.getAbsolutePath() + "'", e);
			}
		}

		final boolean plainListKeysReadable = readKeySet(CONFIG_KEY_PLAIN_LIST_KEYS, plainListKeys);
		if (properties.containsKey(CONFIG_KEY_FORMAT_VERSION)) {
			readKeySet(CONFIG_KEY_LEGACY_LIST_KEYS, legacyListKeys);
		} else {
			// File of an older version: determine the list keys which may still be stored in the legacy format
			if (!properties.containsKey(CONFIG_KEY_PLAIN_LIST_KEYS)) {
				// No list was migrated yet, so every key may hold a legacy list
				addNonInternalKeys(legacyListKeys, Collections.emptySet());
			} else if (plainListKeysReadable) {
				addNonInternalKeys(legacyListKeys, plainListKeys);
			} else {
				// Unknown which lists were migrated: reading a legacy list as plain only shows its escape sequences,
				// while reading a plain list as legacy would silently turn backslashes of paths into control characters
				System.err.println("Configuration key '" + CONFIG_KEY_PLAIN_LIST_KEYS + "' is invalid, list values are not migrated from the legacy format");
			}
			properties.setProperty(CONFIG_KEY_FORMAT_VERSION, Integer.toString(CURRENT_FORMAT_VERSION));
			storeLegacyListKeys();
		}
	}

	/**
	 * Reads a set of key names stored as list value.
	 *
	 * @param key configuration key of the set
	 * @param keySet set to fill
	 * @return true, if the value is missing or could be read
	 */
	private boolean readKeySet(final String key, final Set<String> keySet) {
		final String value = properties.getProperty(key);
		if (Utilities.isNotEmpty(value)) {
			try {
				keySet.addAll(parseListValue(value, false));
			} catch (final Exception e) {
				System.err.println("Cannot read configuration key '" + key + "': " + e.getMessage());
				return false;
			}
		}
		return true;
	}

	/**
	 * Adds all non internal configuration keys, which are not excluded and whose value contains a backslash, to a set.
	 * Values without backslash are read identically in the legacy and in the plain list format, so they need no
	 * migration.
	 *
	 * @param keySet set to fill
	 * @param excludedKeys keys not to add
	 */
	private void addNonInternalKeys(final Set<String> keySet, final Set<String> excludedKeys) {
		for (final Entry<Object, Object> entry : properties.entrySet()) {
			final String key = (String) entry.getKey();
			if (!isInternalKey(key) && !excludedKeys.contains(key) && ((String) entry.getValue()).indexOf('\\') >= 0) {
				keySet.add(key);
			}
		}
	}

	/**
	 * Directory containing the configuration file.
	 *
	 * @return configuration directory
	 */
	private File getConfigDirectory() {
		final File homeDirectory = new File(System.getProperty("user.home"));
		return useSubDirectory ? new File(homeDirectory, "." + applicationName) : homeDirectory;
	}

	/**
	 * Configuration file.
	 *
	 * @return configuration file
	 */
	private File getConfigFile() {
		return new File(getConfigDirectory(), "." + applicationName + ".config");
	}

	/**
	 * Value of a key.
	 *
	 * @param name configuration key
	 * @return value, or null if the key does not exist
	 */
	public synchronized String get(final String name) {
		return properties.getProperty(name);
	}

	/**
	 * Ensures this key is contained in the configuration (with an empty value, if it is missing), so it appears in the saved file.
	 *
	 * @param name configuration key
	 */
	public synchronized void ensure(final String name) {
		if (!properties.containsKey(name)) {
			properties.setProperty(name, "");
		}
	}

	/**
	 * Ensures this key is contained in the configuration (with the default value, if it is missing), so it appears in the saved file.
	 *
	 * @param name configuration key
	 * @param defaultValue value to set if the key is missing
	 */
	public synchronized void ensure(final String name, final boolean defaultValue) {
		if (!properties.containsKey(name)) {
			properties.setProperty(name, defaultValue ? "true" : "false");
		}
	}

	/**
	 * Ensures a recent list key and its maximum size key ("&lt;name&gt;.maxSize") are contained in the configuration.
	 *
	 * @param name configuration key of the list
	 * @param maxSize maximum size to set if the maximum size key is missing
	 */
	public synchronized void ensureRecentList(final String name, final int maxSize) {
		if (!properties.containsKey(name)) {
			properties.setProperty(name, "");
		}
		if (!properties.containsKey(name + MAXSIZE_EXTENSION)) {
			properties.setProperty(name + MAXSIZE_EXTENSION, Integer.toString(maxSize));
		}
	}

	/**
	 * Sets a string value.
	 *
	 * @param name configuration key
	 * @param value value, null is stored as empty string
	 */
	public synchronized void set(final String name, final String value) {
		properties.setProperty(name, value == null ? "" : value);
	}

	/**
	 * Sets an integer value.
	 *
	 * @param name configuration key
	 * @param value value
	 */
	public synchronized void set(final String name, final int value) {
		set(name, Integer.toString(value));
	}

	/**
	 * Sets a character value.
	 *
	 * @param name configuration key
	 * @param value value
	 */
	public synchronized void set(final String name, final char value) {
		set(name, Character.toString(value));
	}

	/**
	 * Sets a date value (format of {@link DateUtilities#YYYYMMDD_HHMMSS}).
	 *
	 * @param name configuration key
	 * @param value value, null is stored as empty string
	 */
	public synchronized void set(final String name, final LocalDateTime value) {
		set(name, value == null ? "" : DateUtilities.formatDate(DateUtilities.YYYYMMDD_HHMMSS, value));
	}

	/**
	 * Integer value of a key.
	 *
	 * @param name configuration key
	 * @return value, or 0 if the key does not exist or its value is empty
	 * @throws NumberFormatException if the value is no valid integer
	 */
	public synchronized int getInteger(final String name) {
		final String value = properties.getProperty(name);
		if (Utilities.isNotBlank(value)) {
			return Integer.parseInt(value.trim());
		} else {
			return 0;
		}
	}

	/**
	 * Character value of a key (first UTF-16 character of its value, so only characters of the basic multilingual
	 * plane are supported).
	 *
	 * @param name configuration key
	 * @return character, or null if the key does not exist or its value is empty
	 */
	public synchronized Character getCharacter(final String name) {
		final String value = properties.getProperty(name);
		if (Utilities.isNotEmpty(value)) {
			return value.charAt(0);
		} else {
			return null;
		}
	}

	/**
	 * Boolean value of a key (see {@link Utilities#interpretAsBool(String)}).
	 * Use {@link #getYesNoAsk(String, YesNoAsk)} for keys that may contain ‘ask’
	 *
	 * @param name configuration key
	 * @return value, false if the key does not exist
	 */
	public synchronized boolean getBoolean(final String name) {
		if (properties.containsKey(name)) {
			return Utilities.interpretAsBool(properties.getProperty(name));
		} else {
			return false;
		}
	}

	/**
	 * Date value of a key (format of {@link DateUtilities#YYYYMMDD_HHMMSS}).
	 *
	 * @param name configuration key
	 * @return value, or null if the key does not exist or its value is no valid date
	 */
	public synchronized LocalDateTime getDate(final String name) {
		try {
			if (properties.containsKey(name)) {
				return DateUtilities.parseLocalDateTime(DateUtilities.YYYYMMDD_HHMMSS, properties.getProperty(name));
			} else {
				return null;
			}
		} catch (@SuppressWarnings("unused") final DateTimeParseException e) {
			return null;
		}
	}

	/**
	 * Sets a boolean value.
	 *
	 * @param name configuration key
	 * @param value value
	 */
	public synchronized void set(final String name, final boolean value) {
		set(name, value ? "true" : "false");
	}

	/**
	 * List value of a key, stored as csv line. Values still stored in the legacy format are migrated to the current
	 * format (persisted on next save).
	 *
	 * @param name configuration key
	 * @return new modifiable list, empty if the key does not exist or its value cannot be read
	 */
	public synchronized List<String> getList(final String name) {
		final String value = properties.getProperty(name);
		if (value == null || value.isEmpty()) {
			return new ArrayList<>();
		} else if (!legacyListKeys.contains(name)) {
			try {
				return parseListValue(value, false);
			} catch (final Exception e) {
				// Do not silently lose the list, report why it could not be read
				System.err.println("Cannot read list value of configuration key '" + name + "': " + e.getMessage());
				return new ArrayList<>();
			}
		} else {
			// Value may be stored in the legacy format with backslash escaping, so migrate it to the plain list format
			List<String> list;
			try {
				list = parseListValue(value, true);
			} catch (@SuppressWarnings("unused") final Exception legacyException) {
				// Not readable as legacy value (e.g. edited manually with single backslashes), so read it as plain list
				try {
					list = parseListValue(value, false);
				} catch (final Exception e) {
					System.err.println("Cannot read list value of configuration key '" + name + "': " + e.getMessage());
					return new ArrayList<>();
				}
			}
			// Stores the list in the plain list format and marks the key as migrated (persisted on next save)
			set(name, list);
			return new ArrayList<>(list);
		}
	}

	/**
	 * Removes the first occurrence of a value from a list.
	 *
	 * @param name configuration key of the list
	 * @param value value to remove
	 * @return true if the value was contained
	 */
	public synchronized boolean removeFromList(final String name, final String value) {
		final List<String> currentList = getList(name);
		final boolean removedValue = currentList.remove(value);
		if (removedValue) {
			set(name, currentList);
		}
		return removedValue;
	}

	/**
	 * Sets a list value, stored as csv line.
	 *
	 * @param name configuration key
	 * @param list values, null is stored as empty list
	 */
	public synchronized void set(final String name, final List<String> list) {
		set(name, formatListValue(list == null ? new ArrayList<>() : list));
		if (plainListKeys.add(name)) {
			storePlainListKeys();
		}
		if (legacyListKeys.remove(name)) {
			storeLegacyListKeys();
		}
	}

	private void storePlainListKeys() {
		properties.setProperty(CONFIG_KEY_PLAIN_LIST_KEYS, formatListValue(new ArrayList<>(plainListKeys)));
	}

	private void storeLegacyListKeys() {
		if (legacyListKeys.isEmpty()) {
			properties.remove(CONFIG_KEY_LEGACY_LIST_KEYS);
		} else {
			properties.setProperty(CONFIG_KEY_LEGACY_LIST_KEYS, formatListValue(new ArrayList<>(legacyListKeys)));
		}
	}

	/**
	 * Formats list values as one csv line (separator ';', string quote '"', RFC 4180).
	 * Values are only quoted if needed (containing separator, string quote or linebreak), string quotes within quoted
	 * values are doubled. Null values are stored as empty values. A list with a single empty value is stored as
	 * <code>""</code>, so it can be told apart from an empty list. Backslashes are stored as they are, because
	 * java.util.Properties already takes care of backslashes and linebreaks when the configuration file is stored.
	 *
	 * @param values values to format
	 * @return csv line without trailing linebreak
	 */
	static String formatListValue(final List<String> values) {
		if (values.size() == 1 && (values.get(0) == null || values.get(0).isEmpty())) {
			return String.valueOf(LIST_STRING_QUOTE) + LIST_STRING_QUOTE;
		}

		final StringBuilder csvLine = new StringBuilder();
		for (int i = 0; i < values.size(); i++) {
			if (i > 0) {
				csvLine.append(LIST_SEPARATOR);
			}
			final String value = values.get(i) == null ? "" : values.get(i);
			if (value.indexOf(LIST_STRING_QUOTE) >= 0 || value.indexOf(LIST_SEPARATOR) >= 0 || value.indexOf('\r') >= 0 || value.indexOf('\n') >= 0) {
				csvLine.append(LIST_STRING_QUOTE);
				csvLine.append(value.replace(String.valueOf(LIST_STRING_QUOTE), String.valueOf(LIST_STRING_QUOTE) + LIST_STRING_QUOTE));
				csvLine.append(LIST_STRING_QUOTE);
			} else {
				csvLine.append(value);
			}
		}
		return csvLine.toString();
	}

	/**
	 * Parses one csv line of list values (separator ';', string quote '"', RFC 4180), as written by
	 * {@link #formatListValue(List)}. Quoted values may contain linebreaks, which are kept as they are (legacy values
	 * normalize them to LF). Unquoted values are not trimmed, whitespace around quoted values is ignored. A leading
	 * byte order mark is skipped.
	 *
	 * @param csvLine csv line, may end with a linebreak
	 * @param legacyBackslashEscaping true to resolve backslash escape sequences (legacy format, see
	 *            {@link #unescapeBackslashSequences(String)})
	 * @return new modifiable list of values
	 * @throws Exception if the text contains no or more than one csv line, a quoted value is not closed, there is text
	 *             after a quoted value or an escape sequence is invalid
	 */
	static List<String> parseListValue(final String csvLine, final boolean legacyBackslashEscaping) throws Exception {
		final List<List<String>> lines = new ArrayList<>();
		List<String> lineValues = new ArrayList<>();
		StringBuilder nextValue = new StringBuilder();
		boolean insideString = false;
		boolean isQuotedString = false;
		char previousCharacter = (char) -1;

		final int startIndex = !csvLine.isEmpty() && csvLine.charAt(0) == '﻿' ? 1 : 0;
		for (int i = startIndex; i < csvLine.length(); i++) {
			final char nextChar = csvLine.charAt(i);
			if (nextChar == LIST_STRING_QUOTE && (isQuotedString || nextValue.toString().trim().isEmpty())) {
				if (isQuotedString && !insideString && previousCharacter != LIST_STRING_QUOTE) {
					// Only a quote directly after the closing quote is a doubled quote, e.g. "a" "b" is invalid
					throw new Exception("Not allowed text '" + nextChar + "' after quoted value");
				}
				insideString = !insideString;
				nextValue.append(nextChar);
				isQuotedString = true;
			} else if (insideString) {
				nextValue.append(nextChar);
			} else if (nextChar == '\r' || nextChar == '\n') {
				if (nextValue.length() > 0 || previousCharacter == LIST_SEPARATOR) {
					lineValues.add(parseListItem(nextValue.toString(), isQuotedString, legacyBackslashEscaping));
					nextValue = new StringBuilder();
					isQuotedString = false;
				}
				if (!lineValues.isEmpty()) {
					// Line completed, linebreaks without any value before them are skipped
					lines.add(lineValues);
					lineValues = new ArrayList<>();
					previousCharacter = (char) -1;
					continue;
				}
			} else if (nextChar == LIST_SEPARATOR) {
				lineValues.add(parseListItem(nextValue.toString(), isQuotedString, legacyBackslashEscaping));
				nextValue = new StringBuilder();
				isQuotedString = false;
			} else if (isQuotedString) {
				if (!Character.isWhitespace(nextChar)) {
					throw new Exception("Not allowed text '" + nextChar + "' after quoted value");
				}
			} else {
				nextValue.append(nextChar);
			}
			previousCharacter = nextChar;
		}

		if (insideString) {
			throw new Exception("Unexpected end of data within quoted value");
		}
		if (nextValue.length() > 0 || previousCharacter == LIST_SEPARATOR) {
			lineValues.add(parseListItem(nextValue.toString(), isQuotedString, legacyBackslashEscaping));
		}
		if (!lineValues.isEmpty()) {
			lines.add(lineValues);
		}

		if (lines.isEmpty()) {
			throw new Exception("No csv lines in data");
		} else if (lines.size() > 1) {
			throw new Exception("Too many csv lines in data");
		} else {
			return lines.get(0);
		}
	}

	/**
	 * Removes the enclosing string quotes of a raw list value and resolves doubled string quotes and (legacy format)
	 * backslash escape sequences.
	 *
	 * @param rawValue raw value as read from the csv line, including string quotes
	 * @param isQuoted true, if the value was quoted. Only the whitespace around quoted values is removed, unquoted
	 *            values are kept as they are.
	 * @param legacyBackslashEscaping true to resolve backslash escape sequences and normalize linebreaks
	 * @return value
	 * @throws Exception if an escape sequence is invalid
	 */
	private static String parseListItem(final String rawValue, final boolean isQuoted, final boolean legacyBackslashEscaping) throws Exception {
		String value = rawValue;
		if (isQuoted) {
			// Whitespace before the opening quote is part of the raw value, whitespace after the closing quote is not
			value = value.trim();
			value = value.substring(1, value.length() - 1).replace(String.valueOf(LIST_STRING_QUOTE) + LIST_STRING_QUOTE, String.valueOf(LIST_STRING_QUOTE));
		}
		if (legacyBackslashEscaping && !value.isEmpty()) {
			value = normalizeLinebreaks(unescapeBackslashSequences(normalizeLinebreaks(value)));
		}
		return value;
	}

	/**
	 * Normalizes all linebreaks (CRLF, CR, LF) to LF.
	 *
	 * @param value text to normalize
	 * @return normalized text
	 */
	private static String normalizeLinebreaks(final String value) {
		return value.replace("\r\n", "\n").replace("\r", "\n");
	}

	/**
	 * Resolves backslash escape sequences of the legacy list format: \n, \r, \t, \b, \f, escaped space, \\, \', \",
	 * hexadecimal sequences with 2 digits (\x41), 16 bit unicode sequences with 4 digits (A) and unicode code
	 * points with 8 digits (\U0001F600).
	 *
	 * @param escapedText text to unescape
	 * @return unescaped text
	 * @throws Exception if the text contains an unknown or invalid escape sequence or ends with a single backslash
	 */
	private static String unescapeBackslashSequences(final String escapedText) throws Exception {
		final StringBuilder unescapedText = new StringBuilder();
		for (int i = 0; i < escapedText.length(); i++) {
			final char nextChar = escapedText.charAt(i);
			if (nextChar != '\\') {
				unescapedText.append(nextChar);
			} else if (i + 1 >= escapedText.length()) {
				throw new Exception("Invalid escape sequence at character index " + i + " (unescaped backslash at end of text)");
			} else {
				final char escapedChar = escapedText.charAt(i + 1);
				switch (escapedChar) {
					case 'n' -> unescapedText.append('\n');
					case 'r' -> unescapedText.append('\r');
					case 't' -> unescapedText.append('\t');
					case 'b' -> unescapedText.append('\b');
					case 'f' -> unescapedText.append('\f');
					case ' ', '\\', '\'', '"' -> unescapedText.append(escapedChar);
					case 'x' -> unescapedText.append((char) parseHexDigits(escapedText, i, 2));
					case 'u' -> unescapedText.append((char) parseHexDigits(escapedText, i, 4));
					case 'U' -> {
						final int codePoint = parseHexDigits(escapedText, i, 8);
						if (!Character.isValidCodePoint(codePoint)) {
							throw new Exception("Invalid unicode sequence at character index " + i);
						}
						unescapedText.appendCodePoint(codePoint);
					}
					default -> throw new Exception("Invalid escape sequence at character index " + i + " ('\\" + escapedChar + "')");
				}
				// Skip the escaped character and the hexadecimal digits of hex and unicode sequences
				i += switch (escapedChar) {
					case 'x' -> 3;
					case 'u' -> 5;
					case 'U' -> 9;
					default -> 1;
				};
			}
		}
		return unescapedText.toString();
	}

	/**
	 * Parses the ASCII hexadecimal digits of an escape sequence like \x41, A or \U0001F600.
	 *
	 * @param text text containing the escape sequence
	 * @param sequenceStartIndex index of the backslash starting the escape sequence
	 * @param digitCount number of hexadecimal digits after the backslash and the type character
	 * @return parsed value
	 * @throws Exception if there are not enough characters left or a character is no ASCII hexadecimal digit
	 */
	private static int parseHexDigits(final String text, final int sequenceStartIndex, final int digitCount) throws Exception {
		int value = 0;
		for (int i = sequenceStartIndex + 2; i < sequenceStartIndex + 2 + digitCount; i++) {
			final char digitChar = i < text.length() ? text.charAt(i) : 'g';
			final int digit;
			if (digitChar >= '0' && digitChar <= '9') {
				digit = digitChar - '0';
			} else if (digitChar >= 'a' && digitChar <= 'f') {
				digit = digitChar - 'a' + 10;
			} else if (digitChar >= 'A' && digitChar <= 'F') {
				digit = digitChar - 'A' + 10;
			} else {
				throw new Exception("Invalid hexadecimal escape sequence at character index " + sequenceStartIndex);
			}
			value = (value << 4) | digit;
		}
		return value;
	}

	/**
	 * Maximum size of a recent list. Falls back to DEFAULT_RECENT_LIST_MAX_SIZE, if "&lt;name&gt;.maxSize"
	 * is missing or invalid, because a maximum size of 0 would silently discard the whole list.
	 */
	private int getRecentListMaxSize(final String name) {
		try {
			final int maxSize = getInteger(name + MAXSIZE_EXTENSION);
			return maxSize > 0 ? maxSize : DEFAULT_RECENT_LIST_MAX_SIZE;
		} catch (@SuppressWarnings("unused") final NumberFormatException e) {
			return DEFAULT_RECENT_LIST_MAX_SIZE;
		}
	}

	/**
	 * Adds a value to a recent list. An existing equal value is moved to the end, the oldest values are removed if the
	 * maximum size ("&lt;name&gt;.maxSize") is exceeded.
	 *
	 * @param name configuration key of the list
	 * @param value value to add
	 */
	public synchronized void addRecentListEntry(final String name, final String value) {
		final List<String> valueList = new UniqueFifoQueuedList<>(getRecentListMaxSize(name), getList(name));
		valueList.add(value);
		set(name, valueList);
	}

	/**
	 * Appends a value to a list (duplicates allowed).
	 *
	 * @param name configuration key of the list
	 * @param value value to add
	 */
	public synchronized void addListEntry(final String name, final String value) {
		final List<String> valueList = getList(name);
		valueList.add(value);
		set(name, valueList);
	}

	/**
	 * Adds a value to a list without duplicates. The order of the existing values is kept, a new value is appended.
	 *
	 * @param name configuration key of the list
	 * @param value value to add
	 */
	public synchronized void addSetEntry(final String name, final String value) {
		final Set<String> valueSet = new LinkedHashSet<>(getList(name));
		valueSet.add(value);
		set(name, new ArrayList<>(valueSet));
	}

	/**
	 * Most recently added value of a recent list.
	 *
	 * @param name configuration key of the list
	 * @return latest value, or null if the list is empty
	 */
	public synchronized String getLatestRecentValue(final String name) {
		return new UniqueFifoQueuedList<>(getRecentListMaxSize(name), getList(name)).getLatestAdded();
	}

	/**
	 * Saves the configuration file (creating the sub directory if needed). Errors are only printed to standard error,
	 * use {@link #saveOrThrow()} to handle them.
	 */
	public void save() {
		try {
			saveOrThrow();
		} catch (final IOException e) {
			System.err.println("Cannot save configuration file '" + getConfigFile().getAbsolutePath() + "': " + e.getMessage());
		}
	}

	/**
	 * Saves the configuration file (creating the sub directory if needed).
	 * <p>
	 * The data is written to a temporary file first, which then replaces the configuration file, so an interrupted
	 * save never leaves a truncated configuration. On POSIX file systems the file is readable by the owner only,
	 * because it may contain credentials.
	 * </p>
	 *
	 * @throws IOException if the configuration file cannot be written
	 */
	public synchronized void saveOrThrow() throws IOException {
		Files.createDirectories(getConfigDirectory().toPath());
		Path configFile = getConfigFile().toPath();
		if (Files.isSymbolicLink(configFile) && Files.exists(configFile)) {
			// Replace the link target, otherwise the move would replace the link itself by a regular file
			configFile = configFile.toRealPath();
		}
		final Path directory = configFile.toAbsolutePath().getParent();

		final Path tempFile;
		if (directory.getFileSystem().supportedFileAttributeViews().contains("posix")) {
			tempFile = Files.createTempFile(directory, "." + applicationName, ".tmp", PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
		} else {
			tempFile = Files.createTempFile(directory, "." + applicationName, ".tmp");
		}

		try {
			try (OutputStream outputStream = Files.newOutputStream(tempFile)) {
				properties.store(outputStream, applicationName + ".config");
			}
			try {
				Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (@SuppressWarnings("unused") final AtomicMoveNotSupportedException e) {
				Files.move(tempFile, configFile, StandardCopyOption.REPLACE_EXISTING);
			}
		} finally {
			// Only exists anymore, if writing or moving failed
			Files.deleteIfExists(tempFile);
		}
	}

	/**
	 * All configuration keys.
	 *
	 * @return new list of keys, sorted alphabetically
	 */
	public synchronized List<String> getKeyList() {
		final List<String> returnList = new ArrayList<>();
		for (final Object key : properties.keySet()) {
			returnList.add(key.toString());
		}
		Collections.sort(returnList);
		return returnList;
	}

	/**
	 * Whether a key exists.
	 *
	 * @param name configuration key
	 * @return true if the key exists
	 */
	public synchronized boolean containsKey(final String name) {
		return properties.containsKey(name);
	}

	/**
	 * Removes a key.
	 *
	 * @param name configuration key
	 */
	public synchronized void removeKey(final String name) {
		properties.remove(name);
		if (plainListKeys.remove(name)) {
			storePlainListKeys();
		}
		if (legacyListKeys.remove(name)) {
			storeLegacyListKeys();
		}
	}

	/**
	 * Sets up the default keys of the library (language) and removes obsolete keys. Saves the configuration, if an
	 * obsolete key was removed. Subclasses add their own default keys and must call this method.
	 */
	public synchronized void setupDefaultConfig() {
		boolean obsoleteKeyRemoved = false;
		if (containsKey(CONFIG_KEY_LANGUAGE + ENUM_EXTENSION)) {
			removeKey(CONFIG_KEY_LANGUAGE + ENUM_EXTENSION);
			obsoleteKeyRemoved = true;
		}
		if (!containsKey(CONFIG_KEY_LANGUAGE)) {
			set(CONFIG_KEY_LANGUAGE, Locale.getDefault().getLanguage());
		}

		// TODO: Remove obsolete old legacy key in future
		if (containsKey("TlsServerCertificateCheck")) {
			removeKey("TlsServerCertificateCheck");
			obsoleteKeyRemoved = true;
		}

		if (obsoleteKeyRemoved) {
			// Saved after all changes, so the file never contains a partly updated configuration
			save();
		}
	}

	/**
	 * Configured application language.
	 *
	 * @return language code, or null if not configured
	 */
	public synchronized String getLanguage() {
		return get(CONFIG_KEY_LANGUAGE);
	}

	/**
	 * Registers a key as {@link YesNoAsk} value and ensures it is contained in the configuration (with the default value,
	 * if it is missing). Existing boolean values ("true" / "false") stay valid.
	 * Must be called on every application start, because the registration is not persisted.
	 *
	 * @param name configuration key
	 * @param defaultValue value to set if the key is missing, must not be null
	 * @throws IllegalArgumentException if the default value is null
	 */
	public synchronized void ensureYesNoAsk(final String name, final YesNoAsk defaultValue) {
		if (defaultValue == null) {
			throw new IllegalArgumentException("Invalid empty default value for configuration key '" + name + "'");
		}
		yesNoAskKeys.add(name);
		if (!properties.containsKey(name)) {
			properties.setProperty(name, defaultValue.getStorageValue());
		}
	}

	/**
	 * Sets a {@link YesNoAsk} value and registers the key as such.
	 *
	 * @param name configuration key
	 * @param value value, null is stored as empty string
	 */
	public synchronized void set(final String name, final YesNoAsk value) {
		yesNoAskKeys.add(name);
		set(name, value == null ? "" : value.getStorageValue());
	}

	/**
	 * {@link YesNoAsk} value of a key.
	 *
	 * @param name configuration key
	 * @param defaultValue value if the key does not exist or its value is empty
	 * @return value
	 */
	public synchronized YesNoAsk getYesNoAsk(final String name, final YesNoAsk defaultValue) {
		return YesNoAsk.fromString(properties.getProperty(name), defaultValue);
	}

	/**
	 * Whether a key was registered as {@link YesNoAsk} value (see {@link #ensureYesNoAsk(String, YesNoAsk)}).
	 *
	 * @param name configuration key
	 * @return true for YesNoAsk keys
	 */
	public synchronized boolean isYesNoAskKey(final String name) {
		return yesNoAskKeys.contains(name);
	}
}
