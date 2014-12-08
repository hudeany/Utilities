package de.soderer.utilities;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

public class CsvWriter extends Writer {
	/**
	 * Default output encoding
	 */
	public static final String DEFAULT_ENCODING = "UTF-8";
	
	/**
	 * Default output separator
	 */
	public static final char DEFAULT_SEPARATOR = ',';
	
	/**
	 * Default output stringquote
	 */
	public static final char DEFAULT_STRING_QUOTE = '"';
	
	/**
	 * Default output linebreak
	 */
	public static final String DEFAULT_LINEBREAK = "\n";
	
	/**
	 * Current output separator
	 */
	private char separator;
	
	/**
	 * Current output separator as string
	 */
	private String separatorString;
	
	/**
	 * Current output string quote
	 */
	private char stringQuote;
	
	/**
	 * Current output string quote as string for internal use
	 */
	private String stringQuoteString;
	
	/**
	 * Current output string quote two times for internal use
	 */
	private String doubleStringQuoteString;
	
	/**
	 * Currently use the string quote
	 */
	private boolean useStringQuote;
	
	/**
	 * Current output linebreak
	 */
	private String lineBreak;
	
	/**
	 * Output stream
	 */
	private OutputStream outputStream;
	
	/**
	 * Output encoding
	 */
	private Charset encoding;
	
	/**
	 * Always quote all data entries
	 */
	private boolean alwaysQuote = false;
	
	/**
	 * Always quote texts or only quote them when they contain linebreaks or the separator character
	 */
	private boolean quoteAllStrings = false;
	
	/**
	 * Lines written until now
	 */
	private int writtenLines = 0;
	
	/**
	 * Number of columns to write, set by first line written
	 */
	private int numberOfColumns = -1;
	
	/**
	 * Output writer
	 */
	private BufferedWriter outputWriter = null;

	/**
	 * CSV Writer derived constructor
	 * 
	 * @param outputStream
	 */
	public CsvWriter(OutputStream outputStream) {
		this(outputStream, Charset.forName(DEFAULT_ENCODING), DEFAULT_SEPARATOR, DEFAULT_STRING_QUOTE, DEFAULT_LINEBREAK);
	}

	/**
	 * CSV Writer derived constructor
	 * 
	 * @param outputStream
	 * @param encoding
	 */
	public CsvWriter(OutputStream outputStream, String encoding) {
		this(outputStream, Charset.forName(encoding), DEFAULT_SEPARATOR, DEFAULT_STRING_QUOTE, DEFAULT_LINEBREAK);
	}
	
	/**
	 * CSV Writer derived constructor
	 * 
	 * @param outputStream
	 * @param encoding
	 */
	public CsvWriter(OutputStream outputStream, Charset encoding) {
		this(outputStream, encoding, DEFAULT_SEPARATOR, DEFAULT_STRING_QUOTE, DEFAULT_LINEBREAK);
	}
	
	/**
	 * CSV Writer derived constructor
	 * 
	 * @param outputStream
	 * @param separator
	 */
	public CsvWriter(OutputStream outputStream, char separator) {
		this(outputStream, Charset.forName(DEFAULT_ENCODING), separator, DEFAULT_STRING_QUOTE, DEFAULT_LINEBREAK);
	}
	
	/**
	 * CSV Writer derived constructor
	 * 
	 * @param outputStream
	 * @param encoding
	 * @param separator
	 */
	public CsvWriter(OutputStream outputStream, String encoding, char separator) {
		this(outputStream, Charset.forName(encoding), separator, DEFAULT_STRING_QUOTE, DEFAULT_LINEBREAK);
	}
	
	/**
	 * CSV Writer derived constructor
	 * 
	 * @param outputStream
	 * @param encoding
	 * @param separator
	 */
	public CsvWriter(OutputStream outputStream, Charset encoding, char separator) {
		this(outputStream, encoding, separator, DEFAULT_STRING_QUOTE, DEFAULT_LINEBREAK);
	}
	
	/**
	 * CSV Writer derived constructor
	 * 
	 * @param outputStream
	 * @param separator
	 * @param stringQuote
	 */
	public CsvWriter(OutputStream outputStream, char separator, Character stringQuote) {
		this(outputStream, Charset.forName(DEFAULT_ENCODING), separator, stringQuote, DEFAULT_LINEBREAK);
	}
	
	/**
	 * CSV Writer derived constructor
	 * 
	 * @param outputStream
	 * @param separator
	 * @param stringQuote
	 * @param lineBreak
	 */
	public CsvWriter(OutputStream outputStream, char separator, Character stringQuote, String lineBreak) {
		this(outputStream, Charset.forName(DEFAULT_ENCODING), separator, stringQuote, lineBreak);
	}

	/**
	 * CSV Writer derived constructor
	 * 
	 * @param outputStream
	 * @param encoding
	 * @param separator
	 * @param stringQuote
	 */
	public CsvWriter(OutputStream outputStream, String encoding, char separator, Character stringQuote) {
		this(outputStream, StringUtils.isBlank(encoding) ? Charset.forName(DEFAULT_ENCODING) : Charset.forName(encoding), separator, stringQuote == null ? DEFAULT_STRING_QUOTE : stringQuote, DEFAULT_LINEBREAK);
	}
	
