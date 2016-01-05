package de.soderer.utilities.jarinjarloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import de.soderer.utilities.SystemUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.ZipUtilities;

public class JarInJarLoader {
	private static class ManifestInfo {
		String rsrcMainClass;
		String[] rsrcClassPath;
	}

	public static void main(String[] args) throws Exception {
		try {
			String currentJarUrlPath = JarInJarLoader.class.getResource(JarInJarLoader.class.getSimpleName() + ".class").toString();
			if (currentJarUrlPath != null && currentJarUrlPath.length() > 0) {
				String jarFilePath = currentJarUrlPath.substring(0, currentJarUrlPath.lastIndexOf("!")).replaceFirst("jar:file:", "");
				File jarFile = new File(jarFilePath);
				if (jarFile.exists()) {
					System.getProperties().put(SystemUtilities.SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR, jarFile.getAbsolutePath());
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		ManifestInfo manifestInfo = getManifestInfo();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL.setURLStreamHandlerFactory(new JarInJarURLStreamHandlerFactory(cl));
		URL[] rsrcUrls = new URL[manifestInfo.rsrcClassPath.length];
		for (int i = 0; i < manifestInfo.rsrcClassPath.length; i++) {
			String rsrcPath = manifestInfo.rsrcClassPath[i];
			if (rsrcPath.endsWith("/")) {
				rsrcUrls[i] = new URL("rsrc:" + rsrcPath);
			} else {
				rsrcUrls[i] = new URL("jar:rsrc:" + rsrcPath + "!/");
			}
		}
		ClassLoader jceClassLoader = new URLClassLoader(rsrcUrls, null);
		Thread.currentThread().setContextClassLoader(jceClassLoader);
		Class<?> mainClass = Class.forName(manifestInfo.rsrcMainClass, true, jceClassLoader);
		Method main = mainClass.getMethod("main", new Class[] { args.getClass() });
		main.invoke((Object) null, new Object[] { args });
	}

	private static ManifestInfo getManifestInfo() throws IOException {
		Enumeration<URL> manifestFileUrls = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
		while (manifestFileUrls.hasMoreElements()) {
			URL manifestFileUrl = manifestFileUrls.nextElement();
			try (InputStream is = manifestFileUrl.openStream()) {
				if (is != null) {
					ManifestInfo result = new ManifestInfo();
					Attributes mainAttribs = new Manifest(is).getMainAttributes();
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
							for (String entryName : ZipUtilities.getZipFileEntries(new File(jarFilePath))) {
								if (entryName.toLowerCase().endsWith(".jar")) {
									rsrcClassPath += " " + entryName;
								}
							}
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					result.rsrcClassPath = rsrcClassPath.split(" ");
					if (Utilities.isNotBlank(result.rsrcMainClass)) {
						return result;
					}
				}
			} catch (Exception e) {
				// Skip invalid manifest file
			}
		}
		System.err.println("Missing attributes for JarInJarLoader in Manifest (Rsrc-Main-Class, Rsrc-Class-Path)");
		return null;
	}
}
