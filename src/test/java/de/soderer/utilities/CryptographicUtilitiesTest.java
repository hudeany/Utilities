package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.junit.Assert;
import org.junit.Test;

public class CryptographicUtilitiesTest {
	
	@Test
	public void testEncryptionAES() {
		ByteArrayInputStream in = null;
		ByteArrayOutputStream out = null;
		try {
			String password = Utilities.getRandomString(12);
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
			AsymmetricCipherKeyPair keyPair = CryptographicUtilities.generateRsaKeyPair(4096);
			byte[] signature = CryptographicUtilities.signData(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), CryptographicUtilities.getPrivateKeyFromKeyPair(keyPair));
			ByteArrayInputStream stream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"));
			byte[] signatureStreamed = CryptographicUtilities.signData(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), CryptographicUtilities.getPrivateKeyFromKeyPair(keyPair));
			Assert.assertArrayEquals(signature, signatureStreamed);
			boolean result = CryptographicUtilities.verifyData(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), CryptographicUtilities.getPublicKeyFromKeyPair(keyPair), signature);
			Assert.assertTrue(result);
			boolean resultStreamed = CryptographicUtilities.verifyStream(stream, CryptographicUtilities.getPublicKeyFromKeyPair(keyPair), signature);
			Assert.assertTrue(resultStreamed);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testEncryptionRSA() {
		try {
			AsymmetricCipherKeyPair keyPair = CryptographicUtilities.generateRsaKeyPair(4096);
			
			ByteArrayInputStream stream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"));
			ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.encryptAsymmetric(stream, encryptedStream, CryptographicUtilities.getPublicKeyFromKeyPair(keyPair));
			byte[] encryptedDataStreamed = encryptedStream.toByteArray();
			
			ByteArrayInputStream encryptedDataStream = new ByteArrayInputStream(encryptedDataStreamed);
			ByteArrayOutputStream decryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.decryptAsymmetric(encryptedDataStream, decryptedStream, CryptographicUtilities.getPrivateKeyFromKeyPair(keyPair));
			Assert.assertArrayEquals(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), decryptedStream.toByteArray());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
