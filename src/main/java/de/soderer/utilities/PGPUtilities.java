package de.soderer.utilities;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.security.Security;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.HashAlgorithmTags;
import org.bouncycastle.bcpg.PublicKeyAlgorithmTags;
import org.bouncycastle.bcpg.SymmetricKeyAlgorithmTags;
import org.bouncycastle.bcpg.sig.Features;
import org.bouncycastle.bcpg.sig.KeyFlags;
import org.bouncycastle.bcpg.sig.NotationData;
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.generators.DSAKeyPairGenerator;
import org.bouncycastle.crypto.generators.DSAParametersGenerator;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.DSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPEncryptedData;
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
import org.bouncycastle.openpgp.PGPSignatureGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketGenerator;
import org.bouncycastle.openpgp.PGPSignatureSubpacketVector;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.operator.PBESecretKeyDecryptor;
import org.bouncycastle.openpgp.operator.PBESecretKeyEncryptor;
import org.bouncycastle.openpgp.operator.PGPDigestCalculator;
import org.bouncycastle.openpgp.operator.bc.BcPBESecretKeyEncryptorBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.bc.BcPGPDigestCalculatorProvider;
import org.bouncycastle.openpgp.operator.bc.BcPGPKeyPair;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcaPGPContentSignerBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;

public class PGPUtilities {
	public static final String DEFAULT_SYMMETRIC_ENCRYPTION_METHOD = "AES-256";

	public enum PgpSymmetricEncryptionMethod {
	    IDEA(SymmetricKeyAlgorithmTags.IDEA),
	    TRIPLE_DES(SymmetricKeyAlgorithmTags.TRIPLE_DES),
	    CAST5(SymmetricKeyAlgorithmTags.CAST5),
	    BLOWFISH(SymmetricKeyAlgorithmTags.BLOWFISH),
	    SAFER(SymmetricKeyAlgorithmTags.SAFER),
	    DES(SymmetricKeyAlgorithmTags.DES),
	    AES_128(SymmetricKeyAlgorithmTags.AES_128),
	    AES_192(SymmetricKeyAlgorithmTags.AES_192),
	    AES_256(SymmetricKeyAlgorithmTags.AES_256),
	    TWOFISH(SymmetricKeyAlgorithmTags.TWOFISH);
	    
		private int id;
		
	    private PgpSymmetricEncryptionMethod(int id) {
	    	this.id = id;
	    }
	    
	    public int getId() {
	    	return id;
	    }
	    
	    public static String[] names() {
	    	String[] names = new String[PgpSymmetricEncryptionMethod.values().length];
	    	for (int i = 0; i < PgpSymmetricEncryptionMethod.values().length; i++) {
	    		names[i] = PgpSymmetricEncryptionMethod.values()[i].name();
	    	}
	    	return names;
	    }
	    
	    public static PgpSymmetricEncryptionMethod getByName(String methodName) throws Exception {
	    	String searchMethodName = methodName.replace("-", "").replace("_", "");
	    	for (PgpSymmetricEncryptionMethod encryptionMethod : PgpSymmetricEncryptionMethod.values()) {
	    		if (encryptionMethod.toString().replace("-", "").replace("_", "").equalsIgnoreCase(searchMethodName)) {
	    			return encryptionMethod;
	    		}
	    	}
	    	throw new Exception("Invalid PgpEncryptionMethod name: " + methodName);
	    }
	    
	    public static PgpSymmetricEncryptionMethod getById(int id) throws Exception {
	    	for (PgpSymmetricEncryptionMethod encryptionMethod : PgpSymmetricEncryptionMethod.values()) {
	    		if (encryptionMethod.getId() == id) {
	    			return encryptionMethod;
	    		}
	    	}
	    	throw new Exception("Invalid PgpEncryptionMethod id: " + id);
	    }
	}
	
	public static final String DEFAULT_PUBLIC_ENCRYPTION_METHOD = "RSA-GENERAL";
	
