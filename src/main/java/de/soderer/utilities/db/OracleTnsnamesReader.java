package de.soderer.utilities.db;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import de.soderer.utilities.Tuple;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.collection.CaseInsensitiveOrderedMap;
import de.soderer.utilities.collection.MultiValueCaseInsensitiveOrderedMap;

public class OracleTnsnamesReader implements Closeable {
	public static final String EXAMPLE =
			"TNSNAME=\n"
					+ "\t(DESCRIPTION=\n"
					+ "\t\t(ADDRESSLIST=\n"
					+ "\t\t\t(ADDRESS=\n"
					+ "\t\t\t\t(PROTOCOL=TCPS)\n"
					+ "\t\t\t\t(HOST=serverHost)\n"
					+ "\t\t\t\t(PORT=1521)\n"
					+ "\t\t\t)\n"
					+ "\t\t\t(ADDRESS=\n"
					+ "\t\t\t\t(PROTOCOL=TCPS)\n"
					+ "\t\t\t\t(HOST=serverHost)\n"
					+ "\t\t\t\t(PORT=1521)\n"
					+ "\t\t	)\n"
					+ "\t\t)\n"
					+ "\t\t(CONNECT_DATA=\n"
					+ "\t\t\t(SERVICE_NAME=serviceName)\n"
					+ "\t\t)\n"
					+ "\t\t(SECURITY=\n"
					+ "\t\t\t(SSL_SERVER_CERT_DN=\"trusted CN\")\n"
					+ "\t\t)\n"
					+ "\t)\n";

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

