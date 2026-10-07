package de.soderer.utilities;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Helpers for bit strings, hexadecimal representations, XOR and padding of byte data.
 */
public class BitUtilities {
	private static final Pattern BITSTRINGPATTERN_BYTE = Pattern.compile("^[10]{1,8}$");
	private static final Pattern BITSTRINGPATTERN_INTEGER = Pattern.compile("^[10]{1,32}$");
	private static final Pattern HEXADECIMAL_PATTERN = Pattern.compile("\\p{XDigit}*");

	private BitUtilities() {
		// Utility class
	}

	/**
	 * Get the unsigned value of a byte.
	 *
	 * @param value
	 *            signed byte value
	 * @return value from 0 to 255 (value range of an unsigned byte)
	 */
	public static int getUnsignedValue(final byte value) {
		if (value < 0) {
			return (value) + 256;
		} else {
			return value;
		}
	}

	/**
	 * Get the unsigned values of a byte array.
	 *
	 * @param value
	 *            signed byte values, must not be null
	 * @return values from 0 to 255 (value range of an unsigned byte)
	 */
	public static int[] getUnsignedValue(final byte[] value) {
		final int[] returnValue = new int[value.length];
		for (int i = 0; i < value.length; i++) {
			returnValue[i] = getUnsignedValue(value[i]);
		}
		return returnValue;
	}

	/**
	 * Convert an unsigned value (0 to 255) into the Java byte with the same bit pattern.
	 * Java bytes are always signed, so values above 127 become negative.
	 *
	 * @param value
	 *            unsigned value from 0 to 255
	 * @return byte with the same bit pattern
	 * @throws Exception
	 *             if the value is out of the range 0 to 255
	 */
	public static byte getUnsignedAsByte(final int value) throws Exception {
		if (value < 0 || value > 255) {
			throw new Exception("Unsigned value out of range for byte");
		} else if (value > 127) {
			return (byte) (value - 256);
		} else {
			return (byte) value;
		}
	}

	/**
	 * Get the bits of a byte as text, most significant bit first.
	 *
	 * @param byteItem
	 *            byte value
	 * @return text of 8 characters '0' or '1'
	 */
	public static String getBitString(final byte byteItem) {
		final StringBuilder result = new StringBuilder();
		for (int i = 7; i >= 0; i--) {
			result.append((byte) ((byteItem >>> i) & 0x01));
		}
		return result.toString();
	}

	/**
	 * Get the bits of an int as text, most significant bit first.
	 *
	 * @param intItem
	 *            int value
	 * @return text of 32 characters '0' or '1'
	 */
	public static String getBitString(final int intItem) {
		final StringBuilder result = new StringBuilder();
		for (int i = 31; i >= 0; i--) {
			result.append((byte) ((intItem >>> i) & 0x01));
		}
		return result.toString();
	}

	/**
	 * Get the bits of a byte array as text, with a blank between the bytes.
	 *
	 * @param byteArray
	 *            bytes, must not be null
	 * @return text of 8 characters '0' or '1' per byte, separated by blanks
	 */
	public static String getBitString(final byte[] byteArray) {
		return getBitString(byteArray, " ");
	}

	/**
	 * Get the bits of a byte array as text, most significant bit of each byte first.
	 *
	 * @param byteArray
	 *            bytes, must not be null
	 * @param byteSeparator
	 *            text inserted between the bytes
	 * @return text of 8 characters '0' or '1' per byte, separated by byteSeparator
	 */
	public static String getBitString(final byte[] byteArray, final String byteSeparator) {
		final StringBuilder result = new StringBuilder();
		for (final byte byteItem : byteArray) {
			if (result.length() > 0) {
				result.append(byteSeparator);
			}

			for (int i = 7; i >= 0; i--) {
				result.append((byte) ((byteItem >>> i) & 0x01));
			}
		}
		return result.toString();
	}

	/**
	 * Parse a bit string of up to 8 bits (most significant bit first) into a byte.
	 *
	 * @param bitString
	 *            text of 1 to 8 characters '0' or '1', surrounding whitespace is ignored
	 * @return byte value; "11111111" yields -1
	 * @throws Exception
	 *             if the text is empty or no valid bit string
	 */
	public static byte getByteFromBitString(String bitString) throws Exception {
		if (Utilities.isEmpty(bitString)) {
			throw new Exception("Invalid bitstring for byte");
		}

		bitString = bitString.trim();

		if (!BITSTRINGPATTERN_BYTE.matcher(bitString).matches()) {
			throw new Exception("Invalid bitstring for byte");
		}

		byte result = 0;
		int posValue = 1;
		final char[] characters = bitString.toCharArray();
		for (int i = characters.length - 1; i >= 0; i--) {
			if (characters[i] == '1') {
				result += posValue;
			}
			posValue *= 2;
		}
		return result;
	}

