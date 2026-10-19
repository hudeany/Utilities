package de.soderer.utilities;

/**
 * Component that can ask the user for credentials (e.g. a GUI or console application).
 */
@FunctionalInterface
public interface CredentialsParent {
	/**
	 * Asks the user for credentials.
	 * <p>
	 * The caller owns the returned credentials and should clear the password after use (see
	 * {@link Credentials#clearPassword()}), so it does not stay in memory longer than needed.
	 * </p>
	 *
	 * @param text text explaining what the credentials are needed for
	 * @param acquireUsername whether a username is requested
	 * @param acquirePassword whether a password is requested; at least one of username and password should be
	 *            requested
	 * @param firstRequest true for the first request, false if a previous attempt with other credentials failed
	 * @return the entered credentials (a value not requested is null), or null if the user canceled or entered
	 *         nothing
	 * @throws Exception if the user cannot be asked
	 */
	Credentials acquireCredentials(String text, boolean acquireUsername, boolean acquirePassword, boolean firstRequest) throws Exception;
}
