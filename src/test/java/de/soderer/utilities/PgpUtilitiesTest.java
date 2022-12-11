package de.soderer.utilities;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.crypto.PGPUtilities;

public class PgpUtilitiesTest {
	private static PGPSecretKey pgpSecretKey1 = null;
	private static char[] password1 = Utilities.getRandomString(12).toCharArray();

	private static PGPSecretKey pgpSecretKey2 = null;
	private static char[] password2 = Utilities.getRandomString(12).toCharArray();

	private static void generatePgpSecretKeys() throws Exception {
		if (pgpSecretKey1 == null) {
			pgpSecretKey1 = PGPUtilities.generatePGPSecretKey(1024, PGPUtilities.PgpPublicEncryptionMethod.RSA_GENERAL, "Test identity1", password1, LocalDateTime.now().plusMinutes(1));
		}

		if (pgpSecretKey2 == null) {
			pgpSecretKey2 = PGPUtilities.generatePGPSecretKey(1024, PGPUtilities.PgpPublicEncryptionMethod.RSA_GENERAL, "Test identity2", password2, LocalDateTime.now().plusMinutes(1));
		}
	}

	@Test
	public void testKeySignature() {
		try {
			generatePgpSecretKeys();

			final PGPPublicKey extendedPublicKey = PGPUtilities.signPublicKey(pgpSecretKey1, password1, pgpSecretKey2.getPublicKey());
			pgpSecretKey2 = PGPSecretKey.replacePublicKey(pgpSecretKey2, extendedPublicKey);

			final List<String> identities = new ArrayList<>();
			for (final Map<String, Object> signerData : PGPUtilities.getKeySigners(pgpSecretKey2.getPublicKey())) {
				identities.add((String) signerData.get("identity"));
			}
			Assert.assertTrue(identities.contains("Test identity1"));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
