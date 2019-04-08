package de.soderer.utilities;

import java.io.InputStream;

public class VersionInfo {
	private static Version APPLICATION_VERSION = null;

	public static Version getApplicationVersion() {
		if (APPLICATION_VERSION == null) {
			APPLICATION_VERSION = Version.getFirstFullVersionNumberFromText(getVersionInfoText());
		}
		return APPLICATION_VERSION;
	}

	public static String getVersionInfoText() {
		InputStream inputStream = null;
		try {
			inputStream = VersionInfo.class.getClassLoader().getResourceAsStream("VersionInfo.txt");
			return new String(Utilities.toByteArray(inputStream), "UTF-8");
		} catch (final Exception e) {
			return "VersionInfo not found";
		} finally {
			Utilities.closeQuietly(inputStream);
		}
	}
}
