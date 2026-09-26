package de.soderer.utilities;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LangResources {
	/** Base name of the language property files in the classpath root (e.g. "LanguageProperties_de.properties") */
	private static final String BUNDLE_NAME = "LanguageProperties";

	/** Matches the file name of a language file and extracts its language code like "de", "de_CH" or "de_BY" */
	private static final Pattern LANGUAGE_FILE_NAME_PATTERN = Pattern.compile("^" + Pattern.quote(BUNDLE_NAME) + "_([a-z]{2,3}(?:_[A-Za-z0-9]{2,8})*)\\.properties$");

	/** Language codes of all language files found in the classpath (without the root file) */
	private static List<String> availableLanguages;

	private static PropertyResourceBundle propertyResourceBundle;

	public static void enforceDefaultLocale() {
		try {
			propertyResourceBundle = (PropertyResourceBundle) ResourceBundle.getBundle(BUNDLE_NAME, Locale.ROOT);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static String get(final String resourceKey, final Object... arguments) {
		if (propertyResourceBundle == null) {
			try {
				propertyResourceBundle = (PropertyResourceBundle) ResourceBundle.getBundle(BUNDLE_NAME);
			} catch (final Exception e) {
				e.printStackTrace();
				return "Missing resourceBundle: " + resourceKey + (arguments != null && arguments.length > 0 ? " " + Utilities.join(arguments, ", ") : "");
			}
		}

		if (propertyResourceBundle != null) {
			if (propertyResourceBundle.containsKey(resourceKey)) {
				final String pattern = propertyResourceBundle.getString(resourceKey);
				return format(pattern, arguments);
			} else {
				return "Missing resourceKey: " + resourceKey + (arguments != null && arguments.length > 0 ? " " + Utilities.join(arguments, ", ") : "");
			}
		} else {
			return "Missing resourceBundle: " + resourceKey + (arguments != null && arguments.length > 0 ? " " + Utilities.join(arguments, ", ") : "");
		}
	}

	/**
	 * Replaces the placeholders "{0}", "{1}", ... literally by the arguments (no MessageFormat quoting)
	 */
	static String format(String pattern, final Object[] arguments) {
		if (arguments != null) {
			for (int i = 0; i < arguments.length; i++) {
				final Object item = arguments [i];
				pattern = pattern.replace("{" + i + "}", item == null ? "" : item.toString());
			}
		}
		return pattern;
	}

	public static boolean existsKey(final String resourceKey) {
		if (propertyResourceBundle == null) {
			try {
				propertyResourceBundle = (PropertyResourceBundle) ResourceBundle.getBundle(BUNDLE_NAME);
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Do nothing
			}
		}

		if (propertyResourceBundle != null) {
			return propertyResourceBundle.containsKey(resourceKey);
		} else {
			return false;
		}
	}

	/**
	 * Detects the languages by listing the existing language files in the
	 * classpath (directories as well as jar files), so any code like "de_BY" is
	 * found, even if the JRE does not know it as a locale.
	 *
	 * Also works with Eclipse's jar-in-jar loader ("rsrc:" URLs), whose
	 * locations cannot be listed directly. In that case the entries of
	 * "java.class.path" (the outer jar file) are listed instead.
	 *
	 * @param defaultLanguageCode
	 *            language of the root file without language suffix
	 *            ("LanguageProperties.properties"), added to the result if that
	 *            file exists. May be null.
	 * @return sorted list of language codes
	 */
	public static synchronized List<String> getAvailableLanguages(final String defaultLanguageCode) {
		final ClassLoader classLoader = LangResources.class.getClassLoader();

		if (availableLanguages == null) {
			// TreeSet keeps the result sorted and free of duplicates
			final Set<String> languages = new TreeSet<>();
			boolean unlistableLocationFound = false;
			for (final String location : getLanguageFileLocations(classLoader, defaultLanguageCode)) {
				if (isListableLocation(location)) {
					addLanguageCodesOfLocation(languages, location);
				} else {
					// e.g. "rsrc:" URLs of Eclipse's jar-in-jar loader, which cannot be listed directly
					unlistableLocationFound = true;
				}
			}

			if (unlistableLocationFound) {
				// The classpath entries (e.g. the outer jar file of a jar-in-jar application) contain those files
				for (final String location : getClassPathLocations()) {
					addLanguageCodesOfLocation(languages, location);
				}
			}

			availableLanguages = new ArrayList<>(languages);
		}

		final Set<String> result = new TreeSet<>(availableLanguages);
		if (defaultLanguageCode != null && !defaultLanguageCode.isBlank() && classLoader.getResource(BUNDLE_NAME + ".properties") != null) {
			result.add(defaultLanguageCode);
		}
		return new ArrayList<>(result);
	}

	/**
	 * Only "file" and "jar" locations can be listed. Checked by the URI scheme,
	 * so no URL stream handler is needed for this check.
	 */
	private static boolean isListableLocation(final String location) {
		try {
			final String scheme = URI.create(location).getScheme();
			return "file".equalsIgnoreCase(scheme) || "jar".equalsIgnoreCase(scheme);
		} catch (@SuppressWarnings("unused") final IllegalArgumentException e) {
			return false;
		}
	}

	private static void addLanguageCodesOfLocation(final Set<String> languages, final String location) {
		try {
			languages.addAll(listLanguageCodes(URI.create(location).toURL()));
		} catch (final Exception e) {
			// Location cannot be listed, the other locations may still contain language files
			e.printStackTrace();
		}
	}

	/**
	 * Finds the classpath locations (directories or jar files) containing
	 * language files, by looking up language files that are known to exist.
	 * Falls back to all entries of the classpath, if none of those exist.
	 */
	private static Set<String> getLanguageFileLocations(final ClassLoader classLoader, final String defaultLanguageCode) {
		final List<String> knownFileNames = new ArrayList<>();
		knownFileNames.add(BUNDLE_NAME + ".properties");
		if (defaultLanguageCode != null && !defaultLanguageCode.isBlank()) {
			knownFileNames.add(BUNDLE_NAME + "_" + defaultLanguageCode + ".properties");
		}

		// LinkedHashSet, because the same location is found once per known file
		final Set<String> locations = new LinkedHashSet<>();
		for (final String knownFileName : knownFileNames) {
			try {
				for (final URL url : Collections.list(classLoader.getResources(knownFileName))) {
					locations.add(url.toExternalForm());
				}
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}

		if (locations.isEmpty()) {
			// No known file: check every classpath entry
			locations.addAll(getClassPathLocations());
		}

		return locations;
	}

	/**
	 * Locations of all entries of the system property "java.class.path"
	 */
	private static Set<String> getClassPathLocations() {
		final Set<String> locations = new LinkedHashSet<>();
		for (final String classPathEntry : System.getProperty("java.class.path", "").split(File.pathSeparator)) {
			if (!classPathEntry.isBlank()) {
				try {
					final File file = new File(classPathEntry);
					if (file.isDirectory()) {
						// Dummy file name, only its directory is listed
						locations.add(new File(file, BUNDLE_NAME + ".properties").toURI().toURL().toExternalForm());
					} else if (file.isFile()) {
						// Root of the jar file, which (unlike a missing entry) can always be opened
						locations.add("jar:" + file.toURI().toURL().toExternalForm() + "!/");
					}
				} catch (final IOException e) {
					e.printStackTrace();
				}
			}
		}
		return locations;
	}

	/**
	 * Lists the language codes of all language files next to the given file URL
	 * (a file in a directory or an entry in a jar file).
	 */
	private static List<String> listLanguageCodes(final URL fileUrl) throws IOException, URISyntaxException {
		final List<String> languageCodes = new ArrayList<>();
		if ("file".equalsIgnoreCase(fileUrl.getProtocol())) {
			final Path directory = Paths.get(fileUrl.toURI()).getParent();
			if (directory != null && Files.isDirectory(directory)) {
				try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(directory, BUNDLE_NAME + "_*.properties")) {
					for (final Path path : directoryStream) {
						addLanguageCode(languageCodes, path.getFileName().toString());
					}
				}
			}
		} else if ("jar".equalsIgnoreCase(fileUrl.getProtocol())) {
			final URLConnection connection = fileUrl.openConnection();
			if (connection instanceof JarURLConnection) {
				// No caching, so the jar file may be closed here without affecting other users of that jar
				connection.setUseCaches(false);
				try (JarFile jarFile = ((JarURLConnection) connection).getJarFile()) {
					final Enumeration<JarEntry> entries = jarFile.entries();
					while (entries.hasMoreElements()) {
						final JarEntry entry = entries.nextElement();
						// Only files in the jar's root directory
						if (!entry.isDirectory() && !entry.getName().contains("/")) {
							addLanguageCode(languageCodes, entry.getName());
						}
					}
				}
			}
		}
		return languageCodes;
	}

	private static void addLanguageCode(final List<String> languageCodes, final String fileName) {
		final Matcher matcher = LANGUAGE_FILE_NAME_PATTERN.matcher(fileName);
		if (matcher.matches()) {
			languageCodes.add(matcher.group(1));
		}
	}
}
