package de.soderer.utilities;

public class VisibleException extends Exception {
	private static final long serialVersionUID = -5425442516236660858L;

	private String visibleMessage = LangResources.get("errorOccurred");

	public VisibleException() {
		super();
	}

	public VisibleException(final String exceptionMessage) {
		super(exceptionMessage);
	}

	public VisibleException(final String exceptionMessage, final String visibleMessage) {
		super(exceptionMessage);
		this.visibleMessage = visibleMessage;
	}

	public VisibleException(final Throwable throwable) {
		super(throwable);
	}

	public VisibleException(final String exceptionMessage, final Throwable throwable) {
		super(exceptionMessage, throwable);
	}

	public VisibleException(final String exceptionMessage, final Throwable throwable, final String visibleMessage) {
		super(exceptionMessage, throwable);
		this.visibleMessage = visibleMessage;
	}

	public VisibleException(final Throwable throwable, final String visibleMessage) {
		super(throwable);
		this.visibleMessage = visibleMessage;
	}

	public String getVisibleMessage() {
		return visibleMessage;
	}
}
