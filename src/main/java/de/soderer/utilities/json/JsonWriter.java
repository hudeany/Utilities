package de.soderer.utilities.json;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Stack;

import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.Utilities;

public class JsonWriter implements Closeable {
	/** Default output encoding. */
	public static final String DEFAULT_ENCODING = "UTF-8";
	
	/** Output stream. */
	private OutputStream outputStream;

	/** Output encoding. */
	private Charset encoding;

	/** Output writer. */
	private BufferedWriter outputWriter = null;
	
	private Stack<JsonItem> openJsonItems = new Stack<>();

	public enum JsonItem {
		Array_Empty,
		Array,
		Object_Empty,
		Object,
		Object_Value
	}
	
	public JsonWriter(OutputStream outputStream) {
		this(outputStream, null);
	}
	
	public JsonWriter(OutputStream outputStream, String encoding) {
		this.outputStream = outputStream;
		this.encoding = isBlank(encoding) ? Charset.forName(DEFAULT_ENCODING) : Charset.forName(encoding);
	}
	
	public void openJsonObject() throws Exception {
		if (outputWriter == null) {
			write("{", true);
			openJsonItems.push(JsonItem.Object_Empty);
		} else {
			JsonItem latestOpenJsonItem = openJsonItems.pop();
			if (latestOpenJsonItem != JsonItem.Array_Empty && latestOpenJsonItem != JsonItem.Array && latestOpenJsonItem != JsonItem.Object_Value) {
				openJsonItems.push(latestOpenJsonItem);
				throw new Exception("Not matching open Json item for opening object: " + latestOpenJsonItem);
			} else {
				if (latestOpenJsonItem == JsonItem.Array) {
					write(",\n", false);
				} else if (latestOpenJsonItem == JsonItem.Array_Empty) {
					write("\n", false);
				}
				
				if (latestOpenJsonItem != JsonItem.Object_Value) {
					openJsonItems.push(JsonItem.Array);
				}
				
				write("{", true);
				openJsonItems.push(JsonItem.Object_Empty);
			}
		}
	}
	
	public void openJsonObjectProperty(String propertyName) throws Exception {
		JsonItem latestOpenJsonItem = openJsonItems.pop();
		if (latestOpenJsonItem != JsonItem.Object_Empty && latestOpenJsonItem != JsonItem.Object) {
			openJsonItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for opening object property: " + latestOpenJsonItem);
		} else {
			if (latestOpenJsonItem == JsonItem.Object) {
				write(",\n", false);
			} else {
				write("\n", false);
			}
			openJsonItems.push(JsonItem.Object);
			write("\"" + propertyName.replace("\"", "\\\"") + "\": ", true);
			openJsonItems.push(JsonItem.Object_Value);
		}
	}
	
	public void addSimpleJsonObjectPropertyValue(Object propertyValue) throws Exception {
		JsonItem latestOpenJsonItem = openJsonItems.pop();
		if (latestOpenJsonItem != JsonItem.Object_Value) {
			openJsonItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding object property value: " + latestOpenJsonItem);
		} else {
			if (propertyValue == null) {
				write("null", false);
			} else if (propertyValue instanceof Boolean) {
				write(Boolean.toString((Boolean) propertyValue), false);
			} else if (propertyValue instanceof Date) {
				write("\"" + DateUtilities.ISO_8601_DATETIME_FORMAT.format((Date) propertyValue) + "\"", false);
			} else if (propertyValue instanceof Number) {
				write(((Number) propertyValue).toString(), false);
			} else {
				write("\"" + propertyValue.toString().replace("\"", "\\\"") + "\"", false);
			}
		}
	}
	
	public void closeJsonObject() throws Exception {
		JsonItem latestOpenJsonItem = openJsonItems.pop();
		if (latestOpenJsonItem != JsonItem.Object_Empty && latestOpenJsonItem != JsonItem.Object) {
			openJsonItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for closing object: " + latestOpenJsonItem);
		} else if (latestOpenJsonItem == JsonItem.Object_Empty) {
			write("}", false);
		} else {
			write("\n", false);
			write("}", true);
		}
	}
	
	public void openJsonArray() throws Exception {
		if (outputWriter == null) {
			write("[", true);
			openJsonItems.push(JsonItem.Array_Empty);
		} else {
			JsonItem latestOpenJsonItem = openJsonItems.pop();
			if (latestOpenJsonItem != JsonItem.Array_Empty && latestOpenJsonItem != JsonItem.Array && latestOpenJsonItem != JsonItem.Object_Value) {
				openJsonItems.push(latestOpenJsonItem);
				throw new Exception("Not matching open Json item for opening array: " + latestOpenJsonItem);
			} else {
				if (latestOpenJsonItem == JsonItem.Array) {
					write(",\n", false);
				} else if (latestOpenJsonItem == JsonItem.Array_Empty) {
					write("\n", false);
				}
				
				if (latestOpenJsonItem != JsonItem.Object_Value) {
					openJsonItems.push(JsonItem.Array);
				}
				
				write("[", true);
				openJsonItems.push(JsonItem.Array_Empty);
			}
		}
	}
	
