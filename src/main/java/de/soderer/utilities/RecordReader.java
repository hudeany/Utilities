package de.soderer.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Reader for fixed width record files, which have no visible separator between their values.
 * <p>
 * The values are separated by their position within the line. The description contains the column
 * names and the length (number of characters) of each column in order. All values are trimmed,
 * blank lines are skipped.
 * </p>
 */
public class RecordReader implements AutoCloseable {
	private final InputStream inputStream;
	private final List<Tuple<String, Integer>> description;
	private Charset encoding = StandardCharsets.UTF_8;
	private BufferedReader reader = null;
	private int linesRead = 0;
	private boolean allowUnderfilledLines = false;

	/**
	 * Creates a new reader using UTF-8 encoding.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @param description
	 *            the column names and their lengths in characters, in column order
	 */
	public RecordReader(final InputStream inputStream, final List<Tuple<String, Integer>> description) {
		this.inputStream = inputStream;
		this.description = description;
	}

	/**
	 * Creates a new reader using the given encoding.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @param encoding
	 *            the encoding of the data
	 * @param description
	 *            the column names and their lengths in characters, in column order
	 */
	public RecordReader(final InputStream inputStream, final Charset encoding, final List<Tuple<String, Integer>> description) {
		this.inputStream = inputStream;
		this.encoding = encoding;
		this.description = description;
	}

	/**
	 * Returns whether lines shorter than the record description are allowed.
	 *
	 * @return true, if lines may be shorter than the record description
	 */
	public boolean isAllowUnderfilledLines() {
		return allowUnderfilledLines;
	}

	/**
	 * Sets whether lines shorter than the record description are allowed. For such lines only the
	 * values of the columns completely contained in the line are returned.
	 *
	 * @param allowUnderfilledLines
	 *            true to allow lines shorter than the record description
	 */
	public void setAllowUnderfilledLines(final boolean allowUnderfilledLines) {
		this.allowUnderfilledLines = allowUnderfilledLines;
	}

	/**
	 * Reads the next non blank line and splits it into its trimmed values.
	 *
	 * @return the values of the next record, or null at the end of data
	 * @throws Exception
	 *             if reading fails or the line is shorter than the record description and
	 *             underfilled lines are not allowed
	 */
	public List<String> readNextRecord() throws Exception {
		if (reader == null) {
			reader = new BufferedReader(new InputStreamReader(inputStream, encoding));
		}

		String dataLine = reader.readLine();
		if (dataLine == null) {
			return null;
		} else {
			linesRead++;
			while (dataLine.trim().length() == 0) {
				dataLine = reader.readLine();
				if (dataLine == null) {
					return null;
				} else {
					linesRead++;
				}
			}

			final List<String> returnList = new ArrayList<>();
			int index = 0;
			try {
				for (final Tuple<String, Integer> columnDescription : description) {
					final int endIndex = index + columnDescription.getSecond();
					if ((index > dataLine.length() || endIndex > dataLine.length()) && allowUnderfilledLines) {
						break;
					}
					final String value = dataLine.substring(index, endIndex);
					index = endIndex;
					returnList.add(value.trim());
				}
			} catch (@SuppressWarnings("unused") final StringIndexOutOfBoundsException e) {
				// Write error detection help data
				int errorIndex = 0;
				for (final Tuple<String, Integer> columnDescription : description) {
					try {
						final int endIndex = errorIndex + columnDescription.getSecond();
						final String value = dataLine.substring(errorIndex, endIndex);

						System.out.println(columnDescription.getFirst() + " (" + errorIndex + "-" + endIndex + "): '" + value + "'");

						errorIndex = endIndex;
					} catch (@SuppressWarnings("unused") final Exception e1) {
						System.out.println("Error at column '" + columnDescription.getFirst() + "' after index " + errorIndex);
						break;
					}
				}

				throw new Exception("Record description does not fit data in line: " + linesRead);
			}
			return returnList;
		}
	}

	/**
	 * Closes this reader and the underlying stream. Errors on closing are printed to stderr.
	 */
	@Override
	public void close() {
		try {
			if (reader != null) {
				reader.close();
			} else if (inputStream != null) {
				// No record was read yet, so the stream was not wrapped by a reader
				inputStream.close();
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