	/**
	 * CSV Writer main constructor
	 * 
	 * @param outputStream
	 * @param encoding
	 * @param separator
	 * @param stringQuote
	 * @param lineBreak
	 */
	public CsvWriter(OutputStream outputStream, Charset encoding, char separator, Character stringQuote, String lineBreak) {
		this.outputStream = outputStream;
		this.encoding = encoding;
		this.separator = separator;
		separatorString = Character.toString(separator);
		this.lineBreak = lineBreak;
		if (stringQuote != null) {
			this.stringQuote = stringQuote;
			stringQuoteString = Character.toString(stringQuote);
			doubleStringQuoteString = stringQuoteString + stringQuoteString;
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
	
	/**
	 * Getter for property alwaysQuote
	 * 
	 * @return
	 */
	public boolean isAlwaysQuote() {
		return alwaysQuote;
	}

	/**
	 * Setter for property alwaysQuote
	 * 
	 * @param alwaysQuote
	 */
	public void setAlwaysQuote(boolean alwaysQuote) {
		this.alwaysQuote = alwaysQuote;
		if (alwaysQuote) {
			quoteAllStrings = false;
		}
	}
	
	/**
	 * Getter for property quoteAllStrings
	 * 
	 * @return
	 */
	public boolean isQuoteAllStrings() {
		return quoteAllStrings;
	}

	/**
	 * Setter for property quoteAllStrings
	 * 
	 * @param quoteAllStrings
	 */
	public void setQuoteAllStrings(boolean quoteAllStrings) {
		this.quoteAllStrings = quoteAllStrings;
		if (quoteAllStrings) {
			alwaysQuote = false;
		}
	}
	
	/**
	 * Write a single line of data entries
	 * 
	 * @param values
	 * @throws Exception
	 */
	public void writeValues(Object... values) throws Exception {
		writeValues(Arrays.asList(values));
	}
	
	/**
	 * Write a single line of data entries
	 * 
	 * @param values
	 * @throws CsvDataException
	 * @throws IOException
	 */
	public void writeValues(List<? extends Object> values) throws CsvDataException, IOException {
		if (numberOfColumns != -1 && (values == null || numberOfColumns != values.size())) {
			throw new CsvDataException("Inconsistent number of values after " + writtenLines + " written lines (expected: " + numberOfColumns + " was: " + (values == null ? "null" : values.size()) + ")", writtenLines);
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
	
	/**
	 * Write a full set of lines of data entries
	 * 
	 * @param valueLines
	 * @throws Exception
	 */
	public void writeAll(List<List<? extends Object>> valueLines) throws Exception {
		for (List<? extends Object> valuesOfLine : valueLines) {
			writeValues(valuesOfLine);
		}
	}
	
	/**
	 * Escape a single data entry using stringquotes as configured
	 * 
	 * @param value
	 * @return
	 * @throws CsvDataException
	 */
	private String escapeValue(Object value) throws CsvDataException {
		String valueString = "";
		if (value != null) {
			valueString = value.toString();
		}
		
		if (alwaysQuote || (quoteAllStrings && value instanceof String) || valueString.contains(separatorString) || valueString.contains("\r") || valueString.contains("\n") || (useStringQuote && valueString.contains(stringQuoteString))) {
			if (!useStringQuote) {
				throw new CsvDataException("StringQuote was deactivated but is needed for csv-value after " + writtenLines + " written lines", writtenLines);
			} else {
				StringBuilder escapedValue = new StringBuilder(); 
				escapedValue.append(stringQuote);
				escapedValue.append(valueString.replace(stringQuoteString, doubleStringQuoteString));
				escapedValue.append(stringQuote);
				return escapedValue.toString();
			}
		} else {
			return valueString;
		}
	}

	/**
	 * Close this writer and its underlying stream
	 */
	@Override
	public void close() {
		IOUtils.closeQuietly(outputWriter);
		outputWriter = null;
		IOUtils.closeQuietly(outputStream);
		outputStream = null;
	}
	
	/**
	 * Get number of lines written until now
	 * 
	 * @return
	 */
	public int getWrittenLines() {
		return writtenLines;
	}

	/**
	 * Not supported method to write data (must implement for writer interface)
	 */
	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		throw new IOException("Write by offset and length is not supported by " + getClass().getSimpleName());
	}

	/**
	 * Flush buffered data
	 */
	@Override
	public void flush() throws IOException {
		if (outputWriter != null) {
			outputWriter.flush();
		}
	}
	
	/**
	 * Create a single csv line
	 * 
	 * @param separator
	 * @param stringQuote
	 * @param values
	 * @return
	 */
	public static String getCsvLine(char separator, Character stringQuote, List<? extends Object> values) {
		return getCsvLine(separator, stringQuote, values.toArray());
	}
	
	/**
	 * Create a single csv line
	 * 
	 * @param separator
	 * @param stringQuote
	 * @param values
	 * @return
	 */
	public static String getCsvLine(char separator, Character stringQuote, Object... values) {
		StringBuilder returnValue = new StringBuilder();
		String separatorString = Character.toString(separator);
		String stringQuoteString = stringQuote == null ? "" : Character.toString(stringQuote);
		String doubleStringQuoteString = stringQuoteString + stringQuoteString;
		if (values != null) {
			for (Object value : values) {
				if (returnValue.length() > 0) {
					returnValue.append(separator);
				}
				if (value != null) {
					String valueString;
					if (value instanceof char[]) {
						valueString = new String((char[]) value);
					} else {
						valueString = value.toString();
					}
					
					if (valueString.contains(separatorString) || valueString.contains("\r") || valueString.contains("\n") || valueString.contains(stringQuoteString)) {
						returnValue.append(stringQuoteString);
						returnValue.append(valueString.replace(stringQuoteString, doubleStringQuoteString));
						returnValue.append(stringQuoteString);
					} else {
						returnValue.append(valueString);
					}
				}
			}
		}
		return returnValue.toString();
	}
}