	/**
	 * Parse a bit string of up to 32 bits (most significant bit first) into an int.
	 *
	 * @param bitString
	 *            text of 1 to 32 characters '0' or '1', surrounding whitespace is ignored
	 * @return int value; a leading '1' of 32 bits yields a negative value
	 * @throws Exception
	 *             if the text is empty or no valid bit string
	 */
	public static int getIntegerFromBitString(String bitString) throws Exception {
		if (Utilities.isEmpty(bitString)) {
			throw new Exception("Invalid bitstring for integer");
		}

		bitString = bitString.trim();

		if (!BITSTRINGPATTERN_INTEGER.matcher(bitString).matches()) {
			throw new Exception("Invalid bitstring for integer");
		}

		int result = 0;
		int posValue = 1;
		final char[] characters = bitString.toCharArray();
		for (int i = characters.length - 1; i >= 0; i--) {
			if (characters[i] == '1') {
				result += posValue;
			}
			posValue *= 2;
		}
		return result;
	}

	/**
	 * Decode a hexadecimal string, optionally prefixed by "0x" or "0X".
	 *
	 * @param value
	 *            hexadecimal text with an even number of digits (upper or lower case), may be null
	 * @return decoded bytes (empty for an empty text), or null for null input
	 * @throws RuntimeException
	 *             if the text contains a non hexadecimal character
	 * @throws IllegalArgumentException
	 *             if the number of hexadecimal digits is odd
	 */
	public static byte[] fromHexString(final String value) {
		if (value == null) {
			return null;
		} else {
			String decodeValue = value;
			if (value.startsWith("0x") || value.startsWith("0X")) {
				decodeValue = decodeValue.substring(2);
			}
			if (!HEXADECIMAL_PATTERN.matcher(decodeValue).matches()) {
				throw new RuntimeException("String contains non hexadecimal character: " + value);
			}
			return decodeHexDigits(decodeValue);
		}
	}

	/**
	 * Decode a hexadecimal string, optionally ignoring all characters that are no hexadecimal digits (like separators).
	 *
	 * @param value
	 *            hexadecimal text, may be null
	 * @param ignoreNonHexCharacters
	 *            true to remove all non hexadecimal characters before decoding (a "0x" prefix is then not removed completely, only its 'x')
	 * @return decoded bytes, or null for null input
	 * @throws RuntimeException
	 *             if the text contains a non hexadecimal character and ignoreNonHexCharacters is false
	 * @throws IllegalArgumentException
	 *             if the number of hexadecimal digits is odd
	 */
	public static byte[] fromHexString(final String value, final boolean ignoreNonHexCharacters) {
		if (value == null) {
			return null;
		} else if (ignoreNonHexCharacters) {
			return fromHexString(value.toLowerCase(Locale.ROOT).replaceAll("[^abcdef0-9]", ""));
		} else {
			return fromHexString(value);
		}
	}

	/**
	 * Uppercase hexadecimal display of byte array data without separators.
	 *
	 * @param data
	 *            bytes, may be null
	 * @return two uppercase hexadecimal digits per byte, or null for null input
	 */
	public static String toHexString(final byte[] data) {
		if (data == null) {
			return null;
		} else {
			final char[] hexArray = "0123456789ABCDEF".toCharArray();

			final char[] hexChars = new char[data.length * 2];
			for (int j = 0; j < data.length; j++) {
				final int v = data[j] & 0xFF;
				hexChars[j * 2] = hexArray[v >>> 4];
				hexChars[j * 2 + 1] = hexArray[v & 0x0F];
			}
			return new String(hexChars);
		}
	}

	/**
	 * Lowercase hexadecimal display of byte array data with optional separator between the bytes.
	 * Note: in contrast to {@link #toHexString(byte[])} the result is lowercase (also the separator is lowercased).
	 *
	 * @param data
	 *            bytes, must not be null
	 * @param separator
	 *            text inserted between the bytes, null for none
	 * @return two lowercase hexadecimal digits per byte, separated by the separator
	 */
	public static String toHexString(final byte[] data, final String separator) {
		final StringBuilder returnString = new StringBuilder();
		for (final byte dataByte : data) {
			if (returnString.length() > 0 && separator != null) {
				returnString.append(separator);
			}
			returnString.append(String.format("%02X", dataByte));
		}
		return returnString.toString().toLowerCase(Locale.ROOT);
	}

