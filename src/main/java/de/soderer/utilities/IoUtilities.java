package de.soderer.utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.Charset;

/**
 * Utilities for copying and reading streams. None of the methods closes the given streams.
 */
public class IoUtilities {
	/** Value returned by read operations at the end of data. */
	public static final int EOF = -1;

	/**
	 * Utility class
	 */
	private IoUtilities() {
		// Utility class
	}

	/**
	 * Reads all remaining data of a stream and decodes it as text.
	 *
	 * @param inputStream
	 *            the stream to read, must not be null
	 * @param encoding
	 *            the encoding of the data
	 * @return the decoded text
	 * @throws IOException
	 *             if reading fails
	 */
	public static String toString(final InputStream inputStream, final Charset encoding) throws IOException {
		return new String(toByteArray(inputStream), encoding);
	}

	/**
	 * Reads all remaining data of a stream.
	 *
	 * @param inputStream
	 *            the stream to read, may be null
	 * @return the data read, or null if the stream is null
	 * @throws IOException
	 *             if reading fails
	 */
	public static byte[] toByteArray(final InputStream inputStream) throws IOException {
		if (inputStream == null) {
			return null;
		} else {
			try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
				copy(inputStream, byteArrayOutputStream);
				return byteArrayOutputStream.toByteArray();
			}
		}
	}

	/**
	 * Copies all remaining data of a stream to another stream and flushes the output stream.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @param outputStream
	 *            the stream to write to
	 * @return the number of bytes copied
	 * @throws IOException
	 *             if reading or writing fails
	 */
	public static long copy(final InputStream inputStream, final OutputStream outputStream) throws IOException {
		final byte[] buffer = new byte[4096];
		int lengthRead;
		long bytesCopied = 0;
		while ((lengthRead = inputStream.read(buffer)) != EOF) {
			outputStream.write(buffer, 0, lengthRead);
			bytesCopied += lengthRead;
		}
		outputStream.flush();
		return bytesCopied;
	}

	/**
	 * Copies all remaining characters of a reader to a stream, encoding them with the given encoding,
	 * and flushes the output stream.
	 *
	 * @param inputReader
	 *            the reader to read from
	 * @param outputStream
	 *            the stream to write to
	 * @param encoding
	 *            the encoding used for the output data
	 * @return the number of bytes written
	 * @throws IOException
	 *             if reading or writing fails
	 */
	public static long copy(final Reader inputReader, final OutputStream outputStream, final Charset encoding) throws IOException {
		final char[] buffer = new char[4096];
		int lengthRead = -1;
		long bytesCopied = 0;
		// A surrogate pair split at the buffer boundary must be encoded together, so a trailing high surrogate is kept for the next chunk
		Character pendingHighSurrogate = null;
		while ((lengthRead = inputReader.read(buffer)) > -1) {
			final StringBuilder data = new StringBuilder(lengthRead + 1);
			if (pendingHighSurrogate != null) {
				data.append(pendingHighSurrogate.charValue());
				pendingHighSurrogate = null;
			}
			data.append(buffer, 0, lengthRead);
			if (data.length() > 0 && Character.isHighSurrogate(data.charAt(data.length() - 1))) {
				pendingHighSurrogate = data.charAt(data.length() - 1);
				data.setLength(data.length() - 1);
			}
			final byte[] dataBytes = data.toString().getBytes(encoding);
			outputStream.write(dataBytes);
			bytesCopied += dataBytes.length;
		}
		if (pendingHighSurrogate != null) {
			// Unpaired high surrogate at the end of data
			final byte[] dataBytes = String.valueOf(pendingHighSurrogate.charValue()).getBytes(encoding);
			outputStream.write(dataBytes);
			bytesCopied += dataBytes.length;
		}
		outputStream.flush();
		return bytesCopied;
	}

	/**
	 * Reads all remaining data of a stream and counts its bytes. The data itself is discarded.
	 *
	 * @param inputStream
	 *            the stream to read
	 * @return the number of bytes read
	 * @throws IOException
	 *             if reading fails
	 */
	public static long getStreamSize(final InputStream inputStream) throws IOException {
		final byte[] buffer = new byte[4096];
		int lengthRead;
		long bytesCopied = 0;
		while ((lengthRead = inputStream.read(buffer)) != EOF) {
			bytesCopied += lengthRead;
		}
		return bytesCopied;
	}
}
