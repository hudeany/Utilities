package de.soderer.utilities.crypto;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.Security;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyPacket;
import org.bouncycastle.bcpg.SignatureSubpacketTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.Features;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.bcpg.sig.NotationData;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPKeyPair;
import org.bouncycastle.openpgp.PGPKeyRingGenerator;
import org.bouncycastle.openpgp.PGPObjectFactory;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPSignature;
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketVector;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.PGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPDigestCalculatorProviderBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyEncryptorBuilder;

import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.TextUtilities;
import de.soderer.utilities.UserError;
import de.soderer.utilities.Utilities;

/**
 * Utilities for OpenPGP keys based on BouncyCastle: key generation, import, export, signing of keys,
 * expiration and password changes.
 */
public class PGPUtilities {
	private PGPUtilities() {
		// Utility class
	}

	/**
	 * Default symmetric encryption method name for PGP encryption.
	 */
	public static final String DEFAULT_SYMMETRIC_ENCRYPTION_METHOD = "AES-256";

	/**
	 * Fermat prime number: F4 = 65537
	 */
	public static final BigInteger FERMAT_PRIME_F4 = BigInteger.valueOf(0x10001);

	/**
	 * PGP symmetric encryption algorithm with its OpenPGP algorithm id.
	 */
	public enum PgpSymmetricEncryptionMethod {
		/**
		 * IDEA
		 */
		IDEA(SymmetricKeyAlgorithmTags.IDEA),
		/**
		 * Triple-DES (DES-EDE, 168 bit key)
		 */
		TRIPLE_DES(SymmetricKeyAlgorithmTags.TRIPLE_DES),
		/**
		 * CAST5 (128 bit key)
		 */
		CAST5(SymmetricKeyAlgorithmTags.CAST5),
		/**
		 * Blowfish (128 bit key)
		 */
		BLOWFISH(SymmetricKeyAlgorithmTags.BLOWFISH),
		/**
		 * SAFER-SK128
		 */
		SAFER(SymmetricKeyAlgorithmTags.SAFER),
		/**
		 * DES (56 bit key, insecure)
		 */
		DES(SymmetricKeyAlgorithmTags.DES),
		/**
		 * AES with 128 bit key
		 */
		AES_128(SymmetricKeyAlgorithmTags.AES_128),
		/**
		 * AES with 192 bit key
		 */
		AES_192(SymmetricKeyAlgorithmTags.AES_192),
		/**
		 * AES with 256 bit key
		 */
		AES_256(SymmetricKeyAlgorithmTags.AES_256),
		/**
		 * Twofish with 256 bit key
		 */
		TWOFISH(SymmetricKeyAlgorithmTags.TWOFISH);

		private final int id;

		PgpSymmetricEncryptionMethod(final int id) {
			this.id = id;
		}

		/**
		 * Returns the OpenPGP algorithm id.
		 *
		 * @return algorithm id
		 */
		public int getId() {
			return id;
		}

		/**
		 * Returns the names of all constants.
		 *
		 * @return constant names in declaration order
		 */
		public static String[] names() {
			final String[] names = new String[PgpSymmetricEncryptionMethod.values().length];
			for (int i = 0; i < PgpSymmetricEncryptionMethod.values().length; i++) {
				names[i] = PgpSymmetricEncryptionMethod.values()[i].name();
			}
			return names;
		}

		/**
		 * Finds the symmetric algorithm by its name, ignoring case, "-" and "_" (e.g. "AES-256" or "aes_256").
		 *
		 * @param methodName name
		 * @return matching constant
		 * @throws Exception if no constant matches the name
		 */
		public static PgpSymmetricEncryptionMethod getByName(final String methodName) throws Exception {
			final String searchMethodName = methodName.replace("-", "").replace("_", "");
			for (final PgpSymmetricEncryptionMethod encryptionMethod : PgpSymmetricEncryptionMethod.values()) {
				if (encryptionMethod.toString().replace("-", "").replace("_", "").equalsIgnoreCase(searchMethodName)) {
					return encryptionMethod;
				}
			}
			throw new Exception("Invalid PgpEncryptionMethod name: " + methodName);
		}

		/**
		 * Finds the symmetric algorithm by its OpenPGP algorithm id.
		 *
		 * @param id OpenPGP algorithm id
		 * @return matching constant
		 * @throws Exception if no constant has the id
		 */
		public static PgpSymmetricEncryptionMethod getById(final int id) throws Exception {
			for (final PgpSymmetricEncryptionMethod encryptionMethod : PgpSymmetricEncryptionMethod.values()) {
				if (encryptionMethod.getId() == id) {
					return encryptionMethod;
				}
			}
			throw new Exception("Invalid PgpEncryptionMethod id: " + id);
		}
	}

	/**
	 * Default public key algorithm name for PGP key generation.
	 */
	public static final String DEFAULT_PUBLIC_ENCRYPTION_METHOD = "RSA-GENERAL";

	/**
	 * PGP public key algorithm with its OpenPGP algorithm id.
	 */
	public enum PgpPublicEncryptionMethod {
		/**
		 * RSA (encrypt or sign)
		 */
		RSA_GENERAL(PublicKeyAlgorithmTags.RSA_GENERAL),

		/**
		 * ElGamal (encrypt only)
		 */
		ELGAMAL_ENCRYPT(PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT),

		/**
		 * DSA (sign only)
		 */
		DSA(PublicKeyAlgorithmTags.DSA),
		/**
		 * Elliptic curve (deprecated algorithm id, use ECDH)
		 */
		@SuppressWarnings("deprecation")
		EC(PublicKeyAlgorithmTags.EC),
		/**
		 * Elliptic curve Diffie-Hellman (encrypt)
		 */
		ECDH(PublicKeyAlgorithmTags.ECDH),
		/**
		 * Elliptic curve DSA (sign)
		 */
		ECDSA(PublicKeyAlgorithmTags.ECDSA),
		/**
		 * Diffie-Hellman (X9.42)
		 */
		DIFFIE_HELLMAN(PublicKeyAlgorithmTags.DIFFIE_HELLMAN);

		private final int id;

		PgpPublicEncryptionMethod(final int id) {
			this.id = id;
		}

		/**
		 * Returns the OpenPGP algorithm id.
		 *
		 * @return algorithm id
		 */
		public int getId() {
			return id;
		}

