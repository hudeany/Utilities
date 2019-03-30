package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.security.KeyPair;

import org.junit.Assert;
import org.junit.Test;

public class OpenSshKeyTest {
	@Test
	public void testTestKeyFiles() throws Exception {
		for (final File sshkeyFile : new File(getClass().getClassLoader().getResource("sshkey").toURI()).listFiles()) {
			final String filename = sshkeyFile.getName();
			try {
				if (filename.contains(".pem") && filename.contains("publickey")) {
					final String md5Hash = filename.substring(filename.indexOf("(") + 1, filename.indexOf(")"));
					try (final InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sshkey/" + filename)) {
						KeyPair readKeyPair;
						try (OpenSshKeyReader reader = new OpenSshKeyReader(inputStream)) {
							readKeyPair = reader.readKey(null);
						}
						final String readMd5Fingerprint = KeyPairUtilities.getMd5Fingerprint(readKeyPair);
						Assert.assertEquals(filename, md5Hash.toUpperCase(), readMd5Fingerprint.toUpperCase().replace(":", ""));
					}
				} else if (filename.contains(".pem") && filename.contains("privatekey")) {
					final String md5Hash = filename.substring(filename.indexOf("(") + 1, filename.indexOf(")"));
					try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("sshkey/" + filename)) {
						KeyPair readKeyPair;
						try (OpenSshKeyReader reader = new OpenSshKeyReader(inputStream)) {
							readKeyPair = reader.readKey("password".toCharArray());
						}
						final String readMd5Fingerprint = KeyPairUtilities.getMd5Fingerprint(readKeyPair);
						Assert.assertEquals(filename, md5Hash.toUpperCase(), readMd5Fingerprint.toUpperCase().replace(":", ""));
					}
				}
			} catch (final Exception e) {
				throw e;
			}
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

		if (!new String(privateKeyPkcs8Data, "UTF-8").contains("----BEGIN ")) {
			Assert.fail();
		}

		KeyPair readKeyPair;
		try (OpenSshKeyReader reader = new OpenSshKeyReader(new ByteArrayInputStream(privateKeyPkcs8Data))) {
			readKeyPair = reader.readKey("password".toCharArray());
		}
		final String readsha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(readKeyPair);
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);

		final byte[] publicKeyPkcs1Data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (OpenSshKeyWriter writer = new OpenSshKeyWriter(byteArrayOutStream)) {
				writer.writePKCS1Format(keyPair.getPublic());
			}
			byteArrayOutStream.flush();
			publicKeyPkcs1Data = byteArrayOutStream.toByteArray();
		}

		if (!new String(publicKeyPkcs1Data, "UTF-8").contains("---- BEGIN SSH2 PUBLIC KEY ----")) {
			Assert.fail();
		}
		KeyPair readKeyPair2;
		try (OpenSshKeyReader reader = new OpenSshKeyReader(new ByteArrayInputStream(publicKeyPkcs1Data))) {
			readKeyPair2 = reader.readKey("password".toCharArray());
		}
		Assert.assertEquals(sha256Fingerprint, KeyPairUtilities.getSha256Fingerprint(readKeyPair2));

		final byte[] keypairPkcs1Data;

		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (OpenSshKeyWriter writer = new OpenSshKeyWriter(byteArrayOutStream)) {
				writer.writePKCS1Format(keyPair);
			}
			byteArrayOutStream.flush();
			keypairPkcs1Data = byteArrayOutStream.toByteArray();
		}

		if (!new String(keypairPkcs1Data, "UTF-8").contains("---- BEGIN SSH2 PUBLIC KEY ----")) {
			Assert.fail();
		}
		KeyPair readKeyPair3;
		try (OpenSshKeyReader reader = new OpenSshKeyReader(new ByteArrayInputStream(keypairPkcs1Data))) {
			readKeyPair3 = reader.readKey("password".toCharArray());
		}
		Assert.assertEquals(sha256Fingerprint, KeyPairUtilities.getSha256Fingerprint(readKeyPair3));
	}
}