	/**
	 * Hex dump of byte data with uppercase hexadecimal digits, like in a hex editor.
	 *
	 * @param data
	 *            bytes, must not be null
	 * @param separator
	 *            text inserted between the bytes within a line
	 * @param bytesBeforeLinebreak
	 *            number of bytes per line, values less than 1 for no line breaks
	 * @param insertHexLineNumber
	 *            prefix each line with its start offset like "00000010h: "
	 * @param insertAsciiText
	 *            append " ; " and the printable ASCII characters of each line, other bytes are shown as '.'
	 * @return hex dump text, lines separated by "\n"
	 */
	public static String getHexString(final byte[] data, final String separator, final int bytesBeforeLinebreak, final boolean insertHexLineNumber, final boolean insertAsciiText) {
		final StringBuilder returnString = new StringBuilder();
		int byteCount = 0;
		StringBuilder asciiTextLine = new StringBuilder();
		for (final byte dataByte : data) {
			if (byteCount > 0) {
				if (bytesBeforeLinebreak > 0 && byteCount % bytesBeforeLinebreak == 0) {
					if (insertAsciiText) {
						returnString.append(" ; ");
						returnString.append(asciiTextLine);
						asciiTextLine = new StringBuilder();
					}
					returnString.append('\n');
					if (insertHexLineNumber) {
						returnString.append(String.format("%08Xh: ", byteCount));
					}
				} else {
					returnString.append(separator);
				}
			}
			returnString.append(String.format("%02X", dataByte));
			// Only printable ASCII is shown (before, bytes >= 0x80 were decoded by the default charset, yielding replacement characters)
			if (dataByte >= 32 && dataByte < 127) {
				asciiTextLine.append((char) dataByte);
			} else {
				asciiTextLine.append('.');
			}
			byteCount++;
		}

		if (insertAsciiText && asciiTextLine.length() > 0) {
			// Without line breaks there is no last line to fill up (before, this caused a division by zero)
			if (bytesBeforeLinebreak > 0) {
				final int bytesInLastLine = byteCount % bytesBeforeLinebreak;
				if (bytesInLastLine > 0) {
					returnString.append(TextUtilities.repeatString("   ", bytesBeforeLinebreak - bytesInLastLine));
				}
			}
			returnString.append(" ; ");
			returnString.append(asciiTextLine.toString());
		}

		if (insertHexLineNumber) {
			return String.format("%08Xh: ", 0) + returnString.toString();
		} else {
			return returnString.toString();
		}
	}

	/**
	 * Decode a hexadecimal string without prefix.
	 *
	 * @param value
	 *            hexadecimal text with an even number of digits (upper or lower case), must not be null
	 * @return decoded bytes
	 * @throws IllegalArgumentException
	 *             if the text contains a non hexadecimal character or has an odd length
	 */
	public static byte[] getByteArrayFromHexString(final String value) {
		// Validated decoding (before, invalid characters silently produced wrong bytes and an odd length caused a StringIndexOutOfBoundsException)
		return decodeHexDigits(value);
	}

	private static byte[] decodeHexDigits(final String value) {
		final int length = value.length();
		if (length % 2 != 0) {
			throw new IllegalArgumentException("Hexadecimal string has an odd number of digits: " + value);
		}
		final byte[] data = new byte[length / 2];
		for (int i = 0; i < length; i += 2) {
			data[i / 2] = hexToByte(value.charAt(i), value.charAt(i + 1));
		}
		return data;
	}

	/**
	 * XOR byte values of multiple byte arrays of the same length.
	 *
	 * @param byteArrays
	 *            byte arrays, at least one; all must have the length of the first array
	 * @return new array, each byte is the XOR of the bytes at the same position
	 */
	public static byte[] xor(final byte[]... byteArrays) {
		return xor(Arrays.asList(byteArrays));
	}

	/**
	 * XOR byte values of multiple byte arrays of the same length.
	 *
	 * @param byteArrays
	 *            byte arrays, at least one; all must have the length of the first array
	 * @return new array, each byte is the XOR of the bytes at the same position
	 * @throws IllegalArgumentException
	 *             if the arrays differ in length
	 */
	public static byte[] xor(final List<byte[]> byteArrays) {
		final byte[] returnArray = new byte[byteArrays.get(0).length];

		for (int i = 0; i < byteArrays.size(); i++) {
			if (byteArrays.get(i).length != returnArray.length) {
				// Before, longer arrays were silently cut and shorter ones caused an ArrayIndexOutOfBoundsException
				throw new IllegalArgumentException("Byte arrays for XOR differ in length");
			}
			for (int j = 0; j < returnArray.length; j++) {
				returnArray[j] = xor(returnArray[j], byteArrays.get(i)[j]);
			}
		}

		return returnArray;
	}

