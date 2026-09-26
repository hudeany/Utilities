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

import de.soderer.pac.PacScriptParser;
import de.soderer.pac.utilities.ProxyConfiguration;
import de.soderer.pac.utilities.ProxyConfiguration.ProxyConfigurationType;
import de.soderer.utilities.collection.UniqueFifoQueuedList;
import de.soderer.utilities.csv.CsvDataException;
import de.soderer.utilities.csv.CsvFormat;
import de.soderer.utilities.csv.CsvReader;
import de.soderer.utilities.csv.CsvWriter;

public class ConfigurationProperties {
	public static final String CONFIG_KEY_VERSION = "Application.Version";
	public static final String CONFIG_KEY_LANGUAGE = "Application.Language";
	public static final String CONFIG_KEY_PROXY_CONFIGURATION_TYPE = "Application.ProxyConfigurationType";
	public static final String CONFIG_KEY_PROXY_URL = "Application.ProxyUrl";
	public static final String CONFIG_KEY_DAILY_UPDATE_CHECK = "Application.DailyUpdateCheck";
	public static final String CONFIG_KEY_NEXT_DAILY_UPDATE_CHECK = "Application.NextDailyUpdateCheck";

	/**
	 * Internal key: names of all list keys, which are stored in the current list format
	 * (plain RFC 4180 csv without backslash escaping). List keys not contained here are
	 * still stored in the legacy format with backslash escaping and are migrated on first read.
	 */
	public static final String CONFIG_KEY_PLAIN_LIST_KEYS = "Application.PlainListKeys";

	public static final String MAXSIZE_EXTENSION = ".maxSize";

	/** Maximum size of a recent list, if no valid "<name>.maxSize" is configured */
	public static final int DEFAULT_RECENT_LIST_MAX_SIZE = 10;
	public static final String ENUM_EXTENSION = ".enum";

	private final Properties properties = new Properties();
	private final String applicationName;
	private boolean useSubDirectory = false;

	private ProxyConfiguration proxyConfiguration = null;

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
	 */
	public static boolean isInternalKey(final String name) {
		return CONFIG_KEY_PLAIN_LIST_KEYS.equals(name);
	}

	public ConfigurationProperties(final String applicationName) throws Exception {
		this(applicationName, false);
	}

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

	public String get(final String name) {
		if (properties.containsKey(name)) {
			return properties.getProperty(name);
		} else {
			return null;
		}
	}

	/**
	 * Ensure this key is enclosed in the configuration within the saved file
	 *
	 * @param name
	 */
	public void ensure(final String name) {
		if (!properties.containsKey(name)) {
			properties.setProperty(name, "");
		}
	}

	/**
	 * Ensure this key is enclosed in the configuration within the saved file
	 *
	 * @param name
	 */
	public void ensure(final String name, final boolean defaultValue) {
		if (!properties.containsKey(name)) {
			properties.setProperty(name, defaultValue ? "true" : "false");
		}
	}

	public void ensureRecentList(final String name, final int maxSize) {
		if (!properties.containsKey(name)) {
			properties.setProperty(name, "");
		}
		if (!properties.containsKey(name + MAXSIZE_EXTENSION)) {
			properties.setProperty(name + MAXSIZE_EXTENSION, Integer.toString(maxSize));
		}
	}

	public void set(final String name, final String value) {
		properties.setProperty(name, value == null ? "" : value);
		if (CONFIG_KEY_PROXY_CONFIGURATION_TYPE.equals(name) || CONFIG_KEY_PROXY_URL.equals(name)) {
			proxyConfiguration = null;
		}
	}

	public void set(final String name, final int value) {
		properties.setProperty(name, Integer.toString(value));
	}

	public void set(final String name, final char value) {
		properties.setProperty(name, Character.toString(value));
	}

	public void set(final String name, final LocalDateTime value) {
		properties.setProperty(name, DateUtilities.formatDate(DateUtilities.YYYYMMDD_HHMMSS, value));
	}

	public int getInteger(final String name) {
		if (properties.containsKey(name)) {
			return Integer.parseInt(properties.getProperty(name));
		} else {
			return 0;
		}
	}

	public Character getCharacter(final String name) {
		if (properties.containsKey(name) && Utilities.isNotEmpty(properties.getProperty(name))) {
			return properties.getProperty(name).toCharArray()[0];
		} else {
			return null;
		}
	}

	public boolean getBoolean(final String name) {
		if (properties.containsKey(name)) {
			return Utilities.interpretAsBool(properties.getProperty(name));
		} else {
			return false;
		}
	}

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

	public void set(final String name, final boolean value) {
		properties.setProperty(name, value ? "true" : "false");
	}

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

