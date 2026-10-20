package de.soderer.utilities;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import de.soderer.utilities.TextUtilities.LineBreak;

/**
 * Reader for text lines, which detects the line break types (Unix LF, Windows CRLF, Mac CR) used in
 * the data.
 */
public class LineReader implements Closeable {
	/** Value returned by read operations at the end of data. */
	public static final int EOF = -1;

	/** Null after closing. Volatile, because {@link #close()} may be called from another thread. */
	private volatile PushbackReader pushbackReader = null;

	private BOM bom = BOM.NONE;
	private LineBreak globalLineBreakType = LineBreak.Unknown;
	private LineBreak latestLineBreakType = LineBreak.Unknown;

	/**
	 * Returns the Byte Order Mark found at the start of the data.
	 *
	 * @return the BOM, {@link BOM#NONE} if there was none or the reader was created for a Reader
	 */
	public BOM getBom() {
		return bom;
	}

	/**
	 * Returns the line break type of all lines read so far.
	 *
	 * @return the line break type, {@link LineBreak#Mixed} if different types were found,
	 *         {@link LineBreak#Unknown} if no line break was read yet
	 */
	public LineBreak getGlobalLineBreakType() {
		return globalLineBreakType;
	}

	/**
	 * Returns the line break type which ended the line read most recently.
	 *
	 * @return the line break type, {@link LineBreak#Unknown} if the last line had no line break
	 */
	public LineBreak getLatestLineBreakType() {
		return latestLineBreakType;
	}

	/**
	 * Creates a new reader for a stream. A leading BOM is skipped. The stream is closed by
	 * {@link #close()}, and also if this constructor fails.
	 *
	 * @param inputStream
	 *            the stream to read from, not null
	 * @param charset
	 *            the encoding of the data, not null. For data with UTF-16 or UTF-32 BOM the generic
	 *            UTF-16 or UTF-32 charset may be used, the byte order is then taken from the BOM.
	 * @throws IOException
	 *             if reading the BOM fails or the BOM does not fit the charset
	 */
	public LineReader(final InputStream inputStream, final Charset charset) throws IOException {
		if (inputStream == null) {
			throw new IllegalArgumentException("InputStream must not be null");
		} else if (charset == null) {
			// Close the stream, as it would be closed on any other failure of this constructor
			closeQuietly(inputStream);
			throw new IllegalArgumentException("Charset must not be null");
		}

		final BOMInputStream bomInputStream = new BOMInputStream(inputStream);
		try {
			bomInputStream.skipBOM();
			bom = bomInputStream.getBOM();
			final Charset effectiveCharset = getCharsetForBom(bom, charset);
			pushbackReader = new PushbackReader(new BufferedReader(new InputStreamReader(bomInputStream, effectiveCharset)));
		} catch (final IOException | RuntimeException e) {
			// Otherwise the stream leaks, because the caller never gets a LineReader to close
			closeQuietly(bomInputStream);
			throw e;
		}
	}

	/**
	 * Creates a new reader for a character reader. The reader is closed by {@link #close()}.
	 *
	 * @param reader
	 *            the reader to read from, not null
	 */
	public LineReader(final Reader reader) {
		if (reader == null) {
			throw new IllegalArgumentException("Reader must not be null");
		}
		pushbackReader = new PushbackReader(new BufferedReader(reader));
	}

	/**
	 * Checks that the charset fits the BOM and resolves the generic UTF-16/UTF-32 charsets to the
	 * byte order given by the BOM.
	 */
	private static Charset getCharsetForBom(final BOM bom, final Charset charset) throws IOException {
		if (bom == BOM.UTF_8) {
			if (!StandardCharsets.UTF_8.equals(charset)) {
				throw new IOException("Invalid charset for inputstream with UTF-8 BOM: " + charset.displayName());
			}
			return charset;
		} else if (bom == BOM.UTF_16_LE) {
			if (StandardCharsets.UTF_16.equals(charset)) {
				// The BOM was skipped, so the generic UTF-16 decoder would fall back to big-endian
				return StandardCharsets.UTF_16LE;
			} else if (!StandardCharsets.UTF_16LE.equals(charset)) {
				throw new IOException("Invalid charset for inputstream with UTF_16_LE BOM: " + charset.displayName());
			}
			return charset;
		} else if (bom == BOM.UTF_16_BE) {
			if (StandardCharsets.UTF_16.equals(charset)) {
				return StandardCharsets.UTF_16BE;
			} else if (!StandardCharsets.UTF_16BE.equals(charset)) {
				throw new IOException("Invalid charset for inputstream with UTF_16_BE BOM: " + charset.displayName());
			}
			return charset;
		} else if (bom == BOM.UTF_32_LE) {
			if ("UTF-32".equals(charset.name())) {
				// The BOM was skipped, so the generic UTF-32 decoder would fall back to big-endian
				return Charset.forName("UTF-32LE");
			} else if (!"UTF-32LE".equals(charset.name())) {
				throw new IOException("Invalid charset for inputstream with UTF_32_LE BOM: " + charset.displayName());
			}
			return charset;
		} else if (bom == BOM.UTF_32_BE) {
			if ("UTF-32".equals(charset.name())) {
				return Charset.forName("UTF-32BE");
			} else if (!"UTF-32BE".equals(charset.name())) {
				throw new IOException("Invalid charset for inputstream with UTF_32_BE BOM: " + charset.displayName());
			}
			return charset;
		} else {
			return charset;
		}
	}

