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

	private PushbackReader pushbackReader = null;

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
	 * Creates a new reader for a stream. A leading BOM is skipped.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @param charset
	 *            the encoding of the data. For data with UTF-16 or UTF-32 BOM the generic UTF-16 or
	 *            UTF-32 charset may be used, the byte order is then taken from the BOM.
	 * @throws IOException
	 *             if reading the BOM fails or the BOM does not fit the charset
	 */
	public LineReader(final InputStream inputStream, Charset charset) throws IOException {
		final BOMInputStream bomInputStream = new BOMInputStream(inputStream);
		bomInputStream.skipBOM();
		bom = bomInputStream.getBOM();
		if (bom == BOM.UTF_8 && !StandardCharsets.UTF_8.equals(charset)) {
			throw new IOException("Invalid charset for inputstream with UTF-8 BOM: " + charset.displayName());
		} else if (bom == BOM.UTF_16_LE) {
			if (StandardCharsets.UTF_16.equals(charset)) {
				// The BOM was skipped, so the generic UTF-16 decoder would fall back to big-endian
				charset = StandardCharsets.UTF_16LE;
			} else if (!StandardCharsets.UTF_16LE.equals(charset)) {
				throw new IOException("Invalid charset for inputstream with UTF_16_LE BOM: " + charset.displayName());
			}
		} else if (bom == BOM.UTF_16_BE) {
			if (StandardCharsets.UTF_16.equals(charset)) {
				charset = StandardCharsets.UTF_16BE;
			} else if (!StandardCharsets.UTF_16BE.equals(charset)) {
				throw new IOException("Invalid charset for inputstream with UTF_16_BE BOM: " + charset.displayName());
			}
		} else if (bom == BOM.UTF_32_LE) {
			if ("UTF-32".equals(charset.name())) {
				// The BOM was skipped, so the generic UTF-32 decoder would fall back to big-endian
				charset = Charset.forName("UTF-32LE");
			} else if (!"UTF-32LE".equals(charset.name())) {
				throw new IOException("Invalid charset for inputstream with UTF_32_LE BOM: " + charset.displayName());
			}
		} else if (bom == BOM.UTF_32_BE) {
			if ("UTF-32".equals(charset.name())) {
				charset = Charset.forName("UTF-32BE");
			} else if (!"UTF-32BE".equals(charset.name())) {
				throw new IOException("Invalid charset for inputstream with UTF_32_BE BOM: " + charset.displayName());
			}
		}
		pushbackReader = new PushbackReader(new BufferedReader(new InputStreamReader(bomInputStream, charset)));
	}

	/**
	 * Creates a new reader for a character reader.
	 *
	 * @param reader
	 *            the reader to read from
	 */
	public LineReader(final Reader reader) {
		pushbackReader = new PushbackReader(new BufferedReader(reader));
	}

	/**
	 * Reads the next line. The terminating line break (LF, CRLF or CR) is consumed but not included.
	 *
	 * @return the next line, or null at the end of data
	 * @throws IOException
	 *             if reading fails
	 */
	public String readLine() throws IOException {
		latestLineBreakType = LineBreak.Unknown;

		StringBuilder returnValue = null;
		int nextCharInt;
		while ((nextCharInt = pushbackReader.read()) != EOF) {
			final char nextChar = (char) nextCharInt;

			if (returnValue == null) {
				returnValue = new StringBuilder();
			}

			if (nextChar == '\n') {
				latestLineBreakType = LineBreak.Unix;

				if (globalLineBreakType == LineBreak.Unknown) {
					globalLineBreakType = LineBreak.Unix;
				} else if (globalLineBreakType == LineBreak.Mac || globalLineBreakType == LineBreak.Windows) {
					globalLineBreakType = LineBreak.Mixed;
				}
				break;
			} else if (nextChar == '\r') {
				nextCharInt = pushbackReader.read();
				if (nextCharInt == EOF) {
					latestLineBreakType = LineBreak.Mac;

					if (globalLineBreakType == LineBreak.Unknown) {
						globalLineBreakType = LineBreak.Mac;
					} else if (globalLineBreakType == LineBreak.Unix || globalLineBreakType == LineBreak.Windows) {
						globalLineBreakType = LineBreak.Mixed;
					}
					break;
				} else if (((char) nextCharInt) == '\n') {
					latestLineBreakType = LineBreak.Windows;

					if (globalLineBreakType == LineBreak.Unknown) {
						globalLineBreakType = LineBreak.Windows;
					} else if (globalLineBreakType == LineBreak.Unix || globalLineBreakType == LineBreak.Mac) {
						globalLineBreakType = LineBreak.Mixed;
					}
					break;
				} else {
					latestLineBreakType = LineBreak.Mac;

					if (globalLineBreakType == LineBreak.Unknown) {
						globalLineBreakType = LineBreak.Mac;
					} else if (globalLineBreakType == LineBreak.Unix || globalLineBreakType == LineBreak.Windows) {
						globalLineBreakType = LineBreak.Mixed;
					}

					pushbackReader.unread(nextCharInt);
					break;
				}
			} else {
				returnValue.append(nextChar);
			}
		}

		if (returnValue == null) {
			return null;
		} else {
			return returnValue.toString();
		}
	}

	/**
	 * Skips characters. Line breaks count as one character per char (CRLF counts as two).
	 *
	 * @param charsToSkip
	 *            the number of characters to skip, nothing is skipped if not positive
	 * @throws IOException
	 *             if reading fails or the end of data is reached before all characters were skipped
	 */
	public void skipChars(final long charsToSkip) throws IOException {
		if (charsToSkip <= 0) {
			return;
		}
		long skipCount = 0;
		while (true) {
			if (pushbackReader.read() == EOF) {
				throw new IOException("No more chars left to skip");
			} else {
				skipCount++;
				if (skipCount == charsToSkip) {
					break;
				}
			}
		}
	}

	/**
	 * Closes this reader and the underlying data source. Closing an already closed reader has no
	 * effect. Errors on closing are printed to stderr.
	 */
	@Override
	public synchronized void close() {
		if (pushbackReader != null) {
			try {
				pushbackReader.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			pushbackReader = null;
		}
	}

	/**
	 * Reads a single character, without any line break handling.
	 *
	 * @return the character read, or {@link #EOF} at the end of data
	 * @throws IOException
	 *             if reading fails
	 */
	public int readChar() throws IOException {
		return pushbackReader.read();
	}
}
