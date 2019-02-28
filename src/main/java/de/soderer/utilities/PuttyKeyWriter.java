package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Base64;

import javax.xml.ws.WebServiceException;

import com.sun.xml.internal.ws.Closeable;

public class PuttyKeyWriter implements Closeable {
	private final OutputStream outputStream;

	public PuttyKeyWriter(final OutputStream outputStream) throws IOException {
		this.outputStream = outputStream;
	}

	/**
	 * Converts this key into unprotected DER format for OpenSSH keys
	 */
	public void writeUnprotectedDerFormat(final PuttyKey puttyKey) throws Exception {
		if ("ssh-rsa".equalsIgnoreCase(puttyKey.getAlgorithm())) {
			outputStream.write(createRsaBinaryKey(puttyKey));
		} else if ("ssh-dss".equalsIgnoreCase(puttyKey.getAlgorithm())) {
			outputStream.write(createDssBinaryKey(puttyKey));
		} else {
			throw new IllegalArgumentException("Unsupported cipher: " + puttyKey.getAlgorithm());
		}
	}

	/**
	 * Converts this key into unprotected PEM format for OpenSSH keys
	 */
	public void writeUnprotectedPemFormat(final PuttyKey puttyKey) throws Exception {
		if ("ssh-rsa".equalsIgnoreCase(puttyKey.getAlgorithm())) {
			outputStream.write("-----BEGIN RSA PRIVATE KEY-----\n".getBytes("UTF-8"));
			outputStream.write(toWrappedBase64(createRsaBinaryKey(puttyKey)).getBytes("UTF-8"));
			outputStream.write("\n".getBytes("UTF-8"));
			outputStream.write("-----END RSA PRIVATE KEY-----\n".getBytes("UTF-8"));
		} else if ("ssh-dss".equalsIgnoreCase(puttyKey.getAlgorithm())) {
			outputStream.write("-----BEGIN DSA PRIVATE KEY-----\n".getBytes("UTF-8"));
			outputStream.write(toWrappedBase64(createDssBinaryKey(puttyKey)).getBytes("UTF-8"));
			outputStream.write("\n".getBytes("UTF-8"));
			outputStream.write("-----END DSA PRIVATE KEY-----\n".getBytes("UTF-8"));
		} else {
			throw new IllegalArgumentException("Unsupported cipher: " + puttyKey.getAlgorithm());
		}
	}

	private byte[] createRsaBinaryKey(final PuttyKey puttyKey) throws Exception, IOException {
		final BlockDataReader publicKeyReader = new BlockDataReader(puttyKey.getPublicKey());
		final String cipherName = publicKeyReader.readString();
		if (!"ssh-rsa".equalsIgnoreCase(cipherName)) {
			throw new Exception("Corrupt public key data: Cipher name in data differs from defined cipher name. Expected \"ssh-rsa\", but was \"" + cipherName + "\"");
		}
		final BigInteger e = publicKeyReader.readBigInt();
		final BigInteger n = publicKeyReader.readBigInt();

		final BlockDataReader privateKeyReader = new BlockDataReader(puttyKey.getPrivateKey());
		final BigInteger d = privateKeyReader.readBigInt();
		final BigInteger p = privateKeyReader.readBigInt();
		final BigInteger q = privateKeyReader.readBigInt();
		final BigInteger iqmp = privateKeyReader.readBigInt();

		final BigInteger dmp1 = d.mod(p.subtract(BigInteger.ONE));
		final BigInteger dmq1 = d.mod(q.subtract(BigInteger.ONE));

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeBigIntegerToStream(out, BigInteger.ZERO);
		writeBigIntegerToStream(out, n);
		writeBigIntegerToStream(out, e);
		writeBigIntegerToStream(out, d);
		writeBigIntegerToStream(out, p);
		writeBigIntegerToStream(out, q);
		writeBigIntegerToStream(out, dmp1);
		writeBigIntegerToStream(out, dmq1);
		writeBigIntegerToStream(out, iqmp);
		return createByteSequence(out.toByteArray());
	}

