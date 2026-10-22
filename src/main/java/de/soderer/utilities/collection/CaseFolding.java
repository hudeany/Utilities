package de.soderer.utilities.collection;

import java.util.Locale;

/**
 * Case folding used by the case-insensitive collections of this package.
 */
final class CaseFolding {
	private CaseFolding() {
		// Utility class with static methods only
	}

	/**
	 * Folds the given String so that two Strings fold to the same result exactly when
	 * {@link String#equalsIgnoreCase(String)} considers them equal.
	 * <p>
	 * Plain {@code toLowerCase(Locale.ROOT)} is not sufficient for that: e.g. the long s (U+017F) and the
	 * dotless i (U+0131) are equalsIgnoreCase to "s" and "I", but do not lowercase to "s" and "i". Like
	 * equalsIgnoreCase, each code point is mapped to {@code toLowerCase(toUpperCase(codePoint))}.
	 * For pure ASCII Strings the result is the same as {@code toLowerCase(Locale.ROOT)}.
	 *
	 * @param value String to fold, may be null
	 * @return folded String, or null if value is null
	 */
	static String fold(final String value) {
		if (value == null) {
			return null;
		}

		boolean isAscii = true;
		for (int i = 0; i < value.length(); i++) {
			if (value.charAt(i) >= 0x80) {
				isAscii = false;
				break;
			}
		}
		if (isAscii) {
			// Fast path: returns the same instance if there is nothing to change
			return value.toLowerCase(Locale.ROOT);
		}

		final StringBuilder folded = new StringBuilder(value.length());
		value.codePoints().forEach(codePoint -> folded.appendCodePoint(Character.toLowerCase(Character.toUpperCase(codePoint))));
		return folded.toString();
	}
}
