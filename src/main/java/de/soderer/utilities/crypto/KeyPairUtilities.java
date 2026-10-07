package de.soderer.utilities.crypto;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPrivateKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import de.soderer.utilities.BitUtilities;
import de.soderer.utilities.crypto.Asn1Codec.DerTag;

/**
 * Utilities for creating key pairs and for OpenSSH related key data: algorithm names ("ssh-rsa", "ecdsa-sha2-nistp256" etc.),
 * the OpenSSH public key blob, key fingerprints and the authorized_keys line format.
 */
public class KeyPairUtilities {
	private KeyPairUtilities() {
		// Utility class
	}

	/**
	 * Create a RSA keypair of given strength with the default JCE provider.
	 *
	 * @param keyStrength key size in bits, at least 2048
	 * @return new key pair
	 * @throws Exception if the key strength is below 2048 bit or the key cannot be generated
	 */
	public static KeyPair createRsaKeyPair(final int keyStrength) throws Exception {
		if (keyStrength < 2048) {
			// RSA keys below 2048 bit are considered insecure by current standards (NIST/BSI).
			// 512 bit RSA is trivially factorizable within hours on commodity hardware.
			throw new Exception("Invalid RSA key strength (minimum 2048 bit required): " + keyStrength);
		}
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(keyStrength);
		return keyPairGenerator.generateKeyPair();
	}

	/**
	 * Create a DSA keypair of 1024 bit strength<br>
	 * Watchout: OpenSSH only supports 1024 bit DSA key strength
	 *
	 * @return new key pair
	 * @throws Exception if the key cannot be generated
	 * @deprecated DSA-1024 is considered cryptographically weak by current standards
	 *             (NIST/BSI). Kept only for compatibility with legacy OpenSSH DSA keys;
	 *             do not use for newly generated keys unless this specific legacy
	 *             compatibility is required.
	 */
	@Deprecated
	public static KeyPair createDsaKeyPair() throws Exception {
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DSA");
		keyPairGenerator.initialize(1024);
		return keyPairGenerator.generateKeyPair();
	}

	/**
	 * Create a DSA keypair of given strength<br>
	 * Watchout: OpenSSH only supports 1024 bit DSA key strength
	 *
	 * @param keyStrength key size in bits
	 * @return new key pair
	 * @throws Exception if the key cannot be generated
	 * @deprecated DSA is considered legacy and, at the 1024 bit strength required for
	 *             OpenSSH compatibility, cryptographically weak by current standards
	 *             (NIST/BSI). Use only for legacy compatibility, never for new keys
	 *             without a specific compatibility requirement.
	 */
	@Deprecated
	public static KeyPair createDsaKeyPair(final int keyStrength) throws Exception {
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("DSA");
		keyPairGenerator.initialize(keyStrength);
		return keyPairGenerator.generateKeyPair();
	}

	/**
	 * Create a ECDSA keypair of eliptic curve name.<br>
	 * Supported eliptic curve names (case insensitive) are "nistp256" or "secp256", "nistp384" or "secp384" and "nistp521" or "secp521".
	 *
	 * @param ecdsaCurveName curve name
	 * @return new key pair
	 * @throws Exception if the curve name is empty or unknown
	 */
	public static KeyPair createEllipticCurveKeyPair(final String ecdsaCurveName) throws Exception {
		if (ecdsaCurveName == null || "".equals(ecdsaCurveName.trim())) {
			throw new Exception("Missing ECDSA curve name parameter");
		}
		final String curveName = ecdsaCurveName.toLowerCase(Locale.ROOT).trim().replace("nist", "sec");
		if (!"secp256".equals(curveName) && !"secp384".equals(curveName) && !"secp521".equals(curveName)) {
			throw new Exception("Unknown ECDSA curve name: " + ecdsaCurveName);
		}
		final AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
		parameters.init(new ECGenParameterSpec(curveName + "r1"));
		final ECParameterSpec ecParameterSpec = parameters.getParameterSpec(ECParameterSpec.class);
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
		keyPairGenerator.initialize(ecParameterSpec, new SecureRandom());
		return keyPairGenerator.generateKeyPair();
	}

