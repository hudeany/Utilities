package de.soderer.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.soderer.utilities.EncodingDetector.DetectionResult;

/**
 * Text Utilities
 *
 * <p>This class does no Logging via Log4J, because it is often used before its initialisation</p>
 */
public class TextUtilities {
	private TextUtilities() {
		// Utility class
	}

	/**
	 * Return value of stream read methods signaling the end of the stream
	 */
	public static final int EOF = -1;

	/**
	 * A simple string for testing, which includes all ASCII characters
	 */
	public static final String ASCII_CHARACTERS_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 !\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";

	/**
	 * A simple string for testing, which includes all german characters
	 */
	public static final String GERMAN_TEST_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 \u00E4\u00F6\u00FC\u00DF\u00C4\u00D6\u00DC\u00B5!?\u00A7@\u20AC$%&/\\<>(){}[]'\"\u00B4`^\u00B0\u00B9\u00B2\u00B3*#.,;:=+-~_|\u00BD\u00BC\u00AC";

	/**
	 * A simple string for testing, which includes all special characters
	 */
	public static final String SPECIAL_TEST_STRING = "\n\r\t\b\f\u00c4\u00e4\u00d6\u00f6\u00dc\u00fc\u00df";

	/**
	 * Enum to represent a linebreak type of an text
	 */
	public enum LineBreak {
		/**
		 * No linebreak
		 */
		Unknown(null),

		/**
		 * Multiple linebreak types
		 */
		Mixed(null),

		/**
		 * Unix/Linux linebreak ("\n")
		 */
		Unix("\n"),

		/**
		 * Mac/Apple linebreak ("\r")
		 */
		Mac("\r"),

		/**
		 * Windows linebreak ("\r\n")
		 */
		Windows("\r\n");

		private final String representationString;

		@Override
		public String toString() {
			return representationString;
		}

		LineBreak(final String representationString) {
			this.representationString = representationString;
		}

		/**
		 * Get the linebreak type by its name, ignoring case.
		 *
		 * @param lineBreakTypeName "Windows", "Unix" or "Mac", may be null
		 * @return matching linebreak type, or {@link #Unknown} for any other name
		 */
		public static LineBreak getLineBreakTypeByName(final String lineBreakTypeName) {
			if ("WINDOWS".equalsIgnoreCase(lineBreakTypeName)) {
				return LineBreak.Windows;
			} else if ("UNIX".equalsIgnoreCase(lineBreakTypeName)) {
				return LineBreak.Unix;
			} else if ("MAC".equalsIgnoreCase(lineBreakTypeName)) {
				return LineBreak.Mac;
			} else {
				return LineBreak.Unknown;
			}
		}
	}

	/**
	 * Trim a string to an exact length with alignment right for display purposes.
	 * If the string underruns the length, it will be filled up with blanks on the left side.
	 * If the string exceeds the length, it will be cut from the left side.
	 *
	 * @param inputString string to align, must not be null
	 * @param length exact length of the result, must be greater than 0
	 * @return string of exactly the given length
	 */
	public static String trimStringToLengthAlignedRight(final String inputString, final int length) {
		if (inputString.length() > length) {
			// Takes the last characters of length
			return inputString.subSequence(inputString.length() - length, inputString.length()).toString();
		} else {
			return String.format("%" + length + "s", inputString);
		}
	}

	/**
	 * Trim a string to an exact length with alignment left for display purposes.
	 * If the string underruns the length, it will be filled up with blanks on the right side.
	 * If the string exceeds the length, it will be cut from the right side.
	 *
	 * @param inputString string to align, must not be null
	 * @param length exact length of the result, must be greater than 0
	 * @return string of exactly the given length
	 */
	public static String trimStringToLengthAlignedLeft(final String inputString, final int length) {
		if (inputString.length() > length) {
			// Takes the first characters of length
			return inputString.subSequence(0, length).toString();
		} else {
			return String.format("%-" + length + "s", inputString);
		}
	}

	/**
	 * Build a string with repetitions. 0 repetitions returns an empty string.
	 *
	 * @param itemString string to be repeated
	 * @param repeatTimes number of repetitions
	 * @return repeated string
	 */
	public static String repeatString(final String itemString, final int repeatTimes) {
		return repeatString(itemString, repeatTimes, null);
	}

	/**
	 * Build a string with repetitions. 0 repetitions returns an empty string.
	 * In other cases a glue string is put between the repetitions, which can be left empty.
	 *
	 * @param itemString string to be repeated
	 * @param repeatTimes number of repetitions
	 * @param separatorString glue string, null for none
	 * @return repeated string
	 */
	public static String repeatString(final String itemString, final int repeatTimes, final String separatorString) {
		final StringBuilder returnStringBuilder = new StringBuilder();
		for (int i = 0; i < repeatTimes; i++) {
			if (separatorString != null && returnStringBuilder.length() > 0) {
				returnStringBuilder.append(separatorString);
			}
			returnStringBuilder.append(itemString);
		}
		return returnStringBuilder.toString();
	}

	/**
	 * Split a string into smaller strings of a maximum size. Only the last chunk may be shorter.
	 *
	 * @param text text to split, must not be null
	 * @param chunkSize maximum number of characters per chunk, must be greater than 0
	 * @return list of chunks, empty for an empty text
	 * @throws IllegalArgumentException if the chunk size is less than 1
	 */
	public static List<String> chopToChunks(final String text, final int chunkSize) {
		if (chunkSize <= 0) {
			throw new IllegalArgumentException("Invalid chunk size: " + chunkSize);
		}
		final List<String> returnList = new ArrayList<>((text.length() + chunkSize - 1) / chunkSize);

		for (int start = 0; start < text.length(); start += chunkSize) {
			returnList.add(text.substring(start, Math.min(text.length(), start + chunkSize)));
		}

		return returnList;
	}

