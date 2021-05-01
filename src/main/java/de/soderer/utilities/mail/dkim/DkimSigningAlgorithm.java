package de.soderer.utilities.mail.dkim;

/**
 * Allowed signing algorithms and their notations
 */
public enum DkimSigningAlgorithm {
	SHA256_WITH_RSA("rsa-sha256", "SHA256withRSA", "sha-256"),
	SHA1_WITH_RSA("rsa-sha1", "SHA1withRSA", "sha-1");

	private final String dkimNotation;
	private final String javaNotation;
	private final String hashNotation;

	DkimSigningAlgorithm(final String dkimNotation, final String javaNotation, final String hashNotation) {
		this.dkimNotation = dkimNotation;
		this.javaNotation = javaNotation;
		this.hashNotation = hashNotation;
	}

	public String getDkimNotation() {
		return dkimNotation;
	}

	public String getJavaNotation() {
		return javaNotation;
	}

	public String getHashNotation() {
		return hashNotation;
	}
}
