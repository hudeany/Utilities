package de.soderer.utilities;

/**
 * Username and password. The password is kept as the given char array (not copied), so the owner may clear it after use.
 */
public class Credentials {
	private String username = null;
	private char[] password = null;

	/**
	 * Creates empty credentials.
	 */
	public Credentials() {
	}

	/**
	 * Creates credentials with username and password.
	 *
	 * @param username username, may be null
	 * @param password password, may be null; the array is stored without copying
	 */
	public Credentials(final String username, final char[] password) {
		this.username = username;
		this.password = password;
	}

	/**
	 * Creates credentials with a password only.
	 *
	 * @param password password, may be null; the array is stored without copying
	 */
	public Credentials(final char[] password) {
		this.password = password;
	}

	/**
	 * Creates credentials with a username only.
	 *
	 * @param username username, may be null
	 */
	public Credentials(final String username) {
		this.username = username;
	}

	/**
	 * Username.
	 *
	 * @return username, may be null
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Sets the username.
	 *
	 * @param username username, may be null
	 * @return this instance for chaining
	 */
	public Credentials setUsername(final String username) {
		this.username = username;
		return this;
	}

	/**
	 * Password (the stored array itself, not a copy).
	 *
	 * @return password, may be null
	 */
	public char[] getPassword() {
		return password;
	}

	/**
	 * Sets the password.
	 *
	 * @param password password, may be null; the array is stored without copying
	 * @return this instance for chaining
	 */
	public Credentials setPassword(final char[] password) {
		this.password = password;
		return this;
	}
}
