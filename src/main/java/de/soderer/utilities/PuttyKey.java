package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.DSAPrivateKeySpec;
import java.security.spec.DSAPublicKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Map;

/**
 * Container for PuTTY key data
 */
public class PuttyKey {
	private final Map<String, String> headers;
	private final KeyPair keyPair;

	public PuttyKey(final Map<String, String> headers, final byte[] privateKeyBytes, final byte[] publicKeyBytes) throws Exception {
		this.headers = headers;
		final BlockDataReader publicKeyReader = new BlockDataReader(publicKeyBytes);
		final String cipherName = publicKeyReader.readString();
		if (cipherName == null || !cipherName.equalsIgnoreCase(getAlgorithm())) {
			throw new Exception("Corrupt public key data: Cipher name in data differs from defined cipher name. Expected \"" +  getAlgorithm()+ "\", but was \"" + cipherName + "\"");
		}
		final BlockDataReader privateKeyReader = new BlockDataReader(privateKeyBytes);

		if ("ssh-rsa".equalsIgnoreCase(cipherName)) {
			final BigInteger publicExponent = publicKeyReader.readBigInt();
			final BigInteger modulus = publicKeyReader.readBigInt();

			final BigInteger privateExponent = privateKeyReader.readBigInt();
			final BigInteger p = privateKeyReader.readBigInt(); // secret prime factor (= PrimeP)
			final BigInteger q = privateKeyReader.readBigInt(); // secret prime factor (= PrimeQ)
			final BigInteger iqmp = privateKeyReader.readBigInt(); // q^-1 mod p (= CrtCoefficient)

			final BigInteger dmp1 = privateExponent.mod(p.subtract(BigInteger.ONE)); // d mod (p-1) (= PrimeExponentP)
			final BigInteger dmq1 = privateExponent.mod(q.subtract(BigInteger.ONE)); // d mod (q-1) (= PrimeExponentQ)

			final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			final PublicKey publicKey = keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
			final PrivateKey privateKey = keyFactory.generatePrivate(new RSAPrivateCrtKeySpec(modulus, publicExponent, privateExponent, p, q, dmp1, dmq1, iqmp));
			keyPair = new KeyPair(publicKey, privateKey);
		} else if ("ssh-dss".equalsIgnoreCase(cipherName)) {
			final BigInteger p = publicKeyReader.readBigInt();
			final BigInteger q = publicKeyReader.readBigInt();
			final BigInteger g = publicKeyReader.readBigInt();

			// Public key exponent
			final BigInteger y = publicKeyReader.readBigInt();

			// Private key exponent
			final BigInteger x = privateKeyReader.readBigInt();

			final KeyFactory keyFactory = KeyFactory.getInstance("DSA");
			final PublicKey publicKey = keyFactory.generatePublic(new DSAPublicKeySpec(y, p, q, g));
			final PrivateKey privateKey = keyFactory.generatePrivate(new DSAPrivateKeySpec(x, p, q, g));
			keyPair = new KeyPair(publicKey, privateKey);
		} else {
			throw new IllegalArgumentException("Unsupported cipher: " + cipherName);
		}
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	/**
	 * Key type. Either "ssh-rsa" for RSA key, or "ssh-dss" for DSA key.
	 */
	public String getAlgorithm() {
		return headers.get("PuTTY-User-Key-File-2").trim();
	}

	public int getRsaKeyStrength() throws Exception {
		if (keyPair.getPublic().getAlgorithm().toLowerCase().contains("rsa")) {
			return ((RSAPublicKey) keyPair.getPublic()).getModulus().bitLength();
		} else {
			return -1;
		}
	}

	public String getKeyComment() {
		return headers.get("Comment").trim();
	}

	/**
	 * Contains
	 *  ssh-rsa: RSAPublicKey and RSAPrivateCrtKey
	 *  ssh-dss: DSAPublicKey and DSAPrivateKey
	 */
	public KeyPair getKeyPair() throws Exception {
		return keyPair;
	}

	public byte[] getPublicKeyBytes() throws Exception {
		if ("ssh-rsa".equalsIgnoreCase(getAlgorithm())) {
			final RSAPublicKey publicKey = ((RSAPublicKey) keyPair.getPublic());
			final BlockDataWriter publicKeyWriter = new BlockDataWriter();
			publicKeyWriter.writeString("ssh-rsa");
			publicKeyWriter.writeBigInt(publicKey.getPublicExponent());
			publicKeyWriter.writeBigInt(publicKey.getModulus());
			return publicKeyWriter.toByteArray();
		} else if ("ssh-dss".equalsIgnoreCase(getAlgorithm())) {
			final DSAPublicKey publicKey = ((DSAPublicKey) keyPair.getPublic());
			final BlockDataWriter publicKeyWriter = new BlockDataWriter();
			publicKeyWriter.writeString("ssh-dss");
			publicKeyWriter.writeBigInt(publicKey.getParams().getP());
			publicKeyWriter.writeBigInt(publicKey.getParams().getQ());
			publicKeyWriter.writeBigInt(publicKey.getParams().getG());
			publicKeyWriter.writeBigInt(publicKey.getY());
			return publicKeyWriter.toByteArray();
		} else {
			throw new IllegalArgumentException("Unsupported cipher: " + getAlgorithm());
		}
	}

	public byte[] getPrivateKeyBytes() throws Exception {
		if ("ssh-rsa".equalsIgnoreCase(getAlgorithm())) {
			final RSAPrivateCrtKey privateKey = ((RSAPrivateCrtKey) keyPair.getPrivate());
			final BlockDataWriter privateKeyWriter = new BlockDataWriter();
			privateKeyWriter.writeBigInt(privateKey.getPrivateExponent());
			privateKeyWriter.writeBigInt(privateKey.getPrimeP());
			privateKeyWriter.writeBigInt(privateKey.getPrimeQ());
			privateKeyWriter.writeBigInt(privateKey.getCrtCoefficient());
			return privateKeyWriter.toByteArray();
		} else if ("ssh-dss".equalsIgnoreCase(getAlgorithm())) {
			final DSAPrivateKey privateKey = ((DSAPrivateKey) keyPair.getPrivate());
			final BlockDataWriter privateKeyWriter = new BlockDataWriter();
			privateKeyWriter.writeBigInt(privateKey.getX());
			return privateKeyWriter.toByteArray();
		} else {
			throw new IllegalArgumentException("Unsupported cipher: " + getAlgorithm());
		}
	}

	public class BlockDataReader {
		private final DataInput keyDataInput;

		public BlockDataReader(final byte[] key) {
			keyDataInput = new DataInputStream(new ByteArrayInputStream(key));
		}

		public BigInteger readBigInt() throws Exception {
			return new BigInteger(readData());
		}

		public String readString() throws Exception {
			return new String(readData(), "UTF-8");
		}

		private byte[] readData() throws IOException, Exception {
			try {
				final int nextBlockSize = keyDataInput.readInt();
				if (nextBlockSize <= 0 || nextBlockSize > 513) {
					throw new Exception("Key blocksize error. Maybe the key encrytion password was wrong");
				}
				final byte[] nextBlock = new byte[nextBlockSize];
				keyDataInput.readFully(nextBlock);
				return nextBlock;
			} catch (final IOException e) {
				throw new Exception("Key block read error", e);
			}
		}
	}

	public class BlockDataWriter {
		private final ByteArrayOutputStream outputStream;
		private final DataOutput keyDataOutput;

		public BlockDataWriter() {
			outputStream = new ByteArrayOutputStream();
			keyDataOutput = new DataOutputStream(outputStream);
		}

		public void writeBigInt(final BigInteger bigIntegerData) throws Exception {
			writeData(bigIntegerData.toByteArray());
		}

		public void writeString(final String stringData) throws Exception {
			writeData(stringData.getBytes("ISO-8859-1"));
		}

		private void writeData(final byte[] data) throws IOException, Exception {
			try {
				if (data.length <= 0 || data.length > 513) {
					throw new Exception("Key blocksize error");
				}

				keyDataOutput.writeInt(data.length);
				keyDataOutput.write(data);
			} catch (final IOException e) {
				throw new Exception("Key block write error", e);
			}
		}

		public byte[] toByteArray() {
			return outputStream.toByteArray();
		}
	}
}
