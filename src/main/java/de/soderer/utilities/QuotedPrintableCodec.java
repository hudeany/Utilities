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
	 * Soft line breaks ("=" followed by LF, CRLF or CR) are removed.
	 * Invalid escape sequences (not followed by two hexadecimal digits) are kept unchanged,
	 * as recommended by RFC 2045, and a single trailing "=" is dropped.
	 *
	 * @param value
	 *            QUOTED-PRINTABLE encoded text, must not be null
	 * @param charset
	 *            charset of the encoded bytes and of unencoded characters
	 * @return decoded text
	 */
	public static String decode(final String value, final Charset charset) {
		final ByteArrayOutputStream decodedByteArray = new ByteArrayOutputStream();
		final char[] charArray = value.toCharArray();
		for (int i = 0; i < charArray.length; i++) {
			if (charArray[i] == '=') {
				if ((i + 1) < charArray.length) {
					final char nextChar = charArray[i + 1];
					if (nextChar == '\n') {
						// Ignore this '=' char and the following LF
						i++;
					} else if (nextChar == '\r' && (i + 2) < charArray.length && charArray[i + 2] == '\n') {
						// Ignore this '=' char and the following CRLF
						i = i + 2;
					} else if (nextChar == '\r') {
						// Ignore this '=' char and the following CR
						i++;
					} else if ((i + 2) < charArray.length && Character.digit(nextChar, 16) >= 0 && Character.digit(charArray[i + 2], 16) >= 0) {
						decodedByteArray.write(BitUtilities.hexToByte(nextChar, charArray[i + 2]));
						i = i + 2;
					} else {
						// Invalid escape sequence: keep the '=' char literally (before, invalid hex digits produced garbage bytes)
						decodedByteArray.write('=');
					}
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
	 * Encode a text in QUOTED-PRINTABLE.
	 * All characters are encoded as "=XX" (uppercase hex), even printable ASCII characters.
	 * Soft line breaks ("=" followed by LF) keep every line at 70 characters at most, well below the limit of 76 characters.
	 *
	 * @param value
	 *            text to encode, must not be null
	 * @param charset
	 *            charset used to convert the text into bytes
	 * @return encoded text
	 */
	public static String encode(final String value, final Charset charset) {
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
