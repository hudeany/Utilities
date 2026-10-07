package de.soderer.utilities;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Helper methods to print debug information (properties, classloader hierarchy, classpath)
 * to the java.util.logging logger at level FINE.
 */
public class DebugUtils {
	private static final Logger LOGGER = Logger.getLogger(DebugUtils.class.getName());

	private DebugUtils() {
		// Utility class, no instances
	}

	/**
	 * Logs all properties as "key = 'value'" lines, sorted by key.
	 * Non-String keys and values are ignored, as in {@link Properties#stringPropertyNames()}.
	 *
	 * @param properties the properties to log, may be null
	 */
	public static void printProperties(final Properties properties) {
		if (properties == null || !LOGGER.isLoggable(Level.FINE)) {
			return;
		}

		for (final String key : new TreeSet<>(properties.stringPropertyNames())) {
			LOGGER.fine(key + " = '" + properties.getProperty(key) + "'");
		}
	}

	/**
	 * Logs the classloader hierarchy of the given class, including class and interfaces of each classloader
	 * and the libraries of each URLClassLoader.
	 * Since Java 9 the application classloader is no URLClassLoader anymore, so the
	 * "java.class.path" entries are logged additionally.
	 *
	 * @param clazz the class whose classloader hierarchy is logged
	 */
	public static void printClasspath(final Class<?> clazz) {
		if (clazz == null || !LOGGER.isLoggable(Level.FINE)) {
			return;
		}

		ClassLoader classLoader = clazz.getClassLoader();
		while (classLoader != null) {
			Class<?> classLoaderClass = classLoader.getClass();
			while (classLoaderClass != null) {
				LOGGER.fine("Classloader (Class): " + classLoaderClass.getName());
				classLoaderClass = classLoaderClass.getSuperclass();
			}

			for (final Class<?> classLoaderInterface : classLoader.getClass().getInterfaces()) {
				LOGGER.fine("Classloader (Interface): " + classLoaderInterface.getName());
			}

			if (classLoader instanceof URLClassLoader) {
				for (final URL url : ((URLClassLoader) classLoader).getURLs()) {
					LOGGER.fine("Library: " + url);
				}
			}

			classLoader = classLoader.getParent();
			LOGGER.fine("------------------------------------------------------------------------------");
		}

		final String classPath = System.getProperty("java.class.path");
		if (classPath != null && !classPath.isEmpty()) {
			for (final String classPathEntry : classPath.split(File.pathSeparator)) {
				LOGGER.fine("Classpath entry: " + classPathEntry);
			}
		}
	}
}
