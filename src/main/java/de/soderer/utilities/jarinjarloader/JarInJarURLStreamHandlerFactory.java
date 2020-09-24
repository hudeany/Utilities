package de.soderer.utilities.jarinjarloader;

import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;

public class JarInJarURLStreamHandlerFactory implements URLStreamHandlerFactory {
	private final ClassLoader classLoader;
	private URLStreamHandlerFactory chainFac;

	public JarInJarURLStreamHandlerFactory(final ClassLoader cl) {
		classLoader = cl;
	}

	@Override
	public URLStreamHandler createURLStreamHandler(final String protocol) {
		if ("rsrc".equals(protocol))
			return new JarInJarURLStreamHandler(classLoader);
		if (chainFac != null)
			return chainFac.createURLStreamHandler(protocol);
		return null;
	}

	public void setURLStreamHandlerFactory(final URLStreamHandlerFactory fac) {
		chainFac = fac;
	}
}
