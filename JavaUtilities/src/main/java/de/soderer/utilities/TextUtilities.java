package de.soderer.utilities;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Allgemeine Utilities
 * 
 * Diese Klasse hat kein Logging via Log4J, da sie meist schon vor dessen Initialisierung genutzt wird
 */
public class TextUtilities {
	public static final String GERMAN_TEST_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"´`^°²³*#.,;:=+-~_|";
	
	public enum LineBreakType {
		Unknown,
		Mixed,
		Unix,
		Mac,
		Windows
	}
	
	public static String LinebreakMac = "\r";
	public static String LinebreakUnix = "\n";
	public static String LinebreakWindows = "\r\n";

	public static String trimStringToLengthAlignedRight(String inputString, int laenge) {
		if (inputString.length() > laenge)
			// nimmt die ersten Zeichen in der Anzahl "laenge"
			return inputString.subSequence(inputString.length() - laenge, inputString.length()).toString();
		else
			return String.format("%" + laenge + "s", inputString);
	}

	public static String trimStringToLengthAlignedLeft(String inputString, int laenge) {
		if (inputString.length() > laenge)
			// nimmt die ersten Zeichen in der Anzahl "laenge"
			return inputString.subSequence(0, laenge).toString();
		else
			return String.format("%-" + laenge + "s", inputString);
	}

	public static String repeatString(String itemString, int repeatTimes) {
		return repeatString(itemString, repeatTimes, null);
	}

	/***
	 * Aufbau eines Strings mit Wiederholungen. 0 Wiederholungen liefert einen Leer-String. Ansonsten wird zwischen jede Wiederholung das Trennzeichen eingesetzt, welches auch leer sein kann.
	 * 
	 * @param itemString
	 *            zu wiederholende Zeichenfolge
	 * @param separatorString
	 *            Trennzeichen
	 * @param repeatTimes
	 *            Anzahl der Wiederholungen
	 * @return
	 */
	public static String repeatString(String itemString, int repeatTimes, String separatorString) {
		StringBuilder returnStringBuilder = new StringBuilder();
		for (int i = 0; i < repeatTimes; i++) {
			if (separatorString != null && returnStringBuilder.length() > 0) {
				returnStringBuilder.append(separatorString);
			}
			returnStringBuilder.append(itemString);
		}
		return returnStringBuilder.toString();
	}

	/***
	 * Aufteilen der Originalliste in kleinere Listen zu je maximal chunkSize-großen Listen
	 * 
	 * @param <E>
	 * @param originalList
	 * @param chunkSize
	 * @return
	 */
	public static List<String> chopToChunks(String text, int chunkSize) {
		List<String> returnList = new ArrayList<String>((text.length() + chunkSize - 1) / chunkSize);

		for (int start = 0; start < text.length(); start += chunkSize) {
			returnList.add(text.substring(start, Math.min(text.length(), start + chunkSize)));
		}

		return returnList;
	}

	public static String trimToMaxNumberOfLines(String value, int maxLines) {
		String normalizedValue = normalizeLineBreaks(value, LineBreakType.Unix);
		int count = 0;
		int nextLinebreak = 0;
		while (nextLinebreak != -1 && count < maxLines) {
			nextLinebreak = normalizedValue.indexOf(LinebreakUnix, nextLinebreak + 1);
			count++;
		}

		if (nextLinebreak != -1) {
			LineBreakType originalLineBreakType = detectLinebreakType(value);
			return normalizeLineBreaks(normalizedValue.substring(0, nextLinebreak + 1), originalLineBreakType) + "...";
		} else {
			return value;
		}
	}

	public static LineBreakType detectLinebreakType(String value) {
		TextPropertiesReader textPropertiesReader = new TextPropertiesReader(value);
		textPropertiesReader.readProperties();
		return textPropertiesReader.getLinebreakType();
	}
	
	public static String normalizeLineBreaks(String value, LineBreakType type) {
		String returnString = value.replace(LinebreakWindows, LinebreakUnix).replace(LinebreakMac, LinebreakUnix);
		if (type == LineBreakType.Mac) {
			return returnString.replace(LinebreakUnix, LinebreakMac);
		} else if (type == LineBreakType.Windows) {
			return returnString.replace(LinebreakUnix, LinebreakWindows);
		} else {
			return returnString;
		}
	}
	
