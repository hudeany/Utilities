package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;

public class AsnCodec {
	public static byte[] getAsnEncodedInteger(final long value) throws Exception {
		if (value < 0) {
			throw new Exception("Minimum ASN.1 encoded Integer underrun");
		} else if (value < 0x80) {
			return new byte[] { (byte) value };
		} else {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] data = BigInteger.valueOf(value).toByteArray();
			if (data[0] == 0) {
				// Removed the obsolete sign bit, which caused an additional byte
				final byte[] tmp = new byte[data.length - 1];
				System.arraycopy(data, 1, tmp, 0, tmp.length);
				data = tmp;
			}
			if (data.length >= 0x80) {
				throw new Exception("Maximum ASN.1 encoded Integer exceeded");
			}
			out.write(0x80 | data.length);
			out.write(data);
			return out.toByteArray();
		}
	}

	public static BigInteger parseAsnEncodedInteger(final byte[] data, final int offset) throws Exception {
		try {
			final DataInput blockDataInput = new DataInputStream(new ByteArrayInputStream(data));
			blockDataInput.skipBytes(offset - 1);

			final int nextBlockSize = blockDataInput.readInt();
			if (nextBlockSize <= 0 || nextBlockSize > 513) {
				throw new Exception("Key blocksize error");
			}
			final byte[] nextBlock = new byte[nextBlockSize];
			blockDataInput.readFully(nextBlock);
			return new BigInteger(nextBlock);
		} catch (final IOException e) {
			throw new Exception("Key block read error", e);
		}
	}
}
