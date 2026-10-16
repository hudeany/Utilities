package de.soderer.utilities;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
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
		// One continuous encoder for the whole data: encoding chunk by chunk with String.getBytes would write a byte order
		// mark for every chunk with encodings like UTF-16 and would split surrogate pairs at chunk boundaries
		// Closing the writer is safe: CountingOutputStream only flushes on close and keeps the given output stream open
		try (CountingOutputStream countingOutputStream = new CountingOutputStream(outputStream);
				Writer writer = new OutputStreamWriter(countingOutputStream, encoding)) {
			final char[] buffer = new char[4096];
			int lengthRead;
			while ((lengthRead = inputReader.read(buffer)) != EOF) {
				writer.write(buffer, 0, lengthRead);
			}
			writer.flush();
			return countingOutputStream.getByteCount();
		}
	}

	/**
	 * Counts the bytes written to an output stream. Closing it only flushes and does not close the underlying stream,
	 * which belongs to the caller.
	 */
	private static class CountingOutputStream extends FilterOutputStream {
		private long byteCount = 0;

		CountingOutputStream(final OutputStream outputStream) {
			super(outputStream);
		}

		@Override
		public void write(final int byteValue) throws IOException {
			out.write(byteValue);
			byteCount++;
		}

		@Override
		public void write(final byte[] data, final int offset, final int length) throws IOException {
			out.write(data, offset, length);
			byteCount += length;
		}

		@Override
		public void close() throws IOException {
			flush();
		}

		long getByteCount() {
			return byteCount;
		}
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
