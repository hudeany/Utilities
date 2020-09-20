package de.soderer.utilities;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public enum HashAlgorithm {
	MD5("MD5"),
	SHA_1("SHA-1"),
	SHA_256("SHA-256"),
	SHA_512("SHA-512");

	private String algorithmName;

	private HashAlgorithm(final String algorithmName) {
		this.algorithmName = algorithmName;
	}

	public String getAlgorithmName() {
		return algorithmName;
	}

	public static HashAlgorithm getAlgorithmFromName(final String algorithmName) throws Exception {
		for (final HashAlgorithm hashAlgorithm : HashAlgorithm.values()) {
			if (hashAlgorithm.getAlgorithmName().replace("-", "").equalsIgnoreCase(algorithmName.replace("-", ""))) {
				return hashAlgorithm;
			}
		}
		throw new Exception("Unknown HashAlgorithm name: " + algorithmName);
	}

	public MessageDigest getMessageDigest() throws Exception {
		try {
			return MessageDigest.getInstance(getAlgorithmName());
		} catch (final NoSuchAlgorithmException e) {
			throw new Exception("NoSuchAlgorithm: " + getAlgorithmName(), e);
		}
	}
}
