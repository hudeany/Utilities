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

	/**
	 * Minimum size of the character buffer: a surrogate pair needs two chars, otherwise a high
	 * surrogate waiting for its partner would block the buffer forever.
	 */
	private static final int MIN_BUFFER_SIZE = 2;

	private static final int MIN_OUTPUT_BUFFER_SIZE = 128;

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
	 *            the reader to read characters from, not null
	 * @param encoder
	 *            the encoder for the characters, which defines the handling of unmappable characters, not null.
	 *            It is reset and must not be used elsewhere while this stream is used.
	 */
	public ReaderInputStream(final Reader reader, final CharsetEncoder encoder) {
		this(reader, encoder, DEFAULT_BUFFER_SIZE);
	}

	/**
	 * Creates a new stream using the given encoder.
	 *
	 * @param reader
	 *            the reader to read characters from, not null
	 * @param encoder
	 *            the encoder for the characters, which defines the handling of unmappable characters, not null.
	 *            It is reset and must not be used elsewhere while this stream is used. With
	 *            {@link CodingErrorAction#REPORT}, reading throws a
	 *            {@link java.nio.charset.CharacterCodingException} for malformed or unmappable characters.
	 * @param bufferSize
	 *            the size of the character buffer, at least 2
	 * @throws IllegalArgumentException
	 *             if a parameter is null or the buffer size is less than 2
	 */
	public ReaderInputStream(final Reader reader, final CharsetEncoder encoder, final int bufferSize) {
		if (reader == null) {
			throw new IllegalArgumentException("Reader must not be null");
		} else if (encoder == null) {
			throw new IllegalArgumentException("Encoder must not be null");
		} else if (bufferSize < MIN_BUFFER_SIZE) {
			throw new IllegalArgumentException("Buffer size must be at least " + MIN_BUFFER_SIZE + ": " + bufferSize);
		}

		this.reader = reader;
		this.encoder = encoder;
		// An encoder used before may still contain state of its previous use
		encoder.reset();
		encoderIn = CharBuffer.allocate(bufferSize);
		encoderIn.flip();
		// Enough space for the bytes of some characters, even for charsets with many bytes per character
		encoderOut = ByteBuffer.allocate(Math.max(MIN_OUTPUT_BUFFER_SIZE, (int) Math.ceil(encoder.maxBytesPerChar()) * 4));
		encoderOut.flip();
	}

	/**
	 * Creates a new stream for a charset. Malformed and unmappable characters are replaced by the
	 * charset's replacement bytes.
	 *
	 * @param reader
	 *            the reader to read characters from, not null
	 * @param charset
	 *            the charset to encode the characters with, not null
	 * @param bufferSize
	 *            the size of the character buffer, at least 2
	 * @throws IllegalArgumentException
	 *             if a parameter is null or the buffer size is less than 2
	 */
	public ReaderInputStream(final Reader reader, final Charset charset, final int bufferSize) {
		this(reader, createReplacingEncoder(charset), bufferSize);
	}

	/**
	 * Creates a new stream for a charset using the default buffer size. Malformed and unmappable
	 * characters are replaced by the charset's replacement bytes.
	 *
	 * @param reader
	 *            the reader to read characters from, not null
	 * @param charset
	 *            the charset to encode the characters with, not null
	 * @throws IllegalArgumentException
	 *             if a parameter is null
	 */
	public ReaderInputStream(final Reader reader, final Charset charset) {
		this(reader, charset, DEFAULT_BUFFER_SIZE);
	}

	private static CharsetEncoder createReplacingEncoder(final Charset charset) {
		if (charset == null) {
			throw new IllegalArgumentException("Charset must not be null");
		}
		return charset.newEncoder().onMalformedInput(CodingErrorAction.REPLACE).onUnmappableCharacter(CodingErrorAction.REPLACE);
	}

	private void fillBuffer() throws IOException {
		if (!endOfInput && (lastCoderResult == null || lastCoderResult.isUnderflow())) {
			encoderIn.compact();
			final int position = encoderIn.position();
			final int nextDataSize = reader.read(encoderIn.array(), position, encoderIn.remaining());
			if (nextDataSize == EOF) {
				endOfInput = true;
			} else {
				encoderIn.position(position + nextDataSize);
			}
			encoderIn.flip();
		}
		encoderOut.compact();
		if (!encoderFlushStarted) {
			lastCoderResult = encoder.encode(encoderIn, encoderOut, endOfInput);
			if (lastCoderResult.isError()) {
				// Only possible with an encoder using CodingErrorAction.REPORT. Without this, the same error would be
				// returned again and again, and reading would never end.
				encoderOut.flip();
				lastCoderResult.throwException();
			}
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

	/**
	 * Whether all characters were read and encoded and all bytes were delivered
	 */
	private boolean isEndOfData() {
		return encoderFlushed && !encoderOut.hasRemaining();
	}

	@Override
	public int read(final byte[] array, int offset, int length) throws IOException {
		if (array == null) {
			throw new NullPointerException("Mandatory array is null");
		}
		// Written without "offset + length", which could overflow for large values
		if (length < 0 || offset < 0 || length > array.length - offset) {
			throw new IndexOutOfBoundsException("Array Size=" + array.length + ", offset=" + offset + ", length=" + length);
		}
		if (length == 0) {
			return 0;
		}
		int read = 0;
		while (length > 0) {
			if (encoderOut.hasRemaining()) {
				final int nextDataSize = Math.min(encoderOut.remaining(), length);
				encoderOut.get(array, offset, nextDataSize);
				offset += nextDataSize;
				length -= nextDataSize;
				read += nextDataSize;
			} else if (isEndOfData()) {
				break;
			} else {
				fillBuffer();
			}
		}
		return read == 0 ? EOF : read;
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
			} else if (isEndOfData()) {
				return EOF;
			} else {
				fillBuffer();
			}
		}
	}

	/**
	 * Number of bytes, which can be read without reading from the underlying reader.
	 *
	 * @return the number of already encoded bytes
	 */
	@Override
	public int available() {
		return encoderOut.remaining();
	}

	@Override
	public void close() throws IOException {
		reader.close();
	}
}
