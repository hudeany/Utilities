package de.soderer.utilities;

public class SimpleCredentialsData {
	private String userName;
	private char[] password;

	/**
	 * Simple constructor is needed for serialization/deserialization
	 */
	protected SimpleCredentialsData() {
	}

	public SimpleCredentialsData(final String userName, final char[] password) {
		this.userName = userName;
		this.password = password;
	}

	public String getUserName() {
		return userName;
	}

	public char[] getPassword() {
		return password;
	}
}
