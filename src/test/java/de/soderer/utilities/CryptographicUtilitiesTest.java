package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.crypto.Cipher;

import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.crypto.AsymmetricDecryptionWorker;
import de.soderer.utilities.crypto.AsymmetricEncryptionWorker;
import de.soderer.utilities.crypto.AsymmetricSignatureWorker;
import de.soderer.utilities.crypto.AsymmetricVerificationWorker;
import de.soderer.utilities.crypto.CryptographicUtilities;
import de.soderer.utilities.crypto.SymmetricDecryptionWorker;
import de.soderer.utilities.crypto.SymmetricEncryptionWorker;

public class CryptographicUtilitiesTest {
	private static KeyPair rsaKeyPairCache = null;
	private static KeyPair elGamalKeyPairCache = null;
	private static KeyPair ecKeyPair = null;
	private static KeyPair dsaKeyPairCache = null;
	private static KeyPair dhKeyPairCache = null;

	private static KeyPair getRsaKeyPair() throws Exception {
		if (rsaKeyPairCache == null) {
			rsaKeyPairCache = CryptographicUtilities.generateRsaKeyPair(512);
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
			dsaKeyPairCache = CryptographicUtilities.generateDsaKeyPair(512);
		}

		return dsaKeyPairCache;
	}

	private static KeyPair getDhKeyPair() throws Exception {
		if (dhKeyPairCache == null) {
			dhKeyPairCache = CryptographicUtilities.generateDhKeyPair(512);
		}

		return dhKeyPairCache;
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
				Assert.fail("Encryption failed. Method: " + CryptographicUtilities.DEFAULT_SYMMETRIC_ENCRYPTION_METHOD + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream encryptedDataInputStream = new ByteArrayInputStream(encryptedData);
				final ByteArrayOutputStream dataOutputStream = new ByteArrayOutputStream();
				new SymmetricDecryptionWorker(null, encryptedDataInputStream, dataOutputStream, password, CryptographicUtilities.DEFAULT_SYMMETRIC_ENCRYPTION_METHOD).work();
				final byte[] decryptedData = dataOutputStream.toByteArray();
				Assert.assertArrayEquals(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8), decryptedData);
			} catch (final Exception e) {
				e.printStackTrace();
				Assert.fail("Decryption failed. Method: " + CryptographicUtilities.DEFAULT_SYMMETRIC_ENCRYPTION_METHOD + ":\n" + e.getMessage());
			}
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSignatureRSA() {
		try {
			byte[] signatureBytes = null;
			try {
				final ByteArrayInputStream dataToSignInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				signatureBytes = new AsymmetricSignatureWorker(null, dataToSignInputStream, getRsaKeyPair().getPrivate(), CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA).work();
			}catch (final Exception e) {
				e.printStackTrace();
				Assert.fail("Signature failed. Method: " + CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream dataToVerifyInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				final boolean result = new AsymmetricVerificationWorker(null, dataToVerifyInputStream, signatureBytes, getRsaKeyPair().getPublic(), CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA).work();
				Assert.assertTrue(result);
			}catch (final Exception e) {
				e.printStackTrace();
				Assert.fail("Verification failed. Method: " + CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA + ":\n" + e.getMessage());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
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
				Assert.fail("Encryption failed. Method: " + encryptionMethod + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream encryptedDataStream = new ByteArrayInputStream(encryptedDataStreamed);
				final ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
				new AsymmetricDecryptionWorker(null, encryptedDataStream, decryptedStream, getRsaKeyPair().getPrivate(), encryptionMethod).work();
				Assert.assertArrayEquals((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8), decryptedStream.toByteArray());
			} catch (final Exception e) {
				e.printStackTrace();
				Assert.fail("Decryption failed. Method: " + encryptionMethod + ":\n" + e.getMessage());
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
			}catch (final Exception e) {
				e.printStackTrace();
				Assert.fail("Signature failed. Method: " + CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream dataToVerifyInputStream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
				final boolean result = new AsymmetricVerificationWorker(null, dataToVerifyInputStream, signatureBytes, getEcKeyPair().getPublic(), CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_EC).work();
				Assert.assertTrue(result);
			}catch (final Exception e) {
				e.printStackTrace();
				Assert.fail("Verification failed. Method: " + CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA + ":\n" + e.getMessage());
			}
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
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
				Assert.fail("Encryption failed. Method: " + encryptionMethod + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream encryptedDataStream = new ByteArrayInputStream(encryptedDataStreamed);
				final ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
				new AsymmetricDecryptionWorker(null, encryptedDataStream, decryptedStream, getEcKeyPair().getPrivate(), encryptionMethod).work();
				Assert.assertArrayEquals((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8), decryptedStream.toByteArray());
			} catch (final Exception e) {
				e.printStackTrace();
				Assert.fail("Decryption failed. Method: " + encryptionMethod + ":\n" + e.getMessage());
			}
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
				Assert.fail("Encryption failed. Method: " + encryptionMethod + ":\n" + e.getMessage());
			}

			try {
				final ByteArrayInputStream encryptedDataStream = new ByteArrayInputStream(encryptedDataStreamed);
				final ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
				new AsymmetricDecryptionWorker(null, encryptedDataStream, decryptedStream, getElGamalKeyPair().getPrivate(), encryptionMethod).work();
				Assert.assertArrayEquals((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8), decryptedStream.toByteArray());
			} catch (final Exception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
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
		Assert.assertArrayEquals(testClear, testResult);
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
		Assert.assertEquals(new String(decrypted, StandardCharsets.UTF_8), "This is a secret message Aöü");
	}

	@Test
	public void testCertificateSigning() {
		try {
			final KeyPair keyPair = CryptographicUtilities.generateRsaKeyPair(2048);

			final PKCS10CertificationRequest certificationRequest = CryptographicUtilities.generatePKCS10CertificationRequest(keyPair.getPrivate(), keyPair.getPublic(), "Test CommonName CN", "Test OrganizationalUnit OU", "Test Organization O", "Test Location L", "Test State S", "Test Country C", "Test Email EMAIL");
			final String csrDataString = CryptographicUtilities.getStringFromCertificationRequest(certificationRequest);

			Assert.assertTrue(csrDataString.startsWith("-----BEGIN CERTIFICATE REQUEST-----"));

			final KeyPair caKeyPair = CryptographicUtilities.generateRsaKeyPair(2048);

			final X509Certificate caCertificate = CryptographicUtilities.generateSelfsignedCertificate(
					caKeyPair,
					new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000),
					new Date(System.currentTimeMillis() + 2 * 365 * 24 * 60 * 60 * 1000),
					CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA,
					"CN=" + "Test CA",
					0);

			final String caCertificateDataString = CryptographicUtilities.getStringFromX509Certificate(caCertificate);

			final X509Certificate testCertificate = CryptographicUtilities.signPKCS10CertificateRequest(caCertificate, caKeyPair.getPrivate(), null, null, certificationRequest, -1, BigInteger.valueOf(123456), 365);
			final String testCertificateDataString = CryptographicUtilities.getStringFromX509Certificate(testCertificate);

			Assert.assertTrue(testCertificateDataString.startsWith("-----BEGIN CERTIFICATE-----"));

			final X509Certificate caCertificateReadFromString = CryptographicUtilities.getCertificatesFromString(caCertificateDataString).get(0);
			final X509Certificate testCertificateReadFromString = CryptographicUtilities.getCertificatesFromString(testCertificateDataString).get(0);

			Assert.assertTrue(CryptographicUtilities.verifyChainOfTrust(testCertificateReadFromString, caCertificateReadFromString));
			Assert.assertTrue(CryptographicUtilities.verifyChainOfTrust(testCertificateReadFromString, testCertificateReadFromString));

			Assert.assertFalse(CryptographicUtilities.verifyChainOfTrust(caCertificateReadFromString, testCertificateReadFromString));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testKeyPairStringMethodsForRSA() {
		try {
			final String rsaKeyPairString = CryptographicUtilities.getStringFromKeyPair(getRsaKeyPair());
			final PrivateKey rsaPrivateKey = CryptographicUtilities.getPrivateKeyFromString(rsaKeyPairString, null);
			Assert.assertEquals(CryptographicUtilities.getMd5FingerPrint(getRsaKeyPair().getPrivate()), CryptographicUtilities.getMd5FingerPrint(rsaPrivateKey));

			final String pubFromPrivKeyFingerPrint = CryptographicUtilities.getMd5FingerPrint(CryptographicUtilities.getPublicKeyFromPrivateKey(getRsaKeyPair().getPrivate()));
			final String pubKeyFingerPrint = CryptographicUtilities.getMd5FingerPrint(getRsaKeyPair().getPublic());
			Assert.assertEquals(pubKeyFingerPrint, pubFromPrivKeyFingerPrint);

			CryptographicUtilities.getKeyInfo(getRsaKeyPair().getPrivate());
			CryptographicUtilities.getKeyInfo(getRsaKeyPair().getPublic());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testKeyPairStringMethodsECDSA() {
		try {
			final String curveName = CryptographicUtilities.getEllipticCurveName(getEcKeyPair().getPrivate());
			final String ecDsaKeyPairString = CryptographicUtilities.getStringFromKeyPair(getEcKeyPair());
			final PrivateKey ecDsaPrivateKey = CryptographicUtilities.getPrivateKeyFromString(ecDsaKeyPairString, null);
			Assert.assertEquals(CryptographicUtilities.getMd5FingerPrint(getEcKeyPair().getPrivate()), CryptographicUtilities.getMd5FingerPrint(ecDsaPrivateKey));

			final String pubFromPrivKeyFingerPrint = CryptographicUtilities.getMd5FingerPrint(CryptographicUtilities.getPublicKeyFromPrivateKey(getEcKeyPair().getPrivate()));
			final String pubKeyFingerPrint = CryptographicUtilities.getMd5FingerPrint(getEcKeyPair().getPublic());
			Assert.assertEquals(pubKeyFingerPrint, pubFromPrivKeyFingerPrint);

			CryptographicUtilities.getKeyInfo(getEcKeyPair().getPrivate());
			CryptographicUtilities.getKeyInfo(getEcKeyPair().getPublic());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}