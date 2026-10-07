package de.soderer.utilities;

import java.net.IDN;
import java.util.ArrayList;
import java.util.List;

import de.soderer.network.NetworkUtilities;

/**
 * Punycode encoder and decoder (RFC 3492) and helpers for internationalized domain names and email addresses.
 */
public class PunycodeCodec {
	private final static int TMIN = 1;
	private final static int TMAX = 26;
	private final static int BASE = 36;
	private final static int INITIAL_N = 128;
	private final static int INITIAL_BIAS = 72;
	private final static int DAMP = 700;
	private final static int SKEW = 38;
	private final static char DELIMITER = '-';

	private PunycodeCodec() {
		// Utility class
	}

	/**
	 * Encode a Unicode string with Punycode (RFC 3492), without the IDNA prefix "xn--".
	 * Unicode characters outside the Basic Multilingual Plane are encoded as one code point.
	 * Basic (ASCII) characters are followed by the delimiter '-', but a pure ASCII string is returned unchanged
	 * (deviating from RFC 3492, which would append a delimiter), so such a result must not be passed to {@link #decode(String)}.
	 *
	 * @param stringToEncode
	 *            Unicode string, may be null
	 * @return Punycode string, or null for null input
	 * @throws Exception
	 *             on arithmetic overflow for very long input
	 */
	public static String encode(final String stringToEncode) throws Exception {
		if (stringToEncode == null) {
			return null;
		}

		// Work on code points, so surrogate pairs are encoded as one character as required by RFC 3492
		final int[] codePoints = stringToEncode.codePoints().toArray();

		int n = INITIAL_N;
		int delta = 0;
		int bias = INITIAL_BIAS;

		final StringBuilder output = new StringBuilder();
		for (final int codePoint : codePoints) {
			if (isBasicCodePoint(codePoint)) {
				output.append((char) codePoint);
			}
		}

		final int baseOutputLength = output.length();
		// Pure ASCII strings are returned unchanged without delimiter (deviating from RFC 3492, like IDN labels)
		if (baseOutputLength > 0 && baseOutputLength < codePoints.length) {
			output.append(DELIMITER);
		}

		int h = baseOutputLength;
		while (h < codePoints.length) {
			int m = Integer.MAX_VALUE;

			for (final int codePoint : codePoints) {
				if (codePoint >= n && codePoint < m) {
					m = codePoint;
				}
			}

			if (m - n > (Integer.MAX_VALUE - delta) / (h + 1)) {
				throw new Exception("Punycode overflow");
			}
			delta = delta + (m - n) * (h + 1);
			n = m;

			for (final int codePoint : codePoints) {
				if (codePoint < n) {
					// Overflow check before incrementing (before, the check "delta == 0" after incrementing never triggered)
					if (delta == Integer.MAX_VALUE) {
						throw new Exception("Punycode overflow");
					}
					delta++;
				}
				if (codePoint == n) {
					int q = delta;

					for (int k = BASE;; k += BASE) {
						int t;
						if (k <= bias) {
							t = TMIN;
						} else if (k >= bias + TMAX) {
							t = TMAX;
						} else {
							t = k - bias;
						}
						if (q < t) {
							break;
						}
						output.append((char) punycodeDigitToCodePoint(t + (q - t) % (BASE - t)));
						q = (q - t) / (BASE - t);
					}

					output.append((char) punycodeDigitToCodePoint(q));
					bias = adapt(delta, h + 1, h == baseOutputLength);
					delta = 0;
					h++;
				}
			}

			delta++;
			n++;
		}

		return output.toString();
	}

	/**
	 * Decode a Punycode string (RFC 3492), without the IDNA prefix "xn--".
	 * Punycode digits are accepted in upper and lower case.
	 *
	 * @param punycodeString
	 *            Punycode string, may be null
	 * @return decoded Unicode string, or null for null input
	 * @throws Exception
	 *             if the input is no valid Punycode or decoding overflows
	 */
	public static String decode(final String punycodeString) throws Exception {
		if (punycodeString == null) {
			return null;
		}

		int n = INITIAL_N;
		int i = 0;
		int bias = INITIAL_BIAS;
		// Code points instead of chars, so decoded characters outside the BMP are not truncated and positions are counted correctly
		final List<Integer> output = new ArrayList<>();

		int d = punycodeString.lastIndexOf(DELIMITER);
		if (d > 0) {
			for (int j = 0; j < d; j++) {
				final char c = punycodeString.charAt(j);
				if (!isBasicCodePoint(c)) {
					throw new Exception("Invalid input for decoding punycode");
				}
				output.add((int) c);
			}
			d++;
		} else {
			d = 0;
		}

		while (d < punycodeString.length()) {
			final int oldi = i;
			int w = 1;

			for (int k = BASE;; k += BASE) {
				if (d == punycodeString.length()) {
					throw new Exception("Invalid input for decoding punycode");
				}
				final int c = punycodeString.charAt(d++);
				final int digit = codePointToPunycodeDigit(c);
				if (digit > (Integer.MAX_VALUE - i) / w) {
					throw new Exception("Punycode overflow");
				}

				i = i + digit * w;

				int t;
				if (k <= bias) {
					t = TMIN;
				} else if (k >= bias + TMAX) {
					t = TMAX;
				} else {
					t = k - bias;
				}
				if (digit < t) {
					break;
				}
				if (w > Integer.MAX_VALUE / (BASE - t)) {
					throw new Exception("Punycode overflow");
				}
				w = w * (BASE - t);
			}

			bias = adapt(i - oldi, output.size() + 1, oldi == 0);

			if (i / (output.size() + 1) > Integer.MAX_VALUE - n) {
				throw new Exception("Punycode overflow");
			}

			n = n + i / (output.size() + 1);
			i = i % (output.size() + 1);
			if (!Character.isValidCodePoint(n) || isBasicCodePoint(n)) {
				throw new Exception("Invalid input for decoding punycode");
			}
			output.add(i, n);
			i++;
		}

		final StringBuilder result = new StringBuilder();
		for (final int codePoint : output) {
			result.appendCodePoint(codePoint);
		}
		return result.toString();
	}