	/**
	 * Create a ECDSA keypair of the NIST curve with the given size.
	 *
	 * @param curveId curve size in bits: 256, 384 or 521
	 * @return new key pair
	 * @throws Exception if the curve size is not supported
	 */
	public static KeyPair createEllipticCurveKeyPair(final int curveId) throws Exception {
		if (256 != curveId && 384 != curveId && 521 != curveId) {
			throw new Exception("Invalid ECDSA curve id parameter");
		}
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
		keyPairGenerator.initialize(curveId);
		return keyPairGenerator.generateKeyPair();
	}

	/**
	 * Create an EdDSA keypair on curve Ed25519.
	 *
	 * @return new key pair
	 * @throws Exception if the key cannot be generated
	 */
	public static KeyPair createEd25519CurveKeyPair() throws Exception {
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed25519");
		return keyPairGenerator.generateKeyPair();
	}

	/**
	 * Create an EdDSA keypair on curve Ed448.
	 *
	 * @return new key pair
	 * @throws Exception if the key cannot be generated
	 */
	public static KeyPair createEd448CurveKeyPair() throws Exception {
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("Ed448");
		return keyPairGenerator.generateKeyPair();
	}

	/**
	 * Returns the OpenSSH key type name of a key pair (the private key is used if available):<br>
	 * "ssh-rsa" for RSA key<br>
	 * "ssh-dss" for DSA key<br>
	 * "ecdsa-sha2-nistp256" or "ecdsa-sha2-nistp384" or "ecdsa-sha2-nistp521" for ECDSA key<br>
	 * "ssh-ed25519" or "ssh-ed448" for EdDSA key
	 *
	 * @param keyPair key pair
	 * @return OpenSSH key type name
	 * @throws Exception if the key is null or of an unsupported type
	 */
	public static String getAlgorithm(final KeyPair keyPair) throws Exception {
		if (keyPair == null) {
			throw new Exception("Invalid empty keyPair parameter");
		} else if (keyPair.getPrivate() != null) {
			return getAlgorithm(keyPair.getPrivate());
		} else if (keyPair.getPublic() != null) {
			return getAlgorithm(keyPair.getPublic());
		} else {
			throw new Exception("KeyPair data is empty");
		}
	}

	/**
	 * Returns the OpenSSH key type name of a public key:<br>
	 * "ssh-rsa" for RSA key<br>
	 * "ssh-dss" for DSA key<br>
	 * "ecdsa-sha2-nistp256" or "ecdsa-sha2-nistp384" or "ecdsa-sha2-nistp521" for ECDSA key<br>
	 * "ssh-ed25519" or "ssh-ed448" for EdDSA key
	 *
	 * @param publicKey public key
	 * @return OpenSSH key type name
	 * @throws Exception if the key is null or of an unsupported type
	 */
	public static String getAlgorithm(final PublicKey publicKey) throws Exception {
		if (publicKey == null){
			throw new Exception("Invalid empty publicKey parameter");
		} else if ("RSA".equals(publicKey.getAlgorithm())) {
			return "ssh-rsa";
		} else if ("DSA".equals(publicKey.getAlgorithm())) {
			return "ssh-dss";
		} else if ("EC".equals(publicKey.getAlgorithm()) || "ECDSA".equals(publicKey.getAlgorithm())) {
			return "ecdsa-sha2-" + CryptographicUtilities.getEcDsaEllipticCurveName((ECPublicKey) publicKey);
		} else if (publicKey instanceof EdECPublicKey) {
			// JDK keys report the algorithm "EdDSA", BouncyCastle keys report "Ed25519" or "Ed448"
			final EdECPublicKey publicEdDsaKey = (EdECPublicKey) publicKey;
			if ("Ed25519".equals(publicEdDsaKey.getParams().getName())) {
				return "ssh-ed25519";
			} else if ("Ed448".equals(publicEdDsaKey.getParams().getName())) {
				return "ssh-ed448";
			} else {
				throw new Exception("Unsupported EdDSA algorithm name: " + publicEdDsaKey.getParams().getName());
			}
		} else {
			throw new Exception("Unsupported ssh algorithm name: " + publicKey.getAlgorithm());
		}
	}

