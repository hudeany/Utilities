package de.soderer.utilities;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.cert.CertPathBuilder;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathBuilderResult;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.encodings.ISO9796d1Encoding;
import org.bouncycastle.crypto.encodings.OAEPEncoding;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.ElGamalEngine;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.generators.ElGamalKeyPairGenerator;
import org.bouncycastle.crypto.generators.ElGamalParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ElGamalKeyGenerationParameters;
import org.bouncycastle.crypto.params.ElGamalParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.crypto.util.PublicKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * May need installed "US_export_policy.jar" and "local_policy.jar" for unlimited key strength Download: http://www.oracle.com/technetwork/java/javase/downloads/jce-7-download-432124.html
 */
public class CryptographicUtilities {
	public static byte[] DEFAULT_SALT = new byte[] { 48, 57, 56, 107, 108, 102, 114, 113, 54, 64, 108, 107 };
	public static byte[] DEFAULT_SYMMETRIC_INITALIZATION_VECTOR = new byte[] { 48, 57, 56, 107, 108, 102, 114, 113, 54, 64, 108, 107, 48, 57, 56, 107 };
	public static final String[] SYMMETRIC_CIPHERS = {
			// Block chiffre
			"AES", "AESWrap", "Blowfish	", "Camellia", "CamelliaWrap", "CAST5", "CAST6", "DES", "DESede", "DESedeWrap", "GOST28147", "IDEA", "Noekeon", "RC2", "RC5", "RC5-64", "RC6", "Rijndael",
			"SEED", "SEEDWrap", "Serpent", "Skipjack", "TEA", "Twofish", "XTEA",

			// Stream chiffre
			"RC4	", "HC128	", "HC256	", "Salsa20	", "VMPC", "Grainv1", "Grain128" };
	public static String DEFAULT_SYMMETRIC_ENCRYPTION_METHOD = "AES/CBC/PKCS7Padding";
	public static final String[] KNOWN_SYMMETRIC_ENCRYPTION_METHODS = new String[] { DEFAULT_SYMMETRIC_ENCRYPTION_METHOD, "DES/CBC/PKCS5Padding", "DES/CBC/X9.23Padding", "DES/OFB8/NoPadding",
			"DES/ECB/WithCTS", "IDEA/CBC/ISO10126Padding", "IDEA/CBC/ISO7816-4Padding", "SKIPJACK/ECB/PKCS7Padding" };
	public static String DEFAULT_SIGNATURE_METHOD = "SHA256WithRSA";
	public static final String[] KNOWN_SIGNATURE_METHODS = new String[] { DEFAULT_SIGNATURE_METHOD, "DSTU4145", "GOST3411withGOST3410", "GOST3411withGOST3410-94", "GOST3411withECGOST3410",
			"GOST3411withGOST3410-2001", "MD2withRSA", "MD5withRSA", "SHA1withRSA", "RIPEMD128withRSA", "RIPEMD160withRSA", "RIPEMD160withECDSA", "RIPEMD256withRSA", "SHA1withDSA", "NONEwithDSA",
			"SHA1withECDSA", "NONEwithECDSA", "SHA224withECDSA", "SHA256withECDSA", "SHA384withECDSA", "SHA512withECDSA", "SHA1withECNR", "SHA224withECNR", "SHA256withECNR", "SHA384withECNR",
			"SHA512withECNR", "SHA224withRSA", "SHA384withRSA", "SHA512withRSA", "SHA1withRSAandMGF1", "SHA256withRSAandMGF1", "SHA384withRSAandMGF1", "SHA512withRSAandMGF1" };