		/**
		 * Returns the names of all constants.
		 *
		 * @return constant names in declaration order
		 */
		public static String[] names() {
			final String[] names = new String[PgpPublicEncryptionMethod.values().length];
			for (int i = 0; i < PgpPublicEncryptionMethod.values().length; i++) {
				names[i] = PgpPublicEncryptionMethod.values()[i].name();
			}
			return names;
		}

		/**
		 * Finds the public key algorithm by its name, ignoring case, "-" and "_" (e.g. "AES-256" or "aes_256"). Additionally "RSA" and "ELGAMAL" are accepted.
		 *
		 * @param methodName name
		 * @return matching constant
		 * @throws Exception if no constant matches the name
		 */
		public static PgpPublicEncryptionMethod getByName(final String methodName) throws Exception {
			final String searchMethodName = methodName.trim().replace("-", "").replace("_", "");
			for (final PgpPublicEncryptionMethod encryptionMethod : PgpPublicEncryptionMethod.values()) {
				if (encryptionMethod.toString().replace("-", "").replace("_", "").equalsIgnoreCase(searchMethodName)) {
					return encryptionMethod;
				}
			}
			if (searchMethodName.equalsIgnoreCase("RSA")) {
				return RSA_GENERAL;
			} else if (searchMethodName.equalsIgnoreCase("ELGAMAL")) {
				return ELGAMAL_ENCRYPT;
			}
			throw new Exception("Invalid PgpPublicEncryptionMethod name: " + methodName);
		}

		/**
		 * Finds the public key algorithm by its OpenPGP algorithm id.
		 *
		 * @param id OpenPGP algorithm id
		 * @return matching constant
		 * @throws Exception if no constant has the id
		 */
		public static PgpPublicEncryptionMethod getById(final int id) throws Exception {
			for (final PgpPublicEncryptionMethod encryptionMethod : PgpPublicEncryptionMethod.values()) {
				if (encryptionMethod.getId() == id) {
					return encryptionMethod;
				}
			}
			throw new Exception("Invalid PgpPublicEncryptionMethod id: " + id);
		}
	}

	/**
	 * Default hashing method name for PGP signatures.
	 */
	public static final String DEFAULT_HASHING_METHOD = "SHA256";

	/**
	 * PGP hash algorithm with its OpenPGP algorithm id.
	 */
	public enum PgpHashMethod {
		/**
		 * MD5 (insecure)
		 */
		MD5(HashAlgorithmTags.MD5),
		/**
		 * SHA-1 (insecure for signatures)
		 */
		SHA1(HashAlgorithmTags.SHA1),
		/**
		 * RIPEMD-160
		 */
		RIPEMD160(HashAlgorithmTags.RIPEMD160),
		/**
		 * Double width SHA (experimental)
		 */
		DOUBLE_SHA(HashAlgorithmTags.DOUBLE_SHA),
		/**
		 * MD2 (insecure)
		 */
		MD2(HashAlgorithmTags.MD2),
		/**
		 * Tiger 192
		 */
		TIGER_192(HashAlgorithmTags.TIGER_192),
		/**
		 * HAVAL (5 pass, 160 bit)
		 */
		HAVAL_5_160(HashAlgorithmTags.HAVAL_5_160),
		/**
		 * SHA-256
		 */
		SHA256(HashAlgorithmTags.SHA256),
		/**
		 * SHA-384
		 */
		SHA384(HashAlgorithmTags.SHA384),
		/**
		 * SHA-512
		 */
		SHA512(HashAlgorithmTags.SHA512),
		/**
		 * SHA-224
		 */
		SHA224(HashAlgorithmTags.SHA224);

		private final int id;

		PgpHashMethod(final int id) {
			this.id = id;
		}

		/**
		 * Returns the OpenPGP algorithm id.
		 *
		 * @return algorithm id
		 */
		public int getId() {
			return id;
		}

		/**
		 * Returns the names of all constants.
		 *
		 * @return constant names in declaration order
		 */
		public static String[] names() {
			final String[] names = new String[PgpHashMethod.values().length];
			for (int i = 0; i < PgpHashMethod.values().length; i++) {
				names[i] = PgpHashMethod.values()[i].name();
			}
			return names;
		}

		/**
		 * Finds the hash algorithm by its name, ignoring case, "-" and "_" (e.g. "AES-256" or "aes_256").
		 *
		 * @param methodName name
		 * @return matching constant
		 * @throws Exception if no constant matches the name
		 */
		public static PgpHashMethod getByName(final String methodName) throws Exception {
			final String searchMethodName = methodName.replace("-", "").replace("_", "");
			for (final PgpHashMethod encryptionMethod : PgpHashMethod.values()) {
				if (encryptionMethod.toString().replace("-", "").replace("_", "").equalsIgnoreCase(searchMethodName)) {
					return encryptionMethod;
				}
			}
			throw new Exception("Invalid PgpHashMethod name: " + methodName);
		}

		/**
		 * Finds the hash algorithm by its OpenPGP algorithm id.
		 *
		 * @param id OpenPGP algorithm id
		 * @return matching constant
		 * @throws Exception if no constant has the id
		 */
		public static PgpHashMethod getById(final int id) throws Exception {
			for (final PgpHashMethod encryptionMethod : PgpHashMethod.values()) {
				if (encryptionMethod.getId() == id) {
					return encryptionMethod;
				}
			}
			throw new Exception("Invalid PgpHashMethod id: " + id);
		}
	}

