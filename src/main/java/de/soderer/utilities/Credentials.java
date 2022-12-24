package de.soderer.utilities;

public class Credentials {
	private String username = null;
	private char[] password = null;

	public Credentials() {
	}

	public Credentials(final String username, final char[] password) {
		this.username = username;
		this.password = password;
	}

	public Credentials(final char[] password) {
		this.password = password;
	}

	public Credentials(final String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public Credentials setUsername(final String username) {
		this.username = username;
		return this;
	}

	public char[] getPassword() {
		return password;
	}

	public Credentials setPassword(final char[] password) {
		this.password = password;
		return this;
	}
}
