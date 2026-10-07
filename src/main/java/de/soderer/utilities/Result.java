package de.soderer.utilities;

/**
 * Outcome of an operation or dialog.
 */
public enum Result {
	/** The operation finished successfully */
	OK,

	/** The operation failed */
	ERROR,

	/** The operation was canceled by the user or the caller */
	CANCELED
}
