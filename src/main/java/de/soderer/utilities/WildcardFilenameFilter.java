package de.soderer.utilities;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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

	private static boolean checkWildcardFilenamePattern(final String fileName, final String wildcardFilenamePattern, final boolean caseSensitive) {
		if (fileName == null && wildcardFilenamePattern == null) {
			return true;
		} else if (fileName == null || wildcardFilenamePattern == null) {
			return false;
		} else {
			final String[] wildcardFilenamePatternParts = splitOnTokens(wildcardFilenamePattern);
			boolean anyChars = false;
			int textIndex = 0;
			int wildcardFilenamePatternPartsIndex = 0;
			final Deque<int[]> backtrack = new ArrayDeque<>(wildcardFilenamePatternParts.length);

			do {
				if (!backtrack.isEmpty()) {
					final int[] array = backtrack.pop();
					wildcardFilenamePatternPartsIndex = array[0];
					textIndex = array[1];
					anyChars = true;
				}

				while (wildcardFilenamePatternPartsIndex < wildcardFilenamePatternParts.length) {
					if (wildcardFilenamePatternParts[wildcardFilenamePatternPartsIndex].equals("?")) {
						textIndex++;
						if (textIndex > fileName.length()) {
							break;
						}
						anyChars = false;
					} else if (wildcardFilenamePatternParts[wildcardFilenamePatternPartsIndex].equals("*")) {
						anyChars = true;
						if (wildcardFilenamePatternPartsIndex == wildcardFilenamePatternParts.length - 1) {
							textIndex = fileName.length();
						}
					} else {
						if (anyChars) {
							textIndex = checkIndexOf(fileName, textIndex, wildcardFilenamePatternParts[wildcardFilenamePatternPartsIndex], caseSensitive);
							if (textIndex == -1) {
								break;
							}
							final int repeat = checkIndexOf(fileName, textIndex + 1, wildcardFilenamePatternParts[wildcardFilenamePatternPartsIndex], caseSensitive);
							if (repeat >= 0) {
								backtrack.push(new int[] { wildcardFilenamePatternPartsIndex, repeat });
							}
						} else {
							if (!fileName.regionMatches(!caseSensitive, textIndex,
									wildcardFilenamePatternParts[wildcardFilenamePatternPartsIndex], 0,
									wildcardFilenamePatternParts[wildcardFilenamePatternPartsIndex].length())) {
								break;
							}
						}

						textIndex += wildcardFilenamePatternParts[wildcardFilenamePatternPartsIndex].length();
						anyChars = false;
					}

					wildcardFilenamePatternPartsIndex++;
				}

				if (wildcardFilenamePatternPartsIndex == wildcardFilenamePatternParts.length && textIndex == fileName.length()) {
					return true;
				}

			} while (!backtrack.isEmpty());

			return false;
		}
	}

	private static String[] splitOnTokens(final String text) {
		if (text.indexOf('?') == -1 && text.indexOf('*') == -1) {
			return new String[] { text };
		}

		final char[] array = text.toCharArray();
		final ArrayList<String> list = new ArrayList<>();
		final StringBuilder buffer = new StringBuilder();
		char previousChar = 0;
		for (final char nextChar : array) {
			if (nextChar == '?' || nextChar == '*') {
				if (buffer.length() != 0) {
					list.add(buffer.toString());
					buffer.setLength(0);
				}
				if (nextChar == '?') {
					list.add("?");
				} else if (previousChar != '*') {
					list.add("*");
				}
			} else {
				buffer.append(nextChar);
			}
			previousChar = nextChar;
		}
		if (buffer.length() != 0) {
			list.add(buffer.toString());
		}

		return list.toArray(new String[0]);
	}

	private static int checkIndexOf(final String str, final int strStartIndex, final String search, final boolean caseSensitive) {
		final int endIndex = str.length() - search.length();
		if (endIndex >= strStartIndex) {
			for (int i = strStartIndex; i <= endIndex; i++) {
				if (str.regionMatches(!caseSensitive, i, search, 0, search.length())) {
					return i;
				}
			}
		}
		return -1;
	}
}
