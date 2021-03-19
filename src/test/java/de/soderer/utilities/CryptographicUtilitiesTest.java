package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.crypto.CryptographicUtilities;

public class CryptographicUtilitiesTest {
	private static KeyPair rsaKeyPairCache = null;
	private static KeyPair elGamalKeyPairCache = null;

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

	@Test
	public void testEncryptionAES() {
		ByteArrayInputStream in = null;
		ByteArrayOutputStream out = null;
		try {
			final char[] password = Utilities.getRandomString(12).toCharArray();
			in = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
			out = new ByteArrayOutputStream();
			CryptographicUtilities.encryptSymmetric(in, out, password);
			final byte[] encryptedData = out.toByteArray();
			in = new ByteArrayInputStream(encryptedData);
			out = new ByteArrayOutputStream();
			CryptographicUtilities.decryptSymmetric(in, out, password);
			final byte[] decryptedData = out.toByteArray();
			Assert.assertArrayEquals(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8), decryptedData);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSignatureRSA() {
		try {
			final byte[] signature = CryptographicUtilities.signData(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8), getRsaKeyPair().getPrivate());
			final ByteArrayInputStream stream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
			final byte[] signatureStreamed = CryptographicUtilities.signData(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8), getRsaKeyPair().getPrivate());
			Assert.assertArrayEquals(signature, signatureStreamed);
			final boolean result = CryptographicUtilities.verifyData(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8), getRsaKeyPair().getPublic(), signature);
			Assert.assertTrue(result);
			final boolean resultStreamed = CryptographicUtilities.verifyStream(stream, getRsaKeyPair().getPublic(), signature);
			Assert.assertTrue(resultStreamed);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testEncryption_RSA_PKCS1() {
		try {
			final ByteArrayInputStream stream = new ByteArrayInputStream((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8));
			final ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.encryptAsymmetric(stream, encryptedStream, getRsaKeyPair().getPublic(), "RSA/PKCS1");
			final byte[] encryptedDataStreamed = encryptedStream.toByteArray();

			final ByteArrayInputStream encryptedDataStream = new ByteArrayInputStream(encryptedDataStreamed);
			final ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.decryptAsymmetric(encryptedDataStream, decryptedStream, getRsaKeyPair().getPrivate(), "RSA/PKCS1");
			Assert.assertArrayEquals((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8), decryptedStream.toByteArray());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testEncryption_RSA_OAEP() {
		try {
			final ByteArrayInputStream stream = new ByteArrayInputStream((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8));
			final ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.encryptAsymmetric(stream, encryptedStream, getRsaKeyPair().getPublic(), "RSA/OAEP");
			final byte[] encryptedDataStreamed = encryptedStream.toByteArray();

			final ByteArrayInputStream encryptedDataStream = new ByteArrayInputStream(encryptedDataStreamed);
			final ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.decryptAsymmetric(encryptedDataStream, decryptedStream, getRsaKeyPair().getPrivate(), "RSA/OAEP");
			Assert.assertArrayEquals((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8), decryptedStream.toByteArray());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testEncryption_ELGAMAL_PKCS1() {
		try {
			final ByteArrayInputStream stream = new ByteArrayInputStream((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8));
			final ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.encryptAsymmetric(stream, encryptedStream, getElGamalKeyPair().getPublic(), "ELGAMAL/PKCS1");
			final byte[] encryptedDataStreamed = encryptedStream.toByteArray();

			final ByteArrayInputStream encryptedDataStream = new ByteArrayInputStream(encryptedDataStreamed);
			final ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.decryptAsymmetric(encryptedDataStream, decryptedStream, getElGamalKeyPair().getPrivate(), "ELGAMAL/PKCS1");
			Assert.assertArrayEquals((TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING + TextUtilities.GERMAN_TEST_STRING).getBytes(StandardCharsets.UTF_8), decryptedStream.toByteArray());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
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
		final int keySize = 2048;
		final KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(keySize);
		final KeyPair keyPair = keyPairGenerator.genKeyPair();

		final PublicKey publicKey = keyPair.getPublic();
		final PrivateKey privateKey = keyPair.getPrivate();

		final Cipher cipher1 = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		cipher1.init(Cipher.ENCRYPT_MODE, publicKey);

		final byte[] encrypted = cipher1.doFinal("This is a secret message Aöü".getBytes(StandardCharsets.UTF_8));

		final Cipher cipher2 = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		cipher2.init(Cipher.DECRYPT_MODE, privateKey);

		final byte[] decrypted =  cipher2.doFinal(encrypted);
		Assert.assertEquals(new String(decrypted, StandardCharsets.UTF_8), "This is a secret message Aöü");
	}
}
