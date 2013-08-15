package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class CryptographicUtilitiesTest {
	
	@Test
	public void testEncryptionAES() {
		try {
			String password = Utilities.getRandomString(12);
			byte[] encryptedData = CryptographicUtilities.encryptAes(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), password);
			byte[] decryptedData = CryptographicUtilities.decryptAes(encryptedData, password);
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
			
			byte[] encryptedData = CryptographicUtilities.encryptRsa(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), CryptographicUtilities.getPublicKeyFromKeyPair(keyPair));
			ByteArrayInputStream stream = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"));
			ByteArrayOutputStream encryptedStream = new ByteArrayOutputStream();
			CryptographicUtilities.encryptRsa(stream, encryptedStream, CryptographicUtilities.getPublicKeyFromKeyPair(keyPair));
			byte[] encryptedDataStreamed = encryptedStream.toByteArray();
			
			//Encrypted data of byte[] and stream must be different
			Assert.assertFalse(Arrays.areEqual(encryptedData, encryptedDataStreamed));
					
			byte[] decryptedData1 = CryptographicUtilities.decryptRsa(encryptedDataStreamed, CryptographicUtilities.getPrivateKeyFromKeyPair(keyPair));
			byte[] decryptedData2 = CryptographicUtilities.decryptRsa(encryptedData, CryptographicUtilities.getPrivateKeyFromKeyPair(keyPair));
			Assert.assertArrayEquals(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), decryptedData1);
			Assert.assertArrayEquals(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), decryptedData2);
			
			ByteArrayInputStream encryptedDataStream1 = new ByteArrayInputStream(encryptedData);
			ByteArrayInputStream encryptedDataStream2 = new ByteArrayInputStream(encryptedDataStreamed);
			ByteArrayOutputStream decryptedStream1 = new ByteArrayOutputStream();
			ByteArrayOutputStream decryptedStream2 = new ByteArrayOutputStream();
			CryptographicUtilities.decryptRsa(encryptedDataStream1, decryptedStream1, CryptographicUtilities.getPrivateKeyFromKeyPair(keyPair));
			CryptographicUtilities.decryptRsa(encryptedDataStream2, decryptedStream2, CryptographicUtilities.getPrivateKeyFromKeyPair(keyPair));
			Assert.assertArrayEquals(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), decryptedStream1.toByteArray());
			Assert.assertArrayEquals(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), decryptedStream2.toByteArray());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
