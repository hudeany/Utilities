package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Security;

import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.bc.BcPEMDecryptorProvider;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.crypto.KeyPairUtilities;
import de.soderer.utilities.crypto.OpenSshKeyReader;
import de.soderer.utilities.crypto.OpenSshKeyWriter;

public class OpenSshKeyTestWithBC {
	@Test
	public void testRsa() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createRsaKeyPair(2048);
		checkWithBc(keyPair);
	}

	@Test
	public void testDsa() throws Exception {
		final KeyPair keyPair = KeyPairUtilities.createDsaKeyPair(1024);
		checkWithBc(keyPair);
	}

	//	@Test
	//	public void testEc256() throws Exception {
	//		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("secp256");
	//		checkWithBc(keyPair);
	//	}
	//
	//	@Test
	//	public void testEc384() throws Exception {
	//		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("secp384");
	//		checkWithBc(keyPair);
	//	}
	//
	//	@Test
	//	public void testEc521() throws Exception {
	//		final KeyPair keyPair = KeyPairUtilities.createEllipticCurveKeyPair("secp521");
	//		checkWithBc(keyPair);
	//	}

	private void checkWithBc(final KeyPair keyPair) throws Exception {
		Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

		final String sha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(keyPair);

		// Create BouncyCastle encrypted PKCS8 data
		byte[] privateKeyPkcs8DataByBc;
		try (ByteArrayOutputStream bufferStream = new ByteArrayOutputStream()) {
			try (OpenSshKeyWriter writer = new OpenSshKeyWriter(bufferStream)) {
				writer.writePKCS8Format(keyPair, "password".toCharArray());
			}
			privateKeyPkcs8DataByBc = bufferStream.toByteArray();
		}

		// Check BouncyCastle encrypted PKCS8 data with BouncyCastle
		final KeyPair readKeyPair2;
		try (PEMParser pemParser = new PEMParser(new StringReader(new String(privateKeyPkcs8DataByBc, StandardCharsets.UTF_8)))) {
			final PEMEncryptedKeyPair pemEncryptedKeyPair = (PEMEncryptedKeyPair) pemParser.readObject();
			final PEMDecryptorProvider keyDecryptorProvider = new BcPEMDecryptorProvider("password".toCharArray());
			final PEMKeyPair pemKeyPair = pemEncryptedKeyPair.decryptKeyPair(keyDecryptorProvider);
			readKeyPair2 = new JcaPEMKeyConverter().getKeyPair(pemKeyPair);
		}
		final String readsha256Fingerprint2 = KeyPairUtilities.getSha256Fingerprint(readKeyPair2);
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint2);

		// Check BouncyCastle encrypted PKCS8 data with OpenSshKeyReader
		KeyPair readKeyPair;
		try (OpenSshKeyReader reader = new OpenSshKeyReader(new ByteArrayInputStream(privateKeyPkcs8DataByBc))) {
			readKeyPair = reader.readKey("password".toCharArray());
		}
		final String readsha256Fingerprint = KeyPairUtilities.getSha256Fingerprint(readKeyPair);
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint);

		// Create OpenSshKeyWriter encrypted PKCS8 data
		final byte[] privateKeyPkcs8DataByOpenSshKeyWriter;
		try (ByteArrayOutputStream byteArrayOutStream = new ByteArrayOutputStream()) {
			try (OpenSshKeyWriter writer = new OpenSshKeyWriter(byteArrayOutStream)) {
				writer.writePKCS8Format(keyPair, "DES-EDE3-CBC", "password".toCharArray());
			}
			byteArrayOutStream.flush();
			privateKeyPkcs8DataByOpenSshKeyWriter = byteArrayOutStream.toByteArray();
		}

		if (!new String(privateKeyPkcs8DataByOpenSshKeyWriter, StandardCharsets.UTF_8).contains("----BEGIN ")) {
			Assert.fail();
		}

		// Check OpenSshKeyWriter encrypted PKCS8 data with BouncyCastle
		final KeyPair readKeyPair3;
		try (PEMParser pemParser = new PEMParser(new StringReader(new String(privateKeyPkcs8DataByOpenSshKeyWriter, StandardCharsets.UTF_8)))) {
			final JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
			final PEMEncryptedKeyPair pemEncryptedKeyPair = (PEMEncryptedKeyPair) pemParser.readObject();
			final PEMDecryptorProvider keyDecryptorProvider = new BcPEMDecryptorProvider("password".toCharArray());
			final PEMKeyPair pemKeyPair = pemEncryptedKeyPair.decryptKeyPair(keyDecryptorProvider);
			readKeyPair3 = converter.getKeyPair(pemKeyPair);
		} catch (final Exception e) {
			e.printStackTrace();
			throw e;
		}
		final String readsha256Fingerprint3 = KeyPairUtilities.getSha256Fingerprint(readKeyPair3);
		Assert.assertEquals(sha256Fingerprint, readsha256Fingerprint3);
	}
}