	public void addSimpleJsonArrayValue(Object arrayValue) throws Exception {
		JsonItem latestOpenJsonItem = openJsonItems.pop();
		if (latestOpenJsonItem != JsonItem.Array_Empty && latestOpenJsonItem != JsonItem.Array) {
			openJsonItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for adding array value: " + latestOpenJsonItem);
		} else {
			if (latestOpenJsonItem == JsonItem.Array) {
				write(",\n", false);
			} else {
				write("\n", false);
			}

			openJsonItems.push(JsonItem.Array);
			
			if (arrayValue == null) {
				write("null", false);
			} else if (arrayValue instanceof Boolean) {
				write(Boolean.toString((Boolean) arrayValue), false);
			} else if (arrayValue instanceof Date) {
				write("\"" + DateUtilities.ISO_8601_DATETIME_FORMAT.format((Date) arrayValue) + "\"", false);
			} else if (arrayValue instanceof Number) {
				write(((Number) arrayValue).toString(), false);
			} else {
				write("\"" + arrayValue.toString().replace("\"", "\\\"") + "\"", false);
			}
		}
	}
	
	public void closeJsonArray() throws Exception {
		JsonItem latestOpenJsonItem = openJsonItems.pop();
		if (latestOpenJsonItem != JsonItem.Array_Empty && latestOpenJsonItem != JsonItem.Array) {
			openJsonItems.push(latestOpenJsonItem);
			throw new Exception("Not matching open Json item for closing array: " + latestOpenJsonItem);
		} else if (latestOpenJsonItem == JsonItem.Array_Empty) {
			write("]", false);
		} else {
			write("\n", false);
			write("]", true);
		}
	}
	
	public void add(JsonObject jsonOjbect) throws Exception {
		openJsonObject();
		for (String propertyName : jsonOjbect.keySet()) {
			openJsonObjectProperty(propertyName);
			Object propertyValue = jsonOjbect.get(propertyName);
			if (propertyValue instanceof JsonObject) {
				add((JsonObject) propertyValue);
			} else if (propertyValue instanceof JsonArray) {
				add((JsonArray) propertyValue);
			} else {
				addSimpleJsonObjectPropertyValue(propertyValue);
			}
		}
		closeJsonObject();
	}
	
	public void add(JsonArray jsonArray) throws Exception {
		openJsonArray();
		for (Object arrayValue : jsonArray) {
			if (arrayValue instanceof JsonObject) {
				add((JsonObject) arrayValue);
			} else if (arrayValue instanceof JsonArray) {
				add((JsonArray) arrayValue);
			} else {
				addSimpleJsonArrayValue(arrayValue);
			}
		}
		closeJsonArray();
	}
	
	public void closeAllOpenJsonItems() throws Exception {
		while (!openJsonItems.isEmpty()) {
			JsonItem openJsonItem = openJsonItems.pop();
			switch(openJsonItem) {
				case Array:
				case Array_Empty:
					closeJsonArray();
					break;
				case Object:
				case Object_Empty:
					closeJsonObject();
					break;
				case Object_Value:
					break;
			}
		}
	}

	/**
	 * Flush buffered data.
	 *
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	public void flush() throws IOException {
		if (outputWriter != null) {
			outputWriter.flush();
		}
	}

	/**
	 * Close this writer and its underlying stream.
	 */
	@Override
	public void close() throws IOException {
		if (outputWriter != null) {
			outputWriter.write("\n");
		}
		
		closeQuietly(outputWriter);
		outputWriter = null;
		closeQuietly(outputStream);
		outputStream = null;
		
		if (!openJsonItems.isEmpty()) {
			String jsonItemsStackString = "";
			while (!openJsonItems.isEmpty()) {
				jsonItemsStackString += "/" + openJsonItems.pop().toString();
			}
			throw new IOException("There are still Json items open: " + jsonItemsStackString);
		}
	}
	
	private void write(String text, boolean indent) throws IOException {
		if (outputWriter == null) {
			if (outputStream == null) {
				throw new IllegalStateException("JsonWriter is already closed");
			}
			outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, encoding));
		}
		
		outputWriter.write((indent ? Utilities.repeat("\t", openJsonItems.size()) : "") + text);
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
