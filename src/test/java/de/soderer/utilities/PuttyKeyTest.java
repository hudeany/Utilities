package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Test;

public class PuttyKeyTest {
	@Test
	public void test() throws Exception {
		final PuttyKey puttyKey = new PuttyKey("TestKey", 2048);

		final ByteArrayOutputStream output = new ByteArrayOutputStream();
		try (PuttyKeyWriter puttyKeyWriter = new PuttyKeyWriter(output)) {
			puttyKeyWriter.writePuttyKeyFormat(puttyKey, "passwörd");
		}

		final PuttyKey puttyKey2;
		try (PuttyKeyReader puttyKeyReader = new PuttyKeyReader(new ByteArrayInputStream(output.toByteArray()))) {
			puttyKeyReader.setPassword("passwörd".toCharArray());
			puttyKey2 = puttyKeyReader.readKey();
		}

		final String authKey = puttyKey2.encodePublicKeyForAuthorizedKeys();
		if (authKey == null || authKey.length() == 0) {
			throw new Exception();
		}

		final String authKey2 = CryptographicUtilities.encodePublicKeyForAuthorizedKeys(puttyKey2.getKeyPair());
		if (!authKey.equals(authKey2)) {
			throw new Exception();
		}

		final ByteArrayOutputStream output2 = new ByteArrayOutputStream();
		try (PuttyKeyWriter puttyKeyWriter = new PuttyKeyWriter(output2)) {
			puttyKeyWriter.writeUnprotectedPemFormat(puttyKey2);
		}
		if (output2.toByteArray() == null || output2.toByteArray().length == 0) {
			throw new Exception();
		}

		final String pemString = CryptographicUtilities.convertPuttyKeyToProtectedPrivateOpenSshKey(puttyKey, "passwörd");
		if (pemString == null || pemString.length() == 0) {
			throw new Exception();
		}
	}
}
