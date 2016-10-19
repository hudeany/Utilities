package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.junit.Assert;
import org.junit.Test;

public class CryptographicUtilitiesTest {
	private static AsymmetricCipherKeyPair rsaKeyPairCache = null;
	private static AsymmetricCipherKeyPair elGamalKeyPairCache = null;

	private static AsymmetricCipherKeyPair getRsaSecretKey() throws Exception {
		if (rsaKeyPairCache == null) {
			rsaKeyPairCache = CryptographicUtilities.generateRsaKeyPair(512);
		}

		return rsaKeyPairCache;
	}
	
	private static AsymmetricCipherKeyPair getElGamalSecretKey() throws Exception {
		if (elGamalKeyPairCache == null) {
			elGamalKeyPairCache = CryptographicUtilities.generateElGamalKeyPair(512);
		}

		return elGamalKeyPairCache;
	}

	@Test
	public void testEncryptionAES() {
		ByteArrayInputStream in = null;
		ByteArrayOutputStream out = null;
		try {
			char[] password = Utilities.getRandomString(12).toCharArray();
			in = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"));
			out = new ByteArrayOutputStream();
			CryptographicUtilities.encryptSymmetric(in, out, password);
			byte[] encryptedData = out.toByteArray();
			in = new ByteArrayInputStream(encryptedData);
			out = new ByteArrayOutputStream();
			CryptographicUtilities.decryptSymmetric(in, out, password);
			byte[] decryptedData = out.toByteArray();
			Assert.assertArrayEquals(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), decryptedData);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSignatureRSA() {
		try {
			byte[] signature = CryptographicUtilities.signData(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), CryptographicUtilities.getPrivateKeyFromKeyPair(getRsaSecretKey()));
			ByteArrayInputStream stream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"));
			byte[] signatureStreamed = CryptographicUtilities.signData(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), CryptographicUtilities.getPrivateKeyFromKeyPair(getRsaSecretKey()));
			Assert.assertArrayEquals(signature, signatureStreamed);
			boolean result = CryptographicUtilities.verifyData(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), CryptographicUtilities.getPublicKeyFromKeyPair(getRsaSecretKey()), signature);
			Assert.assertTrue(result);
			boolean resultStreamed = CryptographicUtilities.verifyStream(stream, CryptographicUtilities.getPublicKeyFromKeyPair(getRsaSecretKey()), signature);
			Assert.assertTrue(resultStreamed);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testEncryption_RSA_PKCS1() {
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes("UTF-8"));
			ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.encryptAsymmetric(stream, encryptedStream, getRsaSecretKey().getPublic(), "RSA/PKCS1");
			byte[] encryptedDataStreamed = encryptedStream.toByteArray();

			ByteArrayInputStream encryptedDataStream = new ByteArrayInputStream(encryptedDataStreamed);
			ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.decryptAsymmetric(encryptedDataStream, decryptedStream, getRsaSecretKey().getPrivate(), "RSA/PKCS1");
			Assert.assertArrayEquals((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes("UTF-8"), decryptedStream.toByteArray());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testEncryption_RSA_OAEP() {
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes("UTF-8"));
			ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.encryptAsymmetric(stream, encryptedStream, getRsaSecretKey().getPublic(), "RSA/OAEP");
			byte[] encryptedDataStreamed = encryptedStream.toByteArray();

			ByteArrayInputStream encryptedDataStream = new ByteArrayInputStream(encryptedDataStreamed);
			ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.decryptAsymmetric(encryptedDataStream, decryptedStream, getRsaSecretKey().getPrivate(), "RSA/OAEP");
			Assert.assertArrayEquals((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes("UTF-8"), decryptedStream.toByteArray());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testEncryption_ELGAMAL_PKCS1() {
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes("UTF-8"));
			ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.encryptAsymmetric(stream, encryptedStream, getElGamalSecretKey().getPublic(), "ELGAMAL/PKCS1");
			byte[] encryptedDataStreamed = encryptedStream.toByteArray();

			ByteArrayInputStream encryptedDataStream = new ByteArrayInputStream(encryptedDataStreamed);
			ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.decryptAsymmetric(encryptedDataStream, decryptedStream, getElGamalSecretKey().getPrivate(), "ELGAMAL/PKCS1");
			Assert.assertArrayEquals((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes("UTF-8"), decryptedStream.toByteArray());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testEncryptionOTP() {
		byte[] testClear = new byte[] { 1, 2, 3, 4, 5, -6, 0, 7, 126, 127 };
		byte[] test2 = new byte[testClear.length];
		byte[] test3 = new byte[testClear.length];
		CryptographicUtilities.otpWork(new byte[][] { testClear }, new byte[][] { test2, test3 });
		byte[] testResult = new byte[testClear.length];
		CryptographicUtilities.otpWork(new byte[][] { test2, test3 }, new byte[][] { testResult });
		Assert.assertArrayEquals(testClear, testResult);
	}
}
