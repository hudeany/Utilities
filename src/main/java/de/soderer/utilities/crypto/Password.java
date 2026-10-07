package de.soderer.utilities.crypto;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wrapper for password charArrays.<br>
 * This wrapper is closeable and autocloseable, so that IDE warnings will pop up if this Object is not closed.<br>
 * The close action removes the password data from memory.<br>
 * <br>
 * Watchout:<br>
 * If the constructor parameter passwordChars is an object that was not created only for this Password, it will be cleared after closing the Password Object.<br>
 * If you want to keep the original passwordChars for later use outside of the Password object use "new Password(passwordChars.clone())"
 */
public class Password implements Closeable {
	private char[] passwordChars = null;
	private byte[] passwordBytesUtfEncoded = null;
	private byte[] passwordBytesIsoEncoded = null;
	private final List<byte[]> sensitiveDataToCleanup = new ArrayList<>();

	/**
	 * Creates a password wrapper. The given array is not copied and will be cleared by {@link #close()}.
	 *
	 * @param passwordChars password characters, may be null
	 */
	public Password(final char[] passwordChars) {
		this.passwordChars = passwordChars;
	}

	/**
	 * Returns the password characters.
	 *
	 * @return the wrapped password array (not a copy), or null after {@link #close()}
	 */
	public char[] getPasswordChars() {
		return passwordChars;
	}

	/**
	 * Returns the password encoded in UTF-8. The bytes are created on first call, cached and cleared by {@link #close()}.
	 *
	 * @return UTF-8 encoded password bytes (the cached array, not a copy)
	 */
	public byte[] getPasswordBytesUtfEncoded() {
		if (passwordBytesUtfEncoded == null) {
			passwordBytesUtfEncoded = encodeCharArrayToByteArray(passwordChars, StandardCharsets.UTF_8);
		}
		return passwordBytesUtfEncoded;
	}

	/**
	 * Returns the password encoded in ISO-8859-1. Characters that cannot be represented in ISO-8859-1 are replaced by '?'.
	 * The bytes are created on first call, cached and cleared by {@link #close()}.
	 *
	 * @return ISO-8859-1 encoded password bytes (the cached array, not a copy)
	 */
	public byte[] getPasswordBytesIsoEncoded() {
		if (passwordBytesIsoEncoded == null) {
			passwordBytesIsoEncoded = encodeCharArrayToByteArray(passwordChars, StandardCharsets.ISO_8859_1);
		}
		return passwordBytesIsoEncoded;
	}

	/**
	 * Registers additional sensitive data, which will be overwritten with zeros by {@link #close()}.
	 *
	 * @param sensitiveData data to clear on close, may be null
	 */
	public void addSensitiveDataToCleanup(final byte[] sensitiveData) {
		sensitiveDataToCleanup.add(sensitiveData);
	}

	/**
	 * Overwrites the password characters, the cached encoded password bytes and all registered sensitive data with zeros.
	 */
	@Override
	public void close() {
		if (passwordChars != null) {
			Arrays.fill(passwordChars, (char) 0);
			passwordChars = null;
		}
		if (passwordBytesUtfEncoded != null) {
			Arrays.fill(passwordBytesUtfEncoded, (byte) 0);
			passwordBytesUtfEncoded = null;
		}
		if (passwordBytesIsoEncoded != null) {
			Arrays.fill(passwordBytesIsoEncoded, (byte) 0);
			passwordBytesIsoEncoded = null;
		}
		for (final byte[] sensitiveData : sensitiveDataToCleanup) {
			if (sensitiveData != null) {
				Arrays.fill(sensitiveData, (byte) 0);
			}
		}
	}

	private static byte[] encodeCharArrayToByteArray(final char[] chars, final Charset encoding) {
		ByteBuffer byteBuffer = null;
		try {
			byteBuffer = encoding.encode(CharBuffer.wrap(chars));
			return Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
		} finally {
			if (byteBuffer != null && byteBuffer.array() != null) {
				Arrays.fill(byteBuffer.array(), (byte) 0);
				byteBuffer = null;
			}
		}

	}
}
