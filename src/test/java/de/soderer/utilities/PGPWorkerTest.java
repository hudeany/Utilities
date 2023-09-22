package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import org.bouncycastle.openpgp.PGPSecretKey;
import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.crypto.PGPFileDecryptionWorker;
import de.soderer.utilities.crypto.PGPFileEncryptionWorker;
import de.soderer.utilities.crypto.PGPFileSignatureWorker;
import de.soderer.utilities.crypto.PGPFileVerificationWorker;
import de.soderer.utilities.crypto.PGPUtilities;

@SuppressWarnings("static-method")
public class PGPWorkerTest {
	private static PGPSecretKey pgpSecretKeyRSA = null;
	private static PGPSecretKey pgpSecretKeyDSA = null;
	private static PGPSecretKey pgpSecretKeyElgamal = null;
	private static char[] password = Utilities.getRandomString(12).toCharArray();

	private static PGPSecretKey getPgpSecretKeyRSA() throws Exception {
		if (pgpSecretKeyRSA == null) {
			pgpSecretKeyRSA = PGPUtilities.generatePGPSecretKey(1024, PGPUtilities.PgpPublicEncryptionMethod.RSA_GENERAL, "Test identity", password, LocalDateTime.now().plusMinutes(1));
		}

		return pgpSecretKeyRSA;
	}

	private static PGPSecretKey getPgpSecretKeyDSA() throws Exception {
		if (pgpSecretKeyDSA == null) {
			pgpSecretKeyDSA = PGPUtilities.generatePGPSecretKey(1024, PGPUtilities.PgpPublicEncryptionMethod.DSA, "Test identity", password, LocalDateTime.now().plusMinutes(1));
		}

		return pgpSecretKeyDSA;
	}

	private static PGPSecretKey getPgpSecretKeyElgamal() throws Exception {
		if (pgpSecretKeyElgamal == null) {
			pgpSecretKeyElgamal = PGPUtilities.generatePGPSecretKey(1024, PGPUtilities.PgpPublicEncryptionMethod.ELGAMAL_GENERAL, "Test identity", password, LocalDateTime.now().plusMinutes(1));
		}

		return pgpSecretKeyElgamal;
	}

	@Test
	public void testEncryptionRSA() {
		ByteArrayInputStream in = null;
		ByteArrayOutputStream out = null;
		try {
			in = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
			out = new ByteArrayOutputStream();

			final PGPFileEncryptionWorker encryptionWorker = new PGPFileEncryptionWorker(null, in, out, getPgpSecretKeyRSA().getPublicKey());
			encryptionWorker.run();

			if (encryptionWorker.getError() != null) {
				encryptionWorker.getError().printStackTrace();
				Assert.fail(encryptionWorker.getError().getMessage());
			}

			final byte[] encryptedData = out.toByteArray();

			in = new ByteArrayInputStream(encryptedData);
			out = new ByteArrayOutputStream();

			final PGPFileDecryptionWorker decryptionWorker = new PGPFileDecryptionWorker(null, in, out, getPgpSecretKeyRSA(), password);
			decryptionWorker.run();

			if (decryptionWorker.getError() != null) {
				decryptionWorker.getError().printStackTrace();
				Assert.fail(decryptionWorker.getError().getMessage());
			}

			final byte[] decryptedData = out.toByteArray();
			Assert.assertArrayEquals(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8), decryptedData);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testSignatureRSA() {
		ByteArrayInputStream in1 = null;
		ByteArrayInputStream in2 = null;
		ByteArrayInputStream signatureInputStream = null;
		try {
			// Create 2 different signatures
			in1 = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));

			final PGPFileSignatureWorker signatureWorker1 = new PGPFileSignatureWorker(null, in1, getPgpSecretKeyRSA(), password);
			signatureWorker1.run();

			if (signatureWorker1.getError() != null) {
				signatureWorker1.getError().printStackTrace();
				Assert.fail(signatureWorker1.getError().getMessage());
			}

			final byte[] signature1 = signatureWorker1.get();

			in2 = new ByteArrayInputStream((TextUtilities.GERMAN_TEST_STRING + "x").getBytes(StandardCharsets.UTF_8));

			final PGPFileSignatureWorker signatureWorker2 = new PGPFileSignatureWorker(null, in2, getPgpSecretKeyRSA(), password);
			signatureWorker2.run();

			if (signatureWorker2.getError() != null) {
				signatureWorker2.getError().printStackTrace();
				Assert.fail(signatureWorker2.getError().getMessage());
			}

			// Check first valid signature
			signatureInputStream = new ByteArrayInputStream(signature1);
			in1.reset();

			final PGPFileVerificationWorker verificationWorker1 = new PGPFileVerificationWorker(null, in1, getPgpSecretKeyRSA().getPublicKey(), signatureInputStream);
			verificationWorker1.run();

			if (verificationWorker1.getError() != null) {
				verificationWorker1.getError().printStackTrace();
				Assert.fail(verificationWorker1.getError().getMessage());
			}

			Assert.assertTrue(verificationWorker1.get());

			// Check second invalid signature
			signatureInputStream.reset();
			in2.reset();

			final PGPFileVerificationWorker verificationWorker2 = new PGPFileVerificationWorker(null, in2, getPgpSecretKeyRSA().getPublicKey(), signatureInputStream);
			verificationWorker2.run();

			if (verificationWorker2.getError() != null) {
				verificationWorker2.getError().printStackTrace();
				Assert.fail(verificationWorker2.getError().getMessage());
			}

			Assert.assertFalse(verificationWorker2.get());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}


