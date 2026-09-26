package de.soderer.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeSet;

public class LangResources {
	/** Base name of the language property files in the classpath root (e.g. "LanguageProperties_de.properties") */
	private static final String BUNDLE_NAME = "LanguageProperties";

	private static PropertyResourceBundle propertyResourceBundle;

	private static List<String> availableLanguages;

	/**
	 * Detects all languages for which a language properties file exists in the classpath.
	 * Works for plain files (IDE) as well as for entries inside jar files.
	 * The root file without language suffix is not included, because its language is unknown.
	 *
	 * @return sorted list of ISO 639 language codes (the result is cached)
	 */
	public static synchronized List<String> getAvailableLanguages() {
		if (availableLanguages == null) {
			final ClassLoader classLoader = LangResources.class.getClassLoader();
			// TreeSet keeps the result sorted and free of duplicates
			final Set<String> languages = new TreeSet<>();

			// Plain languages like "de", "en"
			for (final String languageCode : Locale.getISOLanguages()) {
				if (classLoader.getResource(BUNDLE_NAME + "_" + languageCode + ".properties") != null) {
					languages.add(languageCode);
				}
			}

			// Language and country combinations like "de_CH", "en_GB"
			for (final Locale locale : Locale.getAvailableLocales()) {
				// Skip plain languages (already checked) and locales with script or variant (e.g. "sr_BA_#Latn")
				if (!locale.getCountry().isEmpty() && locale.getScript().isEmpty() && locale.getVariant().isEmpty()) {
					final String localeName = locale.getLanguage() + "_" + locale.getCountry();
					if (classLoader.getResource(BUNDLE_NAME + "_" + localeName + ".properties") != null) {
						languages.add(localeName);
					}
				}
			}

			availableLanguages = new ArrayList<>(languages);
		}
		return new ArrayList<>(availableLanguages);
	}

	public static void enforceDefaultLocale() {
		try {
			propertyResourceBundle = (PropertyResourceBundle) ResourceBundle.getBundle("LanguageProperties", Locale.ROOT);
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public static String get(final String resourceKey, final Object... arguments) {
		if (propertyResourceBundle == null) {
			try {
				propertyResourceBundle = (PropertyResourceBundle) ResourceBundle.getBundle("LanguageProperties");
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
				propertyResourceBundle = (PropertyResourceBundle) ResourceBundle.getBundle("LanguageProperties");
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
}
