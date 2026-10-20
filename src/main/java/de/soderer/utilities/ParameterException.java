package de.soderer.utilities;

/**
 * Signals an invalid or missing parameter, e.g. a command line parameter. Optionally names the
 * affected parameter.
 */
public class ParameterException extends Exception {
	private static final long serialVersionUID = -4435007822821471026L;

	/** Name of the affected parameter, or null if unknown */
	private final String parameter;

	/**
	 * Creates a new exception without parameter name.
	 *
	 * @param errorMessage
	 *            the error message
	 */
	public ParameterException(final String errorMessage) {
		super(errorMessage);
		parameter = null;
	}

	/**
	 * Creates a new exception for a parameter. The parameter name is appended to the message like
	 * "message (Parameter: name)", unless it is null.
	 *
	 * @param parameter
	 *            name of the affected parameter, may be null
	 * @param errorMessage
	 *            the error message
	 */
	public ParameterException(final String parameter, final String errorMessage) {
		super(parameter == null ? errorMessage : errorMessage + " (Parameter: " + parameter + ")");
		this.parameter = parameter;
	}

	/**
	 * Returns the name of the affected parameter.
	 *
	 * @return the parameter name, or null if unknown
	 */
	public String getParameter() {
		return parameter;
	}
}