	/**
	 * Generates a new PGP key ring and returns its last secret key.<br>
	 * For RSA and DSA this is the master key. For ElGamal a DSA master key with an ElGamal encryption subkey is created
	 * and the ElGamal subkey is returned (it cannot be used for signatures).<br>
	 * The secret key is protected with AES-256 and the given password.
	 *
	 * @param keyStrength key size in bits
	 * @param keyType RSA_GENERAL, DSA or ELGAMAL_ENCRYPT
	 * @param identity user ID of the key, e.g. "Name &lt;mail@example.com&gt;"
	 * @param password password to protect the secret key
	 * @param validity local date and time when the key expires (must not be null)
	 * @return generated secret key
	 * @throws Exception if the key type is not supported or the key cannot be generated
	 */
	public static PGPSecretKey generatePGPSecretKey(final int keyStrength, final PgpPublicEncryptionMethod keyType, final String identity, final char[] password, final LocalDateTime validity) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final PGPKeyPair masterKeyPair;
		PGPKeyPair subKeyPair = null;
		if (keyType == PgpPublicEncryptionMethod.RSA_GENERAL) {
			masterKeyPair = new JcaPGPKeyPair(PublicKeyPacket.VERSION_4, keyType.getId(), CryptographicUtilities.generateRsaKeyPair(keyStrength), DateUtilities.getDateForLocalDateTime(LocalDateTime.now()));
		} else if (keyType == PgpPublicEncryptionMethod.ELGAMAL_ENCRYPT) {
			masterKeyPair = new JcaPGPKeyPair(PublicKeyPacket.VERSION_4, PublicKeyAlgorithmTags.DSA, CryptographicUtilities.generateDsaKeyPair(keyStrength), DateUtilities.getDateForLocalDateTime(LocalDateTime.now()));
			subKeyPair = new JcaPGPKeyPair(PublicKeyPacket.VERSION_4, PgpPublicEncryptionMethod.ELGAMAL_ENCRYPT.getId(), CryptographicUtilities.generateElGamalKeyPair(keyStrength), DateUtilities.getDateForLocalDateTime(LocalDateTime.now()));
		} else if (keyType == PgpPublicEncryptionMethod.DSA) {
			masterKeyPair = new JcaPGPKeyPair(PublicKeyPacket.VERSION_4, keyType.getId(), CryptographicUtilities.generateDsaKeyPair(keyStrength), DateUtilities.getDateForLocalDateTime(LocalDateTime.now()));
		} else {
			throw new Exception("Unsupported keytype: " + keyType.name());
		}

		final PGPSignatureSubpacketGenerator subpacketGenerator = new PGPSignatureSubpacketGenerator();

		// Add signed metadata on the signature.
		// Declare its purpose
		subpacketGenerator.setKeyFlags(false, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER | KeyFlags.ENCRYPT_COMMS | KeyFlags.ENCRYPT_STORAGE);
		// Set preferences for secondary crypto algorithms to use when sending messages to this key.
		subpacketGenerator.setPreferredSymmetricAlgorithms
		(false, new int[] {
				SymmetricKeyAlgorithmTags.AES_256,
				SymmetricKeyAlgorithmTags.AES_192,
				SymmetricKeyAlgorithmTags.AES_128
		});
		subpacketGenerator.setPreferredHashAlgorithms
		(false, new int[] {
				HashAlgorithmTags.SHA256,
				HashAlgorithmTags.SHA1,
				HashAlgorithmTags.SHA384,
				HashAlgorithmTags.SHA512,
				HashAlgorithmTags.SHA224,
		});
		// Request senders add additional checksums to the message (useful when verifying unsigned messages.)
		subpacketGenerator.setFeature(false, Features.FEATURE_MODIFICATION_DETECTION);

		final long secondsUntilExpiration = Duration.between(LocalDateTime.now(), validity).toSeconds() + 1;
		subpacketGenerator.setKeyExpirationTime(true, secondsUntilExpiration);

		subpacketGenerator.addNotationData(false, true, "identity", identity);

		// Objects used to encrypt the secret key
		final JcaPGPDigestCalculatorProviderBuilder builder = new JcaPGPDigestCalculatorProviderBuilder();
		final PGPDigestCalculatorProvider pgpDigestCalculatorProvider = builder.build();

		final PGPDigestCalculator sha1Calc = pgpDigestCalculatorProvider.get(HashAlgorithmTags.SHA1);
		final PGPDigestCalculator sha256Calc = pgpDigestCalculatorProvider.get(HashAlgorithmTags.SHA256);

