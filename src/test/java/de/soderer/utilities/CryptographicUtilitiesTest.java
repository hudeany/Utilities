package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.CertPath;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.Cipher;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.Test;

import de.soderer.utilities.crypto.AsymmetricDecryptionWorker;
import de.soderer.utilities.crypto.AsymmetricEncryptionWorker;
import de.soderer.utilities.crypto.AsymmetricSignatureWorker;
import de.soderer.utilities.crypto.AsymmetricVerificationWorker;
import de.soderer.utilities.crypto.CryptographicUtilities;
import de.soderer.utilities.crypto.HybridDecryptionWorker;
import de.soderer.utilities.crypto.HybridEncryptionWorker;
import de.soderer.utilities.crypto.SymmetricDecryptionWorker;
import de.soderer.utilities.crypto.SymmetricEncryptionWorker;

@SuppressWarnings("static-method")
public class CryptographicUtilitiesTest {
	private static KeyPair rsaKeyPairCache = null;
	private static KeyPair elGamalKeyPairCache = null;
	private static KeyPair ecKeyPair = null;
	private static KeyPair dsaKeyPairCache = null;
	private static KeyPair ed25519KeyPair = null;
	private static KeyPair ed448KeyPair = null;

	private static KeyPair getRsaKeyPair() throws Exception {
		if (rsaKeyPairCache == null) {
			rsaKeyPairCache = CryptographicUtilities.generateRsaKeyPair(2048);
		}

		return rsaKeyPairCache;
	}

	private static KeyPair getElGamalKeyPair() throws Exception {
		if (elGamalKeyPairCache == null) {
			elGamalKeyPairCache = CryptographicUtilities.generateElGamalKeyPair(512);
		}

		return elGamalKeyPairCache;
	}

	private static KeyPair getEcKeyPair() throws Exception {
		if (ecKeyPair == null) {
			ecKeyPair = CryptographicUtilities.generateEcKeyPair(CryptographicUtilities.DEFAULT_ELLIPTIC_CURVE_NAME);
		}

		return ecKeyPair;
	}

	private static KeyPair getDsaKeyPair() throws Exception {
		if (dsaKeyPairCache == null) {
			dsaKeyPairCache = CryptographicUtilities.generateDsaKeyPair(2048);
		}

		return dsaKeyPairCache;
	}

	private static KeyPair getEd25519KeyPair() throws Exception {
		if (ed25519KeyPair == null) {
			ed25519KeyPair = CryptographicUtilities.generateEd25519KeyPair();
		}

		return ed25519KeyPair;
	}

	private static KeyPair getEd448KeyPair() throws Exception {
		if (ed448KeyPair == null) {
			ed448KeyPair = CryptographicUtilities.generateEd448KeyPair();
		}

		return ed448KeyPair;
	}

