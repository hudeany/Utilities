package de.soderer.utilities;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.apache.commons.text.StringEscapeUtils;

import de.soderer.utilities.TextUtilities.LineBreak;

public class PropertiesWriter implements Closeable {
	private final LineBreak linebreakType = LineBreak.Unix;
	private boolean encodeJavaEncoding = false;
	private final String keyValueSeparator = "=";

	private OutputStream outputStream;
	private final Charset encoding;
	private int writtenProperties = 0;
	private BufferedWriter outputWriter = null;

	public PropertiesWriter(final OutputStream outputStream) throws Exception {
		this(outputStream, StandardCharsets.UTF_8);
	}

	public PropertiesWriter(final OutputStream outputStream, final Charset encoding) {
		this.outputStream = outputStream;
		this.encoding = encoding;
	}

	public boolean isEncodeJavaEncoding() {
		return encodeJavaEncoding;
	}

	public PropertiesWriter setEncodeJavaEncoding(final boolean encodeJavaEncoding) {
		this.encodeJavaEncoding = encodeJavaEncoding;
		return this;
	}

	public void writeProperty(final String key, final String value) throws Exception {
		if (outputWriter == null) {
			if (outputStream == null) {
				throw new IllegalStateException("PropertiesWriter is already closed");
			}
			outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, encoding));
		}

		outputWriter.write(key);
		outputWriter.write(keyValueSeparator);
		outputWriter.write(escapeValue(value));
		outputWriter.write(linebreakType.toString());

		writtenProperties++;
	}

	/**
	 * Escape a single data value
	 *
	 * @param value
	 *            the value
	 * @return the string
	 */
	private String escapeValue(String valueString) throws Exception {
		if (valueString == null) {
			return "";
		} else {
			if (encodeJavaEncoding) {
				valueString = StringEscapeUtils.escapeJava(valueString);
			}
			valueString = valueString.replace("\r\n", "\\n").replace("\n", "\\n").replace("\r", "\\n");
			return valueString;
		}
	}

	public void writeComment(final String comment) throws Exception {
		if (outputWriter == null) {
			if (outputStream == null) {
				throw new IllegalStateException("PropertiesWriter is already closed");
			}
			outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, encoding));
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
