package de.soderer.utilities;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * {@link TextPropertiesReader} for the content of a file or byte array, which also detects the text encoding.
 * Encodings are described by a {@link Tuple} of the charset name and whether a BOM (byte order mark) is used.
 * If no encoding is given, it is detected; undetectable data is read as UTF-8, or as ISO-8859-1 if it is no valid UTF-8.
 */
public class TextFilePropertiesReader extends TextPropertiesReader {
	private File file;
	private byte[] data;
	private Tuple<String, Boolean> defaultEncoding = new Tuple<>(StandardCharsets.UTF_8.name(), false);
	private Tuple<String, Boolean> encodingData = null;
	private long filesize = -1;

	/**
	 * Creates a reader for a file.
	 *
	 * @param filePath path of the file
	 * @param encodingData encoding name and whether a BOM (byte order mark) is used, or null to detect the encoding
	 * @throws Exception if the file does not exist or is no file
	 */
	public TextFilePropertiesReader(final String filePath, final Tuple<String, Boolean> encodingData) throws Exception {
		this(new File(filePath), encodingData);
	}

	/**
	 * Creates a reader for a file with encoding detection.
	 *
	 * @param filePath path of the file
	 * @throws Exception if the file does not exist or is no file
	 */
	public TextFilePropertiesReader(final String filePath) throws Exception {
		this(new File(filePath));
	}

	/**
	 * Creates a reader for a file with encoding detection.
	 *
	 * @param file the file
	 * @throws Exception if the file does not exist or is no file
	 */
	public TextFilePropertiesReader(final File file) throws Exception {
		this(file, null);
	}

	/**
	 * Creates a reader for a file.
	 *
	 * @param file the file
	 * @param encodingData encoding name and whether a BOM (byte order mark) is used, or null to detect the encoding
	 * @throws Exception if the file does not exist or is no file
	 */
	public TextFilePropertiesReader(final File file, final Tuple<String, Boolean> encodingData) throws Exception {
		this.file = file;

		if (!file.exists()) {
			throw new Exception("File does not exist");
		} else if (!file.isFile()) {
			throw new Exception("Path isn't a file");
		}

		this.encodingData = encodingData;
	}

	/**
	 * Creates a reader for data in memory with encoding detection.
	 *
	 * @param data the data (not copied)
	 */
	public TextFilePropertiesReader(final byte[] data) {
		this(data, null);
	}

	/**
	 * Creates a reader for data in memory.
	 *
	 * @param data the data (not copied)
	 * @param encodingData encoding name and whether a BOM (byte order mark) is used, or null to detect the encoding
	 */
	public TextFilePropertiesReader(final byte[] data, final Tuple<String, Boolean> encodingData) {
		this.data = data;
		filesize = data.length;
		this.encodingData = encodingData;
	}

	/**
	 * Reads and decodes the complete file (or data) and scans its text properties.
	 *
	 * @return the decoded text (without BOM)
	 * @throws Exception if the file cannot be read or the encoding is not supported
	 */
	public String readFileToString() throws Exception {
		if (data == null) {
			filesize = file.length();
			data = Utilities.readFileToByteArray(file);
		}

		if (encodingData == null) {
			// try to detect encoding
			encodingData = TextUtilities.detectEncoding(data);

			if (encodingData == null) {
				// File encoding cannot be detected so use defaultEncoding
				encodingData = defaultEncoding;
			}

			dataString = decodeByteArray(data, encodingData);

			// Check for invalid UTF-8 character
			if (encodingData != null && StandardCharsets.UTF_8.name().equalsIgnoreCase(encodingData.getFirst()) && dataString.contains("�")) {
				// If encoding was UTF-8 (detected or default) but contains illegal chars, then try most common other encoding
				encodingData = new Tuple<>(StandardCharsets.ISO_8859_1.name(), false);
				dataString = decodeByteArray(data, encodingData);
			}
		} else {
			// only use defined encoding
			dataString = decodeByteArray(data, encodingData);
		}

		super.readProperties();
		return dataString;
	}

