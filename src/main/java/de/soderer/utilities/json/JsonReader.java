package de.soderer.utilities.json;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.regex.Pattern;

public class JsonReader implements Closeable {
	/** Default input encoding. */
	public static final String DEFAULT_ENCODING = "UTF-8";
	
	/** Input stream. */
	private InputStream inputStream;

	/** Input encoding. */
	private Charset encoding;

	/** Input reader. */
	private BufferedReader inputReader = null;
	
	private long readCharacters = 0;
	
	private boolean isFirstRootArrayObjectRead = false;

	public JsonReader(InputStream inputStream) {
		this(inputStream, null);
	}
	
	public JsonReader(InputStream inputStream, String encoding) {
		this.inputStream = inputStream;
		this.encoding = isBlank(encoding) ? Charset.forName(DEFAULT_ENCODING) : Charset.forName(encoding);
	}
	
	/**
	 * Read all available Json data from the input stream at once
	 * 
	 * @return JsonObject or JsonArray
	 * @throws Exception
	 */
	public Object read() throws Exception {
		if (inputReader == null) {
			inputReader = new BufferedReader(new InputStreamReader(inputStream, encoding));
		} else {
			throw new Exception("JsonReader position was already initialized for readNextRootArrayJsonItem()");
		}
		
		char nextChar = readNextNonWhitespace(inputReader);
		if (nextChar == '{') {
			// Read root Json object
			Object returnObject = readJsonObject(inputReader);
			inputReader = null;
			return returnObject;
		} else if (nextChar == '[') {
			// Read root Json array
			Object returnObject = readJsonArray(inputReader);
			inputReader = null;
			return returnObject;
		} else {
			inputReader = null;
			throw new Exception("Invalid json data: No Json object or Json array at root");
		}
	}

	/**
	 * If a Json data stream contains a big amount of data, it can be read object by object.
	 * This only works if at the first level there is a Json array containing these data items which must be of type Json object or Json array.
	 * No simple values are allowed there.
	 * <br />
	 * <br />
	 * Usage:<br />
	 * First open the Json array with openRootArray()<br />
	 * Read Json items with readNextRootArrayJsonItem() until null is returned<br />
	 * 
	 * @throws Exception
	 */
	public void openRootArray() throws Exception {
		if (inputReader == null) {
			inputReader = new BufferedReader(new InputStreamReader(inputStream, encoding));
		} else {
			throw new Exception("JsonReader position was already initialized for readNextRootArrayJsonItem()");
		}
		
		char nextChar = readNextNonWhitespace(inputReader);
		if (nextChar == '[') {
			isFirstRootArrayObjectRead = true;
			return;
		} else {
			throw new Exception("Invalid json data: No Json array at root");
		}
	}
	
	/**
	 * If a Json data stream contains a big amount of data, it can be read object by object.
	 * This only works if at the first level there is a Json array containing these data items which must be of type Json object or Json array.
	 * No simple values are allowed there.
	 * <br />
	 * <br />
	 * Usage:<br />
	 * First open the Json array with openRootArray()<br />
	 * Read Json items with readNextRootArrayJsonItem() until null is returned<br />
	 * 
	 * @return null if there is no more Json item left to read
	 * @throws Exception
	 */
	public Object readNextRootArrayJsonItem() throws Exception {
		if (inputReader == null) {
			throw new Exception("JsonReader position was not initialized for readNextRootArrayJsonItem()");
		}
		
		char nextChar = readNextNonWhitespace(inputReader);
		
		if (!isFirstRootArrayObjectRead && nextChar == ',') {
			nextChar = readNextNonWhitespace(inputReader);
		}
		
		isFirstRootArrayObjectRead = false;
		
		if (nextChar == ']') {
			inputReader = null;
			return null;
		} else if (nextChar == '{') {
			return readJsonObject(inputReader);
		} else if (nextChar == '[') {
			return readJsonArray(inputReader);
		} else {
			inputReader = null;
			throw new Exception("Invalid json data: No Json object or Json array in root array");
		}
	}
	
	private JsonObject readJsonObject(BufferedReader inputReader) throws Exception {
		JsonObject jsonObject = new JsonObject();
		char nextChar = readNextNonWhitespace(inputReader);
		if (nextChar == '}') {
			// Empty Json object
			return jsonObject;
		} else {
			while (true) {
				if (nextChar == '"') {
					// Read Json object property key
					String key = readQuotedText(inputReader);
					nextChar = readNextNonWhitespace(inputReader);
					if (nextChar != ':') {
						throw new Exception("Invalid json data at index " + readCharacters);
					}
					// Read Json object property value
					nextChar = readNextNonWhitespace(inputReader);
					Object value;
					if (nextChar == '{') {
						// Read Json object
						value = readJsonObject(inputReader);
						nextChar = readNextNonWhitespace(inputReader);
					} else if (nextChar == '[') {
						// Read Json array
						value = readJsonArray(inputReader);
						nextChar = readNextNonWhitespace(inputReader);
					} else if (nextChar == '"') {
						value = readQuotedText(inputReader);
						nextChar = readNextNonWhitespace(inputReader);
					} else {
						String rawValueString = readUpToNext(inputReader, ',', '}').trim();
						value = readSimpleJsonValue(nextChar + rawValueString.substring(0, rawValueString.length() - 1));
						nextChar = rawValueString.charAt(rawValueString.length() - 1);
					}
					
					jsonObject.add(key, value);
					
					if (nextChar == '}') {
						return jsonObject;
					} else if (nextChar == ',') {
						nextChar = readNextNonWhitespace(inputReader);
					} else {
						throw new Exception("Invalid json data at index " + readCharacters);
					}
				} else {
					throw new Exception("Invalid json data at index " + readCharacters);
				}
			}
		}
	}
	
