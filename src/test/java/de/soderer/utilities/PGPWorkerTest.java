package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;

import org.bouncycastle.openpgp.PGPSecretKey;
import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.crypto.PGPFileDecryptionWorker;
import de.soderer.utilities.crypto.PGPFileEncryptionWorker;
import de.soderer.utilities.crypto.PGPFileSignatureWorker;
import de.soderer.utilities.crypto.PGPFileVerificationWorker;
import de.soderer.utilities.crypto.PGPUtilities;

public class PGPWorkerTest {
	private static PGPSecretKey pgpSecretKey = null;
	private static char[] password = Utilities.getRandomString(12).toCharArray();

	private static PGPSecretKey getPgpSecretKey() throws Exception {
		if (pgpSecretKey == null) {
			pgpSecretKey = PGPUtilities.generatePGPSecretKey(1024, PGPUtilities.PgpPublicEncryptionMethod.RSA_GENERAL, "Test identity", password, new Date(new Date().getTime() + (1000 * 60 * 60)));
		}

		return pgpSecretKey;
	}

	@Test
	public void testEncryption() {
		ByteArrayInputStream in = null;
		ByteArrayOutputStream out = null;
		try {
			in = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"));
			out = new ByteArrayOutputStream();
			
			PGPFileEncryptionWorker encryptionWorker = new PGPFileEncryptionWorker(null, in, out, getPgpSecretKey().getPublicKey());
			encryptionWorker.run();
			
			byte[] encryptedData = out.toByteArray();
			
			in = new ByteArrayInputStream(encryptedData);
			out = new ByteArrayOutputStream();

			PGPFileDecryptionWorker decryptionWorker = new PGPFileDecryptionWorker(null, in, out, getPgpSecretKey(), password);
			decryptionWorker.run();
			
			byte[] decryptedData = out.toByteArray();
			Assert.assertArrayEquals(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"), decryptedData);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSignature() {
		ByteArrayInputStream in1 = null;
		ByteArrayInputStream in2 = null;
		ByteArrayInputStream signatureInputStream = null;
		try {
			// Create 2 different signatures
			in1 = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"));
			
			PGPFileSignatureWorker signatureWorker1 = new PGPFileSignatureWorker(null, in1, getPgpSecretKey(), password);
			signatureWorker1.run();
			
			byte[] signature1 = signatureWorker1.get();
			
			in2 = new ByteArrayInputStream((TextUtilities.GERMAN_TEST_STRING + "x").getBytes("UTF-8"));
			
			PGPFileSignatureWorker signatureWorker2 = new PGPFileSignatureWorker(null, in2, getPgpSecretKey(), password);
			signatureWorker2.run();
			
			// Check first valid signature
			signatureInputStream = new ByteArrayInputStream(signature1);
			in1.reset();

			PGPFileVerificationWorker verificationWorker1 = new PGPFileVerificationWorker(null, in1, getPgpSecretKey().getPublicKey(), signatureInputStream);
			verificationWorker1.run();
			
			Assert.assertTrue(verificationWorker1.get());

			// Check second invalid signature
			signatureInputStream.reset();
			in2.reset();

			PGPFileVerificationWorker verificationWorker2 = new PGPFileVerificationWorker(null, in2, getPgpSecretKey().getPublicKey(), signatureInputStream);
			verificationWorker2.run();
			
			Assert.assertFalse(verificationWorker2.get());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
