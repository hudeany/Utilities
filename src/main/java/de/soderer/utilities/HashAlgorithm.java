package de.soderer.utilities;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Hash algorithms supported for file and directory hashing.
 */
public enum HashAlgorithm {
	/** MD5 (128 bit, not collision resistant, only for checksums) */
	MD5("MD5"),
	/** SHA-1 (160 bit, not collision resistant, only for checksums) */
	SHA_1("SHA-1"),
	/** SHA-256 (256 bit) */
	SHA_256("SHA-256"),
	/** SHA-512 (512 bit) */
	SHA_512("SHA-512");

	private final String algorithmName;

	private HashAlgorithm(final String algorithmName) {
		this.algorithmName = algorithmName;
	}

	/**
	 * Returns the name of the algorithm as used by {@link MessageDigest#getInstance(String)}.
	 *
	 * @return the algorithm name, like "SHA-256"
	 */
	public String getAlgorithmName() {
		return algorithmName;
	}

	/**
	 * Returns the hash algorithm for a name. Case and dashes are ignored, so "sha256" and "SHA-256"
	 * are both accepted.
	 *
	 * @param algorithmName
	 *            the name of the algorithm
	 * @return the matching hash algorithm
	 * @throws Exception
	 *             if the name is null or there is no algorithm with this name
	 */
	public static HashAlgorithm getAlgorithmFromName(final String algorithmName) throws Exception {
		if (algorithmName != null) {
			for (final HashAlgorithm hashAlgorithm : HashAlgorithm.values()) {
				if (hashAlgorithm.getAlgorithmName().replace("-", "").equalsIgnoreCase(algorithmName.replace("-", ""))) {
					return hashAlgorithm;
				}
			}
		}
		throw new Exception("Unknown HashAlgorithm name: " + algorithmName);
	}

	/**
	 * Creates a new message digest for this algorithm.
	 *
	 * @return a new message digest instance
	 * @throws Exception
	 *             if the algorithm is not supported by the Java runtime
	 */
	public MessageDigest getMessageDigest() throws Exception {
		try {
			return MessageDigest.getInstance(getAlgorithmName());
		} catch (final NoSuchAlgorithmException e) {
			throw new Exception("NoSuchAlgorithm: " + getAlgorithmName(), e);
		}
	}
}
