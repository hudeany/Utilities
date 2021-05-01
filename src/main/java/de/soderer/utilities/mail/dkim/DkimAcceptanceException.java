package de.soderer.utilities.mail.dkim;

/**
 * A {@link DkimSigningException} that is used to indicate that an attempt to
 * sign a {@link DkimMessage} failed, because the {@link DkimSignerOrg}
 * configuration is incompatible with the values retrieved from the DNS.
 */
public class DkimAcceptanceException extends DkimSigningException {
	private static final long serialVersionUID = -2198405614508908240L;

	/**
	 * Constructs a {@code DkimAcceptanceException} with the given message.
	 *
	 * @param message
	 *            The message.
	 */
	public DkimAcceptanceException(final String message) {
		super(message);
	}

	/**
	 * Constructs a {@code DkimAcceptanceException} with the given message and
	 * cause. The given cause is chained to this exception.
	 *
	 * @param message
	 *            The message.
	 * @param cause
	 *            The causing exception.
	 */
	public DkimAcceptanceException(final String message, final Exception cause) {
		super(message, cause);
	}
}
