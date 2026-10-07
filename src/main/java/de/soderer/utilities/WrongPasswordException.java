package de.soderer.utilities;

/**
 * Signals that data could not be decrypted or verified because the given password is wrong.
 */
public class WrongPasswordException extends Exception {
	private static final long serialVersionUID = -6791318805414429659L;

	/**
	 * Creates a new exception without detail message.
	 */
	public WrongPasswordException() {
		super();
	}
}
