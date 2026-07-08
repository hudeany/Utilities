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

public class LineReader implements Closeable {
	public static final int EOF = -1;

	private PushbackReader pushbackReader = null;

	private BOM bom = BOM.NONE;
	private LineBreak globalLineBreakType = LineBreak.Unknown;
	private LineBreak latestLineBreakType = LineBreak.Unknown;

	public BOM getBom() {
		return bom;
	}

	public LineBreak getGlobalLineBreakType() {
		return globalLineBreakType;
	}

	public LineBreak getLatestLineBreakType() {
		return latestLineBreakType;
	}

	public LineReader(final InputStream inputStream, final Charset charset) throws IOException {
		final BOMInputStream bomInputStream = new BOMInputStream(inputStream);
		bomInputStream.skipBOM();
		bom = bomInputStream.getBOM();
		if (bom == BOM.UTF_8 && charset != StandardCharsets.UTF_8) {
			throw new IOException("Invalid charset for inputstream with UTF-8 BOM: " + charset.displayName());
		} else if (bom == BOM.UTF_16_LE && (charset != StandardCharsets.UTF_16 || charset != StandardCharsets.UTF_16LE)) {
			throw new IOException("Invalid charset for inputstream with UTF_16_LE BOM: " + charset.displayName());
		} else if (bom == BOM.UTF_16_BE && (charset != StandardCharsets.UTF_16 || charset != StandardCharsets.UTF_16BE)) {
			throw new IOException("Invalid charset for inputstream with UTF_16_BE BOM: " + charset.displayName());
		}
		pushbackReader = new PushbackReader(new BufferedReader(new InputStreamReader(bomInputStream, charset)));
	}

	public LineReader(final Reader reader) {
		pushbackReader = new PushbackReader(new BufferedReader(reader));
	}

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

	public void skipChars(final long charsToSkip) throws IOException {
		if (charsToSkip <= 0) {
			return;
		}
		int skipCount = 0;
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

	@Override
	public void close() {
		synchronized (pushbackReader) {
			try {
				pushbackReader.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
			pushbackReader = null;
		}
	}

	public int readChar() throws IOException {
		return pushbackReader.read();
	}
}
