package de.soderer.utilities;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketVector;
import org.bouncycastle.openpgp.PGPUtil;

public class PgpUtilities {
	public static String DEFAULT_SIGNATURE_METHOD_PGP = "SHA-1";
	public static final String[] KNOWN_SIGNATURE_METHODS_PGP = new String[] { DEFAULT_SIGNATURE_METHOD_PGP, "MD5", "RIPEMD160", "DOUBLE-SHA", "MD2", "TIGER-192", "HAVAL-5-160", "SHA-256", "SHA-384",
			"SHA-512", "SHA-224" };

	public static PGPSecretKey generatePGPSecretKey(int keyStrength, String identity, char[] password, Date validity) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try {
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", "BC");
			keyPairGenerator.initialize(keyStrength, new SecureRandom());
			KeyPair keyPair = keyPairGenerator.generateKeyPair();

			PGPPublicKey publicKey = new PGPPublicKey(PublicKeyAlgorithmTags.RSA_GENERAL, keyPair.getPublic(), new Date());
			PGPPrivateKey privateKey = new PGPPrivateKey(keyPair.getPrivate(), publicKey.getKeyID());
			PGPKeyPair masterKeyPair = new PGPKeyPair(publicKey, privateKey);

			long secondsUntilExpiration = (validity.getTime() - new Date().getTime()) / 1000L + 1;

			PGPSignatureSubpacketGenerator subpacketGenerator = new PGPSignatureSubpacketGenerator();
			PGPSignatureSubpacketVector subpacketVector = null;
			subpacketGenerator.setKeyExpirationTime(false, secondsUntilExpiration);
			subpacketVector = subpacketGenerator.generate();
			PGPKeyRingGenerator keyringGenerator = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, masterKeyPair, identity, SymmetricKeyAlgorithmTags.AES_256, password, subpacketVector,
					null, new SecureRandom(), "BC");

			PGPSecretKeyRing secretKeyRing = keyringGenerator.generateSecretKeyRing();

