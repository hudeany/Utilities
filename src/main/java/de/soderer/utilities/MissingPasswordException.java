package de.soderer.utilities;

/**
 * Signals that a password is required but was not provided.
 */
public class MissingPasswordException extends Exception {
	private static final long serialVersionUID = -5869461272078364993L;

	/**
	 * Creates a new exception without detail message.
	 */
	public MissingPasswordException() {
		super();
	}

	/**
	 * Creates a new exception with a detail message.
	 *
	 * @param message detail message
	 */
	public MissingPasswordException(final String message) {
		super(message);
	}
}