	/**
	 * Reads and decodes a part of the file (or data) and scans its text properties.
	 * The range is moved and shortened to fit into the data, so e.g. a too large start position reads the last bytes.
	 *
	 * @param startPosition position of the first byte to read
	 * @param length number of bytes to read
	 * @return the decoded text
	 * @throws Exception if the file cannot be read or the encoding is not supported
	 */
	public String readFileToString(long startPosition, int length) throws Exception {
		if (data == null) {
			filesize = file.length();
			if (startPosition < 0) {
				startPosition = 0;
			}
			if (filesize < length) {
				length = (int) filesize;
			}
			if (filesize < startPosition + length) {
				startPosition = filesize - length;
			}

			final byte[] subdata = new byte[length];
			try (FileInputStream inputStream = new FileInputStream(file)) {
				// skip() and read() may process fewer bytes than requested
				inputStream.skipNBytes(startPosition);
				final int readBytes = inputStream.readNBytes(subdata, 0, length);
				if (readBytes != length) {
					throw new IOException("Unexpected end of file: " + readBytes + " of " + length + " bytes read");
				}
			}

			if (encodingData == null) {
				// try to detect encoding
				encodingData = TextUtilities.detectEncoding(subdata);

				if (encodingData == null) {
					// File encoding cannot be detected so use defaultEncoding
					encodingData = defaultEncoding;
				}

				dataString = decodeByteArray(subdata, encodingData);

				// Check for invalid UTF-8 character
				if (encodingData != null && StandardCharsets.UTF_8.name().equalsIgnoreCase(encodingData.getFirst()) && dataString.contains("�")) {
					// If encoding was UTF-8 (detected or default) but contains illegal chars, then try most common other encoding
					encodingData = new Tuple<>(StandardCharsets.ISO_8859_1.name(), false);
					dataString = decodeByteArray(subdata, encodingData);
				}
			} else {
				// only use defined encoding
				dataString = decodeByteArray(subdata, encodingData);
			}

			super.readProperties();
			return dataString;
		} else {
			if (startPosition < 0) {
				startPosition = 0;
			}
			if (filesize < length) {
				length = (int) filesize;
			}
			if (filesize < startPosition + length) {
				startPosition = filesize - length;
			}

			final byte[] subdata = new byte[length];
			for (int i = 0; i < length; i++) {
				subdata[i] = data[(int) startPosition + i];
			}

			if (encodingData == null) {
				// try to detect encoding
				encodingData = TextUtilities.detectEncoding(subdata);

				if (encodingData == null) {
					// File encoding cannot be detected so use defaultEncoding
					encodingData = defaultEncoding;
				}

				dataString = decodeByteArray(subdata, encodingData);

				// Check for invalid UTF-8 character
				if (encodingData != null && StandardCharsets.UTF_8.name().equalsIgnoreCase(encodingData.getFirst()) && dataString.contains("�")) {
					// If encoding was UTF-8 (detected or default) but contains illegal chars, then try most common other encoding
					encodingData = new Tuple<>(StandardCharsets.ISO_8859_1.name(), false);
					dataString = decodeByteArray(subdata, encodingData);
				}
			} else {
				// only use defined encoding
				dataString = decodeByteArray(subdata, encodingData);
			}

			super.readProperties();
			return dataString;
		}
	}

