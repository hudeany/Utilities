package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.swt.worker.PGPFileDecryptionWorker;
import de.soderer.utilities.swt.worker.PGPFileEncryptionWorker;
import de.soderer.utilities.swt.worker.PGPFileSignatureWorker;
import de.soderer.utilities.swt.worker.PGPFileVerificationWorker;

public class CryptographicWorkerTest {
	private static PGPSecretKey pgpSecretKey = null;
	private static char[] password = null;
	
	private static PGPSecretKey getPgpSecretKey() throws Exception {
		if (pgpSecretKey == null) {
			password = PasswordGenerator.generatePassword(10);
			pgpSecretKey = CryptographicUtilities.generatePGPSecretKey(4096, "for_testing_only@example.com", password, DateUtils.getDay(1));
		}
		
		return pgpSecretKey;
	}
	
//	@Test
//	public void testPGPEncryption() {
//		try {
//			ByteArrayInputStream dataToEncrypt = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"));
//			ByteArrayOutputStream encryptedData = new ByteArrayOutputStream();
//			
//			PGPFileEncryptionWorker encryptWorker = new PGPFileEncryptionWorker(null, dataToEncrypt, encryptedData, getPgpSecretKey().getPublicKey());
//			encryptWorker.run();
//			Assert.assertTrue(encryptWorker.get());
//			String encrypted = new String(encryptedData.toByteArray(), "UTF-8");
//
//			ByteArrayInputStream dataToDecrypt = new ByteArrayInputStream(encrypted.getBytes("UTF-8"));
//			ByteArrayOutputStream decryptedData = new ByteArrayOutputStream();
//			
//			PGPFileDecryptionWorker decryptWorker = new PGPFileDecryptionWorker(null, dataToDecrypt, decryptedData, getPgpSecretKey(), password);
//			decryptWorker.run();
//			Assert.assertTrue(decryptWorker.get());
//			
//			String decrypted = new String(decryptedData.toByteArray(), "UTF-8");
//			
//			Assert.assertEquals(TextUtilities.GERMAN_TEST_STRING, decrypted);
//		} catch (Exception e) {
//			Assert.fail(e.getMessage());
//		}
//	}
//
//	@Test
//	public void testPGPSignature() {
//		FileInputStream decryptionPrivateKeyFileInputStream = null;
//		try {
//			ByteArrayInputStream dataToSign = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"));
//			
//			PGPFileSignatureWorker signatureWorker = new PGPFileSignatureWorker(null, dataToSign, getPgpSecretKey(), password);
//			signatureWorker.run();
//			byte[] signatureData = signatureWorker.get();
//			Assert.assertTrue(signatureData != null && signatureData.length > 0);
//			String signature = new String(signatureData, "UTF-8");
//
//			ByteArrayInputStream dataToVerify = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"));
//			ByteArrayInputStream signatureDataStream = new ByteArrayInputStream(signature.getBytes("UTF-8"));
//			
//			PGPFileVerificationWorker verificationWorker = new PGPFileVerificationWorker(null, dataToVerify, getPgpSecretKey().getPublicKey(), signatureDataStream);
//			verificationWorker.run();
//			Assert.assertTrue(verificationWorker.get());
//		} catch (Exception e) {
//			Assert.fail(e.getMessage());
//		} finally {
//			IOUtils.closeQuietly(decryptionPrivateKeyFileInputStream);
//		}
//	}
}