	/**
	 * Returns the OpenSSH key type name of a private key:<br>
	 * "ssh-rsa" for RSA key<br>
	 * "ssh-dss" for DSA key<br>
	 * "ecdsa-sha2-nistp256" or "ecdsa-sha2-nistp384" or "ecdsa-sha2-nistp521" for ECDSA key<br>
	 * "ssh-ed25519" or "ssh-ed448" for EdDSA key
	 *
	 * @param privateKey private key
	 * @return OpenSSH key type name
	 * @throws Exception if the key is null or of an unsupported type
	 */
	public static String getAlgorithm(final PrivateKey privateKey) throws Exception {
		if (privateKey == null){
			throw new Exception("Invalid empty privateKey parameter");
		} else if (privateKey instanceof RSAPrivateCrtKey) {
			return "ssh-rsa";
		} else if (privateKey instanceof DSAPrivateKey) {
			return "ssh-dss";
		} else if (privateKey instanceof ECPrivateKey) {
			return "ecdsa-sha2-" + CryptographicUtilities.getEcDsaEllipticCurveName((ECPrivateKey) privateKey);
		} else if (privateKey instanceof EdECPrivateKey) {
			final EdECPrivateKey privateKeyEdEC = (EdECPrivateKey) privateKey;
			if ("Ed25519".equalsIgnoreCase(privateKeyEdEC.getParams().getName())) {
				return "ssh-ed25519";
			} else if ("Ed448".equalsIgnoreCase(privateKeyEdEC.getParams().getName())) {
				return "ssh-ed448";
			} else {
				throw new Exception("Unknown EdDSA type: " + privateKeyEdEC.getParams().getName());
			}
		} else {
			throw new IllegalArgumentException("Unknown private key cipher");
		}
	}

	/**
	 * Returns the key strength of the public key of a key pair, see {@link #getKeyStrength(PublicKey)}.
	 *
	 * @param keyPair key pair
	 * @return key strength in bits
	 * @throws Exception if the key pair is null or the key type is not supported
	 */
	public static int getKeyStrength(final KeyPair keyPair) throws Exception {
		if (keyPair == null) {
			throw new Exception("Invalid empty keyPair parameter");
		} else {
			return getKeyStrength(keyPair.getPublic());
		}
	}

	/**
	 * Returns the key strength: the modulus size for RSA, the size of the prime p for DSA and the curve size for ECDSA keys.
	 *
	 * @param publicKey public key
	 * @return key strength in bits
	 * @throws Exception if the key is null or the key type is not supported (e.g. EdDSA)
	 */
	public static int getKeyStrength(final PublicKey publicKey) throws Exception {
		if (publicKey == null) {
			throw new Exception("Invalid empty publicKey parameter");
		} else {
			final String algorithmName = getAlgorithm(publicKey);
			if ("ssh-rsa".equalsIgnoreCase(algorithmName)) {
				return ((RSAPublicKey) publicKey).getModulus().bitLength();
			} else if ("ssh-dss".equalsIgnoreCase(algorithmName)) {
				// The DSA key size is the size of the prime p, the public value y may be some bits shorter
				return ((DSAPublicKey) publicKey).getParams().getP().bitLength();
			} else if ("ecdsa-sha2-nistp256".equalsIgnoreCase(algorithmName)) {
				return 256;
			} else if ("ecdsa-sha2-nistp384".equalsIgnoreCase(algorithmName)) {
				return 384;
			} else if ("ecdsa-sha2-nistp521".equalsIgnoreCase(algorithmName)) {
				return 521;
			} else {
				throw new Exception("Unsupported ssh algorithm name: " + algorithmName);
			}
		}
	}