		final PBESecretKeyEncryptor pbeSecretKeyEncryptor = (new JcePBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256, sha256Calc, 0xc0)).build(password);

		final PGPKeyRingGenerator pgpKeyRingGenerator = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, masterKeyPair, identity, sha1Calc, subpacketGenerator.generate(), null, new JcaPGPContentSignerBuilder(masterKeyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1), pbeSecretKeyEncryptor);
		if (subKeyPair != null) {
			pgpKeyRingGenerator.addSubKey(subKeyPair);
		}

		final PGPSecretKeyRing pgpSecretKeyRing = pgpKeyRingGenerator.generateSecretKeyRing();

		final Iterator<PGPSecretKey> secretKeys = pgpSecretKeyRing.getSecretKeys();
		PGPSecretKey secretKey = null;
		while (secretKeys.hasNext()) {
			// if available only export the subkey. For ElGamal this is the wanted key, but it cannot be used for signatures
			secretKey = secretKeys.next();
		}
		return secretKey;
	}

	/**
	 * Generates a new PGP key ring with a RSA signing master key and a RSA encryption subkey and returns the
	 * secret key of the encryption subkey.
	 *
	 * @param keyStrength RSA key size in bits
	 * @param identity user ID of the key
	 * @param password password to protect the secret keys
	 * @param validity local date and time when the master key expires (must not be null)
	 * @return secret key of the encryption subkey
	 * @throws Exception if the keys cannot be generated
	 */
	public static PGPSecretKey generateDualKeyPGPSecretKey(final int keyStrength, final String identity, final char[] password, final LocalDateTime validity) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final KeyPair keyPair1 = CryptographicUtilities.generateRsaKeyPair(keyStrength);
		final KeyPair keyPair2 = CryptographicUtilities.generateRsaKeyPair(keyStrength);

		// First create the master (signing) key with the generator.
		final PGPKeyPair rsakp_sign = new JcaPGPKeyPair(PublicKeyPacket.VERSION_4, PublicKeyAlgorithmTags.RSA_GENERAL, keyPair1, DateUtilities.getDateForLocalDateTime(LocalDateTime.now()));
		// Then an encryption subkey.
		final PGPKeyPair rsakp_enc = new JcaPGPKeyPair(PublicKeyPacket.VERSION_4, PublicKeyAlgorithmTags.RSA_GENERAL, keyPair2, DateUtilities.getDateForLocalDateTime(LocalDateTime.now()));

		// Add a self-signature on the id
		final PGPSignatureSubpacketGenerator subpacketGenerator = new PGPSignatureSubpacketGenerator();

		// Add signed metadata on the signature.
		// 1) Declare its purpose
		subpacketGenerator.setKeyFlags(false, KeyFlags.SIGN_DATA | KeyFlags.CERTIFY_OTHER);
		// 2) Set preferences for secondary crypto algorithms to use when sending messages to this key.
		subpacketGenerator.setPreferredSymmetricAlgorithms
		(false, new int[] {
				SymmetricKeyAlgorithmTags.AES_256,
				SymmetricKeyAlgorithmTags.AES_192,
				SymmetricKeyAlgorithmTags.AES_128
		});
		subpacketGenerator.setPreferredHashAlgorithms
		(false, new int[] {
				HashAlgorithmTags.SHA256,
				HashAlgorithmTags.SHA1,
				HashAlgorithmTags.SHA384,
				HashAlgorithmTags.SHA512,
				HashAlgorithmTags.SHA224,
		});
		// 3) Request senders add additional checksums to the message (useful when verifying unsigned messages.)
		subpacketGenerator.setFeature(false, Features.FEATURE_MODIFICATION_DETECTION);

		final long secondsUntilExpiration = Duration.between(LocalDateTime.now(), validity).toSeconds() + 1;
		subpacketGenerator.setKeyExpirationTime(true, secondsUntilExpiration);

		subpacketGenerator.addNotationData(true, true, "identity", identity);

		// Create a signature on the encryption subkey
		final PGPSignatureSubpacketGenerator enchashgen = new PGPSignatureSubpacketGenerator();
		// Add metadata to declare its purpose
		enchashgen.setKeyFlags(false, KeyFlags.ENCRYPT_COMMS|KeyFlags.ENCRYPT_STORAGE);

		// Objects used to encrypt the secret key
		final JcaPGPDigestCalculatorProviderBuilder builder = new JcaPGPDigestCalculatorProviderBuilder();
		final PGPDigestCalculatorProvider pgpDigestCalculatorProvider = builder.build();
		final PGPDigestCalculator sha1Calc = pgpDigestCalculatorProvider.get(HashAlgorithmTags.SHA1);
		final PGPDigestCalculator sha256Calc = pgpDigestCalculatorProvider.get(HashAlgorithmTags.SHA256);

		// bcpg 1.48 exposes this API that includes s2kcount. Earlier versions use a default of 0x60.
		final PBESecretKeyEncryptor pske = (new JcePBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256, sha256Calc, 0xc0)).build(password);

		// Finally, create the keyring itself. The constructor takes parameters that allow it to generate the self signature.
		final PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, rsakp_sign, identity, sha1Calc, subpacketGenerator.generate(), null, new JcaPGPContentSignerBuilder(rsakp_sign.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1), pske);

		// Add our encryption subkey, together with its signature.
		keyRingGen.addSubKey(rsakp_enc, enchashgen.generate(), null);

		final PGPSecretKeyRing skr = keyRingGen.generateSecretKeyRing();

		final Iterator<PGPSecretKey> secretKeys = skr.getSecretKeys();
		PGPSecretKey secretKey = null;
		while (secretKeys.hasNext()) {
			secretKey = secretKeys.next();
		}
		return secretKey;
	}

	/**
	 * Exports a single secret key ASCII-armored.
	 *
	 * @param pgpSecretKey secret key
	 * @return ASCII-armored key data
	 * @throws Exception if the key cannot be encoded
	 */
	public static byte[] exportPGPSecretKey(final PGPSecretKey pgpSecretKey) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try (ByteArrayOutputStream secretKeyStream = new ByteArrayOutputStream()) {
			try (OutputStream outputStream = new ArmoredOutputStream(secretKeyStream)) {
				pgpSecretKey.encode(outputStream);
			}
			return secretKeyStream.toByteArray();
		} catch (final Exception e) {
			throw new Exception("Cannot export PGP key", e);
		}
	}

	/**
	 * Exports a single public key ASCII-armored.
	 *
	 * @param pgpPublicKey public key
	 * @return ASCII-armored key data
	 * @throws Exception if the key cannot be encoded
	 */
	public static byte[] exportPGPPublicKey(final PGPPublicKey pgpPublicKey) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try (ByteArrayOutputStream secretKeyStream = new ByteArrayOutputStream()) {
			try (OutputStream outputStream = new ArmoredOutputStream(secretKeyStream)) {
				pgpPublicKey.encode(outputStream);
			}
			return secretKeyStream.toByteArray();
		} catch (final Exception e) {
			throw new Exception("Cannot export PGP key", e);
		}
	}

	/**
	 * Reads the first master public key from ASCII-armored or binary key data. The stream is closed.
	 *
	 * @param inputStream key data
	 * @return the first master public key, or null if none was found or the data cannot be read
	 */
	public static PGPPublicKey readPGPPublicKey(final InputStream inputStream) {
		Security.addProvider(new BouncyCastleProvider());

		try (InputStream decoderStream = PGPUtil.getDecoderStream(new PGPKeyStorageStringSplitter(inputStream).getPublicKeyInputStream())) {
			final PGPPublicKeyRingCollection pgpPublicKeyRingCollection = new PGPPublicKeyRingCollection(decoderStream, new JcaKeyFingerprintCalculator());
			@SuppressWarnings("rawtypes")
			final
			Iterator keyRingIterator = pgpPublicKeyRingCollection.getKeyRings();
			while (keyRingIterator.hasNext()) {
				final PGPPublicKeyRing keyRing = (PGPPublicKeyRing) keyRingIterator.next();
				for (final PGPPublicKey publicKey : keyRing) {
					if (publicKey.isMasterKey()) {
						return publicKey;
					}
				}
			}

			// key not found
			return null;
		} catch (@SuppressWarnings("unused") final Exception e) {
			return null;
		}
	}

	/**
	 * Reads the first master secret key from ASCII-armored or binary key data. The stream is closed.
	 *
	 * @param inputStream key data
	 * @return the first master secret key, or null if none was found or the data cannot be read
	 */
	public static PGPSecretKey readPGPSecretKey(final InputStream inputStream) {
		Security.addProvider(new BouncyCastleProvider());

		try (InputStream decoderStream = PGPUtil.getDecoderStream(new PGPKeyStorageStringSplitter(inputStream).getPrivateKeyInputStream())) {
			final PGPSecretKeyRingCollection pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(decoderStream, new JcaKeyFingerprintCalculator());
			@SuppressWarnings("rawtypes")
			final
			Iterator keyRingIterator = pgpSecretKeyRingCollection.getKeyRings();
			while (keyRingIterator.hasNext()) {
				final PGPSecretKeyRing keyRing = (PGPSecretKeyRing) keyRingIterator.next();
				@SuppressWarnings("rawtypes")
				final
				Iterator privateKeyIterator = keyRing.getSecretKeys();
				while (privateKeyIterator.hasNext()) {
					final PGPSecretKey nextPGPSecretKey = (PGPSecretKey) privateKeyIterator.next();
					if (nextPGPSecretKey.isMasterKey()) {
						return nextPGPSecretKey;
					}
				}
			}

			// key not found
			return null;
		} catch (@SuppressWarnings("unused") final Exception e) {
			return null;
		}
	}

	/**
	 * Container for the public and secret key rings read by {@link PGPUtilities#readKeyRings(InputStream)}.
	 */
	public static class PGPKeyRings {
		/**
		 * Public key rings in the order of the data.
		 */
		public final List<PGPPublicKeyRing> publicKeyRings = new ArrayList<>();
		/**
		 * Secret key rings in the order of the data.
		 */
		public final List<PGPSecretKeyRing> secretKeyRings = new ArrayList<>();

		/**
		 * Creates an empty container.
		 */
		public PGPKeyRings() {
			// Lists are filled by the reader
		}
	}

	/**
	 * Reads all public and secret key rings from ASCII-armored or binary key data. Other PGP objects are ignored. The stream is closed.
	 *
	 * @param inputStream key data
	 * @return key rings found in the data
	 * @throws Exception if the data cannot be read
	 */
	public static PGPKeyRings readKeyRings(final InputStream inputStream) throws Exception {
		final PGPKeyRings result = new PGPKeyRings();
		try (InputStream decoder = PGPUtil.getDecoderStream(inputStream)) {
			final PGPObjectFactory factory = new PGPObjectFactory(decoder, new JcaKeyFingerprintCalculator());
			Object nextObject;
			while ((nextObject = factory.nextObject()) != null) {
				if (nextObject instanceof PGPSecretKeyRing) {
					result.secretKeyRings.add((PGPSecretKeyRing) nextObject);
				} else if (nextObject instanceof PGPPublicKeyRing) {
					result.publicKeyRings.add((PGPPublicKeyRing) nextObject);
				}
			}
		}
		return result;
	}

	/**
	 * Decrypts the private key of a secret key.
	 *
	 * @param secretKey secret key, may be null
	 * @param password password of the secret key
	 * @return private key, or null if the secret key is null
	 * @throws Exception ({@link UserError}) if the password is wrong or the key strength is not allowed by the JCE
	 */
	public static PGPPrivateKey readPGPPrivateKey(final PGPSecretKey secretKey, final char[] password) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try {
			if (secretKey == null) {
				return null;
			} else {
				final PBESecretKeyDecryptor decryptor = new JcePBESecretKeyDecryptorBuilder().build(password);
				return secretKey.extractPrivateKey(decryptor);
			}
		} catch (final PGPException e) {
			if (e.getUnderlyingException() != null && e.getUnderlyingException().getMessage() != null && e.getUnderlyingException().getMessage().contains("Illegal key size")) {
				throw new UserError("keystrengthExceedsAllowedJCE", e);
			} else {
				throw new UserError("cannotReadPrivateKey", e);
			}
		} catch (final Exception e) {
			throw new UserError("cannotReadPrivateKey", e);
		}
	}

	/**
	 * Returns the public key of a secret key.
	 *
	 * @param pgpSecretKey secret key, may be null
	 * @return public key, or null if the secret key is null
	 */
	public static PGPPublicKey readPGPPublicKey(final PGPSecretKey pgpSecretKey) {
		Security.addProvider(new BouncyCastleProvider());

		if (pgpSecretKey == null) {
			return null;
		} else {
			return pgpSecretKey.getPublicKey();
		}
	}

	/**
	 * Returns the validity in seconds after the key creation time, as defined by the latest self certification
	 * (for master keys) or subkey binding signature (for subkeys).
	 *
	 * @param key public key
	 * @return validity in seconds, 0 means unlimited validity
	 */
	public static long getPgpPublicKeyValiditySeconds(final PGPPublicKey key) {
		PGPSignature latestSignature = null;

		final Iterator<PGPSignature> signatures = key.getSignatures();

		while (signatures.hasNext()) {
			final PGPSignature signature = signatures.next();

			if (key.isMasterKey()) {
				final int type = signature.getSignatureType();

				if (type == PGPSignature.POSITIVE_CERTIFICATION || type == PGPSignature.DEFAULT_CERTIFICATION
						|| type == PGPSignature.CASUAL_CERTIFICATION || type == PGPSignature.NO_CERTIFICATION) {
					if (signature.getKeyID() != key.getKeyID()) {
						continue;
					} else if (latestSignature == null || signature.getCreationTime().after(latestSignature.getCreationTime())) {
						latestSignature = signature;
					}
				}
			} else {
				// A subkey binding signature is issued by the master key, so its key ID is the master key's ID, not the subkey's ID
				if (signature.getSignatureType() == PGPSignature.SUBKEY_BINDING) {
					if (latestSignature == null || signature.getCreationTime().after(latestSignature.getCreationTime())) {
						latestSignature = signature;
					}
				}
			}
		}

		if (latestSignature == null) {
			return 0;
		} else {
			final PGPSignatureSubpacketVector hashed = latestSignature.getHashedSubPackets();
			if (hashed == null || hashed.getKeyExpirationTime() < 0) {
				return 0;
			} else {
				return hashed.getKeyExpirationTime();
			}
		}
	}

	/**
	 * Certifies another public key with a secret key (direct key signature), adding the signer's identity as notation "identity".
	 *
	 * @param pgpSecretKey secret key of the signer
	 * @param password password of the signer's secret key
	 * @param publicKeyToBeSigned public key to certify
	 * @return copy of the public key including the new certification
	 * @throws Exception if the private key cannot be read or the signature cannot be created
	 */
	public static PGPPublicKey signPublicKey(final PGPSecretKey pgpSecretKey, final char[] password, final PGPPublicKey publicKeyToBeSigned) throws Exception {
		return signPublicKey(pgpSecretKey, password, publicKeyToBeSigned, null);
	}

	/**
	 * Certifies another public key with a secret key (direct key signature).
	 *
	 * @param pgpSecretKey secret key of the signer
	 * @param password password of the signer's secret key
	 * @param publicKeyToBeSigned public key to certify
	 * @param notationData notations to add to the signature; null adds the signer's identity as notation "identity", an empty map adds none
	 * @return copy of the public key including the new certification
	 * @throws Exception if the private key cannot be read or the signature cannot be created
	 */
	public static PGPPublicKey signPublicKey(final PGPSecretKey pgpSecretKey, final char[] password, final PGPPublicKey publicKeyToBeSigned, final Map<String, String> notationData) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final PGPPrivateKey pgpPrivKey = readPGPPrivateKey(pgpSecretKey, password);
		// The content signer must use the algorithm of the signing key (e.g. DSA), not always RSA
		final PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(pgpSecretKey.getPublicKey().getAlgorithm(), PgpHashMethod.getByName(DEFAULT_HASHING_METHOD).getId()), pgpSecretKey.getPublicKey());
		signatureGenerator.init(PGPSignature.DIRECT_KEY, pgpPrivKey);

		if (notationData == null) {
			final PGPSignatureSubpacketGenerator pgpSignatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
			pgpSignatureSubpacketGenerator.addNotationData(true, true, "identity", PGPUtilities.getIdentityFromPgpPublicKey(pgpSecretKey.getPublicKey()));
			final PGPSignatureSubpacketVector pgpSignatureSubpacketVector = pgpSignatureSubpacketGenerator.generate();
			signatureGenerator.setHashedSubpackets(pgpSignatureSubpacketVector);
		} else if (!notationData.isEmpty()) {
			final PGPSignatureSubpacketGenerator pgpSignatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
			for (final Entry<String, String> notation : notationData.entrySet()) {
				pgpSignatureSubpacketGenerator.addNotationData(true, true, notation.getKey(), notation.getValue());
				final PGPSignatureSubpacketVector pgpSignatureSubpacketVector = pgpSignatureSubpacketGenerator.generate();
				signatureGenerator.setHashedSubpackets(pgpSignatureSubpacketVector);
			}
		}

		final PGPSignature certification = signatureGenerator.generateCertification(publicKeyToBeSigned);
		return PGPPublicKey.addCertification(publicKeyToBeSigned, certification);
	}

	/**
	 * Certifies another public key with a private key (direct key signature), adding the signer's identity as notation "identity".
	 *
	 * @param pgpPrivKey private key of the signer
	 * @param pgpPublicKey public key of the signer
	 * @param publicKeyToBeSigned public key to certify
	 * @return copy of the public key including the new certification
	 * @throws Exception if the signature cannot be created
	 */
	public static PGPPublicKey signPublicKey(final PGPPrivateKey pgpPrivKey, final PGPPublicKey pgpPublicKey, final PGPPublicKey publicKeyToBeSigned) throws Exception {
		return signPublicKey(pgpPrivKey, pgpPublicKey, publicKeyToBeSigned, null);
	}

	/**
	 * Certifies another public key with a private key (direct key signature).
	 *
	 * @param pgpPrivKey private key of the signer
	 * @param pgpPublicKey public key of the signer
	 * @param publicKeyToBeSigned public key to certify
	 * @param notationData notations to add to the signature; null adds the signer's identity as notation "identity", an empty map adds none
	 * @return copy of the public key including the new certification
	 * @throws Exception if the signature cannot be created
	 */
	public static PGPPublicKey signPublicKey(final PGPPrivateKey pgpPrivKey, final PGPPublicKey pgpPublicKey, final PGPPublicKey publicKeyToBeSigned, final Map<String, String> notationData) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		// The content signer must use the algorithm of the signing key (e.g. DSA), not always RSA
		final PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(pgpPublicKey.getAlgorithm(), PgpHashMethod.getByName(DEFAULT_HASHING_METHOD).getId()), pgpPublicKey);
		signatureGenerator.init(PGPSignature.DIRECT_KEY, pgpPrivKey);

		if (notationData == null) {
			final PGPSignatureSubpacketGenerator pgpSignatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
			pgpSignatureSubpacketGenerator.addNotationData(true, true, "identity", PGPUtilities.getIdentityFromPgpPublicKey(pgpPublicKey));
			final PGPSignatureSubpacketVector pgpSignatureSubpacketVector = pgpSignatureSubpacketGenerator.generate();
			signatureGenerator.setHashedSubpackets(pgpSignatureSubpacketVector);
		} else if (!notationData.isEmpty()) {
			final PGPSignatureSubpacketGenerator pgpSignatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
			for (final Entry<String, String> notation : notationData.entrySet()) {
				pgpSignatureSubpacketGenerator.addNotationData(true, true, notation.getKey(), notation.getValue());
				final PGPSignatureSubpacketVector pgpSignatureSubpacketVector = pgpSignatureSubpacketGenerator.generate();
				signatureGenerator.setHashedSubpackets(pgpSignatureSubpacketVector);
			}
		}

		final PGPSignature certification = signatureGenerator.generateCertification(publicKeyToBeSigned);
		return PGPPublicKey.addCertification(publicKeyToBeSigned, certification);
	}

	/**
	 * Returns all user IDs of a public key.
	 *
	 * @param publicKey public key
	 * @return user IDs separated by ", ", empty if there are none
	 */
	public static String getIdentityFromPgpPublicKey(final PGPPublicKey publicKey) {
		Security.addProvider(new BouncyCastleProvider());

		final StringBuilder identities = new StringBuilder();
		final Iterator<String> userIdentities = publicKey.getUserIDs();
		while (userIdentities.hasNext()) {
			if (identities.length() > 0) {
				identities.append(", ");
			}
			identities.append(userIdentities.next());
		}
		return identities.toString();
	}

	/**
	 * Returns information about all signatures of a public key. Each map contains the keys "id" (Long key ID of the signer),
	 * "creationtime" (LocalDateTime), "encryptionalgorithm", "hashingalgorithm" and "identity" (from the notation data,
	 * with "(Selfsigned)" for self signatures).
	 *
	 * @param publicKey public key
	 * @return one map per signature
	 */
	public static List<Map<String, Object>> getKeySigners(final PGPPublicKey publicKey) {
		Security.addProvider(new BouncyCastleProvider());

		final List<Map<String, Object>> signersData = new ArrayList<>();
		final Iterator<?> signerIterator = publicKey.getSignatures();
		while (signerIterator.hasNext()) {
			final Map<String, Object> signatureData = new HashMap<>();
			final PGPSignature signature = (PGPSignature) signerIterator.next();

			final long id = signature.getKeyID();
			signatureData.put("id", id);

			// time = signature.getHashedSubPackets().getSignatureCreationTime();
			signatureData.put("creationtime", DateUtilities.getLocalDateTimeForDate(signature.getCreationTime()));

			String encryptionAlgorithm;
			try {
				encryptionAlgorithm = PgpPublicEncryptionMethod.getById(signature.getKeyAlgorithm()).name();
			} catch (@SuppressWarnings("unused") final Exception e) {
				encryptionAlgorithm = LangResources.get("Unknown");
			}
			signatureData.put("encryptionalgorithm", encryptionAlgorithm);

			String hashingAlgorithm;
			try {
				hashingAlgorithm = PgpHashMethod.getById(signature.getHashAlgorithm()).name();
			} catch (@SuppressWarnings("unused") final Exception e) {
				hashingAlgorithm = LangResources.get("Unknown");
			}
			signatureData.put("hashingalgorithm", hashingAlgorithm);

			String identity = "";
			final NotationData[] notationData = signature.getHashedSubPackets() == null ? new NotationData[0] : signature.getHashedSubPackets().getNotationDataOccurrences();
			for (int i = 0; i < notationData.length; i++) {
				if (identity.length() > 0) {
					identity += ", ";
				}
				if (notationData.length > 1 || Utilities.isBlank(notationData[i].getNotationValue())) {
					identity += notationData[i].getNotationName() + ": ";
				}
				identity += notationData[i].getNotationValue();
			}
			if (id == publicKey.getKeyID()) {
				if (Utilities.isNotBlank(identity)) {
					identity += " ";
				}
				identity += "(Selfsigned)";
			}
			if (Utilities.isBlank(identity)) {
				identity = LangResources.get("Unknown");
			}

			signatureData.put("identity", identity);

			signersData.add(signatureData);
		}

		return signersData;
	}

	/**
	 * Returns the key IDs of all signers of a public key.
	 *
	 * @param publicKey public key
	 * @return key IDs as upper case hex strings without leading zeros
	 */
	public static List<String> getSignerKeyIDs(final PGPPublicKey publicKey) {
		Security.addProvider(new BouncyCastleProvider());

		final List<String> signatureIDs = new ArrayList<>();
		final Iterator<?> signerIterator = publicKey.getSignatures();
		while (signerIterator.hasNext()) {
			final PGPSignature signature = (PGPSignature) signerIterator.next();
			final long id = signature.getKeyID();
			signatureIDs.add(Long.toHexString(id).toUpperCase(Locale.ROOT));
		}
		return signatureIDs;
	}

	/**
	 * Removes the "Version: " header lines from ASCII-armored data.
	 *
	 * @param armoredData ASCII-armored data
	 * @return data without version header
	 */
	public static String removeVersionFromArmoredData(final String armoredData) {
		return TextUtilities.removeLinesContainingText(armoredData, "Version: ");
	}

	/**
	 * Sets a new expiration time for a key by adding a new self certification of its first user ID, see
	 * {@link #updateKeyExpiration(PGPSecretKey, PGPPrivateKey, LocalDateTime)}.
	 *
	 * @param secretKey secret key to update
	 * @param password password of the secret key
	 * @param validity local date and time when the key expires, null for unlimited validity
	 * @return updated secret key
	 * @throws Exception if the private key cannot be read or the signature cannot be created
	 */
	public static PGPSecretKey updateKeyExpiration(final PGPSecretKey secretKey, final char[] password, final LocalDateTime validity) throws Exception {
		final PGPPrivateKey privateKey = readPGPPrivateKey(secretKey, password);
		return updateKeyExpiration(secretKey, privateKey, validity);
	}

	/**
	 * Sets a new expiration time for a key by adding a new self certification of its first user ID.
	 * Existing self certifications with an expiration time are removed.
	 *
	 * @param secretKey secret key to update
	 * @param privateKey decrypted private key of the secret key
	 * @param validity local date and time when the key expires, null for unlimited validity
	 * @return updated secret key
	 * @throws Exception if the key has no user ID or the signature cannot be created
	 */
	public static PGPSecretKey updateKeyExpiration(final PGPSecretKey secretKey, final PGPPrivateKey privateKey, final LocalDateTime validity) throws Exception {
		final PGPPublicKey publicKey = secretKey.getPublicKey();

		final long secondsUntilExpiration;
		if (validity == null) {
			secondsUntilExpiration = 0;
		} else {
			final LocalDateTime creationLocalDateTime = DateUtilities.getLocalDateTimeForDate(publicKey.getCreationTime());
			secondsUntilExpiration = Duration.between(creationLocalDateTime, validity).toSeconds();
		}

		final Iterator<String> userIds = publicKey.getUserIDs();
		if (!userIds.hasNext()) {
			throw new IllegalStateException("Mandatory User-ID of PGP key is missing");
		}
		final String userId = userIds.next();

		PGPPublicKey cleanedPublicKey = publicKey;
		final Iterator<PGPSignature> signaturesIterator = cleanedPublicKey.getSignaturesForID(userId);
		while (signaturesIterator.hasNext()) {
			final PGPSignature sig = signaturesIterator.next();
			if (sig.getKeyID() == publicKey.getKeyID()) {
				final boolean hasExpiration = sig.getHashedSubPackets() != null && sig.getHashedSubPackets().getKeyExpirationTime() > 0;
				if (hasExpiration) {
					cleanedPublicKey = PGPPublicKey.removeCertification(cleanedPublicKey, userId, sig);
				}
			}
		}

		final PGPSignatureSubpacketGenerator subpacketGenerator = new PGPSignatureSubpacketGenerator();
		subpacketGenerator.setKeyExpirationTime(false, secondsUntilExpiration);
		subpacketGenerator.setSignatureCreationTime(false, new Date());
		subpacketGenerator.addNotationData(false, true, "identity", userId);

		@SuppressWarnings("deprecation")
		final PGPSignatureGenerator sigGen = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(publicKey.getAlgorithm(), HashAlgorithmTags.SHA256).setProvider("BC"));

		sigGen.init(PGPSignature.POSITIVE_CERTIFICATION, privateKey);
		sigGen.setHashedSubpackets(subpacketGenerator.generate());

		final PGPSignature newSignature = sigGen.generateCertification(userId, publicKey);

		final PGPPublicKey updatedPublicKey = PGPPublicKey.addCertification(cleanedPublicKey, userId, newSignature);
		return PGPSecretKey.replacePublicKey(secretKey, updatedPublicKey);
	}

	/**
	 * Sets a new expiration time for the master key of a key ring by replacing the newest self certification of its
	 * first user ID. All other settings of that certification (key flags, preferred algorithms etc.) are kept.
	 *
	 * @param secretKeyRing key ring to update
	 * @param password password of the master secret key
	 * @param expiration local date and time when the key expires, null for unlimited validity
	 * @return updated key ring
	 * @throws Exception if the key cannot be unlocked, has no user ID or the signature cannot be created
	 */
	public static PGPSecretKeyRing updateKeyExpiration(final PGPSecretKeyRing secretKeyRing, final char[] password, final LocalDateTime expiration) throws Exception {
		final PGPSecretKey secretKey = secretKeyRing.getSecretKey();
		if (secretKey == null) {
			throw new IllegalArgumentException("No secret key found");
		}

		final PGPPrivateKey privateKey = secretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(password));
		if (privateKey == null) {
			throw new IllegalArgumentException("Wrong password or cannot unlock key");
		}

		final PGPPublicKey publicKey = secretKey.getPublicKey();

		final Iterator<String> userIds = publicKey.getUserIDs();
		if (!userIds.hasNext()) {
			throw new IllegalStateException("Mandatory User-ID of PGP key is missing");
		}
		final String userId = userIds.next();

		// The newest self certification of the user ID carries the key's settings (key flags, preferred algorithms etc.),
		// which must be kept in the new self certification. Only the expiration time is replaced.
		PGPSignature oldSelfCertification = null;
		final Iterator<PGPSignature> userIdSignatures = publicKey.getSignaturesForID(userId);
		while (userIdSignatures.hasNext()) {
			final PGPSignature signature = userIdSignatures.next();
			if (signature.getKeyID() == publicKey.getKeyID() && signature.isCertification()
					&& (oldSelfCertification == null || signature.getCreationTime().after(oldSelfCertification.getCreationTime()))) {
				oldSelfCertification = signature;
			}
		}

		@SuppressWarnings("deprecation")
		final PGPSignatureGenerator sigGen = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(publicKey.getAlgorithm(), HashAlgorithmTags.SHA256).setProvider("BC"));

		sigGen.init(PGPSignature.POSITIVE_CERTIFICATION, privateKey);

		final PGPSignatureSubpacketGenerator spGen;
		if (oldSelfCertification != null && oldSelfCertification.getHashedSubPackets() != null) {
			spGen = new PGPSignatureSubpacketGenerator(oldSelfCertification.getHashedSubPackets());
			spGen.removePacketsOfType(SignatureSubpacketTags.KEY_EXPIRE_TIME);
			spGen.removePacketsOfType(SignatureSubpacketTags.CREATION_TIME);
		} else {
			spGen = new PGPSignatureSubpacketGenerator();
		}

		if (expiration != null) {
			final long seconds = Duration.between(
				Instant.ofEpochMilli(publicKey.getCreationTime().getTime()),
				expiration.atZone(ZoneId.systemDefault()).toInstant()
			).getSeconds();

			spGen.setKeyExpirationTime(false, seconds);
		}

		sigGen.setHashedSubpackets(spGen.generate());

		final PGPSignature newSig = sigGen.generateCertification(userId, publicKey);

		// The new certification must be bound to the user ID. Adding it without user ID would store it as a
		// direct key signature, which leaves the user ID without any valid self certification.
		PGPPublicKey updatedPublicKey = publicKey;
		if (oldSelfCertification != null) {
			updatedPublicKey = PGPPublicKey.removeCertification(updatedPublicKey, userId, oldSelfCertification);
		}

		updatedPublicKey = PGPPublicKey.addCertification(updatedPublicKey, userId, newSig);

		final PGPSecretKey updatedSecretKey = PGPSecretKey.replacePublicKey(secretKey, updatedPublicKey);

		return PGPSecretKeyRing.insertSecretKey(PGPSecretKeyRing.removeSecretKey(secretKeyRing, secretKey), updatedSecretKey);
	}

	/**
	 * Changes the password of all secret keys of a key ring. The keys are protected with AES-256 and a strong S2K iteration count.
	 *
	 * @param secretKeyRing key ring
	 * @param oldPassword current password
	 * @param newPassword new password
	 * @return updated key ring
	 * @throws Exception if the old password is wrong or the keys cannot be encrypted
	 */
	public static PGPSecretKeyRing changeKeyPassword(final PGPSecretKeyRing secretKeyRing, final char[] oldPassword, final char[] newPassword) throws Exception {
		PGPSecretKeyRing updatedRing = secretKeyRing;

		final PBESecretKeyDecryptor oldDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(oldPassword);
		// Explicit S2K iteration count (0xc0 ~ 4.2 million iterations), matching the strength
		// used when keys are generated. Without this, BouncyCastle falls back to its much
		// weaker default S2K count (0x60 ~ 65536 iterations).
		final PGPDigestCalculator sha256Calc = new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA256);
		final PBESecretKeyEncryptor newEncryptor = new JcePBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256, sha256Calc, 0xc0).setProvider("BC").setSecureRandom(new SecureRandom()).build(newPassword);

		final Iterator<PGPSecretKey> it = secretKeyRing.getSecretKeys();
		while (it.hasNext()) {
			final PGPSecretKey secretKey = it.next();
			final PGPSecretKey newSecretKey = PGPSecretKey.copyWithNewPassword(secretKey, oldDecryptor, newEncryptor);
			updatedRing = PGPSecretKeyRing.insertSecretKey(PGPSecretKeyRing.removeSecretKey(updatedRing, secretKey), newSecretKey);
		}

		return updatedRing;
	}

	/**
	 * Returns the first signature of a key issued by the key itself.
	 *
	 * @param key public key
	 * @return first self signature, or null if there is none
	 */
	public static PGPSignature findSelfSignature(final PGPPublicKey key) {
		final Iterator<PGPSignature> it = key.getSignatures();

		while (it.hasNext()) {
			final PGPSignature sig = it.next();
			if (sig.getKeyID() == key.getKeyID()) {
				return sig;
			}
		}

		return null;
	}

	/**
	 * Changes the password of a secret key. The key is protected with AES-256 and a strong S2K iteration count.
	 *
	 * @param secretKey secret key
	 * @param oldPassword current password
	 * @param newPassword new password
	 * @return copy of the secret key protected with the new password
	 * @throws Exception if the old password is wrong or the key cannot be encrypted
	 */
	public static PGPSecretKey changeKeyPassword(final PGPSecretKey secretKey,  final char[] oldPassword, final char[] newPassword) throws Exception {
		final PBESecretKeyDecryptor oldDecryptor = new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(oldPassword);

		// Explicit S2K iteration count (0xc0), matching the strength used when keys are generated.
		final PBESecretKeyEncryptor newEncryptor = new JcePBESecretKeyEncryptorBuilder(SymmetricKeyAlgorithmTags.AES_256,
			new JcaPGPDigestCalculatorProviderBuilder().build().get(HashAlgorithmTags.SHA256), 0xc0).setProvider("BC").build(newPassword);

		return PGPSecretKey.copyWithNewPassword(secretKey, oldDecryptor, newEncryptor);
	}
}
