package de.soderer.utilities;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugUtils {

	private static final transient Logger logger = LoggerFactory.getLogger(DebugUtils.class);

	public static final void printProperties(Properties properties) {
		Enumeration<?> elementsEnum = properties.keys();
		while (elementsEnum.hasMoreElements()) {
			String key = (String) elementsEnum.nextElement();
			String value = properties.getProperty(key);
			logger.debug(key + " = '" + value + "'");
		}
	}

	public static final void printClasspath(Class<?> clazz) {
		ClassLoader classLoader = clazz.getClassLoader();
		while (classLoader != null) {
			Class<?> clClass = classLoader.getClass();
			while (clClass != null) {
				logger.debug("Classloader (Class): " + clClass.getName());
				clClass = clClass.getSuperclass();
			}
			Class<?>[] interfaces = classLoader.getClass().getInterfaces();
			for (Class<?> interface1 : interfaces) {
				logger.debug("Classloader (Interface): " + interface1);
			}
			if (classLoader instanceof URLClassLoader) {
				URL[] urls = ((URLClassLoader) classLoader).getURLs();
				for (URL url : urls) {
					logger.debug("Library: " + url.toString());
				}
			}
			classLoader = classLoader.getParent();
			logger.debug("------------------------------------------------------------------------------");
		}
	}

}
