package de.soderer.utilities;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * Detects the most likely text encoding from a byte sample taken from the start of a file.
 * Designed to work on just a sample (not the whole file), so it is cheap to run even on a large
 * file - a few KB is usually enough, but a larger sample (e.g. 64 KB - 1 MB) makes the UTF-8
 * validity check more reliable, especially for files that happen to start with a long run of
 * plain ASCII before any "interesting" byte appears.
 *
 * Detection order:
 * <ol>
 *   <li>Byte-order-mark (BOM) signatures - unambiguous when present.</li>
 *   <li>UTF-32 and UTF-16 without BOM, recognized by their zero bytes (see
 *       {@link #detectUtf32WithoutBom(byte[])} and {@link #detectUtf16WithoutBom(byte[])}). Text without any
 *       zero byte, like pure CJK text in UTF-16, cannot be recognized this way.</li>
 *   <li>Whether the sample is well-formed UTF-8 (strict validation: rejects overlong encodings
 *       and directly-encoded surrogate halves; a sequence truncated at the very end of the sample
 *       is ignored rather than treated as invalid, since the sample may simply be cut off
 *       mid-character).</li>
 *   <li>Otherwise, Windows-1252 (or the fallback charset given to {@link #detectEncoding(byte[], Charset)}). Windows-1252 and ISO-8859-1 are byte-identical outside the
 *       0x80-0x9F range, and real C1 control codes in that range essentially never appear in
 *       genuine text, so there is no reliable way to tell them apart from content alone once
 *       UTF-8 has been ruled out - and in practice, non-UTF-8 8-bit text is overwhelmingly more
 *       likely to be Windows-1252 than genuine ISO-8859-1. This also makes a save-as-Windows-1252
 *       round trip detect correctly even when the saved text happens not to use any byte in the
 *       0x80-0x9F range at all (e.g. plain German text with just &auml;&ouml;&uuml;&szlig; and no smart quotes, dashes,
 *       or euro sign).</li>
 * </ol>
 *
 * Pure 7-bit ASCII content is reported as UTF-8, since ASCII is a strict subset of UTF-8.
 */
public class EncodingDetector {
	/**
	 * Utility class
	 */
	private EncodingDetector() {
		// Utility class
	}

	/**
	 * Result of an encoding detection.
	 */
	public static final class DetectionResult {
		/** The detected charset. */
		public final Charset charset;
		/** True if the data starts with a BOM. */
		public final boolean hasBom;
		/** The length of the BOM in bytes, 0 if there is no BOM. */
		public final int bomLength;

		/**
		 * Creates a new detection result.
		 *
		 * @param charset
		 *            the detected charset
		 * @param hasBom
		 *            true if the data starts with a BOM
		 * @param bomLength
		 *            the length of the BOM in bytes, 0 if there is no BOM
		 */
		public DetectionResult(final Charset charset, final boolean hasBom, final int bomLength) {
			this.charset = charset;
			this.hasBom = hasBom;
			this.bomLength = bomLength;
		}

		@Override
		public String toString() {
			return charset.name() + (hasBom ? " (BOM, " + bomLength + " bytes)" : " (no BOM)");
		}
	}

	/**
	 * Detects the most likely encoding of a byte sample taken from the start of the data.
	 *
	 * @param sampleBytes
	 *            the first bytes of the data, may be null
	 * @return the detection result, UTF-8 without BOM for empty or null data
	 */
	public static DetectionResult detectEncoding(final byte[] sampleBytes) {
		return detectEncoding(sampleBytes, null);
	}

	/**
	 * Detects the most likely encoding of a byte sample taken from the start of the data.
	 *
	 * @param sampleBytes
	 *            the first bytes of the data, may be null
	 * @param nonUtf8FallbackCharset
	 *            the charset for data without BOM which is no valid UTF-8, or null for Windows-1252.
	 *            A fallback charset which cannot decode such data (UTF-8 itself, UTF-16 or UTF-32) is
	 *            ignored and Windows-1252 is used instead.
	 * @return the detection result, UTF-8 without BOM for empty or null data
	 */
	public static DetectionResult detectEncoding(final byte[] sampleBytes, final Charset nonUtf8FallbackCharset) {
		if (sampleBytes == null || sampleBytes.length == 0) {
			return new DetectionResult(StandardCharsets.UTF_8, false, 0);
		}

		final DetectionResult bomResult = detectBom(sampleBytes);
		if (bomResult != null) {
			return bomResult;
		}

		// Must be checked before UTF-8, because ASCII text in UTF-16 or UTF-32 is also well-formed UTF-8 (with NUL characters)
		final Charset utf32Charset = detectUtf32WithoutBom(sampleBytes);
		if (utf32Charset != null) {
			return new DetectionResult(utf32Charset, false, 0);
		}
		final Charset utf16Charset = detectUtf16WithoutBom(sampleBytes);
		if (utf16Charset != null) {
			return new DetectionResult(utf16Charset, false, 0);
		}

		if (isWellFormedUtf8(sampleBytes)) {
			return new DetectionResult(StandardCharsets.UTF_8, false, 0);
		}

		if (nonUtf8FallbackCharset != null && !nonUtf8FallbackCharset.name().toUpperCase(Locale.ROOT).startsWith("UTF-")) {
			return new DetectionResult(nonUtf8FallbackCharset, false, 0);
		} else {
			return new DetectionResult(Charset.forName("windows-1252"), false, 0);
		}
	}

	/**
	 * Checks longer/more specific BOM signatures before shorter ones - the UTF-32LE BOM
	 * (FF FE 00 00) starts with the same two bytes as the UTF-16LE BOM (FF FE), so it must be
	 * checked first or a UTF-32LE file would be misdetected as UTF-16LE.
	 */
	private static DetectionResult detectBom(final byte[] b) {
		if (b.length >= 3 && (b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF) {
			return new DetectionResult(StandardCharsets.UTF_8, true, 3);
		}
		if (b.length >= 4 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE && b[2] == 0x00 && b[3] == 0x00) {
			return new DetectionResult(Charset.forName("UTF-32LE"), true, 4);
		}
		if (b.length >= 4 && b[0] == 0x00 && b[1] == 0x00 && (b[2] & 0xFF) == 0xFE && (b[3] & 0xFF) == 0xFF) {
			return new DetectionResult(Charset.forName("UTF-32BE"), true, 4);
		}
		if (b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE) {
			return new DetectionResult(StandardCharsets.UTF_16LE, true, 2);
		}
		if (b.length >= 2 && (b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF) {
			return new DetectionResult(StandardCharsets.UTF_16BE, true, 2);
		}
		return null;
	}

	/**
	 * Detects UTF-16 without BOM by the distribution of zero bytes.
	 * <p>
	 * Text in 8-bit encodings and in UTF-8 never contains zero bytes. In UTF-16 every character of the ranges
	 * U+0001-U+00FF (ASCII, Latin-1) and every character U+xx00 has a zero byte, which is always the high byte
	 * of its code unit: the odd byte for little endian, the even byte for big endian. Even non Latin text usually has
	 * enough blanks, line breaks or digits for this. Data is detected as UTF-16, if
	 * </p>
	 * <ul>
	 * <li>at least 5% of the code units contain a zero byte (so a single stray zero byte in a long 8-bit text does not
	 * count) and at least 90% of the zero bytes are on the same side (odd or even byte),</li>
	 * <li>no code unit is a control character other than tab, line feed, form feed or carriage return (NUL characters
	 * and other control characters are typical for binary data and UTF-32) and</li>
	 * <li>the code units contain no invalid surrogates.</li>
	 * </ul>
	 *
	 * @param b
	 *            the sample, an odd last byte is ignored
	 * @return UTF-16LE or UTF-16BE, or null if the data does not look like UTF-16
	 */
	private static Charset detectUtf16WithoutBom(final byte[] b) {
		final int length = b.length - (b.length % 2);
		if (length < 4) {
			return null;
		}

		int zeroBytesEven = 0;
		int zeroBytesOdd = 0;
		for (int i = 0; i < length; i += 2) {
			if (b[i] == 0) {
				zeroBytesEven++;
			} else if (b[i + 1] == 0) {
				zeroBytesOdd++;
			}
		}

		final int zeroBytes = zeroBytesEven + zeroBytesOdd;
		final boolean littleEndian;
		if (zeroBytes == 0 || zeroBytes * 20 < length / 2) {
			return null;
		} else if (zeroBytesOdd * 10 >= zeroBytes * 9) {
			littleEndian = true;
		} else if (zeroBytesEven * 10 >= zeroBytes * 9) {
			littleEndian = false;
		} else {
			return null;
		}

		if (!isPlausibleUtf16(b, length, littleEndian)) {
			return null;
		}
		return littleEndian ? StandardCharsets.UTF_16LE : StandardCharsets.UTF_16BE;
	}

	/**
	 * Checks that there are no unusual control characters, that high surrogates are followed by low surrogates and
	 * low surrogates are preceded by high surrogates. A high surrogate at the very end of the sample is accepted,
	 * because the sample may be cut off.
	 */
	private static boolean isPlausibleUtf16(final byte[] b, final int length, final boolean littleEndian) {
		boolean expectLowSurrogate = false;
		for (int i = 0; i < length; i += 2) {
			final int codeUnit = littleEndian ? ((b[i + 1] & 0xFF) << 8) | (b[i] & 0xFF) : ((b[i] & 0xFF) << 8) | (b[i + 1] & 0xFF);
			if (isUnusualControlCharacter(codeUnit)) {
				return false;
			}
			final boolean isHighSurrogate = codeUnit >= 0xD800 && codeUnit <= 0xDBFF;
			final boolean isLowSurrogate = codeUnit >= 0xDC00 && codeUnit <= 0xDFFF;
			if (expectLowSurrogate != isLowSurrogate) {
				return false;
			}
			expectLowSurrogate = isHighSurrogate;
		}
		return true;
	}

	/**
	 * Detects UTF-32 without BOM. Every code point is at most 0x10FFFF, so in each 4 byte unit the highest byte is
	 * always zero and the second highest byte is at most 0x10. Text characters below U+10000 (nearly all) have two zero
	 * bytes. Data is detected as UTF-32, if all units are valid code points (no surrogates, no control characters
	 * except tab, line feed, form feed and carriage return) and at least half of them are below U+10000.
	 *
	 * @param b
	 *            the sample, incomplete last bytes are ignored
	 * @return UTF-32LE or UTF-32BE, or null if the data does not look like UTF-32
	 */
	private static Charset detectUtf32WithoutBom(final byte[] b) {
		final int length = b.length - (b.length % 4);
		if (length < 8) {
			return null;
		}
		if (isPlausibleUtf32(b, length, true)) {
			return Charset.forName("UTF-32LE");
		} else if (isPlausibleUtf32(b, length, false)) {
			return Charset.forName("UTF-32BE");
		} else {
			return null;
		}
	}

	private static boolean isPlausibleUtf32(final byte[] b, final int length, final boolean littleEndian) {
		int bmpCount = 0;
		for (int i = 0; i < length; i += 4) {
			final int codePoint;
			if (littleEndian) {
				codePoint = ((b[i + 3] & 0xFF) << 24) | ((b[i + 2] & 0xFF) << 16) | ((b[i + 1] & 0xFF) << 8) | (b[i] & 0xFF);
			} else {
				codePoint = ((b[i] & 0xFF) << 24) | ((b[i + 1] & 0xFF) << 16) | ((b[i + 2] & 0xFF) << 8) | (b[i + 3] & 0xFF);
			}
			if (codePoint > 0x10FFFF || (codePoint >= 0xD800 && codePoint <= 0xDFFF) || isUnusualControlCharacter(codePoint)) {
				return false;
			} else if (codePoint < 0x10000) {
				bmpCount++;
			}
		}
		return bmpCount * 2 >= length / 4;
	}

	/**
	 * Checks for control characters, which do not appear in text: all below U+0020 except tab, line feed, form feed
	 * and carriage return, including the NUL character.
	 */
	private static boolean isUnusualControlCharacter(final int codePoint) {
		return codePoint < 0x20 && codePoint != '\t' && codePoint != '\n' && codePoint != '\f' && codePoint != '\r';
	}

	private static boolean isWellFormedUtf8(final byte[] b) {
		int i = 0;
		while (i < b.length) {
			final int byte1 = b[i] & 0xFF;

			if (byte1 <= 0x7F) {
				i++;
				continue;
			}

			final int continuationBytesNeeded;
			final int minCodepoint;
			final int firstByteValueBits;

			if (byte1 == 0xC0 || byte1 == 0xC1 || byte1 > 0xF4) {
				// These leading bytes can only start overlong encodings or code points above U+10FFFF, which are
				// rejected below too, but this check also covers a sequence truncated at the end of the sample
				return false;
			} else if ((byte1 & 0xE0) == 0xC0) {
				continuationBytesNeeded = 1;
				minCodepoint = 0x80;
				firstByteValueBits = byte1 & 0x1F;
			} else if ((byte1 & 0xF0) == 0xE0) {
				continuationBytesNeeded = 2;
				minCodepoint = 0x800;
				firstByteValueBits = byte1 & 0x0F;
			} else if ((byte1 & 0xF8) == 0xF0) {
				continuationBytesNeeded = 3;
				minCodepoint = 0x10000;
				firstByteValueBits = byte1 & 0x07;
			} else {
				// Continuation byte (0x80-0xBF) or invalid leading byte (0xF8-0xFF) where a new
				// character was expected to start.
				return false;
			}

			if (i + continuationBytesNeeded >= b.length) {
				// Truncated at the end of the sample - the sample may simply be cut off mid-character,
				// which is not a sign of a different encoding. But the bytes that are present must
				// still be continuation bytes, otherwise e.g. a Windows-1252 umlaut byte 0xE4 followed by an
				// ASCII character at the very end would be accepted as UTF-8.
				for (int j = i + 1; j < b.length; j++) {
					if ((b[j] & 0xC0) != 0x80) {
						return false;
					}
				}
				break;
			}

			int codepoint = firstByteValueBits;
			for (int j = 1; j <= continuationBytesNeeded; j++) {
				final int continuationByte = b[i + j] & 0xFF;
				if ((continuationByte & 0xC0) != 0x80) {
					return false;
				}
				codepoint = (codepoint << 6) | (continuationByte & 0x3F);
			}

			if (codepoint < minCodepoint) {
				// Overlong encoding - a validly-formed UTF-8 encoder would never produce this
				return false;
			}
			if (codepoint >= 0xD800 && codepoint <= 0xDFFF) {
				// Surrogate halves must never appear directly encoded in UTF-8
				return false;
			}
			if (codepoint > 0x10FFFF) {
				return false;
			}

			i += 1 + continuationBytesNeeded;
		}

		return true;
	}
}
