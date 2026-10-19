package de.soderer.utilities;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/**
 * Input stream detecting a Byte Order Mark (BOM) at the start of the data.
 * <p>
 * The BOM is detected when this stream is created, by reading up to {@link BOM#MAX_LENGTH} bytes.
 * The constructor therefore blocks until these bytes or the end of data are available. The BOM is
 * only skipped if requested at creation or by {@link #skipBOM()}. Mark and reset are supported if
 * the underlying stream supports them.
 * </p>
 * <p>
 * Like most streams, this stream is not thread safe.
 * </p>
 */
public class BOMInputStream extends FilterInputStream {
	/** The detected BOM, {@link BOM#NONE} if the data does not start with a BOM. */
	private final BOM bom;

	/** The first bytes of the data, read for BOM detection. */
	private final byte[] head = new byte[BOM.MAX_LENGTH];

	/** Number of valid bytes in the head buffer. */
	private final int headLength;

	/** Index of the next head byte to deliver. Equals the number of data bytes consumed while within the head. */
	private int headPosition = 0;

	/** Marker for "no mark set". */
	private static final int NO_MARK = -1;

	/** Head position saved by {@link #mark(int)}, or NO_MARK. */
	private int markedHeadPosition = NO_MARK;

	/**
	 * Creates a new stream and detects the BOM. The BOM is not skipped.
	 *
	 * @param inputStream
	 *            the stream to read from, must not be null
	 * @throws IOException
	 *             if reading the first bytes fails
	 * @throws IllegalArgumentException
	 *             if the input stream is null
	 */
	public BOMInputStream(final InputStream inputStream) throws IOException {
		this(inputStream, false);
	}

	/**
	 * Creates a new stream and detects the BOM.
	 *
	 * @param inputStream
	 *            the stream to read from, must not be null
	 * @param skipBOM
	 *            true to skip a detected BOM, so the first read operation delivers the first byte
	 *            after it
	 * @throws IOException
	 *             if reading the first bytes fails
	 * @throws IllegalArgumentException
	 *             if the input stream is null
	 */
	public BOMInputStream(final InputStream inputStream, final boolean skipBOM) throws IOException {
		super(requireInputStream(inputStream));

		// A single read may return fewer bytes than available, so read until the buffer is full or end of data
		int length = 0;
		while (length < head.length) {
			final int readBytes = inputStream.read(head, length, head.length - length);
			if (readBytes == -1) {
				break;
			}
			length += readBytes;
		}
		headLength = length;
		bom = BOM.detect(head, headLength);

		if (skipBOM) {
			headPosition = bom.getLength();
		}
	}

	/**
	 * Checks the input stream before it is passed to the super constructor.
	 *
	 * @param inputStream
	 *            the stream to check
	 * @return the stream
	 * @throws IllegalArgumentException
	 *             if the stream is null
	 */
	private static InputStream requireInputStream(final InputStream inputStream) {
		if (inputStream == null) {
			throw new IllegalArgumentException("Invalid empty inputStream");
		}
		return inputStream;
	}

	/**
	 * Returns the detected BOM.
	 *
	 * @return the detected BOM, {@link BOM#NONE} if the data does not start with a BOM
	 */
	public final BOM getBOM() {
		return bom;
	}

	/**
	 * Skips the remaining bytes of the detected BOM, so that the next read operation delivers the
	 * first byte after it. Does nothing if no BOM was detected or the BOM was already skipped or
	 * read. Data after the BOM is never skipped.
	 *
	 * @return this stream
	 */
	public final BOMInputStream skipBOM() {
		// The BOM always lies within the head buffer, so the head position tells how much of it was consumed
		if (headPosition < bom.getLength()) {
			headPosition = bom.getLength();
		}
		return this;
	}

	/**
	 * Returns the number of head bytes not yet delivered.
	 *
	 * @return the number of remaining head bytes
	 */
	private int remainingHeadBytes() {
		return headLength - headPosition;
	}

	@Override
	public int read() throws IOException {
		if (remainingHeadBytes() > 0) {
			return head[headPosition++] & 0xFF;
		} else {
			return in.read();
		}
	}

	@Override
	public int read(final byte[] buffer, final int offset, final int length) throws IOException {
		Objects.checkFromIndexSize(offset, length, buffer.length);
		if (length == 0) {
			return 0;
		}

		final int remainingHeadBytes = remainingHeadBytes();
		if (remainingHeadBytes > 0) {
			// Deliver only head bytes, a short read is allowed and avoids blocking on the underlying stream
			final int count = Math.min(length, remainingHeadBytes);
			System.arraycopy(head, headPosition, buffer, offset, count);
			headPosition += count;
			return count;
		} else {
			return in.read(buffer, offset, length);
		}
	}

	@Override
	public long skip(final long count) throws IOException {
		if (count <= 0) {
			return 0;
		}

		final int skippedHeadBytes = (int) Math.min(count, remainingHeadBytes());
		headPosition += skippedHeadBytes;
		if (skippedHeadBytes < count) {
			return skippedHeadBytes + in.skip(count - skippedHeadBytes);
		} else {
			return skippedHeadBytes;
		}
	}

	@Override
	public int available() throws IOException {
		final long available = (long) remainingHeadBytes() + in.available();
		return (int) Math.min(Integer.MAX_VALUE, available);
	}

	/**
	 * Checks whether mark and reset are supported, which depends on the underlying stream.
	 *
	 * @return true, if the underlying stream supports mark and reset
	 */
	@Override
	public boolean markSupported() {
		return in.markSupported();
	}

	/**
	 * Marks the current position. The head position is saved too, because the head bytes were
	 * already read from the underlying stream when this stream was created.
	 *
	 * @param readLimit
	 *            the maximum number of bytes that can be read before the mark becomes invalid
	 */
	@Override
	public synchronized void mark(final int readLimit) {
		if (in.markSupported()) {
			in.mark(readLimit);
			markedHeadPosition = headPosition;
		}
	}

	/**
	 * Resets this stream to the marked position, including the position within the head bytes.
	 *
	 * @throws IOException
	 *             if mark and reset are not supported, no mark was set, or the underlying stream
	 *             cannot reset to its mark
	 */
	@Override
	public synchronized void reset() throws IOException {
		if (!in.markSupported()) {
			throw new IOException("mark/reset not supported");
		} else if (markedHeadPosition == NO_MARK) {
			// Some streams like ByteArrayInputStream reset to their start without a mark, which would deliver the head bytes twice
			throw new IOException("Resetting to invalid mark");
		}
		in.reset();
		headPosition = markedHeadPosition;
	}
}
