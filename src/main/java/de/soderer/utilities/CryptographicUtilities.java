package de.soderer.utilities;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;
import java.util.Iterator;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
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
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;

/**
 * May need installed "US_export_policy.jar" and "local_policy.jar" for unlimited key strength
 * Download:
 *   http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html
 */
public class CryptographicUtilities {
	public static Logger logger = Logger.getLogger(CryptographicUtilities.class);
	
	public static byte[] DEFAULT_SALT = new byte[]{48, 57, 56, 107, 108, 102, 114, 113, 54, 64, 108, 107};
	public static byte[] DEFAULT_SYMMETRIC_INITALIZATION_VECTOR = new byte[]{48, 57, 56, 107, 108, 102, 114, 113, 54, 64, 108, 107, 48, 57, 56, 107};
	public static final String[] SYMMETRIC_CIPHERS = {
		//Block chiffre
		"AES",
		"AESWrap",
		"Blowfish	",
		"Camellia",
		"CamelliaWrap",
		"CAST5",
		"CAST6",
		"DES",
		"DESede",
		"DESedeWrap",
		"GOST28147",
		"IDEA",
		"Noekeon",
		"RC2",
		"RC5",
		"RC5-64",
		"RC6",
		"Rijndael",
		"SEED",
		"SEEDWrap",
		"Serpent",
		"Skipjack",
		"TEA",
		"Twofish",
		"XTEA",

		// Stream chiffre
		"RC4	",
		"HC128	",
		"HC256	",
		"Salsa20	",
		"VMPC",
		"Grainv1",
		"Grain128"
	};
	public static String DEFAULT_SYMMETRIC_ENCRYPTION_METHOD = "AES/CBC/PKCS7Padding";
	public static final String[] KNOWN_SYMMETRIC_ENCRYPTION_METHODS = new String[]{DEFAULT_SYMMETRIC_ENCRYPTION_METHOD,
		"DES/CBC/PKCS5Padding",
		"DES/CBC/X9.23Padding",
		"DES/OFB8/NoPadding",
		"DES/ECB/WithCTS",
		"IDEA/CBC/ISO10126Padding",
		"IDEA/CBC/ISO7816-4Padding",
		"SKIPJACK/ECB/PKCS7Padding"};
	public static String DEFAULT_SIGNATURE_METHOD = "SHA256WithRSA";
	public static final String[] KNOWN_SIGNATURE_METHODS = new String[]{DEFAULT_SIGNATURE_METHOD,
		"DSTU4145",
		"GOST3411withGOST3410",
		"GOST3411withGOST3410-94",
		"GOST3411withECGOST3410",
		"GOST3411withGOST3410-2001",
		"MD2withRSA",
		"MD5withRSA",
		"SHA1withRSA",
		"RIPEMD128withRSA",
		"RIPEMD160withRSA",
		"RIPEMD160withECDSA",
		"RIPEMD256withRSA",
		"SHA1withDSA",
		"NONEwithDSA",
		"SHA1withECDSA",
		"NONEwithECDSA",
		"SHA224withECDSA",
		"SHA256withECDSA",
		"SHA384withECDSA",
		"SHA512withECDSA",
		"SHA1withECNR",
		"SHA224withECNR",
		"SHA256withECNR",
		"SHA384withECNR",
		"SHA512withECNR",
		"SHA224withRSA",
		"SHA384withRSA",
		"SHA512withRSA",
		"SHA1withRSAandMGF1",
		"SHA256withRSAandMGF1",
		"SHA384withRSAandMGF1",
		"SHA512withRSAandMGF1"
	};

	public static String DEFAULT_SIGNATURE_METHOD_OPENPGP = "SHA-1";
	public static final String[] KNOWN_SIGNATURE_METHODS_OPENPGP = new String[]{DEFAULT_SIGNATURE_METHOD_OPENPGP,
	    "MD5",
	    "RIPEMD160",
	    "DOUBLE-SHA",
	    "MD2",
	    "TIGER-192",
	    "HAVAL-5-160",
	    "SHA-256",
	    "SHA-384",
	    "SHA-512",
	    "SHA-224"
	};
	
