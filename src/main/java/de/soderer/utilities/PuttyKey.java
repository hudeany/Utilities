package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
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
	private final byte[] privateKey;
	private final byte[] publicKey;

	public PuttyKey(final Map<String, String> headers, final byte[] privateKey, final byte[] publicKey) {
		this.headers = headers;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public byte[] getPrivateKey() {
		return privateKey;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	/**
	 * Key type. Either "ssh-rsa" for RSA key, or "ssh-dss" for DSA key.
	 */
	public String getAlgorithm() {
		return headers.get("PuTTY-User-Key-File-2").trim();
	}

	public int getRsaKeyStrength() throws Exception {
		final KeyPair keyPair = getKeyPair();
		if (keyPair.getPublic().getAlgorithm().toLowerCase().contains("rsa")) {
			return ((RSAPublicKey) keyPair.getPublic()).getModulus().bitLength();
		} else {
			return -1;
		}
	}

	public String getKeyComment() {
		return headers.get("Comment").trim();
	}

	public KeyPair getKeyPair() throws Exception {
		final BlockDataReader publicKeyReader = new BlockDataReader(publicKey);
		final String cipherName = publicKeyReader.readString();
		if (cipherName == null || !cipherName.equalsIgnoreCase(getAlgorithm())) {
			throw new Exception("Corrupt public key data: Cipher name in data differs from defined cipher name. Expected \"" +  getAlgorithm()+ "\", but was \"" + cipherName + "\"");
		}
		final BlockDataReader privateKeyReader = new BlockDataReader(privateKey);

		if ("ssh-rsa".equalsIgnoreCase(cipherName)) {
			final BigInteger publicExponent = publicKeyReader.readBigInt();
			final BigInteger modulus = publicKeyReader.readBigInt();

			final BigInteger privateExponent = privateKeyReader.readBigInt();
			final BigInteger p = privateKeyReader.readBigInt(); // secret prime factor
			final BigInteger q = privateKeyReader.readBigInt(); // secret prime factor
			final BigInteger iqmp = privateKeyReader.readBigInt(); // q^-1 mod p
			final BigInteger dmp1 = privateExponent.mod(p.subtract(BigInteger.ONE)); // d mod (p-1)
			final BigInteger dmq1 = privateExponent.mod(q.subtract(BigInteger.ONE)); // d mod (q-1)

			final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			final PublicKey publicKey = keyFactory.generatePublic(new RSAPublicKeySpec(modulus, publicExponent));
			final PrivateKey privateKey = keyFactory.generatePrivate(new RSAPrivateCrtKeySpec(modulus, publicExponent, privateExponent, p, q, dmp1, dmq1, iqmp));
			return new KeyPair(publicKey, privateKey);
		} else if ("ssh-dss".equalsIgnoreCase(cipherName)) {
			final BigInteger p = publicKeyReader.readBigInt();
			final BigInteger q = publicKeyReader.readBigInt();
			final BigInteger g = publicKeyReader.readBigInt();
			final BigInteger y = publicKeyReader.readBigInt();
			// Private key exponent
			final BigInteger x = privateKeyReader.readBigInt();

			final KeyFactory keyFactory = KeyFactory.getInstance("DSA");
			final PublicKey publicKey = keyFactory.generatePublic(new DSAPublicKeySpec(y, p, q, g));
			final PrivateKey privateKey = keyFactory.generatePrivate(new DSAPrivateKeySpec(x, p, q, g));
			return new KeyPair(publicKey, privateKey);
		} else {
			throw new IllegalArgumentException("Unsupported cipher: " + cipherName);
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
