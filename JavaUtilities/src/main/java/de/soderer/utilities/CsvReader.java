package de.soderer.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class CsvReader {
	public static final String DEFAULT_ENCODING = "UTF-8";
	public static final char DEFAULT_SEPARATOR = ',';
	public static final char DEFAULT_STRING_QUOTE = '"';
	
	private char separator;
	private char stringQuote;
	private InputStream inputStream;
	private Charset encoding;
	
	private boolean singleReadStarted = false;
	private int numberOfColumns = -1; 
	private char[] readBuffer = new char[1024];
	private int readCount = 0;
	private int readPosition = 0;
	private InputStreamReader inputStreamReader = null;
	private int readLines = 0;

	public CsvReader(InputStream inputStream) {
		this(inputStream, Charset.forName(DEFAULT_ENCODING), DEFAULT_SEPARATOR, DEFAULT_STRING_QUOTE);
	}

	public CsvReader(InputStream inputStream, String encoding) {
		this(inputStream, Charset.forName(encoding), DEFAULT_SEPARATOR, DEFAULT_STRING_QUOTE);
	}
	
	public CsvReader(InputStream inputStream, Charset encoding) {
		this(inputStream, encoding, DEFAULT_SEPARATOR, DEFAULT_STRING_QUOTE);
	}
	
	public CsvReader(InputStream inputStream, char separator) {
		this(inputStream, Charset.forName(DEFAULT_ENCODING), separator, DEFAULT_STRING_QUOTE);
	}
	
	public CsvReader(InputStream inputStream, String encoding, char separator) {
		this(inputStream, Charset.forName(encoding), separator, DEFAULT_STRING_QUOTE);
	}
	
	public CsvReader(InputStream inputStream, Charset encoding, char separator) {
		this(inputStream, encoding, separator, DEFAULT_STRING_QUOTE);
	}
	
	public CsvReader(InputStream inputStream, String encoding, char separator, char stringQuote) {
		this(inputStream, Charset.forName(encoding), separator, stringQuote);
	}
	
	public CsvReader(InputStream inputStream, Charset encoding, char separator, char stringQuote) {
		this.inputStream = inputStream;
		this.encoding = encoding;
		this.separator = separator;
		this.stringQuote = stringQuote;
		
		if (this.encoding == null) {
			throw new IllegalArgumentException("Encoding is null");
		} else if (this.inputStream == null) {
			throw new IllegalArgumentException("InputStream is null");
		} else if (Utilities.anyCharsAreEqual(this.separator, '\r', '\n')) {
			throw new IllegalArgumentException("Separator '" + this.separator + "' is invalid");
		} else if (Utilities.anyCharsAreEqual(this.separator, this.stringQuote, '\r', '\n')) {
			throw new IllegalArgumentException("Stringquote '" + this.stringQuote + "' is invalid");
		}
	}
	
	public List<String> readNextCsvLine() throws IOException {
		readLines++;
		singleReadStarted = true;
		List<String> returnList = new ArrayList<String>();
		StringBuilder nextValue = new StringBuilder();
		boolean insideString = false;
		int nextCharInt = -1;
		while ((nextCharInt = readNextChar()) != -1) {
			char nextChar = (char) nextCharInt;
			if (nextChar == stringQuote) {
				insideString = !insideString;
				nextValue.append(nextChar);
			} else if (!insideString) {
				if (nextChar == '\r' || nextChar == '\n') {
					if (returnList.size() > 0) {
						returnList.add(parseValue(nextValue.toString()));
					}
					if (returnList.size() > 0) {
						if (numberOfColumns != -1 && numberOfColumns != returnList.size()) {
							throw new IllegalStateException("Inconsistent number of values in line " + readLines + " (expected: " + numberOfColumns + " was: " + returnList.size() + ")");
						}
						numberOfColumns = returnList.size();
						return returnList;
					}
				} else if (nextChar == separator) {
					returnList.add(parseValue(nextValue.toString()));
					nextValue = new StringBuilder();
				} else {
					nextValue.append(nextChar);
				}
			} else {
				nextValue.append(nextChar);
			}
		}
		
		if (insideString) {
			close();
			throw new IOException("Unexpected EOF");
		} else {
			if (returnList.size() > 0) {
				returnList.add(parseValue(nextValue.toString()));
			}
		
			if (returnList.size() > 0) {
				if (numberOfColumns != -1 && numberOfColumns != returnList.size()) {
					throw new IllegalStateException("Inconsistent number of values");
				}
				numberOfColumns = returnList.size();
				return returnList;
			} else {
				close();
				return null;
			}
		}
	}
	
	public List<List<String>> readAll() throws IOException {
		if (singleReadStarted) {
			throw new IllegalStateException("Single readNextCsvLine was called before readAll");
		}
		
		try {
			List<List<String>> csvValues = new ArrayList<List<String>>();
			List<String> lineValues;
			while ((lineValues = readNextCsvLine()) != null) {
				csvValues.add(lineValues);
			}
			return csvValues;
		} finally {
			close();
		}
	}
	
	private int readNextChar() throws IOException {
		if (inputStreamReader == null) {
			if (inputStream == null) {
				throw new IllegalStateException("CsvReader is already closed");
			}
			inputStreamReader = new InputStreamReader(inputStream, encoding);
		}
		
		if (readPosition >= readCount) {
			readCount = inputStreamReader.read(readBuffer);
			readPosition = 0;
		}
		
		if (readCount <= 0) {
			return -1;
		} else {
			return (int) readBuffer[readPosition++];
		}
	}
	
	private String parseValue(String rawValue) {
		String returnValue = rawValue;
		
		if (StringUtils.isNotEmpty(returnValue)) {
			if (returnValue.charAt(0) == DEFAULT_STRING_QUOTE
					&& returnValue.charAt(returnValue.length() - 1) == DEFAULT_STRING_QUOTE) {
				returnValue = returnValue.substring(1, returnValue.length() - 1);
			}
			returnValue = returnValue.replace("\"\"", "\"").replace("\r\n", "\n").replace('\r', '\n');
		}

		return returnValue;
	}

	public void close() {
		IOUtils.closeQuietly(inputStreamReader);
		inputStreamReader = null;
		IOUtils.closeQuietly(inputStream);
		inputStream = null;
	}
}