	/**
	 * Returns the OpenSSH public key blob (as used in authorized_keys and for fingerprints) of a RSA, DSA, ECDSA or EdDSA public key.
	 *
	 * @param publicKey public key
	 * @return OpenSSH public key blob
	 * @throws Exception if the key is null or of an unsupported type
	 */
	public static byte[] getPublicKeyBytes(final PublicKey publicKey) throws Exception {
		if (publicKey == null) {
			throw new Exception("Invalid empty publicKey parameter");
		} else {
			final BlockDataWriter publicKeyWriter = new BlockDataWriter();
			final String algorithmName = getAlgorithm(publicKey);
			if (publicKey instanceof RSAPublicKey) {
				final RSAPublicKey publicKeyRSA = (RSAPublicKey) publicKey;
				publicKeyWriter.writeString("ssh-rsa");
				publicKeyWriter.writeBigInt(publicKeyRSA.getPublicExponent());
				publicKeyWriter.writeBigInt(publicKeyRSA.getModulus());
				return publicKeyWriter.toByteArray();
			} else if (publicKey instanceof DSAPublicKey) {
				final DSAPublicKey publicKeyDSA = (DSAPublicKey) publicKey;
				publicKeyWriter.writeString("ssh-dss");
				publicKeyWriter.writeBigInt(publicKeyDSA.getParams().getP());
				publicKeyWriter.writeBigInt(publicKeyDSA.getParams().getQ());
				publicKeyWriter.writeBigInt(publicKeyDSA.getParams().getG());
				publicKeyWriter.writeBigInt(publicKeyDSA.getY());
			} else if (publicKey instanceof ECPublicKey) {
				final ECPublicKey publicKeyEC = (ECPublicKey) publicKey;
				final String ecCurveName = CryptographicUtilities.getEcDsaEllipticCurveName(publicKeyEC);
				publicKeyWriter.writeString(algorithmName);
				publicKeyWriter.writeString(ecCurveName);

				final DerTag enclosingDerTag = Asn1Codec.readDerTag(publicKeyEC.getEncoded());
				if (Asn1Codec.DER_TAG_SEQUENCE != enclosingDerTag.getTagId()) {
					throw new Exception("Invalid key data found");
				}
				final List<DerTag> derDataTags = Asn1Codec.readDerTags(enclosingDerTag.getData());
				if (Asn1Codec.DER_TAG_SEQUENCE != derDataTags.get(0).getTagId()) {
					throw new Exception("Invalid key data found");
				}
				final List<DerTag> sshAlgorithmDerTags = Asn1Codec.readDerTags(derDataTags.get(0).getData());
				final OID ecDsaPublicKeyOid = new OID(sshAlgorithmDerTags.get(0).getData());
				if (Arrays.equals(OID.ECDSA_PUBLICKEY_ARRAY, ecDsaPublicKeyOid.getByteArrayEncoding())) {
					final OID ecDsaCurveOid = new OID(sshAlgorithmDerTags.get(1).getData());
					if (Arrays.equals(OID.ECDSA_CURVE_NISTP256_ARRAY, ecDsaCurveOid.getByteArrayEncoding())
							|| Arrays.equals(OID.ECDSA_CURVE_NISTP384_ARRAY, ecDsaCurveOid.getByteArrayEncoding())
							|| Arrays.equals(OID.ECDSA_CURVE_NISTP521_ARRAY, ecDsaCurveOid.getByteArrayEncoding())
							|| Arrays.equals(OID.ECDSA_CURVE_ANSIP256K1_ARRAY, ecDsaCurveOid.getByteArrayEncoding())) {
						if (Asn1Codec.DER_TAG_BIT_STRING != derDataTags.get(1).getTagId()) {
							throw new Exception("Invalid key data found");
						} else {
							// remove first byte (odd sign)
							final byte[] dataWithLeadingZero = derDataTags.get(1).getData();
							final byte[] qBytes = Arrays.copyOfRange(dataWithLeadingZero, 1, dataWithLeadingZero.length);
							publicKeyWriter.writeData(qBytes);
						}
					} else {
						throw new Exception("Unknown SSH EcDSA curve OID: " + ecDsaCurveOid.getStringEncoding());
					}
				} else {
					throw new Exception("Unknown SSH EcDSA public key OID: " + ecDsaPublicKeyOid.getStringEncoding());
				}
			} else if (publicKey instanceof EdECPublicKey) {
				final EdECPublicKey publicKeyEdEC = (EdECPublicKey) publicKey;
				int publicKeyDataLength;
				if ("Ed25519".equalsIgnoreCase(publicKeyEdEC.getParams().getName())) {
					publicKeyWriter.writeString("ssh-ed25519");
					publicKeyDataLength = 32;
				} else if ("Ed448".equalsIgnoreCase(publicKeyEdEC.getParams().getName())) {
					publicKeyWriter.writeString("ssh-ed448");
					publicKeyDataLength = 57;
				} else {
					throw new Exception("Unknown EdDSA type: " + publicKeyEdEC.getParams().getName());
				}

				final byte[] javaEncoding = publicKeyEdEC.getEncoded();
				final byte[] publicKeyData = new byte[publicKeyDataLength];
				System.arraycopy(javaEncoding, javaEncoding.length - publicKeyDataLength, publicKeyData, 0, publicKeyDataLength);
				publicKeyWriter.writeData(publicKeyData);
			} else {
				throw new Exception("Unsupported algorithm name: " + algorithmName);
			}
			return publicKeyWriter.toByteArray();
		}
	}