	public enum PgpPublicEncryptionMethod {
		RSA(PublicKeyAlgorithmTags.RSA_GENERAL),
		RSA_GENERAL(PublicKeyAlgorithmTags.RSA_GENERAL),
	    RSA_ENCRYPT(PublicKeyAlgorithmTags.RSA_ENCRYPT),
	    RSA_SIGN(PublicKeyAlgorithmTags.RSA_SIGN),

	    ELGAMAL(PublicKeyAlgorithmTags.ELGAMAL_GENERAL),
	    ELGAMAL_GENERAL(PublicKeyAlgorithmTags.ELGAMAL_GENERAL),
	    ELGAMAL_ENCRYPT(PublicKeyAlgorithmTags.ELGAMAL_ENCRYPT),
	    
	    DSA(PublicKeyAlgorithmTags.DSA),
	    /**
	     * @deprecated use ECDH
	     */
	    EC(PublicKeyAlgorithmTags.EC),
	    ECDH(PublicKeyAlgorithmTags.ECDH),
	    ECDSA(PublicKeyAlgorithmTags.ECDSA),
	    DIFFIE_HELLMAN(PublicKeyAlgorithmTags.DIFFIE_HELLMAN);
	    
		private int id;
		
	    private PgpPublicEncryptionMethod(int id) {
	    	this.id = id;
	    }
	    
	    public int getId() {
	    	return id;
	    }
	    
	    public static String[] names() {
	    	String[] names = new String[PgpPublicEncryptionMethod.values().length];
	    	for (int i = 0; i < PgpPublicEncryptionMethod.values().length; i++) {
	    		names[i] = PgpPublicEncryptionMethod.values()[i].name();
	    	}
	    	return names;
	    }
	    
	    public static PgpPublicEncryptionMethod getByName(String methodName) throws Exception {
	    	String searchMethodName = methodName.replace("-", "").replace("_", "");
	    	for (PgpPublicEncryptionMethod encryptionMethod : PgpPublicEncryptionMethod.values()) {
	    		if (encryptionMethod.toString().replace("-", "").replace("_", "").equalsIgnoreCase(searchMethodName)) {
	    			return encryptionMethod;
	    		}
	    	}
	    	throw new Exception("Invalid PgpPublicEncryptionMethod name: " + methodName);
	    }
	    
	    public static PgpPublicEncryptionMethod getById(int id) throws Exception {
	    	for (PgpPublicEncryptionMethod encryptionMethod : PgpPublicEncryptionMethod.values()) {
	    		if (encryptionMethod.getId() == id) {
	    			return encryptionMethod;
	    		}
	    	}
	    	throw new Exception("Invalid PgpPublicEncryptionMethod id: " + id);
	    }
	}

	public static final String DEFAULT_HASHING_METHOD = "SHA256";
	
	public enum PgpHashMethod {
	    MD5(HashAlgorithmTags.MD5),
	    SHA1(HashAlgorithmTags.SHA1),
	    RIPEMD160(HashAlgorithmTags.RIPEMD160),
	    DOUBLE_SHA(HashAlgorithmTags.DOUBLE_SHA),
	    MD2(HashAlgorithmTags.MD2),
	    TIGER_192(HashAlgorithmTags.TIGER_192),
	    HAVAL_5_160(HashAlgorithmTags.HAVAL_5_160),
	    SHA256(HashAlgorithmTags.SHA256),
	    SHA384(HashAlgorithmTags.SHA384),
	    SHA512(HashAlgorithmTags.SHA512),
	    SHA224(HashAlgorithmTags.SHA224);
	    
		private int id;
		
	    private PgpHashMethod(int id) {
	    	this.id = id;
	    }
	    
	    public int getId() {
	    	return id;
	    }
	    
