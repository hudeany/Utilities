package de.soderer.utilities;

import java.io.ByteArrayOutputStream;
import java.nio.charset.Charset;

/**
 * Encoder and decoder for the QUOTED-PRINTABLE content transfer encoding.
 *
 * <p>See: <a href="https://en.wikipedia.org/wiki/Quoted-printable">https://en.wikipedia.org/wiki/Quoted-printable</a></p>
 */
public class QuotedPrintableCodec {
	private QuotedPrintableCodec() {
		// Utility class
	}

	/**
	 * Decode text from QUOTED-PRINTABLE.
	 * Soft line breaks ("=" followed by LF, CRLF or CR) are removed, also if blanks or tabs were added between the "="
	 * and the line break (RFC 2045 allows transports to add such trailing whitespace).
	 * Invalid escape sequences (not followed by two hexadecimal digits) are kept unchanged,
	 * as recommended by RFC 2045, and a single trailing "=" is dropped.
	 *
	 * @param value
	 *            QUOTED-PRINTABLE encoded text, must not be null
	 * @param charset
	 *            charset of the encoded bytes and of unencoded characters, must not be null
	 * @return decoded text
	 */
	public static String decode(final String value, final Charset charset) {
		if (value == null) {
			throw new IllegalArgumentException("Value must not be null");
		} else if (charset == null) {
			throw new IllegalArgumentException("Charset must not be null");
		}

		final ByteArrayOutputStream decodedByteArray = new ByteArrayOutputStream();
		final char[] charArray = value.toCharArray();
		for (int i = 0; i < charArray.length; i++) {
			if (charArray[i] == '=') {
				final int softLineBreakEnd = getSoftLineBreakEnd(charArray, i + 1);
				if (softLineBreakEnd >= 0) {
					// Ignore this '=' char, optional trailing whitespace and the following line break
					i = softLineBreakEnd - 1;
				} else if ((i + 2) < charArray.length && isHexDigit(charArray[i + 1]) && isHexDigit(charArray[i + 2])) {
					decodedByteArray.write((Character.digit(charArray[i + 1], 16) << 4) | Character.digit(charArray[i + 2], 16));
					i = i + 2;
				} else if ((i + 1) < charArray.length) {
					// Invalid escape sequence: keep the '=' char literally
					decodedByteArray.write('=');
				}
				// else: ignore a single '=' char at the end of the data
			} else {
				// Encode the whole run of unescaped characters at once, so surrogate pairs (characters outside the BMP) stay intact
				int runEnd = i + 1;
				while (runEnd < charArray.length && charArray[runEnd] != '=') {
					runEnd++;
				}
				final byte[] runBytes = new String(charArray, i, runEnd - i).getBytes(charset);
				decodedByteArray.write(runBytes, 0, runBytes.length);
				i = runEnd - 1;
			}
		}
		return new String(decodedByteArray.toByteArray(), charset);
	}

	/**
	 * Checks for a soft line break after a '=' char: optional blanks or tabs followed by LF, CRLF or CR.
	 *
	 * @param charArray
	 *            encoded text
	 * @param start
	 *            index directly after the '=' char
	 * @return index after the line break, or -1 if there is no soft line break at this position
	 */
	private static int getSoftLineBreakEnd(final char[] charArray, final int start) {
		int index = start;
		while (index < charArray.length && (charArray[index] == ' ' || charArray[index] == '\t')) {
			index++;
		}
		if (index < charArray.length && charArray[index] == '\n') {
			return index + 1;
		} else if (index < charArray.length && charArray[index] == '\r') {
			if (index + 1 < charArray.length && charArray[index + 1] == '\n') {
				return index + 2;
			} else {
				return index + 1;
			}
		} else {
			return -1;
		}
	}

	/**
	 * Only ASCII hex digits, because {@link Character#digit(char, int)} also accepts other Unicode digits like
	 * fullwidth digits.
	 */
	private static boolean isHexDigit(final char character) {
		return (character >= '0' && character <= '9') || (character >= 'A' && character <= 'F') || (character >= 'a' && character <= 'f');
	}

	/**
	 * Encode a text in QUOTED-PRINTABLE.
	 * All characters are encoded as "=XX" (uppercase hex), even printable ASCII characters.
	 * Soft line breaks ("=" followed by LF) keep every line at 70 characters at most, well below the limit of 76 characters.
	 *
	 * @param value
	 *            text to encode, must not be null
	 * @param charset
	 *            charset used to convert the text into bytes, must not be null
	 * @return encoded text
	 */
	public static String encode(final String value, final Charset charset) {
		if (value == null) {
			throw new IllegalArgumentException("Value must not be null");
		} else if (charset == null) {
			throw new IllegalArgumentException("Charset must not be null");
		}

		final StringBuilder encodedText = new StringBuilder();
		int currentLineLength = 0;
		for (final byte data : value.getBytes(charset)) {
			if (currentLineLength == 69) {
				encodedText.append("=\n");
				currentLineLength = 0;
			}

			encodedText.append("=").append(BitUtilities.byteToHex(data));
			currentLineLength += 3;
		}
		return encodedText.toString();
	}
}
