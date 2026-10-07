package de.soderer.utilities;

/**
 * Error caused by user input or user permissions, whose message is meant to be shown to the user.
 * If the message is a known language resource key (see {@link LangResources}), the localized text is used as message.
 */
public class UserError extends Exception {
	private static final long serialVersionUID = 3676716731550742292L;

	/**
	 * Reason of a user error
	 */
	public enum Reason {
		/** Reason not specified */
		Unknown,
		/** User is not authenticated */
		Unauthenticated,
		/** User is authenticated but not authorized */
		Unauthorized,
		/** User is not authenticated or not authorized (not distinguished for security reasons) */
		UnauthenticatedOrUnauthorized
	}

	/** Reason of this error */
	private final Reason reason;

	/**
	 * Creates a user error with reason {@link Reason#Unknown}.
	 *
	 * @param message message text or language resource key
	 */
	public UserError(final String message) {
		super(LangResources.existsKey(message) ? LangResources.get(message) : message);

		reason = Reason.Unknown;
	}

	/**
	 * Creates a user error with a reason.
	 *
	 * @param message message text or language resource key
	 * @param reason reason of the error
	 */
	public UserError(final String message, final Reason reason) {
		super(LangResources.existsKey(message) ? LangResources.get(message) : message);

		this.reason = reason;
	}

	/**
	 * Creates a user error with reason {@link Reason#Unknown} and a cause.
	 *
	 * @param message message text or language resource key
	 * @param cause cause of the error
	 */
	public UserError(final String message, final Throwable cause) {
		super(LangResources.existsKey(message) ? LangResources.get(message) : message, cause);

		reason = Reason.Unknown;
	}

	/**
	 * Reason of this error.
	 *
	 * @return reason
	 */
	public Reason getReason() {
		return reason;
	}
}