	private JsonArray readJsonArray(BufferedReader inputReader) throws Exception {
		JsonArray jsonArray = new JsonArray();
		char nextChar = readNextNonWhitespace(inputReader);
		if (nextChar == ']') {
			// Empty Json array
			return jsonArray;
		} else {
			while (true) {
				// Read Json array value
				Object value;
				if (nextChar == '{') {
					// Read Json object
					value = readJsonObject(inputReader);
					nextChar = readNextNonWhitespace(inputReader);
				} else if (nextChar == '[') {
					// Read Json array
					value = readJsonArray(inputReader);
					nextChar = readNextNonWhitespace(inputReader);
				} else if (nextChar == '"') {
					value = readQuotedText(inputReader);
					nextChar = readNextNonWhitespace(inputReader);
				} else {
					String rawValueString = readUpToNext(inputReader, ',', ']').trim();
					value = readSimpleJsonValue(nextChar + rawValueString.substring(0, rawValueString.length() - 1));
					nextChar = rawValueString.charAt(rawValueString.length() - 1);
				}
				
				jsonArray.add(value);
				
				if (nextChar == ']') {
					return jsonArray;
				} else if (nextChar == ',') {
					nextChar = readNextNonWhitespace(inputReader);
				} else {
					throw new Exception("Invalid json data at index " + readCharacters);
				}
			}
		}
	}

	private char readNextNonWhitespace(BufferedReader inputReader) throws Exception {
		char nextChar;
		while ((nextChar = (char) inputReader.read()) != -1) {
			readCharacters++;
			if (!Character.isWhitespace(nextChar)) {
				return nextChar;
			}
		}
		throw new Exception("Invalid json data: premature end of data");
	}

	private String readQuotedText(BufferedReader inputReader) throws Exception {
		StringBuilder returnValue = new StringBuilder();
		boolean escapeNextChar = false;
		char nextChar;
		while ((nextChar = (char) inputReader.read()) != -1) {
			readCharacters++;
			if (nextChar == '"' && !escapeNextChar) {
				return returnValue.toString();
			} else if (nextChar == '\\' && !escapeNextChar) {
				escapeNextChar = true;
			} else {
				returnValue.append(nextChar);
				escapeNextChar = false;
			}
		}
		throw new Exception("Invalid json data: premature end of data");
	}

	private String readUpToNext(BufferedReader inputReader, char... endChars) throws Exception {
		StringBuilder returnValue = new StringBuilder();
		char nextChar;
		while ((nextChar = (char) inputReader.read()) != -1) {
			readCharacters++;
			returnValue.append(nextChar);
			for (char endChar : endChars) {
				if (endChar == nextChar) {
					return returnValue.toString();
				}
			}
		}
		throw new Exception("Invalid json data: premature end of data");
	}

	private Object readSimpleJsonValue(String valueString) throws Exception {
		if (valueString.equalsIgnoreCase("null")) {
			return null;
		} else if (valueString.equalsIgnoreCase("true")) {
			return true;
		} else if (valueString.equalsIgnoreCase("false")) {
			return false;
		} else if (Pattern.matches("[+|-]?[0-9]*(\\.[0-9]*)?([e|E][+|-]?[0-9]*)?", valueString)) {
			if (valueString.contains(".")) {
				return new Double(valueString);
			} else {
				return new Long(valueString);
			}
		} else {
			throw new Exception("Invalid json data at index " + readCharacters);
		}
	}

	/**
	 * Close this writer and its underlying stream.
	 */
	@Override
	public void close() throws IOException {
		closeQuietly(inputReader);
		inputReader = null;
		closeQuietly(inputStream);
		inputStream = null;
	}

	/**
	 * Check if String value is null or contains only whitespace characters.
	 *
	 * @param value
	 *            the value
	 * @return true, if is blank
	 */
	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}

	/**
	 * Close a Closable item and ignore any Exception thrown by its close method.
	 *
	 * @param closeableItem
	 *            the closeable item
	 */
	private static void closeQuietly(Closeable closeableItem) {
		if (closeableItem != null) {
			try {
				closeableItem.close();
			} catch (IOException e) {
				// Do nothing
			}
		}
	}
}