	public boolean removeFromList(final String name, final String value) {
		final List<String> currentList = getList(name);
		final boolean removedValue = currentList.remove(value);
		if (removedValue) {
			set(name, currentList);
		}
		return removedValue;
	}

	public void set(final String name, final List<String> list) {
		try {
			properties.setProperty(name, CsvWriter.getCsvLine(createListCsvFormat(), list == null ? new ArrayList<>() : list));
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
	 * Maximum size of a recent list. Falls back to DEFAULT_RECENT_LIST_MAX_SIZE, if "<name>.maxSize"
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

	public void addRecentListEntry(final String name, final String value) {
		final List<String> valueList = new UniqueFifoQueuedList<>(getRecentListMaxSize(name), getList(name));
		valueList.add(value);
		set(name, valueList);
	}

	public void addListEntry(final String name, final String value) {
		final List<String> valueList = getList(name);
		valueList.add(value);
		set(name, valueList);
	}

	public void addSetEntry(final String name, final String value) {
		final Set<String> valueSet = new HashSet<>(getList(name));
		valueSet.add(value);
		set(name, new ArrayList<>(valueSet));
	}

	public String getLatestRecentValue(final String name) {
		return new UniqueFifoQueuedList<>(getRecentListMaxSize(name), getList(name)).getLatestAdded();
	}

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

	public List<String> getKeyList() {
		final List<String> returnList = new ArrayList<>();
		for (final Object key : properties.keySet()) {
			returnList.add(key.toString());
		}
		return returnList;
	}

	public boolean containsKey(final String name) {
		return properties.containsKey(name);
	}

	public void removeKey(final String name) {
		properties.remove(name);
		if (plainListKeys.remove(name)) {
			storePlainListKeys();
		}
	}

	public void setupDefaultConfig() {
		set(CONFIG_KEY_LANGUAGE + ConfigurationProperties.ENUM_EXTENSION, LangResources.getAvailableLanguages("en"));
		if (!containsKey(CONFIG_KEY_LANGUAGE)) {
			set(CONFIG_KEY_LANGUAGE, Locale.getDefault().getLanguage());
		}

		set(CONFIG_KEY_PROXY_CONFIGURATION_TYPE + ConfigurationProperties.ENUM_EXTENSION, "None,System,Proxy-URL,WPAD,PAC-URL");
		if (!containsKey(CONFIG_KEY_PROXY_CONFIGURATION_TYPE)) {
			if (PacScriptParser.findPacFileUrlByWpad() != null) {
				set(CONFIG_KEY_PROXY_CONFIGURATION_TYPE, ProxyConfigurationType.WPAD.name());
			} else {
				set(CONFIG_KEY_PROXY_CONFIGURATION_TYPE, ProxyConfigurationType.None.name());
			}
			save();
		}
		if (!containsKey(CONFIG_KEY_PROXY_URL)) {
			set(CONFIG_KEY_PROXY_URL, "");
		}

		if (containsKey("DailyUpdateCheck")) {
			if (!containsKey(CONFIG_KEY_DAILY_UPDATE_CHECK)) {
				set(CONFIG_KEY_DAILY_UPDATE_CHECK, getBoolean("DailyUpdateCheck"));
			}
			removeKey("DailyUpdateCheck");
		}
		if (!containsKey(CONFIG_KEY_DAILY_UPDATE_CHECK)) {
			set(CONFIG_KEY_DAILY_UPDATE_CHECK, false);
		}

		if (containsKey("NextDailyUpdateCheck")) {
			if (!containsKey(CONFIG_KEY_NEXT_DAILY_UPDATE_CHECK)) {
				set(CONFIG_KEY_NEXT_DAILY_UPDATE_CHECK, get("NextDailyUpdateCheck"));
			}
			removeKey("NextDailyUpdateCheck");
		}
		if (!containsKey(CONFIG_KEY_NEXT_DAILY_UPDATE_CHECK)) {
			set(CONFIG_KEY_NEXT_DAILY_UPDATE_CHECK, "");
		}

		// TODO: Remove obsolete old legacy key in future
		if (containsKey("TlsServerCertificateCheck")) {
			removeKey("TlsServerCertificateCheck");
		}
	}

	public String getLanguage() {
		return get(CONFIG_KEY_LANGUAGE);
	}

	public ProxyConfiguration getProxyConfiguration() {
		if (proxyConfiguration == null) {
			final ProxyConfigurationType proxyConfigurationType = ProxyConfigurationType.getFromString(get(CONFIG_KEY_PROXY_CONFIGURATION_TYPE));
			final String proxyUrl = get(CONFIG_KEY_PROXY_URL);
			proxyConfiguration = new ProxyConfiguration(proxyConfigurationType, proxyUrl);
		}
		return proxyConfiguration;
	}
}