	    public static String[] names() {
	    	String[] names = new String[PgpHashMethod.values().length];
	    	for (int i = 0; i < PgpHashMethod.values().length; i++) {
	    		names[i] = PgpHashMethod.values()[i].name();
	    	}
	    	return names;
	    }
	    
	    public static PgpHashMethod getByName(String methodName) throws Exception {
	    	String searchMethodName = methodName.replace("-", "").replace("_", "");
	    	for (PgpHashMethod encryptionMethod : PgpHashMethod.values()) {
	    		if (encryptionMethod.toString().replace("-", "").replace("_", "").equalsIgnoreCase(searchMethodName)) {
	    			return encryptionMethod;
	    		}
	    	}
	    	throw new Exception("Invalid PgpHashMethod name: " + methodName);
	    }
	    
	    public static PgpHashMethod getById(int id) throws Exception {
	    	for (PgpHashMethod encryptionMethod : PgpHashMethod.values()) {
	    		if (encryptionMethod.getId() == id) {
	    			return encryptionMethod;
	    		}
	    	}
	    	throw new Exception("Invalid PgpHashMethod id: " + id);
	    }
	}
	
	public static PGPSecretKey generatePGPSecretKey(int keyStrength, PgpPublicEncryptionMethod keyType, String identity, char[] password, Date validity) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		AsymmetricCipherKeyPairGenerator keyPairGenerator;
		KeyGenerationParameters keyGenerationParameters;
		if (keyType == PgpPublicEncryptionMethod.RSA
				|| keyType == PgpPublicEncryptionMethod.RSA_GENERAL
				|| keyType == PgpPublicEncryptionMethod.RSA_ENCRYPT
				|| keyType == PgpPublicEncryptionMethod.RSA_SIGN) {
			keyPairGenerator = new RSAKeyPairGenerator();
			keyGenerationParameters = new RSAKeyGenerationParameters(BigInteger.valueOf(0x10001), new SecureRandom(), keyStrength, 80);
//		} else if (keyType == PgpPublicEncryptionMethod.ELGAMAL
//				|| keyType == PgpPublicEncryptionMethod.ELGAMAL_GENERAL
//				|| keyType == PgpPublicEncryptionMethod.ELGAMAL_ENCRYPT) {
//			keyPairGenerator = new ElGamalKeyPairGenerator();
//			ElGamalParametersGenerator elGamalParametersGenerator = new ElGamalParametersGenerator();
//			elGamalParametersGenerator.init(keyStrength, 12, new SecureRandom());
//			keyGenerationParameters = new ElGamalKeyGenerationParameters(new SecureRandom(), elGamalParametersGenerator.generateParameters());
		} else if (keyType == PgpPublicEncryptionMethod.DSA) {
			keyPairGenerator = new DSAKeyPairGenerator();
			DSAParametersGenerator dsaParametersGenerator = new DSAParametersGenerator();
			dsaParametersGenerator.init(keyStrength, 80, new SecureRandom());
			keyGenerationParameters = new DSAKeyGenerationParameters(new SecureRandom(), dsaParametersGenerator.generateParameters());
		} else {
			throw new Exception("Unsupported keytype: " + keyType.name());
		}
		
		keyPairGenerator.init(keyGenerationParameters);
		
		PGPKeyPair keyPair = new BcPGPKeyPair(keyType.getId(), keyPairGenerator.generateKeyPair(), new Date());
		
		PGPSignatureSubpacketGenerator subpacketGenerator = new PGPSignatureSubpacketGenerator();
		
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
		
        long secondsUntilExpiration = (validity.getTime() - new Date().getTime()) / 1000L + 1;
		subpacketGenerator.setKeyExpirationTime(true, secondsUntilExpiration);
        
		subpacketGenerator.setNotationData(false, true, "identity", identity);
		
