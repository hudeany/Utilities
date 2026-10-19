package de.soderer.utilities;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper methods to print debug information (properties, classloader hierarchy, classpath)
 * to the java.util.logging logger at level FINE.
 * <p>
 * Values of properties whose key looks like a secret (password, secret, token, ...) are hidden,
 * and so are the program arguments in "sun.java.command", because log files are often readable
 * by more people than the configuration.
 * </p>
 */
public class DebugUtils {
	private static final Logger LOGGER = Logger.getLogger(DebugUtils.class.getName());

	/** Replacement text for hidden values. */
	private static final String HIDDEN_VALUE = "<hidden>";

	/** Parts of property keys (lower case) whose values are hidden. */
	private static final String[] SECRET_KEY_PARTS = { "password", "passwd", "pwd", "passphrase", "secret", "token", "credential" };

	/** System property containing the main class or jar file followed by the program arguments. */
	private static final String JAVA_COMMAND_PROPERTY = "sun.java.command";

	private DebugUtils() {
		// Utility class, no instances
	}

	/**
	 * Logs all properties as "key = 'value'" lines, sorted by key.
	 * Non-String keys and values are ignored, as in {@link Properties#stringPropertyNames()}.
	 * Values of secret looking keys and program arguments are hidden, line breaks in values are
	 * shown as \r and \n, so each property stays on one log line.
	 *
	 * @param properties the properties to log, may be null
	 */
	public static void printProperties(final Properties properties) {
		if (properties == null || !LOGGER.isLoggable(Level.FINE)) {
			return;
		}

		for (final String key : new TreeSet<>(properties.stringPropertyNames())) {
			LOGGER.fine(key + " = '" + getLoggableValue(key, properties.getProperty(key)) + "'");
		}
	}

	/**
	 * Returns a property value as it may be logged.
	 *
	 * @param key the property key
	 * @param value the property value, may be null
	 * @return the value with hidden secrets and escaped line breaks
	 */
	private static String getLoggableValue(final String key, final String value) {
		if (value == null) {
			return null;
		} else if (isSecretKey(key)) {
			return HIDDEN_VALUE;
		} else if (JAVA_COMMAND_PROPERTY.equals(key)) {
			// Program arguments may contain passwords, only the main class or jar file is logged
			final String trimmedValue = value.trim();
			final int argumentsStart = indexOfWhitespace(trimmedValue);
			if (argumentsStart < 0) {
				return escapeLinebreaks(trimmedValue);
			} else {
				return escapeLinebreaks(trimmedValue.substring(0, argumentsStart)) + " " + HIDDEN_VALUE;
			}
		} else {
			return escapeLinebreaks(value);
		}
	}

	/**
	 * Checks whether a property key looks like the key of a secret value.
	 *
	 * @param key the property key
	 * @return true, if the value should not be logged
	 */
	private static boolean isSecretKey(final String key) {
		final String lowerCaseKey = key.toLowerCase(Locale.ROOT);
		for (final String secretKeyPart : SECRET_KEY_PARTS) {
			if (lowerCaseKey.contains(secretKeyPart)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns the index of the first whitespace character.
	 *
	 * @param text the text to search
	 * @return the index, or -1 if the text contains no whitespace
	 */
	private static int indexOfWhitespace(final String text) {
		for (int i = 0; i < text.length(); i++) {
			if (Character.isWhitespace(text.charAt(i))) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Escapes line breaks, so a value cannot spread over several log lines or fake log entries.
	 *
	 * @param value the value
	 * @return the value with CR and LF shown as \r and \n
	 */
	private static String escapeLinebreaks(final String value) {
		return value.replace("\r", "\\r").replace("\n", "\\n");
	}

	/**
	 * Logs the classloader hierarchy of the given class, including class and interfaces of each classloader
	 * and the libraries of each URLClassLoader. Classes of the JDK core (like java.lang.String) are loaded by
	 * the bootstrap classloader, which has no Java object and is logged as such.
	 * Since Java 9 the application classloader is no URLClassLoader anymore, so the
	 * "java.class.path" and "jdk.module.path" entries are logged additionally.
	 *
	 * @param clazz the class whose classloader hierarchy is logged
	 */
	public static void printClasspath(final Class<?> clazz) {
		if (clazz == null || !LOGGER.isLoggable(Level.FINE)) {
			return;
		}

		ClassLoader classLoader = clazz.getClassLoader();
		while (classLoader != null) {
			// Interfaces of all classes in the hierarchy, not only of the concrete classloader class
			final Set<String> interfaceNames = new LinkedHashSet<>();
			Class<?> classLoaderClass = classLoader.getClass();
			while (classLoaderClass != null) {
				LOGGER.fine("Classloader (Class): " + classLoaderClass.getName());
				for (final Class<?> classLoaderInterface : classLoaderClass.getInterfaces()) {
					interfaceNames.add(classLoaderInterface.getName());
				}
				classLoaderClass = classLoaderClass.getSuperclass();
			}

			for (final String interfaceName : interfaceNames) {
				LOGGER.fine("Classloader (Interface): " + interfaceName);
			}

			if (classLoader instanceof URLClassLoader) {
				for (final URL url : ((URLClassLoader) classLoader).getURLs()) {
					LOGGER.fine("Library: " + url);
				}
			}

			classLoader = classLoader.getParent();
			LOGGER.fine("------------------------------------------------------------------------------");
		}
		LOGGER.fine("Classloader: bootstrap classloader (JDK core classes)");
		LOGGER.fine("------------------------------------------------------------------------------");

		logPathEntries("Classpath entry: ", System.getProperty("java.class.path"));
		logPathEntries("Module path entry: ", System.getProperty("jdk.module.path"));
	}

	/**
	 * Logs the entries of a path list like the classpath.
	 *
	 * @param prefix the text logged before each entry
	 * @param pathList the path list separated by {@link File#pathSeparator}, may be null
	 */
	private static void logPathEntries(final String prefix, final String pathList) {
		if (pathList != null && !pathList.isEmpty()) {
			for (final String pathEntry : pathList.split(File.pathSeparator)) {
				if (!pathEntry.isEmpty()) {
					LOGGER.fine(prefix + pathEntry);
				}
			}
		}
	}
}
