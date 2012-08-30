package de.soderer.utilities;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

public class TextFilePropertiesReader {
	private static final Charset[] CHARSETS_TO_CHECK = new Charset[] {
		Charset.forName("UTF-8"),
		Charset.forName("ISO-8859-1"),
		Charset.forName("UTF-16"),
		Charset.forName("ISO-8859-15"),
		Charset.forName("ASCII")
	};
	
	private File file;
	private String encoding = null;
	
	private long filesize = -1;
	private int characters = -1;
	private Map<Character, Integer> foundCharacters = null;
	private int nonWhitespaceCharacters = -1;
	private int lines = -1;
	private int words = -1;
	private TextPropertiesReader.LineBreakType linebreaks = TextPropertiesReader.LineBreakType.Unknown;

	public TextFilePropertiesReader(String filePath, String encoding) throws Exception {
		this(new File(filePath), encoding);
	}
	
	public TextFilePropertiesReader(String filePath) throws Exception {
		this(new File(filePath));
	}
	
	public TextFilePropertiesReader(File file) throws Exception {
		this(file, null);
	}
	
	public TextFilePropertiesReader(File file, String encoding) throws Exception {
		this.file = file;
		
		if (!file.exists()) {
			throw new Exception("File does not exist");
		} else if (!file.isFile()) {
			throw new Exception("Path isn't a file");
		}
		
		if (StringUtils.isBlank(encoding)) {
			this.encoding = null;
		} else {
			try {
				this.encoding = Charset.forName(encoding).name();
			} catch (Exception e) {
				throw new Exception("Invalid encoding: '" + encoding + "'");
			}
		}
	}
	
	public String readFileToString() throws IOException {
		filesize = file.length();
		foundCharacters = new HashMap<Character, Integer>();
		byte[] data = FileUtils.readFileToByteArray(file);
		if (encoding != null) {
			String dataString = new String(data, encoding);
			readPropertiesOfString(dataString);
			return dataString;
		} else {
			for (Charset charset : CHARSETS_TO_CHECK) {
				String dataString = new String(data, charset);
				if (!dataString.contains("�")) {
					encoding = charset.name();
					readPropertiesOfString(dataString);
					return dataString;
				}
			}
			
			// File doesn't fit any given charset
			String dataString = new String(data, Charset.forName("UTF-8"));
			readPropertiesOfString(dataString);
			return dataString;
		}
	}
	
	private void readPropertiesOfString(String dataString) {
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
			linebreaks = TextPropertiesReader.LineBreakType.Mixed;
		} else if (foundLineBreakUnix) {
			linebreaks = TextPropertiesReader.LineBreakType.Unix;
		} else if (foundLineBreakMac) {
			linebreaks = TextPropertiesReader.LineBreakType.Mac;
		} else if (foundLineBreakWindows) {
			linebreaks = TextPropertiesReader.LineBreakType.Windows;
		} else {
			linebreaks = TextPropertiesReader.LineBreakType.Unknown;
		}
		
		lines = lineCount;
		characters = dataString.length();
		nonWhitespaceCharacters = nonWhitespaceCharactersCount;
		words = wordCount;
	}

	public long getFilesize() {
		return filesize;
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

	public TextPropertiesReader.LineBreakType getLinebreakType() {
		return linebreaks;
	}

	public String getEncoding() {
		return encoding;
	}
	
	public static List<Charset> detectTextEncoding(File file, List<Charset> charsetsToCheck) throws IOException {
		return detectTextEncoding(file, charsetsToCheck, null);
	}
	
	public static List<Charset> detectTextEncoding(File file, List<Charset> charsetsToCheck, Charset defaultEncoding) throws IOException {
		byte[] data = FileUtils.readFileToByteArray(file);
		return detectTextEncoding(data, charsetsToCheck, defaultEncoding);
	}
	
	public static List<Charset> detectTextEncoding(byte[] data, List<Charset> charsetsToCheck) {
		return detectTextEncoding(data, charsetsToCheck, null);
	}
	
	public static List<Charset> detectTextEncoding(byte[] data, List<Charset> charsetsToCheck, Charset defaultEncoding) {
		List<Charset> validEncodings = new ArrayList<Charset>();
		if (data.length > 2 && ((BitUtilities.getUnsignedValue(data[0]) == 0xFF && BitUtilities.getUnsignedValue(data[1]) == 0xFE) || (BitUtilities.getUnsignedValue(data[0]) == 0xFE && BitUtilities.getUnsignedValue(data[1]) == 0xFF))) {
			// Detected UTF-16 BOM (FF FE or FE FF)
			validEncodings.add(Charset.forName("UTF-16"));
			return validEncodings;
		} else if (data.length > 3 && BitUtilities.getUnsignedValue(data[0]) == 0xEF && BitUtilities.getUnsignedValue(data[1]) == 0xBB && BitUtilities.getUnsignedValue(data[2]) == 0xBF) {
			// Detected UTF-8 BOM (EF BB BF)
			validEncodings.add(Charset.forName("UTF-8"));
			return validEncodings;
		}
		
		for (Charset charset : charsetsToCheck) {
			String dataString = new String(data, charset);
			if (!dataString.contains("�")) {
				if (defaultEncoding != null && charset == defaultEncoding) {
					validEncodings = new ArrayList<Charset>();
					validEncodings.add(defaultEncoding);
					return validEncodings;
				} else {
					validEncodings.add(charset);
				}
			}
		}
		
		if (validEncodings.size() == 0) {
			return null;
		} else if (validEncodings.size() == 1) {
			return validEncodings;
		} else {
			return validEncodings;
		}
	}
}