	/**
	 * Reads the complete file (or data) and scans its text properties.
	 * Errors are only printed to standard error, use {@link #readFileToString()} to get them as exceptions.
	 */
	@Override
	public void readProperties() {
		try {
			readFileToString();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Size of the file or data.
	 *
	 * @return size in bytes, or -1 if a file was not read yet
	 */
	public long getFilesize() {
		return filesize;
	}

	/**
	 * Encoding given or detected when reading.
	 *
	 * @return encoding name and BOM flag, or null if not determined yet
	 */
	public Tuple<String, Boolean> getEncoding() {
		return encodingData;
	}

	/**
	 * Converts an encoding display string (see {@link #getEncodingDisplayString(Tuple)}) to encoding data.
	 * "UTF-16BE-BOM", "UTF-16LE-BOM" and "UTF-8-BOM" (and the Java names "UnicodeBig" and "UnicodeLittle") denote BOM usage.
	 *
	 * @param encodingString encoding display string or charset name
	 * @return encoding name and BOM flag
	 */
	public static Tuple<String, Boolean> getEncodingData(final String encodingString) {
		if ("UnicodeBig".equalsIgnoreCase(encodingString) || "UTF-16BE-BOM".equalsIgnoreCase(encodingString)) {
			return new Tuple<>("UTF-16BE", true);
		} else if ("UnicodeLittle".equalsIgnoreCase(encodingString) || "UTF-16LE-BOM".equalsIgnoreCase(encodingString)) {
			return new Tuple<>("UTF-16LE", true);
		} else if ("UTF-8-BOM".equalsIgnoreCase(encodingString)) {
			return new Tuple<>("UTF-8", true);
		} else {
			return new Tuple<>(encodingString, false);
		}
	}

	/**
	 * Display string of encoding data, e.g. "UTF-8-BOM" for UTF-8 with BOM. "UTF-16" is shown as big endian variant.
	 *
	 * @param encodingData encoding name and BOM flag, may be null
	 * @return display string, empty for null
	 */
	public static String getEncodingDisplayString(final Tuple<String, Boolean> encodingData) {
		if (encodingData == null) {
			return "";
		} else {
			if ("UTF-16BE".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond()) {
				return "UTF-16BE-BOM";
			} else if ("UTF-16BE".equalsIgnoreCase(encodingData.getFirst()) && !encodingData.getSecond()) {
				return "UTF-16BE";
			} else if ("UnicodeBig".equalsIgnoreCase(encodingData.getFirst())) {
				return "UTF-16BE-BOM";
			} else if ("UTF-16".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond()) {
				return "UTF-16BE-BOM";
			} else if ("UTF-16".equalsIgnoreCase(encodingData.getFirst()) && !encodingData.getSecond()) {
				return "UTF-16BE";
			} else if ("UnicodeLittle".equalsIgnoreCase(encodingData.getFirst())) {
				return "UTF-16LE-BOM";
			} else if ("UTF-16LE".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond()) {
				return "UTF-16LE-BOM";
			} else if ("UTF-16LE".equalsIgnoreCase(encodingData.getFirst()) && !encodingData.getSecond()) {
				return "UTF-16LE";
			} else if ("UTF-8".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond()) {
				return "UTF-8-BOM";
			} else {
				return encodingData.getFirst();
			}
		}
	}

	/**
	 * Encodes a text, with a leading BOM if the encoding data says so.
	 *
	 * @param text text to encode
	 * @param encodingData encoding name and BOM flag
	 * @return encoded bytes
	 * @throws Exception if the encoding data is missing or the encoding is not supported
	 */
	public static byte[] encodeString(final String text, final Tuple<String, Boolean> encodingData) throws Exception {
		if (encodingData == null) {
			throw new Exception("Missing encoding data");
		} else {
			final ByteArrayOutputStream output = new ByteArrayOutputStream();
			String encoding = encodingData.getFirst();
			if ("UnicodeBig".equalsIgnoreCase(encodingData.getFirst()) || ("UTF-16BE".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond())
					|| ("UTF-16".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond())) {
				output.write(BOM.UTF_16_BE.getBytes());
				encoding = "UTF-16BE";
			} else if ("UnicodeLittle".equalsIgnoreCase(encodingData.getFirst()) || ("UTF-16LE".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond())) {
				output.write(BOM.UTF_16_LE.getBytes());
				encoding = "UTF-16LE";
			} else if (StandardCharsets.UTF_8.name().equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond()) {
				output.write(BOM.UTF_8.getBytes());
			} else if ("UTF-16".equalsIgnoreCase(encodingData.getFirst())) {
				// Java's "UTF-16" encoder always writes a BOM, but this encoding data says "without BOM" (displayed as "UTF-16BE")
				encoding = "UTF-16BE";
			}
			output.write(text.getBytes(encoding));
			return output.toByteArray();
		}
	}

	/**
	 * Decodes data with the given encoding. A leading UTF-8 or UTF-16 BOM is skipped.
	 *
	 * @param data data to decode
	 * @param encodingData encoding name and BOM flag
	 * @return decoded text
	 * @throws IOException if the encoding is not supported
	 */
	public static String decodeByteArray(final byte[] data, final Tuple<String, Boolean> encodingData) throws IOException {
		if (data.length > 2 && data[0] == BOM.UTF_16_BE.getBytes()[0] && data[1] == BOM.UTF_16_BE.getBytes()[1]) {
			return new String(data, 2, data.length - 2, encodingData.getFirst());
		} else if (data.length > 2 && data[0] == BOM.UTF_16_LE.getBytes()[0] && data[1] == BOM.UTF_16_LE.getBytes()[1]) {
			return new String(data, 2, data.length - 2, encodingData.getFirst());
		} else if (data.length > 3 && data[0] == BOM.UTF_8.getBytes()[0] && data[1] == BOM.UTF_8.getBytes()[1] && data[2] == BOM.UTF_8.getBytes()[2]) {
			return new String(data, 3, data.length - 3, encodingData.getFirst());
		} else {
			return new String(data, encodingData.getFirst());
		}
	}

	/**
	 * Sets the encoding used if the encoding cannot be detected (default UTF-8 without BOM).
	 *
	 * @param value encoding name and BOM flag; null is ignored
	 */
	public void setDefaultEncodingData(final Tuple<String, Boolean> value) {
		if (value != null) {
			defaultEncoding = value;
		}
	}

	/**
	 * Scans the file for the byte positions of all line starts.
	 *
	 * @return line start positions, or null if the file cannot be read
	 * @throws Exception if this reader was created for data in memory (not supported)
	 */
	public List<Long> scanLineStartIndexes() throws Exception {
		if (file != null) {
			try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file))) {
				return TextUtilities.scanLineStartIndexes(input);
			} catch (@SuppressWarnings("unused") final Exception e) {
				return null;
			}
		} else {
			throw new Exception("Not implemented");
		}
	}

	/**
	 * Scans the file for the byte positions of all line breaks.
	 *
	 * @return line break positions, or null if the file cannot be read
	 * @throws Exception if this reader was created for data in memory (not supported)
	 */
	public List<Long> scanLinebreakIndexes() throws Exception {
		if (file != null) {
			try (BufferedInputStream input = new BufferedInputStream(new FileInputStream(file))) {
				return TextUtilities.scanLinebreakIndexes(input);
			} catch (@SuppressWarnings("unused") final Exception e) {
				return null;
			}
		} else {
			throw new Exception("Not implemented");
		}
	}
}