	public Map<String, OracleTnsMapValue> read() throws Exception {
		if (inputReader == null) {
			throw new Exception("OracleTnsnamesReader position was already initialized for other read operation");
		} else {
			final Map<String, OracleTnsMapValue> tnsNamesEntries = new CaseInsensitiveOrderedMap<>();

			Tuple<String, OracleTnsMapValue> tnsNameEntry;
			while ((tnsNameEntry = readTnsNameEntry()) != null) {
				final OracleTnsValue previousEntry = tnsNamesEntries.put(tnsNameEntry.getFirst(), tnsNameEntry.getSecond());
				if (previousEntry != null) {
					throw new Exception("Multiple entry for the same TNS entry name: " + tnsNameEntry.getFirst());
				}
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

	private Tuple<String, OracleTnsMapValue> readTnsNameEntry() throws Exception {
		final String tnsName = readUntilAndTrim('=');
		if (Utilities.isBlank(tnsName)) {
			return null;
		}

		if (previewNextNonWhitespaceCharacter() == '(') {
			final MultiValueCaseInsensitiveOrderedMap<OracleTnsValue> tnsValues = new MultiValueCaseInsensitiveOrderedMap<>();
			Tuple<String, OracleTnsValue> tnsValue;
			while ((tnsValue = readValue()) != null) {
				tnsValues.put(tnsValue.getFirst(), tnsValue.getSecond());
			}
			return new Tuple<>(tnsName, new OracleTnsMapValue(tnsValues));
		} else {
			throw new Exception("Premature end of data when reading TNS value");
		}
	}

	private Tuple<String, OracleTnsValue> readValue() throws Exception {
		if (previewNextNonWhitespaceCharacter() == '(') {
			readNextCharacter();
			final String key = readUntilAndTrim('=');
			if (Utilities.isBlank(key)) {
				return null;
			} else if (previewNextNonWhitespaceCharacter() == '(') {
				final MultiValueCaseInsensitiveOrderedMap<OracleTnsValue> tnsValues = new MultiValueCaseInsensitiveOrderedMap<>();
				Tuple<String, OracleTnsValue> tnsValue;
				while ((tnsValue = readValue()) != null) {
					tnsValues.put(tnsValue.getFirst(), tnsValue.getSecond());
				}
				if (previewNextNonWhitespaceCharacter() == ')') {
					readNextCharacter();
					return new Tuple<>(key, new OracleTnsMapValue(tnsValues));
				} else {
					throw new Exception("Unexpected data when reading TNS value");
				}
			} else {
				String value = readUntilAndTrim(')');
				value = value.trim();
				value = Utilities.trim(Utilities.trim(value, '\''), '\"').trim();
				return new Tuple<>(key, new OracleTnsStringValue(value));
			}
		} else {
			return null;
		}
	}

	public static String getSingleLineFormatedTnsEntryData(final OracleTnsValue tnsEntryData) throws Exception {
		if (tnsEntryData instanceof OracleTnsMapValue) {
			String returnValue = "";
			for (final Entry<String, ArrayList<OracleTnsValue>> entry : ((OracleTnsMapValue) tnsEntryData).getValue().entrySet()) {
				for (final OracleTnsValue item : entry.getValue()) {
					if (item instanceof OracleTnsMapValue) {
						final OracleTnsMapValue tnsValue = (OracleTnsMapValue) item;
						returnValue = returnValue + "(" + entry.getKey().toUpperCase() + "=" + getSingleLineFormatedTnsEntryData(tnsValue) + ")";
					} else if (item instanceof OracleTnsStringValue) {
						String valueString = ((OracleTnsStringValue) item).getValue();
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
					} else {
						throw new Exception("Unknown TNS value type");
					}
				}
			}
			return returnValue;
		} else if (tnsEntryData instanceof OracleTnsStringValue) {
			return ((OracleTnsStringValue) tnsEntryData).getValue();
		} else {
			throw new Exception("Unknown TNS value type");
		}
	}

	public static String format(final Map<String, OracleTnsMapValue> tnsnamesData) throws Exception {
		String returnValue = "";
		for (final Entry<String, OracleTnsMapValue> tnsNamesEntry : tnsnamesData.entrySet()) {
			if (returnValue.length() > 0) {
				returnValue = returnValue + "\n";
			}
			returnValue = returnValue + tnsNamesEntry.getKey().toUpperCase() + "=\n";

			for (final Entry<String, ArrayList<OracleTnsValue>> entry : tnsNamesEntry.getValue().getValue().entrySet()) {
				for (final OracleTnsValue item : entry.getValue()) {
					if (item instanceof OracleTnsMapValue) {
						final OracleTnsMapValue valueMap = (OracleTnsMapValue) item;
						returnValue = returnValue + "\t" + "(" + entry.getKey().toUpperCase() + "=\n";
						returnValue = returnValue + format(valueMap, 2);
						returnValue = returnValue + "\t" + ")\n";
					} else if (item instanceof OracleTnsStringValue) {
						String valueString = ((OracleTnsStringValue) item).getValue();
						if (valueString.contains("(")
								|| valueString.contains(")")
								|| valueString.contains("=")
								|| valueString.contains(" ")
								|| valueString.contains("\n")
								|| valueString.contains("\r")
								|| valueString.contains("\t")) {
							valueString = "\"" + valueString + "\"";
						}
						returnValue = returnValue + "\t" + "(" + entry.getKey().toUpperCase() + "=" + valueString + ")\n";
					} else {
						throw new Exception("Unknown TNS value type");
					}
				}
			}
		}
		return returnValue;
	}

	private static String format(final OracleTnsMapValue tnsnamesData, final int indentationLevel) throws Exception {
		String returnValue = "";
		for (final Entry<String, ArrayList<OracleTnsValue>> entry : tnsnamesData.getValue().entrySet()) {
			for (final OracleTnsValue tnsValue : entry.getValue()) {
				if (tnsValue instanceof OracleTnsMapValue) {
					returnValue = returnValue + Utilities.repeat("\t", indentationLevel) + "(" + entry.getKey().toUpperCase() + "=\n";
					returnValue = returnValue + format((OracleTnsMapValue) tnsValue, indentationLevel + 1);
					returnValue = returnValue + Utilities.repeat("\t", indentationLevel) + ")\n";
				} else if (tnsValue instanceof OracleTnsStringValue) {
					String valueString = ((OracleTnsStringValue) tnsValue).getValue();
					if (valueString.contains("(")
							|| valueString.contains(")")
							|| valueString.contains("=")
							|| valueString.contains(" ")
							|| valueString.contains("\n")
							|| valueString.contains("\r")
							|| valueString.contains("\t")) {
						valueString = "\"" + valueString + "\"";
					}
					returnValue = returnValue + Utilities.repeat("\t", indentationLevel) + "(" + entry.getKey().toUpperCase() + "=" + valueString + ")\n";
				} else {
					throw new Exception("Unknown TNS value type");
				}
			}
		}
		return returnValue;
	}
}
