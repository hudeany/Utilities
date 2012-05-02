package de.soderer.utilities;

import java.io.BufferedWriter;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;

public class CsvWriter {
	public static final String DEFAULT_ENCODING = "UTF-8";
	public static final char DEFAULT_SEPARATOR = ',';
	public static final char DEFAULT_STRING_QUOTE = '"';
	public static final String DEFAULT_LINEBREAK = "\n";
	
	private char separator;
	private char stringQuote;
	private boolean useStringQuote;
	private String lineBreak;
	private OutputStream outputStream;
	private Charset encoding;
	
	private int writtenLines = 0;
	private int numberOfColumns = -1;
	private BufferedWriter outputWriter = null;

	public CsvWriter(OutputStream outputStream) {
		this(outputStream, Charset.forName(DEFAULT_ENCODING), DEFAULT_SEPARATOR, DEFAULT_STRING_QUOTE, DEFAULT_LINEBREAK);
	}

	public CsvWriter(OutputStream outputStream, String encoding) {
		this(outputStream, Charset.forName(encoding), DEFAULT_SEPARATOR, DEFAULT_STRING_QUOTE, DEFAULT_LINEBREAK);
	}
	
	public CsvWriter(OutputStream outputStream, Charset encoding) {
		this(outputStream, encoding, DEFAULT_SEPARATOR, DEFAULT_STRING_QUOTE, DEFAULT_LINEBREAK);
	}
	
	public CsvWriter(OutputStream outputStream, char separator) {
		this(outputStream, Charset.forName(DEFAULT_ENCODING), separator, DEFAULT_STRING_QUOTE, DEFAULT_LINEBREAK);
	}
	
	public CsvWriter(OutputStream outputStream, String encoding, char separator) {
		this(outputStream, Charset.forName(encoding), separator, DEFAULT_STRING_QUOTE, DEFAULT_LINEBREAK);
	}
	
	public CsvWriter(OutputStream outputStream, Charset encoding, char separator) {
		this(outputStream, encoding, separator, DEFAULT_STRING_QUOTE, DEFAULT_LINEBREAK);
	}
	
	public CsvWriter(OutputStream outputStream, char separator, Character stringQuote) {
		this(outputStream, Charset.forName(DEFAULT_ENCODING), separator, stringQuote, DEFAULT_LINEBREAK);
	}
	
	public CsvWriter(OutputStream outputStream, char separator, Character stringQuote, String lineBreak) {
		this(outputStream, Charset.forName(DEFAULT_ENCODING), separator, stringQuote, lineBreak);
	}

	public CsvWriter(OutputStream outputStream, String encoding, char separator, Character stringQuote) {
		this(outputStream, Charset.forName(encoding), separator, stringQuote, DEFAULT_LINEBREAK);
	}
	
	public CsvWriter(OutputStream outputStream, Charset encoding, char separator, Character stringQuote, String lineBreak) {
		this.outputStream = outputStream;
		this.encoding = encoding;
		this.separator = separator;
		this.lineBreak = lineBreak;
		if (stringQuote != null) {
			this.stringQuote = stringQuote;
			this.useStringQuote = true;
		} else {
			this.useStringQuote = false;
		}
		
		if (this.encoding == null) {
			throw new IllegalArgumentException("Encoding is null");
		} else if (this.outputStream == null) {
			throw new IllegalArgumentException("OutputStream is null");
		} else if (Utilities.anyCharsAreEqual(this.separator, '\r', '\n')) {
			throw new IllegalArgumentException("Separator '" + this.separator + "' is invalid");
		} else if (useStringQuote && Utilities.anyCharsAreEqual(this.separator, this.stringQuote, '\r', '\n')) {
			throw new IllegalArgumentException("Stringquote '" + this.stringQuote + "' is invalid");
		} else if (!this.lineBreak.equals("\r") && !this.lineBreak.equals("\n") && !this.lineBreak.equals("\r\n")) {
			throw new IllegalArgumentException("Given linebreak is invalid");
		}
	}
	
	public void writeValues(Object... values) throws Exception {
		writeValues(Arrays.asList(values));
	}
	
	public void writeValues(List<? extends Object> values) throws Exception {
		if (numberOfColumns != -1 && (values == null || numberOfColumns != values.size())) {
			throw new IllegalArgumentException("Inconsistent number of values after " + writtenLines + " written lines (expected: " + numberOfColumns + " was: " + (values == null ? "null" : values.size()) + ")");
		}
		
		if (outputWriter == null) {
			if (outputStream == null) {
				throw new IllegalStateException("CsvWriter is already closed");
			}
			outputWriter = new BufferedWriter(new OutputStreamWriter(outputStream, encoding));
		}
		
		boolean isFirst = true;
		for (Object value : values) {
			if (!isFirst) {
				outputWriter.write(separator);
			}
			isFirst = false;
			
			outputWriter.write(escapeValue(value));
		}
		outputWriter.write(lineBreak);

		writtenLines++;
		numberOfColumns = values.size();
	}
	
	public void writeAll(List<List<? extends Object>> valueLines) throws Exception {
		for (List<? extends Object> valuesOfLine : valueLines) {
			writeValues(valuesOfLine);
		}
	}
	
	private String escapeValue(Object value) throws Exception {
		String valueString = value.toString();
		if (valueString.contains(String.valueOf(separator)) || valueString.contains("\r") || valueString.contains("\n") || (useStringQuote && valueString.contains(String.valueOf(stringQuote)))) {
			if (!useStringQuote) {
				throw new Exception("StringQuote is needed for csv-value but was deactivated");
			} else {
				return stringQuote + valueString.replace(String.valueOf(stringQuote), String.valueOf(stringQuote) + String.valueOf(stringQuote)) + stringQuote;
			}
		} else {
			return valueString;
		}
	}

	public void close() {
		IOUtils.closeQuietly(outputWriter);
		outputWriter = null;
		IOUtils.closeQuietly(outputStream);
		outputStream = null;
	}
	
	public int getWrittenLines() {
		return writtenLines;
	}
}
