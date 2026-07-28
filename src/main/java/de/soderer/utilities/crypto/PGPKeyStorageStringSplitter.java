package de.soderer.utilities.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import de.soderer.utilities.IoUtilities;

public class PGPKeyStorageStringSplitter {
	private static final String PUBLIC_KEY_START = "-----BEGIN PGP PUBLIC KEY BLOCK-----";
	private static final String PUBLIC_KEY_END = "-----END PGP PUBLIC KEY BLOCK-----";
	private static final String PRIVATE_KEY_START = "-----BEGIN PGP PRIVATE KEY BLOCK-----";
	private static final String PRIVATE_KEY_END = "-----END PGP PRIVATE KEY BLOCK-----";

	private byte[] keyStorageData;
	private String publicKeyStorageString = null;
	private String privateKeyStorageString = null;

	/**
	 * @return an InputStream over the extracted public key block. If no ASCII-armored PGP
	 *         block markers were found at all in the source data, it is assumed to be a raw
	 *         (non-armored/binary) key and the original data is returned as-is. If armor
	 *         markers were found but none matched a public key block, {@code null} is returned
	 *         instead of silently handing back unrelated (e.g. private key) data.
	 */
	public InputStream getPublicKeyInputStream() {
		if (publicKeyStorageString != null) {
			return new ByteArrayInputStream(publicKeyStorageString.getBytes(StandardCharsets.UTF_8));
		} else if (privateKeyStorageString == null) {
			// Neither block type found: likely raw/binary key data, not ASCII-armored text.
			return new ByteArrayInputStream(keyStorageData);
		} else {
			// A private key block was found but no public key block: this data definitely
			// does not contain the requested public key.
			return null;
		}
	}

	/**
	 * @return an InputStream over the extracted private key block. See {@link #getPublicKeyInputStream()}
	 *         for the fallback behaviour on non-armored/binary data.
	 */
	public InputStream getPrivateKeyInputStream() {
		if (privateKeyStorageString != null) {
			return new ByteArrayInputStream(privateKeyStorageString.getBytes(StandardCharsets.UTF_8));
		} else if (publicKeyStorageString == null) {
			// Neither block type found: likely raw/binary key data, not ASCII-armored text.
			return new ByteArrayInputStream(keyStorageData);
		} else {
			// A public key block was found but no private key block: this data definitely
			// does not contain the requested private key.
			return null;
		}
	}

	public PGPKeyStorageStringSplitter(final InputStream inputStream) throws Exception {
		try {
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			IoUtilities.copy(inputStream, outputStream);
			keyStorageData = outputStream.toByteArray();
			final String keyStorageString = new String(keyStorageData, StandardCharsets.UTF_8);

			if (keyStorageString.contains(PUBLIC_KEY_START) && keyStorageString.contains(PUBLIC_KEY_END)) {
				publicKeyStorageString = keyStorageString.substring(keyStorageString.indexOf(PUBLIC_KEY_START), keyStorageString.lastIndexOf(PUBLIC_KEY_END) + PUBLIC_KEY_END.length());
			}

			if (keyStorageString.contains(PRIVATE_KEY_START) && keyStorageString.contains(PRIVATE_KEY_END)) {
				privateKeyStorageString = keyStorageString.substring(keyStorageString.indexOf(PRIVATE_KEY_START), keyStorageString.lastIndexOf(PRIVATE_KEY_END) + PRIVATE_KEY_END.length());
			}
		} finally {
			inputStream.close();
		}
	}

	/**
	 * Zeroes the retained raw key material. Call once the extracted key streams are no longer needed.
	 */
	public void clear() {
		if (keyStorageData != null) {
			Arrays.fill(keyStorageData, (byte) 0);
			keyStorageData = null;
		}
		publicKeyStorageString = null;
		privateKeyStorageString = null;
	}
}
