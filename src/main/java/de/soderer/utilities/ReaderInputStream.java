package de.soderer.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;

/**
 * Input stream delivering the characters of a {@link Reader} encoded with a charset, the reverse of
 * an {@link java.io.InputStreamReader}.
 */
public class ReaderInputStream extends InputStream {
	/** Value returned by read operations at the end of data. */
	public static final int EOF = -1;

	private static final int DEFAULT_BUFFER_SIZE = 1024;

	private final Reader reader;
	private final CharsetEncoder encoder;
	private final CharBuffer encoderIn;
	private final ByteBuffer encoderOut;
	private CoderResult lastCoderResult;
	private boolean endOfInput;
	/** True after all input was encoded and flushing the encoder was started. */
	private boolean encoderFlushStarted;
	/** True after the encoder was flushed completely at the end of input. */
	private boolean encoderFlushed;

	/**
	 * Creates a new stream using the given encoder and the default buffer size.
	 *
	 * @param reader
	 *            the reader to read characters from
	 * @param encoder
	 *            the encoder for the characters, which defines the handling of unmappable characters
	 */
	public ReaderInputStream(final Reader reader, final CharsetEncoder encoder) {
		this(reader, encoder, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Creates a new stream using the given encoder.
	 *
	 * @param reader
	 *            the reader to read characters from
	 * @param encoder
	 *            the encoder for the characters, which defines the handling of unmappable characters
	 * @param bufferSize
	 *            the size of the character buffer
	 */
	public ReaderInputStream(final Reader reader, final CharsetEncoder encoder, final int bufferSize) {
		this.reader = reader;
		this.encoder = encoder;
		encoderIn = CharBuffer.allocate(bufferSize);
		encoderIn.flip();
		encoderOut = ByteBuffer.allocate(128);
		encoderOut.flip();
	}

	/**
	 * Creates a new stream for a charset. Malformed and unmappable characters are replaced by the
	 * charset's replacement bytes.
	 *
	 * @param reader
	 *            the reader to read characters from
	 * @param charset
	 *            the charset to encode the characters with
	 * @param bufferSize
	 *            the size of the character buffer
	 */
	public ReaderInputStream(final Reader reader, final Charset charset, final int bufferSize) {
		this(reader, charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE), bufferSize);
	}

	/**
	 * Creates a new stream for a charset using the default buffer size. Malformed and unmappable
	 * characters are replaced by the charset's replacement bytes.
	 *
	 * @param reader
	 *            the reader to read characters from
	 * @param charset
	 *            the charset to encode the characters with
	 */
	public ReaderInputStream(final Reader reader, final Charset charset) {
		this(reader, charset, DEFAULT_BUFFER_SIZE);
	}

	private void fillBuffer() throws IOException {
		if (!endOfInput && (lastCoderResult == null || lastCoderResult.isUnderflow())) {
			encoderIn.compact();
			final int position = encoderIn.position();
			final int nextDataSize = reader.read(encoderIn.array(), position, encoderIn.remaining());
			if (nextDataSize == EOF) {
				endOfInput = true;
			} else {
				encoderIn.position(position+nextDataSize);
			}
			encoderIn.flip();
		}
		encoderOut.compact();
		if (!encoderFlushStarted) {
			lastCoderResult = encoder.encode(encoderIn, encoderOut, endOfInput);
			if (endOfInput && lastCoderResult.isUnderflow()) {
				// All input was encoded, the encoder may not be used for encoding anymore after flushing
				encoderFlushStarted = true;
			}
		}
		if (encoderFlushStarted && !encoderFlushed) {
			// Stateful encoders (e.g. ISO-2022-JP) may need to write final bytes at the end of input
			lastCoderResult = encoder.flush(encoderOut);
			if (lastCoderResult.isUnderflow()) {
				encoderFlushed = true;
			}
		}
		encoderOut.flip();
	}

	@Override
	public int read(final byte[] array, int offset, int length) throws IOException {
		if (array == null) {
			throw new IOException("Mandatory array is null");
		}
		if (length < 0 || offset < 0 || (offset + length) > array.length) {
			throw new IndexOutOfBoundsException("Array Size=" + array.length + ", offset=" + offset + ", length=" + length);
		}
		int read = 0;
		if (length == 0) {
			return 0;
		}
		while (length > 0) {
			if (encoderOut.hasRemaining()) {
				final int nextDataSize = Math.min(encoderOut.remaining(), length);
				encoderOut.get(array, offset, nextDataSize);
				offset += nextDataSize;
				length -= nextDataSize;
				read += nextDataSize;
			} else {
				fillBuffer();
				if (endOfInput && !encoderOut.hasRemaining()) {
					break;
				}
			}
		}
		return read == 0 && endOfInput ? EOF : read;
	}

	@Override
	public int read(final byte[] byteArray) throws IOException {
		return read(byteArray, 0, byteArray.length);
	}

	@Override
	public int read() throws IOException {
		while (true) {
			if (encoderOut.hasRemaining()) {
				return encoderOut.get() & 0xFF;
			}
			fillBuffer();
			if (endOfInput && !encoderOut.hasRemaining()) {
				return EOF;
			}
		}
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}
}
