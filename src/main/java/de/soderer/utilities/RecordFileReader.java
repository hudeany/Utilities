package de.soderer.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * FileReader for data files with no visible separator.
 * Data is separated by index position.
 * Description parameter contains list of columnnames and their end index.
 */
public class RecordFileReader implements AutoCloseable {
	private final List<Tuple<String, Integer>> description;
	private final BufferedReader reader;

	public RecordFileReader(final File dataFile, final List<Tuple<String, Integer>> description) throws FileNotFoundException {
		this.description = description;
		reader = new BufferedReader(new FileReader(dataFile));
	}

	public List<String> readNextRecord() throws IOException {
		String dataLine = reader.readLine();
		if (dataLine == null) {
			return null;
		} else {
			while (dataLine.trim().length() == 0) {
				dataLine = reader.readLine();
				if (dataLine == null) {
					return null;
				}
			}

			final List<String> returnList = new ArrayList<>();
			int index = 0;
			for (final Tuple<String, Integer> columnDescription : description) {
				final int endIndex = index + columnDescription.getSecond();
				final String value = dataLine.substring(index, endIndex);
				index = endIndex;
				returnList.add(value.trim());
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
