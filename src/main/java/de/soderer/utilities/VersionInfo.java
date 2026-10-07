package de.soderer.utilities;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Access to the version information of the running application.
 *
 * <p>The version is read from the classpath resources "build.version" (first line), "VersionInfo.txt" or "version.txt".
 * In "VersionInfo.txt" the placeholder {@code ${build.version}} is replaced by the content of "build.version".</p>
 */
public class VersionInfo {
	/** Cached application version, volatile for safe publication between threads */
	private static volatile Version APPLICATION_VERSION = null;

	private VersionInfo() {
		// Utility class
	}

	/**
	 * Get the version of the running application.
	 * The result is cached after the first call.
	 *
	 * @return application version, or version 0.0.0 if no version information is available
	 */
	public static Version getApplicationVersion() {
		Version applicationVersion = APPLICATION_VERSION;
		if (applicationVersion == null) {
			final String buildVersionString = readBuildVersion();
			if (Utilities.isNotBlank(buildVersionString)) {
				try {
					applicationVersion = new Version(buildVersionString);
				} catch (@SuppressWarnings("unused") final Exception e) {
					// Try other sources
				}
			}

			if (applicationVersion == null) {
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
					// The version found here was returned before without being cached, and null was returned if no version was found
					applicationVersion = Version.getFirstFullVersionNumberFromText(versionInfoText);
				} catch (@SuppressWarnings("unused") final Exception e) {
					// Use fallback
				}
			}

			if (applicationVersion == null) {
				applicationVersion = new Version(0, 0, 0);
			}
			APPLICATION_VERSION = applicationVersion;
		}
		return applicationVersion;
	}

	/**
	 * Get the full version information text of the running application from the classpath resource "VersionInfo.txt"
	 * (or "version.txt" as fallback), with {@code ${build.version}} replaced by the build version.
	 *
	 * @return version information text, or an error text like "VersionInfo not found"
	 */
	public static String getVersionInfoText() {
		final String buildVersionString = readBuildVersion();

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

	/**
	 * Read the first line of the classpath resource "build.version".
	 *
	 * @return trimmed first line (without a trailing CR of Windows linebreaks), or null if the resource is missing or unreadable
	 */
	private static String readBuildVersion() {
		try (InputStream buildVersionInputStream = VersionInfo.class.getClassLoader().getResourceAsStream("build.version")) {
			if (buildVersionInputStream != null) {
				final String buildVersionString = IoUtilities.toString(buildVersionInputStream, StandardCharsets.UTF_8);
				if (Utilities.isNotBlank(buildVersionString)) {
					return buildVersionString.split("\n")[0].trim();
				} else {
					return buildVersionString;
				}
			} else {
				return null;
			}
		} catch (@SuppressWarnings("unused") final Exception e) {
			return null;
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