		// Objects used to encrypt the secret key.
		PGPDigestCalculator sha1Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1);
		PGPDigestCalculator sha256Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA256);
		
		PBESecretKeyEncryptor pske = (new BcPBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha256Calc, 0xc0)).build(password);
		
		PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, keyPair, identity, sha1Calc, subpacketGenerator.generate(), null, new BcPGPContentSignerBuilder(keyPair.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1), pske);

        PGPSecretKeyRing skr = keyRingGen.generateSecretKeyRing();
        
        Iterator<PGPSecretKey> secretKeys = skr.getSecretKeys();
        PGPSecretKey secretKey = null;
        while (secretKeys.hasNext()) {
        	secretKey = secretKeys.next();
        	break;
        }
        return secretKey;
        
        // TODO
//		try {
//			RSAKeyPairGenerator keyPairGenerator = new RSAKeyPairGenerator();
//			keyPairGenerator.init(new RSAKeyGenerationParameters(RSAKeyGenParameterSpec.F4, new SecureRandom(), keyStrength, 80));
//			AsymmetricCipherKeyPair keyPair = keyPairGenerator.generateKeyPair();
//
//			PublicKeyPacket publicKeyPacket = new PublicKeyPacket(PgpPublicEncryptionMethod.getByName(DEFAULT_PUBLIC_ENCRYPTION_METHOD).getId(), new Date(), null);
//			
//			PGPPublicKey publicKey = new PGPPublicKey(publicKeyPacket, new JcaKeyFingerprintCalculator());
//			PGPPublicKey publicKey = new PGPPublicKey(PgpPublicEncryptionMethod.getByName(DEFAULT_PUBLIC_ENCRYPTION_METHOD).getId(), keyPair.getPublic(), new Date());
//			PGPPrivateKey privateKey = new PGPPrivateKey(keyPair.getPrivate(), publicKey.getKeyID());
//			PGPKeyPair masterKeyPair = new PGPKeyPair(publicKey, privateKey);
//
//			long secondsUntilExpiration = (validity.getTime() - new Date().getTime()) / 1000L + 1;
//
//			PGPSignatureSubpacketGenerator subpacketGenerator = new PGPSignatureSubpacketGenerator();
//			PGPSignatureSubpacketVector subpacketVector = null;
//			subpacketGenerator.setKeyExpirationTime(false, secondsUntilExpiration);
//			subpacketGenerator.setNotationData(true, true, "identity", identity);
//			subpacketVector = subpacketGenerator.generate();
//			PGPKeyRingGenerator keyringGenerator = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, masterKeyPair, identity, PgpHashMethod.getByName(DEFAULT_HASHING_METHOD).getId(), password, subpacketVector, null, new SecureRandom(), BouncyCastleProvider.PROVIDER_NAME);
//
//			PGPSecretKeyRing secretKeyRing = keyringGenerator.generateSecretKeyRing();
//
//			return secretKeyRing.getSecretKey();
//		} catch (PGPException e) {
//			if (e.getUnderlyingException() != null && e.getUnderlyingException().getMessage().contains("Illegal key size")) {
//				throw new UserError("keystrengthExceedsAllowedJCE", e);
//			} else {
//				throw new Exception("Cannot create PGP key", e);
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new Exception("Cannot create PGP key", e);
//		}
	}
	
	public static PGPSecretKey generateDualKeyPGPSecretKey(int keyStrength, String identity, char[] password, Date validity) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		RSAKeyPairGenerator keyPairGenerator = new RSAKeyPairGenerator();
		keyPairGenerator.init(new RSAKeyGenerationParameters(BigInteger.valueOf(0x10001), new SecureRandom(), keyStrength, 80));
		
		// First create the master (signing) key with the generator.
		PGPKeyPair rsakp_sign = new BcPGPKeyPair(PGPPublicKey.RSA_SIGN, keyPairGenerator.generateKeyPair(), new Date());
		// Then an encryption subkey.
		PGPKeyPair rsakp_enc = new BcPGPKeyPair(PGPPublicKey.RSA_ENCRYPT, keyPairGenerator.generateKeyPair(), new Date());
		
		// Add a self-signature on the id
		PGPSignatureSubpacketGenerator subpacketGenerator = new PGPSignatureSubpacketGenerator();
		
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
		
        long secondsUntilExpiration = (validity.getTime() - new Date().getTime()) / 1000L + 1;
		subpacketGenerator.setKeyExpirationTime(true, secondsUntilExpiration);
        
		subpacketGenerator.setNotationData(true, true, "identity", identity);
		
		// Create a signature on the encryption subkey.
		PGPSignatureSubpacketGenerator enchashgen = new PGPSignatureSubpacketGenerator();
		// Add metadata to declare its purpose
		enchashgen.setKeyFlags(false, KeyFlags.ENCRYPT_COMMS|KeyFlags.ENCRYPT_STORAGE);
		
		// Objects used to encrypt the secret key.
		PGPDigestCalculator sha1Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA1);
		PGPDigestCalculator sha256Calc = new BcPGPDigestCalculatorProvider().get(HashAlgorithmTags.SHA256);
		
		// bcpg 1.48 exposes this API that includes s2kcount. Earlier versions use a default of 0x60.
		PBESecretKeyEncryptor pske = (new BcPBESecretKeyEncryptorBuilder(PGPEncryptedData.AES_256, sha256Calc, 0xc0)).build(password);
		
		// Finally, create the keyring itself. The constructor takes parameters that allow it to generate the self signature.
		PGPKeyRingGenerator keyRingGen = new PGPKeyRingGenerator(PGPSignature.POSITIVE_CERTIFICATION, rsakp_sign, identity, sha1Calc, subpacketGenerator.generate(), null, new BcPGPContentSignerBuilder(rsakp_sign.getPublicKey().getAlgorithm(), HashAlgorithmTags.SHA1), pske);
		
		// Add our encryption subkey, together with its signature.
		keyRingGen.addSubKey(rsakp_enc, enchashgen.generate(), null);

        PGPSecretKeyRing skr = keyRingGen.generateSecretKeyRing();
        
        Iterator<PGPSecretKey> secretKeys = skr.getSecretKeys();
        PGPSecretKey secretKey = null;
        while (secretKeys.hasNext()) {
        	secretKey = secretKeys.next();
        }
        return secretKey;
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
			PGPPublicKeyRingCollection pgpPublicKeyRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(new PGPKeyStorageStringSplitter(inputStream).getPublicKeyInputStream()), new JcaKeyFingerprintCalculator());
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
			PGPSecretKeyRingCollection pgpSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(new PGPKeyStorageStringSplitter(inputStream).getPrivateKeyInputStream()), new JcaKeyFingerprintCalculator());
			@SuppressWarnings("rawtypes")
			Iterator keyRingIterator = pgpSecretKeyRingCollection.getKeyRings();
			while (keyRingIterator.hasNext()) {
				PGPSecretKeyRing keyRing = (PGPSecretKeyRing) keyRingIterator.next();
				@SuppressWarnings("rawtypes")
				Iterator privateKeyIterator = keyRing.getSecretKeys();
				while (privateKeyIterator.hasNext()) {
					PGPSecretKey nextPGPSecretKey = (PGPSecretKey) privateKeyIterator.next();
					if (nextPGPSecretKey.isMasterKey()) {
						return nextPGPSecretKey;
					}
				}
			}

			// key not found
			return null;
		} catch (Exception e) {
			return null;
		}
	}

	public static PGPPrivateKey readPGPPrivateKey(PGPSecretKey secretKey, char[] password) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		try {
			if (secretKey == null) {
				return null;
			} else {
				PBESecretKeyDecryptor decryptor = new JcePBESecretKeyDecryptorBuilder().build(password);
				return secretKey.extractPrivateKey(decryptor);
			}
		} catch (PGPException e) {
			if (e.getUnderlyingException() != null && e.getUnderlyingException().getMessage().contains("Illegal key size")) {
				throw new UserError("keystrengthExceedsAllowedJCE", e);
			} else {
				throw new UserError("cannotReadPrivateKey", e);
			}
		} catch (Exception e) {
			throw new UserError("cannotReadPrivateKey", e);
		}
	}

	public static PGPPublicKey readPGPPublicKey(PGPSecretKey pgpSecretKey) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		if (pgpSecretKey == null) {
			return null;
		} else {
			return pgpSecretKey.getPublicKey();
		}
	}

	public static PGPPublicKey signPublicKey(PGPSecretKey pgpSecretKey, char[] password, PGPPublicKey publicKeyToBeSigned) throws Exception {
		return signPublicKey(pgpSecretKey, password, publicKeyToBeSigned, null);
	}
	
	public static PGPPublicKey signPublicKey(PGPSecretKey pgpSecretKey, char[] password, PGPPublicKey publicKeyToBeSigned, Map<String, String> notationData) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		PGPPrivateKey pgpPrivKey = readPGPPrivateKey(pgpSecretKey, password);
		PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(PgpPublicEncryptionMethod.RSA_GENERAL.getId(), PgpHashMethod.getByName(DEFAULT_HASHING_METHOD).getId()));
		signatureGenerator.init(PGPSignature.DIRECT_KEY, pgpPrivKey);
		
		if (notationData == null) {
			PGPSignatureSubpacketGenerator pgpSignatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
	        pgpSignatureSubpacketGenerator.setNotationData(true, true, "identity", PGPUtilities.getIdentityFromPgpPublicKey(pgpSecretKey.getPublicKey()));
	        PGPSignatureSubpacketVector pgpSignatureSubpacketVector = pgpSignatureSubpacketGenerator.generate();
			signatureGenerator.setHashedSubpackets(pgpSignatureSubpacketVector);
		} else if (!notationData.isEmpty()) {
			PGPSignatureSubpacketGenerator pgpSignatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
			for (Entry<String, String> notation : notationData.entrySet()) {
		        pgpSignatureSubpacketGenerator.setNotationData(true, true, notation.getKey(), notation.getValue());
		        PGPSignatureSubpacketVector pgpSignatureSubpacketVector = pgpSignatureSubpacketGenerator.generate();
				signatureGenerator.setHashedSubpackets(pgpSignatureSubpacketVector);
			}
		}
        
		PGPSignature certification = signatureGenerator.generateCertification(publicKeyToBeSigned);
		return PGPPublicKey.addCertification(publicKeyToBeSigned, certification);
	}

	public static PGPPublicKey signPublicKey(PGPPrivateKey pgpPrivKey, PGPPublicKey pgpPublicKey, PGPPublicKey publicKeyToBeSigned) throws Exception {
		return signPublicKey(pgpPrivKey, pgpPublicKey, publicKeyToBeSigned, null);
	}
	
	public static PGPPublicKey signPublicKey(PGPPrivateKey pgpPrivKey, PGPPublicKey pgpPublicKey, PGPPublicKey publicKeyToBeSigned, Map<String, String> notationData) throws Exception {
		Security.addProvider(new BouncyCastleProvider());
		
		PGPSignatureGenerator signatureGenerator = new PGPSignatureGenerator(new JcaPGPContentSignerBuilder(PgpPublicEncryptionMethod.RSA_GENERAL.getId(), PgpHashMethod.getByName(DEFAULT_HASHING_METHOD).getId()));
		signatureGenerator.init(PGPSignature.DIRECT_KEY, pgpPrivKey);
		
		if (notationData == null) {
			PGPSignatureSubpacketGenerator pgpSignatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
	        pgpSignatureSubpacketGenerator.setNotationData(true, true, "identity", PGPUtilities.getIdentityFromPgpPublicKey(pgpPublicKey));
	        PGPSignatureSubpacketVector pgpSignatureSubpacketVector = pgpSignatureSubpacketGenerator.generate();
			signatureGenerator.setHashedSubpackets(pgpSignatureSubpacketVector);
		} else if (!notationData.isEmpty()) {
			PGPSignatureSubpacketGenerator pgpSignatureSubpacketGenerator = new PGPSignatureSubpacketGenerator();
			for (Entry<String, String> notation : notationData.entrySet()) {
		        pgpSignatureSubpacketGenerator.setNotationData(true, true, notation.getKey(), notation.getValue());
		        PGPSignatureSubpacketVector pgpSignatureSubpacketVector = pgpSignatureSubpacketGenerator.generate();
				signatureGenerator.setHashedSubpackets(pgpSignatureSubpacketVector);
			}
		}
        
		PGPSignature certification = signatureGenerator.generateCertification(publicKeyToBeSigned);
		return PGPPublicKey.addCertification(publicKeyToBeSigned, certification);
	}
	
	public static String getIdentityFromPgpPublicKey(PGPPublicKey publicKey) {
		Security.addProvider(new BouncyCastleProvider());
		
		StringBuilder identities = new StringBuilder();
		@SuppressWarnings("unchecked")
		Iterator<String> userIdentities = publicKey.getUserIDs();
		while (userIdentities.hasNext()) {
			if (identities.length() > 0) {
				identities.append(", ");
			}
			identities.append(userIdentities.next());
		}
		return identities.toString();
	}
	
	public static List<Map<String, Object>> getKeySigners(PGPPublicKey publicKey) {
		Security.addProvider(new BouncyCastleProvider());
		
		List<Map<String, Object>> signersData = new ArrayList<Map<String, Object>>();
		@SuppressWarnings("unchecked")
		Iterator<PGPSignature> signerIterator = publicKey.getSignatures();
		while (signerIterator.hasNext()) {
			Map<String, Object> signatureData = new HashMap<String, Object>();
			PGPSignature signature = signerIterator.next();
			
			long id = signature.getKeyID();
			signatureData.put("id", id);
			
			// time = signature.getHashedSubPackets().getSignatureCreationTime();
			signatureData.put("creationtime", signature.getCreationTime());
			
			String encryptionAlgorithm;
			try {
				encryptionAlgorithm = PgpPublicEncryptionMethod.getById(signature.getKeyAlgorithm()).name();
			} catch (Exception e1) {
				encryptionAlgorithm = LangResources.get("Unknown");
			}
			signatureData.put("encryptionalgorithm", encryptionAlgorithm);
					
			String hashingAlgorithm;
			try {
				hashingAlgorithm = PgpHashMethod.getById(signature.getHashAlgorithm()).name();
			} catch (Exception e1) {
				hashingAlgorithm = LangResources.get("Unknown");
			}
			signatureData.put("hashingalgorithm", hashingAlgorithm);
			
			String identity = "";
			NotationData[] notationData = signature.getHashedSubPackets().getNotationDataOccurrences();
			for (int i = 0; i < notationData.length; i++) {
				if (identity.length() > 0) {
					identity += ", ";
				}
				if (notationData.length > 1 || Utilities.isBlank(notationData[i].getNotationValue())) {
					identity += notationData[i].getNotationName() + ": ";
				}
				identity += notationData[i].getNotationValue();
			}
			if (publicKey != null && id == publicKey.getKeyID()) {
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
	
	public static List<String> getSignerKeyIDs(PGPPublicKey publicKey) {
		Security.addProvider(new BouncyCastleProvider());
		
		List<String> signatureIDs = new ArrayList<String>();
		@SuppressWarnings("unchecked")
		Iterator<PGPSignature> signerIterator = publicKey.getSignatures();
		while (signerIterator.hasNext()) {
			PGPSignature signature = signerIterator.next();
			long id = signature.getKeyID();
			signatureIDs.add(Long.toHexString(id).toUpperCase());
		}
		return signatureIDs;
	}
}