	private final static int adapt(int delta, final int numpoints, final boolean first) {
		if (first) {
			delta = delta / DAMP;
		} else {
			delta = delta / 2;
		}

		delta = delta + (delta / numpoints);

		int k = 0;
		while (delta > ((BASE - TMIN) * TMAX) / 2) {
			delta = delta / (BASE - TMIN);
			k = k + BASE;
		}

		return k + ((BASE - TMIN + 1) * delta) / (delta + SKEW);
	}

	private final static boolean isBasicCodePoint(final int codePoint) {
		return codePoint >= 0 && codePoint < 0x80;
	}

	private final static int punycodeDigitToCodePoint(final int punycodeDigit) throws Exception {
		if (punycodeDigit < 0) {
			throw new Exception("Invalid input for encoding punycode");
		} else if (punycodeDigit < 26) {
			return punycodeDigit + 'a';
		} else if (punycodeDigit < 36) {
			return punycodeDigit - 26 + '0';
		} else {
			throw new Exception("Invalid input for encoding punycode");
		}
	}

	private final static int codePointToPunycodeDigit(final int codepoint) throws Exception {
		// Range checks on both ends (before, any character below '0' was accepted as a digit and uppercase letters were decoded wrongly)
		if (codepoint >= '0' && codepoint <= '9') {
			return codepoint - '0' + 26;
		} else if (codepoint >= 'a' && codepoint <= 'z') {
			return codepoint - 'a';
		} else if (codepoint >= 'A' && codepoint <= 'Z') {
			return codepoint - 'A';
		} else {
			throw new Exception("Invalid input for decoding punycode");
		}
	}

	/**
	 * Convert an internationalized domain name to its ASCII form ("xn--" labels, see {@link IDN#toASCII(String)}).
	 *
	 * @param domainName
	 *            domain name, may be null
	 * @return ASCII compatible domain name, or null for null input
	 * @throws IllegalArgumentException
	 *             if the domain name does not conform to RFC 3490
	 */
	public static String encodeDomainName(final String domainName) {
		if (domainName == null) {
			return null;
		} else {
			return IDN.toASCII(domainName);
		}
	}

	/**
	 * Convert an ASCII domain name with "xn--" labels to its Unicode form (see {@link IDN#toUnicode(String)}).
	 *
	 * @param domainNameToDecode
	 *            ASCII compatible domain name, may be null
	 * @return Unicode domain name, or null for null input
	 */
	public static String decodeDomainName(final String domainNameToDecode) {
		if (domainNameToDecode == null) {
			return null;
		} else {
			return IDN.toUnicode(domainNameToDecode);
		}
	}

	/**
	 * Convert the domain part of an email address to its ASCII form. The local part is kept unchanged.
	 *
	 * @param email
	 *            email address, may be null; surrounding whitespace is removed
	 * @return email address with ASCII compatible domain, or null for null input
	 * @throws Exception
	 *             if the email address is invalid
	 */
	public static String encodeEmailAdress(final String email) throws Exception {
		if (email == null) {
			return null;
		} else if (!NetworkUtilities.isValidEmail(email)) {
			throw new Exception("Not a valid email address: " + email);
		} else {
			// Use the trimmed address for index and substrings (before, the index of the trimmed text was applied to the untrimmed text)
			final String trimmedEmail = email.trim();
			final int mailDelimiterIndex = trimmedEmail.lastIndexOf('@');
			if (mailDelimiterIndex < 0) {
				throw new Exception("Invalid email address");
			}

			final String localPart = trimmedEmail.substring(0, mailDelimiterIndex);
			final String domainName = trimmedEmail.substring(mailDelimiterIndex + 1);
			final StringBuilder encodedEmail = new StringBuilder();
			encodedEmail.append(localPart);
			encodedEmail.append('@');
			encodedEmail.append(encodeDomainName(domainName));

			return encodedEmail.toString();
		}
	}

	/**
	 * Convert the ASCII ("xn--") domain part of an email address to its Unicode form. The local part is kept unchanged.
	 *
	 * @param emailToDecode
	 *            email address, may be null; surrounding whitespace is removed
	 * @return email address with Unicode domain, or null for null input
	 * @throws Exception
	 *             if the text contains no '@'
	 */
	public static String decodeEmailAdress(final String emailToDecode) throws Exception {
		if (emailToDecode == null) {
			return null;
		} else {
			final String trimmedEmail = emailToDecode.trim();
			final int mailDelimiterIndex = trimmedEmail.lastIndexOf('@');
			if (mailDelimiterIndex < 0) {
				throw new Exception("Invalid email address");
			}

			final String localPart = trimmedEmail.substring(0, mailDelimiterIndex);
			final String domainName = trimmedEmail.substring(mailDelimiterIndex + 1);
			final StringBuilder decodedEmail = new StringBuilder();
			decodedEmail.append(localPart);
			decodedEmail.append('@');
			decodedEmail.append(decodeDomainName(domainName));

			return decodedEmail.toString();
		}
	}
}
