package de.soderer.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Date;

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
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMReader;
import org.bouncycastle.openssl.PEMWriter;

public class CryptographicUtilities {
	public static Logger logger = Logger.getLogger(CryptographicUtilities.class);
	
	public static byte[] SALT = new byte[]{48, 57, 56, 107, 108, 102, 114, 113, 54, 64, 108, 107};
	public static byte[] AES_INITALIZATION_VECTOR = new byte[]{48, 57, 56, 107, 108, 102, 114, 113, 54, 64, 108, 107, 48, 57, 56, 107};
	public static String AES_ENCRYPTION_METHOD = "AES/CBC/PKCS7Padding";
	
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
	
	@SuppressWarnings("deprecation")
	public static X509Certificate generateRsaCertificate(AsymmetricCipherKeyPair keyPair, String issuerAndSubjectName, Date validFrom, Date validUntil) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			org.bouncycastle.x509.X509V3CertificateGenerator certificateGenerator = new org.bouncycastle.x509.X509V3CertificateGenerator();
			
		    certificateGenerator.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
		    certificateGenerator.setSubjectDN(new X500Principal("CN=" + issuerAndSubjectName));
		    certificateGenerator.setIssuerDN(new X500Principal("CN=" + issuerAndSubjectName));
		    certificateGenerator.setNotBefore(validFrom);
		    certificateGenerator.setNotAfter(validUntil);
		    certificateGenerator.setPublicKey(getPublicKeyFromKeyPair(keyPair));
		    certificateGenerator.setSignatureAlgorithm("SHA256WithRSAEncryption");

		    return certificateGenerator.generate(getPrivateKeyFromKeyPair(keyPair), "BC");
		} catch (Exception e) {
			throw new Exception("Cannot create RSA certificate", e);
		}
	}
	
	public static String getStringFromX509Certificate(X509Certificate certificate) throws Exception {
		PEMWriter pemWriter = null;
		try {
			StringWriter stringWriter = new StringWriter();
			pemWriter = new PEMWriter(stringWriter);
			pemWriter.writeObject(certificate);
			pemWriter.close();
			return stringWriter.toString();
		} catch (Exception e) {
			throw new Exception("Cannot create certificate string: " + e.getMessage(), e);
		} finally {
			if (pemWriter!= null) {
				pemWriter.close();
			}
		}
	}
	
	public static byte[] encryptRsa(byte[] data, PublicKey key) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.ENCRYPT_MODE, key);
			return cipher.doFinal(data);
		} catch (Exception e) {
			throw new Exception("Error while encrypting: " + e.getMessage(), e);
		}
	}
	
	public static byte[] decryptRsa(byte[] encryptedData, PrivateKey key) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
			cipher.init(Cipher.DECRYPT_MODE, key);
			return cipher.doFinal(encryptedData);
		} catch (Exception e) {
			throw new Exception("Error while decrypting: " + e.getMessage(), e);
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
		PEMWriter pemWriter = null;
		try {
			StringWriter stringWriter = new StringWriter();
			pemWriter = new PEMWriter(stringWriter);
			pemWriter.writeObject(privateKey);
			pemWriter.writeObject(publicKey);
			pemWriter.close();
			return stringWriter.toString();
		} catch (Exception e) {
			throw new Exception("Cannot create key string: " + e.getMessage(), e);
		} finally {
			if (pemWriter!= null) {
				pemWriter.close();
			}
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
	public static PublicKey getPublicKeyFromFile(File publicKeyFile) throws Exception {
		String keyString = FileUtils.readFileToString(publicKeyFile);
		return getPublicKeyFromString(keyString);
	}

	/**
	 * Read a keyfile generated with 'openssl genrsa...'
	 */
	public static PrivateKey getPrivateKeyFromFile(File privateKeyFile) throws Exception {
		String keyString = FileUtils.readFileToString(privateKeyFile);
		return getPrivateKeyFromString(keyString);
	}

	/**
	 * Read a certificate file
	 */
	public static X509Certificate getCertificateFromFile(File certificateFile) throws Exception {
		String certificateFileString = FileUtils.readFileToString(certificateFile);
		return getCertificateFromString(certificateFileString);
	}
	
	public static byte[] encryptAes(byte[] data, String key) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		byte[] keyBytes = stretchPassword(key, 128, SALT);
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		Cipher encryptCipher = Cipher.getInstance(AES_ENCRYPTION_METHOD, "BC");
		encryptCipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(AES_INITALIZATION_VECTOR));
		return encryptCipher.doFinal(data);
	}
	
	public static void encryptFileAes(File dataFile, File destinationFile, String key) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		CipherOutputStream cipherOutputStream = null;
		FileInputStream fileInputStream = null;
		FileOutputStream fileOutputStream = null;
		try {
			byte[] keyBytes = stretchPassword(key, 128, SALT);
			SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
			Cipher encryptCipher = Cipher.getInstance(AES_ENCRYPTION_METHOD, "BC");
			encryptCipher.init(Cipher.ENCRYPT_MODE, keySpec, new IvParameterSpec(AES_INITALIZATION_VECTOR));
			fileInputStream = new FileInputStream(dataFile);
			fileOutputStream = new FileOutputStream(destinationFile);
			cipherOutputStream = new CipherOutputStream(fileOutputStream, encryptCipher);
			IOUtils.copy(fileInputStream, cipherOutputStream);
		} finally {
			if (cipherOutputStream != null) {
				cipherOutputStream.close();
			}
			if (fileInputStream != null) {
				fileInputStream.close();
			}
			if (fileOutputStream != null) {
				fileOutputStream.close();
			}
		}
	}

	public static byte[] decryptAes(byte[] enryptedData, String key) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		byte[] keyBytes = stretchPassword(key, 128, SALT);
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		Cipher decryptCipher = Cipher.getInstance(AES_ENCRYPTION_METHOD, "BC");
		decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(AES_INITALIZATION_VECTOR));

		return decryptCipher.doFinal(enryptedData);
	}
	
	public static void decryptFileAes(File enryptedFile, File destinationFile, String key) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		CipherInputStream cipherInputStream = null;
		FileInputStream fileInputStream = null;
		FileOutputStream fileOutputStream = null;
		try {
			byte[] keyBytes = stretchPassword(key, 128, SALT);
			SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
			Cipher decryptCipher = Cipher.getInstance(AES_ENCRYPTION_METHOD, "BC");
			decryptCipher.init(Cipher.DECRYPT_MODE, keySpec, new IvParameterSpec(AES_INITALIZATION_VECTOR));
			fileInputStream = new FileInputStream(enryptedFile);
			fileOutputStream = new FileOutputStream(destinationFile);
			cipherInputStream = new CipherInputStream(fileInputStream, decryptCipher);
			IOUtils.copy(cipherInputStream, fileOutputStream);
		} finally {
			if (cipherInputStream != null) {
				cipherInputStream.close();
			}
			if (fileInputStream != null) {
				fileInputStream.close();
			}
			if (fileOutputStream != null) {
				fileOutputStream.close();
			}
		}
	}
	
	public static byte[] stretchPassword(String password, int keyLength, byte[] salt) {
		PBEParametersGenerator generator = new PKCS5S2ParametersGenerator();
	    generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password.toCharArray()), salt, 1000);
	    KeyParameter params = (KeyParameter) generator.generateDerivedParameters(keyLength);
	    return params.getKey();
	}
}