			return secretKeyRing.getSecretKey();
		} catch (PGPException e) {
			if (e.getUnderlyingException() != null && e.getUnderlyingException().getMessage().contains("Illegal key size")) {
				throw new UserError("keystrengthExceedsAllowedJCE", e);
			} else {
				throw new Exception("Cannot create PGP key", e);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new Exception("Cannot create PGP key", e);
		}
	}

	public static byte[] exportPGPSecretKey(PGPSecretKey pgpSecretKey) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		ByteArrayOutputStream secretKeyStream = null;
		OutputStream outputStream = null;
		try {
			boolean armor = true;

			secretKeyStream = new ByteArrayOutputStream();

			outputStream = secretKeyStream;
			if (armor) {
				outputStream = new ArmoredOutputStream(secretKeyStream);
			}

			pgpSecretKey.encode(outputStream);

			outputStream.close();
			outputStream = null;

			return secretKeyStream.toByteArray();
		} catch (Exception e) {
			throw new Exception("Cannot export PGP key", e);
		} finally {
			Utilities.closeQuietly(outputStream);
			Utilities.closeQuietly(secretKeyStream);
		}
	}

	public static byte[] exportPGPPublicKey(PGPPublicKey pgpPublicKey) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		ByteArrayOutputStream secretKeyStream = null;
		OutputStream outputStream = null;
		try {
			boolean armor = true;

			secretKeyStream = new ByteArrayOutputStream();

			outputStream = secretKeyStream;
			if (armor) {
				outputStream = new ArmoredOutputStream(secretKeyStream);
			}

			pgpPublicKey.encode(outputStream);

			outputStream.close();
			outputStream = null;

			return secretKeyStream.toByteArray();
		} catch (Exception e) {
			throw new Exception("Cannot export PGP key", e);
		} finally {
			Utilities.closeQuietly(outputStream);
			Utilities.closeQuietly(secretKeyStream);
		}
	}

	public static PGPPublicKey readPGPPublicKey(InputStream inputStream) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try {
			PGPPublicKeyRingCollection pgpPublicKeyRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(inputStream));
			@SuppressWarnings("rawtypes")
			Iterator keyRingIterator = pgpPublicKeyRingCollection.getKeyRings();
			while (keyRingIterator.hasNext()) {
				PGPPublicKeyRing keyRing = (PGPPublicKeyRing) keyRingIterator.next();
				@SuppressWarnings("rawtypes")
				Iterator publicKeyIterator = keyRing.getPublicKeys();
				while (publicKeyIterator.hasNext()) {
					PGPPublicKey nextPGPPublicKey = (PGPPublicKey) publicKeyIterator.next();
					if (nextPGPPublicKey.isEncryptionKey()) {
						return nextPGPPublicKey;
					}
				}
			}

			// key not found
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	public static PGPSecretKey readPGPSecretKey(InputStream inputStream) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try {
			PGPSecretKeyRingCollection pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(inputStream));
			@SuppressWarnings("rawtypes")
			Iterator keyRingIterator = pgpSecretKeyRingCollection.getKeyRings();
			while (keyRingIterator.hasNext()) {
				PGPSecretKeyRing keyRing = (PGPSecretKeyRing) keyRingIterator.next();
				@SuppressWarnings("rawtypes")
				Iterator privateKeyIterator = keyRing.getSecretKeys();
				while (privateKeyIterator.hasNext()) {
					PGPSecretKey nextPGPPrivateKey = (PGPSecretKey) privateKeyIterator.next();
					if (nextPGPPrivateKey.isMasterKey()) {
						return nextPGPPrivateKey;
					}
				}
			}

			// key not found
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	public static PGPPrivateKey readPGPPrivateKey(PGPSecretKey pgpSecretKey, char[] password) throws Exception {
		try {
			if (pgpSecretKey == null) {
				return null;
			} else {
				Security.addProvider(new BouncyCastleProvider());

				return pgpSecretKey.extractPrivateKey(password, "BC");
			}
		} catch (PGPException e) {
			if (e.getUnderlyingException() != null && e.getUnderlyingException().getMessage().contains("Illegal key size")) {
				throw new UserError("keystrengthExceedsAllowedJCE", e);
			} else {
				throw new UserError("cannotReadPrivateKey", e);
			}
		} catch (Exception e) {
			throw e;
		}
	}

	public static PGPPublicKey readPGPPublicKey(PGPSecretKey pgpSecretKey) throws Exception {
		if (pgpSecretKey == null) {
			return null;
		} else {
			Security.addProvider(new BouncyCastleProvider());

			return pgpSecretKey.getPublicKey();
		}
	}

	public static int getPGPSignatureMethod(String methodName) {
		methodName = methodName.replace("-", "").replace("_", "");
		if ("MD5".equalsIgnoreCase(methodName)) {
			return HashAlgorithmTags.MD5;
		} else if ("RIPEMD160".equalsIgnoreCase(methodName)) {
			return HashAlgorithmTags.RIPEMD160;
		} else if ("DOUBLESHA".equalsIgnoreCase(methodName)) {
			return HashAlgorithmTags.DOUBLE_SHA;
		} else if ("MD2".equalsIgnoreCase(methodName)) {
			return HashAlgorithmTags.MD2;
		} else if ("TIGER192".equalsIgnoreCase(methodName)) {
			return HashAlgorithmTags.TIGER_192;
		} else if ("HAVAL5160".equalsIgnoreCase(methodName)) {
			return HashAlgorithmTags.HAVAL_5_160;
		} else if ("SHA256".equalsIgnoreCase(methodName)) {
			return HashAlgorithmTags.SHA256;
		} else if ("SHA384".equalsIgnoreCase(methodName)) {
			return HashAlgorithmTags.SHA384;
		} else if ("SHA512".equalsIgnoreCase(methodName)) {
			return HashAlgorithmTags.SHA512;
		} else if ("SHA224".equalsIgnoreCase(methodName)) {
			return HashAlgorithmTags.SHA224;
		} else {
			return HashAlgorithmTags.SHA1;
		}
	}
}
