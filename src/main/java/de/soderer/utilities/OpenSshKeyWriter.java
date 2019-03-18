package de.soderer.utilities;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class OpenSshKeyWriter implements Closeable {
	public static String SSH_CIPHER_NAME_RSA = "ssh-rsa";
	public static String SSH_CIPHER_NAME_DSA = "ssh-dss";
	public static String SSH_CIPHER_NAME_ECDSA_NISTP256 = "ecdsa-sha2-nistp256";
	public static String SSH_CIPHER_NAME_ECDSA_NISTP384 = "ecdsa-sha2-nistp384";
	public static String SSH_CIPHER_NAME_ECDSA_NISTP521 = "ecdsa-sha2-nistp521";

	private static final String AES128_PemEncryptionMethodName = "AES-128-CBC";
	private static final String TripleDES_PemEncryptionMethodName = "DES-EDE3-CBC";

	private final OutputStream outputStream;

	public OpenSshKeyWriter(final OutputStream outputStream) throws IOException {
		this.outputStream = outputStream;
	}

	/**
	 * Converts this public key into unprotected PEM format (PKCS#1) for OpenSSH keys<br />
	 * This format includes public key data only and is NOT accepted by PuTTY's key import<br />
	 */
	public void writePublicKey(final PublicKey publicKey) throws Exception {
		final byte[] publicKeyBytes = getPublicKeyBytes(publicKey);

		final String publicKeyBase64 = toWrappedBase64(publicKeyBytes, 64, "\r\n");

		final StringBuilder content = new StringBuilder();
		content.append("---- BEGIN SSH2 PUBLIC KEY ----").append("\r\n");
		content.append(publicKeyBase64).append("\r\n");
		content.append("---- END SSH2 PUBLIC KEY ----").append("\r\n");

		outputStream.write(content.toString().getBytes("UTF-8"));
	}

	/**
	 * Converts this private key into unprotected PEM format (PKCS#1) for OpenSSH keys<br />
	 * This format includes private key data only and is NOT accepted by PuTTY's key import<br />
	 * @throws Exception
	 */
	public void writePrivateKey(final PrivateKey privateKey) throws Exception {
		final byte[] privateKeyBytes = getPrivateKeyBytes(privateKey);

		final String privateKeyBase64 = toWrappedBase64(privateKeyBytes, 64, "\r\n");

		final StringBuilder content = new StringBuilder();
		content.append("---- BEGIN SSH2 PRIVATE KEY ----").append("\r\n");
		content.append(privateKeyBase64).append("\r\n");
		content.append("---- END SSH2 PRIVATE KEY ----").append("\r\n");

		outputStream.write(content.toString().getBytes("UTF-8"));
	}

	/**
	 * Converts this key into unprotected PEM format (PKCS#8) for OpenSSH keys<br />
	 * Using default encryption method "AES-128-CBC"<br />
	 * This format includes private and public key data and is accepted by PuTTY's key import<br />
	 */
	public void writePrivateAndPublicKey(final KeyPair keyPair, final char[] password) throws Exception {
		writePrivateAndPublicKey(keyPair, null, password);
	}

	/**
	 * Converts this keypair into protected PEM format (PKCS#8) for OpenSSH keys<br />
	 * This format includes private and public key data and is accepted by PuTTY's key import<br />
	 * <br />
	 * keyEncryptionCipherName:<br />
	 *   default is "AES-128-CBC"<br />
	 *   other value may be "DES-EDE3-CBC"<br />
	 */
	public void writePrivateAndPublicKey(final KeyPair keyPair, String keyEncryptionCipherName, final char[] password) throws Exception {
		if (keyEncryptionCipherName == null || "".equals(keyEncryptionCipherName.trim())) {
			keyEncryptionCipherName = AES128_PemEncryptionMethodName;
		}

		if (!AES128_PemEncryptionMethodName.equalsIgnoreCase(keyEncryptionCipherName) && !TripleDES_PemEncryptionMethodName.equalsIgnoreCase(keyEncryptionCipherName)) {
			throw new Exception("Unknown key encryption cipher: " + keyEncryptionCipherName);
		}

		if (password == null || password.length == 0) {
			throw new Exception("Mandatory password is missing");
		}

		String keyTypeName;
		byte[] keyData;
		final String algorithmName = getAlgorithm(keyPair);
		if (SSH_CIPHER_NAME_RSA.equalsIgnoreCase(algorithmName)) {
			keyTypeName = "RSA PRIVATE KEY";
			keyData = createRsaBinaryKey(keyPair);
		} else if (SSH_CIPHER_NAME_DSA.equalsIgnoreCase(algorithmName)) {
			keyTypeName = "DSA PRIVATE KEY";
			keyData = createDssBinaryKey(keyPair);
		} else if (SSH_CIPHER_NAME_ECDSA_NISTP256.equalsIgnoreCase(algorithmName)) {
			keyTypeName = "EC PRIVATE KEY";
			keyData = createEcdsaBinaryKey(keyPair, OID.SECP256R1_ARRAY);
		} else if (SSH_CIPHER_NAME_ECDSA_NISTP384.equalsIgnoreCase(algorithmName)) {
			keyTypeName = "EC PRIVATE KEY";
			keyData = createEcdsaBinaryKey(keyPair, OID.SECP384R1_ARRAY);
		} else if (SSH_CIPHER_NAME_ECDSA_NISTP521.equalsIgnoreCase(algorithmName)) {
			keyTypeName = "EC PRIVATE KEY";
			keyData = createEcdsaBinaryKey(keyPair, OID.SECP521R1_ARRAY);
		} else {
			throw new IllegalArgumentException("Unsupported cipher: " + algorithmName);
		}

		final Map<String, String> headers = new LinkedHashMap<>();

		final SecureRandom rnd = new SecureRandom();
		final Cipher cipher;
		final String ivString;
		if (TripleDES_PemEncryptionMethodName.equalsIgnoreCase(keyEncryptionCipherName)) {
			final byte[] iv = new byte[8];
			rnd.nextBytes(iv);
			ivString = toHexString(iv);
			cipher = Cipher.getInstance("DESede/CBC/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(stretchPasswordForOpenSsl(password, iv, 8, 24), "DESede"), new IvParameterSpec(iv));
			keyData = addSecurePadding(keyData, 16);
		} else if (AES128_PemEncryptionMethodName.equalsIgnoreCase(keyEncryptionCipherName)) {
			final byte[] iv = new byte[16];
			rnd.nextBytes(iv);
			ivString = toHexString(iv);
			cipher = Cipher.getInstance("AES/CBC/NoPadding");
			cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(stretchPasswordForOpenSsl(password, iv, 8, 16), "AES"), new IvParameterSpec(iv));
			keyData = addSecurePadding(keyData, 16);
		} else {
			throw new Exception("Unknown key encryption cipher: " + keyEncryptionCipherName);
		}
		headers.put("Proc-Type", "4,ENCRYPTED");
		headers.put("DEK-Info", keyEncryptionCipherName.toUpperCase() + "," + ivString);

		final byte[] encryptedKeyData = cipher.doFinal(keyData);

		outputStream.write(("-----BEGIN " + keyTypeName + "-----\n").getBytes("UTF-8"));
		outputStream.write(getPemHeaderLines(headers, 76).getBytes("UTF-8"));
		outputStream.write(toWrappedBase64(encryptedKeyData, 76, "\n").getBytes("UTF-8"));
		outputStream.write(("\n-----END " + keyTypeName + "-----\n").getBytes("UTF-8"));
	}

	/**
	 * Converts this key into unprotected DER format for OpenSSH keys<br />
	 * <br />
	 * <b>Use with caution, because this key format is not protected by any password</>
	 */
	public void writeUnprotectedDerFormat(final KeyPair keyPair) throws Exception {
		final String algorithmName = getAlgorithm(keyPair);
		if (SSH_CIPHER_NAME_RSA.equalsIgnoreCase(algorithmName)) {
			outputStream.write(createRsaBinaryKey(keyPair));
		} else if (SSH_CIPHER_NAME_DSA.equalsIgnoreCase(algorithmName)) {
			outputStream.write(createDssBinaryKey(keyPair));
		} else if (SSH_CIPHER_NAME_ECDSA_NISTP256.equalsIgnoreCase(algorithmName)) {
			outputStream.write(createEcdsaBinaryKey(keyPair, OID.SECP256R1_ARRAY));
		} else if (SSH_CIPHER_NAME_ECDSA_NISTP384.equalsIgnoreCase(algorithmName)) {
			outputStream.write(createEcdsaBinaryKey(keyPair, OID.SECP384R1_ARRAY));
		} else if (SSH_CIPHER_NAME_ECDSA_NISTP521.equalsIgnoreCase(algorithmName)) {
			outputStream.write(createEcdsaBinaryKey(keyPair, OID.SECP521R1_ARRAY));
		} else {
			throw new IllegalArgumentException("Unsupported cipher: " + algorithmName);
		}
	}

	/**
	 * Converts this key into unprotected PEM format (PKCS#1) for OpenSSH keys<br />
	 * <br />
	 * <b>Use with caution, because this key format is not protected by any password</>
	 */
	public void writeUnprotectedPemFormat(final KeyPair keyPair) throws Exception {
		final String algorithmName = getAlgorithm(keyPair);
		if (SSH_CIPHER_NAME_RSA.equalsIgnoreCase(algorithmName)) {
			outputStream.write("-----BEGIN RSA PRIVATE KEY-----\n".getBytes("UTF-8"));
			outputStream.write(toWrappedBase64(createRsaBinaryKey(keyPair), 64, "\n").getBytes("UTF-8"));
			outputStream.write("\n-----END RSA PRIVATE KEY-----\n".getBytes("UTF-8"));
		} else if (SSH_CIPHER_NAME_DSA.equalsIgnoreCase(algorithmName)) {
			outputStream.write("-----BEGIN DSA PRIVATE KEY-----\n".getBytes("UTF-8"));
			outputStream.write(toWrappedBase64(createDssBinaryKey(keyPair), 64, "\n").getBytes("UTF-8"));
			outputStream.write("\n-----END DSA PRIVATE KEY-----\n".getBytes("UTF-8"));
		} else if (SSH_CIPHER_NAME_ECDSA_NISTP256.equalsIgnoreCase(algorithmName)) {
			outputStream.write("-----BEGIN EC PRIVATE KEY-----\n".getBytes("UTF-8"));
			outputStream.write(toWrappedBase64(createEcdsaBinaryKey(keyPair, OID.SECP256R1_ARRAY), 64, "\n").getBytes("UTF-8"));
			outputStream.write("\n-----END EC PRIVATE KEY-----\n".getBytes("UTF-8"));
		} else if (SSH_CIPHER_NAME_ECDSA_NISTP384.equalsIgnoreCase(algorithmName)) {
			outputStream.write("-----BEGIN EC PRIVATE KEY-----\n".getBytes("UTF-8"));
			outputStream.write(toWrappedBase64(createEcdsaBinaryKey(keyPair, OID.SECP384R1_ARRAY), 64, "\n").getBytes("UTF-8"));
			outputStream.write("\n-----END EC PRIVATE KEY-----\n".getBytes("UTF-8"));
		} else if (SSH_CIPHER_NAME_ECDSA_NISTP521.equalsIgnoreCase(algorithmName)) {
			outputStream.write("-----BEGIN EC PRIVATE KEY-----\n".getBytes("UTF-8"));
			outputStream.write(toWrappedBase64(createEcdsaBinaryKey(keyPair, OID.SECP521R1_ARRAY), 64, "\n").getBytes("UTF-8"));
			outputStream.write("\n-----END EC PRIVATE KEY-----\n".getBytes("UTF-8"));
		} else {
			throw new IllegalArgumentException("Unsupported cipher: " + algorithmName);
		}
	}

	private static String getPemHeaderLines(final Map<String, String> headers, final int maxLineLimit) {
		final StringBuilder headerBuilder = new StringBuilder();
		if (!headers.isEmpty()) {
			for (final Entry<String, String> entry : headers.entrySet()) {
				headerBuilder.append(entry.getKey() + ": ");
				if ((entry.getKey().length() + entry.getValue().length() + 2) > maxLineLimit) {
					int offset = Math.max(maxLineLimit - entry.getKey().length() - 2, 0);
					headerBuilder.append(entry.getValue().substring(0, offset) + "\\" + "\n");
					for (; offset < entry.getValue().length(); offset += maxLineLimit) {
						if ((offset + maxLineLimit) >= entry.getValue().length()) {
							headerBuilder.append(entry.getValue().substring(offset) + "\n");
						} else {
							headerBuilder.append(entry.getValue().substring(offset, offset + maxLineLimit) + "\\" + "\n");
						}
					}
				} else {
					headerBuilder.append(entry.getValue() + "\n");
				}
			}

			headerBuilder.append("\n");
		}
		return headerBuilder.toString();
	}

	private static byte[] stretchPasswordForOpenSsl(final char[] password, final byte[] iv, final int usingIvSize, final int keySize) throws Exception {
		final byte[] passphraseBytes = toBytes(password, "ISO-8859-1");
		final MessageDigest hash = MessageDigest.getInstance("MD5");
		final byte[] key = new byte[keySize];
		int hashesSize = keySize & 0XFFFFFFF0;

		if ((keySize & 0XF) != 0) {
			hashesSize += 0x10;
		}

		final byte[] hashes = new byte[hashesSize];
		byte[] previous;
		for (int index = 0; (index + 0x10) <= hashes.length; hash.update(previous, 0, previous.length)) {
			hash.update(passphraseBytes, 0, passphraseBytes.length);
			hash.update(iv, 0, usingIvSize);
			previous = hash.digest();
			System.arraycopy(previous, 0, hashes, index, previous.length);
			index += previous.length;
		}

		System.arraycopy(hashes, 0, key, 0, key.length);
		return key;
	}

	private static byte[] createRsaBinaryKey(final KeyPair keyPair) throws Exception {
		final RSAPrivateCrtKey privateKey = ((RSAPrivateCrtKey) keyPair.getPrivate());

		return Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_SEQUENCE,
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, BigInteger.ZERO.toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, privateKey.getModulus().toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, privateKey.getPublicExponent().toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, privateKey.getPrivateExponent().toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, privateKey.getPrimeP().toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, privateKey.getPrimeQ().toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, privateKey.getPrimeExponentP().toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, privateKey.getPrimeExponentQ().toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, privateKey.getCrtCoefficient().toByteArray())
				);
	}

	private static byte[] createDssBinaryKey(final KeyPair keyPair) throws Exception {
		final DSAPublicKey publicKey = ((DSAPublicKey) keyPair.getPublic());
		final DSAPrivateKey privateKey = ((DSAPrivateKey) keyPair.getPrivate());

		return Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_SEQUENCE,
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, BigInteger.ZERO.toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, privateKey.getParams().getP().toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, privateKey.getParams().getQ().toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, privateKey.getParams().getG().toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, publicKey.getY().toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, privateKey.getX().toByteArray())
				);
	}

	private static byte[] createEcdsaBinaryKey(final KeyPair keyPair, final byte[] oidKey) throws Exception {
		final ECPrivateKey privateKey = ((ECPrivateKey) keyPair.getPrivate());
		final ECPublicKey publicKey = ((ECPublicKey) keyPair.getPublic());

		return Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_SEQUENCE,
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_INTEGER, BigInteger.ONE.toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_OCTET_STRING, privateKey.getS().toByteArray()),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_CONTEXT_SPECIFIC_0, Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_OBJECT, oidKey)),
				Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_CONTEXT_SPECIFIC_1, Asn1Codec.createDerTagData(Asn1Codec.DER_TAG_BIT_STRING,
						joinByteArrays(
								new byte[] {0, 4},
								publicKey.getW().getAffineX().toByteArray(),
								publicKey.getW().getAffineY().toByteArray()))
						)
				);
	}

	private static byte[] addSecurePadding(byte[] data, final int paddingSize) {
		if (data.length % paddingSize != 0) {
			final byte[] dataPadded = new byte[((data.length / paddingSize) + 1) * paddingSize];
			for (int i = 0; i < data.length; i++) {
				dataPadded[i] = data[i];
			}
			final byte[] randomArray = new byte[dataPadded.length - data.length];
			new SecureRandom().nextBytes(randomArray);
			for (int i = 0; i < randomArray.length; i++) {
				dataPadded[data.length + i] = randomArray[i];
			}
			data = dataPadded;
		}
		return data;
	}

	/**
	 * Converts byte array to base64 with linebreaks
	 */
	private static String toWrappedBase64(final byte[] byteArray, final int maxLineLength, final String lineBreak) {
		return Base64.getMimeEncoder(maxLineLength, lineBreak.getBytes(Charset.forName("UTF-8"))).encodeToString(byteArray);
	}

	private static byte[] joinByteArrays(final byte[]... arrays) throws Exception {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (final byte[] array : arrays) {
			out.write(array);
		}
		return out.toByteArray();
	}

	private static String toHexString(final byte[] data) {
		final StringBuilder returnString = new StringBuilder();
		for (final byte dataByte : data) {
			returnString.append(String.format("%02X", dataByte));
		}
		return returnString.toString();
	}

	private static byte[] toBytes(final char[] chars, final String encoding) {
		final ByteBuffer byteBuffer = Charset.forName(encoding).encode(CharBuffer.wrap(chars));
		final byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return bytes;
	}

	private static byte[] getPublicKeyBytes(final PublicKey publicKey) throws Exception {
		final BlockDataWriter publicKeyWriter = new BlockDataWriter();
		final String algorithmName = getAlgorithm(publicKey);
		if (SSH_CIPHER_NAME_RSA.equalsIgnoreCase(algorithmName)) {
			final RSAPublicKey publicKeyRSA = ((RSAPublicKey) publicKey);
			publicKeyWriter.writeString(SSH_CIPHER_NAME_RSA);
			publicKeyWriter.writeBigInt(publicKeyRSA.getPublicExponent());
			publicKeyWriter.writeBigInt(publicKeyRSA.getModulus());
			return publicKeyWriter.toByteArray();
		} else if (SSH_CIPHER_NAME_DSA.equalsIgnoreCase(algorithmName)) {
			final DSAPublicKey publicKeyDSA = ((DSAPublicKey) publicKey);
			publicKeyWriter.writeString(SSH_CIPHER_NAME_DSA);
			publicKeyWriter.writeBigInt(publicKeyDSA.getParams().getP());
			publicKeyWriter.writeBigInt(publicKeyDSA.getParams().getQ());
			publicKeyWriter.writeBigInt(publicKeyDSA.getParams().getG());
			publicKeyWriter.writeBigInt(publicKeyDSA.getY());
		} else if (SSH_CIPHER_NAME_ECDSA_NISTP256.equalsIgnoreCase(algorithmName)
				|| SSH_CIPHER_NAME_ECDSA_NISTP384.equalsIgnoreCase(algorithmName)
				|| SSH_CIPHER_NAME_ECDSA_NISTP521.equalsIgnoreCase(algorithmName)) {
			final ECPublicKey publicKeyEC = ((ECPublicKey) publicKey);
			final int bitLength = publicKeyEC.getW().getAffineX().bitLength();
			String curveName = null;
			int qLength;
			if (bitLength <= 256) {
				curveName = "nistp256";
				qLength = 65;
			} else if (bitLength <= 384) {
				curveName = "nistp384";
				qLength = 97;
			} else if (bitLength <= 521) {
				curveName = "nistp521";
				qLength = 133;
			} else {
				throw new Exception("Unsupported ECDSA bit length: " + bitLength);
			}

			publicKeyWriter.writeString(algorithmName);
			publicKeyWriter.writeString(curveName);

			final byte[] javaEncoding = publicKeyEC.getEncoded();
			final byte[] q = new byte[qLength];
			System.arraycopy(javaEncoding, javaEncoding.length - qLength, q, 0, qLength);
			publicKeyWriter.writeData(q);
		} else {
			throw new IllegalArgumentException("Unsupported cipher: " + algorithmName);
		}
		return publicKeyWriter.toByteArray();
	}

	private static byte[] getPrivateKeyBytes(final PrivateKey privateKey) throws Exception {
		final BlockDataWriter privateKeyWriter = new BlockDataWriter();
		final String algorithmName = getAlgorithm(privateKey);
		if (SSH_CIPHER_NAME_RSA.equalsIgnoreCase(algorithmName)) {
			final RSAPrivateCrtKey privateKeyRSA = ((RSAPrivateCrtKey) privateKey);
			privateKeyWriter.writeBigInt(privateKeyRSA.getPrivateExponent());
			privateKeyWriter.writeBigInt(privateKeyRSA.getPrimeP());
			privateKeyWriter.writeBigInt(privateKeyRSA.getPrimeQ());
			privateKeyWriter.writeBigInt(privateKeyRSA.getCrtCoefficient());
		} else if (SSH_CIPHER_NAME_DSA.equalsIgnoreCase(algorithmName)) {
			final DSAPrivateKey privateKeyDSA = ((DSAPrivateKey) privateKey);
			privateKeyWriter.writeBigInt(privateKeyDSA.getX());
		} else if (SSH_CIPHER_NAME_ECDSA_NISTP256.equalsIgnoreCase(algorithmName)
				|| SSH_CIPHER_NAME_ECDSA_NISTP384.equalsIgnoreCase(algorithmName)
				|| SSH_CIPHER_NAME_ECDSA_NISTP521.equalsIgnoreCase(algorithmName)) {
			final ECPrivateKey privateKeyEC = ((ECPrivateKey) privateKey);
			privateKeyWriter.writeBigInt(privateKeyEC.getS());
		} else {
			throw new IllegalArgumentException("Unsupported cipher: " + algorithmName);
		}
		return privateKeyWriter.toByteArray();
	}

	/**
	 * Key type<br />
	 * "ssh-rsa" for RSA key<br />
	 * "ssh-dss" for DSA key<br />
	 * "ecdsa-sha2-nistp256" or "ecdsa-sha2-nistp384" or "ecdsa-sha2-nistp521" for ECDSA key<br />
	 */
	private static String getAlgorithm(final KeyPair keyPair) throws Exception {
		if (keyPair.getPublic() != null) {
			return getAlgorithm(keyPair.getPublic());
		} else if (keyPair.getPrivate() != null) {
			return getAlgorithm(keyPair.getPrivate());
		} else{
			throw new IllegalArgumentException("Keypair contains no key data");
		}
	}

	/**
	 * Key type<br />
	 * "ssh-rsa" for RSA key<br />
	 * "ssh-dss" for DSA key<br />
	 * "ecdsa-sha2-nistp256" or "ecdsa-sha2-nistp384" or "ecdsa-sha2-nistp521" for ECDSA key<br />
	 */
	private static String getAlgorithm(final PublicKey publicKey) throws Exception {
		if (publicKey.getAlgorithm().equals("RSA")){
			return SSH_CIPHER_NAME_RSA;
		} else if(publicKey.getAlgorithm().equals("DSA")){
			return SSH_CIPHER_NAME_DSA;
		} else if(publicKey.getAlgorithm().equals("EC")){
			final int bitLength = ((ECPublicKey) publicKey).getW().getAffineX().bitLength();
			if (bitLength <= 256) {
				return SSH_CIPHER_NAME_ECDSA_NISTP256;
			} else if (bitLength <= 384) {
				return SSH_CIPHER_NAME_ECDSA_NISTP384;
			} else if (bitLength <= 521) {
				return SSH_CIPHER_NAME_ECDSA_NISTP521;
			} else {
				throw new Exception("Unsupported ECDSA bit length: " + bitLength);
			}
		} else{
			throw new IllegalArgumentException("Unknown public key cipher");
		}
	}

	/**
	 * Key type<br />
	 * "ssh-rsa" for RSA key<br />
	 * "ssh-dss" for DSA key<br />
	 * "ecdsa-sha2-nistp256" or "ecdsa-sha2-nistp384" or "ecdsa-sha2-nistp521" for ECDSA key<br />
	 */
	private static String getAlgorithm(final PrivateKey privateKey) throws Exception {
		if (privateKey instanceof RSAPrivateCrtKey) {
			return SSH_CIPHER_NAME_RSA;
		} else if (privateKey instanceof DSAPrivateKey) {
			return SSH_CIPHER_NAME_DSA;
		} else if (privateKey instanceof ECPrivateKey) {
			final int bitLength = ((ECPrivateKey) privateKey).getS().bitLength();
			if (bitLength <= 256) {
				return SSH_CIPHER_NAME_ECDSA_NISTP256;
			} else if (bitLength <= 384) {
				return SSH_CIPHER_NAME_ECDSA_NISTP384;
			} else if (bitLength <= 521) {
				return SSH_CIPHER_NAME_ECDSA_NISTP521;
			} else {
				throw new Exception("Unsupported ECDSA bit length: " + bitLength);
			}
		} else{
			throw new IllegalArgumentException("Unknown private key cipher");
		}
	}

	@Override
	public void close() {
		try {
			outputStream.close();
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	private static class BlockDataWriter {
		private final ByteArrayOutputStream outputStream;
		private final DataOutput keyDataOutput;

		private BlockDataWriter() {
			outputStream = new ByteArrayOutputStream();
			keyDataOutput = new DataOutputStream(outputStream);
		}

		private void writeBigInt(final BigInteger bigIntegerData) throws Exception {
			writeData(bigIntegerData.toByteArray());
		}

		private void writeString(final String stringData) throws Exception {
			writeData(stringData.getBytes("ISO-8859-1"));
		}

		private void writeData(final byte[] data) throws IOException, Exception {
			try {
				if (data.length <= 0) {
					throw new Exception("Key blocksize error");
				}

				keyDataOutput.writeInt(data.length);
				keyDataOutput.write(data);
			} catch (final IOException e) {
				throw new Exception("Key block write error", e);
			}
		}

		private byte[] toByteArray() {
			return outputStream.toByteArray();
		}
	}
}
