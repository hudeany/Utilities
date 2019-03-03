package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Base64;

import javax.xml.ws.WebServiceException;

public class PuttyKeyWriter implements Closeable {
	private final OutputStream outputStream;

	public PuttyKeyWriter(final OutputStream outputStream) throws IOException {
		this.outputStream = outputStream;
	}

	/**
	 * Converts this key into unprotected DER format for OpenSSH keys<br />
	 * <br />
	 * <b>Use with caution, because this key format is not protected by any password</>
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
	 * Converts this key into unprotected PEM format for OpenSSH keys<br />
	 * <br />
	 * <b>Use with caution, because this key format is not protected by any password</>
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
		String cipherName;
		BigInteger e;
		BigInteger n;
		try {
			final BlockDataReader publicKeyReader = new BlockDataReader(puttyKey.getPublicKey());
			cipherName = publicKeyReader.readString();
			e = publicKeyReader.readBigInt();
			n = publicKeyReader.readBigInt();
		} catch (final Exception e1) {
			throw new Exception("Invalid public key. Maybe the public key was tampered.");
		}
		if (!"ssh-rsa".equalsIgnoreCase(cipherName)) {
			throw new Exception("Corrupt public key data: Cipher name in data differs from defined cipher name. Expected \"ssh-rsa\", but was \"" + cipherName + "\"");
		}

		BigInteger d;
		BigInteger p;
		BigInteger q;
		BigInteger iqmp;
		BigInteger dmp1;
		BigInteger dmq1;
		try {
			final BlockDataReader privateKeyReader = new BlockDataReader(puttyKey.getPrivateKey());
			d = privateKeyReader.readBigInt();
			p = privateKeyReader.readBigInt();
			q = privateKeyReader.readBigInt();
			iqmp = privateKeyReader.readBigInt();

			dmp1 = d.mod(p.subtract(BigInteger.ONE));
			dmq1 = d.mod(q.subtract(BigInteger.ONE));
		} catch (final Exception e1) {
			throw new Exception("Invalid private key. Maybe the key encryption password was wrong or the private key was tampered.");
		}

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
		String cipherName;
		BigInteger p;
		BigInteger q;
		BigInteger g;
		BigInteger y;
		try {
			final BlockDataReader publicKeyReader = new BlockDataReader(puttyKey.getPublicKey());
			cipherName = publicKeyReader.readString();
			p = publicKeyReader.readBigInt();
			q = publicKeyReader.readBigInt();
			g = publicKeyReader.readBigInt();
			y = publicKeyReader.readBigInt();
		} catch (final Exception e) {
			throw new Exception("Invalid public key. Maybe the public key was tampered.");
		}
		if (!"ssh-dss".equalsIgnoreCase(cipherName)) {
			throw new Exception("Corrupt public key data: Cipher name in data differs from defined cipher name. Expected \"ssh-dss\", but was \"" + cipherName + "\"");
		}

		BigInteger x;
		try {
			final BlockDataReader privateKeyReader = new BlockDataReader(puttyKey.getPrivateKey());
			x = privateKeyReader.readBigInt();
		} catch (final Exception e) {
			throw new Exception("Invalid private key. Maybe the key encryption password was wrong or the private key was tampered.");
		}

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

	private static void writeBigIntegerToStream(final ByteArrayOutputStream out, final BigInteger bigIntegerValue) throws IOException {
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

		public void skipNextBlock() throws Exception {
			try {
				final int bytesToSkip = keyDataInput.readInt();
				keyDataInput.skipBytes(bytesToSkip);
			} catch (final IOException e) {
				throw new Exception("Skip data error", e);
			}
		}

		public BigInteger readBigInt() throws Exception {
			try {
				final int nextBlockSize = keyDataInput.readInt();
				if (nextBlockSize <= 0 || nextBlockSize > 513) {
					throw new Exception("Key blocksize error. Maybe the key encrytion password was wrong");
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
				if (nextBlockSize <= 0 || nextBlockSize > 513) {
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
