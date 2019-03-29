package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.security.KeyPair;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.json.schema.JsonSchema;

public class OpenSshKeyTest {
	@Test
	public void testTestKeyFiles() throws Exception {
		for (final File sshkeyFile : new File(getClass().getClassLoader().getResource("sshkey").toURI()).listFiles()) {
			final String filename = sshkeyFile.getName();
			try {
				if (filename.contains(".pem") && filename.contains("publickey")) {
					final String md5Hash = filename.substring(filename.indexOf("(") + 1, filename.indexOf(")"));
					try (InputStream inputStream = JsonSchema.class.getClassLoader().getResourceAsStream("sshkey/" + filename)) {
						KeyPair readKeyPair;
						try (OpenSshKeyReader reader = new OpenSshKeyReader(inputStream)) {
							readKeyPair = reader.readKey(null);
						}
						final String readMd5Fingerprint = KeyPairUtilities.getMd5Fingerprint(readKeyPair);
						Assert.assertEquals(filename, md5Hash.toUpperCase(), readMd5Fingerprint.toUpperCase().replace(":", ""));
					}
				} else if (filename.contains(".pem") && filename.contains("privatekey")) {
					final String md5Hash = filename.substring(filename.indexOf("(") + 1, filename.indexOf(")"));
					try (InputStream inputStream = JsonSchema.class.getClassLoader().getResourceAsStream("sshkey/" + filename)) {
						KeyPair readKeyPair;
						try (OpenSshKeyReader reader = new OpenSshKeyReader(inputStream)) {
							readKeyPair = reader.readKey("password".toCharArray());
						}
						final String readMd5Fingerprint = KeyPairUtilities.getMd5Fingerprint(readKeyPair);
						Assert.assertEquals(filename, md5Hash.toUpperCase(), readMd5Fingerprint.toUpperCase().replace(":", ""));
					}
				}
			} catch (final Exception e) {
				e.printStackTrace();
				Assert.fail(filename + ": " + e.getMessage());
			}
		}
	}

	@Test
	public void testRsa() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createRsaKeyPair(2048);
		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (OpenSshKeyWriter writer = new OpenSshKeyWriter(byteArrayOutStream)) {
				writer.writePKCS8Format(keyPair, "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, "UTF-8").contains("----BEGIN ")) {
			Assert.fail();
		}

		KeyPair readKeyPair;
		try (OpenSshKeyReader reader = new OpenSshKeyReader(new ByteArrayInputStream(data))) {
			readKeyPair = reader.readKey("password".toCharArray());
		}
		final String readsha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(readKeyPair);
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}

	@Test
	public void testDsa() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createDsaKeyPair(1024);
		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (OpenSshKeyWriter writer = new OpenSshKeyWriter(byteArrayOutStream)) {
				writer.writePKCS8Format(keyPair, "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, "UTF-8").contains("----BEGIN ")) {
			Assert.fail();
		}

		KeyPair readKeyPair;
		try (OpenSshKeyReader reader = new OpenSshKeyReader(new ByteArrayInputStream(data))) {
			readKeyPair = reader.readKey("password".toCharArray());
		}
		final String readsha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(readKeyPair);
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}

	@Test
	public void testEc256() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("nistp256");

		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (OpenSshKeyWriter writer = new OpenSshKeyWriter(byteArrayOutStream)) {
				writer.writePKCS8Format(keyPair, "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, "UTF-8").contains("----BEGIN ")) {
			Assert.fail();
		}

		KeyPair readKeyPair;
		try (OpenSshKeyReader reader = new OpenSshKeyReader(new ByteArrayInputStream(data))) {
			readKeyPair = reader.readKey("password".toCharArray());
		}
		final String readsha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(readKeyPair);
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}

	@Test
	public void testEc384() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("nistp384");

		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (OpenSshKeyWriter writer = new OpenSshKeyWriter(byteArrayOutStream)) {
				writer.writePKCS8Format(keyPair, "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, "UTF-8").contains("----BEGIN ")) {
			Assert.fail();
		}

		KeyPair readKeyPair;
		try (OpenSshKeyReader reader = new OpenSshKeyReader(new ByteArrayInputStream(data))) {
			readKeyPair = reader.readKey("password".toCharArray());
		}
		final String readsha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(readKeyPair);
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}

	@Test
	public void testEc521() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("nistp521");

		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		final byte[] data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (OpenSshKeyWriter writer = new OpenSshKeyWriter(byteArrayOutStream)) {
				writer.writePKCS8Format(keyPair, "password".toCharArray());
			}
			byteArrayOutStream.flush();
			data = byteArrayOutStream.toByteArray();
		}

		if (!new String(data, "UTF-8").contains("----BEGIN ")) {
			Assert.fail();
		}

		KeyPair readKeyPair;
		try (OpenSshKeyReader reader = new OpenSshKeyReader(new ByteArrayInputStream(data))) {
			readKeyPair = reader.readKey("password".toCharArray());
		}
		final String readsha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(readKeyPair);
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);
	}
}
