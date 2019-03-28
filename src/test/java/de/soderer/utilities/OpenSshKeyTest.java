package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.KeyPair;

import org.junit.Assert;
import org.junit.Test;

public class OpenSshKeyTest {
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
