package de.soderer.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.RSAPrivateCrtKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

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

public class CryptographicUtilities {
	public static Logger logger = Logger.getLogger(CryptographicUtilities.class);
	
	public static final String PUBLIC_KEY_BEGIN_SIGN = "-----BEGIN PUBLIC KEY-----";
	public static final String PUBLIC_KEY_END_SIGN = "-----END PUBLIC KEY-----";
	public static final String PRIVATE_RSA_KEY_BEGIN_SIGN = "-----BEGIN RSA PRIVATE KEY-----";
	public static final String PRIVATE_RSA_KEY_END_SIGN = "-----END RSA PRIVATE KEY-----";
	
	public static byte[] SALT = new byte[]{48, 57, 56, 107, 108, 102, 114, 113, 54, 64, 108, 107};
	public static byte[] AES_INITALIZATION_VECTOR = new byte[]{48, 57, 56, 107, 108, 102, 114, 113, 54, 64, 108, 107, 48, 57, 56, 107};
	
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

	/**
	 * Convert a Key to string encoded as BASE64
	 * @throws Exception 
	 */
	public static String getStringFromKeyPair(AsymmetricCipherKeyPair keyPair) throws Exception {
		PublicKey publicKey = getPublicKeyFromKeyPair(keyPair);
		PrivateKey privateKey = getPrivateKeyFromKeyPair(keyPair);

		StringBuilder outputString = new StringBuilder();
		outputString.append(PRIVATE_RSA_KEY_BEGIN_SIGN);
		outputString.append("\n");
		outputString.append(TextUtilities.breakLinesAfterMaximumLength(getStringFromKey(privateKey), 64));
		outputString.append("\n");
		outputString.append(PRIVATE_RSA_KEY_END_SIGN);
		outputString.append("\n");
		outputString.append("\n");
		outputString.append(PUBLIC_KEY_BEGIN_SIGN);
		outputString.append("\n");
		outputString.append(TextUtilities.breakLinesAfterMaximumLength(getStringFromKey(publicKey), 64));
		outputString.append("\n");
		outputString.append(PUBLIC_KEY_END_SIGN);
		
		return outputString.toString();
	}
	
	public static PublicKey getPublicKeyFromKeyPair(AsymmetricCipherKeyPair keyPair) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		RSAKeyParameters publicKey = (RSAKeyParameters) keyPair.getPublic();
		return KeyFactory.getInstance("RSA").generatePublic(new RSAPublicKeySpec(publicKey.getModulus(), publicKey.getExponent()));
	}
	
	public static PrivateKey getPrivateKeyFromKeyPair(AsymmetricCipherKeyPair keyPair) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		RSAPrivateCrtKeyParameters privateKey = (RSAPrivateCrtKeyParameters)keyPair.getPrivate();
		RSAKeyParameters publicKey = (RSAKeyParameters)keyPair.getPublic();
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
			if (keyString.contains(PRIVATE_RSA_KEY_BEGIN_SIGN)) {
				keyString = keyString.substring(keyString.indexOf(PRIVATE_RSA_KEY_BEGIN_SIGN) + PRIVATE_RSA_KEY_BEGIN_SIGN.length());
			}
			if (keyString.contains(PRIVATE_RSA_KEY_END_SIGN)) {
				keyString = keyString.substring(0, keyString.indexOf(PRIVATE_RSA_KEY_END_SIGN));
			}
			keyString = keyString.replace("\n", "").replace("\r", "");
			
			KeyFactory keyFactory = KeyFactory.getInstance("RSA", new BouncyCastleProvider());
			EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(keyString));
			PrivateKey privateKey = keyFactory.generatePrivate(privateKeySpec);
			return privateKey;
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
			if (keyString.contains(PUBLIC_KEY_BEGIN_SIGN)) {
				keyString = keyString.substring(keyString.indexOf(PUBLIC_KEY_BEGIN_SIGN) + PUBLIC_KEY_BEGIN_SIGN.length());
			}
			if (keyString.contains(PUBLIC_KEY_END_SIGN)) {
				keyString = keyString.substring(0, keyString.indexOf(PUBLIC_KEY_END_SIGN));
			}
			keyString = keyString.replace("\n", "").replace("\r", "");
			
			KeyFactory keyFactory = KeyFactory.getInstance("RSA", new BouncyCastleProvider());
			EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.decodeBase64(keyString));
			PublicKey publicKey = keyFactory.generatePublic(publicKeySpec);
			return publicKey;
		} catch (Exception e) {
			throw new Exception("Cannot read public key", e);
		}
	}

	/**
	 * Read a keyfile generated with 'openssl genrsa...' and format it
	 */
	public static PublicKey getPublicKeyFromFile(File publicKeyFile) throws Exception {
		String keyString = FileUtils.readFileToString(publicKeyFile);
		return getPublicKeyFromString(keyString);
	}

	/**
	 * Read a keyfile generated with 'openssl genrsa...' and format it
	 */
	public static PrivateKey getPrivateKeyFromFile(File privateKeyFile) throws Exception {
		String keyString = FileUtils.readFileToString(privateKeyFile);
		return getPrivateKeyFromString(keyString);
	}
	
	public static byte[] encryptAes(byte[] data, String key) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		byte[] keyBytes = stretchPassword(key, 128, SALT);
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
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
			Cipher encryptCipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
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
		Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
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
			Cipher decryptCipher = Cipher.getInstance("AES/CBC/PKCS7Padding", "BC");
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
