package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.crypto.CryptographicUtilities;
import de.soderer.utilities.crypto.KeyPairUtilities;
import de.soderer.utilities.crypto.OpenSshKeyWriter;
import de.soderer.utilities.crypto.PuttyKeyWriter;
import de.soderer.utilities.crypto.SshKey;
import de.soderer.utilities.crypto.SshKey.SshKeyFormat;
import de.soderer.utilities.crypto.SshKeyReader;

public class SshKeyTest {
	@Test
	public void testTestKeyFiles() throws Exception {
		final ByteArrayOutputStream authorizedKeys = new ByteArrayOutputStream();

		boolean overallSuccess = true;
		for (final File sshkeyFileDirectory : new File(getClass().getClassLoader().getResource("sshkey").toURI()).listFiles()) {
			for (final File sshkeyFile : sshkeyFileDirectory.listFiles()) {
				final String filename = sshkeyFile.getName();
				try {
					if (filename.endsWith(".ppk")) {
						final String md5Hash = filename.substring(filename.indexOf("(") + 1, filename.indexOf(")"));
						try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sshkey/" + sshkeyFileDirectory.getName() + "/" + filename)) {
							final SshKey sshKey;
							if (filename.toLowerCase().contains("_no_password_")) {
								sshKey = SshKeyReader.readKey(inputStream, null);
							} else {
								sshKey = SshKeyReader.readKey(inputStream, "pÄsswOrd".toCharArray());
							}
							final String readMd5Fingerprint = sshKey.getMd5Fingerprint();
							Assert.assertEquals(filename, md5Hash.toUpperCase(), readMd5Fingerprint.toUpperCase().replace(":", ""));

							final boolean keyCheckSuccess = CryptographicUtilities.checkPrivateKeyFitsPublicKey(sshKey.getKeyPair().getPrivate(), sshKey.getKeyPair().getPublic());
							if (!keyCheckSuccess) {
								System.out.println("Signature sign/verify test failed with file: " + filename + ": " + keyCheckSuccess);
							}
							overallSuccess &= keyCheckSuccess;
						}
					} else if (filename.endsWith(".pub")) {
						final String md5Hash = filename.substring(filename.indexOf("(") + 1, filename.indexOf(")"));
						try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sshkey/" + sshkeyFileDirectory.getName() + "/" + filename)) {
							final SshKey sshKey = SshKeyReader.readKey(inputStream, null);
							final String readMd5Fingerprint = sshKey.getMd5Fingerprint();
							Assert.assertEquals(filename, md5Hash.toUpperCase(), readMd5Fingerprint.toUpperCase().replace(":", ""));
						}

						try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sshkey/" + sshkeyFileDirectory.getName() + "/" + filename)) {
							IoUtilities.copy(inputStream, authorizedKeys);
							authorizedKeys.write(("# " + filename).getBytes(StandardCharsets.UTF_8));
							authorizedKeys.write("\n\n".getBytes(StandardCharsets.UTF_8));
						}
					} else if (filename.endsWith(".pem") && filename.toLowerCase().contains("public")) {
						final String md5Hash = filename.substring(filename.indexOf("(") + 1, filename.indexOf(")"));
						try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sshkey/" + sshkeyFileDirectory.getName() + "/" + filename)) {
							final SshKey sshKey = SshKeyReader.readKey(inputStream, null);
							final String readMd5Fingerprint = sshKey.getMd5Fingerprint();
							Assert.assertEquals(filename, md5Hash.toUpperCase(), readMd5Fingerprint.toUpperCase().replace(":", ""));
						}
					} else if (filename.endsWith(".pem") && filename.toLowerCase().contains("private")) {
						final String md5Hash = filename.substring(filename.indexOf("(") + 1, filename.indexOf(")"));
						try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sshkey/" + sshkeyFileDirectory.getName() + "/" + filename)) {
							final SshKey sshKey;
							if (filename.toLowerCase().contains("_no_password_")) {
								sshKey = SshKeyReader.readKey(inputStream, null);
							} else {
								sshKey = SshKeyReader.readKey(inputStream, "pÄsswOrd".toCharArray());
							}
							final String readMd5Fingerprint = sshKey.getMd5Fingerprint();
							Assert.assertEquals(filename, md5Hash.toUpperCase(), readMd5Fingerprint.toUpperCase().replace(":", ""));

							final boolean keyCheckSuccess = CryptographicUtilities.checkPrivateKeyFitsPublicKey(sshKey.getKeyPair().getPrivate(), sshKey.getKeyPair().getPublic());
							if (!keyCheckSuccess) {
								System.out.println("Signature sign/verify test failed with file: " + filename + ": " + keyCheckSuccess);
							}
							overallSuccess &= keyCheckSuccess;
						}
					}
				} catch (final Exception e) {
					System.out.println("Signature sign/verify test failed with file: " + filename);
					e.printStackTrace();
					overallSuccess = false;
				}
			}
		}
		Assert.assertTrue("At least one keyfile test had errors", overallSuccess);

		try {
			final ByteArrayInputStream inputStream = new ByteArrayInputStream(authorizedKeys.toByteArray());
			SshKey nextKey;
			int numberOfPublicKeys = 0;
			while ((nextKey = SshKeyReader.readKey(inputStream, null)) != null) {
				// Do nothing, just read the authorizedKeys data
				numberOfPublicKeys++;
			}
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testRsa() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createRsaKeyPair(2048);
		testKeyPair(keyPair);
	}

	@Test
	public void testDsa() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createDsaKeyPair(1024);
		testKeyPair(keyPair);
	}

	@Test
	public void testEc256() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("nistp256");
		testKeyPair(keyPair);
	}

	@Test
	public void testSimpleEc256() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair(256);
		testKeyPair(keyPair);
	}

	@Test
	public void testEc384() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("nistp384");
		testKeyPair(keyPair);
	}

	@Test
	public void testSimpleEc384() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair(384);
		testKeyPair(keyPair);
	}

	@Test
	public void testEc521() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("nistp521");
		testKeyPair(keyPair);
	}

	@Test
	public void testSimpleEc521() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair(521);
		testKeyPair(keyPair);
	}

	private void testKeyPair(final KeyPair keyPair) throws Exception {
		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] privateKeyPkcs8Data;
		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (OpenSshKeyWriter writer = new OpenSshKeyWriter(byteArrayOutStream)) {
				writer.writePKCS8Format(keyPair, "password".toCharArray());
			}
			byteArrayOutStream.flush();
			privateKeyPkcs8Data = byteArrayOutStream.toByteArray();
		}

		if (!new String(privateKeyPkcs8Data, StandardCharsets.UTF_8).contains("----BEGIN ")) {
			Assert.fail();
		}

		SshKey openSshKey1;
		try (InputStream inputStream = new ByteArrayInputStream(privateKeyPkcs8Data)) {
			openSshKey1 = SshKeyReader.readKey(inputStream, "password".toCharArray());
		}
		final String readsha256Fingerprint = openSshKey1.getSha256Fingerprint();
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);

		final byte[] publicKeyPkcs1Data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (OpenSshKeyWriter writer = new OpenSshKeyWriter(byteArrayOutStream)) {
				writer.writePKCS1Format(keyPair.getPublic());
			}
			byteArrayOutStream.flush();
			publicKeyPkcs1Data = byteArrayOutStream.toByteArray();
		}

		if (!new String(publicKeyPkcs1Data, StandardCharsets.UTF_8).contains("---- BEGIN SSH2 PUBLIC KEY ----")) {
			Assert.fail();
		}
		final SshKey openSshKey2;
		try (InputStream inputStream = new ByteArrayInputStream(publicKeyPkcs1Data)) {
			openSshKey2 = SshKeyReader.readKey(inputStream, "password".toCharArray());
		}
		Assert.assertEquals(sha256Fingerprint, openSshKey2.getSha256Fingerprint());

		final byte[] keypairPkcs1Data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (OpenSshKeyWriter writer = new OpenSshKeyWriter(byteArrayOutStream)) {
				writer.writePKCS8Format(keyPair, null);
				writer.writePKCS1Format(keyPair.getPublic());
			}
			byteArrayOutStream.flush();
			keypairPkcs1Data = byteArrayOutStream.toByteArray();
		}

		if (!new String(keypairPkcs1Data, StandardCharsets.UTF_8).contains("---- BEGIN SSH2 PUBLIC KEY ----")) {
			Assert.fail();
		}
		final SshKey openSshKey3;
		try (InputStream inputStream = new ByteArrayInputStream(keypairPkcs1Data)) {
			openSshKey3 = SshKeyReader.readKey(inputStream, "password".toCharArray());
		}
		Assert.assertEquals(sha256Fingerprint, openSshKey3.getSha256Fingerprint());
	}


	@Test
	public void testPutty() throws Exception {
		final SshKey sshKey = new SshKey(SshKeyFormat.Putty2, "TestKey (Ä)", KeyPairUtilities.createRsaKeyPair(2048));

		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (PuttyKeyWriter sshKeyWriter = new PuttyKeyWriter(output)) {
			sshKeyWriter.writeKey(sshKey, "passwörd".toCharArray());
		}

		final SshKey sshKey2 = SshKeyReader.readKey(new ByteArrayInputStream(output.toByteArray()), "passwörd".toCharArray());

		final ByteArrayOutputStream outputNoPassword = new ByteArrayOutputStream();
		try (PuttyKeyWriter sshKeyWriter = new PuttyKeyWriter(outputNoPassword)) {
			sshKeyWriter.writeKey(sshKey, null);
		}

		final SshKey sshKey3 = SshKeyReader.readKey(new ByteArrayInputStream(outputNoPassword.toByteArray()), null);

		if (sshKey.getMd5Fingerprint() == null || sshKey.getMd5Fingerprint().length() == 0
				|| !sshKey.getMd5Fingerprint().equals(sshKey2.getMd5Fingerprint())
				|| !sshKey.getMd5Fingerprint().equals(sshKey3.getMd5Fingerprint())) {
			Assert.fail();
		}

		final String authKey = sshKey2.encodePublicKeyForAuthorizedKeys();
		if (authKey == null || authKey.length() == 0) {
			Assert.fail();
		}

		final ByteArrayOutputStream output2 = new ByteArrayOutputStream();
		try (OpenSshKeyWriter writer = new OpenSshKeyWriter(output2)) {
			writer.writePKCS8Format(sshKey2.getKeyPair(), null);
		}
		if (output2.toByteArray() == null || output2.toByteArray().length == 0) {
			Assert.fail();
		}

		final String sha256Fingerprint = sshKey.getSha256Fingerprint();
		if (sha256Fingerprint == null || sha256Fingerprint.length() == 0) {
			Assert.fail();
		}

		final ByteArrayOutputStream output3 = new ByteArrayOutputStream();
		try (OpenSshKeyWriter writer = new OpenSshKeyWriter(output3)) {
			writer.writePKCS8Format(sshKey.getKeyPair(), "DES-EDE3-CBC", "passwörd".toCharArray());
		}
		if (output3.toByteArray() == null || output3.toByteArray().length == 0) {
			Assert.fail();
		}
	}

	@Test
	public void testRsaPutty() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createRsaKeyPair(2048);

		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (PuttyKeyWriter writer = new PuttyKeyWriter(byteArrayOutStream)) {
				writer.writeKey(new SshKey(SshKeyFormat.Putty2, "Test key", keyPair), "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, StandardCharsets.UTF_8).contains("PuTTY-User-Key-File-2")) {
			Assert.fail();
		}

		final SshKey readSshKey = SshKeyReader.readKey(new ByteArrayInputStream(data), "password".toCharArray());
		final String readsha256Fingerprint = readSshKey.getSha256Fingerprint();
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}

	@Test
	public void testDsaPutty() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createDsaKeyPair();

		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (PuttyKeyWriter writer = new PuttyKeyWriter(byteArrayOutStream)) {
				writer.writeKey(new SshKey(SshKeyFormat.Putty2, "Test key", keyPair), "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, StandardCharsets.UTF_8).contains("PuTTY-User-Key-File-2")) {
			Assert.fail();
		}

		final SshKey readSshKey = SshKeyReader.readKey(new ByteArrayInputStream(data), "password".toCharArray());
		final String readsha256Fingerprint = readSshKey.getSha256Fingerprint();
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}

	@Test
	public void testEc256Putty() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("nistp256");

		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (PuttyKeyWriter writer = new PuttyKeyWriter(byteArrayOutStream)) {
				writer.writeKey(new SshKey(SshKeyFormat.Putty2, "Test key", keyPair), "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, StandardCharsets.UTF_8).contains("PuTTY-User-Key-File-2")) {
			Assert.fail();
		}

		final SshKey readSshKey = SshKeyReader.readKey(new ByteArrayInputStream(data), "password".toCharArray());
		final String readsha256Fingerprint = readSshKey.getSha256Fingerprint();
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}

	@Test
	public void testEc384Putty() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("nistp384");

		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (PuttyKeyWriter writer = new PuttyKeyWriter(byteArrayOutStream)) {
				writer.writeKey(new SshKey(SshKeyFormat.Putty2, "Test key", keyPair), "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, StandardCharsets.UTF_8).contains("PuTTY-User-Key-File-2")) {
			Assert.fail();
		}

		final SshKey readSshKey = SshKeyReader.readKey(new ByteArrayInputStream(data), "password".toCharArray());
		final String readsha256Fingerprint = readSshKey.getSha256Fingerprint();
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}

	@Test
	public void testEc521Putty() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("nistp521");

		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (PuttyKeyWriter writer = new PuttyKeyWriter(byteArrayOutStream)) {
				writer.writeKey(new SshKey(SshKeyFormat.Putty2, "Test key", keyPair), "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, StandardCharsets.UTF_8).contains("PuTTY-User-Key-File-2")) {
			Assert.fail();
		}

		final SshKey readSshKey = SshKeyReader.readKey(new ByteArrayInputStream(data), "password".toCharArray());
		final String readsha256Fingerprint = readSshKey.getSha256Fingerprint();
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}
}