	/**
	 * Returns the MD5 fingerprint of the OpenSSH public key blob of the public key of a key pair (upper case hex with ":" separators).
	 *
	 * @param keyPair key pair
	 * @return fingerprint
	 * @throws Exception if the key pair is null or the public key is not supported
	 */
	public static String getMd5Fingerprint(final KeyPair keyPair) throws Exception {
		if (keyPair == null) {
			throw new Exception("Invalid empty keyPair parameter");
		} else {
			return getMd5Fingerprint(keyPair.getPublic());
		}
	}

	/**
	 * Returns the MD5 fingerprint of the OpenSSH public key blob of a public key (upper case hex with ":" separators).
	 *
	 * @param publicKey public key
	 * @return fingerprint
	 * @throws Exception if the key is null or not supported
	 */
	public static String getMd5Fingerprint(final PublicKey publicKey) throws Exception {
		if (publicKey == null) {
			throw new Exception("Invalid empty publicKey parameter");
		} else {
			try {
				final MessageDigest md = MessageDigest.getInstance("MD5");
				md.update(getPublicKeyBytes(publicKey));
				return BitUtilities.toHexString(md.digest(), ":").toUpperCase(Locale.ROOT);
			} catch (final Exception e) {
				throw new Exception("Cannot create MD5 fingerprint", e);
			}
		}
	}

	/**
	 * Returns the SHA-1 fingerprint of the OpenSSH public key blob of the public key of a key pair (lower case hex with ":" separators).
	 *
	 * @param keyPair key pair
	 * @return fingerprint
	 * @throws Exception if the key pair is null or the public key is not supported
	 */
	public static String getSha1Fingerprint(final KeyPair keyPair) throws Exception {
		if (keyPair == null) {
			throw new Exception("Invalid empty keyPair parameter");
		} else {
			return getSha1Fingerprint(keyPair.getPublic());
		}
	}

	/**
	 * Returns the SHA-1 fingerprint of the OpenSSH public key blob of a public key (lower case hex with ":" separators).
	 *
	 * @param publicKey public key
	 * @return fingerprint
	 * @throws Exception if the key is null or not supported
	 */
	public static String getSha1Fingerprint(final PublicKey publicKey) throws Exception {
		if (publicKey == null) {
			throw new Exception("Invalid empty publicKey parameter");
		} else {
			try {
				final MessageDigest md = MessageDigest.getInstance("SHA-1");
				md.update(getPublicKeyBytes(publicKey));
				return BitUtilities.toHexString(md.digest(), ":");
			} catch (final Exception e) {
				throw new Exception("Cannot create SHA1 fingerprint", e);
			}
		}
	}

	/**
	 * Returns the SHA-1 fingerprint of the OpenSSH public key blob of the public key of a key pair (Base64 encoded).
	 *
	 * @param keyPair key pair
	 * @return fingerprint
	 * @throws Exception if the key pair is null or the public key is not supported
	 */
	public static String getSha1FingerprintBase64(final KeyPair keyPair) throws Exception {
		if (keyPair == null) {
			throw new Exception("Invalid empty keyPair parameter");
		} else {
			return getSha1FingerprintBase64(keyPair.getPublic());
		}
	}

	/**
	 * Returns the SHA-1 fingerprint of the OpenSSH public key blob of a public key (Base64 encoded).
	 *
	 * @param publicKey public key
	 * @return fingerprint
	 * @throws Exception if the key is null or not supported
	 */
	public static String getSha1FingerprintBase64(final PublicKey publicKey) throws Exception {
		if (publicKey == null) {
			throw new Exception("Invalid empty publicKey parameter");
		} else {
			try {
				final MessageDigest md = MessageDigest.getInstance("SHA-1");
				md.update(getPublicKeyBytes(publicKey));
				return Base64.getEncoder().encodeToString(md.digest());
			} catch (final Exception e) {
				throw new Exception("Cannot create SHA1 fingerprint", e);
			}
		}
	}

	/**
	 * Returns the SHA-256 fingerprint of the OpenSSH public key blob of the public key of a key pair (lower case hex with ":" separators).
	 *
	 * @param keyPair key pair
	 * @return fingerprint
	 * @throws Exception if the key pair is null or the public key is not supported
	 */
	public static String getSha256Fingerprint(final KeyPair keyPair) throws Exception {
		if (keyPair == null) {
			throw new Exception("Invalid empty keyPair parameter");
		} else {
			return getSha256Fingerprint(keyPair.getPublic());
		}
	}