	/**
	 * Reduce a text to a maximum number of lines and append "..." if lines were cut off.
	 * The type of linebreaks in the result string will not be changed (mixed linebreaks become Unix linebreaks).
	 * If the text has less lines than the maximum it will be returned unchanged.
	 *
	 * @param value text to reduce, must not be null
	 * @param maxLines maximum number of lines
	 * @return reduced text
	 */
	public static String trimToMaxNumberOfLines(final String value, final int maxLines) {
		final String normalizedValue = normalizeLineBreaks(value, LineBreak.Unix);
		int count = 0;
		// Start before the first character, so a linebreak at index 0 is also counted
		int nextLinebreak = -1;
		while ((count == 0 || nextLinebreak != -1) && count < maxLines) {
			nextLinebreak = normalizedValue.indexOf(LineBreak.Unix.toString(), nextLinebreak + 1);
			count++;
		}

		// Only cut if text follows the linebreak (before, "a\n" with 1 line got a "..." although nothing was cut off)
		if (nextLinebreak != -1 && nextLinebreak < normalizedValue.length() - 1) {
			final LineBreak originalLineBreakType = detectLinebreakType(value);
			return normalizeLineBreaks(normalizedValue.substring(0, nextLinebreak + 1), originalLineBreakType) + "...";
		} else {
			return value;
		}
	}

	/**
	 * Detect the type of linebreaks in a text.
	 *
	 * @param value text to analyze
	 * @return linebreak type, {@link LineBreak#Mixed} for multiple types or {@link LineBreak#Unknown} if there is no linebreak
	 */
	public static LineBreak detectLinebreakType(final String value) {
		final TextPropertiesReader textPropertiesReader = new TextPropertiesReader(value);
		textPropertiesReader.readProperties();
		return textPropertiesReader.getLinebreakType();
	}

	/**
	 * Change all linebreaks of a text, no matter what type they are, to a given type.
	 *
	 * @param value text to change, may be null
	 * @param type new linebreak type, Unix linebreaks are used for {@link LineBreak#Unknown} or {@link LineBreak#Mixed}
	 * @return text with normalized linebreaks, or null for null input
	 */
	public static String normalizeLineBreaks(final String value, final LineBreak type) {
		if (value == null) {
			return value;
		} else {
			final String returnString = value.replace(LineBreak.Windows.toString(), LineBreak.Unix.toString()).replace(LineBreak.Mac.toString(), LineBreak.Unix.toString());
			if (type == LineBreak.Mac) {
				return returnString.replace(LineBreak.Unix.toString(), LineBreak.Mac.toString());
			} else if (type == LineBreak.Windows) {
				return returnString.replace(LineBreak.Unix.toString(), LineBreak.Windows.toString());
			} else {
				return returnString;
			}
		}
	}

	/**
	 * Change all linebreaks of a text to the type of the current operating system (Windows or Unix).
	 *
	 * @param value text to change, may be null
	 * @return text with normalized linebreaks, or null for null input
	 */
	public static String normalizeLineBreaksForCurrentSystem(final String value) {
		if (SystemUtilities.isWindowsSystem()) {
			return normalizeLineBreaks(value, LineBreak.Windows);
		} else {
			return normalizeLineBreaks(value, LineBreak.Unix);
		}
	}

	/**
	 * Get the text lines of a text as a list of strings. Linebreaks (LF, CR or CRLF) are not included.
	 *
	 * @param dataString text to split, must not be null
	 * @return lines of the text
	 * @throws IOException not thrown for string data
	 */
	public static List<String> getLines(final String dataString) throws IOException {
		try (LineNumberReader lineNumberReader = new LineNumberReader(new StringReader(dataString))) {
			final List<String> list = new ArrayList<>();

			String line;
			while ((line = lineNumberReader.readLine()) != null) {
				list.add(line);
			}

			return list;
		}
	}

