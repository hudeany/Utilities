package de.soderer.utilities.mail.dkim;

import java.io.IOException;

import javax.mail.MessagingException;

/**
 * A {@link MessagingException} that is used to indicate DKIM specific
 * missbehaviors or to wrap other {@link Exception Exceptions} that were thrown
 * during the processing of a DKIM signature, or operations necessary for DKIM
 * signatures.
 */
public class DkimSigningException extends MessagingException {
	private static final long serialVersionUID = -133638580649609093L;

	/**
	 * Constructs a {@code DkimSigningException} with the given message.
	 *
	 * @param message
	 *            The message.
	 */
	public DkimSigningException(final String message) {
		/*
		 * This is a hack: If an {@link Exception} caught in {@link IOException}
		 * or a {@link MessagingException} caused by an {@link IOException},
		 * {@link SMTPTransport} will hang forever (neither returning, nor
		 * throwing the exception)
		 */
		super(message, new IOException());
	}

	/**
	 * Constructs a {@code DkimSigningException} with the given message and
	 * cause. The given cause is chained to this exception.
	 *
	 * @param message
	 *            The message.
	 * @param cause
	 *            The causing exception.
	 */
	public DkimSigningException(final String message, final Exception cause) {
		/*
		 * This is a hack: If an {@link Exception} caught in {@link IOException}
		 * or a {@link MessagingException} caused by an {@link IOException},
		 * {@link SMTPTransport} will hang forever (neither returning, nor
		 * throwing the exception)
		 */
		super(message, new IOException(cause));
	}
}
