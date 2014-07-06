package de.soderer.utilities;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

// TODO: Auto-generated Javadoc
/**
 * The Class Version.
 */
public class Version {
	
	/** The versionpattern. */
	private static Pattern VERSIONPATTERN = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)$");
	
	/** The major version number. */
	private int majorVersionNumber;
	
	/** The minor version number. */
	private int minorVersionNumber;
	
	/** The micro version number. */
	private int microVersionNumber;
	
	/**
	 * The Constructor.
	 *
	 * @param majorVersionNumber the major version number
	 * @param minorVersionNumber the minor version number
	 * @param microVersionNumber the micro version number
	 */
	public Version(int majorVersionNumber, int minorVersionNumber, int microVersionNumber) {
		this.majorVersionNumber = majorVersionNumber;
		this.minorVersionNumber = minorVersionNumber;
		this.microVersionNumber = microVersionNumber;
	}
	
	/**
	 * The Constructor.
	 *
	 * @param versionString the version string
	 * @throws Exception the exception
	 */
	public Version(String versionString) throws Exception {
		Matcher matcher = VERSIONPATTERN.matcher(versionString);
		if (matcher.find()) {
			majorVersionNumber = Integer.parseInt(matcher.group(1));
			minorVersionNumber = Integer.parseInt(matcher.group(2));
			microVersionNumber = Integer.parseInt(matcher.group(3));
		} else {
			throw new Exception("Invalid version number");
		}
	}

	/**
	 * Gets the major version number.
	 *
	 * @return the major version number
	 */
	public int getMajorVersionNumber() {
		return majorVersionNumber;
	}

	/**
	 * Gets the minor version number.
	 *
	 * @return the minor version number
	 */
	public int getMinorVersionNumber() {
		return minorVersionNumber;
	}

	/**
	 * Gets the micro version number.
	 *
	 * @return the micro version number
	 */
	public int getMicroVersionNumber() {
		return microVersionNumber;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return new StringBuilder(majorVersionNumber).append(".").append(minorVersionNumber).append(".").append(microVersionNumber).toString();
	}
	
	/**
	 * Gets the first full version from a text.
	 *
	 * @param data the data
	 * @return the first full version from a text
	 */
	public static Version getFirstFullVersionNumberFromText(String data) {
		if (StringUtils.isBlank(data)) {
			return null;
		} else {
			Pattern versionPattern = Pattern.compile("\\d+\\.\\d+\\.\\d+");
			Matcher matcher = versionPattern.matcher(data);
			if (matcher.find()) {
				try {
					return new Version(matcher.group());
				} catch (Exception e) {
					return null;
				}
			} else {
				return null;
			}
		}
	}
}