	/**
	 * Get the number of lines in a text. A trailing linebreak does not start a new line.
	 *
	 * @param dataString text to analyze, may be null
	 * @return number of lines, 1 for an empty text, 0 for null
	 */
	public static int getLineCount(final String dataString) {
		if (dataString == null) {
			return 0;
		} else if ("".equals(dataString)) {
			return 1;
		} else {
			try (LineNumberReader lineNumberReader = new LineNumberReader(new StringReader(dataString))) {
				while (lineNumberReader.readLine() != null) {
					// do nothing
				}

				return lineNumberReader.getLineNumber();
			} catch (final IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Count the number of words separated by whitespace in a text.
	 *
	 * @param dataString text to analyze, must not be null
	 * @return number of words
	 */
	public static int countWords(final String dataString) {
		int counter = 0;
		boolean isWord = false;
		final int endOfLine = dataString.length() - 1;

		for (int i = 0; i < dataString.length(); i++) {
			if (!Character.isWhitespace(dataString.charAt(i)) && i != endOfLine) {
				isWord = true;
			} else if (Character.isWhitespace(dataString.charAt(i)) && isWord) {
				counter++;
				isWord = false;
			} else if (!Character.isWhitespace(dataString.charAt(i)) && i == endOfLine) {
				counter++;
			}
		}
		return counter;
	}

	/**
	 * Get the start index of a line within a text. LF, CR and CRLF linebreaks are supported.
	 *
	 * @param dataString text, may be null
	 * @param lineNumber 1-based line number; for a line number beyond the last line the start of the last line is returned
	 * @return start index of the line, or -1 for null text or a negative line number
	 */
	public static int getStartIndexOfLine(final String dataString, final int lineNumber) {
		if (dataString == null || lineNumber < 0) {
			return -1;
		} else if (lineNumber == 1) {
			return 0;
		} else {
			int lineCount = 1;
			int position = 0;
			while (lineCount < lineNumber) {
				final int nextLineBreakMac = dataString.indexOf(LineBreak.Mac.toString(), position);
				final int nextLineBreakUnix = dataString.indexOf(LineBreak.Unix.toString(), position);
				final int nextLineBreakWindows = dataString.indexOf(LineBreak.Windows.toString(), position);
				int nextPosition = -1;
				int lineBreakSize = 0;
				if (nextLineBreakMac >= 0 && (nextLineBreakUnix < 0 || nextLineBreakMac < nextLineBreakUnix) && (nextLineBreakWindows < 0 || nextLineBreakMac < nextLineBreakWindows)) {
					nextPosition = nextLineBreakMac;
					lineBreakSize = 1;
				} else if (nextLineBreakUnix >= 0 && (nextLineBreakWindows < 0 || nextLineBreakUnix < nextLineBreakWindows)) {
					nextPosition = nextLineBreakUnix;
					lineBreakSize = 1;
				} else if (nextLineBreakWindows >= 0) {
					nextPosition = nextLineBreakWindows;
					lineBreakSize = 2;
				}

				if (nextPosition >= 0) {
					position = nextPosition + lineBreakSize;
					if (position >= dataString.length()) {
						break;
					}
				} else {
					break;
				}
				lineCount++;
			}
			return position;
		}
	}

	/**
	 * Get the start index of the line containing a given position within the text.
	 * A linebreak character at the position belongs to the line it ends.
	 *
	 * @param dataString text, may be null
	 * @param index position within the text
	 * @return start index of the line, or -1 for null text or a negative index
	 */
	public static int getStartIndexOfLineAtIndex(final String dataString, final int index) {
		if (dataString == null || index < 0) {
			return -1;
		} else {
			// The nearest linebreak character (CR or LF) before the index ends the previous line.
			// Before, the farthest linebreak type was chosen in mixed texts and a linebreak exactly at the index was taken as the previous line's end.
			for (int position = Math.min(index, dataString.length()) - 1; position >= 0; position--) {
				final char character = dataString.charAt(position);
				if (character == '\n' || character == '\r') {
					return position + 1;
				}
			}
			return 0;
		}
	}

	/**
	 * Get the end index of the line containing a given position within the text, which is the index of its linebreak (CR or LF)
	 * or the text length for the last line.
	 *
	 * @param dataString text, may be null
	 * @param index position within the text
	 * @return index of the line's end, or -1 for null text or a negative index
	 */
	public static int getEndIndexOfLineAtIndex(final String dataString, final int index) {
		if (dataString == null || index < 0) {
			return -1;
		} else {
			// The nearest linebreak character (CR or LF) at or after the index ends the line.
			// Before, the farthest linebreak type was chosen in mixed texts and index 0 always returned 0.
			for (int position = index; position < dataString.length(); position++) {
				final char character = dataString.charAt(position);
				if (character == '\n' || character == '\r') {
					return position;
				}
			}
			return dataString.length();
		}
	}

	/**
	 * Search the next position of a string or search pattern beginning at a start index.
	 * If nothing is found, the search continues from the other end of the text (wrap around).
	 *
	 * @param text text to search in; null or blank text yields null
	 * @param startPosition position to start the search at
	 * @param searchTextOrPattern simple text or regular expression to search for
	 * @param searchTextIsRegularExpression true if searchTextOrPattern is a regular expression
	 * @param searchCaseSensitive true for a case-sensitive search
	 * @param searchReversely true to search backwards for the last match starting before startPosition
	 * @return Tuple First = start index, Second = length of found substring; null if nothing was found or a parameter is invalid
	 */
	public static Tuple<Integer, Integer> searchNextPosition(final String text, final int startPosition, final String searchTextOrPattern, final boolean searchTextIsRegularExpression, final boolean searchCaseSensitive,
			final boolean searchReversely) {
		if (Utilities.isBlank(text) || startPosition < 0 || text.length() < startPosition || Utilities.isEmpty(searchTextOrPattern)) {
			return null;
		} else {
			final String fullText = text;
			int nextPosition = -1;
			int length = -1;

			Pattern searchPattern;
			if (searchTextIsRegularExpression) {
				if (searchCaseSensitive) {
					searchPattern = Pattern.compile(searchTextOrPattern, Pattern.MULTILINE);
				} else {
					searchPattern = Pattern.compile(searchTextOrPattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
				}
			} else {
				if (searchCaseSensitive) {
					searchPattern = Pattern.compile(searchTextOrPattern, Pattern.LITERAL | Pattern.MULTILINE);
				} else {
					searchPattern = Pattern.compile(searchTextOrPattern, Pattern.CASE_INSENSITIVE | Pattern.LITERAL | Pattern.MULTILINE);
				}
			}

			final Matcher matcher = searchPattern.matcher(fullText);

			if (searchReversely) {
				// Last match before the start position, or else (wrap around) the last match of the whole text.
				// Before, matcher.start() was called after a failed find(), throwing an IllegalStateException if the text had no match,
				// and a single match at index 0 was not found when wrapping around.
				int lastMatchPosition = -1;
				int lastMatchLength = -1;
				while (matcher.find()) {
					if (matcher.start() < startPosition) {
						nextPosition = matcher.start();
						length = matcher.end() - nextPosition;
					}
					lastMatchPosition = matcher.start();
					lastMatchLength = matcher.end() - lastMatchPosition;
				}
				if (nextPosition < 0) {
					nextPosition = lastMatchPosition;
					length = lastMatchLength;
				}
			} else {
				if (matcher.find(startPosition)) {
					nextPosition = matcher.start();
					length = matcher.end() - nextPosition;
				}
				if (startPosition > 0 && nextPosition < 0) {
					if (matcher.find(0)) {
						nextPosition = matcher.start();
						length = matcher.end() - nextPosition;
					}
				}
			}

			if (nextPosition < 0) {
				return null;
			} else {
				return new Tuple<>(nextPosition, length);
			}
		}
	}

	/**
	 * Count the non-overlapping occurrences of a string or pattern within a text.
	 *
	 * @param text text to search in
	 * @param searchTextOrPattern simple text or regular expression to search for
	 * @param searchTextIsRegularExpression true if searchTextOrPattern is a regular expression
	 * @param searchCaseSensitive true for a case-sensitive search
	 * @return number of occurrences, or -1 if text or searchTextOrPattern is null
	 */
	public static int countOccurences(final String text, final String searchTextOrPattern, final boolean searchTextIsRegularExpression, final boolean searchCaseSensitive) {
		if (text != null && searchTextOrPattern != null) {
			final String fullText = text;
			int occurences = 0;

			Pattern searchPattern;
			if (searchTextIsRegularExpression) {
				if (searchCaseSensitive) {
					searchPattern = Pattern.compile(searchTextOrPattern, Pattern.MULTILINE);
				} else {
					searchPattern = Pattern.compile(searchTextOrPattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
				}
			} else {
				if (searchCaseSensitive) {
					searchPattern = Pattern.compile(searchTextOrPattern, Pattern.LITERAL | Pattern.MULTILINE);
				} else {
					searchPattern = Pattern.compile(searchTextOrPattern, Pattern.CASE_INSENSITIVE | Pattern.LITERAL | Pattern.MULTILINE);
				}
			}

			final Matcher matcher = searchPattern.matcher(fullText);
			while (matcher.find()) {
				occurences++;
			}
			return occurences;
		} else {
			return -1;
		}
	}

	/**
	 * Get the line number at a given text position.
	 *
	 * @param dataString text, may be null
	 * @param textPosition position within the text
	 * @return 1-based line number, or -1 for null text
	 */
	public static int getLineNumberOfTextposition(final String dataString, final int textPosition) {
		if (dataString == null) {
			return -1;
		} else {
			try {
				String textPart = dataString;
				if (dataString.length() > textPosition) {
					textPart = dataString.substring(0, textPosition);
				}
				int lineNumber = getLineCount(textPart);
				if (textPart.endsWith(LineBreak.Unix.toString()) || textPart.endsWith(LineBreak.Mac.toString())) {
					lineNumber++;
				}
				return lineNumber;
			} catch (final Exception e) {
				e.printStackTrace();
				return -1;
			}
		}
	}

	/**
	 * Get the number of the first line containing a string or pattern.
	 *
	 * @param dataString text to search in, must not be null
	 * @param searchTextOrPattern simple text or regular expression to search for
	 * @param searchCaseSensitive true for a case-sensitive search
	 * @param searchTextIsRegularExpression true if searchTextOrPattern is a regular expression
	 * @return 1-based line number, or -1 if not found
	 * @throws IOException not thrown for string data
	 */
	public static int getNumberOfLineContainingText(final String dataString, final String searchTextOrPattern, final boolean searchCaseSensitive, final boolean searchTextIsRegularExpression) throws IOException {
		Pattern searchPattern;
		if (searchTextIsRegularExpression) {
			if (searchCaseSensitive) {
				searchPattern = Pattern.compile(searchTextOrPattern, Pattern.MULTILINE);
			} else {
				searchPattern = Pattern.compile(searchTextOrPattern, Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
			}
		} else {
			if (searchCaseSensitive) {
				searchPattern = Pattern.compile(searchTextOrPattern, Pattern.LITERAL | Pattern.MULTILINE);
			} else {
				searchPattern = Pattern.compile(searchTextOrPattern, Pattern.CASE_INSENSITIVE | Pattern.LITERAL | Pattern.MULTILINE);
			}
		}

		try (LineNumberReader lineNumberReader = new LineNumberReader(new StringReader(dataString))) {
			String lineContent;
			while ((lineContent = lineNumberReader.readLine()) != null) {
				final Matcher matcher = searchPattern.matcher(lineContent);
				if (matcher.find()) {
					return lineNumberReader.getLineNumber();
				}
			}
			return -1;
		}
	}

	/**
	 * Insert some string before a given line into a text.
	 *
	 * @param dataString text, must not be null
	 * @param insertText text to insert
	 * @param lineNumber 1-based line number, negative values leave the text unchanged
	 * @return text with the inserted string
	 */
	public static String insertTextBeforeLine(final String dataString, final String insertText, final int lineNumber) {
		if (lineNumber > -1) {
			final int startIndex = TextUtilities.getStartIndexOfLine(dataString, lineNumber);
			final StringBuilder dataBuilder = new StringBuilder(dataString);
			dataBuilder.insert(startIndex, insertText);
			return dataBuilder.toString();
		} else {
			return dataString;
		}
	}

	/**
	 * Insert some string after a given line (at the start of the next line) into a text.
	 *
	 * @param dataString text, must not be null
	 * @param insertText text to insert
	 * @param lineNumber 1-based line number, negative values leave the text unchanged
	 * @return text with the inserted string
	 */
	public static String insertTextAfterLine(final String dataString, final String insertText, final int lineNumber) {
		if (lineNumber > -1) {
			final int startIndex = TextUtilities.getStartIndexOfLine(dataString, lineNumber + 1);
			final StringBuilder dataBuilder = new StringBuilder(dataString);
			dataBuilder.insert(startIndex, insertText);
			return dataBuilder.toString();
		} else {
			return dataString;
		}
	}

	/**
	 * Try to detect the encoding of byte array text data.
	 *
	 * @param data text data
	 * @return Tuple First = charset name, Second = true if the data starts with a byte order mark (BOM)
	 */
	public static Tuple<String, Boolean> detectEncoding(final byte[] data) {
		return detectEncoding(data, null);
	}

	/**
	 * Try to detect the encoding of byte array text data.
	 *
	 * @param data text data
	 * @param nonUtf8FallbackCharset charset for data without BOM which is no valid UTF-8, or null for Windows-1252
	 * @return Tuple First = charset name, Second = true if the data starts with a byte order mark (BOM)
	 */
	public static Tuple<String, Boolean> detectEncoding(final byte[] data, final Charset nonUtf8FallbackCharset) {
		final DetectionResult result = EncodingDetector.detectEncoding(data, nonUtf8FallbackCharset);
		return new Tuple<>(result.charset.name(), result.hasBom);
	}

	/**
	 * Replace the leading blanks of each line by some other text like a tab.
	 * Each group of blankCount leading blanks is replaced, remaining blanks are kept.
	 *
	 * @param data text, may be null
	 * @param blankCount number of blanks representing one indentation level, values less than 1 leave the text unchanged
	 * @param replacement text replacing one indentation level, e.g. "\t"
	 * @return text with replaced indentation, or null for null input
	 */
	public static String replaceIndentingBlanks(final String data, final int blankCount, final String replacement) {
		if (data == null || blankCount <= 0) {
			return data;
		}
		// Single pass over all leading blanks. Before, only pairs of blanks were matched regardless of blankCount
		// (so e.g. 6 blanks with blankCount 4 lost 2 blanks), and the loop never ended if the replacement started with two blanks.
		final Pattern pattern = Pattern.compile("^ +", Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(data);
		final StringBuilder result = new StringBuilder();
		while (matcher.find()) {
			final int indentionLength = matcher.end() - matcher.start();
			final String newIndention = Utilities.repeat(replacement, indentionLength / blankCount) + Utilities.repeat(" ", indentionLength % blankCount);
			matcher.appendReplacement(result, Matcher.quoteReplacement(newIndention));
		}
		matcher.appendTail(result);
		return result.toString();
	}

	/**
	 * Remove the first leading blank or tab of each line of a text.
	 *
	 * @param data text, must not be null
	 * @return text with each line unindented by one character
	 */
	public static String removeFirstLeadingWhitespace(final String data) {
		final Pattern pattern = Pattern.compile("^[\\t ]", Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(data);
		return matcher.replaceAll("");
	}

	/**
	 * Indent the lines of a string by one tab.
	 *
	 * @param data text, must not be null
	 * @return indented text
	 */
	public static String addLeadingTab(final String data) {
		return addLeadingTabs(data, 1);
	}

	/**
	 * Indent the lines of a string by some tabs.
	 *
	 * @param data text, must not be null
	 * @param numberOfTabs number of tabs to prepend to each line
	 * @return indented text
	 */
	public static String addLeadingTabs(final String data, final int numberOfTabs) {
		final Pattern pattern = Pattern.compile("^", Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(data);
		return matcher.replaceAll(Utilities.repeat("\t", numberOfTabs));
	}

	/**
	 * Chop a string into pieces of maximum length.
	 *
	 * @param value text to chop, must not be null
	 * @param length maximum length of each piece, must be greater than 0
	 * @return list of pieces, empty for an empty text
	 * @throws IllegalArgumentException if the length is less than 1
	 */
	public static List<String> truncate(final String value, final int length) {
		final List<String> parts = new ArrayList<>();
		if (length <= 0) {
			// Avoid an endless loop
			throw new IllegalArgumentException("Invalid length: " + length);
		}
		final int valueLength = value.length();
		for (int i = 0; i < valueLength; i += length) {
			parts.add(value.substring(i, Math.min(valueLength, i + length)));
		}
		return parts;
	}

	/**
	 * Trim all strings in an array of strings to a maximum length and add the cut off rest as new lines.
	 *
	 * @param lines lines to truncate, must not be null
	 * @param length maximum length of each line, must be greater than 0
	 * @return list of truncated lines
	 */
	public static List<String> truncate(final String[] lines, final int length) {
		final List<String> linesTruncated = new ArrayList<>();
		for (final String line : lines) {
			linesTruncated.addAll(truncate(line, length));
		}
		return linesTruncated;
	}

	/**
	 * Trim all strings in a list of strings to a maximum length and add the cut off rest as new lines.
	 *
	 * @param lines lines to truncate, must not be null
	 * @param length maximum length of each line, must be greater than 0
	 * @return list of truncated lines
	 */
	public static List<String> truncate(final List<String> lines, final int length) {
		final List<String> linesTruncated = new ArrayList<>();
		for (final String line : lines) {
			linesTruncated.addAll(truncate(line, length));
		}
		return linesTruncated;
	}

	/**
	 * Remove all leading and trailing whitespaces of each line, not only blanks like it is done by {@link String#trim()}.
	 * Empty lines are removed, too.
	 *
	 * @param data text, must not be null
	 * @return text without leading and trailing whitespace
	 */
	public static String removeLeadingAndTrailingWhitespaces(String data) {
		final Pattern pattern1 = Pattern.compile("^\\s*", Pattern.MULTILINE);
		final Matcher matcher1 = pattern1.matcher(data);
		data = matcher1.replaceAll("");

		final Pattern pattern2 = Pattern.compile("\\s*$", Pattern.MULTILINE);
		final Matcher matcher2 = pattern2.matcher(data);
		data = matcher2.replaceAll("");

		return data;
	}

	/**
	 * Remove all trailing whitespaces of each line. Empty lines are removed, too.
	 *
	 * @param data text, must not be null
	 * @return text without trailing whitespace
	 */
	public static String removeTrailingWhitespaces(String data) {
		final Pattern pattern = Pattern.compile("\\s*$", Pattern.MULTILINE);
		final Matcher matcher = pattern.matcher(data);
		data = matcher.replaceAll("");

		return data;
	}

	/**
	 * Scan a stream for the byte positions where text lines start. LF, CR and CRLF linebreaks are supported.
	 * The stream is read to its end but not closed.
	 *
	 * @param input stream to scan
	 * @return start positions of all lines, beginning with 0
	 * @throws IOException if reading fails
	 */
	public static List<Long> scanLineStartIndexes(final InputStream input) throws IOException {
		final List<Long> returnList = new ArrayList<>();
		long position = 0;
		int bufferPrevious = 0;
		int bufferNext;
		returnList.add(0L);
		while ((bufferNext = input.read()) != EOF) {
			if ((char) bufferNext == '\n') {
				returnList.add(position + 1);
			} else if ((char) bufferPrevious == '\r') {
				returnList.add(position);
			}

			bufferPrevious = bufferNext;
			position++;
		}

		if ((char) bufferPrevious == '\r') {
			returnList.add(position);
		}

		return returnList;
	}

	/**
	 * Scan a stream for the byte positions where text lines start, skipping the first bytes up to startIndex.
	 * The stream is read to its end but not closed.
	 *
	 * @param input stream to scan
	 * @param startIndex number of bytes to skip; positions are counted from the start of the stream
	 * @return start positions of the lines at or after startIndex (including 0 if startIndex is 0 or less)
	 * @throws IOException if reading fails
	 */
	public static List<Long> scanLineStartIndexes(final InputStream input, final long startIndex) throws IOException {
		final List<Long> returnList = new ArrayList<>();
		long position = 0;
		int bufferPrevious = 0;
		int bufferNext;
		if (startIndex <= 0) {
			returnList.add(0L);
		} else {
			// skip() may skip less bytes than requested, so skip until the byte before the start index or the end of the stream is reached
			long skipped = 0;
			while (skipped < startIndex - 1) {
				final long newlySkipped = input.skip(startIndex - 1 - skipped);
				if (newlySkipped <= 0) {
					if (input.read() == EOF) {
						break;
					}
					skipped++;
				} else {
					skipped += newlySkipped;
				}
			}
			// The byte before the start index decides, whether a line starts at the start index (before, such a line start was missed)
			if (skipped == startIndex - 1) {
				final int byteBeforeStart = input.read();
				if (byteBeforeStart != EOF) {
					skipped++;
					if ((char) byteBeforeStart == '\n') {
						returnList.add(skipped);
					} else {
						bufferPrevious = byteBeforeStart;
					}
				}
			}
			position = skipped;
		}
		while ((bufferNext = input.read()) != EOF) {
			if ((char) bufferNext == '\n') {
				returnList.add(position + 1);
			} else if ((char) bufferPrevious == '\r') {
				returnList.add(position);
			}

			bufferPrevious = bufferNext;
			position++;
		}

		if ((char) bufferPrevious == '\r') {
			returnList.add(position);
		}

		return returnList;
	}

	/**
	 * Scan a stream for the byte positions of linebreaks. For CRLF the position of the CR is returned.
	 * The stream is read to its end but not closed.
	 *
	 * @param dataInputStream stream to scan
	 * @return positions of all linebreaks
	 * @throws IOException if reading fails
	 */
	public static List<Long> scanLinebreakIndexes(final InputStream dataInputStream) throws IOException {
		final List<Long> returnList = new ArrayList<>();
		long position = 0;
		int bufferPrevious = 0;
		int bufferNext;
		while ((bufferNext = dataInputStream.read()) != EOF) {
			if ((char) bufferNext == '\n' && (char) bufferPrevious != '\r') {
				returnList.add(position);
			} else if ((char) bufferNext == '\r') {
				returnList.add(position);
			}

			bufferPrevious = bufferNext;
			position++;
		}
		return returnList;
	}

	/**
	 * Break all lines after a maximum number of characters by inserting LF linebreaks.
	 *
	 * @param dataString text with LF linebreaks, must not be null
	 * @param maximumLength maximum line length, values less than 1 leave the text unchanged
	 * @return text with no line longer than maximumLength
	 */
	public static String breakLinesAfterMaximumLength(final String dataString, final int maximumLength) {
		if (maximumLength <= 0) {
			return dataString;
		}
		final String[] originalLines = dataString.split("\n", -1);
		final StringBuilder result = new StringBuilder();
		for (int i = 0; i < originalLines.length; i++) {
			final String line = originalLines[i];
			if (line.length() > maximumLength) {
				// Chunks by index: the former split regex ("\\G.{n}") stopped working after characters not matched by '.', like CR or NEL
				result.append(String.join("\n", chopToChunks(line, maximumLength)));
			} else {
				result.append(line);
			}
			if (i < originalLines.length - 1) {
				result.append("\n");
			}
		}
		return result.toString();
	}

	/**
	 * Scan a string for the characters used in it.
	 *
	 * @param data text, must not be null
	 * @return distinct characters in order of their first occurrence
	 */
	public static List<Character> getUsedChars(final String data) {
		final List<Character> usedCharsList = new ArrayList<>();
		for (final char item : data.toCharArray()) {
			if (!usedCharsList.contains(item)) {
				usedCharsList.add(item);
			}
		}
		return usedCharsList;
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
	 * Trim a string to a maximum number of characters by replacing its middle part by a cutoff sign.
	 *
	 * @param inputString text, may be null
	 * @param maxLength maximum length of the result, must not be less than the length of the cutoff sign
	 * @param cutoffSign text replacing the removed middle part, e.g. "..."
	 * @return shortened text, unchanged if it is not longer than maxLength
	 */
	public static String trimStringToMaximumLength(final String inputString, final int maxLength, final String cutoffSign) {
		if (inputString == null || inputString.length() <= maxLength) {
			return inputString;
		} else {
			final int takeFirstLength = (maxLength - cutoffSign.length()) / 2;
			final int takeLastLength = maxLength - cutoffSign.length() - takeFirstLength;
			return inputString.substring(0, takeFirstLength) + cutoffSign + inputString.substring(inputString.length() - takeLastLength);
		}
	}

	/**
	 * Check if a string contains a stringpart, ignoring case.
	 *
	 * @param str text, may be null
	 * @param substr text to search for, may be null
	 * @return true if substr is contained; true if both are null or only substr is null; false if only str is null
	 */
	public static boolean containsIgnoreCase(final String str, final String substr) {
		if (str == substr) {
			return true;
		} else if (str == null) {
			return false;
		} else if (substr == null) {
			return true;
		} else if (substr.length() > str.length()) {
			return false;
		} else {
			// Check every position (before, only position 0 was compared with the full length of str, so it only matched equal strings)
			for (int i = 0; i <= str.length() - substr.length(); i++) {
				if (str.regionMatches(true, i, substr, 0, substr.length())) {
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Get the entries of a map as lines "key: value". Null keys and values are shown as empty strings.
	 *
	 * @param map map to show, must not be null
	 * @return entries separated by LF in iteration order of the map
	 */
	public static String mapToString(final Map<? extends Object, ? extends Object> map) {
		final StringBuilder returnValue = new StringBuilder();
		for (final Entry<? extends Object, ? extends Object> entry : map.entrySet()) {
			if (returnValue.length() > 0) {
				returnValue.append("\n");
			}
			Object key = entry.getKey();
			if (key == null) {
				key = "";
			}
			Object value = entry.getValue();
			if (value == null) {
				value = "";
			}

			returnValue.append(key.toString() + ": " + value.toString());
		}
		return returnValue.toString();
	}

	/**
	 * Get the entries of a map as lines "key: value", sorted by key. Null values are shown as empty strings.
	 *
	 * @param map map to show, must not be null and must not contain a null key
	 * @return entries separated by LF in key order
	 */
	public static String mapToStringWithSortedKeys(final Map<? extends String, ? extends Object> map) {
		final StringBuilder returnValue = new StringBuilder();
		for (String key : Utilities.asSortedList(map.keySet())) {
			@SuppressWarnings("unlikely-arg-type")
			Object value = map.get(key);
			if (returnValue.length() > 0) {
				returnValue.append("\n");
			}
			if (key == null) {
				key = "";
			}
			if (value == null) {
				value = "";
			}

			returnValue.append(key.toString() + ": " + value.toString());
		}
		return returnValue.toString();
	}

	/**
	 * Convert spreadsheet column letters into a column number, e.g. "A" = 1, "Z" = 26, "AA" = 27.
	 *
	 * @param columnChars column letters A to Z, case-insensitive
	 * @return 1-based column number
	 */
	public static int getColumnNumberFromColumnChars(final String columnChars) {
		int value = 0;
		// Locale.ROOT: in Turkish locale "I" would become a dotless i, yielding a wrong column number
		for (final char columnChar : columnChars.toLowerCase(Locale.ROOT).toCharArray()) {
			final int columnIndex = (byte) columnChar - (byte) 'a' + 1;
			value = value * 26 + columnIndex;
		}
		return value;
	}

	/**
	 * Check whether a text is valid standard Base64 with padding and without linebreaks.
	 *
	 * @param value text to check, must not be null
	 * @return true for valid Base64 (also for an empty text)
	 */
	public static boolean isValidBase64(final String value) {
		return Pattern.matches("^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$", value);
	}

	/**
	 * Get the first line of a text starting with a given prefix.
	 *
	 * @param text text to search in, must not be null
	 * @param starter prefix of the line
	 * @return first matching line, or null if there is none
	 * @throws Exception if reading the lines fails
	 */
	public static String getLineStartingWith(final String text, final String starter) throws Exception {
		for (final String line : TextUtilities.getLines(text)) {
			if (line.startsWith(starter)) {
				return line;
			}
		}
		return null;
	}

	/**
	 * Get the value of a parameter line like "name: value" within a text.
	 *
	 * @param text text to search in, may be null
	 * @param parameterName name of the parameter (used literally, not as regular expression)
	 * @return trimmed value of the first matching line, or null if not found
	 */
	public static String getProsaParameter(final String text, final String parameterName) {
		if (Utilities.isBlank(text)) {
			return null;
		} else {
			final Pattern parameterPattern = Pattern.compile("^\\s*" + Pattern.quote(parameterName) + "\\s*:(.*)$", Pattern.MULTILINE);
			final Matcher parameterMatcher = parameterPattern.matcher(text);
			if (parameterMatcher.find()) {
				return parameterMatcher.group(1).trim();
			} else {
				return null;
			}
		}
	}

	/**
	 * Remove all lines containing a given text. The linebreaks of the remaining lines are kept unchanged.
	 *
	 * @param text text, may be null
	 * @param searchLinePart text identifying the lines to remove, must not be null
	 * @return text without the matching lines, or null for null input
	 */
	public static String removeLinesContainingText(final String text, final String searchLinePart) {
		if (text == null) {
			return null;
		}

		// Each line is taken with its own linebreak (LF, CRLF or CR). Before, consecutive linebreaks were grouped wrongly,
		// so an empty line was removed together with a following matching line.
		final StringBuilder resultText = new StringBuilder();
		int lineStart = 0;
		while (lineStart < text.length()) {
			int contentEnd = lineStart;
			while (contentEnd < text.length() && text.charAt(contentEnd) != '\n' && text.charAt(contentEnd) != '\r') {
				contentEnd++;
			}
			int lineEnd = contentEnd;
			if (lineEnd < text.length()) {
				lineEnd += (text.charAt(lineEnd) == '\r' && lineEnd + 1 < text.length() && text.charAt(lineEnd + 1) == '\n') ? 2 : 1;
			}
			if (!text.substring(lineStart, contentEnd).contains(searchLinePart)) {
				resultText.append(text, lineStart, lineEnd);
			}
			lineStart = lineEnd;
		}
		return resultText.toString();
	}

	/**
	 * Check whether a text contains characters outside the 7-bit ASCII range.
	 *
	 * @param text characters to check, must not be null
	 * @return true if any character is greater than 127
	 */
	public static boolean containsNonAsciiCharacters(final char[] text) {
		for (final char nextChar : text) {
			// Control characters like linebreaks and tabs are ASCII, too (before, they were reported as non ASCII)
			if (nextChar > 127) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Fix the encoding of a String if it was stored in UTF-8 encoding but decoded with ISO-8859-1 encoding.
	 *
	 * <p>Examples of byte data of wrongly encoded Umlauts and other special characters:</p>
	 * <pre>
	 *   Ä: [-61, -124]
	 *   ä: [-61, -92]
	 *   ß: [-61, -97]
	 *   è: [-61, -88]
	 * </pre>
	 *
	 * @param text text to fix, must not be null
	 * @return text decoded as UTF-8 if a wrong encoding was detected, otherwise the unchanged text
	 */
	public static String fixEncodingErrorUTF8AsISO8859(final String text) {
		boolean wrongEncodingDetected = false;
		for (final char nextChar : text.toCharArray()) {
			if (nextChar > 0xFF) {
				// Characters outside of ISO-8859-1 cannot result from wrongly decoded bytes, so the text is correct
				return text;
			} else if (nextChar >= 0xC2 && nextChar <= 0xF4) {
				// Lead byte of a UTF-8 multibyte character. Before, only 0xC3 (umlauts) was checked, so e.g. "\u00b0" or "\u20ac" were not fixed.
				wrongEncodingDetected = true;
			}
		}
		if (wrongEncodingDetected) {
			// Only convert if the bytes are valid UTF-8, otherwise the text was correct (e.g. a real "\u00c3")
			try {
				return StandardCharsets.UTF_8.newDecoder()
						.onMalformedInput(CodingErrorAction.REPORT)
						.onUnmappableCharacter(CodingErrorAction.REPORT)
						.decode(ByteBuffer.wrap(text.getBytes(StandardCharsets.ISO_8859_1)))
						.toString();
			} catch (@SuppressWarnings("unused") final CharacterCodingException e) {
				return text;
			}
		} else {
			return text;
		}
	}

	/**
	 * Replace the last occurrence of a text. Search text and replacement are used literally.
	 *
	 * @param text text, must not be null
	 * @param searchText text to replace
	 * @param replacement replacement text
	 * @return text with the last occurrence replaced, unchanged if searchText is not contained
	 */
	public static String replaceLast(final String text, final String searchText, final String replacement) {
		return text.replaceFirst("(?s)" + Pattern.quote(searchText) + "(?!.*?" + Pattern.quote(searchText) + ")", Matcher.quoteReplacement(replacement));
	}

	/**
	 * Remove all HTML tags ("&lt;...&gt;") from a text. HTML entities are not decoded.
	 *
	 * @param data text, must not be null
	 * @return text without tags
	 */
	public static String removeHtmlTags(final String data) {
		String returnValue = data;
		int nextTagStart = returnValue.indexOf("<");
		int nextTagEnd = -1;
		if (nextTagStart > -1) {
			nextTagEnd = returnValue.indexOf(">", nextTagStart + 1);
		}
		while (nextTagStart > -1 && nextTagEnd > -1) {
			returnValue = returnValue.substring(0, nextTagStart) + returnValue.substring(nextTagEnd + 1);
			nextTagStart = returnValue.indexOf("<");
			nextTagEnd = -1;
			if (nextTagStart > -1) {
				nextTagEnd = returnValue.indexOf(">", nextTagStart + 1);
			}
		}
		return returnValue;
	}

	/**
	 * Decode all Java unicode escape sequences (backslash, 'u' and 4 hex digits) within a text.
	 *
	 * @param input text, must not be null
	 * @return decoded text
	 */
	public static String decodeUnicodeEncoding(final String input) {
		final Pattern pattern = Pattern.compile("\\\\u[0-9a-fA-F]{4}");
		final Matcher matcher = pattern.matcher(input);

		final StringBuilder decodedString = new StringBuilder();

		while (matcher.find()) {
			final String unicodeSequence = matcher.group();
			final char unicodeChar = (char) Integer.parseInt(unicodeSequence.substring(2), 16);
			// The replacement must be quoted, because '\\' and '$' have a special meaning in replacement strings
			matcher.appendReplacement(decodedString, Matcher.quoteReplacement(Character.toString(unicodeChar)));
		}

		matcher.appendTail(decodedString);
		return decodedString.toString();
	}

	/**
	 * Get the number of Unicode code points of a text (surrogate pairs count as one character).
	 *
	 * @param input text, must not be null
	 * @return number of code points
	 */
	public static int getUnicodeStringLength(final String input) {
		return input.codePointCount(0, input.length());
	}

	/**
	 * Get the length of the longest line of a text.
	 *
	 * @param text text, must not be null
	 * @return maximum line length in characters
	 * @throws IOException not thrown for string data
	 */
	public static int getMaximumLineLength(final String text) throws IOException {
		int maximum = 0;
		for (final String line : getLines(text)) {
			maximum = Math.max(maximum, line.length());
		}
		return maximum;
	}

	/**
	 * Break all lines of a text after a maximum number of characters. All lines of the result end with LF.
	 *
	 * @param text text, must not be null
	 * @param maxLineLength maximum line length, must be greater than 0
	 * @return text with no line longer than maxLineLength
	 * @throws IOException not thrown for string data
	 */
	public static String ensureMaximumLineLength(final String text, final int maxLineLength) throws IOException {
		if (maxLineLength <= 0) {
			// Avoid an endless loop
			throw new IllegalArgumentException("Invalid maximum line length: " + maxLineLength);
		}
		// StringBuilder instead of String concatenation, which copied the whole text for every line
		final StringBuilder returnText = new StringBuilder(text.length() + text.length() / maxLineLength + 1);
		for (String line : getLines(text)) {
			while (line.length() > maxLineLength) {
				returnText.append(line, 0, maxLineLength).append('\n');
				line = line.substring(maxLineLength);
			}
			returnText.append(line).append('\n');
		}
		return returnText.toString();
	}
}
