package de.soderer.utilities.jarinjarloader;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;

import de.soderer.utilities.http.HttpUtilities;

public class JarInJarURLConnection extends URLConnection {

	private final ClassLoader classLoader;

	public JarInJarURLConnection(final URL url, final ClassLoader classLoader) {
		super(url);
		this.classLoader= classLoader;
	}

	@Override
	public void connect() throws IOException {
		// nothing to do
	}

	@Override
	public InputStream getInputStream() throws IOException {
		final String file= HttpUtilities.urlDecode(url.getFile(), StandardCharsets.UTF_8);
		final InputStream result= classLoader.getResourceAsStream(file);
		if (result == null) {
			throw new MalformedURLException("Could not open InputStream for URL '" + url + "'");
		} else {
			return result;
		}
	}
}
