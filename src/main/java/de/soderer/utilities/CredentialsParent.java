package de.soderer.utilities;

/**
 * Component that can ask the user for credentials (e.g. a GUI or console application).
 */
public interface CredentialsParent {
	/**
	 * Asks the user for credentials.
	 *
	 * @param text text explaining what the credentials are needed for
	 * @param aquireUsername whether a username is requested
	 * @param aquirePassword whether a password is requested
	 * @param firstRequest true for the first request, false if a previous attempt with other credentials failed
	 * @return the entered credentials, or null if the user canceled or entered nothing
	 * @throws Exception if the user cannot be asked
	 */
	Credentials aquireCredentials(String text, boolean aquireUsername, boolean aquirePassword, boolean firstRequest) throws Exception;
}
