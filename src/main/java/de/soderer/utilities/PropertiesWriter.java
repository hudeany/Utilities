package de.soderer.utilities;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

import de.soderer.utilities.TextUtilities.LineBreak;

public class PropertiesWriter implements Closeable {
	private final LineBreak linebreakType = LineBreak.Unix;
	private final String keyValueSeparator = "=";

	private OutputStream outputStream;
	private int writtenProperties = 0;
	private BufferedWriter outputWriter = null;

	public PropertiesWriter(final OutputStream outputStream) {
		this.outputStream = outputStream;
		outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.ISO_8859_1));
	}

	public void writeProperty(final String key, final String value) throws Exception {
		if (outputStream == null) {
			throw new IllegalStateException("PropertiesWriter is already closed");
		}

		outputWriter.write(key);
		outputWriter.write(keyValueSeparator);
		outputWriter.write(escapeValue(value));
		outputWriter.write(linebreakType.toString());

		writtenProperties++;
	}

	public void writeProperties(final Map<String, String> properties) throws Exception {
		writeProperties(properties, null);
	}

	public void writeProperties(final Map<String, String> properties, final Map<String, String> comments) throws Exception {
		if (outputStream == null) {
			throw new IllegalStateException("PropertiesWriter is already closed");
		}

		for (final Entry<String, String> propertyEntry : properties.entrySet()) {
			if (comments != null && comments.get(propertyEntry.getKey()) != null) {
				writeComment(comments.get(propertyEntry.getKey()));
			}
			writeProperty(propertyEntry.getKey(), propertyEntry.getValue());
		}
	}

	/**
	 * Escape a single data value
	 *
	 * @param value
	 *            the value
	 * @return the string
	 */
	private static String escapeValue(String valueString) throws Exception {
		if (valueString == null) {
			return "";
		} else {
			valueString = Utilities.escapePropertiesString(valueString);
			if (valueString.length() > 0 && Character.isWhitespace(valueString.charAt(0))) {
				valueString = "\\" + valueString;
			}
			valueString = valueString.replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
			return valueString;
		}
	}

	public void writeComment(final String comment) throws Exception {
		if (outputStream == null) {
			throw new IllegalStateException("PropertiesWriter is already closed");
		}

		for (final String commentLine : comment.replace("\r\n", "\n").replace("\r", "\n").split("\n")) {
			outputWriter.write("# " + commentLine + "\n");
		}
	}

	public void writeEmptyLine() throws Exception {
		outputWriter.write("\n");
	}

	/**
	 * Get number of properties written until now.
	 *
	 * @return the written properties
	 */
	public int getWrittenProperties() {
		return writtenProperties;
	}

	/**
	 * Close this writer and its underlying stream.
	 */
	@Override
	public void close() {
		closeQuietly(outputWriter);
		outputWriter = null;
		closeQuietly(outputStream);
		outputStream = null;
	}

	/**
	 * Close a Closable item and ignore any Exception thrown by its close method.
	 *
	 * @param closeableItem
	 *            the closeable item
	 */
	private static void closeQuietly(final Closeable closeableItem) {
		if (closeableItem != null) {
			try {
				closeableItem.close();
			} catch (@SuppressWarnings("unused") final IOException e) {
				// Do nothing
			}
		}
	}
}
