package de.soderer.utilities;

import java.io.Writer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DebugUtils {

	private static final transient Logger logger = LoggerFactory.getLogger(DebugUtils.class);

	public static final void printProperties(Properties properties, Writer writer) {
		Enumeration<?> elementsEnum = properties.keys();
		while(elementsEnum.hasMoreElements()) {
			String key = (String) elementsEnum.nextElement();
			String value = (String) properties.getProperty(key);
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
			for (int i = 0; i < interfaces.length; i++) {
				logger.debug("Classloader (Interface): " + interfaces[i]);
			}
			if (classLoader instanceof URLClassLoader) {
				URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
				URL[] urls = urlClassLoader.getURLs();
				for (int i = 0; i < urls.length; i++) {
					logger.debug("Library: " + urls[i].toString());
				}
			}
			classLoader = classLoader.getParent();
			logger.debug("------------------------------------------------------------------------------");
		}
	}

}
