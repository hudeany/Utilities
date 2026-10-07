package de.soderer.utilities.crypto;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.CodeSigner;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStore.PrivateKeyEntry;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStore.TrustedCertificateEntry;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAKey;
import java.security.interfaces.DSAPrivateKey;
import java.security.interfaces.ECKey;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.EdECPublicKey;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ThreadLocalRandom;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipFile;

import javax.security.auth.x500.X500Principal;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.AccessDescription;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.CRLDistPoint;
import org.bouncycastle.asn1.x509.DistributionPoint;
import org.bouncycastle.asn1.x509.DistributionPointName;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.io.InvalidCipherTextIOException;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PrivateKeyInfoFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.crypto.util.SubjectPublicKeyInfoFactory;
import org.bouncycastle.jcajce.provider.asymmetric.util.EC5Util;
import org.bouncycastle.jcajce.util.DefaultJcaJceHelper;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.bouncycastle.jce.spec.ECParameterSpec;
import org.bouncycastle.jce.spec.ECPublicKeySpec;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMException;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.PKCS8Generator;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.operator.OutputEncryptor;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import de.soderer.utilities.BitUtilities;
import de.soderer.utilities.DateUtilities;
import de.soderer.utilities.FileUtilities;
import de.soderer.utilities.IoUtilities;
import de.soderer.utilities.MapStringReader;
import de.soderer.utilities.MissingPasswordException;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.WrongPasswordException;
import de.soderer.utilities.collection.CaseInsensitiveMap;
import de.soderer.utilities.crypto.Asn1Codec.DerTag;
import de.soderer.utilities.zip.ZipUtilities;
import jdk.security.jarsigner.JarSigner;

/**
 * Cryptographic utilities based on the JCE and BouncyCastle: key pair generation, PEM import and export of keys and
 * certificates, certificate and certification request handling, jar signatures, password based key derivation,
 * fingerprints and key store helpers.
 */
public class CryptographicUtilities {
	private CryptographicUtilities() {
		// Utility class
	}

	/**
	 * Return value of stream read methods at the end of the stream.
	 */
	public static final int EOF = -1;

	/**
	 * Names of symmetric block and stream ciphers known by BouncyCastle.
	 */
	public static final String[] SYMMETRIC_CIPHERS = {
			// Block chiffre
			"AES", "AESWrap", "Blowfish", "Camellia", "CamelliaWrap", "CAST5", "CAST6", "DES", "DESede", "TripleDES", "3DES", "DESedeWrap", "GOST28147", "IDEA", "Noekeon", "RC2", "RC5", "RC5-64", "RC6", "Rijndael",
			"SEED", "SEEDWrap", "Serpent", "Skipjack", "TEA", "Twofish", "XTEA",

			// Stream chiffre
			"RC4", "HC128", "HC256", "Salsa20", "VMPC", "Grainv1", "Grain128" };

	/**
	 * Default symmetric cipher transformation.
	 */
	public static final String DEFAULT_SYMMETRIC_ENCRYPTION_METHOD = "AES/CBC/PKCS7Padding";
	/**
	 * Examples of symmetric cipher transformations.
	 */
	public static final String[] KNOWN_SYMMETRIC_ENCRYPTION_METHODS = new String[] {
			"AES/CBC/PKCS7Padding", "DES/CBC/PKCS5Padding", "DES/CBC/X9.23Padding", "DES/OFB8/NoPadding",
			"DES/ECB/WithCTS", "IDEA/CBC/ISO10126Padding", "IDEA/CBC/ISO7816-4Padding", "SKIPJACK/ECB/PKCS7Padding" };

	/**
	 * Default signature method for RSA keys.
	 */
	public static final String DEFAULT_SIGNATURE_METHOD_RSA = "SHA256WithRSA";
	/**
	 * Signature methods for RSA keys.
	 */
	public static final String[] KNOWN_SIGNATURE_METHODS_RSA = new String[] { "MD2withRSA", "MD5withRSA", "SHA1withRSA",
			"RIPEMD128withRSA", "RIPEMD160withRSA", "RIPEMD256withRSA", "SHA256withRSA", "SHA224withRSA", "SHA384withRSA",
			"SHA512withRSA", "SHA1withRSAandMGF1", "SHA256withRSAandMGF1", "SHA384withRSAandMGF1", "SHA512withRSAandMGF1" };

	/**
	 * Default signature method for DSA keys.
	 */
	public static final String DEFAULT_SIGNATURE_METHOD_DSA = "SHA256withDSA";
	/**
	 * Signature methods for DSA keys.
	 */
	public static final String[] KNOWN_SIGNATURE_METHODS_DSA = new String[] { "SHA256withDSA", "SHA1withDSA",
			"SHA384withDSA", "SHA512withDSA", "NONEwithDSA" };

	/**
	 * Default signature method for elliptic curve keys.
	 */
	public static final String DEFAULT_SIGNATURE_METHOD_EC = "SHA256withECDSA";
	/**
	 * Signature methods for elliptic curve keys.
	 */
	public static final String[] KNOWN_SIGNATURE_METHODS_EC = new String[] { "RIPEMD160withECDSA", "SHA1withECDSA",
			"NONEwithECDSA", "SHA224withECDSA", "SHA256withECDSA", "SHA384withECDSA", "SHA512withECDSA", "SHA1withECNR",
			"SHA224withECNR", "SHA256withECNR", "SHA384withECNR", "SHA512withECNR" };

	/**
	 * Signature methods for other (GOST, DSTU) keys.
	 */
	public static final String[] KNOWN_SIGNATURE_METHODS_OTHERS = new String[] { "DSTU4145", "GOST3411withGOST3410", "GOST3411withGOST3410-94", "GOST3411withECGOST3410",
	"GOST3411withGOST3410-2001" };

	/**
	 * Supported asymmetric key algorithms.
	 */
	public static final String[] ASYMMETRIC_CIPHERS = new String[] { "RSA", "EC", "ElGamal", "Ed25519", "Ed448" };

	/**
	 * PKCS#1 v1.5 padding is susceptible to Bleichenbacher-style padding oracle attacks
	 * if decryption error information (message, timing) is ever exposed to an untrusted
	 * caller. OAEP is the recommended default; PKCS1Padding remains available explicitly
	 * for callers that need it for compatibility with existing ciphertexts.
	 */
	public static final String DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD_RSA = "RSA/NONE/OAEPWithSHA256AndMGF1Padding";
	/**
	 * Asymmetric encryption methods for RSA keys.
	 */
	public static final String[] KNOWN_ASYMMETRIC_ENCRYPTION_METHODS_RSA = new String[] {
			"RSA/NONE/PKCS1Padding",
			"RSA/NONE/OAEPPadding",
			"RSA/NONE/NoPadding",
			"RSA/NONE/PKCS1Padding",
			"RSA/NONE/OAEPWithMD5AndMGF1Padding",
			"RSA/NONE/OAEPWithSHA1AndMGF1Padding",
			"RSA/NONE/OAEPWithSHA224AndMGF1Padding",
			"RSA/NONE/OAEPWithSHA256AndMGF1Padding",
			"RSA/NONE/OAEPWithSHA384AndMGF1Padding",
			"RSA/NONE/OAEPWithSHA512AndMGF1Padding"
			// Note: ISO9796-1Padding was removed - it is a signature encoding with message
			// recovery, not an encryption padding scheme, and is no longer supported by
			// AsymmetricEncryptionWorker/AsymmetricDecryptionWorker.
	};

	/**
	 * Default asymmetric encryption method for elliptic curve keys.
	 */
	public static final String DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD_EC = "ECIES";
	/**
	 * Asymmetric encryption methods for elliptic curve keys.
	 */
	public static final String[] KNOWN_ASYMMETRIC_ENCRYPTION_METHODS_EC = new String[] {
			"ECIES"
	};

	/**
	 * Default asymmetric encryption method for ElGamal keys.
	 */
	public static final String DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD_ELGAMAL = "ELGAMAL/NONE/PKCS1PADDING";
	/**
	 * Asymmetric encryption methods for ElGamal keys.
	 */
	public static final String[] KNOWN_ASYMMETRIC_ENCRYPTION_METHODS_ELGAMAL = new String[] {
			"ELGAMAL/NONE/NoPadding",
			"ELGAMAL/NONE/PKCS1PADDING",
	};

	/**
	 * Default elliptic curve name for key generation.
	 */
	public static final String DEFAULT_ELLIPTIC_CURVE_NAME = "secp256k1";
	/**
	 * Names of elliptic curves known by BouncyCastle.
	 */
	public static final String[] KNOWN_ELLIPTIC_CURVE_NAMES = new String[] {
			"secp112r1", "secp112r2", "secp128r1", "secp128r2", "secp160k1", "secp160r1", "secp160r2", "secp192k1",
			"secp192r1", //= prime192v1
			"prime192v1", //= secp192r1
			"secp224k1", "secp224r1",
			"secp256k1", //= Bitcoin
			"secp256r1", //= prime256v1
			"prime256v1", //= secp256r1
			"secp384r1", "secp521r1",
			"sect113r1", "sect113r2", "sect131r1", "sect131r2", "sect163k1", "sect163r1", "sect163r2", "sect193r1",
			"sect193r2", "sect233k1", "sect233r1", "sect239k1", "sect283k1", "sect283r1", "sect409k1", "sect409r1",
			"sect571k1", "sect571r1"
	};

	/**
	 * Algorithm name of EdDSA with curve Ed25519.
	 */
	public static final String ENCRYPTION_METHOD_ED25519 = "ED25519";
	/**
	 * Algorithm name of EdDSA with curve Ed448.
	 */
	public static final String ENCRYPTION_METHOD_ED448 = "ED448";