	/**
	 * Reads the next line. The terminating line break (LF, CRLF or CR) is consumed but not included.
	 *
	 * @return the next line, or null at the end of data
	 * @throws IOException
	 *             if reading fails or the reader is closed
	 */
	public String readLine() throws IOException {
		final PushbackReader reader = getOpenReader();
		latestLineBreakType = LineBreak.Unknown;

		StringBuilder returnValue = null;
		int nextCharInt;
		while ((nextCharInt = reader.read()) != EOF) {
			if (returnValue == null) {
				returnValue = new StringBuilder();
			}

			if (nextCharInt == '\n') {
				registerLineBreak(LineBreak.Unix);
				break;
			} else if (nextCharInt == '\r') {
				final int followingCharInt = reader.read();
				if (followingCharInt == '\n') {
					registerLineBreak(LineBreak.Windows);
				} else {
					registerLineBreak(LineBreak.Mac);
					if (followingCharInt != EOF) {
						// First char of the next line
						reader.unread(followingCharInt);
					}
				}
				break;
			} else {
				returnValue.append((char) nextCharInt);
			}
		}

		return returnValue == null ? null : returnValue.toString();
	}

	/**
	 * Sets the latest line break type and updates the global one
	 */
	private void registerLineBreak(final LineBreak lineBreakType) {
		latestLineBreakType = lineBreakType;
		if (globalLineBreakType == LineBreak.Unknown) {
			globalLineBreakType = lineBreakType;
		} else if (globalLineBreakType != lineBreakType) {
			globalLineBreakType = LineBreak.Mixed;
		}
	}

	/**
	 * Skips characters. Line breaks count as one character per char (CRLF counts as two).
	 *
	 * @param charsToSkip
	 *            the number of characters to skip, nothing is skipped if not positive
	 * @throws IOException
	 *             if reading fails, the reader is closed or the end of data is reached before all
	 *             characters were skipped
	 */
	public void skipChars(final long charsToSkip) throws IOException {
		final PushbackReader reader = getOpenReader();
		long remaining = charsToSkip;
		while (remaining > 0) {
			final long skipped = reader.skip(remaining);
			if (skipped > 0) {
				remaining -= skipped;
			} else if (reader.read() == EOF) {
				// skip() may return 0 without having reached the end of data, so check by reading a char
				throw new IOException("No more chars left to skip");
			} else {
				remaining--;
			}
		}
	}

	/**
	 * Closes this reader and the underlying data source. Closing an already closed reader has no
	 * effect. Errors on closing are printed to stderr.
	 */
	@Override
	public synchronized void close() {
		final PushbackReader reader = pushbackReader;
		if (reader != null) {
			pushbackReader = null;
			try {
				reader.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Reads a single character, without any line break handling.
	 *
	 * @return the character read, or {@link #EOF} at the end of data
	 * @throws IOException
	 *             if reading fails or the reader is closed
	 */
	public int readChar() throws IOException {
		return getOpenReader().read();
	}

	private PushbackReader getOpenReader() throws IOException {
		final PushbackReader reader = pushbackReader;
		if (reader == null) {
			throw new IOException("LineReader is closed");
		}
		return reader;
	}

	private static void closeQuietly(final Closeable closeable) {
		try {
			closeable.close();
		} catch (@SuppressWarnings("unused") final IOException e) {
			// The original error is more important
		}
	}
}
