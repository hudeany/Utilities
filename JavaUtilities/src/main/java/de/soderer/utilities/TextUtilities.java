package de.soderer.utilities;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

/**
 * Allgemeine Utilities
 * 
 * Diese Klasse hat kein Logging via Log4J, da sie meist schon vor dessen Initialisierung genutzt wird
 */
public class TextUtilities {
	public static final String GERMAN_TEST_STRING = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 äöüßÄÖÜµ!?§@€$%&/\\<>(){}[]'\"´`^°²³*#.,;:=+-~_|";

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
		value = normalizeLineBreaks(value);
		int count = 0;
		int nextLinebreak = 0;
		while (nextLinebreak != -1 && count < maxLines) {
			nextLinebreak = value.indexOf("\n", nextLinebreak + 1);
			count++;
		}

		if (nextLinebreak != -1) {
			return value.substring(0, nextLinebreak + 1) + "...";
		} else {
			return value;
		}
	}

	public static String normalizeLineBreaks(String value) {
		return value.replace("\r\n", "\n").replace("\r", "\n");
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
		if (dataString == null || lineNumber <= 1) {
			return 0;
		} else {
			int lineCount = 1;
			int position = 0;
			while (lineCount < lineNumber) {
				int nextLineBreakMac = dataString.indexOf("\r", position);
				int nextLineBreakUnix = dataString.indexOf("\n", position);
				int nextLineBreakWindows = dataString.indexOf("\r\n", position);
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
	public static Tuple<Integer, Integer> searchNextPosition(String text, int startPosition, String searchTextOrPattern, boolean searchTextIsRegularExpression, boolean searchCaseSensitive,
			boolean searchReversely) {
		if (text != null && searchTextOrPattern != null) {
			String fullText = text;
			String textToSearch = searchTextOrPattern;

			int nextPosition = -1;
			if (searchTextIsRegularExpression) {
				int length = -1;
				Pattern searchPattern;
				if (!searchCaseSensitive) {
					searchPattern = Pattern.compile(textToSearch, Pattern.CASE_INSENSITIVE);
				} else {
					searchPattern = Pattern.compile(textToSearch);
				}
				if (searchReversely) {
					Matcher matcher = searchPattern.matcher(fullText);
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

					if (nextPosition < 0) {
						return null;
					} else {
						return new Tuple<Integer, Integer>(nextPosition, length);
					}
				} else {
					Matcher matcher = searchPattern.matcher(fullText);
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
					return new Tuple<Integer, Integer>(nextPosition, length);
				}
			} else {
				if (!searchCaseSensitive) {
					fullText = fullText.toLowerCase();
					textToSearch = textToSearch.toLowerCase();
				}
				if (searchReversely) {
					fullText = new StringBuilder(fullText).reverse().toString();
					textToSearch = new StringBuilder(textToSearch).reverse().toString();
					startPosition = fullText.length() - startPosition - textToSearch.length() + 1;
					nextPosition = fullText.indexOf(textToSearch, startPosition);
					if (startPosition > 0 && nextPosition < 0) {
						nextPosition = fullText.indexOf(textToSearch, 0);
					}
					if (nextPosition >= 0) {
						nextPosition = fullText.length() - nextPosition - textToSearch.length();
						return new Tuple<Integer, Integer>(nextPosition, textToSearch.length());
					} else {
						return null;
					}
				} else {
					nextPosition = fullText.indexOf(textToSearch, startPosition);
					if (startPosition > 0 && nextPosition < 0) {
						nextPosition = fullText.indexOf(textToSearch, 0);
					}
					if (nextPosition >= 0) {
						return new Tuple<Integer, Integer>(nextPosition, textToSearch.length());
					} else {
						return null;
					}
				}
			}
		} else {
			return null;
		}
	}

	public static int countOccurences(String text, String searchTextOrPattern, boolean searchTextIsRegularExpression, boolean searchCaseSensitive) {
		if (text != null && searchTextOrPattern != null) {
			String fullText = text;
			String textToSearch = searchTextOrPattern;
			int occurences = 0;

			if (searchTextIsRegularExpression) {
				Pattern searchPattern;
				if (!searchCaseSensitive) {
					searchPattern = Pattern.compile(textToSearch, Pattern.CASE_INSENSITIVE);
				} else {
					searchPattern = Pattern.compile(textToSearch);
				}
				Matcher matcher = searchPattern.matcher(fullText);
				while (matcher.find()) {
					occurences++;
				}
				return occurences;
			} else {
				if (!searchCaseSensitive) {
					fullText = fullText.toLowerCase();
					textToSearch = textToSearch.toLowerCase();
				}

				int nextPosition = 0;
				while (nextPosition != -1) {
					nextPosition = fullText.indexOf(textToSearch, nextPosition);
					if (nextPosition >= 0) {
						occurences++;
						nextPosition++;
					}
				}
				return occurences;
			}
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
				if (textPart.endsWith("\n") || textPart.endsWith("\r")) {
					lineNumber++;
				}
				return lineNumber;
			} catch (IOException e) {
				return -1;
			}
		}
	}
	
	public static int getNumberOfLineContainingText(String dataString, String searchText, boolean searchCaseSensitive, boolean searchTextIsRegularExpression) throws IOException {
		LineNumberReader lineNumberReader = null;
		try {
			Pattern searchPattern;
			if (searchTextIsRegularExpression) {
				if (searchCaseSensitive) {
					searchPattern = Pattern.compile(searchText);
				} else {
					searchPattern = Pattern.compile(searchText, Pattern.CASE_INSENSITIVE);
				}
			} else {
				if (searchCaseSensitive) {
					searchPattern = Pattern.compile(searchText, Pattern.LITERAL);
				} else {
					searchPattern = Pattern.compile(searchText, Pattern.CASE_INSENSITIVE | Pattern.LITERAL);
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
}