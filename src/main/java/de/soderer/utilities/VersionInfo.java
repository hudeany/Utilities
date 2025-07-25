package de.soderer.utilities;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class VersionInfo {
	private static Version APPLICATION_VERSION = null;

	public static Version getApplicationVersion() {
		if (APPLICATION_VERSION == null) {
			String buildVersionString = null;
			try (InputStream buildVersionInputStream = VersionInfo.class.getClassLoader().getResourceAsStream("build.version")) {
				if (buildVersionInputStream != null) {
					buildVersionString = IoUtilities.toString(buildVersionInputStream, StandardCharsets.UTF_8);
					if (Utilities.isNotBlank(buildVersionString)) {
						final String[] buildVersionLines = buildVersionString.split("\n");
						buildVersionString = buildVersionLines[0];
						if (Utilities.isNotBlank(buildVersionString)) {
							APPLICATION_VERSION = new Version(buildVersionString);
						}
					}
				}
			} catch (@SuppressWarnings("unused") final Exception e) {
				// do nothing
			}

			if (APPLICATION_VERSION == null) {
				try (InputStream inputStream = VersionInfo.class.getClassLoader().getResourceAsStream("VersionInfo.txt")) {
					String versionInfoText;
					if (inputStream == null) {
						versionInfoText = getVersionText();
					} else {
						versionInfoText = new String(IoUtilities.toByteArray(inputStream), StandardCharsets.UTF_8);
					}
					if (buildVersionString != null && versionInfoText != null) {
						versionInfoText = versionInfoText.replace("${build.version}", buildVersionString);
					}
					return Version.getFirstFullVersionNumberFromText(versionInfoText);
				} catch (@SuppressWarnings("unused") final Exception e) {
					APPLICATION_VERSION = new Version(0, 0, 0);
				}
			}
		}
		return APPLICATION_VERSION;
	}

	public static String getVersionInfoText() {
		String buildVersionString = null;
		try (InputStream buildVersionInputStream = VersionInfo.class.getClassLoader().getResourceAsStream("build.version")) {
			if (buildVersionInputStream != null) {
				buildVersionString = IoUtilities.toString(buildVersionInputStream, StandardCharsets.UTF_8);
				if (Utilities.isNotBlank(buildVersionString)) {
					final String[] buildVersionLines = buildVersionString.split("\n");
					buildVersionString = buildVersionLines[0];
				}
			}
		} catch (@SuppressWarnings("unused") final Exception e) {
			return "build.version not found";
		}

		try (InputStream inputStream = VersionInfo.class.getClassLoader().getResourceAsStream("VersionInfo.txt")) {
			String versionInfoText;
			if (inputStream == null) {
				versionInfoText = getVersionText();
			} else {
				versionInfoText = new String(IoUtilities.toByteArray(inputStream), StandardCharsets.UTF_8);
			}
			if (buildVersionString != null && versionInfoText != null) {
				versionInfoText = versionInfoText.replace("${build.version}", buildVersionString);
			}
			return versionInfoText;
		} catch (@SuppressWarnings("unused") final Exception e) {
			return "VersionInfo not found";
		}
	}

	private static String getVersionText() {
		try (InputStream inputStream = VersionInfo.class.getClassLoader().getResourceAsStream("version.txt")) {
			return new String(IoUtilities.toByteArray(inputStream), StandardCharsets.UTF_8);
		} catch (@SuppressWarnings("unused") final Exception e) {
			return "VersionInfo not found";
		}
	}
}
