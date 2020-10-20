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
 * FileReader for data files with no visible separator.
 * Data is separated by index position.
 * Description parameter contains list of columnnames and their end index.
 */
public class RecordReader implements AutoCloseable {
	private final InputStream inputStream;
	private final List<Tuple<String, Integer>> description;
	private Charset encoding = StandardCharsets.UTF_8;
	private BufferedReader reader = null;
	private int linesRead = 0;
	private boolean allowUnderfilledLines = false;

	public RecordReader(final InputStream inputStream, final List<Tuple<String, Integer>> description) {
		this.inputStream = inputStream;
		this.description = description;
	}

	public RecordReader(final InputStream inputStream, final Charset encoding, final List<Tuple<String, Integer>> description) {
		this.inputStream = inputStream;
		this.encoding = encoding;
		this.description = description;
	}

	public boolean isAllowUnderfilledLines() {
		return allowUnderfilledLines;
	}

	public void setAllowUnderfilledLines(final boolean allowUnderfilledLines) {
		this.allowUnderfilledLines = allowUnderfilledLines;
	}

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

	@Override
	public void close() throws Exception {
		if (reader != null ) {
			try {
				reader.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}
}
