package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.security.KeyPair;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.crypto.CryptographicUtilities;
import de.soderer.utilities.crypto.OpenSshKeyWriter;
import de.soderer.utilities.crypto.PuttyKey;
import de.soderer.utilities.crypto.PuttyKeyReader;
import de.soderer.utilities.crypto.PuttyKeyWriter;

public class PuttyKeyTest {
	@Test
	public void testTestKeyFiles() throws Exception {
		for (final File sshkeyFile : new File(getClass().getClassLoader().getResource("sshkey").toURI()).listFiles()) {
			final String filename = sshkeyFile.getName();
			try {
				if (filename.contains(".ppk")) {
					final String md5Hash = filename.substring(filename.indexOf("(") + 1, filename.indexOf(")"));
					try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sshkey/" + filename)) {
						PuttyKey puttyKey;
						try (PuttyKeyReader reader = new PuttyKeyReader(inputStream)) {
							puttyKey = reader.readKey("password".toCharArray());
						}
						final String readMd5Fingerprint = puttyKey.getMd5Fingerprint();
						Assert.assertEquals(filename, md5Hash.toUpperCase(), readMd5Fingerprint.toUpperCase().replace(":", ""));
					}
				}
			} catch (final Exception e) {
				throw e;
			}
		}
	}

	@Test
	public void test() throws Exception {
		final PuttyKey puttyKey = new PuttyKey("TestKey (Ä)", KeyPairUtilities.createRsaKeyPair(2048));
		puttyKey.setComment("TestKey (Ä)");

		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (PuttyKeyWriter puttyKeyWriter = new PuttyKeyWriter(output)) {
			puttyKeyWriter.writeKey(puttyKey, "passwörd".toCharArray());
		}

		PuttyKey puttyKey2;
		try (PuttyKeyReader puttyKeyReader = new PuttyKeyReader(new ByteArrayInputStream(output.toByteArray()))) {
			puttyKey2 = puttyKeyReader.readKey("passwörd".toCharArray());
		}

		final ByteArrayOutputStream outputNoPassword = new ByteArrayOutputStream();
		try (PuttyKeyWriter puttyKeyWriter = new PuttyKeyWriter(outputNoPassword)) {
			puttyKeyWriter.writeKey(puttyKey, null);
		}

		PuttyKey puttyKey3;
		try (PuttyKeyReader puttyKeyReader = new PuttyKeyReader(new ByteArrayInputStream(outputNoPassword.toByteArray()))) {
			puttyKey3 = puttyKeyReader.readKey(null);
		}

		if (puttyKey.getMd5Fingerprint() == null || puttyKey.getMd5Fingerprint().length() == 0
				|| !puttyKey.getMd5Fingerprint().equals(puttyKey2.getMd5Fingerprint())
				|| !puttyKey.getMd5Fingerprint().equals(puttyKey3.getMd5Fingerprint())) {
			Assert.fail();
		}

		final String authKey = puttyKey2.encodePublicKeyForAuthorizedKeys();
		if (authKey == null || authKey.length() == 0) {
			Assert.fail();
		}

		final ByteArrayOutputStream output2 = new ByteArrayOutputStream();
		try (OpenSshKeyWriter writer = new OpenSshKeyWriter(output2)) {
			writer.writePKCS8Format(puttyKey2.getKeyPair(), null);
		}
		if (output2.toByteArray() == null || output2.toByteArray().length == 0) {
			Assert.fail();
		}

		final String pemString = CryptographicUtilities.writeKeyPairToProtectedPrivateOpenSshKey(puttyKey.getKeyPair(), false, null, "passwörd");
		if (pemString == null || pemString.length() == 0) {
			Assert.fail();
		}

		final String sha256Fingerprint = puttyKey.getSha256Fingerprint();
		if (sha256Fingerprint == null || sha256Fingerprint.length() == 0) {
			Assert.fail();
		}

		final ByteArrayOutputStream output3 = new ByteArrayOutputStream();
		try (OpenSshKeyWriter writer = new OpenSshKeyWriter(output3)) {
			writer.writePKCS8Format(puttyKey.getKeyPair(), "DES-EDE3-CBC", "passwörd".toCharArray());
		}
		if (output3.toByteArray() == null || output3.toByteArray().length == 0) {
			Assert.fail();
		}
	}

	@Test
	public void testRsa() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createRsaKeyPair(2048);

		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (PuttyKeyWriter writer = new PuttyKeyWriter(byteArrayOutStream)) {
				writer.writeKey(new PuttyKey("Test key", keyPair), "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, "UTF-8").contains("PuTTY-User-Key-File-2")) {
			Assert.fail();
		}

		PuttyKey readPuttyKey;
		try (PuttyKeyReader reader = new PuttyKeyReader(new ByteArrayInputStream(data))) {
			readPuttyKey = reader.readKey("password".toCharArray());
		}
		final String readsha256Fingerprint = readPuttyKey.getSha256Fingerprint();
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}

	@Test
	public void testDsa() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createDsaKeyPair();

		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (PuttyKeyWriter writer = new PuttyKeyWriter(byteArrayOutStream)) {
				writer.writeKey(new PuttyKey("Test key", keyPair), "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, "UTF-8").contains("PuTTY-User-Key-File-2")) {
			Assert.fail();
		}

		PuttyKey readPuttyKey;
		try (PuttyKeyReader reader = new PuttyKeyReader(new ByteArrayInputStream(data))) {
			readPuttyKey = reader.readKey("password".toCharArray());
		}
		final String readsha256Fingerprint = readPuttyKey.getSha256Fingerprint();
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}

	@Test
	public void testEc256() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("nistp256");

		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (PuttyKeyWriter writer = new PuttyKeyWriter(byteArrayOutStream)) {
				writer.writeKey(new PuttyKey("Test key", keyPair), "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, "UTF-8").contains("PuTTY-User-Key-File-2")) {
			Assert.fail();
		}

		PuttyKey readPuttyKey;
		try (PuttyKeyReader reader = new PuttyKeyReader(new ByteArrayInputStream(data))) {
			readPuttyKey = reader.readKey("password".toCharArray());
		}
		final String readsha256Fingerprint = readPuttyKey.getSha256Fingerprint();
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}

	@Test
	public void testEc384() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("nistp384");

		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (PuttyKeyWriter writer = new PuttyKeyWriter(byteArrayOutStream)) {
				writer.writeKey(new PuttyKey("Test key", keyPair), "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, "UTF-8").contains("PuTTY-User-Key-File-2")) {
			Assert.fail();
		}

		PuttyKey readPuttyKey;
		try (PuttyKeyReader reader = new PuttyKeyReader(new ByteArrayInputStream(data))) {
			readPuttyKey = reader.readKey("password".toCharArray());
		}
		final String readsha256Fingerprint = readPuttyKey.getSha256Fingerprint();
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}

	@Test
	public void testEc521() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("nistp521");

		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (PuttyKeyWriter writer = new PuttyKeyWriter(byteArrayOutStream)) {
				writer.writeKey(new PuttyKey("Test key", keyPair), "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, "UTF-8").contains("PuTTY-User-Key-File-2")) {
			Assert.fail();
		}

		PuttyKey readPuttyKey;
		try (PuttyKeyReader reader = new PuttyKeyReader(new ByteArrayInputStream(data))) {
			readPuttyKey = reader.readKey("password".toCharArray());
		}
		final String readsha256Fingerprint = readPuttyKey.getSha256Fingerprint();
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}
}