	/**
	 * Returns the SHA-256 fingerprint of the OpenSSH public key blob of a public key (lower case hex with ":" separators).
	 *
	 * @param publicKey public key
	 * @return fingerprint
	 * @throws Exception if the key is null or not supported
	 */
	public static String getSha256Fingerprint(final PublicKey publicKey) throws Exception {
		if (publicKey == null) {
			throw new Exception("Invalid empty publicKey parameter");
		} else {
			try {
				final MessageDigest md = MessageDigest.getInstance("SHA-256");
				md.update(getPublicKeyBytes(publicKey));
				return BitUtilities.toHexString(md.digest(), ":");
			} catch (final Exception e) {
				throw new Exception("Cannot create SHA256 fingerprint", e);
			}
		}
	}

	/**
	 * Returns the SHA-256 fingerprint of the OpenSSH public key blob of the public key of a key pair (Base64 encoded).
	 *
	 * @param keyPair key pair
	 * @return fingerprint
	 * @throws Exception if the key pair is null or the public key is not supported
	 */
	public static String getSha256FingerprintBase64(final KeyPair keyPair) throws Exception {
		if (keyPair == null) {
			throw new Exception("Invalid empty keyPair parameter");
		} else {
			return getSha256FingerprintBase64(keyPair.getPublic());
		}
	}

	/**
	 * Returns the SHA-256 fingerprint of the OpenSSH public key blob of a public key (Base64 encoded).
	 *
	 * @param publicKey public key
	 * @return fingerprint
	 * @throws Exception if the key is null or not supported
	 */
	public static String getSha256FingerprintBase64(final PublicKey publicKey) throws Exception {
		if (publicKey == null) {
			throw new Exception("Invalid empty publicKey parameter");
		} else {
			try {
				final MessageDigest md = MessageDigest.getInstance("SHA-256");
				md.update(getPublicKeyBytes(publicKey));
				return Base64.getEncoder().encodeToString(md.digest());
			} catch (final Exception e) {
				throw new Exception("Cannot create SHA256 fingerprint", e);
			}
		}
	}

	/**
	 * Returns the SHA-384 fingerprint of the OpenSSH public key blob of the public key of a key pair (lower case hex with ":" separators).
	 *
	 * @param keyPair key pair
	 * @return fingerprint
	 * @throws Exception if the key pair is null or the public key is not supported
	 */
	public static String getSha384Fingerprint(final KeyPair keyPair) throws Exception {
		if (keyPair == null) {
			throw new Exception("Invalid empty keyPair parameter");
		} else {
			return getSha384Fingerprint(keyPair.getPublic());
		}
	}

	/**
	 * Returns the SHA-384 fingerprint of the OpenSSH public key blob of a public key (lower case hex with ":" separators).
	 *
	 * @param publicKey public key
	 * @return fingerprint
	 * @throws Exception if the key is null or not supported
	 */
	public static String getSha384Fingerprint(final PublicKey publicKey) throws Exception {
		if (publicKey == null) {
			throw new Exception("Invalid empty publicKey parameter");
		} else {
			try {
				final MessageDigest md = MessageDigest.getInstance("SHA-384");
				md.update(getPublicKeyBytes(publicKey));
				return BitUtilities.toHexString(md.digest(), ":");
			} catch (final Exception e) {
				throw new Exception("Cannot create SHA384 fingerprint", e);
			}
		}
	}

	/**
	 * Returns the SHA-384 fingerprint of the OpenSSH public key blob of the public key of a key pair (Base64 encoded).
	 *
	 * @param keyPair key pair
	 * @return fingerprint
	 * @throws Exception if the key pair is null or the public key is not supported
	 */
	public static String getSha384FingerprintBase64(final KeyPair keyPair) throws Exception {
		if (keyPair == null) {
			throw new Exception("Invalid empty keyPair parameter");
		} else {
			return getSha384FingerprintBase64(keyPair.getPublic());
		}
	}

	/**
	 * Returns the SHA-384 fingerprint of the OpenSSH public key blob of a public key (Base64 encoded).
	 *
	 * @param publicKey public key
	 * @return fingerprint
	 * @throws Exception if the key is null or not supported
	 */
	public static String getSha384FingerprintBase64(final PublicKey publicKey) throws Exception {
		if (publicKey == null) {
			throw new Exception("Invalid empty publicKey parameter");
		} else {
			try {
				final MessageDigest md = MessageDigest.getInstance("SHA-384");
				md.update(getPublicKeyBytes(publicKey));
				return Base64.getEncoder().encodeToString(md.digest());
			} catch (final Exception e) {
				throw new Exception("Cannot create SHA384 fingerprint", e);
			}
		}
	}

