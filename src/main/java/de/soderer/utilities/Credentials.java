package de.soderer.utilities;

import java.util.Arrays;

/**
 * Username and password. The password is kept as the given char array (not copied), so the owner may clear it after
 * use, e.g. by {@link #clearPassword()}.
 * <p>
 * Because of the overloaded constructors, a call with a plain {@code null} argument is ambiguous and does not compile.
 * Use {@code new Credentials((String) null)}, {@code new Credentials((char[]) null)} or {@link #Credentials()}.
 * </p>
 * <p>
 * {@link #toString()} never contains the password, so instances may be logged safely.
 * </p>
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
	 */
	public void setUsername(final String username) {
		this.username = username;
	}

	/**
	 * Sets the username.
	 *
	 * @param newUsername username, may be null
	 * @return this instance for chaining
	 */
	public Credentials withUsername(final String newUsername) {
		setUsername(newUsername);
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
	 * Sets the password. A previously stored password array is not cleared, because it is owned by the caller.
	 *
	 * @param password password, may be null; the array is stored without copying
	 */
	public void setPassword(final char[] password) {
		this.password = password;
	}

	/**
	 * Sets the password.
	 *
	 * @param newPassword password, may be null; the array is stored without copying
	 * @return this instance for chaining
	 */
	public Credentials withPassword(final char[] newPassword) {
		setPassword(newPassword);
		return this;
	}

	/**
	 * Overwrites the stored password array with zero characters and removes it from these credentials. Since the array
	 * is not copied, this also clears it for every other holder of the same array.
	 */
	public void clearPassword() {
		if (password != null) {
			Arrays.fill(password, '\0');
			password = null;
		}
	}

	/**
	 * Returns a text representation for logging, without the password.
	 *
	 * @return text with the username and whether a password is set
	 */
	@Override
	public String toString() {
		return "Credentials[username=" + username + ", password=" + (password == null ? "<none>" : "<hidden>") + "]";
	}
}
