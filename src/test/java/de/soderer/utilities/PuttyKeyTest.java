package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

public class PuttyKeyTest {
	@Test
	public void test() throws Exception {
		final PuttyKey puttyKey = new PuttyKey("TestKey (Ä)", 2048);
		puttyKey.setComment("TestKey (Ä)");

		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (PuttyKeyWriter puttyKeyWriter = new PuttyKeyWriter(output)) {
			puttyKeyWriter.writePrivatePuttyKeyFormat(puttyKey, "passwörd".toCharArray());
		}

		PuttyKey puttyKey2;
		try (PuttyKeyReader puttyKeyReader = new PuttyKeyReader(new ByteArrayInputStream(output.toByteArray()))) {
			puttyKeyReader.setPassword("passwörd".toCharArray());
			puttyKey2 = puttyKeyReader.readKey();
		}

		final ByteArrayOutputStream outputNoPassword = new ByteArrayOutputStream();
		try (PuttyKeyWriter puttyKeyWriter = new PuttyKeyWriter(outputNoPassword)) {
			puttyKeyWriter.writePrivatePuttyKeyFormat(puttyKey, null);
		}

		PuttyKey puttyKey3;
		try (PuttyKeyReader puttyKeyReader = new PuttyKeyReader(new ByteArrayInputStream(outputNoPassword.toByteArray()))) {
			puttyKey3 = puttyKeyReader.readKey();
		}

		if (puttyKey.getMd5Fingerprint() == null || puttyKey.getMd5Fingerprint().length() == 0
				|| !puttyKey.getMd5Fingerprint().equals(puttyKey2.getMd5Fingerprint())
				|| !puttyKey.getMd5Fingerprint().equals(puttyKey3.getMd5Fingerprint())) {
			throw new Exception();
		}

		final String authKey = puttyKey2.encodePublicKeyForAuthorizedKeys();
		if (authKey == null || authKey.length() == 0) {
			throw new Exception();
		}

		final ByteArrayOutputStream output2 = new ByteArrayOutputStream();
		try (PuttyKeyWriter puttyKeyWriter = new PuttyKeyWriter(output2)) {
			puttyKeyWriter.writeUnprotectedPemFormat(puttyKey2);
		}
		if (output2.toByteArray() == null || output2.toByteArray().length == 0) {
			throw new Exception();
		}

		final String pemString = CryptographicUtilities.writeKeyPairToProtectedPrivateOpenSshKey(puttyKey.getKeyPair(), false, null, "passwörd");
		if (pemString == null || pemString.length() == 0) {
			throw new Exception();
		}

		final String sha256Fingerprint = puttyKey.getSha256Fingerprint();
		if (sha256Fingerprint == null || sha256Fingerprint.length() == 0) {
			throw new Exception();
		}

		final ByteArrayOutputStream output3 = new ByteArrayOutputStream();
		try (PuttyKeyWriter puttyKeyWriter = new PuttyKeyWriter(output3)) {
			puttyKeyWriter.writeProtectedPemFormat(puttyKey, "DES-EDE3-CBC", "passwörd".toCharArray());
		}
		if (output3.toByteArray() == null || output3.toByteArray().length == 0) {
			throw new Exception();
		}
	}
}
