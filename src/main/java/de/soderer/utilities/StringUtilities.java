package de.soderer.utilities;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

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
	 * Remove leading whitespace (regex \s) of a string.
	 *
	 * @param value string to trim, may be null
	 * @return string without leading whitespace, or null for null input
	 */
	public static String trimLeft(final String value) {
		// Null-safe like trim(String) (before, a NullPointerException was thrown)
		if (value == null) {
			return null;
		} else {
			return value.replaceAll("^\\s+", "");
		}
	}

	/**
	 * Remove trailing whitespace (regex \s) of a string.
	 *
	 * @param value string to trim, may be null
	 * @return string without trailing whitespace, or null for null input
	 */
	public static String trimRight(final String value) {
		// Null-safe like trim(String) (before, a NullPointerException was thrown)
		if (value == null) {
			return null;
		} else {
			return value.replaceAll("\\s+$", "");
		}
	}

	/**
	 * Null-safe {@link String#trim()}.
	 *
	 * @param value string to trim, may be null
	 * @return trimmed string, or null for null input
	 */
	public static String trim(final String value) {
		if (value == null) {
			return null;
		} else {
			return value.trim();
		}
	}

	/**
	 * Remove all occurrences of a character at the start and the end of a string.
	 *
	 * @param value string to trim, may be null
	 * @param trimChar character to remove
	 * @return trimmed string, or null for null input
	 */
	public static String trim(String value, final char trimChar) {
		while (value != null && value.startsWith(Character.toString(trimChar))) {
			value = value.substring(1);
		}

		while (value != null && value.endsWith(Character.toString(trimChar))) {
			value = value.substring(0, value.length() - 1);
		}

		return value;
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
	 * Find the first case-insensitive occurrence of a text, independent of the default locale.
	 *
	 * @param data string to search in, may be null
	 * @param part text to search for, may be null
	 * @return index of the first occurrence in data, 0 if both are the same object (or null), -1 if not found or only one is null
	 */
	public static int indexOfIgnoreCase(final String data, final String part) {
		if (data == part) {
			// both null or same object
			return 0;
		} else if (data == null || part == null) {
			// suffix is null but data is not or vice versa
			return -1;
		} else {
			// regionMatches is independent of the default locale and keeps the indexes of the original text
			// (toLowerCase() may change the text length, e.g. for 'İ', and depends on the default locale)
			for (int i = 0; i <= data.length() - part.length(); i++) {
				if (data.regionMatches(true, i, part, 0, part.length())) {
					return i;
				}
			}
			return -1;
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

	/**
	 * Shorten a string to a maximum length by cutting off its end and appending a cut sign.
	 *
	 * @param value string to shorten, may be null
	 * @param maxLength maximum length of the result including the cut sign; if it is less than the length of the cut sign, the end is cut off without cut sign
	 * @param cutSign text appended if the string was shortened, null for none
	 * @return shortened string, unchanged if it is not longer than maxLength
	 */
	public static String shortenStringToMaxLengthCutRight(final String value, final int maxLength, final String cutSign) {
		if (value != null && value.length() > maxLength) {
			// Use the real length of the cut sign (before, always 4 characters were reserved, which only fits the default " ...")
			final String cutSignToUse = cutSign == null ? "" : cutSign;
			if (maxLength < cutSignToUse.length()) {
				// The cut sign does not fit (before, the result was the complete cut sign and so longer than maxLength)
				return value.substring(0, Math.max(0, maxLength));
			} else {
				return value.substring(0, maxLength - cutSignToUse.length()) + cutSignToUse;
			}
		} else {
			return value;
		}
	}

	/**
	 * Shorten a string to a maximum length by cutting off its start and prepending "... ".
	 *
	 * @param value string to shorten, may be null
	 * @param maxLength maximum length of the result, below 4 the start is cut off without cut sign
	 * @return shortened string, unchanged if it is not longer than maxLength
	 */
	public static String shortenStringToMaxLengthCutLeft(final String value, final int maxLength) {
		if (value != null && value.length() > maxLength && maxLength < 4) {
			// The cut sign "... " does not fit (before, a StringIndexOutOfBoundsException was thrown)
			return value.substring(value.length() - Math.max(0, maxLength));
		} else if (value != null && value.length() > maxLength) {
			return "... " + value.substring((value.length() - maxLength) + 4);
		} else {
			return value;
		}
	}

	/**
	 * Shorten a string to a maximum length by cutting off its end and appending " ...".
	 *
	 * @param value string to shorten, may be null
	 * @param maxLength maximum length of the result including the cut sign
	 * @return shortened string, unchanged if it is not longer than maxLength
	 */
	public static String shortenStringToMaxLengthCutRight(final String value, final int maxLength) {
		return shortenStringToMaxLengthCutRight(value, maxLength, " ...");
	}

	/**
	 * Null-safe substring that tolerates a start index behind the end of the text.
	 *
	 * @param text text, may be null
	 * @param startIndex start index, must not be negative
	 * @return substring, "" if startIndex is greater than the text length, or null for null text
	 */
	public static String substring(final String text, final int startIndex) {
		if (text == null) {
			return null;
		} else {
			if (text.length() < startIndex) {
				return "";
			} else {
				return text.substring(startIndex);
			}
		}
	}

	/**
	 * Null-safe substring that tolerates indexes behind the end of the text.
	 *
	 * @param text text, may be null
	 * @param startIndex start index (inclusive), must not be negative
	 * @param endIndex end index (exclusive), cut to the text length
	 * @return substring, "" if startIndex is greater than the text length, or null for null text
	 */
	public static String substring(final String text, final int startIndex, final int endIndex) {
		if (text == null) {
			return null;
		} else {
			if (text.length() < startIndex) {
				return "";
			} else if (text.length() < endIndex) {
				return text.substring(startIndex);
			} else {
				return text.substring(startIndex, endIndex);
			}
		}
	}

	/**
	 * Null-safe check whether a text contains another text.
	 *
	 * @param text text to search in, may be null
	 * @param searchText text to search for, must not be null
	 * @return true if contained, false for null text
	 */
	public static boolean contains(final String text, final String searchText) {
		if (text == null) {
			return false;
		} else {
			return text.contains(searchText);
		}
	}

	/**
	 * Join the string representations of array items. Null items are shown as empty strings.
	 *
	 * @param array items to join, may be null
	 * @param glue separator between the items, null for none
	 * @return joined text, or null for a null array
	 */
	public static String join(final Object[] array, String glue) {
		if (array == null) {
			return null;
		} else if (array.length == 0) {
			return "";
		} else {
			if (glue == null) {
				glue = "";
			}

			final StringBuilder returnValue = new StringBuilder();
			boolean isFirst = true;
			for (Object object : array) {
				if (!isFirst) {
					returnValue.append(glue);
				}
				if (object == null) {
					object = "";
				}
				returnValue.append(object.toString());
				isFirst = false;
			}
			return returnValue.toString();
		}
	}

	/**
	 * Join characters with a separator.
	 *
	 * @param array characters to join, may be null
	 * @param glue separator between the characters, null for none
	 * @return joined text, or null for a null array
	 */
	public static String join(final char[] array, String glue) {
		if (array == null) {
			return null;
		} else if (array.length == 0) {
			return "";
		} else {
			if (glue == null) {
				glue = "";
			}

			final StringBuilder returnValue = new StringBuilder();
			boolean isFirst = true;
			for (final char nextChar : array) {
				if (!isFirst) {
					returnValue.append(glue);
				}
				returnValue.append(nextChar);
				isFirst = false;
			}
			return returnValue.toString();
		}
	}

	/**
	 * Join the string representations of items. Null items are shown as empty strings.
	 *
	 * @param iterableObject items to join, may be null
	 * @param glue separator between the items, null for none
	 * @return joined text, or null for null items
	 */
	public static String join(final Iterable<?> iterableObject, String glue) {
		if (iterableObject == null) {
			return null;
		} else {
			if (glue == null) {
				glue = "";
			}

			final StringBuilder returnValue = new StringBuilder();
			boolean isFirst = true;
			for (Object object : iterableObject) {
				if (!isFirst) {
					returnValue.append(glue);
				}
				if (object == null) {
					object = "";
				}
				returnValue.append(object.toString());
				isFirst = false;
			}
			return returnValue.toString();
		}
	}

	/**
	 * Join all non-blank strings of a list.
	 *
	 * @param listOfStrings strings to join, must not be null
	 * @param glue separator between the strings, null for none
	 * @return joined text
	 */
	public static String joinNotBlank(final List<String> listOfStrings, final String glue) {
		return join(listOfStrings.stream().filter(x -> StringUtilities.isNotBlank(x)).collect(Collectors.toList()), glue);
	}

	/**
	 * Null-safe conversion to upper case, independent of the default locale.
	 * Locale.ROOT is used, because the default locale changes the result, e.g. "i" becomes the dotted "\u0130" in
	 * Turkish, which breaks comparisons of keywords, identifiers and file names.
	 *
	 * @param value text, may be null
	 * @return upper case text, or null for null input
	 */
	public static String upperCase(final String value) {
		if (value == null) {
			return null;
		} else {
			return value.toUpperCase(Locale.ROOT);
		}
	}

	/**
	 * Check a simple name string (only letters A-Z and a-z, digits, '_' and '-')
	 *
	 * @param value
	 *            name to check
	 * @return true if the name is valid, false for null or an empty name
	 */
	public static boolean checkForValidUserName(final String value) {
		return value != null && value.matches("[A-Za-z0-9_-]+");
	}
}