	/**
	 * Returns the SHA-512 fingerprint of the OpenSSH public key blob of the public key of a key pair (lower case hex with ":" separators).
	 *
	 * @param keyPair key pair
	 * @return fingerprint
	 * @throws Exception if the key pair is null or the public key is not supported
	 */
	public static String getSha512Fingerprint(final KeyPair keyPair) throws Exception {
		if (keyPair == null) {
			throw new Exception("Invalid empty keyPair parameter");
		} else {
			return getSha512Fingerprint(keyPair.getPublic());
		}
	}

	/**
	 * Returns the SHA-512 fingerprint of the OpenSSH public key blob of a public key (lower case hex with ":" separators).
	 *
	 * @param publicKey public key
	 * @return fingerprint
	 * @throws Exception if the key is null or not supported
	 */
	public static String getSha512Fingerprint(final PublicKey publicKey) throws Exception {
		if (publicKey == null) {
			throw new Exception("Invalid empty publicKey parameter");
		} else {
			try {
				final MessageDigest md = MessageDigest.getInstance("SHA-512");
				md.update(getPublicKeyBytes(publicKey));
				return BitUtilities.toHexString(md.digest(), ":");
			} catch (final Exception e) {
				throw new Exception("Cannot create SHA512 fingerprint", e);
			}
		}
	}

	/**
	 * Returns the SHA-512 fingerprint of the OpenSSH public key blob of the public key of a key pair (Base64 encoded).
	 *
	 * @param keyPair key pair
	 * @return fingerprint
	 * @throws Exception if the key pair is null or the public key is not supported
	 */
	public static String getSha512FingerprintBase64(final KeyPair keyPair) throws Exception {
		if (keyPair == null) {
			throw new Exception("Invalid empty keyPair parameter");
		} else {
			return getSha512FingerprintBase64(keyPair.getPublic());
		}
	}

	/**
	 * Returns the SHA-512 fingerprint of the OpenSSH public key blob of a public key (Base64 encoded).
	 *
	 * @param publicKey public key
	 * @return fingerprint
	 * @throws Exception if the key is null or not supported
	 */
	public static String getSha512FingerprintBase64(final PublicKey publicKey) throws Exception {
		if (publicKey == null) {
			throw new Exception("Invalid empty publicKey parameter");
		} else {
			try {
				final MessageDigest md = MessageDigest.getInstance("SHA-512");
				md.update(getPublicKeyBytes(publicKey));
				return Base64.getEncoder().encodeToString(md.digest());
			} catch (final Exception e) {
				throw new Exception("Cannot create SHA512 fingerprint", e);
			}
		}
	}

	/**
	 * Returns the authorized_keys line (key type and Base64 encoded public key blob, without comment) of the public key of a key pair.
	 *
	 * @param keyPair key pair
	 * @return authorized_keys line like "ssh-ed25519 AAAA..."
	 * @throws Exception if the key pair or its public key is missing or not supported
	 */
	public static String encodePublicKeyForAuthorizedKeys(final KeyPair keyPair) throws Exception {
		if (keyPair == null) {
			throw new Exception("Invalid empty keyPair parameter");
		} else if (keyPair.getPublic() == null) {
			throw new Exception("Missing public key for AuthorizedKey string generation");
		} else {
			return encodePublicKeyForAuthorizedKeys(keyPair.getPublic());
		}
	}

	/**
	 * Returns the authorized_keys line (key type and Base64 encoded public key blob, without comment) of a public key.
	 *
	 * @param publicKey public key
	 * @return authorized_keys line like "ssh-ed25519 AAAA..."
	 * @throws Exception if the key is null or not supported
	 */
	public static String encodePublicKeyForAuthorizedKeys(final PublicKey publicKey) throws Exception {
		if (publicKey == null) {
			throw new Exception("Invalid empty publicKey parameter");
		} else {
			return getAlgorithm(publicKey) + " " + new String(Base64.getEncoder().encode(getPublicKeyBytes(publicKey)));
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
			writeData(stringData.getBytes(StandardCharsets.ISO_8859_1));
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