	/**
	 * Generates a RSA key pair with the BouncyCastle provider.
	 *
	 * @param keyStrength key size in bits
	 * @return new key pair
	 * @throws Exception if the key pair cannot be generated
	 */
	public static KeyPair generateRsaKeyPair(final int keyStrength) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try {
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);
			keyGen.initialize(keyStrength, new SecureRandom());
			return keyGen.generateKeyPair();
		} catch (final Exception e) {
			throw new Exception("Cannot create RSA keypair", e);
		}
	}

	/**
	 * Generates a DSA key pair with the BouncyCastle provider.
	 *
	 * @param keyStrength key size in bits
	 * @return new key pair
	 * @throws Exception if the key pair cannot be generated
	 */
	public static KeyPair generateDsaKeyPair(final int keyStrength) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try {
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", BouncyCastleProvider.PROVIDER_NAME);
			keyGen.initialize(keyStrength, new SecureRandom());
			return keyGen.generateKeyPair();
		} catch (final Exception e) {
			throw new Exception("Cannot create DSA keypair", e);
		}
	}

	/**
	 * Generates a Diffie-Hellman key pair with the BouncyCastle provider.
	 *
	 * @param keyStrength key size in bits
	 * @return new key pair
	 * @throws Exception if the key pair cannot be generated
	 */
	public static KeyPair generateDhKeyPair(final int keyStrength) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try {
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DH", BouncyCastleProvider.PROVIDER_NAME);
			keyGen.initialize(keyStrength, new SecureRandom());
			return keyGen.generateKeyPair();
		} catch (final Exception e) {
			throw new Exception("Cannot create DH keypair", e);
		}
	}

	/**
	 * Generates an elliptic curve key pair with the BouncyCastle provider.
	 *
	 * @param ecCurveName curve name, e.g. "secp256r1", see {@link #KNOWN_ELLIPTIC_CURVE_NAMES}
	 * @return new key pair
	 * @throws Exception if the curve name is empty or unknown
	 */
	public static KeyPair generateEcKeyPair(final String ecCurveName) throws Exception {
		if (ecCurveName == null || "".equals(ecCurveName.trim())) {
			throw new Exception("Missing EC curve name parameter");
		}

		Security.addProvider(new BouncyCastleProvider());

		try {
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
			final ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec(ecCurveName);
			keyGen.initialize(ecGenParameterSpec, new SecureRandom());
			return keyGen.generateKeyPair();
		} catch (final Exception e) {
			throw new Exception("Cannot create EC keypair", e);
		}
	}

	/**
	 * Generates a ElGamal key pair with the BouncyCastle provider.
	 *
	 * @param keyStrength key size in bits
	 * @return new key pair
	 * @throws Exception if the key pair cannot be generated
	 */
	public static KeyPair generateElGamalKeyPair(final int keyStrength) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try {
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ElGamal", BouncyCastleProvider.PROVIDER_NAME);
			keyGen.initialize(keyStrength, new SecureRandom());
			return keyGen.generateKeyPair();
		} catch (final Exception e) {
			throw new Exception("Cannot create ElGamal keypair", e);
		}
	}

	/**
	 * Generates an EdDSA key pair with curve Ed25519 with the BouncyCastle provider.
	 *
	 * @return new key pair
	 * @throws Exception if the key pair cannot be generated
	 */
	public static KeyPair generateEd25519KeyPair() throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try {
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ED25519", BouncyCastleProvider.PROVIDER_NAME);
			final ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("ED25519");
			keyGen.initialize(ecGenParameterSpec, new SecureRandom());
			return keyGen.generateKeyPair();
		} catch (final Exception e) {
			throw new Exception("Cannot create EC keypair", e);
		}
	}

	/**
	 * Generates an EdDSA key pair with curve Ed448 with the BouncyCastle provider.
	 *
	 * @return new key pair
	 * @throws Exception if the key pair cannot be generated
	 */
	public static KeyPair generateEd448KeyPair() throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try {
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ED448", BouncyCastleProvider.PROVIDER_NAME);
			final ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("ED448");
			keyGen.initialize(ecGenParameterSpec, new SecureRandom());
			return keyGen.generateKeyPair();
		} catch (final Exception e) {
			throw new Exception("Cannot create EC keypair", e);
		}
	}

	/**
	 * Generates a self-signed X.509 certificate with a random 64 bit serial number.
	 *
	 * @param keyPair key pair to certify and sign with
	 * @param validFrom start of the validity period (local time)
	 * @param validUntil end of the validity period (local time)
	 * @param signatureAlgorithm signature algorithm, e.g. {@link #DEFAULT_SIGNATURE_METHOD_RSA}
	 * @param subjectDN subject and issuer distinguished name, e.g. "CN=Test"
	 * @param allowedSubCaCertificateLevels path length for a CA certificate (0 or more), or -1 for an end entity certificate
	 * @return new certificate
	 * @throws Exception if the certificate cannot be created
	 */
	public static X509Certificate generateSelfsignedCertificate(final KeyPair keyPair, final LocalDateTime validFrom, final LocalDateTime validUntil, final String signatureAlgorithm, final String subjectDN, final int allowedSubCaCertificateLevels) throws Exception {
		final Provider bcProvider = new BouncyCastleProvider();
		Security.addProvider(bcProvider);

		try {
			final X500Name dnName = new X500Name(subjectDN);
			final BigInteger certificateSerialNumber = new BigInteger(64, new SecureRandom());

			final ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.getPrivate());
			final JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(dnName, certificateSerialNumber, DateUtilities.getDateForLocalDateTime(validFrom), DateUtilities.getDateForLocalDateTime(validUntil), dnName, keyPair.getPublic());
			if (allowedSubCaCertificateLevels >= 0) {
				certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(allowedSubCaCertificateLevels));
			} else {
				certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
			}
			return new JcaX509CertificateConverter().setProvider(bcProvider).getCertificate(certificateBuilder.build(contentSigner));
		} catch (final Exception e) {
			throw new Exception("Cannot create selfsigned certificate: " + e.getMessage(), e);
		}
	}

	/**
	 * Returns the PEM encoding of a certificate.
	 *
	 * @param certificate certificate
	 * @return PEM string "-----BEGIN CERTIFICATE-----..."
	 * @throws Exception if the certificate cannot be encoded
	 */
	public static String getStringFromX509Certificate(final X509Certificate certificate) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final StringWriter stringWriter = new StringWriter();
		try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
			pemWriter.writeObject(certificate);
		} catch (final Exception e) {
			throw new Exception("Cannot create certificate string: " + e.getMessage(), e);
		}
		return stringWriter.toString();
	}

	/**
	 * Returns the PEM encoding of a RSA key pair: the private key followed by the public key.
	 *
	 * @param keyPair RSA key pair
	 * @param password password to encrypt the private key with, null or empty for an unencrypted private key
	 * @return PEM string
	 * @throws Exception if the keys cannot be encoded
	 */
	public static String getStringFromKeyPair(final AsymmetricCipherKeyPair keyPair, final char[] password) throws Exception {
		final PublicKey publicKey = getPublicKeyFromAsymmetricCipherKeyPair(keyPair);
		final PrivateKey privateKey = getPrivateKeyFromAsymmetricCipherKeyPair(keyPair);

		return getStringFromKeyPair(privateKey, password, publicKey);
	}

	/**
	 * Returns the PEM encoding of a key pair: the private key followed by the public key.
	 *
	 * @param keyPair key pair
	 * @param password password to encrypt the private key with, null or empty for an unencrypted private key
	 * @return PEM string
	 * @throws Exception if the keys cannot be encoded
	 */
	public static String getStringFromKeyPair(final KeyPair keyPair, final char[] password) throws Exception {
		final PublicKey publicKey = keyPair.getPublic();
		final PrivateKey privateKey = keyPair.getPrivate();

		return getStringFromKeyPair(privateKey, password, publicKey);
	}

	/**
	 * Returns the PEM encoding of a private key followed by the PEM encoding of a public key.
	 *
	 * @param privateKey private key
	 * @param password password to encrypt the private key with, null or empty for an unencrypted private key
	 * @param publicKey public key
	 * @return PEM string
	 * @throws Exception if the keys cannot be encoded
	 */
	public static String getStringFromKeyPair(final PrivateKey privateKey, final char[] password, final PublicKey publicKey) throws Exception {
		final StringBuilder result = new StringBuilder();
		result.append(getStringFromKey(privateKey, password));
		result.append(getStringFromKey(publicKey));
		return result.toString();
	}

	/**
	 * Returns the PEM encoding of a public key.
	 *
	 * @param publicKey public key
	 * @return PEM string "-----BEGIN PUBLIC KEY-----..."
	 * @throws Exception if the key cannot be encoded
	 */
	public static String getStringFromKey(final PublicKey publicKey) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final StringWriter stringWriter = new StringWriter();
		try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
			pemWriter.writeObject(publicKey);
		} catch (final Exception e) {
			throw new Exception("Cannot create public key string: " + e.getMessage(), e);
		}
		return stringWriter.toString();
	}

	/**
	 * Returns the PEM encoding of a private key. With a password the key is written as encrypted PKCS#8
	 * ("ENCRYPTED PRIVATE KEY", PBES2 with AES-256-CBC and PBKDF2-HMAC-SHA256).
	 *
	 * @param privateKey private key
	 * @param password password to encrypt the key with, null or empty for an unencrypted key
	 * @return PEM string
	 * @throws Exception if the key cannot be encoded or encrypted
	 */
	public static String getStringFromKey(final PrivateKey privateKey, final char[] password) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		if (password == null || password.length == 0) {
			final StringWriter stringWriter = new StringWriter();
			try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
				pemWriter.writeObject(privateKey);
			} catch (final Exception e) {
				throw new Exception("Cannot create private key string: " + e.getMessage(), e);
			}
			return stringWriter.toString();
		} else {
			final StringWriter stringWriter = new StringWriter();
			try (JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
				// PBES2 with AES-256-CBC and PBKDF2-HMAC-SHA256 instead of the legacy PKCS#12 3DES/SHA-1 scheme
				final OutputEncryptor encryptor = new JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.AES_256_CBC)
						.setPRF(PKCS8Generator.PRF_HMACSHA256)
						.setIterationCount(PKCS8_ENCRYPTION_ITERATIONS)
						.setProvider(BouncyCastleProvider.PROVIDER_NAME)
						.setRandom(new SecureRandom())
						.setPassword(password).build();
				pemWriter.writeObject(new JcaPKCS8Generator(privateKey, encryptor));
			} catch (final Exception e) {
				throw new Exception("Cannot create private key string: " + e.getMessage(), e);
			}
			return stringWriter.toString();
		}
	}

	/**
	 * Reads an unencrypted PEM key pair (or private key) into a BouncyCastle lightweight key pair. The stream is read completely.
	 *
	 * @param inputStream PEM data (UTF-8)
	 * @return key pair, its public part is null if the PEM data contains no public key
	 * @throws Exception if the data contains no readable key pair
	 */
	public static AsymmetricCipherKeyPair getAsymmetricCipherKeyPair(final InputStream inputStream) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final PEMKeyPair keyPair = getPEMKeyPairFromString(IoUtilities.toString(inputStream, StandardCharsets.UTF_8));
		AsymmetricKeyParameter privateAsymmetricKeyParameter;
		if (keyPair.getPrivateKeyInfo() != null) {
			privateAsymmetricKeyParameter = PrivateKeyFactory.createKey(keyPair.getPrivateKeyInfo());
		} else {
			privateAsymmetricKeyParameter = null;
		}
		final AsymmetricKeyParameter publicAsymmetricKeyParameter;
		if (keyPair.getPublicKeyInfo() != null) {
			publicAsymmetricKeyParameter = PublicKeyFactory.createKey(keyPair.getPublicKeyInfo());
		} else {
			publicAsymmetricKeyParameter = null;
		}

		return new AsymmetricCipherKeyPair(publicAsymmetricKeyParameter, privateAsymmetricKeyParameter);
	}

	/**
	 * Reads an unencrypted PEM RSA key pair into a JCE key pair. The stream is read completely.
	 *
	 * @param inputStream PEM data (UTF-8)
	 * @return RSA key pair
	 * @throws Exception if the data contains no readable RSA key pair
	 */
	public static KeyPair getKeyPair(final InputStream inputStream) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final PEMKeyPair keyPair = getPEMKeyPairFromString(IoUtilities.toString(inputStream, StandardCharsets.UTF_8));
		AsymmetricKeyParameter privateAsymmetricKeyParameter;
		if (keyPair.getPrivateKeyInfo() != null) {
			privateAsymmetricKeyParameter = PrivateKeyFactory.createKey(keyPair.getPrivateKeyInfo());
		} else {
			privateAsymmetricKeyParameter = null;
		}
		final AsymmetricKeyParameter publicAsymmetricKeyParameter;
		if (keyPair.getPublicKeyInfo() != null) {
			publicAsymmetricKeyParameter = PublicKeyFactory.createKey(keyPair.getPublicKeyInfo());
		} else {
			publicAsymmetricKeyParameter = null;
		}

		final AsymmetricCipherKeyPair asymmetricCipherKeyPair = new AsymmetricCipherKeyPair(publicAsymmetricKeyParameter, privateAsymmetricKeyParameter);

		return new KeyPair(getPublicKeyFromAsymmetricCipherKeyPair(asymmetricCipherKeyPair), getPrivateKeyFromAsymmetricCipherKeyPair(asymmetricCipherKeyPair));
	}

	/**
	 * Converts a BouncyCastle lightweight RSA key pair into a JCE key pair. Only RSA keys are supported.
	 *
	 * @param asymmetricCipherKeyPair RSA key pair
	 * @return JCE key pair
	 * @throws Exception if the keys cannot be converted
	 */
	public static KeyPair getKeyPairFromAsymmetricCipherKeyPair(final AsymmetricCipherKeyPair asymmetricCipherKeyPair) throws Exception {
		final byte[] pkcs8Encoded = PrivateKeyInfoFactory.createPrivateKeyInfo(asymmetricCipherKeyPair.getPrivate()).getEncoded();
		final PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(pkcs8Encoded);
		final byte[] spkiEncoded = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(asymmetricCipherKeyPair.getPublic()).getEncoded();
		final X509EncodedKeySpec spkiKeySpec = new X509EncodedKeySpec(spkiEncoded);
		final KeyFactory keyFac = KeyFactory.getInstance("RSA");
		return new KeyPair(keyFac.generatePublic(spkiKeySpec), keyFac.generatePrivate(pkcs8KeySpec));
	}

	/**
	 * Converts a JCE private and public key into a BouncyCastle lightweight key pair.
	 *
	 * @param privateKey private key (with a traditional PEM encoding, e.g. RSA)
	 * @param publicKey public key
	 * @return lightweight key pair
	 * @throws Exception if the keys cannot be converted
	 */
	public static AsymmetricCipherKeyPair getAsymmetricCipherKeyPair(final PrivateKey privateKey, final PublicKey publicKey) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final PEMKeyPair keyPair = getPEMKeyPairFromString(getStringFromKeyPair(privateKey, null, publicKey));
		final AsymmetricKeyParameter privateAsymmetricKeyParameter = PrivateKeyFactory.createKey(keyPair.getPrivateKeyInfo());
		final AsymmetricKeyParameter publicAsymmetricKeyParameter = PublicKeyFactory.createKey(keyPair.getPublicKeyInfo());

		return new AsymmetricCipherKeyPair(publicAsymmetricKeyParameter, privateAsymmetricKeyParameter);
	}

	/**
	 * Returns the JCE public key of a BouncyCastle lightweight RSA key pair.
	 *
	 * @param keyPair RSA key pair
	 * @return RSA public key
	 * @throws Exception if the key is no RSA key or cannot be converted
	 */
	public static PublicKey getPublicKeyFromAsymmetricCipherKeyPair(final AsymmetricCipherKeyPair keyPair) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final RSAKeyParameters publicKey = (RSAKeyParameters) keyPair.getPublic();
		return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(publicKey.getModulus(), publicKey.getExponent()));
	}

	/**
	 * Returns the public key of a key pair. RSA public keys are converted to a key of the default RSA key factory.
	 *
	 * @param keyPair key pair
	 * @return public key
	 * @throws Exception if the key cannot be converted
	 */
	public static PublicKey getPublicKeyFromKeyPair(final KeyPair keyPair) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final PublicKey publicKey = keyPair.getPublic();
		if (publicKey instanceof RSAPublicKey) {
			final RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;
			return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(rsaPublicKey.getModulus(), rsaPublicKey.getPublicExponent()));
		} else {
			return publicKey;
		}
	}

	/**
	 * Returns the JCE private key of a BouncyCastle lightweight RSA key pair.
	 *
	 * @param keyPair RSA key pair with CRT private key parameters
	 * @return RSA private CRT key
	 * @throws Exception if the key is no RSA CRT key or cannot be converted
	 */
	public static PrivateKey getPrivateKeyFromAsymmetricCipherKeyPair(final AsymmetricCipherKeyPair keyPair) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final RSAPrivateCrtKeyParameters privateKey = (RSAPrivateCrtKeyParameters) keyPair.getPrivate();
		// getExponent() of RSAPrivateCrtKeyParameters is the private exponent, the public one is getPublicExponent()
		return KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateCrtKeySpec(privateKey.getModulus(), privateKey.getPublicExponent(), privateKey.getExponent(), privateKey.getP(), privateKey.getQ(),
				privateKey.getDP(), privateKey.getDQ(), privateKey.getQInv()));
	}

	/**
	 * Reads the first object of unencrypted PEM data as key pair.
	 *
	 * @param keyString PEM data
	 * @return key pair (with a null public key info for a PKCS#8 private key), or null if the first object is no key
	 * @throws Exception if the data cannot be parsed
	 */
	public static PEMKeyPair getPEMKeyPairFromString(final String keyString) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try (PEMParser pemReader = new PEMParser(new StringReader(keyString))) {
			final Object readObject = pemReader.readObject();
			pemReader.close();
			//			if (readObject instanceof PEMEncryptedKeyPair) {
			//                PEMEncryptedKeyPair pemEncryptedKeyPairKeyPair = (PEMEncryptedKeyPair) readObject;
			//                JcePEMDecryptorProviderBuilder jcePEMDecryptorProviderBuilder = new JcePEMDecryptorProviderBuilder();
			//                PEMKeyPair pemKeyPair = pemEncryptedKeyPairKeyPair.decryptKeyPair(jcePEMDecryptorProviderBuilder.build(keyPassword.toCharArray()));
			//            } else
			if (readObject instanceof PEMKeyPair) {
				final PEMKeyPair keyPair = (PEMKeyPair) readObject;
				return keyPair;
			} else if (readObject instanceof PrivateKeyInfo) {
				final PEMKeyPair keyPair = new PEMKeyPair(null, (PrivateKeyInfo) readObject);
				return keyPair;
			} else {
				return null;
			}
		} catch (final Exception e) {
			throw new Exception("Cannot read private key", e);
		}
	}

	/**
	 * Reads all certificates from PEM data. Other PEM objects are skipped.
	 *
	 * @param certificateString PEM data
	 * @return certificates in the order of the data
	 * @throws Exception if the data cannot be parsed
	 */
	public static List<X509Certificate> getCertificatesFromString(final String certificateString) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try (PEMParser pemReader = new PEMParser(new StringReader(certificateString))) {
			final List<X509Certificate> returnList = new ArrayList<>();
			Object readObject;
			while ((readObject = pemReader.readObject()) != null) {
				if (readObject instanceof X509Certificate) {
					returnList.add((X509Certificate) readObject);
				} else if (readObject instanceof X509CertificateHolder) {
					returnList.add(new JcaX509CertificateConverter().getCertificate((X509CertificateHolder) readObject));
				}
			}
			return returnList;
		} catch (final Exception e) {
			throw new Exception("Cannot read certificate", e);
		}
	}

	/**
	 * Reads all certificates from a PEM file (UTF-8).
	 *
	 * @param certificateFile PEM file
	 * @return certificates in the order of the file
	 * @throws Exception if the file cannot be read or parsed
	 */
	public static List<X509Certificate> getCertificatesFromFile(final File certificateFile) throws Exception {
		final String certificateFileString = FileUtilities.readFileToString(certificateFile, StandardCharsets.UTF_8);
		return getCertificatesFromString(certificateFileString);
	}

	/**
	 * Minimum recommended PBKDF2-HMAC-SHA1 iteration count as of 2024 (OWASP guidance). The previous
	 * hardcoded value of 1000 iterations was far too weak by modern standards and made brute-forcing
	 * a derived key significantly easier.
	 */
	public static final int DEFAULT_PASSWORD_STRETCH_ITERATIONS = 210_000;

	/**
	 * PBKDF2-HMAC-SHA256 iteration count for password protected PKCS#8 private key strings.
	 */
	private static final int PKCS8_ENCRYPTION_ITERATIONS = 100_000;

	/**
	 * Derives a key from a password with PBKDF2-HMAC-SHA1 and {@link #DEFAULT_PASSWORD_STRETCH_ITERATIONS} iterations.
	 *
	 * @param password password (UTF-8 encoded for the derivation)
	 * @param keyLength key length in bits
	 * @param salt salt
	 * @return derived key bytes
	 */
	public static byte[] stretchPassword(final char[] password, final int keyLength, final byte[] salt) {
		return stretchPassword(password, keyLength, salt, DEFAULT_PASSWORD_STRETCH_ITERATIONS);
	}

	/**
	 * Derives a key from a password with PBKDF2-HMAC-SHA1.
	 *
	 * @param password password (UTF-8 encoded for the derivation)
	 * @param keyLength key length in bits
	 * @param salt salt
	 * @param iterations number of iterations, must be positive
	 * @return derived key bytes
	 */
	public static byte[] stretchPassword(final char[] password, final int keyLength, final byte[] salt, final int iterations) {
		if (iterations <= 0) {
			throw new IllegalArgumentException("Invalid iterations value: " + iterations);
		}

		Security.addProvider(new BouncyCastleProvider());

		final PBEParametersGenerator generator = new PKCS5S2ParametersGenerator();
		final byte[] passwordBytes = PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password);
		try {
			generator.init(passwordBytes, salt, iterations);
			final KeyParameter params = (KeyParameter) generator.generateDerivedParameters(keyLength);
			return params.getKey();
		} finally {
			// Do not leave a copy of the password in memory
			Arrays.fill(passwordBytes, (byte) 0);
		}
	}

	/**
	 * Determine the correct key length in bits for a given symmetric cipher transformation
	 * (e.g. "aes/cbc/pkcs7padding" or "desede"). Using a single hardcoded key length (e.g.
	 * always 128 bit) for all ciphers is wrong: DESede/3DES needs 192 bit, DES needs 64 bit,
	 * and AES can safely use 256 bit. Callers deriving a key via stretchPassword() should use
	 * this instead of assuming a fixed length.
	 *
	 * @param encryptionMethod cipher name or transformation, case insensitive
	 * @return key length in bits
	 * @throws IllegalArgumentException if the method is blank or the cipher is not supported (AES, DESede, DES and Blowfish are)
	 */
	public static int determineSymmetricKeyLengthBits(final String encryptionMethod) {
		if (Utilities.isBlank(encryptionMethod)) {
			throw new IllegalArgumentException("Empty encryption method");
		}

		final String lowerCaseMethod = encryptionMethod.toLowerCase(Locale.ROOT);
		final String baseAlgorithm = lowerCaseMethod.contains("/") ? lowerCaseMethod.substring(0, lowerCaseMethod.indexOf('/')) : lowerCaseMethod;
		switch (baseAlgorithm) {
			case "aes":
				return 256;
			case "desede":
			case "tripledes":
			case "3des":
				return 192;
			case "des":
				return 64;
			case "blowfish":
				return 128;
			default:
				throw new IllegalArgumentException("Cannot determine key length for encryption method: " + encryptionMethod);
		}
	}

	/**
	 * One-time-pad style secret splitting: fills all but the last array of {@code mutableData} with random bytes and sets
	 * the last array to the XOR of all {@code immutableData} arrays and the random arrays. XORing all mutableData arrays
	 * then results in the XOR of the immutableData arrays. All arrays must have at least the length of {@code immutableData[0]}.
	 *
	 * @param immutableData data to split (or the shares to combine)
	 * @param mutableData arrays to fill, the last one receives the XOR result
	 */
	public static void otpWork(final byte[][] immutableData, final byte[][] mutableData) {
		for (int i = 0; i < mutableData.length - 1; i++) {
			Utilities.getRandomByteArray(mutableData[i]);
		}
		final byte[] lastArray = mutableData[mutableData.length - 1];
		for (int i = 0; i < immutableData[0].length; i++) {
			lastArray[i] = 0;
			for (final byte[] element : immutableData) {
				lastArray[i] = (byte) (lastArray[i] ^ element[i]);
			}
			for (int j = 0; j < mutableData.length - 1; j++) {
				lastArray[i] = (byte) (lastArray[i] ^ mutableData[j][i]);
			}
		}
	}

	/**
	 * List all Certificate names (CN) of jar signatures, including the CNs of the signers' certificate chains.
	 * WATCH OUT: This DOES NOT validate the certificates or the trust of the signatures.
	 *
	 * @param jarFile jar file
	 * @return CNs of all certificates, or null if the jar has no manifest
	 * @throws Exception if the jar cannot be read or an entry's digest does not match its signature
	 */
	public static Set<String> getJarSignatureNames(final File jarFile) throws Exception {
		final Set<String> returnData = new HashSet<>();
		try (JarFile jar = new JarFile(jarFile)) {
			final Manifest manifest = jar.getManifest();
			if (manifest == null) {
				return null;
			}

			final byte[] buffer = new byte[4096];
			final Enumeration<JarEntry> jarEntriesEnumerator = jar.entries();

			while (jarEntriesEnumerator.hasMoreElements()) {
				final JarEntry jarEntry = jarEntriesEnumerator.nextElement();

				try (InputStream jarEntryInputStream = jar.getInputStream(jarEntry)) {
					// Reading the jarEntry throws a SecurityException if signature/digest check fails.
					while (jarEntryInputStream.read(buffer, 0, buffer.length) != EOF) {
						// just read it
					}
				}

				if (!jarEntry.isDirectory()) {
					// Every file must be signed, except for files in META-INF
					final Certificate[] certificates = jarEntry.getCertificates();
					if (certificates != null && certificates.length > 0) {
						for (final Certificate cert : certificates) {
							if (cert instanceof X509Certificate) {
								returnData.add(getValuesFromX500Principal(((X509Certificate) cert).getSubjectX500Principal()).get("CN"));
							} else {
								returnData.add("Unknown type of certificate");
							}
						}
					}
				}
			}

			return returnData;
		}
	}

	/**
	 * Check if jar file has a signature and has only signed class and resource files.
	 * Only the signature related files in "META-INF" (manifest, signature files and signature blocks) may be unsigned.
	 * This does not check whether the signers are trusted, see {@link #verifyJarSignature(File, Collection)}.
	 *
	 * @param jarFile jar file
	 * @return true if all entries are signed with X.509 certificates
	 * @throws Exception if the jar cannot be read or an entry's digest does not match its signature
	 */
	public static boolean checkJarIsCompletlySigned(final File jarFile) throws Exception {
		try (JarFile jar = new JarFile(jarFile)) {
			final Manifest manifest = jar.getManifest();
			if (manifest == null) {
				// Has no MANIFEST.MF file
				return false;
			}

			final byte[] buffer = new byte[4096];
			final Enumeration<JarEntry> jarEntriesEnumerator = jar.entries();

			while (jarEntriesEnumerator.hasMoreElements()) {
				final JarEntry jarEntry = jarEntriesEnumerator.nextElement();

				try (InputStream jarEntryInputStream = jar.getInputStream(jarEntry)) {
					// Reading the jarEntry throws a SecurityException if signature/digest check fails.
					while (jarEntryInputStream.read(buffer, 0, buffer.length) != EOF) {
						// just read it
					}
				}

				if (!jarEntry.isDirectory()) {
					// Every file must be signed, except for the signature related files in META-INF
					final Certificate[] certificates = jarEntry.getCertificates();
					if ((certificates == null) || (certificates.length == 0)) {
						if (!isSignatureRelatedJarEntry(jarEntry.getName())) {
							// Contains unsigned files
							return false;
						}
					} else {
						for (final Certificate cert : certificates) {
							if (!(cert instanceof X509Certificate)) {
								// Unknown type of certificate
								return false;
							}
						}
					}
				}
			}

			return true;
		}
	}

	/**
	 * Verifies that all entries of a jar file (except the signature related files in "META-INF") are signed by a signer
	 * whose certificate is currently valid and chains up to one of the trusted certificates. The other certificates
	 * from the signature block are only used as intermediate certificates. Revocation is not checked.
	 *
	 * @param jarFile jar file
	 * @param trustedCertificates trusted (root) certificates
	 * @return true if the jar is completely signed by trusted signers, false otherwise or on any error
	 */
	public static boolean verifyJarSignature(final File jarFile, final Collection<? extends Certificate> trustedCertificates) {
		if (trustedCertificates == null || trustedCertificates.size() == 0) {
			return false;
		}

		try (JarFile jar = new JarFile(jarFile)) {
			final Manifest manifest = jar.getManifest();
			if (manifest == null) {
				throw new SecurityException("The jar file has no manifest, which contains the file signatures");
			}

			final byte[] buffer = new byte[4096];
			final Enumeration<JarEntry> jarEntriesEnumerator = jar.entries();

			while (jarEntriesEnumerator.hasMoreElements()) {
				final JarEntry jarEntry = jarEntriesEnumerator.nextElement();

				try (InputStream jarEntryInputStream = jar.getInputStream(jarEntry)) {
					// Reading the jarEntry throws a SecurityException if signature/digest check fails.
					while (jarEntryInputStream.read(buffer, 0, buffer.length) != EOF) {
						// just read it
					}
				}

				if (!jarEntry.isDirectory()) {
					// Every file must be signed, except for the signature related files in META-INF
					final CodeSigner[] codeSigners = jarEntry.getCodeSigners();
					if ((codeSigners == null) || (codeSigners.length == 0)) {
						if (!isSignatureRelatedJarEntry(jarEntry.getName())) {
							throw new SecurityException("The jar file contains unsigned files: " + jarEntry.getName());
						}
					} else {
						boolean isSignedByTrustedCert = false;

						// Only the first certificate of each signer's certificate path is the certificate whose key
						// actually signed the entry. The other certificates of the path are unverified extra data
						// taken from the signature block, so they may only serve as intermediates for the path
						// building and must never be accepted as signers themselves.
						for (final CodeSigner codeSigner : codeSigners) {
							final List<? extends Certificate> signerCertificatePath = codeSigner.getSignerCertPath().getCertificates();
							if (!signerCertificatePath.isEmpty() && signerCertificatePath.get(0) instanceof X509Certificate) {
								final X509Certificate signerCertificate = (X509Certificate) signerCertificatePath.get(0);
								try {
									signerCertificate.checkValidity();
								} catch (@SuppressWarnings("unused") final CertificateExpiredException | CertificateNotYetValidException e) {
									// This signer certificate is expired or not yet valid, so it cannot be trusted
									// even if it otherwise chains to a trusted root. Try the next signer.
									continue;
								}

								if (verifyChainOfTrust(signerCertificate, signerCertificatePath.subList(1, signerCertificatePath.size()), trustedCertificates)) {
									isSignedByTrustedCert = true;
									break;
								}
							}
						}

						if (!isSignedByTrustedCert) {
							throw new SecurityException("The jar file contains untrusted signed files");
						}
					}
				}
			}

			return true;
		} catch (@SuppressWarnings("unused") final Exception e) {
			return false;
		}
	}

	/**
	 * Creates a copy of a jar file without the signature files and signature blocks in "META-INF".
	 * The digests in the manifest are kept.
	 *
	 * @param signedTestJarFile signed jar file
	 * @param unsignedTestJarFile jar file to create
	 * @throws Exception if the files cannot be read or written
	 */
	public static void removeJarSignature(final File signedTestJarFile, final File unsignedTestJarFile) throws Exception {
		ZipUtilities.removeFilesFromZipFile(signedTestJarFile, unsignedTestJarFile, StandardCharsets.UTF_8, "META-INF/*.RSA", "META-INF/*.DSA", "META-INF/*.EC", "META-INF/*.SF", "META-INF/SIG-*");
	}

	/**
	 * Checks whether a jar entry is one of the signature related files, which are not signed themselves
	 * ("META-INF/MANIFEST.MF", "META-INF/*.SF", "META-INF/*.RSA", "META-INF/*.DSA", "META-INF/*.EC" and
	 * "META-INF/SIG-*", directly in the "META-INF" directory). This is the same rule the JDK jar verifier uses.
	 * All other entries, including other files in "META-INF" like multi-release classes in
	 * "META-INF/versions/", must be signed.
	 *
	 * @param entryName name of the jar entry
	 * @return true if the entry is a signature related file
	 */
	private static boolean isSignatureRelatedJarEntry(final String entryName) {
		final String upperCaseEntryName = entryName.toUpperCase(Locale.ROOT);
		if (!upperCaseEntryName.startsWith("META-INF/")) {
			return false;
		}
		final String fileName = upperCaseEntryName.substring("META-INF/".length());
		if (fileName.contains("/")) {
			return false;
		}
		return "MANIFEST.MF".equals(fileName)
				|| fileName.endsWith(".SF")
				|| fileName.endsWith(".RSA")
				|| fileName.endsWith(".DSA")
				|| fileName.endsWith(".EC")
				|| fileName.startsWith("SIG-");
	}

	/**
	 * Check if "certificate" was certified by "trustedCertificates", allowing the given untrusted intermediate
	 * certificates to be used for building the certificate path.
	 *
	 * @param certificate certificate to check
	 * @param intermediateCertificates untrusted certificates that may be used as intermediate certificates of the path
	 * @param trustedCertificates trusted root certificates
	 * @return true if a valid certificate path from the certificate to one of the trusted certificates exists
	 */
	private static boolean verifyChainOfTrust(final X509Certificate certificate, final Collection<? extends Certificate> intermediateCertificates, final Collection<? extends Certificate> trustedCertificates) {
		try {
			final X509CertSelector targetConstraints = new X509CertSelector();
			targetConstraints.setCertificate(certificate);

			final Set<TrustAnchor> trustAnchors = new HashSet<>();
			for (final Certificate trustedRootCert : trustedCertificates) {
				trustAnchors.add(new TrustAnchor((X509Certificate) trustedRootCert, null));
			}

			final PKIXBuilderParameters params = new PKIXBuilderParameters(trustAnchors, targetConstraints);
			// See verifyChainOfTrust(X509Certificate, Collection): no revocation data source is configured
			params.setRevocationEnabled(false);
			if (intermediateCertificates != null && !intermediateCertificates.isEmpty()) {
				params.addCertStore(CertStore.getInstance("Collection", new CollectionCertStoreParameters(intermediateCertificates)));
			}
			final PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult) CertPathBuilder.getInstance("PKIX").build(params);
			return result != null;
		} catch (@SuppressWarnings("unused") final Exception e) {
			return false;
		}
	}

	/**
	 * Signs a jar file with the JDK jar signer.
	 *
	 * @param unsignedJarFile jar file to sign
	 * @param privateKey private key of the signer
	 * @param certPath certificate path of the signer, starting with the signer's certificate
	 * @param signedJarFile signed jar file to create
	 * @throws Exception if the jar cannot be signed
	 */
	public static void createJarSignature(final File unsignedJarFile, final PrivateKey privateKey, final CertPath certPath, final File signedJarFile) throws Exception {
		final JarSigner signer = new JarSigner.Builder(privateKey, certPath).build();
		try (ZipFile in = new ZipFile(unsignedJarFile);
				FileOutputStream out = new FileOutputStream(signedJarFile)) {
			signer.sign(in, out);
		}
	}

	/**
	 * Creates an X.509 certificate path.
	 *
	 * @param certs certificates, starting with the target certificate
	 * @return certificate path
	 * @throws Exception if the certificates are no X.509 certificates
	 */
	public static CertPath createX509CertPath(final Certificate[] certs) throws Exception {
		return CertificateFactory.getInstance("X509").generateCertPath(Arrays.asList(certs));
	}

	/**
	 * Creates an X.509 certificate path.
	 *
	 * @param certs certificates, starting with the target certificate
	 * @return certificate path
	 * @throws Exception if the path cannot be created
	 */
	public static CertPath createX509CertPath(final List<X509Certificate> certs) throws Exception {
		return CertificateFactory.getInstance("X509").generateCertPath(certs);
	}

	/**
	 * Creates an X.509 certificate path containing a single certificate.
	 *
	 * @param cert certificate
	 * @return certificate path
	 * @throws Exception if the path cannot be created
	 */
	public static CertPath createX509CertPath(final X509Certificate cert) throws Exception {
		final List<Certificate> certs = new ArrayList<>();
		certs.add(cert);
		return CertificateFactory.getInstance("X509").generateCertPath(certs);
	}

	/**
	 * Check if "certificate" was certified by "trustedCertificates" (directly or by being one of them).
	 * No intermediate certificates are used and revocation is not checked.
	 *
	 * @param certificate certificate to check
	 * @param trustedCertificates trusted X.509 certificates
	 * @return true if a valid certificate path to one of the trusted certificates exists
	 * @throws Exception if the trusted certificates are invalid (e.g. empty)
	 */
	public static boolean verifyChainOfTrust(final X509Certificate certificate, final Collection<? extends Certificate> trustedCertificates) throws Exception {
		final X509CertSelector targetConstraints = new X509CertSelector();
		targetConstraints.setCertificate(certificate);

		final Set<TrustAnchor> trustAnchors = new HashSet<>();
		for (final Certificate trustedRootCert : trustedCertificates) {
			trustAnchors.add(new TrustAnchor((X509Certificate) trustedRootCert, null));
		}

		final PKIXBuilderParameters params = new PKIXBuilderParameters(trustAnchors, targetConstraints);
		// Revocation checking requires a CRL/OCSP data source (a CertStore added to params, or a
		// reachable CRLDistributionPoint/OCSP responder referenced by the certificate itself).
		// Since none is configured here, enabling it makes path building fail with
		// "Could not determine revocation status" for ANY certificate chain that doesn't carry
		// its own CRL/AIA extensions - including self-issued test/internal CA chains - even though
		// the chain of trust itself is perfectly valid. Disabled until proper revocation data
		// sources are wired in.
		params.setRevocationEnabled(false);
		try {
			final PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult) CertPathBuilder.getInstance("PKIX").build(params);
			return result != null;
		} catch (@SuppressWarnings("unused") final Exception cpbe) {
			return false;
		}
	}

	/**
	 * Check if "certificate" was certified by "trustedCertificates" (directly or by being one of them).
	 * No intermediate certificates are used and revocation is not checked.
	 *
	 * @param certificate certificate to check
	 * @param trustedCertificates trusted X.509 certificates
	 * @return true if a valid certificate path to one of the trusted certificates exists
	 * @throws Exception if the trusted certificates are invalid (e.g. empty)
	 */
	public static boolean verifyChainOfTrust(final X509Certificate certificate, final Certificate... trustedCertificates) throws Exception {
		final X509CertSelector targetConstraints = new X509CertSelector();
		targetConstraints.setCertificate(certificate);

		final Set<TrustAnchor> trustAnchors = new HashSet<>();
		for (final Certificate trustedRootCert : trustedCertificates) {
			trustAnchors.add(new TrustAnchor((X509Certificate) trustedRootCert, null));
		}

		final PKIXBuilderParameters params = new PKIXBuilderParameters(trustAnchors, targetConstraints);
		// See the comment in the Collection<Certificate> overload above: revocation checking is
		// disabled because no CRL/OCSP data source is configured, and would otherwise fail path
		// building for any chain lacking its own CRL/AIA extensions.
		params.setRevocationEnabled(false);
		try {
			final PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult) CertPathBuilder.getInstance("PKIX").build(params);
			return result != null;
		} catch (@SuppressWarnings("unused") final Exception cpbe) {
			return false;
		}
	}

	/**
	 * Returns the last certificate of each certificate chain contained in a concatenated list of chains (as returned by
	 * {@link java.util.jar.JarEntry#getCertificates()}). A chain ends where the next certificate is not the issuer of the current one.
	 *
	 * @param certificates concatenated X.509 certificate chains, must not be empty
	 * @return last certificate of each chain
	 */
	public static X509Certificate[] getChainRootCertificates(final Certificate[] certificates) {
		final Vector<X509Certificate> result = new Vector<>();
		for (int i = 0; i < certificates.length - 1; i++) {
			if (!((X509Certificate) certificates[i + 1]).getSubjectX500Principal().equals(((X509Certificate) certificates[i]).getIssuerX500Principal())) {
				result.addElement((X509Certificate) certificates[i]);
			}
		}
		// The final entry in the certificates array is always a root certificate
		result.addElement((X509Certificate) certificates[certificates.length - 1]);
		final X509Certificate[] returnValue = new X509Certificate[result.size()];
		result.copyInto(returnValue);
		return returnValue;
	}

	/**
	 * Reads all X.509 certificates from a PEM or DER stream.
	 *
	 * @param pemInputStream certificate data
	 * @return certificates
	 * @throws Exception if the data cannot be parsed
	 */
	public static Collection<? extends X509Certificate> loadCertificatesFromPemStream(final InputStream pemInputStream) throws Exception {
		final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
		@SuppressWarnings("unchecked")
		final Collection<? extends X509Certificate> certificates = (Collection<? extends X509Certificate>) certificateFactory.generateCertificates(pemInputStream);
		return certificates;
	}

	/**
	 * Returns the attributes of an X.500 principal (e.g. "CN", "O") as case insensitive map.
	 *
	 * @param x500Principal principal
	 * @return attribute values by attribute name
	 * @throws Exception if the principal string cannot be parsed
	 */
	public static CaseInsensitiveMap<String> getValuesFromX500Principal(final X500Principal x500Principal) throws Exception {
		return new CaseInsensitiveMap<>(MapStringReader.readMap(x500Principal.toString().replace("'", "\\'")));
	}

	/**
	 * Checks whether a certificate is a CA certificate (has the basic constraints CA flag).
	 *
	 * @param certificate certificate
	 * @return true for a CA certificate
	 */
	public static boolean checkForCaCertificate(final X509Certificate certificate) {
		return certificate.getBasicConstraints() >= 0;
	}

	/**
	 * Returns the MD5 fingerprint of the DER encoding of a certificate (lower case hex without separators).
	 *
	 * @param certificate certificate
	 * @return fingerprint
	 * @throws CertificateEncodingException if the certificate cannot be encoded
	 * @throws NoSuchAlgorithmException if the hash algorithm is not available
	 */
	public static String getMd5FingerPrint(final X509Certificate certificate) throws CertificateEncodingException, NoSuchAlgorithmException {
		final MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(certificate.getEncoded());
		return BitUtilities.toHexString(md.digest());
	}

	/**
	 * Returns the SHA-1 fingerprint of the DER encoding of a certificate (lower case hex without separators).
	 *
	 * @param certificate certificate
	 * @return fingerprint
	 * @throws CertificateEncodingException if the certificate cannot be encoded
	 * @throws NoSuchAlgorithmException if the hash algorithm is not available
	 */
	public static String getSha1FingerPrint(final X509Certificate certificate) throws CertificateEncodingException, NoSuchAlgorithmException {
		final MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(certificate.getEncoded());
		return BitUtilities.toHexString(md.digest());
	}

	/**
	 * Returns the SHA-1 fingerprint of the DER encoding of a certificate (Base64 encoded).
	 *
	 * @param certificate certificate
	 * @return fingerprint
	 * @throws CertificateEncodingException if the certificate cannot be encoded
	 * @throws NoSuchAlgorithmException if the hash algorithm is not available
	 */
	public static String getSha1FingerPrintBase64(final X509Certificate certificate) throws CertificateEncodingException, NoSuchAlgorithmException {
		final MessageDigest md = MessageDigest.getInstance("SHA-1");
		md.update(certificate.getEncoded());
		return Base64.getEncoder().encodeToString(md.digest());
	}

	/**
	 * Returns the SHA-256 fingerprint of the DER encoding of a certificate (lower case hex without separators).
	 *
	 * @param certificate certificate
	 * @return fingerprint
	 * @throws CertificateEncodingException if the certificate cannot be encoded
	 * @throws NoSuchAlgorithmException if the hash algorithm is not available
	 */
	public static String getSha256FingerPrint(final X509Certificate certificate) throws CertificateEncodingException, NoSuchAlgorithmException {
		final MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(certificate.getEncoded());
		return BitUtilities.toHexString(md.digest());
	}

	/**
	 * Returns the SHA-256 fingerprint of the DER encoding of a certificate (Base64 encoded).
	 *
	 * @param certificate certificate
	 * @return fingerprint
	 * @throws CertificateEncodingException if the certificate cannot be encoded
	 * @throws NoSuchAlgorithmException if the hash algorithm is not available
	 */
	public static String getSha256FingerPrintBase64(final X509Certificate certificate) throws CertificateEncodingException, NoSuchAlgorithmException {
		final MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(certificate.getEncoded());
		return Base64.getEncoder().encodeToString(md.digest());
	}

	/**
	 * Returns the SHA-384 fingerprint of the DER encoding of a certificate (lower case hex without separators).
	 *
	 * @param certificate certificate
	 * @return fingerprint
	 * @throws CertificateEncodingException if the certificate cannot be encoded
	 * @throws NoSuchAlgorithmException if the hash algorithm is not available
	 */
	public static String getSha384FingerPrint(final X509Certificate certificate) throws CertificateEncodingException, NoSuchAlgorithmException {
		final MessageDigest md = MessageDigest.getInstance("SHA-384");
		md.update(certificate.getEncoded());
		return BitUtilities.toHexString(md.digest());
	}

	/**
	 * Returns the SHA-384 fingerprint of the DER encoding of a certificate (Base64 encoded).
	 *
	 * @param certificate certificate
	 * @return fingerprint
	 * @throws CertificateEncodingException if the certificate cannot be encoded
	 * @throws NoSuchAlgorithmException if the hash algorithm is not available
	 */
	public static String getSha384FingerPrintBase64(final X509Certificate certificate) throws CertificateEncodingException, NoSuchAlgorithmException {
		final MessageDigest md = MessageDigest.getInstance("SHA-384");
		md.update(certificate.getEncoded());
		return Base64.getEncoder().encodeToString(md.digest());
	}

	/**
	 * Returns the SHA-512 fingerprint of the DER encoding of a certificate (lower case hex without separators).
	 *
	 * @param certificate certificate
	 * @return fingerprint
	 * @throws CertificateEncodingException if the certificate cannot be encoded
	 * @throws NoSuchAlgorithmException if the hash algorithm is not available
	 */
	public static String getSha512FingerPrint(final X509Certificate certificate) throws CertificateEncodingException, NoSuchAlgorithmException {
		final MessageDigest md = MessageDigest.getInstance("SHA-512");
		md.update(certificate.getEncoded());
		return BitUtilities.toHexString(md.digest());
	}

	/**
	 * Returns the SHA-512 fingerprint of the DER encoding of a certificate (Base64 encoded).
	 *
	 * @param certificate certificate
	 * @return fingerprint
	 * @throws CertificateEncodingException if the certificate cannot be encoded
	 * @throws NoSuchAlgorithmException if the hash algorithm is not available
	 */
	public static String getSha512FingerPrintBase64(final X509Certificate certificate) throws CertificateEncodingException, NoSuchAlgorithmException {
		final MessageDigest md = MessageDigest.getInstance("SHA-512");
		md.update(certificate.getEncoded());
		return Base64.getEncoder().encodeToString(md.digest());
	}

	/**
	 * Returns the OpenSSH public key blob used for fingerprints of a public key or of a RSA private CRT key.
	 *
	 * @param key public key or RSA private CRT key
	 * @return OpenSSH public key blob, or null for other private keys
	 * @throws Exception if the public key type is not supported
	 */
	public static byte[] getOpenSshFingerPrintData(final Key key) throws Exception {
		if (key instanceof PublicKey) {
			return KeyPairUtilities.getPublicKeyBytes((PublicKey) key);
		} else if (key instanceof RSAPrivateCrtKey) {
			final RSAPrivateCrtKey rsaPrivateCrtKey = (RSAPrivateCrtKey) key;
			final byte[] n = rsaPrivateCrtKey.getModulus().toByteArray();
			final byte[] e = rsaPrivateCrtKey.getPublicExponent().toByteArray();
			final byte[] tag = "ssh-rsa".getBytes(StandardCharsets.UTF_8);
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			final DataOutputStream dos = new DataOutputStream(os);
			dos.writeInt(tag.length);
			dos.write(tag);
			dos.writeInt(e.length);
			dos.write(e);
			dos.writeInt(n.length);
			dos.write(n);
			return os.toByteArray();
		} else {
			return null;
		}
	}

	/**
	 * Returns the OpenSSH MD5 fingerprint of a key as hex string.
	 *
	 * @param key public key or RSA private CRT key
	 * @param byteSeparator separator between the hex encoded bytes, e.g. ":", or null for none
	 * @return fingerprint, or "No public key available" for unsupported private keys
	 * @throws Exception if the key type is not supported
	 */
	public static String getOpenSshFingerPrintMd5(final Key key, final String byteSeparator) throws Exception {
		final byte[] openSshFingerPrintData = getOpenSshFingerPrintData(key);
		if (openSshFingerPrintData == null) {
			return "No public key available";
		} else {
			final MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(openSshFingerPrintData);
			return BitUtilities.toHexString(md.digest(), byteSeparator);
		}
	}

	/**
	 * Returns the OpenSSH MD5 fingerprint of a key Base64 encoded.
	 *
	 * @param key public key or RSA private CRT key
	 * @return fingerprint, or "No public key available" for unsupported private keys
	 * @throws Exception if the key type is not supported
	 */
	public static String getOpenSshFingerPrintMd5Base64(final Key key) throws Exception {
		final byte[] openSshFingerPrintData = getOpenSshFingerPrintData(key);
		if (openSshFingerPrintData == null) {
			return "No public key available";
		} else {
			final MessageDigest md = MessageDigest.getInstance("MD5");
			md.update(openSshFingerPrintData);
			return Base64.getEncoder().encodeToString(md.digest());
		}
	}

	/**
	 * Returns the OpenSSH SHA-1 fingerprint of a key as hex string.
	 *
	 * @param key public key or RSA private CRT key
	 * @param byteSeparator separator between the hex encoded bytes, e.g. ":", or null for none
	 * @return fingerprint, or "No public key available" for unsupported private keys
	 * @throws Exception if the key type is not supported
	 */
	public static String getOpenSshFingerPrintSha1(final Key key, final String byteSeparator) throws Exception {
		final byte[] openSshFingerPrintData = getOpenSshFingerPrintData(key);
		if (openSshFingerPrintData == null) {
			return "No public key available";
		} else {
			final MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(openSshFingerPrintData);
			return BitUtilities.toHexString(md.digest(), byteSeparator);
		}
	}

	/**
	 * Returns the OpenSSH SHA-1 fingerprint of a key Base64 encoded.
	 *
	 * @param key public key or RSA private CRT key
	 * @return fingerprint, or "No public key available" for unsupported private keys
	 * @throws Exception if the key type is not supported
	 */
	public static String getOpenSshFingerPrintSha1Base64(final Key key) throws Exception {
		final byte[] openSshFingerPrintData = getOpenSshFingerPrintData(key);
		if (openSshFingerPrintData == null) {
			return "No public key available";
		} else {
			final MessageDigest md = MessageDigest.getInstance("SHA-1");
			md.update(openSshFingerPrintData);
			return Base64.getEncoder().encodeToString(md.digest());
		}
	}

	/**
	 * Returns the OpenSSH SHA-256 fingerprint of a key as hex string.
	 *
	 * @param key public key or RSA private CRT key
	 * @param byteSeparator separator between the hex encoded bytes, e.g. ":", or null for none
	 * @return fingerprint, or "No public key available" for unsupported private keys
	 * @throws Exception if the key type is not supported
	 */
	public static String getOpenSshFingerPrintSha256(final Key key, final String byteSeparator) throws Exception {
		final byte[] openSshFingerPrintData = getOpenSshFingerPrintData(key);
		if (openSshFingerPrintData == null) {
			return "No public key available";
		} else {
			final MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(openSshFingerPrintData);
			return BitUtilities.toHexString(md.digest(), byteSeparator);
		}
	}

	/**
	 * Returns the OpenSSH SHA-256 fingerprint of a key Base64 encoded.
	 *
	 * @param key public key or RSA private CRT key
	 * @return fingerprint, or "No public key available" for unsupported private keys
	 * @throws Exception if the key type is not supported
	 */
	public static String getOpenSshFingerPrintSha256Base64(final Key key) throws Exception {
		final byte[] openSshFingerPrintData = getOpenSshFingerPrintData(key);
		if (openSshFingerPrintData == null) {
			return "No public key available";
		} else {
			final MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(openSshFingerPrintData);
			return Base64.getEncoder().encodeToString(md.digest());
		}
	}

	/**
	 * Returns the OpenSSH SHA-384 fingerprint of a key as hex string.
	 *
	 * @param key public key or RSA private CRT key
	 * @param byteSeparator separator between the hex encoded bytes, e.g. ":", or null for none
	 * @return fingerprint, or "No public key available" for unsupported private keys
	 * @throws Exception if the key type is not supported
	 */
	public static String getOpenSshFingerPrintSha384(final Key key, final String byteSeparator) throws Exception {
		final byte[] openSshFingerPrintData = getOpenSshFingerPrintData(key);
		if (openSshFingerPrintData == null) {
			return "No public key available";
		} else {
			final MessageDigest md = MessageDigest.getInstance("SHA-384");
			md.update(openSshFingerPrintData);
			return BitUtilities.toHexString(md.digest(), byteSeparator);
		}
	}

	/**
	 * Returns the OpenSSH SHA-384 fingerprint of a key Base64 encoded.
	 *
	 * @param key public key or RSA private CRT key
	 * @return fingerprint, or "No public key available" for unsupported private keys
	 * @throws Exception if the key type is not supported
	 */
	public static String getOpenSshFingerPrintSha384Base64(final Key key) throws Exception {
		final byte[] openSshFingerPrintData = getOpenSshFingerPrintData(key);
		if (openSshFingerPrintData == null) {
			return "No public key available";
		} else {
			final MessageDigest md = MessageDigest.getInstance("SHA-384");
			md.update(openSshFingerPrintData);
			return Base64.getEncoder().encodeToString(md.digest());
		}
	}

	/**
	 * Returns the OpenSSH SHA-512 fingerprint of a key as hex string.
	 *
	 * @param key public key or RSA private CRT key
	 * @param byteSeparator separator between the hex encoded bytes, e.g. ":", or null for none
	 * @return fingerprint, or "No public key available" for unsupported private keys
	 * @throws Exception if the key type is not supported
	 */
	public static String getOpenSshFingerPrintSha512(final Key key, final String byteSeparator) throws Exception {
		final byte[] openSshFingerPrintData = getOpenSshFingerPrintData(key);
		if (openSshFingerPrintData == null) {
			return "No public key available";
		} else {
			final MessageDigest md = MessageDigest.getInstance("SHA-512");
			md.update(openSshFingerPrintData);
			return BitUtilities.toHexString(md.digest(), byteSeparator);
		}
	}

	/**
	 * Returns the OpenSSH SHA-512 fingerprint of a key Base64 encoded.
	 *
	 * @param key public key or RSA private CRT key
	 * @return fingerprint, or "No public key available" for unsupported private keys
	 * @throws Exception if the key type is not supported
	 */
	public static String getOpenSshFingerPrintSha512Base64(final Key key) throws Exception {
		final byte[] openSshFingerPrintData = getOpenSshFingerPrintData(key);
		if (openSshFingerPrintData == null) {
			return "No public key available";
		} else {
			final MessageDigest md = MessageDigest.getInstance("SHA-512");
			md.update(openSshFingerPrintData);
			return Base64.getEncoder().encodeToString(md.digest());
		}
	}

	/**
	 * Converts a PEM key pair into a JCE key pair.
	 *
	 * @param keyPair PEM key pair with private and public key info
	 * @return JCE key pair
	 * @throws PEMException if the key pair cannot be converted
	 */
	public static KeyPair convertPEMKeyPairToKeyPair(final PEMKeyPair keyPair) throws PEMException {
		try {
			String algorithm = keyPair.getPrivateKeyInfo().getPrivateKeyAlgorithm().getAlgorithm().getId();
			if (X9ObjectIdentifiers.id_ecPublicKey.getId().equals(algorithm)) {
				algorithm = "ECDSA";
			}

			final KeyFactory keyFactory = new DefaultJcaJceHelper().createKeyFactory(algorithm);

			return new KeyPair(
					keyFactory.generatePublic(new X509EncodedKeySpec(keyPair.getPublicKeyInfo().getEncoded())),
					keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyPair.getPrivateKeyInfo().getEncoded())));
		} catch (final Exception e) {
			throw new PEMException("Unable to convert key pair: " + e.getMessage(), e);
		}
	}

	/**
	 * Reads the first private key from PEM data (traditional OpenSSL or PKCS#8 format, encrypted or not).
	 *
	 * @param keyDataString PEM data
	 * @param password password of an encrypted key, null or empty for an unencrypted key
	 * @return private key
	 * @throws MissingPasswordException if the key is encrypted and no password is given
	 * @throws WrongPasswordException if the password of a PKCS#8 key is wrong
	 * @throws Exception if no private key is found, a password is given for an unencrypted key or the key cannot be read
	 */
	public static PrivateKey getPrivateKeyFromString(final String keyDataString, final char[] password) throws Exception {
		try (PEMParser pemParser = new PEMParser(new StringReader(keyDataString))) {
			Security.addProvider(new BouncyCastleProvider());

			Object object;
			while ((object = pemParser.readObject()) != null) {
				if (object instanceof PEMKeyPair) {
					final PEMKeyPair pemKeyPair = (PEMKeyPair) object;
					if (password == null || password.length == 0) {
						final JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
						final KeyPair keyPair = converter.getKeyPair(pemKeyPair);
						return keyPair.getPrivate();
					} else {
						throw new Exception("Unencrypted private key found. Password is obsolete.");
					}
				} else if (object instanceof PEMEncryptedKeyPair) {
					if (password == null || password.length == 0) {
						throw new MissingPasswordException("Encrypted private key found. Password is needed.");
					} else {
						final JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
						final PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder().build(password);
						final KeyPair keyPair = converter.getKeyPair(((PEMEncryptedKeyPair) object).decryptKeyPair(decProv));
						return keyPair.getPrivate();
					}
				} else if (object instanceof PrivateKeyInfo) {
					if (password == null || password.length == 0) {
						final JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
						return converter.getPrivateKey((PrivateKeyInfo) object);
					} else {
						throw new Exception("Unencrypted private key found. Password is obsolete.");
					}
				} else if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
					if (password == null || password.length == 0) {
						throw new MissingPasswordException("Encrypted private key (PKCS8) found. Password is needed.");
					} else {
						try {
							final JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
							final InputDecryptorProvider decryptionProv = new JceOpenSSLPKCS8DecryptorProviderBuilder().build(password);
							final PrivateKeyInfo keyInfo = ((PKCS8EncryptedPrivateKeyInfo) object).decryptPrivateKeyInfo(decryptionProv);
							return converter.getPrivateKey(keyInfo);
						} catch (final Exception e) {
							if (e instanceof PKCSException && e.getCause() != null && e.getCause() instanceof InvalidCipherTextIOException) {
								throw new WrongPasswordException();
							} else {
								throw e;
							}
						}
					}
				}
			}
			throw new Exception("No private key object found in data");
		} catch (final MissingPasswordException mpe) {
			throw mpe;
		} catch (final WrongPasswordException wpe) {
			throw wpe;
		} catch (final Exception e) {
			throw new Exception("Cannot read private key: " + e.getMessage(), e);
		}
	}

	/**
	 * Reads the first public key from PEM data (public key or unencrypted key pair).
	 *
	 * @param keyDataString PEM data
	 * @return public key
	 * @throws Exception if no public key is found or it cannot be read
	 */
	public static PublicKey getPublicKeyFromString(final String keyDataString) throws Exception {
		try (PEMParser pemParser = new PEMParser(new StringReader(keyDataString))) {
			Security.addProvider(new BouncyCastleProvider());

			Object object;
			while ((object = pemParser.readObject()) != null) {
				if (object instanceof PEMKeyPair) {
					final JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME);
					final KeyPair keyPair = converter.getKeyPair((PEMKeyPair) object);
					return keyPair.getPublic();
				} else if (object instanceof SubjectPublicKeyInfo) {
					final JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
					return converter.getPublicKey((SubjectPublicKeyInfo) object);
				}
			}
			throw new Exception("No public key object found in data");
		} catch (final Exception e) {
			throw new Exception("Cannot read public key: " + e.getMessage(), e);
		}
	}

	/**
	 * Returns a human readable multi-line description of a certificate (names, validity, key length and fingerprints).
	 *
	 * @param certificate certificate
	 * @return description
	 * @throws Exception if the certificate data cannot be read
	 */
	public static String getX509CertificateInfo(final X509Certificate certificate) throws Exception {
		String dataOutput = "";
		dataOutput += "CN: " + getValuesFromX500Principal(certificate.getSubjectX500Principal()).get("CN");
		dataOutput += "\n";
		dataOutput += "Subject: " + certificate.getSubjectX500Principal();
		dataOutput += "\n";
		dataOutput += "Issuer: " + certificate.getIssuerX500Principal();
		dataOutput += "\n";
		dataOutput += "is CA-certificate: " + (CryptographicUtilities.checkForCaCertificate(certificate) ? "true" : "false");
		dataOutput += "\n";
		dataOutput += "Valid from: " + DateUtilities.formatDate(DateUtilities.YYYY_MM_DD_HHMMSS, certificate.getNotBefore());
		dataOutput += "\n";
		dataOutput += "Valid until: " + DateUtilities.formatDate(DateUtilities.YYYY_MM_DD_HHMMSS, certificate.getNotAfter());
		dataOutput += "\n";
		dataOutput += "Signature algorithm: " + certificate.getSigAlgName();
		dataOutput += "\n";
		dataOutput += "Type: " + certificate.getType();
		dataOutput += "\n";
		dataOutput += "Version: " + certificate.getVersion();
		dataOutput += "\n";
		dataOutput += "Serial: " + certificate.getSerialNumber();
		dataOutput += "\n";
		final PublicKey certificatePublicKey = certificate.getPublicKey();
		if (certificatePublicKey instanceof RSAKey) {
			dataOutput += "Key length: " + ((RSAKey) certificatePublicKey).getModulus().bitLength();
		} else if (certificatePublicKey instanceof ECKey) {
			dataOutput += "Key length: " + ((ECKey) certificatePublicKey).getParams().getOrder().bitLength();
		} else if (certificatePublicKey instanceof DSAKey && ((DSAKey) certificatePublicKey).getParams() != null) {
			dataOutput += "Key length: " + ((DSAKey) certificatePublicKey).getParams().getP().bitLength();
		} else {
			dataOutput += "Key algorithm: " + certificatePublicKey.getAlgorithm();
		}
		dataOutput += "\n";
		dataOutput += "MD5 fingerprint: " + CryptographicUtilities.getMd5FingerPrint(certificate);
		dataOutput += "\n";
		dataOutput += "SHA1 fingerprint: " + CryptographicUtilities.getSha1FingerPrintBase64(certificate);
		dataOutput += "\n";
		dataOutput += "SHA256 fingerprint: " + CryptographicUtilities.getSha256FingerPrintBase64(certificate);
		dataOutput += "\n";
		dataOutput += "SHA384 fingerprint: " + CryptographicUtilities.getSha384FingerPrintBase64(certificate);
		dataOutput += "\n";
		dataOutput += "SHA512 fingerprint: " + CryptographicUtilities.getSha512FingerPrintBase64(certificate);
		dataOutput += "\n";
		return dataOutput;
	}

	/**
	 * Returns a human readable multi-line description of a key (algorithm, type, size or curve and OpenSSH fingerprints).
	 *
	 * @param key key
	 * @return description
	 * @throws Exception if the key data cannot be read
	 */
	public static String getKeyInfo(final Key key) throws Exception {
		String dataOutput = "";
		dataOutput += "Algorithm: " + key.getAlgorithm();
		dataOutput += "\n";

		if (key instanceof PrivateKey) {
			dataOutput += "Keytype: PrivateKey";
			dataOutput += "\n";
		} else if (key instanceof PublicKey) {
			dataOutput += "Keytype: PublicKey";
			dataOutput += "\n";
		}

		if (key instanceof RSAKey) {
			dataOutput += "Key length: " + ((RSAKey) key).getModulus().bitLength();
			dataOutput += "\n";
		} else if (key instanceof ECKey) {
			try {
				if (key instanceof PrivateKey) {
					dataOutput += "Elliptic Curve Name: " + getEllipticCurveName((PrivateKey) key);
					dataOutput += "\n";
				} else if (key instanceof PublicKey) {
					dataOutput += "Elliptic Curve Name: " + getEllipticCurveName((PublicKey) key);
					dataOutput += "\n";
				}
			} catch (@SuppressWarnings("unused") final Exception e) {
				dataOutput += "Elliptic Curve Name: Unknown";
				dataOutput += "\n";
			}
		}

		try {
			dataOutput += "MD5 fingerprint: " + CryptographicUtilities.getOpenSshFingerPrintMd5(key, null);
		} catch (@SuppressWarnings("unused") final NoSuchAlgorithmException e) {
			dataOutput += "MD5 fingerprint: Unknown";
		}
		dataOutput += "\n";

		try {
			dataOutput += "SHA1 fingerprint: " + CryptographicUtilities.getOpenSshFingerPrintSha1(key, null);
		} catch (@SuppressWarnings("unused") final NoSuchAlgorithmException e) {
			dataOutput += "SHA1 fingerprint: Unknown";
		}
		dataOutput += "\n";

		try {
			dataOutput += "SHA256 fingerprint: " + CryptographicUtilities.getOpenSshFingerPrintSha256(key, null);
		} catch (@SuppressWarnings("unused") final NoSuchAlgorithmException e) {
			dataOutput += "SHA256 fingerprint: Unknown";
		}
		dataOutput += "\n";

		try {
			dataOutput += "SHA384 fingerprint: " + CryptographicUtilities.getOpenSshFingerPrintSha384(key, null);
		} catch (@SuppressWarnings("unused") final NoSuchAlgorithmException e) {
			dataOutput += "SHA384 fingerprint: Unknown";
		}
		dataOutput += "\n";

		try {
			dataOutput += "SHA512 fingerprint: " + CryptographicUtilities.getOpenSshFingerPrintSha512(key, null);
		} catch (@SuppressWarnings("unused") final NoSuchAlgorithmException e) {
			dataOutput += "SHA512 fingerprint: Unknown";
		}
		dataOutput += "\n";

		return dataOutput;
	}

	/**
	 * Checks whether a private key belongs to a public key by signing random data and verifying the signature.
	 *
	 * @param privateKey private key (RSA, DSA, EC or EdDSA)
	 * @param publicKey public key
	 * @return true if the keys belong together
	 * @throws Exception if a key is missing or the keys are of incompatible types
	 */
	public static boolean checkPrivateKeyFitsPublicKey(final PrivateKey privateKey, final PublicKey publicKey) throws Exception {
		final Signature challengeSignature;
		if (privateKey == null) {
			throw new Exception("PrivateKey is missing");
		} else if (publicKey == null) {
			throw new Exception("PublicKey is missing");
		}

		final String privateKeyAlgorithm = privateKey.getAlgorithm().toLowerCase(Locale.ROOT);
		if (privateKeyAlgorithm.equals("dsa")) {
			challengeSignature = Signature.getInstance("SHA512withDSA");
		} else if (privateKeyAlgorithm.equals("ec") || privateKeyAlgorithm.equals("ecdsa")) {
			// JDK EC keys report "EC", BouncyCastle EC keys report "ECDSA"
			Security.addProvider(new BouncyCastleProvider());
			challengeSignature = Signature.getInstance("SHA512withECDSA", BouncyCastleProvider.PROVIDER_NAME);
		} else if (privateKeyAlgorithm.equals("eddsa")) {
			final EdECPublicKey publicEdDsaKey = (EdECPublicKey) publicKey;
			if ("Ed25519".equals(publicEdDsaKey.getParams().getName())) {
				challengeSignature = Signature.getInstance("Ed25519");
			} else if ("Ed448".equals(publicEdDsaKey.getParams().getName())) {
				challengeSignature = Signature.getInstance("Ed448");
			} else {
				throw new Exception("Unsupported EdDSA algorithm name: " + publicEdDsaKey.getParams().getName());
			}
		} else if (privateKeyAlgorithm.equals("ed25519")) {
			challengeSignature = Signature.getInstance("Ed25519");
		} else if (privateKeyAlgorithm.equals("ed448")) {
			challengeSignature = Signature.getInstance("Ed448");
		} else {
			challengeSignature = Signature.getInstance("SHA512withRSA");
		}

		final byte[] challenge = new byte[1024];
		ThreadLocalRandom.current().nextBytes(challenge);

		challengeSignature.initSign(privateKey);
		challengeSignature.update(challenge);
		final byte[] signature = challengeSignature.sign();

		challengeSignature.initVerify(publicKey);
		challengeSignature.update(challenge);

		return challengeSignature.verify(signature);
	}

	/**
	 * Finds the known signature method matching a name, ignoring case and the characters " ", "_", "/" and "-".
	 *
	 * @param signatureMethodName signature method name
	 * @return the known method name, or null if the name is unknown
	 */
	public static String checkSignatureMethodName(final String signatureMethodName) {
		for (final String signatureMethodNameItem : KNOWN_SIGNATURE_METHODS_RSA) {
			if (signatureMethodNameItem.replace(" ", "").replace("_", "").replace("/", "").replace("-", "").equalsIgnoreCase(signatureMethodName.replace(" ", "").replace("_", "").replace("/", "").replace("-", ""))) {
				return signatureMethodNameItem;
			}
		}
		for (final String signatureMethodNameItem : KNOWN_SIGNATURE_METHODS_DSA) {
			if (signatureMethodNameItem.replace(" ", "").replace("_", "").replace("/", "").replace("-", "").equalsIgnoreCase(signatureMethodName.replace(" ", "").replace("_", "").replace("/", "").replace("-", ""))) {
				return signatureMethodNameItem;
			}
		}
		for (final String signatureMethodNameItem : KNOWN_SIGNATURE_METHODS_EC) {
			if (signatureMethodNameItem.replace(" ", "").replace("_", "").replace("/", "").replace("-", "").equalsIgnoreCase(signatureMethodName.replace(" ", "").replace("_", "").replace("/", "").replace("-", ""))) {
				return signatureMethodNameItem;
			}
		}
		for (final String signatureMethodNameItem : KNOWN_SIGNATURE_METHODS_OTHERS) {
			if (signatureMethodNameItem.replace(" ", "").replace("_", "").replace("/", "").replace("-", "").equalsIgnoreCase(signatureMethodName.replace(" ", "").replace("_", "").replace("/", "").replace("-", ""))) {
				return signatureMethodNameItem;
			}
		}
		return null;
	}

	/**
	 * Returns the OID of a CMS encryption algorithm (a constant of {@link CMSAlgorithm}) by its name,
	 * ignoring case and the characters " ", "_", "/" and "-".
	 *
	 * @param encryptionMethodName name like "AES256_CBC"
	 * @return OID, or null if the name is unknown
	 */
	public static ASN1ObjectIdentifier getASN1ObjectIdentifierByEncryptionMethodName(final String encryptionMethodName) {
		try {
			for (final Field field : CMSAlgorithm.class.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers()) && field.getName().replace(" ", "").replace("_", "").replace("/", "").replace("-", "").equalsIgnoreCase(encryptionMethodName.replace(" ", "").replace("_", "").replace("/", "").replace("-", ""))) {
					return (ASN1ObjectIdentifier) field.get(encryptionMethodName);
				}
			}
			return null;
		} catch (@SuppressWarnings("unused") final Exception e) {
			return null;
		}
	}

	/**
	 * Checks whether a name matches a CMS encryption algorithm (a constant of {@link CMSAlgorithm}),
	 * ignoring case and the characters " ", "_", "/" and "-".
	 *
	 * @param encryptionMethodName name like "AES256_CBC"
	 * @return the given name if it is known, otherwise null
	 */
	public static String checkEncryptionMethodName(final String encryptionMethodName) {
		try {
			for (final Field field : CMSAlgorithm.class.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers()) && field.getName().replace(" ", "").replace("_", "").replace("/", "").replace("-", "").equalsIgnoreCase(encryptionMethodName.replace(" ", "").replace("_", "").replace("/", "").replace("-", ""))) {
					return encryptionMethodName;
				}
			}
			return null;
		} catch (@SuppressWarnings("unused") final Exception e) {
			return null;
		}
	}

	/**
	 * Escapes an attribute value for the use in a RFC 2253 distinguished name string, so that special characters
	 * like ',' or '=' in a value cannot inject additional name attributes.
	 *
	 * @param value attribute value
	 * @return escaped attribute value
	 */
	private static String escapeX500AttributeValue(final String value) {
		final StringBuilder escapedValue = new StringBuilder();
		for (int i = 0; i < value.length(); i++) {
			final char nextChar = value.charAt(i);
			if (",+\"\\<>;=".indexOf(nextChar) >= 0
					|| (i == 0 && (nextChar == '#' || nextChar == ' '))
					|| (i == value.length() - 1 && nextChar == ' ')) {
				escapedValue.append('\\');
			}
			escapedValue.append(nextChar);
		}
		return escapedValue.toString();
	}

	/**
	 * Generates an X.500 distinguished name string with the state as "ST". Special characters in the values are escaped.
	 *
	 * @param commonName common name (CN), mandatory
	 * @param organizationalUnit organizational unit (OU), null to omit
	 * @param organizationName organization (O), null to omit
	 * @param location locality (L), null to omit
	 * @param state state or province (ST), null to omit
	 * @param country country (C), null to omit
	 * @param email email address (EMAILADDRESS), null to omit
	 * @return distinguished name string
	 * @throws Exception if the common name is blank
	 */
	public static String generateLegacyX500PrincipalString(final String commonName, final String organizationalUnit, final String organizationName, final String location, final String state, final String country, final String email) throws Exception {
		final StringBuilder result = new StringBuilder();

		if (Utilities.isNotBlank(commonName)) {
			result.append("CN=").append(escapeX500AttributeValue(commonName));
		} else {
			throw new Exception("CommonName(CN) is mandatory for X500Principal");
		}

		if (organizationalUnit != null) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("OU=").append(escapeX500AttributeValue(organizationalUnit));
		}

		if (organizationName != null) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("O=").append(escapeX500AttributeValue(organizationName));
		}

		if (location != null) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("L=").append(escapeX500AttributeValue(location));
		}

		if (state != null) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("ST=").append(escapeX500AttributeValue(state));
		}

		if (country != null) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("C=").append(escapeX500AttributeValue(country));
		}

		if (email != null) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("EMAILADDRESS=").append(escapeX500AttributeValue(email));
		}

		return result.toString();
	}

	/**
	 * Generates an X.500 distinguished name string with the state as "S". Special characters in the values are escaped.
	 *
	 * @param commonName common name (CN), mandatory
	 * @param organizationalUnit organizational unit (OU), null to omit
	 * @param organizationName organization (O), null to omit
	 * @param location locality (L), null to omit
	 * @param state state or province (S), null to omit
	 * @param country country (C), null to omit
	 * @param email email address (EMAILADDRESS), null to omit
	 * @return distinguished name string
	 * @throws Exception if the common name is blank
	 */
	public static String generateX500PrincipalString(final String commonName, final String organizationalUnit, final String organizationName, final String location, final String state, final String country, final String email) throws Exception {
		final StringBuilder result = new StringBuilder();

		if (Utilities.isNotBlank(commonName)) {
			result.append("CN=").append(escapeX500AttributeValue(commonName));
		} else {
			throw new Exception("CommonName(CN) is mandatory for X500Principal");
		}

		if (organizationalUnit != null) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("OU=").append(escapeX500AttributeValue(organizationalUnit));
		}

		if (organizationName != null) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("O=").append(escapeX500AttributeValue(organizationName));
		}

		if (location != null) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("L=").append(escapeX500AttributeValue(location));
		}

		if (state != null) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("S=").append(escapeX500AttributeValue(state));
		}

		if (country != null) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("C=").append(escapeX500AttributeValue(country));
		}

		if (email != null) {
			if (result.length() > 0) {
				result.append(", ");
			}
			result.append("EMAILADDRESS=").append(escapeX500AttributeValue(email));
		}

		return result.toString();
	}

	/**
	 * Parses the known attributes (CN, OU, O, L, S, ST, C, E, EMAIL, EMAILADDRESS) of a distinguished name string.
	 *
	 * @param x500NameString distinguished name string
	 * @return attribute values in the order of the attribute list above, or null if the string cannot be parsed
	 */
	public static Map<String, String> parseX500PrincipalString(final String x500NameString) {
		try {
			final Map<String, String> dataMap = MapStringReader.readMap(x500NameString.replace("'", "\\'"));
			final LinkedHashMap<String, String> returnMap = new LinkedHashMap<>();
			for (final String keyName : new String[] { "CN", "OU", "O", "L", "S", "ST", "C", "E", "EMAIL", "EMAILADDRESS" }) {
				if (dataMap.containsKey(keyName)) {
					returnMap.put(keyName, dataMap.get(keyName));
				}
			}
			return returnMap;
		} catch (@SuppressWarnings("unused") final Exception e) {
			return null;
		}
	}

	/**
	 * Generates a PKCS#10 certification request signed with {@link #DEFAULT_SIGNATURE_METHOD_RSA}.
	 *
	 * @param privateKey private key to sign the request with
	 * @param publicKey public key to certify
	 * @param commonName common name (CN), mandatory
	 * @param organizationalUnit organizational unit, null to omit
	 * @param organizationName organization, null to omit
	 * @param location locality, null to omit
	 * @param state state or province, null to omit
	 * @param country country, null to omit
	 * @param email email address, null to omit
	 * @return certification request
	 * @throws Exception if the request cannot be created
	 */
	public static PKCS10CertificationRequest generatePKCS10CertificationRequest(final PrivateKey privateKey, final PublicKey publicKey, final String commonName, final String organizationalUnit, final String organizationName, final String location, final String state, final String country, final String email) throws Exception {
		return generatePKCS10CertificationRequest(privateKey, publicKey, commonName, organizationalUnit, organizationName, location, state, country, email, DEFAULT_SIGNATURE_METHOD_RSA);
	}

	/**
	 * Generates a PKCS#10 certification request.
	 *
	 * @param privateKey private key to sign the request with
	 * @param publicKey public key to certify
	 * @param commonName common name (CN), mandatory
	 * @param organizationalUnit organizational unit, null to omit
	 * @param organizationName organization, null to omit
	 * @param location locality, null to omit
	 * @param state state or province, null to omit
	 * @param country country, null to omit
	 * @param email email address, null to omit
	 * @param signatureMethod signature method, e.g. {@link #DEFAULT_SIGNATURE_METHOD_RSA}
	 * @return certification request
	 * @throws Exception if the request cannot be created
	 */
	public static PKCS10CertificationRequest generatePKCS10CertificationRequest(final PrivateKey privateKey, final PublicKey publicKey, final String commonName, final String organizationalUnit, final String organizationName, final String location, final String state, final String country, final String email, final String signatureMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final PKCS10CertificationRequestBuilder certificationRequestBuilder = new JcaPKCS10CertificationRequestBuilder(new X500Principal(generateX500PrincipalString(commonName, organizationalUnit, organizationName, location, state, country, email)), publicKey);
		final JcaContentSignerBuilder contentSignerBuilder = new JcaContentSignerBuilder(signatureMethod);
		final ContentSigner contentSigner = contentSignerBuilder.build(privateKey);
		final PKCS10CertificationRequest certificationRequest = certificationRequestBuilder.build(contentSigner);

		return certificationRequest;
	}

	/**
	 * Returns the PEM encoding of a certification request.
	 *
	 * @param certificationRequest certification request
	 * @return PEM string "-----BEGIN CERTIFICATE REQUEST-----..."
	 * @throws Exception if the request cannot be encoded
	 */
	public static String getStringFromCertificationRequest(final PKCS10CertificationRequest certificationRequest) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final StringWriter writer = new StringWriter();
		try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(writer)) {
			jcaPEMWriter.writeObject(certificationRequest);
		} catch (final Exception e) {
			throw new Exception("Cannot create certification signing request string: " + e.getMessage(), e);
		}
		return writer.toString();
	}

	/**
	 * Generates a PKCS#10 certification request signed with {@link #DEFAULT_SIGNATURE_METHOD_RSA} and returns its PEM encoding.
	 *
	 * @param privateKey private key to sign the request with
	 * @param publicKey public key to certify
	 * @param commonName common name (CN), mandatory
	 * @param organizationalUnit organizational unit, null to omit
	 * @param organizationName organization, null to omit
	 * @param location locality, null to omit
	 * @param state state or province, null to omit
	 * @param country country, null to omit
	 * @param email email address, null to omit
	 * @return PEM encoded certification request
	 * @throws Exception if the request cannot be created
	 */
	public static String generatePKCS10CertificationRequestString(final PrivateKey privateKey, final PublicKey publicKey, final String commonName, final String organizationalUnit, final String organizationName, final String location, final String state, final String country, final String email) throws Exception {
		final PKCS10CertificationRequest certificationRequest = generatePKCS10CertificationRequest(privateKey, publicKey, commonName, organizationalUnit, organizationName, location, state, country, email, DEFAULT_SIGNATURE_METHOD_RSA);
		final String csrDataString = CryptographicUtilities.getStringFromCertificationRequest(certificationRequest);
		return csrDataString;
	}

	/**
	 * Reads a certification request from PEM data.
	 *
	 * @param encodedCertificationRequest PEM data
	 * @return certification request, or null if the data is empty
	 * @throws Exception if the data cannot be read or contains no certification request
	 */
	public static PKCS10CertificationRequest getCertificationRequestFromString(final String encodedCertificationRequest) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		PKCS10CertificationRequest certificationRequest;
		try (PEMParser pemParser = new PEMParser(new StringReader(encodedCertificationRequest))) {
			certificationRequest = (PKCS10CertificationRequest) pemParser.readObject();
		} catch (final IOException e) {
			throw new Exception("Error in reading the certificate signing request: " + e.getMessage(), e);
		}
		return certificationRequest;
	}

	/**
	 * Returns the subject distinguished name of a PEM encoded certification request.
	 *
	 * @param csrDataString PEM data
	 * @return subject distinguished name
	 * @throws Exception if the data cannot be read
	 */
	public static String getCertificationRequestSubjectFromString(final String csrDataString) throws Exception {
		final PKCS10CertificationRequest certificationRequest = getCertificationRequestFromString(csrDataString);
		return certificationRequest.getSubject().toString();
	}

	/**
	 * Signs a certification request with a CA key using {@link #DEFAULT_SIGNATURE_METHOD_RSA}, see
	 * {@link #signPKCS10CertificateRequest(X509Certificate, PrivateKey, String, String, PKCS10CertificationRequest, int, BigInteger, int, String)}.
	 *
	 * @param caCertificate CA certificate of the issuer
	 * @param caPrivateKey private key of the CA
	 * @param caCrlEndpointUrl URL of the CA's CRL to add as CRL distribution point, blank to omit
	 * @param caOcspEndpointUrl URL of the CA's OCSP responder to add as authority information access, blank to omit
	 * @param certificationRequest certification request
	 * @param allowedSubCaCertificateLevels path length for a CA certificate (0 or more), or -1 for an end entity certificate
	 * @param serialNo serial number of the new certificate
	 * @param validityDays validity of the new certificate in days from now
	 * @return new certificate
	 * @throws Exception if the request is invalid or the CA may not issue the certificate
	 */
	public static X509Certificate signPKCS10CertificateRequest(final X509Certificate caCertificate, final PrivateKey caPrivateKey, final String caCrlEndpointUrl, final String caOcspEndpointUrl, final PKCS10CertificationRequest certificationRequest, final int allowedSubCaCertificateLevels, final BigInteger serialNo, final int validityDays) throws Exception {
		return signPKCS10CertificateRequest(caCertificate, caPrivateKey, caCrlEndpointUrl, caOcspEndpointUrl, certificationRequest, allowedSubCaCertificateLevels, serialNo, validityDays, DEFAULT_SIGNATURE_METHOD_RSA);
	}

	/**
	 * Signs a PEM encoded certification request with a CA key using {@link #DEFAULT_SIGNATURE_METHOD_RSA}.
	 *
	 * @param caCertificate CA certificate of the issuer
	 * @param caPrivateKey private key of the CA
	 * @param caCrlEndpointUrl URL of the CA's CRL to add as CRL distribution point, blank to omit
	 * @param caOcspEndpointUrl URL of the CA's OCSP responder to add as authority information access, blank to omit
	 * @param csrDataString PEM encoded certification request
	 * @param allowedSubCaCertificateLevels path length for a CA certificate (0 or more), or -1 for an end entity certificate
	 * @param serialNo serial number of the new certificate
	 * @param validityDays validity of the new certificate in days from now
	 * @return new certificate
	 * @throws Exception if the request is invalid or the CA may not issue the certificate
	 */
	public static X509Certificate signPKCS10CertificateRequestString(final X509Certificate caCertificate, final PrivateKey caPrivateKey, final String caCrlEndpointUrl, final String caOcspEndpointUrl, final String csrDataString, final int allowedSubCaCertificateLevels, final BigInteger serialNo, final int validityDays) throws Exception {
		return signPKCS10CertificateRequest(caCertificate, caPrivateKey, caCrlEndpointUrl, caOcspEndpointUrl, getCertificationRequestFromString(csrDataString), allowedSubCaCertificateLevels, serialNo, validityDays, DEFAULT_SIGNATURE_METHOD_RSA);
	}

	/**
	 * Signs a certification request with a CA key. The signature of the request is verified first (proof of possession).
	 * The new certificate gets key identifiers, basic constraints, key usage (including certificate and CRL signing for
	 * CA certificates) and the extended key usage "serverAuth".
	 *
	 * @param caCertificate CA certificate of the issuer
	 * @param caPrivateKey private key of the CA
	 * @param caCrlEndpointUrl URL of the CA's CRL to add as CRL distribution point, blank to omit
	 * @param caOcspEndpointUrl URL of the CA's OCSP responder to add as authority information access, blank to omit
	 * @param certificationRequest certification request
	 * @param allowedSubCaCertificateLevels path length for a CA certificate (0 or more), or -1 for an end entity certificate;
	 *        must be less than the path length of the CA certificate
	 * @param serialNo serial number of the new certificate
	 * @param validityDays validity of the new certificate in days from now
	 * @param signatureMethod signature method of the CA, e.g. {@link #DEFAULT_SIGNATURE_METHOD_RSA}
	 * @return new certificate
	 * @throws Exception if the request signature is invalid, the issuer is no CA or may not issue the certificate
	 */
	public static X509Certificate signPKCS10CertificateRequest(final X509Certificate caCertificate, final PrivateKey caPrivateKey, final String caCrlEndpointUrl, final String caOcspEndpointUrl, final PKCS10CertificationRequest certificationRequest, final int allowedSubCaCertificateLevels, final BigInteger serialNo, final int validityDays, final String signatureMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try {
			// Proof of possession: the request must be signed with the private key of the public key to be certified
			if (!certificationRequest.isSignatureValid(new JcaContentVerifierProviderBuilder().setProvider(BouncyCastleProvider.PROVIDER_NAME).build(certificationRequest.getSubjectPublicKeyInfo()))) {
				throw new Exception("Signature of the Certificate Signing Request is invalid");
			}

			if (caCertificate.getBasicConstraints() < 0) {
				throw new Exception("Given certificate is not a CA certificate and therefore may not sign a Certificate Signing Requests");
			} else if (allowedSubCaCertificateLevels > -1 && caCertificate.getBasicConstraints() - 1 < allowedSubCaCertificateLevels) {
				throw new Exception("Given CA certificate with BasicConstraints value '" + caCertificate.getBasicConstraints() + "' may not sign a Certificate Signing Requests for a CA Certificate with BasicConstraints value '" + allowedSubCaCertificateLevels + "'");
			}

			final LocalDateTime issuedDate = LocalDateTime.now();
			final LocalDateTime expiryDate = LocalDateTime.now().plusDays(validityDays);
			final JcaPKCS10CertificationRequest jcaRequest = new JcaPKCS10CertificationRequest(certificationRequest);
			final X509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
					caCertificate,
					serialNo,
					DateUtilities.getDateForLocalDateTime(issuedDate),
					DateUtilities.getDateForLocalDateTime(expiryDate),
					jcaRequest.getSubject(),
					jcaRequest.getPublicKey());
			final JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
			certificateBuilder.addExtension(Extension.authorityKeyIdentifier, false, extUtils.createAuthorityKeyIdentifier(caCertificate));
			certificateBuilder.addExtension(Extension.subjectKeyIdentifier, false, extUtils.createSubjectKeyIdentifier(jcaRequest.getPublicKey()));
			if (allowedSubCaCertificateLevels >= 0) {
				certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(allowedSubCaCertificateLevels));
			} else {
				certificateBuilder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
			}
			if (allowedSubCaCertificateLevels >= 0) {
				// A CA certificate must be allowed to sign certificates and CRLs, otherwise certificate path validation rejects it
				certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment | KeyUsage.keyCertSign | KeyUsage.cRLSign));
			} else {
				certificateBuilder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
			}
			certificateBuilder.addExtension(Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
			final ContentSigner signer = new JcaContentSignerBuilder(signatureMethod).setProvider(BouncyCastleProvider.PROVIDER_NAME).build(caPrivateKey);

			if (Utilities.isNotBlank(caCrlEndpointUrl)) {
				// Add CRL endpoint
				final DistributionPointName crlEp = new DistributionPointName(new GeneralNames(new GeneralName(GeneralName.uniformResourceIdentifier, caCrlEndpointUrl)));
				final DistributionPoint disPoint = new DistributionPoint(crlEp, null, null);
				certificateBuilder.addExtension(Extension.cRLDistributionPoints, false, new CRLDistPoint(new DistributionPoint[] { disPoint }));
			}

			if (Utilities.isNotBlank(caOcspEndpointUrl)) {
				// Add OCSP endpoint
				final AccessDescription ocsp = new AccessDescription(AccessDescription.id_ad_ocsp, new GeneralName(GeneralName.uniformResourceIdentifier, caOcspEndpointUrl));
				final ASN1EncodableVector authInfoAccessASN = new ASN1EncodableVector();
				authInfoAccessASN.add(ocsp);
				certificateBuilder.addExtension(Extension.authorityInfoAccess, false, new DERSequence(authInfoAccessASN));
			}

			final X509Certificate signedCert = new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certificateBuilder.build(signer));
			return signedCert;
		} catch (final Exception e) {
			throw new Exception("Error in signing the certificate", e);
		}
	}

	/**
	 * Checks whether a file contains a PEM certificate block.
	 *
	 * @param potentialCertificateFile file to check, may be null
	 * @return true if the file exists and contains a PEM certificate block
	 */
	public static boolean isCertificateFile(final File potentialCertificateFile) {
		if (potentialCertificateFile != null && potentialCertificateFile.exists()) {
			try {
				final String dataString = FileUtilities.readFileToString(potentialCertificateFile, StandardCharsets.UTF_8);

				final String pemBegin = "-----BEGIN CERTIFICATE-----";
				final String pemEnd = "-----END CERTIFICATE-----";

				if (dataString.contains(pemBegin) && dataString.contains(pemEnd) && dataString.indexOf(pemBegin) < dataString.indexOf(pemEnd)) {
					return true;
				} else {
					return false;
				}
			} catch (@SuppressWarnings("unused") final Exception e) {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Checks whether a file contains a PEM certification request block.
	 *
	 * @param potentialCertificationRequestFile file to check, may be null
	 * @return true if the file exists and contains a PEM certification request block
	 */
	public static boolean isCertificationRequestFile(final File potentialCertificationRequestFile) {
		if (potentialCertificationRequestFile != null && potentialCertificationRequestFile.exists()) {
			try {
				final String dataString = FileUtilities.readFileToString(potentialCertificationRequestFile, StandardCharsets.UTF_8);

				final String pemBegin = "-----BEGIN CERTIFICATE REQUEST-----";
				final String pemEnd = "-----END CERTIFICATE REQUEST-----";

				if (dataString.contains(pemBegin) && dataString.contains(pemEnd) && dataString.indexOf(pemBegin) < dataString.indexOf(pemEnd)) {
					return true;
				} else {
					return false;
				}
			} catch (@SuppressWarnings("unused") final Exception e) {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Checks whether a file contains a PEM private key block, see {@link #isPrivateKeyString(String)}.
	 *
	 * @param potentialPrivateKeyFile file to check, may be null
	 * @return true if the file exists and contains a PEM private key block
	 */
	public static boolean isPrivateKeyFile(final File potentialPrivateKeyFile) {
		if (potentialPrivateKeyFile != null && potentialPrivateKeyFile.exists()) {
			try {
				final String dataString = FileUtilities.readFileToString(potentialPrivateKeyFile, StandardCharsets.UTF_8);
				return isPrivateKeyString(dataString);
			} catch (@SuppressWarnings("unused") final Exception e) {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Checks whether a string contains a PEM private key block (RSA, EC, PKCS#8, encrypted PKCS#8 or OpenSSH format).
	 *
	 * @param potentialPrivateKeyString string to check
	 * @return true if a private key block is contained
	 */
	public static boolean isPrivateKeyString(final String potentialPrivateKeyString) {
		try {
			final String pemRsaBegin = "-----BEGIN RSA PRIVATE KEY-----";
			final String pemRsaEnd = "-----END RSA PRIVATE KEY-----";

			final String pemEcBegin = "-----BEGIN EC PRIVATE KEY-----";
			final String pemEcEnd = "-----END EC PRIVATE KEY-----";

			final String pemBegin = "-----BEGIN PRIVATE KEY-----";
			final String pemEnd = "-----END PRIVATE KEY-----";

			final String pemOpenSshBegin = "-----BEGIN OPENSSH PRIVATE KEY-----";
			final String pemOpenSshEnd = "-----END OPENSSH PRIVATE KEY-----";

			final String pemEncryptedBegin = "-----BEGIN ENCRYPTED PRIVATE KEY-----";
			final String pemEncryptedEnd = "-----END ENCRYPTED PRIVATE KEY-----";

			if (potentialPrivateKeyString.contains(pemRsaBegin) && potentialPrivateKeyString.contains(pemRsaEnd) && potentialPrivateKeyString.indexOf(pemRsaBegin) < potentialPrivateKeyString.indexOf(pemRsaEnd)) {
				return true;
			} else if (potentialPrivateKeyString.contains(pemEcBegin) && potentialPrivateKeyString.contains(pemEcEnd) && potentialPrivateKeyString.indexOf(pemEcBegin) < potentialPrivateKeyString.indexOf(pemEcEnd)) {
				return true;
			} else if (potentialPrivateKeyString.contains(pemBegin) && potentialPrivateKeyString.contains(pemEnd) && potentialPrivateKeyString.indexOf(pemBegin) < potentialPrivateKeyString.indexOf(pemEnd)) {
				return true;
			} else if (potentialPrivateKeyString.contains(pemOpenSshBegin) && potentialPrivateKeyString.contains(pemOpenSshEnd) && potentialPrivateKeyString.indexOf(pemOpenSshBegin) < potentialPrivateKeyString.indexOf(pemOpenSshEnd)) {
				return true;
			} else if (potentialPrivateKeyString.contains(pemEncryptedBegin) && potentialPrivateKeyString.contains(pemEncryptedEnd) && potentialPrivateKeyString.indexOf(pemEncryptedBegin) < potentialPrivateKeyString.indexOf(pemEncryptedEnd)) {
				return true;
			} else {
				return false;
			}
		} catch (@SuppressWarnings("unused") final Exception e) {
			return false;
		}
	}

	/**
	 * Checks whether a file contains a PEM public key block, see {@link #isPublicKeyString(String)}.
	 *
	 * @param potentialPublicKeyFile file to check, may be null
	 * @return true if the file exists and contains a PEM public key block
	 */
	public static boolean isPublicKeyFile(final File potentialPublicKeyFile) {
		if (potentialPublicKeyFile != null && potentialPublicKeyFile.exists()) {
			try {
				final String potentialPublicKeyString = FileUtilities.readFileToString(potentialPublicKeyFile, StandardCharsets.UTF_8);
				return isPublicKeyString(potentialPublicKeyString);
			} catch (@SuppressWarnings("unused") final Exception e) {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Checks whether a string contains a PEM public key block (RSA or X.509 SubjectPublicKeyInfo format).
	 *
	 * @param potentialPublicKeyString string to check
	 * @return true if a public key block is contained
	 */
	public static boolean isPublicKeyString(final String potentialPublicKeyString) {
		try {
			final String pemRsaBegin = "-----BEGIN RSA PUBLIC KEY-----";
			final String pemRsaEnd = "-----END RSA PUBLIC KEY-----";

			final String pemBegin = "-----BEGIN PUBLIC KEY-----";
			final String pemEnd = "-----END PUBLIC KEY-----";

			if (potentialPublicKeyString.contains(pemRsaBegin) && potentialPublicKeyString.contains(pemRsaEnd) && potentialPublicKeyString.indexOf(pemRsaBegin) < potentialPublicKeyString.indexOf(pemRsaEnd)) {
				return true;
			} else if (potentialPublicKeyString.contains(pemBegin) && potentialPublicKeyString.contains(pemEnd) && potentialPublicKeyString.indexOf(pemBegin) < potentialPublicKeyString.indexOf(pemEnd)) {
				return true;
			} else {
				return false;
			}
		} catch (@SuppressWarnings("unused") final Exception e) {
			return false;
		}
	}

	/**
	 * Checks whether a file can be loaded as key store of the default type (without password).
	 *
	 * @param potentialKeyStoreFile file to check, may be null
	 * @return true if the file can be loaded as key store
	 */
	public static boolean isKeyStoreFile(final File potentialKeyStoreFile) {
		if (potentialKeyStoreFile != null && potentialKeyStoreFile.exists()) {
			try (InputStream keyStoreInputStream = new FileInputStream(potentialKeyStoreFile)) {
				final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
				keyStore.load(keyStoreInputStream, null);
				return true;
			} catch (@SuppressWarnings("unused") final Exception e) {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Checks whether a file starts with the magic number of the JKS key store format.
	 *
	 * @param potentialJavaKeyStoreFile file to check, may be null
	 * @return true for a JKS file
	 */
	public static boolean isJavaKeyStoreFile(final File potentialJavaKeyStoreFile) {
		if (potentialJavaKeyStoreFile != null && potentialJavaKeyStoreFile.exists()) {
			final byte[] firstBytes = new byte[4];
			try (InputStream keyStoreInputStream = new FileInputStream(potentialJavaKeyStoreFile)) {
				if (keyStoreInputStream.read(firstBytes) == 4) {
					// Magic file numbers "FE ED FE ED"
					return firstBytes[0] == -2 && firstBytes[1] == -19 && firstBytes[2] == -2 && firstBytes[3] == -19;
				} else {
					return false;
				}
			} catch (@SuppressWarnings("unused") final Exception e) {
				return false;
			}
		} else {
			return false;
		}
	}

	/**
	 * Converts a key store file into another key store type (e.g. "JKS" to "PKCS12"). Key entries and trusted certificate
	 * entries are copied, the keys are protected with the destination key store password.
	 *
	 * @param sourceKeyStoreFile key store file to read
	 * @param sourceKeyStoreType type of the source key store
	 * @param sourceKeyStorePassword password of the source key store and its keys
	 * @param destinationKeyStoreFile key store file to write
	 * @param destinationKeyStoreType type of the destination key store
	 * @param destinationKeyStorePassword password of the destination key store and its keys
	 * @throws Exception if the key stores cannot be read or written
	 */
	public static void convertKeyStoreFileType(final File sourceKeyStoreFile, final String sourceKeyStoreType, final char[] sourceKeyStorePassword, final File destinationKeyStoreFile, final String destinationKeyStoreType, final char[] destinationKeyStorePassword) throws Exception {
		try {
			final KeyStore sourceKeyStore = KeyStore.getInstance(sourceKeyStoreType.toUpperCase(Locale.ROOT));
			try (FileInputStream sourceKeyStoreInputStream = new FileInputStream(sourceKeyStoreFile)) {
				sourceKeyStore.load(sourceKeyStoreInputStream, sourceKeyStorePassword);
			}

			final KeyStore destinationKeyStore = KeyStore.getInstance(destinationKeyStoreType.toUpperCase(Locale.ROOT));
			destinationKeyStore.load(null, null);

			for (final String alias : Utilities.asSortedList(Collections.list(sourceKeyStore.aliases()))) {
				if (sourceKeyStore.isKeyEntry(alias)) {
					final java.security.cert.Certificate[] chain = sourceKeyStore.getCertificateChain(alias);
					final java.security.Key key = sourceKeyStore.getKey(alias, sourceKeyStorePassword);
					destinationKeyStore.setKeyEntry(alias, key, destinationKeyStorePassword, chain);
				} else if (sourceKeyStore.isCertificateEntry(alias)) {
					// Trusted certificate entries must not get lost by the conversion
					destinationKeyStore.setCertificateEntry(alias, sourceKeyStore.getCertificate(alias));
				}
			}

			try (FileOutputStream destinationKeyStoreOutputStream = new FileOutputStream(destinationKeyStoreFile)) {
				destinationKeyStore.store(destinationKeyStoreOutputStream, destinationKeyStorePassword);
			}
		} catch (final Exception e) {
			throw new Exception("Cannot convert KeyStore file: " + e.getMessage(), e);
		}
	}

	/**
	 * Derives the public key from a RSA CRT or named curve elliptic curve private key.
	 *
	 * @param privateKey private key
	 * @return public key
	 * @throws Exception if the key is null or of another type
	 */
	public static PublicKey getPublicKeyFromPrivateKey(final PrivateKey privateKey) throws Exception {
		if (privateKey == null) {
			throw new Exception("Cannot extract PublicKey from empty PrivateKey");
		} else if (privateKey instanceof RSAPrivateCrtKey) {
			final RSAPublicKeySpec publicKeySpec = new RSAPublicKeySpec(((RSAPrivateCrtKey) privateKey).getModulus(), ((RSAPrivateCrtKey) privateKey).getPublicExponent());
			final KeyFactory keyFactory = KeyFactory.getInstance("RSA");
			return keyFactory.generatePublic(publicKeySpec);
		} else if (privateKey instanceof org.bouncycastle.jce.interfaces.ECPrivateKey) {
			final org.bouncycastle.jce.interfaces.ECPrivateKey ecPrivateKey = (org.bouncycastle.jce.interfaces.ECPrivateKey) privateKey;
			final String name = getEllipticCurveName(ecPrivateKey);
			final KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
			final ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(name);
			final ECPoint q = ecSpec.getG().multiply(ecPrivateKey.getD());
			final byte[] publicDerBytes = q.getEncoded(false);
			final ECPoint point = ecSpec.getCurve().decodePoint(publicDerBytes);
			final ECPublicKeySpec pubSpec = new ECPublicKeySpec(point, ecSpec);
			return keyFactory.generatePublic(pubSpec);
		} else if (privateKey instanceof ECPrivateKey) {
			final ECPrivateKey ecPrivateKey = (ECPrivateKey) privateKey;
			final String name = getEllipticCurveName(ecPrivateKey);
			final KeyFactory keyFactory = KeyFactory.getInstance("EC", BouncyCastleProvider.PROVIDER_NAME);
			final ECNamedCurveParameterSpec ecSpec = ECNamedCurveTable.getParameterSpec(name);
			final ECPoint q = ecSpec.getG().multiply(ecPrivateKey.getS());
			final byte[] publicDerBytes = q.getEncoded(false);
			final ECPoint point = ecSpec.getCurve().decodePoint(publicDerBytes);
			final ECPublicKeySpec pubSpec = new ECPublicKeySpec(point, ecSpec);
			return keyFactory.generatePublic(pubSpec);
		} else if (privateKey instanceof DSAPrivateKey) {
			throw new Exception("Cannot extract PublicKey from " + privateKey.getClass().getSimpleName());
		} else {
			throw new Exception("Cannot extract PublicKey from " + privateKey.getClass().getSimpleName());
		}
	}

	/**
	 * Returns the aliases of all secret key and private key entries of a key store.
	 *
	 * @param keyStore loaded key store
	 * @return key aliases
	 * @throws KeyStoreException if the key store is not loaded
	 */
	public static List<String> getKeyAliasesFromJavaKeyStore(final KeyStore keyStore) throws KeyStoreException {
		final List<String> keyAliases = new ArrayList<>();
		for (final String alias : Collections.list(keyStore.aliases())) {
			try {
				if (keyStore.entryInstanceOf(alias, SecretKeyEntry.class)) {
					keyAliases.add(alias);
				} else if (keyStore.entryInstanceOf(alias, PrivateKeyEntry.class)) {
					keyAliases.add(alias);
				}
			} catch (@SuppressWarnings("unused") final KeyStoreException e) {
				// Do not list this alias
			}
		}
		return keyAliases;
	}

	/**
	 * Returns the trusted certificates and the certificates of the private key entries of a key store.
	 *
	 * @param keyStore loaded key store
	 * @return X.509 certificates
	 * @throws Exception if the key store cannot be read
	 */
	public static List<X509Certificate> getCertificatesFromJavaKeyStore(final KeyStore keyStore) throws Exception {
		final List<X509Certificate> certificates = new ArrayList<>();
		for (final String alias : Collections.list(keyStore.aliases())) {
			try {
				if (keyStore.entryInstanceOf(alias, TrustedCertificateEntry.class)) {
					final TrustedCertificateEntry entry = (TrustedCertificateEntry) keyStore.getEntry(alias, null);
					certificates.add((X509Certificate) entry.getTrustedCertificate());
				} else if (keyStore.entryInstanceOf(alias, PrivateKeyEntry.class)) {
					final Certificate certificate = keyStore.getCertificate(alias);
					if (certificate != null && certificate instanceof X509Certificate) {
						certificates.add((X509Certificate) certificate);
					}
				}
			} catch (@SuppressWarnings("unused") final KeyStoreException e) {
				// Do not list this alias
			}
		}
		return certificates;
	}

	/**
	 * Returns the OpenSSH curve name of an ECDSA public key.
	 *
	 * @param publicKeyEC elliptic curve public key
	 * @return "nistp256", "nistp384", "nistp521" or "ansip256"
	 * @throws Exception if the curve is not supported
	 */
	public static String getEcDsaEllipticCurveName(final ECPublicKey publicKeyEC) throws Exception {
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
			if (Arrays.equals(OID.ECDSA_CURVE_NISTP256_ARRAY, ecDsaCurveOid.getByteArrayEncoding())) {
				return "nistp256";
			} else if (Arrays.equals(OID.ECDSA_CURVE_NISTP384_ARRAY, ecDsaCurveOid.getByteArrayEncoding())) {
				return "nistp384";
			} else if (Arrays.equals(OID.ECDSA_CURVE_NISTP521_ARRAY, ecDsaCurveOid.getByteArrayEncoding())) {
				return "nistp521";
			} else if (Arrays.equals(OID.ECDSA_CURVE_ANSIP256K1_ARRAY, ecDsaCurveOid.getByteArrayEncoding())) {
				return "ansip256";
			} else {
				throw new Exception("Unknown SSH EcDSA curve OID: " + ecDsaCurveOid.getStringEncoding());
			}
		} else {
			throw new Exception("Unknown SSH EcDSA public key OID: " + ecDsaPublicKeyOid.getStringEncoding());
		}
	}

	/**
	 * Returns the OpenSSH curve name of an ECDSA private key.
	 *
	 * @param ecPrivateKey elliptic curve private key
	 * @return "nistp256", "nistp384" or "nistp521"
	 * @throws Exception if the curve is not supported
	 */
	public static String getEcDsaEllipticCurveName(final ECPrivateKey ecPrivateKey) throws Exception {
		final DerTag enclosingDerTag = Asn1Codec.readDerTag(ecPrivateKey.getEncoded());
		if (Asn1Codec.DER_TAG_SEQUENCE != enclosingDerTag.getTagId()) {
			throw new Exception("Invalid key data found");
		}
		final List<DerTag> derDataTags = Asn1Codec.readDerTags(enclosingDerTag.getData());

		final BigInteger keyEncodingVersion = new BigInteger(derDataTags.get(0).getData());
		if (!BigInteger.ZERO.equals(keyEncodingVersion)) {
			throw new Exception("Invalid key data version found");
		}

		if (Asn1Codec.DER_TAG_SEQUENCE != derDataTags.get(1).getTagId()) {
			throw new Exception("Invalid key data found");
		}
		final List<DerTag> sshAlgorithmDerTags = Asn1Codec.readDerTags(derDataTags.get(1).getData());
		final OID ecDsaPublicKeyOid = new OID(sshAlgorithmDerTags.get(0).getData());
		if (Arrays.equals(OID.ECDSA_PUBLICKEY_ARRAY, ecDsaPublicKeyOid.getByteArrayEncoding())) {
			final OID ecDsaCurveOid = new OID(sshAlgorithmDerTags.get(1).getData());
			if (Arrays.equals(OID.ECDSA_CURVE_NISTP256_ARRAY, ecDsaCurveOid.getByteArrayEncoding())) {
				return "nistp256";
			} else if (Arrays.equals(OID.ECDSA_CURVE_NISTP384_ARRAY, ecDsaCurveOid.getByteArrayEncoding())) {
				return "nistp384";
			} else if (Arrays.equals(OID.ECDSA_CURVE_NISTP521_ARRAY, ecDsaCurveOid.getByteArrayEncoding())) {
				return "nistp521";
			} else {
				throw new Exception("Unknown SSH EcDSA curve OID: " + ecDsaCurveOid.getStringEncoding());
			}
		} else {
			throw new Exception("Unknown SSH EcDSA public key OID: " + ecDsaPublicKeyOid.getStringEncoding());
		}
	}

	/**
	 * Returns the name of the named curve of an elliptic curve public key.
	 *
	 * @param publicKey elliptic curve public key
	 * @return curve name as known by BouncyCastle
	 * @throws Exception if the key is no elliptic curve key or the curve is unknown
	 */
	public static final String getEllipticCurveName(final PublicKey publicKey) throws Exception{
		if (publicKey instanceof ECPublicKey) {
			final ECPublicKey pk = (ECPublicKey) publicKey;
			final java.security.spec.ECParameterSpec params = pk.getParams();
			return getEllipticCurveName(EC5Util.convertSpec(params));
		} else if(publicKey instanceof org.bouncycastle.jce.interfaces.ECPublicKey) {
			final org.bouncycastle.jce.interfaces.ECPublicKey pk = (org.bouncycastle.jce.interfaces.ECPublicKey) publicKey;
			return getEllipticCurveName(pk.getParameters());
		} else {
			throw new IllegalArgumentException("This public key is no elliptic curve public key");
		}
	}

	/**
	 * Returns the name of the named curve of an elliptic curve private key.
	 *
	 * @param privateKey elliptic curve private key
	 * @return curve name as known by BouncyCastle
	 * @throws Exception if the key is no elliptic curve key or the curve is unknown
	 */
	public static final String getEllipticCurveName(final PrivateKey privateKey) throws Exception{
		if (privateKey instanceof ECPrivateKey) {
			final ECPrivateKey pk = (ECPrivateKey) privateKey;
			final java.security.spec.ECParameterSpec params = pk.getParams();
			return getEllipticCurveName(EC5Util.convertSpec(params));
		} else if(privateKey instanceof org.bouncycastle.jce.interfaces.ECPrivateKey) {
			final org.bouncycastle.jce.interfaces.ECPrivateKey pk = (org.bouncycastle.jce.interfaces.ECPrivateKey) privateKey;
			return getEllipticCurveName(pk.getParameters());
		} else {
			throw new IllegalArgumentException("This private key is no elliptic curve private key");
		}
	}

	/**
	 * Returns the name of the named curve with the given parameters.
	 *
	 * @param ecParameterSpec curve parameters
	 * @return curve name as known by BouncyCastle
	 * @throws Exception if no named curve has these parameters
	 */
	public static final String getEllipticCurveName(final ECParameterSpec ecParameterSpec) throws Exception{
		// Bouncy Castle returns a raw Enumeration, its elements are the curve names as Strings
		final Enumeration<?> curveNamesEnumeration = org.bouncycastle.asn1.x9.ECNamedCurveTable.getNames();

		while (curveNamesEnumeration.hasMoreElements()) {
			final String name = (String) curveNamesEnumeration.nextElement();
			final X9ECParameters params = org.bouncycastle.asn1.x9.ECNamedCurveTable.getByName(name);
			if (params.getN().equals(ecParameterSpec.getN())
					&& params.getH().equals(ecParameterSpec.getH())
					&& params.getCurve().equals(ecParameterSpec.getCurve())
					&& params.getG().equals(ecParameterSpec.getG())) {
				return name;
			}
		}
		throw new Exception("Could not find elliptic curve name");
	}
}
