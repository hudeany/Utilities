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
 * names and the length (number of chars) of each column in order. All values are trimmed,
 * blank lines are skipped. Characters behind the last described column are ignored. A leading
 * byte order mark is skipped.
 * </p>
 */
public class RecordReader implements AutoCloseable {
	/** Byte order mark, which is skipped at the beginning of the data */
	private static final char BYTE_ORDER_MARK = '﻿';

	private final InputStream inputStream;
	private final List<Tuple<String, Integer>> description;
	private final Charset encoding;
	private BufferedReader reader = null;
	private int linesRead = 0;
	private boolean allowUnderfilledLines = false;

	/**
	 * Creates a new reader using UTF-8 encoding.
	 *
	 * @param inputStream
	 *            the stream to read from, not null
	 * @param description
	 *            the column names and their lengths in chars (at least 0), in column order, not null
	 * @throws IllegalArgumentException
	 *             if a parameter is null or the description contains a null entry or a negative length
	 */
	public RecordReader(final InputStream inputStream, final List<Tuple<String, Integer>> description) {
		this(inputStream, StandardCharsets.UTF_8, description);
	}

	/**
	 * Creates a new reader using the given encoding.
	 *
	 * @param inputStream
	 *            the stream to read from, not null
	 * @param encoding
	 *            the encoding of the data, not null
	 * @param description
	 *            the column names and their lengths in chars (at least 0), in column order, not null
	 * @throws IllegalArgumentException
	 *             if a parameter is null or the description contains a null entry or a negative length
	 */
	public RecordReader(final InputStream inputStream, final Charset encoding, final List<Tuple<String, Integer>> description) {
		if (inputStream == null) {
			throw new IllegalArgumentException("InputStream must not be null");
		} else if (encoding == null) {
			throw new IllegalArgumentException("Encoding must not be null");
		} else if (description == null) {
			throw new IllegalArgumentException("Record description must not be null");
		}
		for (final Tuple<String, Integer> columnDescription : description) {
			if (columnDescription == null || columnDescription.getSecond() == null || columnDescription.getSecond() < 0) {
				throw new IllegalArgumentException("Invalid column description: " + (columnDescription == null ? "null" : columnDescription.getFirst() + " with length " + columnDescription.getSecond()));
			}
		}

		this.inputStream = inputStream;
		this.encoding = encoding;
		// Copy, so later changes of the caller's list do not bypass the validation
		this.description = new ArrayList<>(description);
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
	 * Returns the number of lines read so far, including skipped blank lines.
	 *
	 * @return the number of lines read
	 */
	public int getLinesRead() {
		return linesRead;
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
			skipByteOrderMark(reader);
		}

		String dataLine;
		do {
			dataLine = reader.readLine();
			if (dataLine == null) {
				return null;
			}
			linesRead++;
		} while (dataLine.trim().length() == 0);

		final List<String> returnList = new ArrayList<>();
		int index = 0;
		for (final Tuple<String, Integer> columnDescription : description) {
			// Computed as long, so very large column lengths cannot overflow
			final long endIndex = (long) index + columnDescription.getSecond();
			if (endIndex > dataLine.length()) {
				if (allowUnderfilledLines) {
					break;
				} else {
					throw new Exception("Record description does not fit data in line " + linesRead + ": column '" + columnDescription.getFirst()
						+ "' needs chars " + index + " to " + endIndex + ", but the line has only " + dataLine.length() + " chars");
				}
			}
			returnList.add(dataLine.substring(index, (int) endIndex).trim());
			index = (int) endIndex;
		}
		return returnList;
	}

	private static void skipByteOrderMark(final BufferedReader bufferedReader) throws IOException {
		bufferedReader.mark(1);
		if (bufferedReader.read() != BYTE_ORDER_MARK) {
			bufferedReader.reset();
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
			} else {
				// No record was read yet, so the stream was not wrapped by a reader
				inputStream.close();
			}
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
