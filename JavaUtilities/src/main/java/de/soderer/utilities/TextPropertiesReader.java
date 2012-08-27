package de.soderer.utilities;

import java.util.HashMap;
import java.util.Map;

public class TextPropertiesReader {
	public enum LineBreakType {
		Unknown,
		Mixed,
		Unix,
		Mac,
		Windows
	}
	
	private String dataString;
	private int characters = -1;
	private Map<Character, Integer> foundCharacters = null;
	private int nonWhitespaceCharacters = -1;
	private int lines = -1;
	private int words = -1;
	private LineBreakType linebreaks = LineBreakType.Unknown;

	public TextPropertiesReader(String dataString) throws Exception {
		this.dataString = dataString;
	}
	
	public void readProperties() {
		foundCharacters = new HashMap<Character, Integer>();
		int lineCount = 0;
		int nonWhitespaceCharactersCount = 0;
		int wordCount = 0;
		boolean foundLineBreakUnix = false;
		boolean foundLineBreakMac = false;
		boolean foundLineBreakWindows = false;
		boolean isWithinWord = false;
		
		for (int i = 0; i < dataString.length(); i++) {
			char character = dataString.charAt(i);
			
			if (foundCharacters.containsKey(character)) {
				foundCharacters.put(character, foundCharacters.get(character) + 1);
			} else {
				foundCharacters.put(character, 1);
			}
				
			if (character == '\r') {
				if (dataString.charAt(i + 1) == '\n') {
					i++;
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
			linebreaks = LineBreakType.Mixed;
		} else if (foundLineBreakUnix) {
			linebreaks = LineBreakType.Unix;
		} else if (foundLineBreakMac) {
			linebreaks = LineBreakType.Mac;
		} else if (foundLineBreakWindows) {
			linebreaks = LineBreakType.Windows;
		} else {
			linebreaks = LineBreakType.Unknown;
		}
		
		lines = lineCount;
		characters = dataString.length();
		nonWhitespaceCharacters = nonWhitespaceCharactersCount;
		words = wordCount;
	}

	public int getCharactersCount() {
		return characters;
	}

	public Map<Character, Integer> getFoundCharacters() {
		return foundCharacters;
	}

	public int getNonWhitespaceCharactersCount() {
		return nonWhitespaceCharacters;
	}

	public int getLinesCount() {
		return lines;
	}

	public int getWordsCount() {
		return words;
	}

	public LineBreakType getLinebreakType() {
		return linebreaks;
	}
}