	private byte[] createDssBinaryKey(final PuttyKey puttyKey) throws Exception, IOException {
		final BlockDataReader publicKeyReader = new BlockDataReader(puttyKey.getPublicKey());
		final String cipherName = publicKeyReader.readString();
		if (!"ssh-dss".equalsIgnoreCase(cipherName)) {
			throw new Exception("Corrupt public key data: Cipher name in data differs from defined cipher name. Expected \"ssh-dss\", but was \"" + cipherName + "\"");
		}
		final BigInteger p = publicKeyReader.readBigInt();
		final BigInteger q = publicKeyReader.readBigInt();
		final BigInteger g = publicKeyReader.readBigInt();
		final BigInteger y = publicKeyReader.readBigInt();

		final BlockDataReader privateKeyReader = new BlockDataReader(puttyKey.getPrivateKey());
		final BigInteger x = privateKeyReader.readBigInt();

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeBigIntegerToStream(out, BigInteger.ZERO);
		writeBigIntegerToStream(out, p);
		writeBigIntegerToStream(out, q);
		writeBigIntegerToStream(out, g);
		writeBigIntegerToStream(out, y);
		writeBigIntegerToStream(out, x);
		return createByteSequence(out.toByteArray());
	}

	/**
	 * Converts byte array to base64 with 64 chars per line
	 */
	private static String toWrappedBase64(final byte[] byteArray) {
		return Base64.getMimeEncoder(64, "\n".getBytes(Charset.forName("UTF-8"))).encodeToString(byteArray);
	}

	public static byte[] createByteSequence(final byte[] data) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		out.write(0x30);
		final int len = data.length;
		if (len < 0x80) {
			out.write(len);
		} else {
			final int bytes = getByteEncodedLength(len);
			out.write(0x80 | bytes);
			for (int i = bytes - 1; i >= 0; i--) {
				out.write((len >> (8 * i)) & 0xFF);
			}
		}
		out.write(data);
		return out.toByteArray();
	}

	private static int getByteEncodedLength(int value) {
		int lengthInBytes = 0;
		while (value > 0) {
			lengthInBytes++;
			value >>= 8;
		}
		return lengthInBytes;
	}

	public static void writeBigIntegerToStream(final ByteArrayOutputStream out, final BigInteger bigIntegerValue) throws IOException {
		out.write(0x02);
		final byte[] bytes = bigIntegerValue.toByteArray();
		final int len = bytes.length;
		if (len < 0x80) {
			out.write(len);
		} else {
			final int bytes1 = getByteEncodedLength(len);
			out.write(0x80 | bytes1);
			for (int i1 = bytes1 - 1; i1 >= 0; i1--) {
				out.write((len >> (8 * i1)) & 0xFF);
			}
		}
		out.write(bytes);
	}

	@Override
	public void close() throws WebServiceException {
		try {
			outputStream.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	public class BlockDataReader {
		private final DataInput keyDataInput;

		public BlockDataReader(final byte[] key) {
			keyDataInput = new DataInputStream(new ByteArrayInputStream(key));
		}

		public void skipNextBlock() {
			try {
				final int bytesToSkip = keyDataInput.readInt();
				keyDataInput.skipBytes(bytesToSkip);
			} catch (final IOException e) {
				throw new AssertionError(e);
			}
		}

		public BigInteger readBigInt() throws Exception {
			try {
				final int nextBlockSize = keyDataInput.readInt();
				if (nextBlockSize <= 0 || nextBlockSize > 512) {
					throw new Exception("Key blocksize error");
				}
				final byte[] nextBlock = new byte[nextBlockSize];
				keyDataInput.readFully(nextBlock);
				return new BigInteger(nextBlock);
			} catch (final IOException e) {
				throw new Exception("Key block read error", e);
			}
		}

		public String readString() throws Exception {
			try {
				final int nextBlockSize = keyDataInput.readInt();
				if (nextBlockSize <= 0 || nextBlockSize > 512) {
					throw new Exception("Key blocksize error");
				}
				final byte[] nextBlock = new byte[nextBlockSize];
				keyDataInput.readFully(nextBlock);
				return new String(nextBlock, "UTF-8");
			} catch (final IOException e) {
				throw new Exception("Key block read error", e);
			}
		}
	}
}
