package de.soderer.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import de.soderer.utilities.collection.UniqueFifoQueuedList;
import de.soderer.utilities.csv.CsvDataException;
import de.soderer.utilities.csv.CsvFormat;
import de.soderer.utilities.csv.CsvReader;
import de.soderer.utilities.csv.CsvWriter;

/**
 * Application configuration stored as properties file "~/.&lt;applicationName&gt;.config" (or
 * "~/.&lt;applicationName&gt;/.&lt;applicationName&gt;.config" when using a sub directory).
 * Supports typed values, lists (stored as csv line) and recent lists with a maximum size.
 * Changes are only persisted by {@link #save()}.
 * Application specific settings like the proxy configuration are added by subclasses (e.g. in ApplicationBase).
 */
public class ConfigurationProperties {
	/** Key of the application version */
	public static final String CONFIG_KEY_VERSION = "Application.Version";
	/** Key of the application language (language code like "en") */
	public static final String CONFIG_KEY_LANGUAGE = "Application.Language";

	/**
	 * Internal key: names of all list keys, which are stored in the current list format
	 * (plain RFC 4180 csv without backslash escaping). List keys not contained here are
	 * still stored in the legacy format with backslash escaping and are migrated on first read.
	 */
	public static final String CONFIG_KEY_PLAIN_LIST_KEYS = "Application.PlainListKeys";

	/** Key extension for the maximum size of a recent list ("&lt;name&gt;.maxSize") */
	public static final String MAXSIZE_EXTENSION = ".maxSize";

	/** Maximum size of a recent list, if no valid "&lt;name&gt;.maxSize" is configured */
	public static final int DEFAULT_RECENT_LIST_MAX_SIZE = 10;
	/** Key extension for the comma separated allowed values of a key ("&lt;name&gt;.enum"), used by configuration dialogs */
	public static final String ENUM_EXTENSION = ".enum";

	private final Properties properties = new Properties();
	private final String applicationName;
	private boolean useSubDirectory = false;

	/** List keys already stored in the current (plain) list format */
	private final Set<String> plainListKeys = new TreeSet<>();

	/**
	 * CSV format of list values (e.g. recent lists) in the configuration.
	 * Plain RFC 4180 csv: backslashes (e.g. in Windows paths) are stored as they are.
	 * No backslash escaping is needed, because java.util.Properties already takes care of
	 * backslashes and linebreaks when the configuration file is stored and loaded.
	 */
	private static CsvFormat createListCsvFormat() {
		return new CsvFormat()
				.withSeparator(';')
				.withStringQuote('"')
				.withEscapeLineBreaks(false);
	}

	/**
	 * Legacy CSV format of list values with backslash escaping.
	 * Only used to read lists of existing configuration files, which were stored before
	 * the switch to the plain list format.
	 */
	private static CsvFormat createLegacyListCsvFormat() {
		return new CsvFormat()
				.withSeparator(';')
				.withStringQuote('"')
				.withEscapeLineBreaks(true);
	}

	/**
	 * Internal keys are managed by this class and are not meant to be edited by the user
	 * (e.g. hidden in configuration dialogs)
	 *
	 * @param name configuration key
	 * @return true for internal keys
	 */
	public static boolean isInternalKey(final String name) {
		return CONFIG_KEY_PLAIN_LIST_KEYS.equals(name);
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
		final File configFile = new File(System.getProperty("user.home") + (useSubDirectory ? File.separator + "." + applicationName : "") + File.separator + "." + applicationName + ".config");
		if (configFile.exists()) {
			try (FileInputStream fileInputStream = new FileInputStream(configFile)) {
				final Properties propertiesLoaded = new Properties();
				propertiesLoaded.load(fileInputStream);
				for (final Entry<Object, Object> entry : propertiesLoaded.entrySet()) {
					properties.setProperty((String) entry.getKey(), (String) entry.getValue());
				}
			} catch (final IOException e) {
				e.printStackTrace();
				throw new Exception("Cannot load configuration", e);
			}
		}

		final String plainListKeysValue = properties.getProperty(CONFIG_KEY_PLAIN_LIST_KEYS);
		if (Utilities.isNotEmpty(plainListKeysValue)) {
			try {
				plainListKeys.addAll(CsvReader.parseCsvLine(createListCsvFormat(), plainListKeysValue));
			} catch (final Exception e) {
				System.err.println("Cannot read configuration key '" + CONFIG_KEY_PLAIN_LIST_KEYS + "': " + e.getMessage());
			}
		}
	}

