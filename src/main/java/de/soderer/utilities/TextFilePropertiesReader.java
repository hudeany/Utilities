package de.soderer.utilities;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * {@link TextPropertiesReader} for the content of a file or byte array, which also detects the text encoding.
 * Encodings are described by a {@link Tuple} of the charset name and whether a BOM (byte order mark) is used.
 * If no encoding is given, it is detected: by its BOM, as UTF-8 if it is valid UTF-8, otherwise as the default encoding
 * (see {@link #setDefaultEncodingData(Tuple)}), or as Windows-1252 if the default encoding is UTF-8.
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
			// The data is complete, so a multibyte UTF-8 sequence cut off at its end is invalid UTF-8 and not the end of a sample
			encodingData = detectEncoding(data, true);
		}
		dataString = decodeByteArray(data, encodingData);

		super.readProperties();
		return dataString;
	}

	/**
	 * Reads and decodes a part of the file (or data) and scans its text properties.
	 * The range is moved and shortened to fit into the data, so e.g. a too large start position reads the last bytes.
	 * If no encoding is given, it is detected from a BOM at the start of the data, then UTF-16 and UTF-32 without BOM
	 * from the start of the data, otherwise from the part read.
	 * The part is aligned to character boundaries for UTF-8, UTF-16 and UTF-32: the start position is moved back to the
	 * start of a character, a character cut off at the end of the part is left out.
	 *
	 * @param startPosition position of the first byte to read
	 * @param length number of bytes to read
	 * @return the decoded text
	 * @throws Exception if the file cannot be read or the encoding is not supported
	 */
	public String readFileToString(long startPosition, int length) throws Exception {
		if (data == null) {
			filesize = file.length();
		}
		if (length < 0) {
			length = 0;
		}
		if (startPosition < 0) {
			startPosition = 0;
		}
		if (filesize < length) {
			length = (int) filesize;
		}
		if (filesize < startPosition + length) {
			startPosition = filesize - length;
		}

		// A BOM can only be found at the start of the data, not in a part read from the middle
		final byte[] dataStart = readBytes(0, (int) Math.min(4, filesize));
		if (encodingData == null) {
			final Tuple<String, Boolean> bomEncodingData = TextUtilities.detectEncoding(dataStart);
			if (bomEncodingData.getSecond()) {
				encodingData = bomEncodingData;
			} else if (startPosition > 0) {
				// The byte order of UTF-16 or UTF-32 without BOM is detected from the start of the data, because a part
				// starting at an odd position would be detected with the wrong byte order
				final Tuple<String, Boolean> startEncodingData = TextUtilities.detectEncoding(readBytes(0, (int) Math.min(Math.max(length, 4096), filesize)));
				if (getFixedCharacterSize(startEncodingData.getFirst()) > 1) {
					encodingData = startEncodingData;
				}
			}
		}

		if (encodingData != null) {
			final int characterSize = getFixedCharacterSize(encodingData.getFirst());
			if (characterSize > 1) {
				final int bomLength = getBomLength(dataStart, encodingData.getFirst());
				startPosition -= Math.max(0, startPosition - bomLength) % characterSize;
				// Also end at a character boundary, otherwise the last incomplete character becomes a replacement character
				length -= length % characterSize;
			}
		}

		byte[] subdata = readBytes(startPosition, length);
		if (encodingData == null) {
			// Up to 3 following bytes complete a UTF-8 character cut off at the end of the part, otherwise e.g. a
			// Windows-1252 "\u00DF" (0xDF) at the end of the part would look like the start of such a character
			final int followingLength = (int) Math.min(3, filesize - startPosition - length);
			final byte[] sampleData = followingLength > 0 ? readBytes(startPosition, length + followingLength) : subdata;
			// A part starting within a multibyte UTF-8 character would otherwise not be detected as UTF-8.
			// A sample reaching the end of the data contains its complete end, so a sequence cut off there is invalid.
			encodingData = detectEncoding(startPosition > 0 ? skipLeadingUtf8ContinuationBytes(sampleData) : sampleData, startPosition + sampleData.length >= filesize);
		}

		if (length > 0 && isUtf8(encodingData.getFirst())) {
			// Like for UTF-16 and UTF-32, otherwise a character cut off at the start or end becomes a replacement character
			final long endPosition = startPosition + length;
			final long alignedStartPosition = getUtf8CharacterStart(startPosition);
			final long alignedEndPosition = endPosition < filesize ? getUtf8CharacterStart(endPosition) : endPosition;
			if (alignedStartPosition != startPosition || alignedEndPosition != endPosition) {
				startPosition = alignedStartPosition;
				length = (int) Math.max(0, alignedEndPosition - alignedStartPosition);
				subdata = readBytes(startPosition, length);
			}
		}

		if (startPosition == 0) {
			dataString = decodeByteArray(subdata, encodingData);
		} else {
			// Bytes in the middle of the data, which look like a BOM, are no BOM.
			// The generic UTF-16/UTF-32 charsets take the byte order from a BOM, which a part from the middle does not contain.
			dataString = new String(subdata, getPartDecodingCharset(encodingData.getFirst(), dataStart));
		}

		super.readProperties();
		return dataString;
	}

	/**
	 * Charset for decoding a part from the middle of the data. For the generic "UTF-16" and "UTF-32" (and "UnicodeBig",
	 * "UnicodeLittle") the byte order is taken from the BOM at the start of the data, because the part itself contains
	 * no BOM and would otherwise always be decoded as big endian.
	 */
	private static Charset getPartDecodingCharset(final String encodingName, final byte[] dataStart) {
		final Charset charset = getDecodingCharset(encodingName);
		final String charsetName = charset.name().toUpperCase(Locale.ROOT);
		if ("UTF-16".equals(charsetName)) {
			return startsWith(dataStart, BOM.UTF_16_LE.getBytes()) ? StandardCharsets.UTF_16LE : StandardCharsets.UTF_16BE;
		} else if ("UTF-32".equals(charsetName)) {
			return Charset.forName(startsWith(dataStart, BOM.UTF_32_LE.getBytes()) ? "UTF-32LE" : "UTF-32BE");
		} else {
			return charset;
		}
	}

	/**
	 * Detects the encoding of data without given encoding.
	 *
	 * @param sampleData data to examine
	 * @param isCompleteData true if the data ends with the end of the file (or data), see
	 *            {@link EncodingDetector#detectEncoding(byte[], Charset, boolean)}
	 */
	private Tuple<String, Boolean> detectEncoding(final byte[] sampleData, final boolean isCompleteData) {
		Charset nonUtf8FallbackCharset = null;
		if (defaultEncoding != null && !StandardCharsets.UTF_8.name().equalsIgnoreCase(defaultEncoding.getFirst())) {
			nonUtf8FallbackCharset = Charset.forName(defaultEncoding.getFirst());
		}
		// Not via TextUtilities.detectEncoding(), which always treats the data as a sample that may be cut off
		final EncodingDetector.DetectionResult result = EncodingDetector.detectEncoding(sampleData, nonUtf8FallbackCharset, isCompleteData);
		return new Tuple<>(result.charset.name(), result.hasBom);
	}

	private static boolean isUtf8(final String encodingName) {
		return StandardCharsets.UTF_8.equals(getDecodingCharset(encodingName));
	}

	/**
	 * Returns the start position of the UTF-8 character containing the byte at the given position. If the byte is no
	 * continuation byte, or no lead byte is found within the preceding 3 bytes (invalid data), the position is returned
	 * unchanged.
	 */
	private long getUtf8CharacterStart(final long position) throws IOException {
		if (position <= 0 || position >= filesize) {
			return position;
		}
		// A UTF-8 character has at most 3 continuation bytes
		final long windowStart = Math.max(0, position - 3);
		final byte[] window = readBytes(windowStart, (int) (position - windowStart + 1));
		int index = window.length - 1;
		if (!isUtf8ContinuationByte(window[index])) {
			return position;
		}
		while (index > 0) {
			index--;
			if (!isUtf8ContinuationByte(window[index])) {
				// Only a lead byte (11xxxxxx) starts a multibyte character, an ASCII byte means invalid data
				return (window[index] & 0xC0) == 0xC0 ? windowStart + index : position;
			}
		}
		return position;
	}

	private static boolean isUtf8ContinuationByte(final byte value) {
		return (value & 0xC0) == 0x80;
	}

	/**
	 * Reads bytes from the data in memory or from the file.
	 */
	private byte[] readBytes(final long startPosition, final int length) throws IOException {
		final byte[] result = new byte[length];
		if (data != null) {
			System.arraycopy(data, (int) startPosition, result, 0, length);
		} else {
			try (FileInputStream inputStream = new FileInputStream(file)) {
				// skip() and read() may process fewer bytes than requested
				inputStream.skipNBytes(startPosition);
				final int readBytes = inputStream.readNBytes(result, 0, length);
				if (readBytes != length) {
					throw new IOException("Unexpected end of file: " + readBytes + " of " + length + " bytes read");
				}
			}
		}
		return result;
	}

	private static byte[] skipLeadingUtf8ContinuationBytes(final byte[] bytes) {
		int start = 0;
		// A UTF-8 character has at most 3 continuation bytes
		while (start < bytes.length && start < 3 && (bytes[start] & 0xC0) == 0x80) {
			start++;
		}
		return start == 0 ? bytes : Arrays.copyOfRange(bytes, start, bytes.length);
	}

	/**
	 * Returns the size of each character for encodings with fixed character size.
	 *
	 * @return 2 for UTF-16, 4 for UTF-32, 1 for all other encodings
	 */
	private static int getFixedCharacterSize(final String encodingName) {
		final String charsetName = getDecodingCharset(encodingName).name().toUpperCase(Locale.ROOT);
		if (charsetName.startsWith("UTF-32")) {
			return 4;
		} else if (charsetName.startsWith("UTF-16") || charsetName.contains("UTF-16")) {
			return 2;
		} else {
			return 1;
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
	 * "UTF-16BE-BOM", "UTF-16LE-BOM", "UTF-32BE-BOM", "UTF-32LE-BOM" and "UTF-8-BOM" (and the Java names "UnicodeBig" and
	 * "UnicodeLittle") denote BOM usage.
	 *
	 * @param encodingString encoding display string or charset name
	 * @return encoding name and BOM flag
	 */
	public static Tuple<String, Boolean> getEncodingData(final String encodingString) {
		if ("UnicodeBig".equalsIgnoreCase(encodingString) || "UTF-16BE-BOM".equalsIgnoreCase(encodingString)) {
			return new Tuple<>("UTF-16BE", true);
		} else if ("UnicodeLittle".equalsIgnoreCase(encodingString) || "UTF-16LE-BOM".equalsIgnoreCase(encodingString)) {
			return new Tuple<>("UTF-16LE", true);
		} else if ("UTF-32BE-BOM".equalsIgnoreCase(encodingString)) {
			return new Tuple<>("UTF-32BE", true);
		} else if ("UTF-32LE-BOM".equalsIgnoreCase(encodingString)) {
			return new Tuple<>("UTF-32LE", true);
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
			} else if (("UTF-32BE".equalsIgnoreCase(encodingData.getFirst()) || "UTF-32".equalsIgnoreCase(encodingData.getFirst())) && encodingData.getSecond()) {
				return "UTF-32BE-BOM";
			} else if ("UTF-32LE".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond()) {
				return "UTF-32LE-BOM";
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
			} else if (("UTF-32BE".equalsIgnoreCase(encodingData.getFirst()) || "UTF-32".equalsIgnoreCase(encodingData.getFirst())) && encodingData.getSecond()) {
				// Java's "UTF-32" encoder writes big endian without BOM, so the BOM is written here
				output.write(BOM.UTF_32_BE.getBytes());
				encoding = "UTF-32BE";
			} else if ("UTF-32LE".equalsIgnoreCase(encodingData.getFirst()) && encodingData.getSecond()) {
				output.write(BOM.UTF_32_LE.getBytes());
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
	 * Decodes data with the given encoding. A leading BOM is skipped, if it matches the encoding.
	 *
	 * @param data data to decode
	 * @param encodingData encoding name and BOM flag
	 * @return decoded text
	 * @throws IOException if the encoding is not supported
	 */
	public static String decodeByteArray(final byte[] data, final Tuple<String, Boolean> encodingData) throws IOException {
		try {
			// getBomLength() also looks up the charset, so an unsupported encoding must be caught here, too
			final int bomLength = getBomLength(data, encodingData.getFirst());
			return new String(data, bomLength, data.length - bomLength, getDecodingCharset(encodingData.getFirst()));
		} catch (final IllegalArgumentException e) {
			throw new IOException("Unsupported encoding: " + encodingData.getFirst(), e);
		}
	}

	/**
	 * Returns the charset to decode data of an encoding. "UnicodeBig" and "UnicodeLittle" are decoded with "UTF-16",
	 * which detects the byte order by the BOM itself.
	 */
	private static Charset getDecodingCharset(final String encodingName) {
		if ("UnicodeBig".equalsIgnoreCase(encodingName) || "UnicodeLittle".equalsIgnoreCase(encodingName)) {
			return StandardCharsets.UTF_16;
		} else {
			return Charset.forName(encodingName);
		}
	}

	/**
	 * Returns the length of the BOM at the start of the data, if it matches the encoding. Before, a UTF-16 BOM was
	 * also removed for other encodings, e.g. two bytes of a UTF-32LE BOM, which shifted the rest of the data.
	 */
	private static int getBomLength(final byte[] data, final String encodingName) {
		final String charsetName = getDecodingCharset(encodingName).name().toUpperCase(Locale.ROOT);
		// "UTF-16" and "UTF-32" without byte order evaluate the BOM themselves, so it must not be removed for them
		if (charsetName.equals("UTF-8") && startsWith(data, BOM.UTF_8.getBytes())) {
			return 3;
		} else if (charsetName.equals("UTF-16BE") && startsWith(data, BOM.UTF_16_BE.getBytes())) {
			return 2;
		} else if (charsetName.equals("UTF-16LE") && startsWith(data, BOM.UTF_16_LE.getBytes())) {
			return 2;
		} else if (charsetName.equals("UTF-32BE") && startsWith(data, BOM.UTF_32_BE.getBytes())) {
			return 4;
		} else if (charsetName.equals("UTF-32LE") && startsWith(data, BOM.UTF_32_LE.getBytes())) {
			return 4;
		} else {
			return 0;
		}
	}

	private static boolean startsWith(final byte[] data, final byte[] prefix) {
		if (data.length < prefix.length) {
			return false;
		}
		for (int i = 0; i < prefix.length; i++) {
			if (data[i] != prefix[i]) {
				return false;
			}
		}
		return true;
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
