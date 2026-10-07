package de.soderer.utilities;

import java.util.HashMap;
import java.util.Map;

import de.soderer.utilities.TextUtilities.LineBreak;

/**
 * Scanner for statistical properties of a text: number of characters, non whitespace characters, lines and words,
 * the characters used and the type of line breaks.
 * Call {@link #readProperties()} before using the getters; until then they return -1 (or null).
 */
public class TextPropertiesReader {
	/** Text to scan */
	protected String dataString = null;

	private int characters = -1;
	private Map<Character, Integer> foundCharacters = null;
	private int nonWhitespaceCharacters = -1;
	private int lines = -1;
	private int words = -1;
	private LineBreak linebreaks = LineBreak.Unknown;

	/**
	 * Internal constructor for derived classes, which set {@link #dataString} themselves
	 */
	protected TextPropertiesReader() {
	}

	/**
	 * Creates a scanner for a text, which is scanned by {@link #readProperties()} later on.
	 *
	 * @param dataString text to scan, must not be null when scanning
	 */
	public TextPropertiesReader(String dataString) {
		this.dataString = dataString;
	}

	/**
	 * Scans the text and determines its properties.
	 * Lines are separated by "\n", "\r\n" or "\r"; a final line without line break is counted too.
	 * Words are sequences of non whitespace characters.
	 */
	public void readProperties() {
		foundCharacters = new HashMap<>();
		int lineCount = 0;
		int nonWhitespaceCharactersCount = 0;
		int wordCount = 0;
		boolean foundLineBreakUnix = false;
		boolean foundLineBreakMac = false;
		boolean foundLineBreakWindows = false;
		boolean isWithinWord = false;

		for (int i = 0; i < dataString.length(); i++) {
			char character = dataString.charAt(i);

			foundCharacters.merge(character, 1, Integer::sum);

			if (character == '\r') {
				if (i + 1 < dataString.length() && dataString.charAt(i + 1) == '\n') {
					i++;
					// The skipped '\n' is part of the text as well
					foundCharacters.merge('\n', 1, Integer::sum);
					foundLineBreakWindows = true;
				} else {
					foundLineBreakMac = true;
				}

				if (isWithinWord) {
					wordCount++;
				}
				isWithinWord = false;

				lineCount++;
			} else if (character == '\n') {
				foundLineBreakUnix = true;

				if (isWithinWord) {
					wordCount++;
				}
				isWithinWord = false;

				lineCount++;
			} else if (!Character.isWhitespace(character)) {
				nonWhitespaceCharactersCount++;

				isWithinWord = true;

				if (i == dataString.length() - 1) {
					lineCount++;
				}
			} else {
				if (isWithinWord) {
					wordCount++;
				}
				isWithinWord = false;

				if (i == dataString.length() - 1) {
					lineCount++;
				}
			}
		}

		if (isWithinWord) {
			wordCount++;
		}

		if ((foundLineBreakUnix && foundLineBreakMac) || (foundLineBreakWindows && foundLineBreakMac) || (foundLineBreakUnix && foundLineBreakWindows)) {
			linebreaks = LineBreak.Mixed;
		} else if (foundLineBreakUnix) {
			linebreaks = LineBreak.Unix;
		} else if (foundLineBreakMac) {
			linebreaks = LineBreak.Mac;
		} else if (foundLineBreakWindows) {
			linebreaks = LineBreak.Windows;
		} else {
			linebreaks = LineBreak.Unknown;
		}

		lines = lineCount;
		characters = dataString.length();
		nonWhitespaceCharacters = nonWhitespaceCharactersCount;
		words = wordCount;
	}

	/**
	 * Number of characters of the text.
	 *
	 * @return number of characters, or -1 if not scanned yet
	 */
	public int getCharactersCount() {
		return characters;
	}

	/**
	 * Characters used in the text and their numbers of occurrences.
	 *
	 * @return map of characters and their counts, or null if not scanned yet
	 */
	public Map<Character, Integer> getFoundCharacters() {
		return foundCharacters;
	}

	/**
	 * Number of non whitespace characters of the text.
	 *
	 * @return number of non whitespace characters, or -1 if not scanned yet
	 */
	public int getNonWhitespaceCharactersCount() {
		return nonWhitespaceCharacters;
	}

	/**
	 * Number of lines of the text.
	 *
	 * @return number of lines, or -1 if not scanned yet
	 */
	public int getLinesCount() {
		return lines;
	}

	/**
	 * Number of words (separated by whitespace) of the text.
	 *
	 * @return number of words, or -1 if not scanned yet
	 */
	public int getWordsCount() {
		return words;
	}

	/**
	 * Type of line breaks used in the text.
	 *
	 * @return line break type, {@link LineBreak#Mixed} for different types, {@link LineBreak#Unknown} if there are none or not scanned yet
	 */
	public LineBreak getLinebreakType() {
		return linebreaks;
	}
}
