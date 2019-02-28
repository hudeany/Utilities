package de.soderer.utilities;

import java.util.Map;

/**
 * Container for PuTTY key data
 */
public class PuttyKey {
	private final Map<String, String> headers;
	private final byte[] privateKey;
	private final byte[] publicKey;

	public PuttyKey(final Map<String, String> headers, final byte[] privateKey, final byte[] publicKey) {
		this.headers = headers;
		this.privateKey = privateKey;
		this.publicKey = publicKey;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public byte[] getPrivateKey() {
		return privateKey;
	}

	public byte[] getPublicKey() {
		return publicKey;
	}

	/**
	 * Key type. Either "ssh-rsa" for RSA key, or "ssh-dss" for DSA key.
	 */
	public String getAlgorithm() {
		return headers.get("PuTTY-User-Key-File-2").trim();
	}

	public String getKeyComment() {
		return headers.get("Comment").trim();
	}
}