	public static final String[] ASYMMETRIC_CIPHERS = new String[] { "RSA", "ElGamal" };
	public static String DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD = "RSA/ECB/PKCS1Padding";
	public static final String[] KNOWN_ASYMMETRIC_ENCRYPTION_METHODS = new String[] {
		"RSA/ECB/PKCS1Padding",
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
			generator.init(new RSAKeyGenerationParameters(RSAKeyGenParameterSpec.F4, new SecureRandom(), keyStrength, 80));
			AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
			return keyPair;
		} catch (Exception e) {
			throw new Exception("Cannot create RSA keypair", e);
		}
	}

	public static AsymmetricCipherKeyPair generateElGamalKeyPair(int keyStrength) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try {
			ElGamalParametersGenerator elGamalParametersGenerator = new ElGamalParametersGenerator();
			elGamalParametersGenerator.init(keyStrength, 80, new SecureRandom());
			ElGamalParameters elGamalParameters = elGamalParametersGenerator.generateParameters();
			ElGamalKeyPairGenerator generator = new ElGamalKeyPairGenerator();
			generator.init(new ElGamalKeyGenerationParameters(new SecureRandom(), elGamalParameters));
			AsymmetricCipherKeyPair keyPair = generator.generateKeyPair();
			return keyPair;
		} catch (Exception e) {
			throw new Exception("Cannot create RSA keypair", e);
		}
	}

	public static X509Certificate generateRsaCertificate(KeyPair keyPair, String issuerName, Date validFrom, Date validUntil) throws Exception {
		return generateRsaCertificate(keyPair, issuerName, validFrom, validUntil, DEFAULT_SIGNATURE_METHOD);
	}

	public static X509Certificate generateRsaCertificate(KeyPair keyPair, String issuerName, Date validFrom, Date validUntil, String signatureMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
	        builder.addRDN(BCStyle.CN, issuerName);
	        X500Name x500Name = builder.build();
	        
			ContentSigner sigGen = new JcaContentSignerBuilder(signatureMethod).build(keyPair.getPrivate());
	        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(x500Name, BigInteger.ONE, validFrom, validUntil, x500Name, keyPair.getPublic());
	        return new JcaX509CertificateConverter().getCertificate(certGen.build(sigGen));
		} catch (Exception e) {
			throw new Exception("Cannot create RSA certificate", e);
		}
	}

	public static X509Certificate generateRsaCertificate(AsymmetricCipherKeyPair keyPair, String issuerName, Date validFrom, Date validUntil) throws Exception {
		return generateRsaCertificate(keyPair, issuerName, validFrom, validUntil, DEFAULT_SIGNATURE_METHOD);
	}

	public static X509Certificate generateRsaCertificate(AsymmetricCipherKeyPair keyPair, String issuerName, Date validFrom, Date validUntil, String signatureMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
	        builder.addRDN(BCStyle.CN, issuerName);
	        X500Name x500Name = builder.build();
	        
			ContentSigner sigGen = new JcaContentSignerBuilder(signatureMethod).build(getPrivateKeyFromKeyPair(keyPair));
	        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(x500Name, BigInteger.ONE, validFrom, validUntil, x500Name, getPublicKeyFromKeyPair(keyPair));
	        return new JcaX509CertificateConverter().getCertificate(certGen.build(sigGen));
		} catch (Exception e) {
			throw new Exception("Cannot create RSA certificate", e);
		}
	}

	public static String getStringFromX509Certificate(X509Certificate certificate) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		StringWriter stringWriter = null;
		JcaPEMWriter pemWriter = null;
		try {
			stringWriter = new StringWriter();
			pemWriter = new JcaPEMWriter(stringWriter);
			pemWriter.writeObject(certificate);
			pemWriter.close();
			return stringWriter.toString();
		} catch (Exception e) {
			throw new Exception("Cannot create certificate string: " + e.getMessage(), e);
		} finally {
			Utilities.closeQuietly(stringWriter);
			Utilities.closeQuietly(pemWriter);
		}
	}

	public static void encryptAsymmetric(InputStream dataStream, OutputStream encryptedOutputStream, AsymmetricKeyParameter publicKey) throws Exception {
		encryptAsymmetric(dataStream, encryptedOutputStream, publicKey, DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD);
	}

	public static void encryptAsymmetric(InputStream dataStream, OutputStream encryptedOutputStream, AsymmetricKeyParameter publicKey, String encryptionMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		try {
			if (Utilities.isBlank(encryptionMethod)) {
				throw new Exception("Empty asymmetric encryption method");
			}
			encryptionMethod = encryptionMethod.toLowerCase();
			AsymmetricBlockCipher encryptCipher;
			if (encryptionMethod.startsWith("rsa")) {
				encryptCipher = new RSAEngine();
			} else if (encryptionMethod.startsWith("elgamal")) {
				encryptCipher = new ElGamalEngine();
			} else {
				throw new Exception("Unknown asymmetric encryption cipher method: " + encryptionMethod);
			}
			if (encryptionMethod.contains("pkcs1")) {
				encryptCipher = new PKCS1Encoding(encryptCipher);
			} else if (encryptionMethod.contains("oaep")) {
				encryptCipher = new OAEPEncoding(encryptCipher);
			} else if (encryptionMethod.contains("iso9796")) {
				encryptCipher = new ISO9796d1Encoding(encryptCipher);
			} else {
				throw new Exception("Unknown asymmetric encryption padding method: " + encryptionMethod);
			}
			encryptCipher.init(true, publicKey);
			
            byte[] buffer = new byte[encryptCipher.getInputBlockSize()];
            int readBytes;
            while ((readBytes = dataStream.read(buffer)) > -1) {
                encryptedOutputStream.write(encryptCipher.processBlock(buffer, 0, readBytes));
            }
		} catch (Exception e) {
			throw new Exception("Error while encrypting: " + e.getMessage(), e);
		}
	}

	public static void decryptAsymmetric(InputStream encryptedDataStream, OutputStream dataOutputStream, AsymmetricKeyParameter privateKey) throws Exception {
		decryptAsymmetric(encryptedDataStream, dataOutputStream, privateKey, DEFAULT_ASYMMETRIC_ENCRYPTION_METHOD);
	}

	public static void decryptAsymmetric(InputStream encryptedDataStream, OutputStream dataOutputStream, AsymmetricKeyParameter privateKey, String encryptionMethod) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			if (Utilities.isBlank(encryptionMethod)) {
				throw new Exception("Empty asymmetric encryption method");
			}
			encryptionMethod = encryptionMethod.toLowerCase();
			AsymmetricBlockCipher decryptCipher;
			if (encryptionMethod.startsWith("rsa")) {
				decryptCipher = new RSAEngine();
			} else if (encryptionMethod.startsWith("elgamal")) {
				decryptCipher = new ElGamalEngine();
			} else {
				throw new Exception("Unknown asymmetric encryption cipher method: " + encryptionMethod);
			}
			if (encryptionMethod.contains("pkcs1")) {
				decryptCipher = new PKCS1Encoding(decryptCipher);
			} else if (encryptionMethod.contains("oaep")) {
				decryptCipher = new OAEPEncoding(decryptCipher);
			} else if (encryptionMethod.contains("iso9796")) {
				decryptCipher = new ISO9796d1Encoding(decryptCipher);
			} else {
				throw new Exception("Unknown asymmetric encryption padding method: " + encryptionMethod);
			}
			
			decryptCipher.init(false, privateKey);
			
			byte[] buffer = new byte[decryptCipher.getInputBlockSize()];
            int readBytes;
            while ((readBytes = encryptedDataStream.read(buffer)) > -1) {
            	dataOutputStream.write(decryptCipher.processBlock(buffer, 0, readBytes));
            }
		} catch (Exception e) {
			throw new Exception("Error while encrypting: " + e.getMessage(), e);
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
		Security.addProvider(new BouncyCastleProvider());
		
		StringWriter stringWriter = null;
		JcaPEMWriter pemWriter = null;
		try {
			stringWriter = new StringWriter();
			pemWriter = new JcaPEMWriter(stringWriter);
			pemWriter.writeObject(privateKey);
			pemWriter.writeObject(publicKey);
			pemWriter.close();
			return stringWriter.toString();
		} catch (Exception e) {
			throw new Exception("Cannot create key string: " + e.getMessage(), e);
		} finally {
			Utilities.closeQuietly(stringWriter);
			Utilities.closeQuietly(pemWriter);
		}
	}

	public static AsymmetricCipherKeyPair getAsymmetricCipherKeyPair(InputStream inputStream) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		PEMKeyPair keyPair = getPEMKeyPairFromString(new String(Utilities.readStreamToByteArray(inputStream), "UTF-8"));
		AsymmetricKeyParameter privateAsymmetricKeyParameter = PrivateKeyFactory.createKey(keyPair.getPrivateKeyInfo());
		AsymmetricKeyParameter publicAsymmetricKeyParameter = PublicKeyFactory.createKey(keyPair.getPublicKeyInfo());
		
		return new AsymmetricCipherKeyPair(privateAsymmetricKeyParameter, publicAsymmetricKeyParameter);
	}

	public static AsymmetricCipherKeyPair getAsymmetricCipherKeyPair(PrivateKey privateKey, PublicKey publicKey) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		PEMKeyPair keyPair = getPEMKeyPairFromString(getStringFromKeyPair(privateKey, publicKey));
		AsymmetricKeyParameter privateAsymmetricKeyParameter = PrivateKeyFactory.createKey(keyPair.getPrivateKeyInfo());
		AsymmetricKeyParameter publicAsymmetricKeyParameter = PublicKeyFactory.createKey(keyPair.getPublicKeyInfo());
		
		return new AsymmetricCipherKeyPair(privateAsymmetricKeyParameter, publicAsymmetricKeyParameter);
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
		return KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateCrtKeySpec(privateKey.getModulus(), publicKey.getExponent(), privateKey.getExponent(), privateKey.getP(), privateKey.getQ(),
				privateKey.getDP(), privateKey.getDQ(), privateKey.getQInv()));
	}

	/**
	 * Generates Private Key from BASE64 encoded string
	 */
	public static PEMKeyPair getPEMKeyPairFromString(String keyString) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			PEMParser pemReader = new PEMParser(new StringReader(keyString));
			Object readObject = pemReader.readObject();
			pemReader.close();
