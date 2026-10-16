package de.soderer.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.List;

/**
 * File filter accepting file names which match at least one of a list of wildcard patterns.
 * <p>
 * The wildcard "?" matches exactly one character, "*" matches any number of characters (also none).
 * Only the file name is matched, not the path.
 * </p>
 */
public class WildcardFilenameFilter implements FileFilter, FilenameFilter {
	private final String[] wildcardFilenamePatterns;
	private final boolean caseSensitive;

	/**
	 * Creates a case sensitive filter for a single pattern.
	 *
	 * @param wildcardFilenamePattern
	 *            the wildcard pattern, like "*.txt"
	 * @throws IllegalArgumentException
	 *             if the pattern is null
	 */
	public WildcardFilenameFilter(final String wildcardFilenamePattern) {
		this(wildcardFilenamePattern, true);
	}

	/**
	 * Creates a filter for a single pattern.
	 *
	 * @param wildcardFilenamePattern
	 *            the wildcard pattern, like "*.txt"
	 * @param caseSensitive
	 *            true to match case sensitive
	 * @throws IllegalArgumentException
	 *             if the pattern is null
	 */
	public WildcardFilenameFilter(final String wildcardFilenamePattern, final boolean caseSensitive) {
		if (wildcardFilenamePattern == null) {
			throw new IllegalArgumentException("The wildcardFilenamePattern must not be null");
		}
		wildcardFilenamePatterns = new String[] { wildcardFilenamePattern };
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Creates a case sensitive filter for multiple patterns.
	 *
	 * @param wildcardFilenamePatterns
	 *            the wildcard patterns, a file name must match at least one of them
	 * @throws IllegalArgumentException
	 *             if the pattern array is null
	 */
	public WildcardFilenameFilter(final String... wildcardFilenamePatterns) {
		this(wildcardFilenamePatterns, true);
	}

	/**
	 * Creates a filter for multiple patterns.
	 *
	 * @param wildcardFilenamePatterns
	 *            the wildcard patterns, a file name must match at least one of them
	 * @param caseSensitive
	 *            true to match case sensitive
	 * @throws IllegalArgumentException
	 *             if the pattern array is null
	 */
	public WildcardFilenameFilter(final String[] wildcardFilenamePatterns, final boolean caseSensitive) {
		if (wildcardFilenamePatterns == null) {
			throw new IllegalArgumentException("The wildcardFilenamePattern array must not be null");
		}
		this.wildcardFilenamePatterns = new String[wildcardFilenamePatterns.length];
		System.arraycopy(wildcardFilenamePatterns, 0, this.wildcardFilenamePatterns, 0, wildcardFilenamePatterns.length);
		this.caseSensitive = caseSensitive;
	}

	/**
	 * Creates a case sensitive filter for multiple patterns.
	 *
	 * @param wildcardFilenamePatterns
	 *            the wildcard patterns, a file name must match at least one of them
	 * @throws IllegalArgumentException
	 *             if the pattern list is null
	 */
	public WildcardFilenameFilter(final List<String> wildcardFilenamePatterns) {
		this(wildcardFilenamePatterns, true);
	}

	/**
	 * Creates a filter for multiple patterns.
	 *
	 * @param wildcardFilenamePatterns
	 *            the wildcard patterns, a file name must match at least one of them
	 * @param caseSensitive
	 *            true to match case sensitive
	 * @throws IllegalArgumentException
	 *             if the pattern list is null
	 */
	public WildcardFilenameFilter(final List<String> wildcardFilenamePatterns, final boolean caseSensitive) {
		if (wildcardFilenamePatterns == null) {
			throw new IllegalArgumentException("The wildcardFilenamePattern list must not be null");
		}
		this.wildcardFilenamePatterns = wildcardFilenamePatterns.toArray(new String[0]);
		this.caseSensitive = caseSensitive;
	}

	@Override
	public boolean accept(final File directory, final String name) {
		for (final String wildcardFilenamePattern : wildcardFilenamePatterns) {
			if (checkWildcardFilenamePattern(name, wildcardFilenamePattern, caseSensitive)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean accept(final File file) {
		final String name = file.getName();
		for (final String wildcardFilenamePattern : wildcardFilenamePatterns) {
			if (checkWildcardFilenamePattern(name, wildcardFilenamePattern, caseSensitive)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		final StringBuilder buffer = new StringBuilder();
		buffer.append(super.toString());
		buffer.append("(");
		if (wildcardFilenamePatterns != null) {
			for (int i = 0; i < wildcardFilenamePatterns.length; i++) {
				if (i > 0) {
					buffer.append(",");
				}
				buffer.append(wildcardFilenamePatterns[i]);
			}
		}
		buffer.append(")");
		return buffer.toString();
	}

	/**
	 * Checks whether a file name matches a wildcard pattern. When a later part of the pattern does not match, the
	 * last "*" is extended by one character and matching continues from there, so combinations like "*?" are handled
	 * correctly.
	 */
	private static boolean checkWildcardFilenamePattern(final String fileName, final String wildcardFilenamePattern, final boolean caseSensitive) {
		if (fileName == null && wildcardFilenamePattern == null) {
			return true;
		} else if (fileName == null || wildcardFilenamePattern == null) {
			return false;
		} else {
			int textIndex = 0;
			int patternIndex = 0;
			// Position of the last "*" in the pattern and of the text it was matched against, -1 if there was none yet
			int lastStarPatternIndex = -1;
			int lastStarTextIndex = 0;
			while (textIndex < fileName.length()) {
				if (patternIndex < wildcardFilenamePattern.length() && wildcardFilenamePattern.charAt(patternIndex) == '*') {
					lastStarPatternIndex = patternIndex;
					lastStarTextIndex = textIndex;
					patternIndex++;
				} else if (patternIndex < wildcardFilenamePattern.length()
						&& (wildcardFilenamePattern.charAt(patternIndex) == '?' || charactersMatch(fileName.charAt(textIndex), wildcardFilenamePattern.charAt(patternIndex), caseSensitive))) {
					patternIndex++;
					textIndex++;
				} else if (lastStarPatternIndex >= 0) {
					// Let the last "*" match one more character and retry the rest of the pattern
					patternIndex = lastStarPatternIndex + 1;
					lastStarTextIndex++;
					textIndex = lastStarTextIndex;
				} else {
					return false;
				}
			}
			// Remaining "*" match the empty rest of the text
			while (patternIndex < wildcardFilenamePattern.length() && wildcardFilenamePattern.charAt(patternIndex) == '*') {
				patternIndex++;
			}
			return patternIndex == wildcardFilenamePattern.length();
		}
	}

	/**
	 * Compares two characters, ignoring the case like {@link String#regionMatches(boolean, int, String, int, int)}.
	 */
	private static boolean charactersMatch(final char textChar, final char patternChar, final boolean caseSensitive) {
		if (textChar == patternChar) {
			return true;
		} else if (caseSensitive) {
			return false;
		} else {
			return Character.toUpperCase(textChar) == Character.toUpperCase(patternChar)
					|| Character.toLowerCase(textChar) == Character.toLowerCase(patternChar);
		}
	}
}
