package de.soderer.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;

/**
 * Input stream detecting a Byte Order Mark (BOM) at the start of the data.
 * <p>
 * The BOM is detected when this stream is created. It is not skipped automatically, call
 * {@link #skipBOM()} to skip it.
 * </p>
 */
public class BOMInputStream extends PushbackInputStream {
	private BOM bom = null;
	private boolean skipped = false;

	/**
	 * Creates a new stream and detects the BOM by reading up to the first 4 bytes, which are pushed
	 * back afterwards.
	 *
	 * @param inputStream
	 *            the stream to read from
	 * @throws IOException
	 *             if reading the first bytes fails
	 */
	public BOMInputStream(final InputStream inputStream) throws IOException {
		super(inputStream, 4);

		final byte firstBytes[] = new byte[4];
		// A single read may return fewer bytes than available, so read until 4 bytes or end of data
		final int read = readNBytes(firstBytes, 0, firstBytes.length);

		switch (read) {
			case 4:
				if ((firstBytes[0] == (byte) 0xFF) && (firstBytes[1] == (byte) 0xFE) && (firstBytes[2] == (byte) 0x00) && (firstBytes[3] == (byte) 0x00)) {
					bom = BOM.UTF_32_LE;
					break;
				} else if ((firstBytes[0] == (byte) 0x00) && (firstBytes[1] == (byte) 0x00) && (firstBytes[2] == (byte) 0xFE) && (firstBytes[3] == (byte) 0xFF)) {
					bom = BOM.UTF_32_BE;
					break;
				}
				//$FALL-THROUGH$
			case 3:
				if ((firstBytes[0] == (byte) 0xEF) && (firstBytes[1] == (byte) 0xBB) && (firstBytes[2] == (byte) 0xBF)) {
					bom = BOM.UTF_8;
					break;
				}
				//$FALL-THROUGH$
			case 2:
				if ((firstBytes[0] == (byte) 0xFF) && (firstBytes[1] == (byte) 0xFE)) {
					bom = BOM.UTF_16_LE;
					break;
				} else if ((firstBytes[0] == (byte) 0xFE) && (firstBytes[1] == (byte) 0xFF)) {
					bom = BOM.UTF_16_BE;
					break;
				}
				//$FALL-THROUGH$
			default:
				bom = BOM.NONE;
				break;
		}

		if (read > 0) {
			unread(firstBytes, 0, read);
		}
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
	 * Skips the detected BOM, so that the next read operation delivers the first byte after it. Does
	 * nothing if the BOM was already skipped or no BOM was detected.
	 *
	 * @return this stream
	 * @throws IOException
	 *             if skipping fails
	 */
	public final synchronized BOMInputStream skipBOM() throws IOException {
		if (!skipped) {
			skip(bom.bytes.length);
			skipped = true;
		}
		return this;
	}
}