	@Test
	public void testEncryptionAES() {
		try {
			final char[] password = Utilities.getRandomString(12).toCharArray();
			byte[] encryptedData = null;
			try {
				final ByteArrayInputStream dataInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				final ByteArrayOutputStream encryptedDataOutputStream = new ByteArrayOutputStream();
				new SymmetricEncryptionWorker(null, dataInputStream, encryptedDataOutputStream, password, CryptographicUtilities.DEFAULT_SYMMETRIC_ENCRYPTION_METHOD).work();
				encryptedData = encryptedDataOutputStream.toByteArray();
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Encryption failed. Method: " + CryptographicUtilities.DEFAULT_SYMMETRIC_ENCRYPTION_METHOD + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream encryptedDataInputStream = new ByteArrayInputStream(encryptedData);
				final ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();
				new SymmetricDecryptionWorker(null, encryptedDataInputStream, dataOutputStream, password, CryptographicUtilities.DEFAULT_SYMMETRIC_ENCRYPTION_METHOD).work();
				final byte[] decryptedData = dataOutputStream.toByteArray();
				assertArrayEquals(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8), decryptedData);
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Decryption failed. Method: " + CryptographicUtilities.DEFAULT_SYMMETRIC_ENCRYPTION_METHOD + ":\n" + e.getMessage());
			}
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSignatureRSA() {
		try {
			byte[] signatureBytes = null;
			try {
				final ByteArrayInputStream dataToSignInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				signatureBytes = new AsymmetricSignatureWorker(null, dataToSignInputStream, getRsaKeyPair().getPrivate(), CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA).work();
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Signature failed. Method: " + CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream dataToVerifyInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				final boolean result = new AsymmetricVerificationWorker(null, dataToVerifyInputStream, signatureBytes, getRsaKeyPair().getPublic(), CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA).work();
				assertTrue(result);
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Verification failed. Method: " + CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA + ":\n" + e.getMessage());
			}

			assertTrue(CryptographicUtilities.checkPrivateKeyFitsPublicKey(getRsaKeyPair().getPrivate(), getRsaKeyPair().getPublic()));
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEncryptionRSA() {
		for (final String encryptionMethod : CryptographicUtilities.KNOWN_ASYMMETRIC_ENCRYPTION_METHODS_RSA) {
			byte[] encryptedDataStreamed = null;
			try {
				final ByteArrayInputStream dataStream = new ByteArrayInputStream((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8));
				final ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
				new AsymmetricEncryptionWorker(null, dataStream, encryptedStream, getRsaKeyPair().getPublic(), encryptionMethod).work();
				encryptedDataStreamed = encryptedStream.toByteArray();
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Encryption failed. Method: " + encryptionMethod + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream encryptedDataStream = new ByteArrayInputStream(encryptedDataStreamed);
				final ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
				new AsymmetricDecryptionWorker(null, encryptedDataStream, decryptedStream, getRsaKeyPair().getPrivate(), encryptionMethod).work();
				assertArrayEquals((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8), decryptedStream.toByteArray());
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Decryption failed. Method: " + encryptionMethod + ":\n" + e.getMessage());
			}
		}
	}

	@Test
	public void testSignatureEC() {
		try {
			byte[] signatureBytes = null;
			try {
				final ByteArrayInputStream dataToSignInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				signatureBytes = new AsymmetricSignatureWorker(null, dataToSignInputStream, getEcKeyPair().getPrivate(), CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_EC).work();
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Signature failed. Method: " + CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream dataToVerifyInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				final boolean result = new AsymmetricVerificationWorker(null, dataToVerifyInputStream, signatureBytes, getEcKeyPair().getPublic(), CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_EC).work();
				assertTrue(result);
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Verification failed. Method: " + CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA + ":\n" + e.getMessage());
			}

			assertTrue(CryptographicUtilities.checkPrivateKeyFitsPublicKey(getEcKeyPair().getPrivate(), getEcKeyPair().getPublic()));
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSignatureEC_invalidEmptySignatureBytes() {
		try {
			final KeyPair keys = KeyPairGenerator.getInstance("EC").generateKeyPair();
			final byte[] blankSignature = new byte[64];
			final Signature signature = Signature.getInstance("SHA256WithECDSAInP1363Format");
			signature.initVerify(keys.getPublic());
			signature.update("Hello, World".getBytes());
			final boolean result = signature.verify(blankSignature);
			// CVE-2022-21449 ("Psychic Signatures"): before the April 2022 JDK patches, SunEC's
			// ECDSA verification accepted an all-zero r/s signature as valid for any key/message.
			// Fixed since JDK 15.0.7/17.0.3/18.0.1; this assertion would fail again if that
			// regression ever reappeared.
			assertFalse(result, "Java JRE accepts invalid empty signature (CVE-2022-21449 regression)");
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEncryptionEC() {
		for (final String encryptionMethod : CryptographicUtilities.KNOWN_ASYMMETRIC_ENCRYPTION_METHODS_EC) {
			byte[] encryptedDataStreamed = null;
			try {
				final ByteArrayInputStream dataStream = new ByteArrayInputStream((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8));
				final ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
				new AsymmetricEncryptionWorker(null, dataStream, encryptedStream, getEcKeyPair().getPublic(), encryptionMethod).work();
				encryptedDataStreamed = encryptedStream.toByteArray();
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Encryption failed. Method: " + encryptionMethod + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream encryptedDataStream = new ByteArrayInputStream(encryptedDataStreamed);
				final ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
				new AsymmetricDecryptionWorker(null, encryptedDataStream, decryptedStream, getEcKeyPair().getPrivate(), encryptionMethod).work();
				assertArrayEquals((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8), decryptedStream.toByteArray());
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Decryption failed. Method: " + encryptionMethod + ":\n" + e.getMessage());
			}
		}
	}

	@Test
	public void testSignatureDSA() {
		try {
			byte[] signatureBytes = null;
			try {
				final ByteArrayInputStream dataToSignInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				signatureBytes = new AsymmetricSignatureWorker(null, dataToSignInputStream, getDsaKeyPair().getPrivate(), CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_DSA).work();
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Signature failed. Method: " + CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_DSA + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream dataToVerifyInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				final boolean result = new AsymmetricVerificationWorker(null, dataToVerifyInputStream, signatureBytes, getDsaKeyPair().getPublic(), CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_DSA).work();
				assertTrue(result);
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Verification failed. Method: " + CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_DSA + ":\n" + e.getMessage());
			}

			assertTrue(CryptographicUtilities.checkPrivateKeyFitsPublicKey(getDsaKeyPair().getPrivate(), getDsaKeyPair().getPublic()));
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEncryptionELGAMAL() {
		for (final String encryptionMethod : CryptographicUtilities.KNOWN_ASYMMETRIC_ENCRYPTION_METHODS_ELGAMAL) {
			byte[] encryptedDataStreamed = null;
			try {
				final ByteArrayInputStream dataStream = new ByteArrayInputStream((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8));
				final ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
				new AsymmetricEncryptionWorker(null, dataStream, encryptedStream, getElGamalKeyPair().getPublic(), encryptionMethod).work();
				encryptedDataStreamed = encryptedStream.toByteArray();
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Encryption failed. Method: " + encryptionMethod + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream encryptedDataStream = new ByteArrayInputStream(encryptedDataStreamed);
				final ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
				new AsymmetricDecryptionWorker(null, encryptedDataStream, decryptedStream, getElGamalKeyPair().getPrivate(), encryptionMethod).work();
				assertArrayEquals((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8), decryptedStream.toByteArray());
			} catch (final Exception e) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		}
	}

	@Test
	public void testEncryptionOTP() {
		final byte[] testClear = new byte[] { 1, 2, 3, 4, 5, -6, 0, 7, 126, 127 };
		final byte[] test2 = new byte[testClear.length];
		final byte[] test3 = new byte[testClear.length];
		CryptographicUtilities.otpWork(new byte[][] { testClear }, new byte[][] { test2, test3 });
		final byte[] testResult = new byte[testClear.length];
		CryptographicUtilities.otpWork(new byte[][] { test2, test3 }, new byte[][] { testResult });
		assertArrayEquals(testClear, testResult);
	}

	@Test
	public void testEncryptionWithoutBouncyCastle() throws Exception {
		final KeyPair keyPair = CryptographicUtilities.generateRsaKeyPair(2048);

		final PublicKey publicKey = keyPair.getPublic();
		final PrivateKey privateKey = keyPair.getPrivate();

		final Cipher cipher1 = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		cipher1.init(Cipher.ENCRYPT_MODE, publicKey);

		final byte[] encrypted = cipher1.doFinal("This is a secret message Aöü".getBytes(StandardCharsets.UTF_8));

		final Cipher cipher2 = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		cipher2.init(Cipher.DECRYPT_MODE, privateKey);

		final byte[] decrypted = cipher2.doFinal(encrypted);
		assertEquals(new String(decrypted, StandardCharsets.UTF_8), "This is a secret message Aöü");
	}

	@Test
	public void testCertificateSigning() {
		try {
			final KeyPair keyPair = CryptographicUtilities.generateRsaKeyPair(2048);

			final PKCS10CertificationRequest certificationRequest = CryptographicUtilities.generatePKCS10CertificationRequest(keyPair.getPrivate(), keyPair.getPublic(), "Test CommonName CN", "Test OrganizationalUnit OU", "Test Organization O", "Test Location L", "Test State S", "Test Country C", "Test Email EMAIL");
			final String csrDataString = CryptographicUtilities.getStringFromCertificationRequest(certificationRequest);

			assertTrue(csrDataString.startsWith("-----BEGIN CERTIFICATE REQUEST-----"));

			final KeyPair caKeyPair = CryptographicUtilities.generateRsaKeyPair(2048);

			final X509Certificate caCertificate = CryptographicUtilities.generateSelfsignedCertificate(
					caKeyPair,
					LocalDateTime.now().minusDays(1),
					LocalDateTime.now().plusYears(2),
					CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA,
					"CN=" + "Test CA",
					0);

			final String caCertificateDataString = CryptographicUtilities.getStringFromX509Certificate(caCertificate);

			final X509Certificate testCertificate = CryptographicUtilities.signPKCS10CertificateRequest(caCertificate, caKeyPair.getPrivate(), null, null, certificationRequest, -1, BigInteger.valueOf(123456), 365);
			final String testCertificateDataString = CryptographicUtilities.getStringFromX509Certificate(testCertificate);

			assertTrue(testCertificateDataString.startsWith("-----BEGIN CERTIFICATE-----"));

			final X509Certificate caCertificateReadFromString = CryptographicUtilities.getCertificatesFromString(caCertificateDataString).get(0);
			final X509Certificate testCertificateReadFromString = CryptographicUtilities.getCertificatesFromString(testCertificateDataString).get(0);

			assertTrue(CryptographicUtilities.verifyChainOfTrust(testCertificateReadFromString, caCertificateReadFromString));
			assertTrue(CryptographicUtilities.verifyChainOfTrust(testCertificateReadFromString, testCertificateReadFromString));

			assertFalse(CryptographicUtilities.verifyChainOfTrust(caCertificateReadFromString, testCertificateReadFromString));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testKeyPairStringMethodsForRSA() {
		try {
			final String rsaKeyPairString = CryptographicUtilities.getStringFromKeyPair(getRsaKeyPair(), null);
			final PrivateKey rsaPrivateKey = CryptographicUtilities.getPrivateKeyFromString(rsaKeyPairString, null);
			assertEquals(CryptographicUtilities.getOpenSshFingerPrintMd5(getRsaKeyPair().getPrivate(), null), CryptographicUtilities.getOpenSshFingerPrintMd5(rsaPrivateKey, null));

			final String pubFromPrivKeyFingerPrint = CryptographicUtilities.getOpenSshFingerPrintMd5(CryptographicUtilities.getPublicKeyFromPrivateKey(getRsaKeyPair().getPrivate()), null);
			final String pubKeyFingerPrint = CryptographicUtilities.getOpenSshFingerPrintMd5(getRsaKeyPair().getPublic(), null);
			assertEquals(pubKeyFingerPrint, pubFromPrivKeyFingerPrint);

			CryptographicUtilities.getKeyInfo(getRsaKeyPair().getPrivate());
			CryptographicUtilities.getKeyInfo(getRsaKeyPair().getPublic());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testKeyPairStringMethodsForRsaWithPassword() {
		try {
			final char[] testPassword = "äBc12@".toCharArray();

			final String rsaKeyPairString = CryptographicUtilities.getStringFromKeyPair(getRsaKeyPair(), testPassword);
			final PrivateKey rsaPrivateKey = CryptographicUtilities.getPrivateKeyFromString(rsaKeyPairString, testPassword);
			assertEquals(CryptographicUtilities.getOpenSshFingerPrintMd5(getRsaKeyPair().getPrivate(), null), CryptographicUtilities.getOpenSshFingerPrintMd5(rsaPrivateKey, null));

			final String pubFromPrivKeyFingerPrint = CryptographicUtilities.getOpenSshFingerPrintMd5(CryptographicUtilities.getPublicKeyFromPrivateKey(getRsaKeyPair().getPrivate()), null);
			final String pubKeyFingerPrint = CryptographicUtilities.getOpenSshFingerPrintMd5(getRsaKeyPair().getPublic(), null);
			assertEquals(pubKeyFingerPrint, pubFromPrivKeyFingerPrint);

			CryptographicUtilities.getKeyInfo(getRsaKeyPair().getPrivate());
			CryptographicUtilities.getKeyInfo(getRsaKeyPair().getPublic());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testKeyPairStringMethodsForRsaWithEmptyPassword() {
		try {
			final char[] testPassword = "".toCharArray();

			final String rsaKeyPairString = CryptographicUtilities.getStringFromKeyPair(getRsaKeyPair(), testPassword);
			final PrivateKey rsaPrivateKey = CryptographicUtilities.getPrivateKeyFromString(rsaKeyPairString, testPassword);
			assertEquals(CryptographicUtilities.getOpenSshFingerPrintMd5(getRsaKeyPair().getPrivate(), null), CryptographicUtilities.getOpenSshFingerPrintMd5(rsaPrivateKey, null));

			final String pubFromPrivKeyFingerPrint = CryptographicUtilities.getOpenSshFingerPrintMd5(CryptographicUtilities.getPublicKeyFromPrivateKey(getRsaKeyPair().getPrivate()), null);
			final String pubKeyFingerPrint = CryptographicUtilities.getOpenSshFingerPrintMd5(getRsaKeyPair().getPublic(), null);
			assertEquals(pubKeyFingerPrint, pubFromPrivKeyFingerPrint);

			CryptographicUtilities.getKeyInfo(getRsaKeyPair().getPrivate());
			CryptographicUtilities.getKeyInfo(getRsaKeyPair().getPublic());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testKeyPairStringMethodsECDSA() {
		try {
			CryptographicUtilities.getEllipticCurveName(getEcKeyPair().getPrivate());
			final String ecDsaKeyPairString = CryptographicUtilities.getStringFromKeyPair(getEcKeyPair(), null);
			final PrivateKey ecDsaPrivateKey = CryptographicUtilities.getPrivateKeyFromString(ecDsaKeyPairString, null);
			assertEquals(CryptographicUtilities.getOpenSshFingerPrintMd5(getEcKeyPair().getPrivate(), null), CryptographicUtilities.getOpenSshFingerPrintMd5(ecDsaPrivateKey, null));

			final PublicKey publicKeyFromPrivateKey = CryptographicUtilities.getPublicKeyFromPrivateKey(getEcKeyPair().getPrivate());
			final String pubFromPrivKeyFingerPrint = CryptographicUtilities.getOpenSshFingerPrintMd5(publicKeyFromPrivateKey, null);
			final String pubKeyFingerPrint = CryptographicUtilities.getOpenSshFingerPrintMd5(getEcKeyPair().getPublic(), null);
			assertEquals(pubKeyFingerPrint, pubFromPrivKeyFingerPrint);

			CryptographicUtilities.getKeyInfo(getEcKeyPair().getPrivate());
			CryptographicUtilities.getKeyInfo(getEcKeyPair().getPublic());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testJarSignatureWithCA() {
		final File signedTestJarFile = new File(System.getProperty("java.io.tmpdir"), "SignedSimpleTest.jar");
		try {
			final KeyPair codeSigningKeyPair = CryptographicUtilities.generateRsaKeyPair(2048);
			final PKCS10CertificationRequest certificationRequest = CryptographicUtilities.generatePKCS10CertificationRequest(codeSigningKeyPair.getPrivate(), codeSigningKeyPair.getPublic(), "Test Code Signing CN", null, null, null, null, null, null);
			final KeyPair caKeyPair = CryptographicUtilities.generateRsaKeyPair(2048);
			final X509Certificate caCertificate = CryptographicUtilities.generateSelfsignedCertificate(
					caKeyPair,
					LocalDateTime.now().minusDays(1),
					LocalDateTime.now().plusYears(2),
					CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA,
					"CN=" + "Test CA",
					0);
			final Certificate codeSigningCertificate = CryptographicUtilities.signPKCS10CertificateRequest(caCertificate, caKeyPair.getPrivate(), null, null, certificationRequest, -1, BigInteger.valueOf(123456), 365);

			final File testJarFile = new File(getClass().getClassLoader().getResource("SimpleTest.jar").getFile());

			final boolean completelySigned = CryptographicUtilities.checkJarIsCompletlySigned(testJarFile);
			assertFalse(completelySigned);

			final CertPath certPath = CryptographicUtilities.createX509CertPath(new Certificate[]{ codeSigningCertificate });

			CryptographicUtilities.createJarSignature(testJarFile, codeSigningKeyPair.getPrivate(), certPath, signedTestJarFile);

			final Set<String> signatureNames2 = CryptographicUtilities.getJarSignatureNames(signedTestJarFile);
			assertEquals(new HashSet<>(Arrays.asList(new String[]{ "Test Code Signing CN" })), signatureNames2);

			assertTrue(CryptographicUtilities.verifyJarSignature(signedTestJarFile, Arrays.asList(new Certificate[] { caCertificate })));
		} catch (final Exception e) {
			fail(e.getMessage());
		} finally {
			if (signedTestJarFile.exists()) {
				signedTestJarFile.delete();
			}
		}
	}

	@Test
	public void testJarSignatureWithoutCA() {
		final File signedTestJarFile = new File(System.getProperty("java.io.tmpdir"), "SignedSimpleTest.jar");
		final File unsignedTestJarFile = new File(System.getProperty("java.io.tmpdir"), "UnSignedSimpleTest.jar");
		try {
			final KeyPair codeSigningKeyPair = CryptographicUtilities.generateRsaKeyPair(2048);
			final String x500PrincipalString = CryptographicUtilities.generateLegacyX500PrincipalString("Test Code Signing CN", null, null, null, null, null, null);
			final X509Certificate codeSigningCertificate = CryptographicUtilities.generateSelfsignedCertificate(
					codeSigningKeyPair,
					LocalDateTime.now().minusDays(1),
					LocalDateTime.now().plusYears(2),
					CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA,
					x500PrincipalString,
					-1);

			final File testJarFile = new File(getClass().getClassLoader().getResource("SimpleTest.jar").getFile());

			final boolean completelySigned = CryptographicUtilities.checkJarIsCompletlySigned(testJarFile);
			assertFalse(completelySigned);

			final CertPath certPath = CryptographicUtilities.createX509CertPath(new Certificate[]{ codeSigningCertificate });

			CryptographicUtilities.createJarSignature(testJarFile, codeSigningKeyPair.getPrivate(), certPath, signedTestJarFile);

			final Set<String> signatureNames2 = CryptographicUtilities.getJarSignatureNames(signedTestJarFile);
			assertEquals(new HashSet<>(Arrays.asList(new String[]{ "Test Code Signing CN" })), signatureNames2);

			assertTrue(CryptographicUtilities.verifyJarSignature(signedTestJarFile, Arrays.asList(new Certificate[] { codeSigningCertificate })));

			assertFalse(CryptographicUtilities.verifyJarSignature(testJarFile, Arrays.asList(new Certificate[] { codeSigningCertificate })));

			CryptographicUtilities.removeJarSignature(signedTestJarFile, unsignedTestJarFile);

			assertFalse(CryptographicUtilities.verifyJarSignature(unsignedTestJarFile, Arrays.asList(new Certificate[] { codeSigningCertificate })));

			final boolean completelySigned2 = CryptographicUtilities.checkJarIsCompletlySigned(unsignedTestJarFile);
			assertFalse(completelySigned2);
		} catch (final Exception e) {
			fail(e.getMessage());
		} finally {
			if (signedTestJarFile.exists()) {
				signedTestJarFile.delete();
			}
			if (unsignedTestJarFile.exists()) {
				unsignedTestJarFile.delete();
			}
		}
	}

	@Test
	public void testJarSignatureInvalid() {
		final File signedTestJarFile = new File(System.getProperty("java.io.tmpdir"), "SignedSimpleTest.jar");
		try {
			final KeyPair codeSigningKeyPair = CryptographicUtilities.generateRsaKeyPair(2048);
			final String x500PrincipalString = CryptographicUtilities.generateLegacyX500PrincipalString("Test Code Signing CN", null, null, null, null, null, null);
			final X509Certificate codeSigningCertificate = CryptographicUtilities.generateSelfsignedCertificate(
					codeSigningKeyPair,
					LocalDateTime.now().minusDays(1),
					LocalDateTime.now().plusYears(2),
					CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA,
					x500PrincipalString,
					-1);

			final KeyPair otherKeyPair = CryptographicUtilities.generateRsaKeyPair(2048);
			final String otherX500PrincipalString = CryptographicUtilities.generateLegacyX500PrincipalString("Test Code Signing CN", null, null, null, null, null, null);
			final X509Certificate otherCertificate = CryptographicUtilities.generateSelfsignedCertificate(
					otherKeyPair,
					LocalDateTime.now().minusDays(1),
					LocalDateTime.now().plusYears(2),
					CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA,
					otherX500PrincipalString,
					-1);

			final File testJarFile = new File(getClass().getClassLoader().getResource("SimpleTest.jar").getFile());

			final CertPath certPath = CryptographicUtilities.createX509CertPath(new Certificate[]{ codeSigningCertificate });

			CryptographicUtilities.createJarSignature(testJarFile, codeSigningKeyPair.getPrivate(), certPath, signedTestJarFile);

			assertFalse(CryptographicUtilities.verifyJarSignature(signedTestJarFile, Arrays.asList(new Certificate[] { otherCertificate })));
		} catch (final Exception e) {
			fail(e.getMessage());
		} finally {
			if (signedTestJarFile.exists()) {
				signedTestJarFile.delete();
			}
		}
	}

	@Test
	public void testSignatureEd25519() {
		try {
			byte[] signatureBytes = null;
			try {
				final ByteArrayInputStream dataToSignInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				signatureBytes = new AsymmetricSignatureWorker(null, dataToSignInputStream, getEd25519KeyPair().getPrivate(), CryptographicUtilities.ENCRYPTION_METHOD_ED25519).work();
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Signature failed. Method: " + CryptographicUtilities.ENCRYPTION_METHOD_ED25519 + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream dataToVerifyInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				final boolean result = new AsymmetricVerificationWorker(null, dataToVerifyInputStream, signatureBytes, getEd25519KeyPair().getPublic(), CryptographicUtilities.ENCRYPTION_METHOD_ED25519).work();
				assertTrue(result);
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Verification failed. Method: " + CryptographicUtilities.ENCRYPTION_METHOD_ED25519 + ":\n" + e.getMessage());
			}

			assertTrue(CryptographicUtilities.checkPrivateKeyFitsPublicKey(getEd25519KeyPair().getPrivate(), getEd25519KeyPair().getPublic()));
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testSignatureEd448() {
		try {
			byte[] signatureBytes = null;
			try {
				final ByteArrayInputStream dataToSignInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				signatureBytes = new AsymmetricSignatureWorker(null, dataToSignInputStream, getEd448KeyPair().getPrivate(), CryptographicUtilities.ENCRYPTION_METHOD_ED448).work();
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Signature failed. Method: " + CryptographicUtilities.ENCRYPTION_METHOD_ED448 + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream dataToVerifyInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				final boolean result = new AsymmetricVerificationWorker(null, dataToVerifyInputStream, signatureBytes, getEd448KeyPair().getPublic(), CryptographicUtilities.ENCRYPTION_METHOD_ED448).work();
				assertTrue(result);
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Verification failed. Method: " + CryptographicUtilities.ENCRYPTION_METHOD_ED448 + ":\n" + e.getMessage());
			}

			assertTrue(CryptographicUtilities.checkPrivateKeyFitsPublicKey(getEd448KeyPair().getPrivate(), getEd448KeyPair().getPublic()));
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testEncryptionAESGCM() {
		final String encryptionMethod = "AES/GCM/NoPadding";
		try {
			final char[] password = Utilities.getRandomString(12).toCharArray();
			byte[] encryptedData = null;
			try {
				final ByteArrayInputStream dataInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				final ByteArrayOutputStream encryptedDataOutputStream = new ByteArrayOutputStream();
				new SymmetricEncryptionWorker(null, dataInputStream, encryptedDataOutputStream, password, encryptionMethod).work();
				encryptedData = encryptedDataOutputStream.toByteArray();
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Encryption failed. Method: " + encryptionMethod + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream encryptedDataInputStream = new ByteArrayInputStream(encryptedData);
				final ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();
				new SymmetricDecryptionWorker(null, encryptedDataInputStream, dataOutputStream, password, encryptionMethod).work();
				final byte[] decryptedData = dataOutputStream.toByteArray();
				assertArrayEquals(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8), decryptedData);
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Decryption failed. Method: " + encryptionMethod + ":\n" + e.getMessage());
			}
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Regression test for the GCMParameterSpec fix: unlike plain CBC, AES/GCM must detect
	 * any tampering of the ciphertext (including the authentication tag at the end) and
	 * refuse to decrypt instead of silently returning corrupted data.
	 */
	@Test
	public void testEncryptionAESGCM_tamperingIsDetected() {
		final String encryptionMethod = "AES/GCM/NoPadding";
		try {
			final char[] password = Utilities.getRandomString(12).toCharArray();
			final ByteArrayInputStream dataInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
			final ByteArrayOutputStream encryptedDataOutputStream = new ByteArrayOutputStream();
			new SymmetricEncryptionWorker(null, dataInputStream, encryptedDataOutputStream, password, encryptionMethod).work();
			final byte[] encryptedData = encryptedDataOutputStream.toByteArray();

			// Flip the last byte, which is part of the GCM authentication tag
			encryptedData[encryptedData.length - 1] ^= 0x01;

			try {
				final ByteArrayInputStream encryptedDataInputStream = new ByteArrayInputStream(encryptedData);
				final ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();
				new SymmetricDecryptionWorker(null, encryptedDataInputStream, dataOutputStream, password, encryptionMethod).work();
				fail("Expected decryption of tampered GCM ciphertext to fail, but it succeeded");
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Expected: tampering must be detected
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Regression test for the key-length fix: DESede/3DES needs a 192 bit key.
	 * The previously hardcoded 128 bit key length would have broken this.
	 */
	@Test
	public void testEncryptionDESede() {
		final String encryptionMethod = "DESede/CBC/PKCS5Padding";
		try {
			final char[] password = Utilities.getRandomString(12).toCharArray();
			byte[] encryptedData = null;
			try {
				final ByteArrayInputStream dataInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				final ByteArrayOutputStream encryptedDataOutputStream = new ByteArrayOutputStream();
				new SymmetricEncryptionWorker(null, dataInputStream, encryptedDataOutputStream, password, encryptionMethod).work();
				encryptedData = encryptedDataOutputStream.toByteArray();
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Encryption failed. Method: " + encryptionMethod + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream encryptedDataInputStream = new ByteArrayInputStream(encryptedData);
				final ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();
				new SymmetricDecryptionWorker(null, encryptedDataInputStream, dataOutputStream, password, encryptionMethod).work();
				final byte[] decryptedData = dataOutputStream.toByteArray();
				assertArrayEquals(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8), decryptedData);
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Decryption failed. Method: " + encryptionMethod + ":\n" + e.getMessage());
			}
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testDetermineSymmetricKeyLengthBits() {
		assertEquals(256, CryptographicUtilities.determineSymmetricKeyLengthBits("aes"));
		assertEquals(256, CryptographicUtilities.determineSymmetricKeyLengthBits("AES/GCM/NoPadding"));
		assertEquals(192, CryptographicUtilities.determineSymmetricKeyLengthBits("desede"));
		assertEquals(192, CryptographicUtilities.determineSymmetricKeyLengthBits("DESede/CBC/PKCS5Padding"));
		assertEquals(64, CryptographicUtilities.determineSymmetricKeyLengthBits("des"));
		assertEquals(128, CryptographicUtilities.determineSymmetricKeyLengthBits("blowfish"));

		try {
			CryptographicUtilities.determineSymmetricKeyLengthBits("SomeUnknownCipher");
			fail("Expected IllegalArgumentException for an unknown cipher");
		} catch (@SuppressWarnings("unused") final IllegalArgumentException e) {
			// Expected
		}

		try {
			CryptographicUtilities.determineSymmetricKeyLengthBits(null);
			fail("Expected IllegalArgumentException for a null encryption method");
		} catch (@SuppressWarnings("unused") final IllegalArgumentException e) {
			// Expected
		}
	}

	/**
	 * Regression test: ISO9796-1Padding is a signature encoding with message recovery, not an
	 * encryption padding, and must not be listed as a supported RSA encryption method anymore.
	 */
	@Test
	public void testKnownAsymmetricEncryptionMethodsRSA_doesNotContainIso9796() {
		for (final String method : CryptographicUtilities.KNOWN_ASYMMETRIC_ENCRYPTION_METHODS_RSA) {
			assertFalse(method.toLowerCase().contains("iso9796"), "ISO9796-1 must not be listed as a supported encryption method: " + method);
		}
	}

	/**
	 * Regression test for the "Blowfish\t" typo fix: the entry must match exactly, with no
	 * embedded whitespace that would make comparisons against a clean "Blowfish" fail.
	 */
	@Test
	public void testSymmetricCiphersBlowfishEntryHasNoTypo() {
		boolean found = false;
		for (final String cipher : CryptographicUtilities.SYMMETRIC_CIPHERS) {
			if ("Blowfish".equals(cipher)) {
				found = true;
			}
			assertEquals(cipher.trim(), cipher, "Cipher entry must not contain leading/trailing whitespace: \"" + cipher + "\"");
		}
		assertTrue(found, "Expected to find a clean \"Blowfish\" entry in SYMMETRIC_CIPHERS");
	}

	@Test
	public void testHybridEncryptionRoundTripRSA() {
		try {
			final String testData = TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING;
			byte[] encryptedData = null;
			try {
				final ByteArrayInputStream dataInputStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));
				final ByteArrayOutputStream encryptedDataOutputStream = new ByteArrayOutputStream();
				new HybridEncryptionWorker(null, dataInputStream, encryptedDataOutputStream, getRsaKeyPair().getPublic()).work();
				encryptedData = encryptedDataOutputStream.toByteArray();
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Hybrid encryption failed:\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream encryptedDataInputStream = new ByteArrayInputStream(encryptedData);
				final ByteArrayOutputStream decryptedDataOutputStream = new ByteArrayOutputStream();
				new HybridDecryptionWorker(null, encryptedDataInputStream, decryptedDataOutputStream, getRsaKeyPair().getPrivate()).work();
				assertArrayEquals(testData.getBytes(StandardCharsets.UTF_8), decryptedDataOutputStream.toByteArray());
			} catch (final Exception e) {
				e.printStackTrace();
				fail("Hybrid decryption failed:\n" + e.getMessage());
			}
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * The bulk data is protected by AES/GCM, so tampering with the ciphertext must be detected
	 * on decryption, exactly like the plain symmetric GCM tamper test above.
	 */
	@Test
	public void testHybridEncryption_tamperingIsDetected() {
		try {
			final ByteArrayInputStream dataInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
			final ByteArrayOutputStream encryptedDataOutputStream = new ByteArrayOutputStream();
			new HybridEncryptionWorker(null, dataInputStream, encryptedDataOutputStream, getRsaKeyPair().getPublic()).work();
			final byte[] encryptedData = encryptedDataOutputStream.toByteArray();

			// Flip the last byte, which is part of the GCM authentication tag of the bulk data
			encryptedData[encryptedData.length - 1] ^= 0x01;

			try {
				final ByteArrayInputStream encryptedDataInputStream = new ByteArrayInputStream(encryptedData);
				final ByteArrayOutputStream decryptedDataOutputStream = new ByteArrayOutputStream();
				new HybridDecryptionWorker(null, encryptedDataInputStream, decryptedDataOutputStream, getRsaKeyPair().getPrivate()).work();
				fail("Expected decryption of tampered hybrid ciphertext to fail, but it succeeded");
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Expected: tampering must be detected
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	/**
	 * Decrypting with a private key that does not match the public key the session key was
	 * wrapped with must fail during key unwrapping, not silently produce garbage output.
	 */
	@Test
	public void testHybridEncryption_wrongPrivateKeyFails() {
		try {
			final ByteArrayInputStream dataInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
			final ByteArrayOutputStream encryptedDataOutputStream = new ByteArrayOutputStream();
			new HybridEncryptionWorker(null, dataInputStream, encryptedDataOutputStream, getRsaKeyPair().getPublic()).work();
			final byte[] encryptedData = encryptedDataOutputStream.toByteArray();

			final KeyPair otherRsaKeyPair = CryptographicUtilities.generateRsaKeyPair(2048);

			try {
				final ByteArrayInputStream encryptedDataInputStream = new ByteArrayInputStream(encryptedData);
				final ByteArrayOutputStream decryptedDataOutputStream = new ByteArrayOutputStream();
				new HybridDecryptionWorker(null, encryptedDataInputStream, decryptedDataOutputStream, otherRsaKeyPair.getPrivate()).work();
				fail("Expected decryption with the wrong private key to fail, but it succeeded");
			} catch (@SuppressWarnings("unused") final Exception e) {
				// Expected: key unwrap with a non-matching private key must fail
			}
		} catch (final Exception e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}
}
