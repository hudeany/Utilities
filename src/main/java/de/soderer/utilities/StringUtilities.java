package de.soderer.utilities;

/**
 * Null-safe string comparison helpers.
 */
public class StringUtilities {
	private StringUtilities() {
		// Utility class
	}

	/**
	 * Null-safe, case-sensitive comparison of two strings.
	 *
	 * @param string1
	 *            first string, may be null
	 * @param string2
	 *            second string, may be null
	 * @return true if both strings are null or contain the same characters
	 */
	public static boolean equals(final String string1, final String string2) {
		if (string1 == string2) {
			return true;
		} else if (string1 == null || string2 == null) {
			return false;
		} else if (string1.length() != string2.length()) {
			return false;
		} else {
			return string1.equals(string2);
		}
	}

	/**
	 * Null-safe, case-insensitive comparison of two strings.
	 * The comparison is independent of the default locale (see {@link String#equalsIgnoreCase(String)}).
	 *
	 * @param string1
	 *            first string, may be null
	 * @param string2
	 *            second string, may be null
	 * @return true if both strings are null or are equal ignoring case
	 */
	public static boolean equalsIgnoreCase(final String string1, final String string2) {
		if (string1 == string2) {
			return true;
		} else if (string1 == null || string2 == null) {
			return false;
		} else {
			// String.equalsIgnoreCase does not depend on the default locale (e.g. Turkish dotless i)
			return string1.equalsIgnoreCase(string2);
		}
	}
}