	/**
	 * XOR multiple byte values.
	 *
	 * @param keyData
	 *            byte values
	 * @return XOR of all values, 0 for no values
	 */
	public static byte xor(final byte... keyData) {
		byte resultItem = 0;

		for (final byte key : keyData) {
			resultItem = (byte) (resultItem ^ key);
		}

		return resultItem;
	}

	/**
	 * Concatenate byte arrays.
	 *
	 * @param arrays
	 *            byte arrays, none of them may be null
	 * @return new array with the content of all arrays in the given order
	 * @throws Exception
	 *             if writing the data fails (does not happen for in-memory data)
	 */
	public static byte[] joinByteArrays(final byte[]... arrays) throws Exception {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (final byte[] array : arrays) {
			out.write(array);
		}
		return out.toByteArray();
	}

	/**
	 * Pad data to a multiple of the padding size, each padding byte containing the number of padding bytes (like PKCS#7).
	 * At least one padding byte is always added, so this padding type can be removed after a decryption.
	 *
	 * @param data
	 *            data to pad, must not be null
	 * @param paddingSize
	 *            block size from 1 to 256 (256 is stored as padding value 0)
	 * @return new padded array
	 */
	public static byte[] addLengthCodedPadding(final byte[] data, final int paddingSize) {
		final byte[] dataPadded;
		if (data.length % paddingSize != 0) {
			dataPadded = new byte[((data.length / paddingSize) + 1) * paddingSize];
		} else {
			dataPadded = new byte[data.length + paddingSize];
		}
		for (int i = 0; i < data.length; i++) {
			dataPadded[i] = data[i];
		}
		final byte padValue = (byte) (dataPadded.length - data.length);
		for (int i = data.length; i < dataPadded.length; i++) {
			dataPadded[i] = padValue;
		}
		return dataPadded;
	}

	/**
	 * Pad data to a multiple of the padding size with secure random bytes.
	 * This padding type is irremovable, because the original length is not stored.
	 *
	 * @param data
	 *            data to pad, must not be null
	 * @param paddingSize
	 *            block size, must be greater than 0
	 * @return new padded array, or the given array itself if its length already is a multiple of the padding size
	 */
	public static byte[] addRandomPadding(final byte[] data, final int paddingSize) {
		if (data.length % paddingSize != 0) {
			final byte[] dataPadded;
			dataPadded = new byte[((data.length / paddingSize) + 1) * paddingSize];
			for (int i = 0; i < data.length; i++) {
				dataPadded[i] = data[i];
			}
			final byte[] randomPadding = new byte[dataPadded.length - data.length];
			new SecureRandom().nextBytes(randomPadding);
			for (int i = 0; i < randomPadding.length; i++) {
				dataPadded[data.length + i] = randomPadding[i];
			}
			return dataPadded;
		} else {
			return data;
		}
	}

	/**
	 * Decode the first two characters of a text as hexadecimal byte value.
	 *
	 * @param data
	 *            text starting with two hexadecimal digits, must not be null
	 * @return byte value
	 * @throws IllegalArgumentException
	 *             if one of the characters is no hexadecimal digit
	 */
	public static byte hexToByte(final String data) {
		return BitUtilities.hexToByte(data.charAt(0), data.charAt(1));
	}

	/**
	 * Decode two hexadecimal digits as byte value.
	 *
	 * @param char1
	 *            high nibble digit (upper or lower case)
	 * @param char2
	 *            low nibble digit (upper or lower case)
	 * @return byte value
	 * @throws IllegalArgumentException
	 *             if one of the characters is no hexadecimal digit
	 */
	public static byte hexToByte(final char char1, final char char2) {
		final int highNibble = Character.digit(char1, 16);
		final int lowNibble = Character.digit(char2, 16);
		if (highNibble < 0 || lowNibble < 0) {
			// Before, Character.digit's -1 for invalid digits silently produced wrong byte values
			throw new IllegalArgumentException("Invalid hexadecimal digits: '" + char1 + char2 + "'");
		}
		return (byte) ((highNibble << 4) + lowNibble);
	}

	/**
	 * Encode a byte value as two uppercase hexadecimal digits.
	 *
	 * @param data
	 *            byte value
	 * @return two uppercase hexadecimal digits
	 */
	public static String byteToHex(final byte data) {
		return String.format("%02X", data);
	}
}
