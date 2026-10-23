package de.soderer.utilities;

/**
 * Null-safe basic operations on single strings: checks for empty and blank values, comparisons and repetition.
 */
public class StringUtilities {
	private StringUtilities() {
		// Utility class
	}

	/**
	 * Check whether a string is null or empty.
	 *
	 * @param value string to check
	 * @return true for null or ""
	 */
	public static boolean isEmpty(final String value) {
		return value == null || value.length() == 0;
	}

	/**
	 * Check whether a string has at least one character.
	 *
	 * @param value string to check
	 * @return false for null or "", otherwise true
	 */
	public static boolean isNotEmpty(final String value) {
		return !isEmpty(value);
	}

	/**
	 * Check whether a string is null, empty or contains only whitespace (see {@link Character#isWhitespace(int)}, like
	 * {@link String#isBlank()} and {@link #isBlank(char[])}).
	 *
	 * @param value string to check
	 * @return true for null, empty or whitespace only
	 */
	public static boolean isBlank(final String value) {
		return value == null || value.isBlank();
	}

	/**
	 * Check whether a string contains at least one non-whitespace character (see {@link #isBlank(String)}).
	 *
	 * @param value string to check
	 * @return true if the string is not blank
	 */
	public static boolean isNotBlank(final String value) {
		return !isBlank(value);
	}

	/**
	 * Check whether a character array is null or empty.
	 *
	 * @param value array to check
	 * @return true for null or an empty array
	 */
	public static boolean isEmpty(final char[] value) {
		return value == null || value.length == 0;
	}

	/**
	 * Check whether a character array has at least one character.
	 *
	 * @param value array to check
	 * @return false for null or an empty array, otherwise true
	 */
	public static boolean isNotEmpty(final char[] value) {
		return !isEmpty(value);
	}

	/**
	 * Check whether a character array is null, empty or contains only whitespace (see {@link Character#isWhitespace(char)}).
	 *
	 * @param value array to check
	 * @return true for null, empty or whitespace only
	 */
	public static boolean isBlank(final char[] value) {
		if (value == null || value.length == 0) {
			return true;
		} else {
			for (final char character : value) {
				if (!Character.isWhitespace(character)) {
					return false;
				}
			}
			return true;
		}
	}

	/**
	 * Check whether a character array contains at least one non-whitespace character.
	 *
	 * @param value array to check
	 * @return true if the array is not blank
	 */
	public static boolean isNotBlank(final char[] value) {
		return !isBlank(value);
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

	/**
	 * Check if a string starts with a stringpart, ignoring case.
	 *
	 * @param str text, may be null
	 * @param prefix prefix to look for, may be null
	 * @return true if str starts with prefix or both are null
	 */
	public static boolean startsWithIgnoreCase(final String str, final String prefix) {
		if (str == null || prefix == null) {
			return (str == null && prefix == null);
		} else if (prefix.length() > str.length()) {
			return false;
		} else {
			return str.regionMatches(true, 0, prefix, 0, prefix.length());
		}
	}

	/**
	 * Check if a string ends with a stringpart, ignoring case.
	 *
	 * @param str text, may be null
	 * @param suffix suffix to look for, may be null
	 * @return true if str ends with suffix or both are null
	 */
	public static boolean endsWithIgnoreCase(final String str, final String suffix) {
		if (str == null || suffix == null) {
			return (str == null && suffix == null);
		} else if (suffix.length() > str.length()) {
			return false;
		} else {
			final int strOffset = str.length() - suffix.length();
			return str.regionMatches(true, strOffset, suffix, 0, suffix.length());
		}
	}

	/**
	 * Build a string by repeating a character.
	 *
	 * @param valueChar character to repeat
	 * @param count number of repetitions, 0 or less for an empty string
	 * @return repeated characters
	 */
	public static String repeat(final char valueChar, final int count) {
		return repeat(Character.toString(valueChar), count, null);
	}

	/**
	 * Build a string by repeating a text.
	 *
	 * @param value text to repeat, may be null
	 * @param count number of repetitions, 0 or less for an empty string
	 * @return repeated text, or null for null input
	 */
	public static String repeat(final String value, final int count) {
		return repeat(value, count, null);
	}

	/**
	 * Build a string by repeating a text with a separator between the repetitions.
	 *
	 * @param value text to repeat, may be null
	 * @param count number of repetitions, 0 or less for an empty string
	 * @param separatorString separator between the repetitions, null for none
	 * @return repeated text, or null for null input
	 */
	public static String repeat(final String value, final int count, final String separatorString) {
		if (value == null) {
			return null;
		} else if (value.length() == 0 || count == 0) {
			return "";
		} else {
			final StringBuilder returnValue = new StringBuilder();
			for (int i = 0; i < count; i++) {
				if (separatorString != null && returnValue.length() > 0) {
					returnValue.append(separatorString);
				}
				returnValue.append(value);
			}
			return returnValue.toString();
		}
	}

	/**
	 * Trim a string to a maximum number of characters by replacing its middle part by a cutoff sign.
	 * Surrogate pairs (like emojis) are never split, so the result may be shorter than maxLength by up to two characters.
	 *
	 * @param inputString text, may be null
	 * @param maxLength maximum length of the result; if it is less than the length of the cutoff sign, the end is cut off without cutoff sign
	 * @param cutoffSign text replacing the removed middle part, e.g. "...", null for none
	 * @return shortened text, unchanged if it is not longer than maxLength
	 */
	public static String trimStringToMaximumLength(final String inputString, final int maxLength, String cutoffSign) {
		if (inputString == null || inputString.length() <= maxLength) {
			return inputString;
		}
		if (cutoffSign == null) {
			cutoffSign = "";
		}
		if (maxLength < cutoffSign.length()) {
			// The cutoff sign does not fit, so the end is cut off without it (before, a StringIndexOutOfBoundsException was thrown)
			int endIndex = Math.max(0, maxLength);
			if (endIndex > 0 && Character.isHighSurrogate(inputString.charAt(endIndex - 1))) {
				endIndex--;
			}
			return inputString.substring(0, endIndex);
		}
		int takeFirstLength = (maxLength - cutoffSign.length()) / 2;
		final int takeLastLength = maxLength - cutoffSign.length() - takeFirstLength;
		int lastPartStartIndex = inputString.length() - takeLastLength;
		// Do not split a surrogate pair (like an emoji) at the cut positions
		if (takeFirstLength > 0 && Character.isHighSurrogate(inputString.charAt(takeFirstLength - 1))) {
			takeFirstLength--;
		}
		if (lastPartStartIndex < inputString.length() && Character.isLowSurrogate(inputString.charAt(lastPartStartIndex))) {
			lastPartStartIndex++;
		}
		return inputString.substring(0, takeFirstLength) + cutoffSign + inputString.substring(lastPartStartIndex);
	}
}
