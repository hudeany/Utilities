package de.soderer.utilities;

public class DoNotWrapException extends Exception {
	private static final long serialVersionUID = -5425442516236660858L;

	public DoNotWrapException() {
		super();
	}

	public DoNotWrapException(String exceptionMessage) {
		super(exceptionMessage);
	}

	public DoNotWrapException(Throwable throwable) {
		super(throwable);
	}

	public DoNotWrapException(String exceptionMessage, Throwable throwable) {
		super(exceptionMessage, throwable);
	}
}
