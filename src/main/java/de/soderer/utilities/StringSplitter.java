package de.soderer.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Utilities for splitting Strings and Lists
 */
public class StringSplitter {
	private StringSplitter() {
		// Utility class
	}

	/**
	 * Split a text at a delimiter character, which can be escaped by an escape character.
	 * An escaped delimiter is not split at and becomes the plain delimiter, an escaped escape character becomes the plain
	 * escape character (so a part may end with the escape character). An escape character before any other character is kept.
	 *
	 * @param text text to split, must not be null
	 * @param delimiterChar delimiter character
	 * @param escapeChar escape character preceding a delimiter or escape character that is part of the data
	 * @param limit limit like in {@link String#split(String, int)}
	 * @return parts of the text
	 */
	public static String[] split(final String text, final char delimiterChar, final char escapeChar, final int limit) {
		// Parsed character by character, because a regex lookbehind cannot distinguish an escaped delimiter from an escaped escape character
		final List<String> parts = new ArrayList<>();
		StringBuilder currentPart = new StringBuilder();
		for (int i = 0; i < text.length(); i++) {
			final char nextChar = text.charAt(i);
			if (nextChar == escapeChar && i + 1 < text.length() && (text.charAt(i + 1) == delimiterChar || text.charAt(i + 1) == escapeChar)) {
				currentPart.append(text.charAt(i + 1));
				i++;
			} else if (nextChar == delimiterChar && (limit <= 0 || parts.size() < limit - 1)) {
				parts.add(currentPart.toString());
				currentPart = new StringBuilder();
			} else {
				currentPart.append(nextChar);
			}
		}
		parts.add(currentPart.toString());

		if (limit == 0) {
			// Like String.split: trailing empty parts are removed, but a text without delimiter is kept as single part
			while (parts.size() > 1 && parts.get(parts.size() - 1).isEmpty()) {
				parts.remove(parts.size() - 1);
			}
			if (parts.size() == 1 && parts.get(0).isEmpty() && text.length() > 0) {
				parts.clear();
			}
		}
		return parts.toArray(new String[0]);
	}

	/**
	 * Split a text at a delimiter character, which can be escaped by an escape character.
	 * Escaped delimiters and escaped escape characters are unescaped in the result. Trailing empty parts are removed.
	 *
	 * @param text text to split, must not be null
	 * @param delimiterChar delimiter character
	 * @param escapeChar escape character preceding a delimiter or escape character that is part of the data
	 * @return parts of the text
	 */
	public static String[] split(final String text, final char delimiterChar, final char escapeChar) {
		return split(text, delimiterChar, escapeChar, 0);
	}

	/**
	 * Split a command line into arguments at blanks, tabs and linebreaks.
	 * Like in a shell, quoted parts (single or double quotes) may contain blanks and are part of the surrounding argument,
	 * e.g. {@code --name="a b"} becomes {@code --name=a b}.
	 * Within double quotes a backslash escapes a double quote or a backslash, before any other character it is kept.
	 * Within single quotes a backslash has no special meaning, so Windows paths like {@code 'C:\Temp\'} can be quoted.
	 *
	 * @param argumentLine command line, must not be null
	 * @return list of arguments
	 * @throws Exception if a quote is not closed
	 */
	public static List<String> parseArguments(final String argumentLine) throws Exception {
		return parseTokens(argumentLine, ' ', '\n', '\t');
	}

	/**
	 * Split a text into tokens at the given delimiter characters.
	 * Like in a shell, quoted parts (single or double quotes) may contain delimiters and are part of the surrounding
	 * token, e.g. {@code key="a;b"} becomes {@code key=a;b}.
	 * Within double quotes a backslash escapes a double quote or a backslash, before any other character it is kept.
	 * Within single quotes a backslash has no special meaning.
	 *
	 * @param line text to split, must not be null
	 * @param delimiters characters separating the tokens
	 * @return list of tokens
	 * @throws Exception if a quote is not closed
	 */
	public static List<String> parseTokens(final String line, final char... delimiters) throws Exception {
		final List<String> returnList = new ArrayList<>();
		StringBuilder currentArgument = new StringBuilder();
		Character enclosingChar = null;
		boolean escapedNextChar = false;
		boolean argumentStarted = false;
		for (final char nextChar : line.replace("\r\n", "\n").replace("\r", "\n").toCharArray()) {
			if (enclosingChar != null) {
				if (escapedNextChar) {
					// Only a double quote and a backslash can be escaped, so "C:\Temp" keeps its backslash
					if (nextChar != '"' && nextChar != '\\') {
						currentArgument.append('\\');
					}
					currentArgument.append(nextChar);
					escapedNextChar = false;
				} else if (nextChar == '\\' && enclosingChar == '"') {
					// Like in a shell, escaping only works within double quotes
					escapedNextChar = true;
				} else if (nextChar == enclosingChar) {
					// Like in a shell the quoted part continues the current argument, e.g. --name="a b" becomes --name=a b
					enclosingChar = null;
				} else {
					currentArgument.append(nextChar);
				}
			} else {
				if (nextChar == '\'' || nextChar == '"') {
					enclosingChar = nextChar;
					// Also an empty quoted text like "" is an argument
					argumentStarted = true;
				} else if (Utilities.equalsAnyChar(nextChar, delimiters)) {
					if (argumentStarted) {
						returnList.add(currentArgument.toString());
						currentArgument = new StringBuilder();
						argumentStarted = false;
					}
				} else {
					currentArgument.append(nextChar);
					argumentStarted = true;
				}
			}
		}
		if (enclosingChar != null) {
			throw new Exception("Invalid quotation. Missing closing " + enclosingChar);
		} else if (argumentStarted) {
			returnList.add(currentArgument.toString());
		}
		return returnList;
	}

