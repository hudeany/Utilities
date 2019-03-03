package de.soderer.utilities;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
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
		final RSAPrivateCrtKey privateKey = ((RSAPrivateCrtKey) puttyKey.getKeyPair().getPrivate());

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeBigIntegerToStream(out, BigInteger.ZERO);
		writeBigIntegerToStream(out, privateKey.getModulus());
		writeBigIntegerToStream(out, privateKey.getPublicExponent());
		writeBigIntegerToStream(out, privateKey.getPrivateExponent());
		writeBigIntegerToStream(out, privateKey.getPrimeP());
		writeBigIntegerToStream(out, privateKey.getPrimeQ());
		writeBigIntegerToStream(out, privateKey.getPrimeExponentP());
		writeBigIntegerToStream(out, privateKey.getPrimeExponentQ());
		writeBigIntegerToStream(out, privateKey.getCrtCoefficient());
		return createByteSequence(out.toByteArray());
	}

	private byte[] createDssBinaryKey(final PuttyKey puttyKey) throws Exception, IOException {
		final DSAPublicKey publicKey = ((DSAPublicKey) puttyKey.getKeyPair().getPublic());
		final DSAPrivateKey privateKey = ((DSAPrivateKey) puttyKey.getKeyPair().getPrivate());

		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		writeBigIntegerToStream(out, BigInteger.ZERO);
		writeBigIntegerToStream(out, privateKey.getParams().getP());
		writeBigIntegerToStream(out, privateKey.getParams().getQ());
		writeBigIntegerToStream(out, privateKey.getParams().getG());
		writeBigIntegerToStream(out, publicKey.getY());
		writeBigIntegerToStream(out, privateKey.getX());
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
}
