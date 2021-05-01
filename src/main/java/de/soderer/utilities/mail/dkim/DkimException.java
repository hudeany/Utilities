package de.soderer.utilities.mail.dkim;

/**
 * A {@link RuntimeException} that is used to indicate DKIM specific
 * missbehaviors or to wrap other {@link Exception Exceptions} that were thrown
 * during the processing of DKIM specific operations.
 */
public class DkimException extends RuntimeException {
	private static final long serialVersionUID = 7900237410305457600L;

	/**
	 * Constructs a {@code DkimException} with the given message.
	 *
	 * @param message
	 *            The message.
	 */
	public DkimException(final String message) {
		super(message);
	}

	/**
	 * Constructs a {@code DkimException} with the given message and cause. The
	 * given cause is chained to this exception.
	 *
	 * @param message
	 *            The message.
	 * @param cause
	 *            The causing exception.
	 */
	public DkimException(final String message, final Exception cause) {
		super(message, cause);
	}
}
