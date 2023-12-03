package de.soderer.utilities.db;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;

import de.soderer.utilities.Tuple;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.collection.CaseInsensitiveOrderedMap;

public class OracleTnsnamesReader implements Closeable {
	private PushbackReader inputReader = null;

	public OracleTnsnamesReader(final InputStream inputStream) throws Exception {
		this(inputStream, StandardCharsets.UTF_8);
	}

	public OracleTnsnamesReader(final InputStream inputStream, final Charset encodingCharset) throws Exception {
		if (inputStream == null) {
			throw new IllegalStateException("InputStream is missing");
		} else {
			inputReader = new PushbackReader(new BufferedReader(new InputStreamReader(inputStream, encodingCharset)));
		}
	}

	@Override
	public void close() {
		if (inputReader != null) {
			try {
				inputReader.close();
			} catch (@SuppressWarnings("unused") final IOException e) {
				// Do nothing
			}
		}
	}

	private Character readNextCharacter() throws IOException {
		final int nextInt = inputReader.read();
		if (nextInt == -1) {
			return null;
		} else {
			return (char) nextInt;
		}
	}

	private char previewNextNonWhitespaceCharacter() throws Exception {
		Character nextChar = readNextCharacter();
		while (nextChar != null && Character.isWhitespace(nextChar) ) {
			nextChar = readNextCharacter();
		}
		if (nextChar == null) {
			return (char) -1;
		}
		inputReader.unread(nextChar);
		return nextChar;
	}

	private String readUntilAndTrim(final char endChar) throws IOException {
		final StringBuilder textBuilder = new StringBuilder();
		Character nextChar;
		while ((nextChar = readNextCharacter()) != null) {
			if (nextChar == endChar) {
				break;
			} else {
				textBuilder.append(nextChar);
			}
		}
		return textBuilder.toString().trim();
	}

	public Map<String, Map<String, Object>> read() throws Exception {
		if (inputReader == null) {
			throw new Exception("OracleTnsnamesReader position was already initialized for other read operation");
		} else {
			final Map<String, Map<String, Object>> tnsNamesEntries = new CaseInsensitiveOrderedMap<>();

			Tuple<String, Map<String, Object>> tnsNameEntry;
			while ((tnsNameEntry = readTnsNameEntry()) != null) {
				tnsNamesEntries.put(tnsNameEntry.getFirst(), tnsNameEntry.getSecond());
			}

			inputReader.close();
			inputReader = null;

			if (tnsNamesEntries.size() == 0) {
				return null;
			} else {
				return tnsNamesEntries;
			}
		}
	}

	private Tuple<String, Map<String, Object>> readTnsNameEntry() throws Exception {
		final String tnsName = readUntilAndTrim('=');
		if (Utilities.isBlank(tnsName)) {
			return null;
		}

		if (previewNextNonWhitespaceCharacter() == '(') {
			final Map<String, Object> tnsValues = new CaseInsensitiveOrderedMap<>();
			Tuple<String, Object> tnsValue;
			while ((tnsValue = readValue()) != null) {
				tnsValues.put(tnsValue.getFirst(), tnsValue.getSecond());
			}
			return new Tuple<>(tnsName, tnsValues);
		} else {
			throw new Exception("Premature end of data when reading TNS value");
		}
	}

	private Tuple<String, Object> readValue() throws Exception {
		if (previewNextNonWhitespaceCharacter() == '(') {
			readNextCharacter();
			final String key = readUntilAndTrim('=');
			if (Utilities.isBlank(key)) {
				return null;
			} else if (previewNextNonWhitespaceCharacter() == '(') {
				final Map<String, Object> tnsValues = new CaseInsensitiveOrderedMap<>();
				Tuple<String, Object> tnsValue;
				while ((tnsValue = readValue()) != null) {
					tnsValues.put(tnsValue.getFirst(), tnsValue.getSecond());
				}
				if (previewNextNonWhitespaceCharacter() == ')') {
					readNextCharacter();
					return new Tuple<>(key, tnsValues);
				} else {
					throw new Exception("Unexpected data when reading TNS value");
				}
			} else {
				String value = readUntilAndTrim(')');
				value = value.trim();
				value = Utilities.trim(Utilities.trim(value, '\''), '\"').trim();
				return new Tuple<>(key, value);
			}
		} else {
			return null;
		}
	}

	public static String getSingleLineFormatedTnsEntryData(final Map<String, Object> tnsEntryData) throws Exception {
		String returnValue = "";
		for (final Entry<String, Object> entry : tnsEntryData.entrySet()) {
			if (entry.getValue() instanceof Map) {
				@SuppressWarnings("unchecked")
				final Map<String, Object> valueMap = (Map<String, Object>) entry.getValue();
				returnValue = returnValue + "(" + entry.getKey().toUpperCase() + "=" + getSingleLineFormatedTnsEntryData(valueMap) + ")";
			} else {
				String valueString = entry.getValue().toString();
				if (valueString.contains("(")
						|| valueString.contains(")")
						|| valueString.contains("=")
						|| valueString.contains(" ")
						|| valueString.contains("\n")
						|| valueString.contains("\r")
						|| valueString.contains("\t")) {
					valueString = "\"" + valueString + "\"";
				}
				returnValue = returnValue + "(" + entry.getKey().toUpperCase() + "=" + valueString + ")";
			}
		}
		return returnValue;
	}
}
