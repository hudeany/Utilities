package de.soderer.utilities.jarinjarloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import de.soderer.utilities.SystemUtilities;

public class JarInJarLoader {
	private static class ManifestInfo {
		String rsrcMainClass;
		String[] rsrcClassPath;
	}

	public static void main(String[] args) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException, IOException {
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

		ManifestInfo mi = getManifestInfo();
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		URL.setURLStreamHandlerFactory(new JarInJarURLStreamHandlerFactory(cl));
		URL[] rsrcUrls = new URL[mi.rsrcClassPath.length];
		for (int i = 0; i < mi.rsrcClassPath.length; i++) {
			String rsrcPath = mi.rsrcClassPath[i];
			if (rsrcPath.endsWith("/")) {
				rsrcUrls[i] = new URL("rsrc:" + rsrcPath);
			} else {
				rsrcUrls[i] = new URL("jar:rsrc:" + rsrcPath + "!/");
			}
		}
		ClassLoader jceClassLoader = new URLClassLoader(rsrcUrls, null);
		Thread.currentThread().setContextClassLoader(jceClassLoader);
		Class<?> c = Class.forName(mi.rsrcMainClass, true, jceClassLoader);
		Method main = c.getMethod("main", new Class[] { args.getClass() });
		main.invoke((Object) null, new Object[] { args });
	}

	private static ManifestInfo getManifestInfo() throws IOException {
		Enumeration<URL> resEnum = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
		while (resEnum.hasMoreElements()) {
			URL url = resEnum.nextElement();
			try (InputStream is = url.openStream()) {
				if (is != null) {
					ManifestInfo result = new ManifestInfo();
					Manifest manifest = new Manifest(is);
					Attributes mainAttribs = manifest.getMainAttributes();
					result.rsrcMainClass = mainAttribs.getValue("Rsrc-Main-Class");
					String rsrcCP = mainAttribs.getValue("Rsrc-Class-Path");
					if (rsrcCP == null) {
						rsrcCP = "";
					}
					result.rsrcClassPath = rsrcCP.split(" ");
					if (result.rsrcMainClass != null && !result.rsrcMainClass.trim().equals("")) {
						return result;
					}
				}
			} catch (Exception e) {
				// Do nothing
			}
		}
		System.err.println("Missing attributes for RsrcLoader in Manifest (Rsrc-Main-Class, Rsrc-Class-Path)");
		return null;
	}
}