	/**
	 * Watch out:
	 * DSA is for signatures only. Therefore a dual-key keyring with a Elgamal key for encryption should be created
	 * DSA Master key (for signing) with Elgamal subkey (for encryption)
	 */
	@Test
	public void testSignatureDSA() {
		ByteArrayInputStream in1 = null;
		ByteArrayInputStream in2 = null;
		ByteArrayInputStream signatureInputStream = null;
		try {
			// Create 2 different signatures
			in1 = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));

			final PGPFileSignatureWorker signatureWorker1 = new PGPFileSignatureWorker(null, in1, getPgpSecretKeyDSA(), password);
			signatureWorker1.run();

			if (signatureWorker1.getError() != null) {
				signatureWorker1.getError().printStackTrace();
				Assert.fail(signatureWorker1.getError().getMessage());
			}

			final byte[] signature1 = signatureWorker1.get();

			in2 = new ByteArrayInputStream((TextUtilities.GERMAN_TEST_STRING + "x").getBytes(StandardCharsets.UTF_8));

			final PGPFileSignatureWorker signatureWorker2 = new PGPFileSignatureWorker(null, in2, getPgpSecretKeyDSA(), password);
			signatureWorker2.run();

			if (signatureWorker2.getError() != null) {
				signatureWorker2.getError().printStackTrace();
				Assert.fail(signatureWorker2.getError().getMessage());
			}

			// Check first valid signature
			signatureInputStream = new ByteArrayInputStream(signature1);
			in1.reset();

			final PGPFileVerificationWorker verificationWorker1 = new PGPFileVerificationWorker(null, in1, getPgpSecretKeyDSA().getPublicKey(), signatureInputStream);
			verificationWorker1.run();

			if (verificationWorker1.getError() != null) {
				verificationWorker1.getError().printStackTrace();
				Assert.fail(verificationWorker1.getError().getMessage());
			}

			Assert.assertTrue(verificationWorker1.get());

			// Check second invalid signature
			signatureInputStream.reset();
			in2.reset();

			final PGPFileVerificationWorker verificationWorker2 = new PGPFileVerificationWorker(null, in2, getPgpSecretKeyDSA().getPublicKey(), signatureInputStream);
			verificationWorker2.run();

			if (verificationWorker2.getError() != null) {
				verificationWorker2.getError().printStackTrace();
				Assert.fail(verificationWorker2.getError().getMessage());
			}

			Assert.assertFalse(verificationWorker2.get());
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * Watch out:
	 * Elgamal signatures are not supported by PGP. Only Elgamal encryption is supported in PGP. Therefore a dual-key keyring with a DSA key for signature should be created
	 * DSA Master key (for signing) with Elgamal subkey (for encryption)
	 */
	@Test
	public void testEncryptionElgamal() {
		ByteArrayInputStream in = null;
		ByteArrayOutputStream out = null;
		try {
			in = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
			out = new ByteArrayOutputStream();

			final PGPFileEncryptionWorker encryptionWorker = new PGPFileEncryptionWorker(null, in, out, getPgpSecretKeyElgamal().getPublicKey());
			encryptionWorker.run();

			if (encryptionWorker.getError() != null) {
				encryptionWorker.getError().printStackTrace();
				Assert.fail(encryptionWorker.getError().getMessage());
			}

			final byte[] encryptedData = out.toByteArray();

			in = new ByteArrayInputStream(encryptedData);
			out = new ByteArrayOutputStream();

			final PGPFileDecryptionWorker decryptionWorker = new PGPFileDecryptionWorker(null, in, out, getPgpSecretKeyElgamal(), password);
			decryptionWorker.run();

			if (decryptionWorker.getError() != null) {
				decryptionWorker.getError().printStackTrace();
				Assert.fail(decryptionWorker.getError().getMessage());
			}

			final byte[] decryptedData = out.toByteArray();
			Assert.assertArrayEquals(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8), decryptedData);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}
}
