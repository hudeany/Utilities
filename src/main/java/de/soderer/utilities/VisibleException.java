package de.soderer.utilities;

/**
 * Exception with an additional message meant to be shown to the user, separate from the technical exception message.
 * If no visible message is given, the localized text of the language key "errorOccurred" is used.
 */
public class VisibleException extends Exception {
	private static final long serialVersionUID = -5425442516236660858L;

	/** Message to be shown to the user, defaults to the localized text of "errorOccurred" */
	private String visibleMessage = LangResources.get("errorOccurred");

	/**
	 * Creates an exception with the default visible message.
	 */
	public VisibleException() {
		super();
	}

	/**
	 * Creates an exception with the default visible message.
	 *
	 * @param exceptionMessage technical exception message
	 */
	public VisibleException(final String exceptionMessage) {
		super(exceptionMessage);
	}

	/**
	 * Creates an exception with a visible message.
	 *
	 * @param exceptionMessage technical exception message
	 * @param visibleMessage message to be shown to the user
	 */
	public VisibleException(final String exceptionMessage, final String visibleMessage) {
		super(exceptionMessage);
		this.visibleMessage = visibleMessage;
	}

	/**
	 * Creates an exception with the default visible message.
	 *
	 * @param throwable cause
	 */
	public VisibleException(final Throwable throwable) {
		super(throwable);
	}

	/**
	 * Creates an exception with the default visible message.
	 *
	 * @param exceptionMessage technical exception message
	 * @param throwable cause
	 */
	public VisibleException(final String exceptionMessage, final Throwable throwable) {
		super(exceptionMessage, throwable);
	}

	/**
	 * Creates an exception with a visible message.
	 *
	 * @param exceptionMessage technical exception message
	 * @param throwable cause
	 * @param visibleMessage message to be shown to the user
	 */
	public VisibleException(final String exceptionMessage, final Throwable throwable, final String visibleMessage) {
		super(exceptionMessage, throwable);
		this.visibleMessage = visibleMessage;
	}

	/**
	 * Creates an exception with a visible message.
	 *
	 * @param throwable cause
	 * @param visibleMessage message to be shown to the user
	 */
	public VisibleException(final Throwable throwable, final String visibleMessage) {
		super(throwable);
		this.visibleMessage = visibleMessage;
	}

	/**
	 * Message to be shown to the user.
	 *
	 * @return visible message
	 */
	public String getVisibleMessage() {
		return visibleMessage;
	}
}