//			if (readObject instanceof PEMEncryptedKeyPair) {
//                PEMEncryptedKeyPair pemEncryptedKeyPairKeyPair = (PEMEncryptedKeyPair) readObject;
//                JcePEMDecryptorProviderBuilder jcePEMDecryptorProviderBuilder = new JcePEMDecryptorProviderBuilder();
//                PEMKeyPair pemKeyPair = pemEncryptedKeyPairKeyPair.decryptKeyPair(jcePEMDecryptorProviderBuilder.build(keyPassword.toCharArray()));
//            } else
            	if (readObject instanceof PEMKeyPair) {
				PEMKeyPair keyPair = (PEMKeyPair) readObject;
				return keyPair;
			} else {
				return null;
			}
		} catch (Exception e) {
			throw new Exception("Cannot read private key", e);
		}
	}

	/**
	 * Generates Private Key from BASE64 encoded string
	 */
	public static PrivateKey getPrivateKeyFromString(String keyString) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			PEMParser pemReader = new PEMParser(new StringReader(keyString));
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
			PEMParser pemReader = new PEMParser(new StringReader(keyString));
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
			PEMParser pemReader = new PEMParser(new StringReader(certificateString));
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
		return getPublicKeyFromString(Utilities.toString(inputStream, "UTF-8"));
	}

	/**
	 * Read a keyfile generated with 'openssl genrsa...'
	 */
	public static PrivateKey readRSAPrivateKey(InputStream inputStream) throws Exception {
		return getPrivateKeyFromString(Utilities.toString(inputStream, "UTF-8"));
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
			Cipher encryptCipher = Cipher.getInstance(encryptionMethod, BouncyCastleProvider.PROVIDER_NAME);
			encryptCipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(initializationVector));
			cipherOutputStream = new CipherOutputStream(encryptedOutputStream, encryptCipher);
			Utilities.copy(dataStream, cipherOutputStream);
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
			Cipher decryptCipher = Cipher.getInstance(encryptionMethod, BouncyCastleProvider.PROVIDER_NAME);
			decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(initializationVector));
			cipherInputStream = new CipherInputStream(encryptedDataStream, decryptCipher);
			Utilities.copy(cipherInputStream, dataOutputStream);
		} catch (Exception e) {
			throw new Exception("Error while decrypting: " + e.getMessage(), e);
		} finally {
			if (cipherInputStream != null) {
				cipherInputStream.close();
			}
		}
	}

	public static byte[] stretchPassword(char[] password, int keyLength, byte[] salt) {
		Security.addProvider(new BouncyCastleProvider());
		
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
			Signature signature = Signature.getInstance(signatureMethod, BouncyCastleProvider.PROVIDER_NAME);
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
			Signature signature = Signature.getInstance(signatureMethod, BouncyCastleProvider.PROVIDER_NAME);
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
			Signature signature = Signature.getInstance(signatureMethod, BouncyCastleProvider.PROVIDER_NAME);
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
			Signature signature = Signature.getInstance(signatureMethod, BouncyCastleProvider.PROVIDER_NAME);
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

	public static void otpWork(byte[][] immutableData, byte[][] mutableData) {
		for (int i = 0; i < mutableData.length - 1; i++) {
			Utilities.getRandomByteArray(mutableData[i]);
		}
		byte[] lastArray = mutableData[mutableData.length - 1];
		for (int i = 0; i < immutableData[0].length; i++) {
			lastArray[i] = 0;
			for (byte[] element : immutableData) {
				lastArray[i] = (byte) (lastArray[i] ^ element[i]);
			}
			for (int j = 0; j < mutableData.length - 1; j++) {
				lastArray[i] = (byte) (lastArray[i] ^ mutableData[j][i]);
			}
		}
	}

	public static Set<String> getJarSignatureNames(File jarFile) throws Exception {
		Set<String> returnData = new HashSet<String>();
		JarFile jar = null;
		try {
			jar = new JarFile(jarFile);
			Manifest manifest = jar.getManifest();
			if (manifest == null) {
				returnData.add("Has no MANIFEST.MF file");
				return returnData;
			}

			byte[] buffer = new byte[4096];
			Enumeration<JarEntry> jarEntriesEnumerator = jar.entries();
			List<JarEntry> jarEntries = new ArrayList<JarEntry>();

			while (jarEntriesEnumerator.hasMoreElements()) {
				JarEntry jarEntry = jarEntriesEnumerator.nextElement();
				jarEntries.add(jarEntry);

				InputStream jarEntryInputStream = null;
				try {
					jarEntryInputStream = jar.getInputStream(jarEntry);
					// Reading the jarEntry throws a SecurityException if signature/digest check fails.
					while (jarEntryInputStream.read(buffer, 0, buffer.length) != -1) {
					}
				} finally {
					Utilities.closeQuietly(jarEntryInputStream);
				}
			}

			for (JarEntry jarEntry : jarEntries) {
				if (!jarEntry.isDirectory()) {
					// Every file must be signed, except for files in META-INF
					Certificate[] certs = jarEntry.getCertificates();
					if ((certs == null) || (certs.length == 0)) {
						if (!jarEntry.getName().startsWith("META-INF")) {
							returnData.add("Contains unsigned files");
						}
					} else {
						for (Certificate cert : certs) {
							if (cert instanceof X509Certificate) {
								X509Certificate certificate = (X509Certificate) cert;
								if (certificate.getBasicConstraints() == -1) {
									// Certificate is not a CA certificate
									returnData.add(getCnFromCertificate(certificate));
								} else {
									// Certificate is a CA certificate
									returnData.add("CA: " + getCnFromCertificate(certificate));
								}
							} else {
								returnData.add("Unknown type of certificate");
							}
						}
					}
				}
			}

			return returnData;
		} finally {
			Utilities.closeQuietly(jar);
		}
	}

	public static boolean verifyJarSignature(File jarFile, X509Certificate[] trustedCerts) throws Exception {
		if (trustedCerts == null || trustedCerts.length == 0) {
			return false;
		}

		try (JarFile jar = new JarFile(jarFile)) {
			Manifest manifest = jar.getManifest();
			if (manifest == null) {
				throw new SecurityException("The jar file has no manifest, which contains the file signatures");
			}

			byte[] buffer = new byte[4096];
			Enumeration<JarEntry> jarEntriesEnumerator = jar.entries();
			List<JarEntry> jarEntries = new ArrayList<JarEntry>();

			while (jarEntriesEnumerator.hasMoreElements()) {
				JarEntry jarEntry = jarEntriesEnumerator.nextElement();
				jarEntries.add(jarEntry);

				InputStream jarEntryInputStream = null;
				try {
					jarEntryInputStream = jar.getInputStream(jarEntry);
					// Reading the jarEntry throws a SecurityException if signature/digest check fails.
					while (jarEntryInputStream.read(buffer, 0, buffer.length) != -1) {
					}
				} finally {
					Utilities.closeQuietly(jarEntryInputStream);
				}
			}

			for (JarEntry jarEntry : jarEntries) {
				if (!jarEntry.isDirectory()) {
					// Every file must be signed, except for files in META-INF
					Certificate[] certs = jarEntry.getCertificates();
					if ((certs == null) || (certs.length == 0)) {
						if (!jarEntry.getName().startsWith("META-INF")) {
							throw new SecurityException("The jar file contains unsigned files.");
						}
					} else {
						boolean isSignedByTrustedCert = false;

						for (Certificate chainRootCertificate : certs) {
							if (chainRootCertificate instanceof X509Certificate && verifyChainOfTrust((X509Certificate) chainRootCertificate, trustedCerts)) {
								isSignedByTrustedCert = true;
								break;
							}
						}

						if (!isSignedByTrustedCert) {
							throw new SecurityException("The jar file contains untrusted signed files");
						}
					}
				}
			}

			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean verifyChainOfTrust(X509Certificate cert, X509Certificate[] trustedCerts) throws Exception {
		CertPathBuilder certifier = CertPathBuilder.getInstance("PKIX");
		X509CertSelector targetConstraints = new X509CertSelector();
		targetConstraints.setCertificate(cert);

		Set<TrustAnchor> trustAnchors = new HashSet<TrustAnchor>();
		for (X509Certificate trustedRootCert : trustedCerts) {
			trustAnchors.add(new TrustAnchor(trustedRootCert, null));
		}

		PKIXBuilderParameters params = new PKIXBuilderParameters(trustAnchors, targetConstraints);
		params.setRevocationEnabled(false);
		try {
			PKIXCertPathBuilderResult result = (PKIXCertPathBuilderResult) certifier.build(params);
			return result != null;
		} catch (Exception cpbe) {
			return false;
		}
	}

	public static X509Certificate[] getChainRootCertificates(Certificate[] certs) {
		Vector<X509Certificate> result = new Vector<X509Certificate>();
		for (int i = 0; i < certs.length - 1; i++) {
			if (!((X509Certificate) certs[i + 1]).getSubjectDN().equals(((X509Certificate) certs[i]).getIssuerDN())) {
				result.addElement((X509Certificate) certs[i]);
			}
		}
		// The final entry in the certs array is always a root certificate
		result.addElement((X509Certificate) certs[certs.length - 1]);
		X509Certificate[] returnValue = new X509Certificate[result.size()];
		result.copyInto(returnValue);
		return returnValue;
	}

	public static X509Certificate[] loadCertificatesFromPemFile(File pemFile) throws Exception {
		PEMParser pemReader = null;
		try {
			List<X509Certificate> certificateList = new ArrayList<X509Certificate>();
			pemReader = new PEMParser(new FileReader(pemFile));
			Object nextCertificate;
			while ((nextCertificate = pemReader.readObject()) != null) {
				if (nextCertificate instanceof X509Certificate) {
					certificateList.add((X509Certificate) nextCertificate);
				}
			}
			return certificateList.toArray(new X509Certificate[0]);
		} finally {
			Utilities.closeQuietly(pemReader);
		}
	}

	public static X509Certificate[] loadCertificatesFromPemStream(InputStream pemInputStream) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try (PEMParser pemReader = new PEMParser(new InputStreamReader(pemInputStream))) {
			List<X509Certificate> certificateList = new ArrayList<X509Certificate>();
			Object pemReaderObject;
			while ((pemReaderObject = pemReader.readObject()) != null) {
				if (pemReaderObject instanceof X509Certificate) {
					// Old BouncyCastle API
					certificateList.add((X509Certificate) pemReaderObject);
				} else if (pemReaderObject instanceof X509CertificateHolder) {
					X509Certificate certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate((X509CertificateHolder) pemReaderObject);
					certificateList.add(certificate);
				}
			}
			return certificateList.toArray(new X509Certificate[0]);
		}
	}

	public static String getCnFromCertificate(X509Certificate certificate) throws InvalidNameException {
		Principal principal = certificate.getSubjectDN();
		String dn = principal.getName();
		LdapName ln = new LdapName(dn);

		for (Rdn rdn : ln.getRdns()) {
			if (rdn.getType().equalsIgnoreCase("CN")) {
				return (String) rdn.getValue();
			}
		}

		return null;
	}

	public static boolean checkForCaCertificate(X509Certificate certificate) {
		return certificate.getBasicConstraints() != 1;
	}

	public static String getMd5FingerPrint(X509Certificate certificate) throws InvalidNameException, CertificateEncodingException {
		return DigestUtils.md5Hex(certificate.getEncoded()).toUpperCase();
	}

	public static String getSha1FingerPrint(X509Certificate certificate) throws InvalidNameException, CertificateEncodingException {
		return DigestUtils.sha1Hex(certificate.getEncoded()).toUpperCase();
	}

	public static int getKeySize(X509Certificate certificate) {
		RSAKey rsaKey = (RSAKey) certificate.getPublicKey();
		return rsaKey.getModulus().bitLength();
	}
}
