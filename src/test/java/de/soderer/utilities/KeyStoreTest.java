package de.soderer.utilities;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.crypto.CryptographicUtilities;

public class KeyStoreTest {
	@Test
	public void testWithFixedNullPassword() throws Exception {
		final String tempJksFilePath = Files.createTempFile("TestJavaKeyStore", ".jks").toString();
		final KeyPair keyPair = CryptographicUtilities.generateRsaKeyPair(1024);
		final X509Certificate testCert = CryptographicUtilities.generateSelfsignedCertificate(
				keyPair,
				LocalDateTime.now().minusDays(1),
				LocalDateTime.now().plusYears(2),
				CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA,
				"CN=testCert",
				-1);

		final KeyStore writeKeyStore = KeyStore.getInstance("PKCS12");
		writeKeyStore.load(null);
		writeKeyStore.setCertificateEntry("testCert", testCert);
		try (FileOutputStream outputStream = new FileOutputStream(tempJksFilePath)) {
			writeKeyStore.store(outputStream, null);
		}

		Assert.assertEquals(1, getTrustStoreSize(tempJksFilePath, ""));

		// This is the problem case of unfixed trustore with empty password
		Assert.assertEquals(0, getTrustStoreSize(tempJksFilePath, null));

		Utilities.convertPkcs12TrustStoreToJKS(new File(tempJksFilePath));

		Assert.assertEquals(1, getTrustStoreSize(tempJksFilePath, ""));

		// This is the problem case of unfixed trustore with empty password
		Assert.assertEquals(1, getTrustStoreSize(tempJksFilePath, null));

		new File(tempJksFilePath).delete();
	}
	@Test
	public void testWithFixedBlankPassword() throws Exception {
		final String tempJksFilePath = Files.createTempFile("TestJavaKeyStore", ".jks").toString();
		final KeyPair keyPair = CryptographicUtilities.generateRsaKeyPair(1024);
		final X509Certificate testCert = CryptographicUtilities.generateSelfsignedCertificate(
				keyPair,
				LocalDateTime.now().minusDays(1),
				LocalDateTime.now().plusYears(2),
				CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA,
				"CN=testCert",
				-1);

		final KeyStore writeKeyStore = KeyStore.getInstance("PKCS12");
		writeKeyStore.load(null);
		writeKeyStore.setCertificateEntry("testCert", testCert);
		try (FileOutputStream outputStream = new FileOutputStream(tempJksFilePath)) {
			writeKeyStore.store(outputStream, "".toCharArray());
		}

		Assert.assertEquals(1, getTrustStoreSize(tempJksFilePath, ""));

		// This is the problem case of unfixed trustore with empty password
		Assert.assertEquals(0, getTrustStoreSize(tempJksFilePath, null));

		Utilities.convertPkcs12TrustStoreToJKS(new File(tempJksFilePath));

		Assert.assertEquals(1, getTrustStoreSize(tempJksFilePath, ""));

		// This is the problem case of unfixed trustore with empty password
		Assert.assertEquals(1, getTrustStoreSize(tempJksFilePath, null));

		new File(tempJksFilePath).delete();
	}

	@Test
	public void testWithTestPassword() throws Exception {
		final String tempJksFilePath = Files.createTempFile("TestJavaKeyStore", ".jks").toString();
		final KeyPair keyPair = CryptographicUtilities.generateRsaKeyPair(1024);
		final X509Certificate testCert = CryptographicUtilities.generateSelfsignedCertificate(
				keyPair,
				LocalDateTime.now().minusDays(1),
				LocalDateTime.now().plusYears(2),
				CryptographicUtilities.DEFAULT_SIGNATURE_METHOD_RSA,
				"CN=testCert",
				-1);

		final KeyStore writeKeyStore = KeyStore.getInstance("PKCS12");
		writeKeyStore.load(null);
		writeKeyStore.setCertificateEntry("testCert", testCert);
		try (FileOutputStream outputStream = new FileOutputStream(tempJksFilePath)) {
			writeKeyStore.store(outputStream, "test".toCharArray());
		}

		Assert.assertEquals(1, getTrustStoreSize(tempJksFilePath, "test"));
		Assert.assertEquals(0, getTrustStoreSize(tempJksFilePath, null));

		Utilities.convertPkcs12TrustStoreToJKS(new File(tempJksFilePath));

		Assert.assertEquals(1, getTrustStoreSize(tempJksFilePath, "test"));
		Assert.assertEquals(0, getTrustStoreSize(tempJksFilePath, null));

		new File(tempJksFilePath).delete();
	}

	private int getTrustStoreSize(final String tempJksFilePath, final String password) throws Exception {
		final KeyStore readKeyStore = KeyStore.getInstance("PKCS12");
		try (FileInputStream inputStream = new FileInputStream(tempJksFilePath)) {
			readKeyStore.load(inputStream, password == null ? null : password.toCharArray());
		}
		return readKeyStore.size();
	}
}
