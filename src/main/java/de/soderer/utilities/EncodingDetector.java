package de.soderer.utilities;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

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
 *   <li>Whether the sample is well-formed UTF-8 (strict validation: rejects overlong encodings
 *       and directly-encoded surrogate halves; a sequence truncated at the very end of the sample
 *       is ignored rather than treated as invalid, since the sample may simply be cut off
 *       mid-character).</li>
 *   <li>Otherwise, Windows-1252. Windows-1252 and ISO-8859-1 are byte-identical outside the
 *       0x80-0x9F range, and real C1 control codes in that range essentially never appear in
 *       genuine text, so there is no reliable way to tell them apart from content alone once
 *       UTF-8 has been ruled out - and in practice, non-UTF-8 8-bit text is overwhelmingly more
 *       likely to be Windows-1252 than genuine ISO-8859-1. This also makes a save-as-Windows-1252
 *       round trip detect correctly even when the saved text happens not to use any byte in the
 *       0x80-0x9F range at all (e.g. plain German text with just äöüß and no smart quotes, dashes,
 *       or euro sign).</li>
 * </ol>
 *
 * Pure 7-bit ASCII content is reported as UTF-8, since ASCII is a strict subset of UTF-8.
 */
public class EncodingDetector {

	public static final class DetectionResult {
		public final Charset charset;
		public final boolean hasBom;
		public final int bomLength;

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

	public static DetectionResult detectEncoding(final byte[] sampleBytes) {
		if (sampleBytes == null || sampleBytes.length == 0) {
			return new DetectionResult(StandardCharsets.UTF_8, false, 0);
		}

		final DetectionResult bomResult = detectBom(sampleBytes);
		if (bomResult != null) {
			return bomResult;
		}

		if (isWellFormedUtf8(sampleBytes)) {
			return new DetectionResult(StandardCharsets.UTF_8, false, 0);
		}

		return new DetectionResult(Charset.forName("windows-1252"), false, 0);
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

			if ((byte1 & 0xE0) == 0xC0) {
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
				// Truncated at the end of the sample - the sample was simply cut off mid-character,
				// not a sign of a different encoding, so stop validating here rather than fail.
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