	public static final String[] ASYMMETRIC_CIPHERS = new String[]{
		"RSA",
		"ElGamal"
	};
	public static String DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD = "RSA/ECB/PKCS1Padding";
	public static final String[] KNOWN_ASYMMETRIC_ENCRYPTION_METHODS = new String[]{DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD,
		"RSA/ECB/OAEPPadding",
		"RSA/NONE/NoPadding",
		"RSA/NONE/PKCS1Padding",
		"RSA/NONE/OAEPWithMD5AndMGF1Padding",
		"RSA/NONE/OAEPWithSHA1AndMGF1Padding",
		"RSA/NONE/OAEPWithSHA224AndMGF1Padding",
		"RSA/NONE/OAEPWithSHA256AndMGF1Padding",
		"RSA/NONE/OAEPWithSHA384AndMGF1Padding",
		"RSA/NONE/OAEPWithSHA512AndMGF1Padding",
		"RSA/NONE/ISO9796-1Padding",
		"ElGamal/NONE/NoPadding"
	};
	
	public static AsymmetricCipherKeyPair generateRsaKeyPair(int keyStrength) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			RSAKeyPairGenerator generator = new RSAKeyPairGenerator();
			generator.init(new RSAKeyGenerationParameters(RSAKeyGenParameterSpec.F4, SecureRandom.getInstance("SHA1PRNG"), keyStrength, 80));
			AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
			return keyPair;
		} catch (Exception e) {
			throw new Exception("Cannot create RSA keypair", e);
		}
	}
	
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
			PGPKeyRingGenerator keyringGenerator = new PGPKeyRingGenerator(
				PGPSignature.POSITIVE_CERTIFICATION,
				masterKeyPair,
				identity,
				SymmetricKeyAlgorithmTags.AES_256,
				password,
				subpacketVector,
				null,
				new SecureRandom(),
				"BC");
			
			PGPSecretKeyRing secretKeyRing = keyringGenerator.generateSecretKeyRing();
			
			return secretKeyRing.getSecretKey();
		} catch (Exception e) {
			throw new Exception("Cannot create OpenPGP key", e);
		}
	}
	
	public static byte[] exportOpenPGPSecretKey(PGPSecretKey pgpSecretKey) throws Exception {
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
			throw new Exception("Cannot export OpenPGP key", e);
		} finally {
			IOUtils.closeQuietly(outputStream);
			IOUtils.closeQuietly(secretKeyStream);
		}
	}
	
	public static byte[] exportOpenPGPPublicKey(PGPPublicKey pgpPublicKey) throws Exception {
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
			throw new Exception("Cannot export OpenPGP key", e);
		} finally {
			IOUtils.closeQuietly(outputStream);
			IOUtils.closeQuietly(secretKeyStream);
		}
	}
	
	public static X509Certificate generateRsaCertificate(AsymmetricCipherKeyPair keyPair, String issuerAndSubjectName, Date validFrom, Date validUntil) throws Exception {
		return generateRsaCertificate(keyPair, issuerAndSubjectName, validFrom, validUntil, DEFAULT_SIGNATURE_METHOD);
	}
	
	@SuppressWarnings("deprecation")
	public static X509Certificate generateRsaCertificate(AsymmetricCipherKeyPair keyPair, String issuerAndSubjectName, Date validFrom, Date validUntil, String signatureMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			org.bouncycastle.x509.X509V3CertificateGenerator certificateGenerator = new org.bouncycastle.x509.X509V3CertificateGenerator();
			
		    certificateGenerator.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
		    certificateGenerator.setSubjectDN(new X500Principal("CN=" + issuerAndSubjectName));
		    certificateGenerator.setIssuerDN(new X500Principal("CN=" + issuerAndSubjectName));
		    certificateGenerator.setNotBefore(validFrom);
		    certificateGenerator.setNotAfter(validUntil);
		    certificateGenerator.setPublicKey(getPublicKeyFromKeyPair(keyPair));
		    certificateGenerator.setSignatureAlgorithm(signatureMethod);

		    return certificateGenerator.generate(getPrivateKeyFromKeyPair(keyPair), "BC");
		} catch (Exception e) {
			throw new Exception("Cannot create RSA certificate", e);
		}
	}
	
	public static String getStringFromX509Certificate(X509Certificate certificate) throws Exception {
		StringWriter stringWriter = null;
		PEMWriter pemWriter = null;
		try {
			stringWriter = new StringWriter();
			pemWriter = new PEMWriter(stringWriter);
			pemWriter.writeObject(certificate);
			pemWriter.close();
			return stringWriter.toString();
		} catch (Exception e) {
			throw new Exception("Cannot create certificate string: " + e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(stringWriter);
			IOUtils.closeQuietly(pemWriter);
		}
	}

	public static void encryptAsymmetric(InputStream dataStream, OutputStream encryptedOutputStream, PublicKey publicKey) throws Exception {
		encryptAsymmetric(dataStream, encryptedOutputStream, publicKey, DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD);	
	}

	public static void encryptAsymmetric(InputStream dataStream, OutputStream encryptedOutputStream, PublicKey publicKey, String encryptionMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		CipherOutputStream cipherOutputStream = null;
		try {
			Cipher encryptCipher = Cipher.getInstance(encryptionMethod, "BC");
			encryptCipher.init(Cipher.ENCRYPT_MODE, publicKey);
			cipherOutputStream = new CipherOutputStream(encryptedOutputStream, encryptCipher);
			IOUtils.copy(dataStream, cipherOutputStream);
		} catch (Exception e) {
			throw new Exception("Error while encrypting: " + e.getMessage(), e);
		} finally {
			if (cipherOutputStream != null) {
				cipherOutputStream.close();
			}
		}
	}
	
	public static void decryptAsymmetric(InputStream encryptedDataStream, OutputStream dataOutputStream, PrivateKey privateKey) throws Exception {
		decryptAsymmetric(encryptedDataStream, dataOutputStream, privateKey, DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD);
	}
	
	public static void decryptAsymmetric(InputStream encryptedDataStream, OutputStream dataOutputStream, PrivateKey privateKey, String encryptionMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		InputStream cipherInputStream = null;
		
		try {
			Cipher decryptCipher = Cipher.getInstance(encryptionMethod, "BC");
			decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);
			cipherInputStream = new CipherInputStream(encryptedDataStream, decryptCipher);
			IOUtils.copy(cipherInputStream, dataOutputStream);
		} catch (Exception e) {
			throw new Exception("Error while decrypting: " + e.getMessage(), e);
		} finally {
			if (cipherInputStream != null) {
				cipherInputStream.close();
			}
		}
	}

	/**
	 * Convert a Key to string encoded as BASE64
	 */
	public static String getStringFromKey(Key key) {
		return Base64.encodeBase64String(key.getEncoded());
	}
	
	public static String getStringFromKeyPair(AsymmetricCipherKeyPair keyPair) throws Exception {
		PublicKey publicKey = getPublicKeyFromKeyPair(keyPair);
		PrivateKey privateKey = getPrivateKeyFromKeyPair(keyPair);
		
		return getStringFromKeyPair(privateKey, publicKey);
	}
	
	public static String getStringFromKeyPair(PrivateKey privateKey, PublicKey publicKey) throws Exception {
		StringWriter stringWriter = null;
		PEMWriter pemWriter = null;
		try {
			stringWriter = new StringWriter();
			pemWriter = new PEMWriter(stringWriter);
			pemWriter.writeObject(privateKey);
			pemWriter.writeObject(publicKey);
			pemWriter.close();
			return stringWriter.toString();
		} catch (Exception e) {
			throw new Exception("Cannot create key string: " + e.getMessage(), e);
		} finally {
			IOUtils.closeQuietly(stringWriter);
			IOUtils.closeQuietly(pemWriter);
		}
	}
	
	public static PublicKey getPublicKeyFromKeyPair(AsymmetricCipherKeyPair keyPair) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		RSAKeyParameters publicKey = (RSAKeyParameters) keyPair.getPublic();
		return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(publicKey.getModulus(), publicKey.getExponent()));
	}
	
	public static PrivateKey getPrivateKeyFromKeyPair(AsymmetricCipherKeyPair keyPair) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		RSAPrivateCrtKeyParameters privateKey = (RSAPrivateCrtKeyParameters) keyPair.getPrivate();
		RSAKeyParameters publicKey = (RSAKeyParameters) keyPair.getPublic();
		return KeyFactory.getInstance("RSA").generatePrivate(
			new RSAPrivateCrtKeySpec(
				privateKey.getModulus(),
				publicKey.getExponent(),
				privateKey.getExponent(),
				privateKey.getP(),
				privateKey.getQ(),
				privateKey.getDP(),
				privateKey.getDQ(),
				privateKey.getQInv()));
	}
	
	/**
	 * Generates Private Key from BASE64 encoded string
	 */
	public static PrivateKey getPrivateKeyFromString(String keyString) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			PEMReader pemReader = new PEMReader(new StringReader(keyString));
			Object readObject = pemReader.readObject();
			pemReader.close();
			if (readObject instanceof KeyPair) {
				KeyPair keyPair = (KeyPair) readObject;
				return keyPair.getPrivate();
			} else if (readObject instanceof PrivateKey) {
				return (PrivateKey) readObject;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new Exception("Cannot read private key", e);
		}
	}

	/**
	 * Generates Public Key from BASE64 encoded string
	 */
	public static PublicKey getPublicKeyFromString(String keyString) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			PEMReader pemReader = new PEMReader(new StringReader(keyString));
			Object readObject = pemReader.readObject();
			pemReader.close();
			if (readObject instanceof KeyPair) {
				KeyPair keyPair = (KeyPair) readObject;
				return keyPair.getPublic();
			} else if (readObject instanceof PublicKey) {
				return (PublicKey) readObject;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new Exception("Cannot read public key", e);
		}
	}
	
	/**
	 * Generates X509Certificate from BASE64 encoded string
	 */
	public static X509Certificate getCertificateFromString(String certificateString) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			PEMReader pemReader = new PEMReader(new StringReader(certificateString));
			Object readObject = pemReader.readObject();
			pemReader.close();
			if (readObject instanceof X509Certificate) {
				return (X509Certificate) readObject;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new Exception("Cannot read certificate", e);
		}
	}

	/**
	 * Read a keyfile generated with 'openssl genrsa...'
	 */
	public static PublicKey readRSAPublicKey(InputStream inputStream) throws Exception {
		return getPublicKeyFromString(IOUtils.toString(inputStream, "UTF-8"));
	}
	
	public static PGPPublicKey readPGPPublicKey(InputStream inputStream) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try {
			PGPPublicKeyRingCollection pgpPublicKeyRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(inputStream));
			@SuppressWarnings("rawtypes")
			Iterator keyRingIterator = pgpPublicKeyRingCollection.getKeyRings();
			while (keyRingIterator.hasNext()) {
				PGPPublicKeyRing keyRing = (PGPPublicKeyRing)keyRingIterator.next();
				@SuppressWarnings("rawtypes")
				Iterator publicKeyIterator = keyRing.getPublicKeys();
				while (publicKeyIterator.hasNext()) {
					PGPPublicKey nextPGPPublicKey = (PGPPublicKey)publicKeyIterator.next();
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
	
	public static PGPSecretKey readPGPSecretKey(InputStream inputStream) throws IOException, PGPException, NoSuchProviderException {
		Security.addProvider(new BouncyCastleProvider());

		try {
			PGPSecretKeyRingCollection pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(inputStream));
			@SuppressWarnings("rawtypes")
			Iterator keyRingIterator = pgpSecretKeyRingCollection.getKeyRings();
			while (keyRingIterator.hasNext()) {
				PGPSecretKeyRing keyRing = (PGPSecretKeyRing)keyRingIterator.next();
				@SuppressWarnings("rawtypes")
				Iterator privateKeyIterator = keyRing.getSecretKeys();
				while (privateKeyIterator.hasNext()) {
					PGPSecretKey nextPGPPrivateKey = (PGPSecretKey)privateKeyIterator.next();
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
	
	public static PGPPrivateKey readPGPPrivateKey(PGPSecretKey pgpSecretKey, char[] password) throws NoSuchProviderException, PGPException {
		if (pgpSecretKey == null) {
			return null;
		} else {
			Security.addProvider(new BouncyCastleProvider());
			
			return pgpSecretKey.extractPrivateKey(password, "BC");
		}
	}
	
	public static PGPPublicKey readPGPPublicKey(PGPSecretKey pgpSecretKey) throws NoSuchProviderException, PGPException {
		if (pgpSecretKey == null) {
			return null;
		} else {
			Security.addProvider(new BouncyCastleProvider());
			
			return pgpSecretKey.getPublicKey();
		}
	}

	/**
	 * Read a keyfile generated with 'openssl genrsa...'
	 */
	public static PrivateKey readRSAPrivateKey(InputStream inputStream) throws Exception {
		return getPrivateKeyFromString(IOUtils.toString(inputStream, "UTF-8"));
	}

	/**
	 * Read a certificate file
	 */
	public static X509Certificate getCertificateFromFile(File certificateFile) throws Exception {
		String certificateFileString = FileUtils.readFileToString(certificateFile);
		return getCertificateFromString(certificateFileString);
	}
	
	public static void encryptSymmetric(InputStream dataStream, OutputStream encryptedOutputStream, char[] password) throws Exception {
		encryptSymmetric(dataStream, encryptedOutputStream, password, DEFAULT_SALT, DEFAULT_SYMMETRIC_INITALIZATION_VECTOR, DEFAULT_SYMMETRIC_ENCRYPTION_METHOD);
	}
	
	public static void encryptSymmetric(InputStream dataStream, OutputStream encryptedOutputStream, char[] password, byte[] salt, byte[] initializationVector, String encryptionMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		CipherOutputStream cipherOutputStream = null;
		try {
			byte[] keyBytes = stretchPassword(password, 128, salt);
			SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
			Cipher encryptCipher = Cipher.getInstance(encryptionMethod, "BC");
			encryptCipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(initializationVector));
			cipherOutputStream = new CipherOutputStream(encryptedOutputStream, encryptCipher);
			IOUtils.copy(dataStream, cipherOutputStream);
		} catch (Exception e) {
			throw new Exception("Error while encrypting: " + e.getMessage(), e);
		} finally {
			if (cipherOutputStream != null) {
				cipherOutputStream.close();
			}
		}
	}
	
	public static void decryptSymmetric(InputStream encryptedDataStream, OutputStream dataOutputStream, char[] password) throws Exception {
		decryptSymmetric(encryptedDataStream, dataOutputStream, password, DEFAULT_SALT, DEFAULT_SYMMETRIC_INITALIZATION_VECTOR, DEFAULT_SYMMETRIC_ENCRYPTION_METHOD);
	}
	
	public static void decryptSymmetric(InputStream encryptedDataStream, OutputStream dataOutputStream, char[] password, byte[] salt, byte[] initializationVector, String encryptionMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		CipherInputStream cipherInputStream = null;
		try {
			byte[] keyBytes = stretchPassword(password, 128, salt);
			SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
			Cipher decryptCipher = Cipher.getInstance(encryptionMethod, "BC");
			decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(initializationVector));
			cipherInputStream = new CipherInputStream(encryptedDataStream, decryptCipher);
			IOUtils.copy(cipherInputStream, dataOutputStream);
		} catch (Exception e) {
			throw new Exception("Error while decrypting: " + e.getMessage(), e);
		} finally {
			if (cipherInputStream != null) {
				cipherInputStream.close();
			}
		}
	}
	
	public static byte[] stretchPassword(char[] password, int keyLength, byte[] salt) {
		PBEParametersGenerator generator = new PKCS5S2ParametersGenerator();
	    generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password), salt, 1000);
	    KeyParameter params = (KeyParameter) generator.generateDerivedParameters(keyLength);
	    return params.getKey();
	}
	
	public static byte[] signData(byte[] data, PrivateKey privateKey) throws Exception {
		return signData(data, privateKey, DEFAULT_SIGNATURE_METHOD);
	}
	
	public static byte[] signData(byte[] data, PrivateKey privateKey, String signatureMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
	
		try {
			Signature signature = Signature.getInstance(signatureMethod, "BC");
			signature.initSign(privateKey);
			signature.update(data);
			return signature.sign();
		} catch (Exception e) {
			throw new Exception("Cannot create signature", e);
		}
	}
	
	public static byte[] signStream(InputStream dataStream, PrivateKey privateKey) throws Exception {
		return signStream(dataStream, privateKey, DEFAULT_SIGNATURE_METHOD);
	}
	
	public static byte[] signStream(InputStream dataStream, PrivateKey privateKey, String signatureMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
	
		try {
			Signature signature = Signature.getInstance(signatureMethod, "BC");
			signature.initSign(privateKey);
			byte[] buffer = new byte[4096];
			int bytesRead = dataStream.read(buffer);
			while (bytesRead >= 0) {
				signature.update(buffer, 0, bytesRead);
				bytesRead = dataStream.read(buffer);
			}
			return signature.sign();
		} catch (Exception e) {
			throw new Exception("Cannot create signature", e);
		}
	}
	
	public static boolean verifyData(byte[] data, PublicKey publicKey, byte[] signatureData) throws Exception {
		return verifyData(data, publicKey, signatureData, DEFAULT_SIGNATURE_METHOD);
	}
	
	public static boolean verifyData(byte[] data, PublicKey publicKey, byte[] signatureData, String signatureMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
	
		try {
			Signature signature = Signature.getInstance(signatureMethod, "BC");
			signature.initVerify(publicKey);
			signature.update(data);
			return signature.verify(signatureData);
		} catch (Exception e) {
			throw new Exception("Cannot verify signature", e);
		}
	}
	
	public static boolean verifyStream(InputStream dataStream, PublicKey publicKey, byte[] signatureData) throws Exception {
		return verifyStream(dataStream, publicKey, signatureData, DEFAULT_SIGNATURE_METHOD);
	}
	
	public static boolean verifyStream(InputStream dataStream, PublicKey publicKey, byte[] signatureData, String signatureMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
	
		try {
			Signature signature = Signature.getInstance(signatureMethod, "BC");
			signature.initVerify(publicKey);
			byte[] buffer = new byte[4096];
			int bytesRead = dataStream.read(buffer);
			while (bytesRead >= 0) {
				signature.update(buffer, 0, bytesRead);
				bytesRead = dataStream.read(buffer);
			}
			return signature.verify(signatureData);
		} catch (Exception e) {
			throw new Exception("Cannot verify signature", e);
		}
	}
	
	public static int getOpenpgpSignatureMethod(String methodName) {
		methodName = methodName.replace("-", "").replace("_", "");
		if ("MD5".equalsIgnoreCase(methodName)) {
			return PGPUtil.MD5;
		} else if ("RIPEMD160".equalsIgnoreCase(methodName)) {
			return PGPUtil.RIPEMD160;
		} else if ("DOUBLESHA".equalsIgnoreCase(methodName)) {
			return PGPUtil.DOUBLE_SHA;
		} else if ("MD2".equalsIgnoreCase(methodName)) {
			return PGPUtil.MD2;
		} else if ("TIGER192".equalsIgnoreCase(methodName)) {
			return PGPUtil.TIGER_192;
		} else if ("HAVAL5160".equalsIgnoreCase(methodName)) {
			return PGPUtil.HAVAL_5_160;
		} else if ("SHA256".equalsIgnoreCase(methodName)) {
			return PGPUtil.SHA256;
		} else if ("SHA384".equalsIgnoreCase(methodName)) {
			return PGPUtil.SHA384;
		} else if ("SHA512".equalsIgnoreCase(methodName)) {
			return PGPUtil.SHA512;
		} else if ("SHA224".equalsIgnoreCase(methodName)) {
			return PGPUtil.SHA224;
		} else {
			return PGPUtil.SHA1;
		}
	}
}
