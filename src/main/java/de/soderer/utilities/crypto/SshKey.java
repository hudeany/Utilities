package de.soderer.utilities.crypto;

import java.security.KeyPair;

/**
 * Container for OpenSsh key data
 */
public class SshKey {
	public enum SshKeyFormat {
		Undefined,
		OpenSSL,
		OpenSSHv1,
		Putty2,
		Putty3
	}

	private SshKeyFormat format;
	private String comment;
	private final KeyPair keyPair;

	/**
	 * Create a SSH key with given keypair
	 */
	public SshKey(final SshKeyFormat format, final String comment, final KeyPair keyPair) throws Exception {
		this.format = format == null ? SshKeyFormat.Undefined : format;
		this.comment = comment;
		if ("RSA".equals(keyPair.getPublic().getAlgorithm())
				|| "DSA".equals(keyPair.getPublic().getAlgorithm())
				|| "EC".equals(keyPair.getPublic().getAlgorithm())
				|| "EdDSA".equals(keyPair.getPublic().getAlgorithm())) {
			this.keyPair = keyPair;
		} else {
			throw new IllegalArgumentException("Unsupported SSH cipher algorithm for SSH key (only supports RSA / DSA / EC (ECDSA) / EdDSA): " + keyPair.getPublic().getAlgorithm());
		}
	}

	public String getAlgorithm() throws Exception {
		return KeyPairUtilities.getAlgorithm(keyPair);
	}

	public int getKeyStrength() throws Exception {
		return KeyPairUtilities.getKeyStrength(keyPair);
	}

	public SshKeyFormat getFormat() {
		return format;
	}

	public void setFormat(final SshKeyFormat format) {
		this.format = format == null ? SshKeyFormat.Undefined : format;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(final String comment) {
		this.comment = comment;
	}

	public String getMd5Fingerprint() throws Exception {
		return KeyPairUtilities.getMd5Fingerprint(keyPair);
	}

	public String getSha256Fingerprint() throws Exception {
		return KeyPairUtilities.getSha256Fingerprint(keyPair);
	}

	public String getSha256FingerprintBase64() throws Exception {
		return KeyPairUtilities.getSha256FingerprintBase64(keyPair);
	}

	public String getSha384Fingerprint() throws Exception {
		return KeyPairUtilities.getSha384Fingerprint(keyPair);
	}

	public String getSha384FingerprintBase64() throws Exception {
		return KeyPairUtilities.getSha384FingerprintBase64(keyPair);
	}

	public String getSha512Fingerprint() throws Exception {
		return KeyPairUtilities.getSha512Fingerprint(keyPair);
	}

	public String getSha512FingerprintBase64() throws Exception {
		return KeyPairUtilities.getSha512FingerprintBase64(keyPair);
	}

	public KeyPair getKeyPair() throws Exception {
		return keyPair;
	}

	public String encodePublicKeyForAuthorizedKeys() throws Exception {
		return KeyPairUtilities.encodePublicKeyForAuthorizedKeys(keyPair);
	}
}