	/**
	 * Split a text at ',', ';', '|', blanks, tabs and linebreaks into trimmed, non-empty items.
	 *
	 * @param stringList text to split, may be null
	 * @return list of items, or null for null input
	 */
	public static List<String> splitAndTrimList(final String stringList) {
		if (stringList == null) {
			return null;
		} else {
			final List<String> returnList = new ArrayList<>();
			final String[] parts = stringList.split(",|;|\\|| |\\n|\\r|\\t");
			for (final String part : parts) {
				// Blank parts (e.g. only "\f") would become empty items after trimming
				if (StringUtilities.isNotBlank(part)) {
					returnList.add(part.trim());
				}
			}
			return returnList;
		}
	}

	/**
	 * Split a text at the given separator characters into trimmed, non-empty items.
	 *
	 * @param stringList text to split, may be null
	 * @param separatorChars characters separating the items
	 * @return list of items, or null for null input
	 */
	public static List<String> splitAndTrimList(final String stringList, final Character... separatorChars) {
		if (stringList == null) {
			return null;
		} else {
			final List<String> returnList = new ArrayList<>();
			// Each separator is quoted, so regex meta characters like '.' or '|' are used literally
			final StringBuilder separatorRegex = new StringBuilder();
			for (final Character separatorChar : separatorChars) {
				if (separatorChar != null) {
					if (separatorRegex.length() > 0) {
						separatorRegex.append('|');
					}
					separatorRegex.append(Pattern.quote(separatorChar.toString()));
				}
			}
			if (separatorRegex.length() == 0) {
				final List<String> singleItemList = new ArrayList<>();
				if (StringUtilities.isNotBlank(stringList)) {
					singleItemList.add(stringList.trim());
				}
				return singleItemList;
			}
			final String[] parts = stringList.split(separatorRegex.toString());
			for (final String part : parts) {
				// Blank parts like in "a, ,b" would become empty items after trimming
				if (StringUtilities.isNotBlank(part)) {
					returnList.add(part.trim());
				}
			}
			return returnList;
		}
	}

	/**
	 * Split a text at the given separator characters into trimmed, non-empty items.
	 * Separators within single or double quotes are ignored, the quotes are kept in the items.
	 *
	 * @param stringList text to split, must not be null
	 * @param separatorChars characters separating the items
	 * @return list of items
	 */
	public static List<String> splitAndTrimListQuoted(final String stringList, final char... separatorChars) {
		final List<String> returnList = new ArrayList<>();
		StringBuilder nextLine = new StringBuilder();
		boolean quotedBySingleQoute = false;
		boolean quotedByDoubleQoute = false;
		for (final char nextChar : stringList.toCharArray()) {
			if ('\'' == nextChar) {
				if (!quotedBySingleQoute && !quotedByDoubleQoute) {
					quotedBySingleQoute = true;
				} else if (quotedBySingleQoute) {
					quotedBySingleQoute = false;
				}
			} else if ('"' == nextChar) {
				if (!quotedBySingleQoute && !quotedByDoubleQoute) {
					quotedByDoubleQoute = true;
				} else if (quotedByDoubleQoute) {
					quotedByDoubleQoute = false;
				}
			}

			boolean splitFound = false;
			for (final char separatorChar : separatorChars) {
				if (separatorChar == nextChar && !quotedBySingleQoute && !quotedByDoubleQoute) {
					final String line = nextLine.toString().trim();
					if (line.length() > 0) {
						returnList.add(line);
					}
					// The separator is consumed also after an empty item (before, it was added to the next item)
					splitFound = true;
					nextLine = new StringBuilder();
					break;
				}
			}

			if (!splitFound) {
				nextLine.append(nextChar);
			}
		}
		final String line = nextLine.toString().trim();
		if (line.length() > 0) {
			returnList.add(line);
		}
		return returnList;
	}
}