	public static List<String> getLines(String dataString) throws IOException {
		List<String> list = new ArrayList<String>();
		LineNumberReader lineNumberReader = null;
		try {
			lineNumberReader = new LineNumberReader(new StringReader(dataString));
			String line;
			while ((line = lineNumberReader.readLine()) != null) {
				list.add(line);
			}

			return list;
		} finally {
			IOUtils.closeQuietly(lineNumberReader);
		}
	}

	public static int getLineCount(String dataString) throws IOException {
		LineNumberReader lineNumberReader = null;
		try {
			lineNumberReader = new LineNumberReader(new StringReader(dataString));
			while (lineNumberReader.readLine() != null) {
				System.out.println("");
			}

			return lineNumberReader.getLineNumber();
		} finally {
			IOUtils.closeQuietly(lineNumberReader);
		}
	}

	public static int countWords(String dataString) {
		int counter = 0;
		boolean isWord = false;
		int endOfLine = dataString.length() - 1;

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

	public static int getStartIndexOfLine(String dataString, int lineNumber) {
		if (dataString == null || lineNumber < 0) {
			return -1;
		} else if (lineNumber == 1) {
			return 0;
		} else {
			int lineCount = 1;
			int position = 0;
			while (lineCount < lineNumber) {
				int nextLineBreakMac = dataString.indexOf(LinebreakMac, position);
				int nextLineBreakUnix = dataString.indexOf(LinebreakUnix, position);
				int nextLineBreakWindows = dataString.indexOf(LinebreakWindows, position);
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

	public static int getStartIndexOfLineAtIndex(String dataString, int index) {
		if (dataString == null || index < 0) {
			return -1;
		} else if (index == 0) {
			return 0;
		} else {
			int nextLineBreakMac = dataString.lastIndexOf(LinebreakMac, index);
			int nextLineBreakUnix = dataString.lastIndexOf(LinebreakUnix, index);
			int nextLineBreakWindows = dataString.lastIndexOf(LinebreakWindows, index);
			
			if (nextLineBreakMac >= 0 && (nextLineBreakUnix < 0 || nextLineBreakMac < nextLineBreakUnix) && (nextLineBreakWindows < 0 || nextLineBreakMac < nextLineBreakWindows)) {
				return nextLineBreakMac + LinebreakMac.length();
			} else if (nextLineBreakUnix >= 0 && (nextLineBreakWindows < 0 || nextLineBreakUnix < nextLineBreakWindows)) {
				return nextLineBreakUnix + LinebreakUnix.length();
			} else if (nextLineBreakWindows >= 0) {
				return nextLineBreakWindows + LinebreakWindows.length();
			} else {
				return 0;
			}
		}
	}

	public static int getEndIndexOfLineAtIndex(String dataString, int index) {
		if (dataString == null || index < 0) {
			return -1;
		} else if (index == 0) {
			return 0;
		} else {
			int nextLineBreakMac = dataString.indexOf(LinebreakMac, index);
			int nextLineBreakUnix = dataString.indexOf(LinebreakUnix, index);
			int nextLineBreakWindows = dataString.indexOf(LinebreakWindows, index);
			
			if (nextLineBreakMac >= 0 && (nextLineBreakUnix < 0 || nextLineBreakMac > nextLineBreakUnix) && (nextLineBreakWindows < 0 || nextLineBreakMac > nextLineBreakWindows)) {
				return nextLineBreakMac;
			} else if (nextLineBreakUnix >= 0 && (nextLineBreakWindows < 0 || nextLineBreakUnix - 1 > nextLineBreakWindows)) {
				return nextLineBreakUnix;
			} else if (nextLineBreakWindows >= 0) {
				return nextLineBreakWindows;
			} else {
				return dataString.length();
			}
		}
	}

	/**
	 * 
	 * 
	 * @param text
	 * @param startPosition
	 * @param searchTextOrPattern
	 * @param searchTextIsRegularExpression
	 * @param searchCaseSensitive
	 * @param searchReversely
	 * @return Tuple First = Startindex, Second = Length of found substring
	 */
	public static Tuple<Integer, Integer> searchNextPosition(String text, int startPosition, String searchTextOrPattern, boolean searchTextIsRegularExpression, boolean searchCaseSensitive, boolean searchReversely) {
		if (text != null && searchTextOrPattern != null) {
			String fullText = text;
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

			Matcher matcher = searchPattern.matcher(fullText);
			
			if (searchReversely) {
				while (matcher.find()) {
					if (matcher.start() < startPosition) {
						nextPosition = matcher.start();
						length = matcher.end() - nextPosition;
					} else {
						break;
					}
				}
				if (nextPosition < 0) {
					if (matcher.start() > 0) {
						nextPosition = matcher.start();
						length = matcher.end() - nextPosition;
					}
					while (matcher.find()) {
						nextPosition = matcher.start();
						length = matcher.end() - nextPosition;
					}
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
				return new Tuple<Integer, Integer>(nextPosition, length);
			}
		} else {
			return null;
		}
	}

	public static int countOccurences(String text, String searchTextOrPattern, boolean searchTextIsRegularExpression, boolean searchCaseSensitive) {
		if (text != null && searchTextOrPattern != null) {
			String fullText = text;
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

			Matcher matcher = searchPattern.matcher(fullText);
			while (matcher.find()) {
				occurences++;
			}
			return occurences;
		} else {
			return -1;
		}
	}
	
	public static int getLineNumberOfTextposition(String dataString, int textPosition) {
		if (dataString == null) {
			return -1;
		} else {
			try {
				String textPart = dataString.substring(0, textPosition);
				int lineNumber = getLineCount(textPart);
				if (textPart.endsWith(LinebreakUnix) || textPart.endsWith(LinebreakMac)) {
					lineNumber++;
				}
				return lineNumber;
			} catch (IOException e) {
				return -1;
			}
		}
	}
	
	public static int getNumberOfLineContainingText(String dataString, String searchTextOrPattern, boolean searchCaseSensitive, boolean searchTextIsRegularExpression) throws IOException {
		LineNumberReader lineNumberReader = null;
		try {
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
			
			lineNumberReader = new LineNumberReader(new StringReader(dataString));
			String lineContent;
			while ((lineContent = lineNumberReader.readLine()) != null) {
				Matcher matcher = searchPattern.matcher(lineContent);
				if (matcher.find()) {
					return lineNumberReader.getLineNumber();
				}
			}
			return -1;
		} finally {
			IOUtils.closeQuietly(lineNumberReader);
		}
	}
	
	public static String insertTextBeforeLine(String dataString, String insertText, int lineNumber) throws IOException {
		if (lineNumber > -1) {
			int startIndex = TextUtilities.getStartIndexOfLine(dataString, lineNumber);
			StringBuilder dataBuilder = new StringBuilder(dataString);
			dataBuilder.insert(startIndex, insertText);
			return dataBuilder.toString();
		} else {
			return dataString;
		}
	}
	
	public static String insertTextAfterLine(String dataString, String insertText, int lineNumber) throws IOException {
		if (lineNumber > -1) {
			int startIndex = TextUtilities.getStartIndexOfLine(dataString, lineNumber + 1);
			StringBuilder dataBuilder = new StringBuilder(dataString);
			dataBuilder.insert(startIndex, insertText);
			return dataBuilder.toString();
		} else {
			return dataString;
		}
	}

	public static Tuple<String, Boolean> detectEncoding(byte[] data) throws UnsupportedEncodingException {
		if (data.length > 2 && data[0] == Utilities.UTF_16_BE_BOM[0] && data[1] == Utilities.UTF_16_BE_BOM[1]) {
			return new Tuple<String, Boolean>("UTF-16BE", true);
		} else if (data.length > 2 && data[0] == Utilities.UTF_16_LE_BOM[0] && data[1] == Utilities.UTF_16_LE_BOM[1]) {
			return new Tuple<String, Boolean>("UTF-16LE", true);
		} else if (data.length > 3 && data[0] == Utilities.UTF_8_BOM[0] && data[1] == Utilities.UTF_8_BOM[1] && data[2] == Utilities.UTF_8_BOM[2]) {
			return new Tuple<String, Boolean>("UTF-8", true);
		} else {
			// Detect Xml Encoding
			try {
				// Use first data part only to speed up
				String interimString = new String(data, 0, Math.min(data.length, 100), "UTF-8").toLowerCase();
				String reducedInterimString = interimString.replace("\u0000", "");
				int encodingStart = reducedInterimString.indexOf("encoding");
				if (encodingStart >= 0) {
					encodingStart = reducedInterimString.indexOf("=", encodingStart);
					if (encodingStart >= 0) {
						encodingStart++;
						while (reducedInterimString.charAt(encodingStart) == ' ') {
							encodingStart++;
						}
						if (reducedInterimString.charAt(encodingStart) == '"' || reducedInterimString.charAt(encodingStart) == '\'') {
							encodingStart++;
						}
						StringBuilder encodingString = new StringBuilder();
						while (Character.isLetter(reducedInterimString.charAt(encodingStart)) || Character.isDigit(reducedInterimString.charAt(encodingStart)) || reducedInterimString.charAt(encodingStart) == '-') {
							encodingString.append(reducedInterimString.charAt(encodingStart));
							encodingStart++;
						}
						Charset.forName(encodingString.toString());
						if (encodingString.toString().startsWith("utf-16") && data[0] == 0) {
							return new Tuple<String, Boolean>("UTF-16BE", false);
						} else if (encodingString.toString().startsWith("utf-16") && data[1] == 0) {
							return new Tuple<String, Boolean>("UTF-16LE", false);
						} else {
							return new Tuple<String, Boolean>(encodingString.toString().toUpperCase(), false);
						}
					}
				}
			} catch (Exception e) {
			}
			
			String interimString = new String(data, "UTF-8").toLowerCase();
			int zeroIndex = interimString.indexOf('\u0000');
			if (zeroIndex >= 0) {
				if (zeroIndex % 2 == 0) {
					return new Tuple<String, Boolean>("UTF-16BE", false);
				} else {
					return new Tuple<String, Boolean>("UTF-16LE", false);
				}
			}
			
			return null;
		}
	}
	
	public static String replaceIndentingBlanks(String data, int blankCount, String replacement) {
		String result = data;
		Pattern pattern = Pattern.compile("^(  )+", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(result);
		while (matcher.find()) {
			int replacementCount = (matcher.end() - matcher.start()) / blankCount;
			result = matcher.replaceFirst(StringUtils.repeat(replacement, replacementCount));
			matcher = pattern.matcher(result);
		}
		return result;
	}

	public static String removeLeadingBlank(String data) {
		Pattern pattern = Pattern.compile("^\\s", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(data);
		return matcher.replaceAll("");
	}

	public static String addLeadingTab(String data) {
		return addLeadingTabs(data, 1);
	}

	public static String addLeadingTabs(String data, int numberOfTabs) {
		Pattern pattern = Pattern.compile("^", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(data);
		return matcher.replaceAll(StringUtils.repeat("\t", numberOfTabs));
	}
	
	public static List<String> truncate(String value, int length) {
		List<String> parts = new ArrayList<String>();
		int valueLength = value.length();
		for (int i = 0; i < valueLength; i += length) {
			parts.add(value.substring(i, Math.min(valueLength, i + length)));
		}
		return parts;
	}
	
	public static List<String> truncate(String[] lines, int length) {
		List<String> linesTruncated = new ArrayList<String>();
		for (String line : lines) {
			linesTruncated.addAll(truncate(line, length));
		}
		return linesTruncated;
	}
	
	public static List<String> truncate(List<String> lines, int length) {
		List<String> linesTruncated = new ArrayList<String>();
		for (String line : lines) {
			linesTruncated.addAll(truncate(line, length));
		}
		return linesTruncated;
	}
}