	/**
	 * Value of a key.
	 *
	 * @param name configuration key
	 * @return value, or null if the key does not exist
	 */
	public String get(final String name) {
		if (properties.containsKey(name)) {
			return properties.getProperty(name);
		} else {
			return null;
		}
	}

	/**
	 * Ensures this key is contained in the configuration (with an empty value, if it is missing), so it appears in the saved file.
	 *
	 * @param name configuration key
	 */
	public void ensure(final String name) {
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
	public void ensure(final String name, final boolean defaultValue) {
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
	public void ensureRecentList(final String name, final int maxSize) {
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
	public void set(final String name, final String value) {
		properties.setProperty(name, value == null ? "" : value);
	}

	/**
	 * Sets an integer value.
	 *
	 * @param name configuration key
	 * @param value value
	 */
	public void set(final String name, final int value) {
		set(name, Integer.toString(value));
	}

	/**
	 * Sets a character value.
	 *
	 * @param name configuration key
	 * @param value value
	 */
	public void set(final String name, final char value) {
		set(name, Character.toString(value));
	}

	/**
	 * Sets a date value (format of {@link DateUtilities#YYYYMMDD_HHMMSS}).
	 *
	 * @param name configuration key
	 * @param value value, null is stored as empty string
	 */
	public void set(final String name, final LocalDateTime value) {
		set(name, value == null ? "" : DateUtilities.formatDate(DateUtilities.YYYYMMDD_HHMMSS, value));
	}

	/**
	 * Integer value of a key.
	 *
	 * @param name configuration key
	 * @return value, or 0 if the key does not exist or its value is empty
	 * @throws NumberFormatException if the value is no valid integer
	 */
	public int getInteger(final String name) {
		if (properties.containsKey(name) && Utilities.isNotBlank(properties.getProperty(name))) {
			return Integer.parseInt(properties.getProperty(name).trim());
		} else {
			return 0;
		}
	}

	/**
	 * Character value of a key (first character of its value).
	 *
	 * @param name configuration key
	 * @return character, or null if the key does not exist or its value is empty
	 */
	public Character getCharacter(final String name) {
		if (properties.containsKey(name) && Utilities.isNotEmpty(properties.getProperty(name))) {
			return properties.getProperty(name).toCharArray()[0];
		} else {
			return null;
		}
	}

	/**
	 * Boolean value of a key (see {@link Utilities#interpretAsBool(String)}).
	 *
	 * @param name configuration key
	 * @return value, false if the key does not exist
	 */
	public boolean getBoolean(final String name) {
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
	public LocalDateTime getDate(final String name) {
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
	public void set(final String name, final boolean value) {
		set(name, value ? "true" : "false");
	}

	/**
	 * List value of a key, stored as csv line. Values in the legacy format are migrated to the current format.
	 *
	 * @param name configuration key
	 * @return new modifiable list, empty if the key does not exist or its value cannot be read
	 */
	public List<String> getList(final String name) {
		final String value = properties.getProperty(name);
		if (value == null || value.isEmpty()) {
			return new ArrayList<>();
		} else if (plainListKeys.contains(name)) {
			try {
				return new ArrayList<>(CsvReader.parseCsvLine(createListCsvFormat(), value));
			} catch (final Exception e) {
				// Do not silently lose the list, report why it could not be read
				System.err.println("Cannot read list value of configuration key '" + name + "': " + e.getMessage());
				return new ArrayList<>();
			}
		} else {
			// Value was stored in the legacy format with backslash escaping, so migrate it to the plain list format
			List<String> list;
			try {
				list = CsvReader.parseCsvLine(createLegacyListCsvFormat(), value);
			} catch (@SuppressWarnings("unused") final Exception legacyException) {
				// Not readable as legacy value (e.g. edited manually with single backslashes), so read it as plain list
				try {
					list = CsvReader.parseCsvLine(createListCsvFormat(), value);
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
	public boolean removeFromList(final String name, final String value) {
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
	public void set(final String name, final List<String> list) {
		try {
			set(name, CsvWriter.getCsvLine(createListCsvFormat(), list == null ? new ArrayList<>() : list));
		} catch (final CsvDataException e) {
			// Can not happen, because the list format always allows quotation
			throw new IllegalStateException("Cannot store list value for '" + name + "': " + e.getMessage(), e);
		}
		if (plainListKeys.add(name)) {
			storePlainListKeys();
		}
	}

	private void storePlainListKeys() {
		try {
			properties.setProperty(CONFIG_KEY_PLAIN_LIST_KEYS, CsvWriter.getCsvLine(createListCsvFormat(), new ArrayList<>(plainListKeys)));
		} catch (final CsvDataException e) {
			// Can not happen, because the list format always allows quotation
			throw new IllegalStateException("Cannot store configuration key '" + CONFIG_KEY_PLAIN_LIST_KEYS + "': " + e.getMessage(), e);
		}
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
	public void addRecentListEntry(final String name, final String value) {
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
	public void addListEntry(final String name, final String value) {
		final List<String> valueList = getList(name);
		valueList.add(value);
		set(name, valueList);
	}

	/**
	 * Adds a value to a list without duplicates. The order of the list is not preserved.
	 *
	 * @param name configuration key of the list
	 * @param value value to add
	 */
	public void addSetEntry(final String name, final String value) {
		final Set<String> valueSet = new HashSet<>(getList(name));
		valueSet.add(value);
		set(name, new ArrayList<>(valueSet));
	}

	/**
	 * Most recently added value of a recent list.
	 *
	 * @param name configuration key of the list
	 * @return latest value, or null if the list is empty
	 */
	public String getLatestRecentValue(final String name) {
		return new UniqueFifoQueuedList<>(getRecentListMaxSize(name), getList(name)).getLatestAdded();
	}

	/**
	 * Saves the configuration file (creating the sub directory if needed). Errors are only printed to standard error.
	 */
	public void save() {
		if (useSubDirectory && !new File(System.getProperty("user.home") + File.separator + "." + applicationName).exists()) {
			new File(System.getProperty("user.home") + File.separator + "." + applicationName).mkdirs();
		}
		try (FileOutputStream fileOutputStream = new FileOutputStream(System.getProperty("user.home") + (useSubDirectory ? File.separator + "." + applicationName : "") + File.separator + "." + applicationName + ".config")) {
			final Properties propertiesToStore = new Properties();
			for (final Entry<Object, Object> entry : properties.entrySet()) {
				propertiesToStore.setProperty((String) entry.getKey(), (String) entry.getValue());
			}
			propertiesToStore.store(fileOutputStream, applicationName + ".config");
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * All configuration keys.
	 *
	 * @return new list of keys
	 */
	public List<String> getKeyList() {
		final List<String> returnList = new ArrayList<>();
		for (final Object key : properties.keySet()) {
			returnList.add(key.toString());
		}
		return returnList;
	}

	/**
	 * Whether a key exists.
	 *
	 * @param name configuration key
	 * @return true if the key exists
	 */
	public boolean containsKey(final String name) {
		return properties.containsKey(name);
	}

	/**
	 * Removes a key.
	 *
	 * @param name configuration key
	 */
	public void removeKey(final String name) {
		properties.remove(name);
		if (plainListKeys.remove(name)) {
			storePlainListKeys();
		}
	}

	/**
	 * Sets up the default keys of the library (language) and removes obsolete keys. May save the configuration.
	 * Subclasses add their own default keys and must call this method.
	 */
	public void setupDefaultConfig() {
		if (containsKey(CONFIG_KEY_LANGUAGE + ENUM_EXTENSION)) {
			removeKey(CONFIG_KEY_LANGUAGE + ENUM_EXTENSION);
			save();
		}
		if (!containsKey(CONFIG_KEY_LANGUAGE)) {
			set(CONFIG_KEY_LANGUAGE, Locale.getDefault().getLanguage());
		}

		// TODO: Remove obsolete old legacy key in future
		if (containsKey("TlsServerCertificateCheck")) {
			removeKey("TlsServerCertificateCheck");
		}
	}

	/**
	 * Configured application language.
	 *
	 * @return language code, or null if not configured
	 */
	public String getLanguage() {
		return get(CONFIG_KEY_LANGUAGE);
	}
}
