package de.soderer.utilities.jarinjarloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class JarInJarLoader {
	public static final String SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR = "process.jar";

	private static class ManifestInfo {
		String rsrcMainClass;
		String[] rsrcClassPath;
	}

	public static void main(final String[] args) throws Exception {
		// Fill an environment variable with the path of the executed jar file
		try {
			final String currentJarUrlPath = JarInJarLoader.class.getResource(JarInJarLoader.class.getSimpleName() + ".class").toString();
			if (currentJarUrlPath != null && currentJarUrlPath.length() > 0) {
				final String jarFilePath = currentJarUrlPath.substring(0, currentJarUrlPath.lastIndexOf("!")).replaceFirst("jar:file:", "");
				final File jarFile = new File(jarFilePath);
				if (jarFile.exists()) {
					System.getProperties().put(SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR, jarFile.getAbsolutePath());
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}

		final ManifestInfo manifestInfo = getManifestInfo();
		final ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL.setURLStreamHandlerFactory(new JarInJarURLStreamHandlerFactory(cl));
		final URL[] rsrcUrls = new URL[manifestInfo.rsrcClassPath.length];
		for (int i = 0; i < manifestInfo.rsrcClassPath.length; i++) {
			final String rsrcPath = manifestInfo.rsrcClassPath[i];
			if (rsrcPath.endsWith("/")) {
				rsrcUrls[i] = new URL("rsrc:" + rsrcPath);
			} else {
				rsrcUrls[i] = new URL("jar:rsrc:" + rsrcPath + "!/");
			}
		}
		try (URLClassLoader jceClassLoader = new URLClassLoader(rsrcUrls, getParentClassLoader())) {
			Thread.currentThread().setContextClassLoader(jceClassLoader);
			final Class<?> mainClass = Class.forName(manifestInfo.rsrcMainClass, true, jceClassLoader);
			final Method main = mainClass.getMethod("main", new Class[] { args.getClass() });
			main.invoke((Object) null, new Object[] { args });
		}
	}

	private static ClassLoader getParentClassLoader() throws InvocationTargetException, IllegalAccessException {
		try {
			// We use reflection here because the method ClassLoader.getPlatformClassLoader() is only present starting from Java 9
			final Method platformClassLoader = ClassLoader.class.getMethod("getPlatformClassLoader", (Class[]) null);
			return (ClassLoader) platformClassLoader.invoke(null, (Object[]) null);
		} catch (@SuppressWarnings("unused") final NoSuchMethodException e) {
			// This is a safe value to be used on Java 8 and previous versions
			return null;
		}
	}

	private static ManifestInfo getManifestInfo() throws IOException {
		final Enumeration<URL> manifestFileUrls = Thread.currentThread().getContextClassLoader().getResources(JarFile.MANIFEST_NAME);
		while (manifestFileUrls.hasMoreElements()) {
			final URL manifestFileUrl = manifestFileUrls.nextElement();
			try (InputStream is = manifestFileUrl.openStream()) {
				if (is != null) {
					final ManifestInfo result = new ManifestInfo();
					final Attributes mainAttribs = new Manifest(is).getMainAttributes();
					result.rsrcMainClass = mainAttribs.getValue("Rsrc-Main-Class");
					String rsrcClassPath = mainAttribs.getValue("Rsrc-Class-Path");
					if (rsrcClassPath == null) {
						// find all jar files included in the jar and add them to the classpath
						rsrcClassPath = "./";
						String jarFilePath = manifestFileUrl.getFile().substring(0, manifestFileUrl.getPath().indexOf("!/META-INF/MANIFEST.MF"));
						if (jarFilePath.startsWith("file:")) {
							jarFilePath = jarFilePath.substring(5);
						}
						try {
							for (final String entryName : getZipFileEntries(new File(jarFilePath))) {
								if (entryName.toLowerCase().endsWith(".jar")) {
									rsrcClassPath += " " + entryName;
								}
							}
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
					result.rsrcClassPath = rsrcClassPath.split(" ");
					if (isNotBlank(result.rsrcMainClass)) {
						return result;
					}
				}
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Skip invalid manifest file
			}
		}
		System.err.println("Missing attributes for JarInJarLoader in Manifest (Rsrc-Main-Class, Rsrc-Class-Path)");
		return null;
	}

	private static boolean isBlank(final String value) {
		return value == null || value.length() == 0 || value.trim().length() == 0;
	}

	private static boolean isNotBlank(final String value) {
		return !isBlank(value);
	}

	private static String[] getZipFileEntries(final File file) throws ZipException, IOException {
		try (ZipFile zipFile = new ZipFile(file)) {
			final Enumeration<? extends ZipEntry> entries = zipFile.entries();
			final List<String> entryList = new ArrayList<>();
			while (entries.hasMoreElements()) {
				final ZipEntry entry = entries.nextElement();
				entryList.add(entry.getName());
			}
			return entryList.toArray(new String[0]);
		}
	}
}
