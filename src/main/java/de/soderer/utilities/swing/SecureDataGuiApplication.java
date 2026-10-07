package de.soderer.utilities.swing;

import java.io.File;

import javax.swing.JFrame;

import de.soderer.utilities.Credentials;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.SecureDataStore;

/**
 * Base class of GUI applications that keep sensitive values (e.g. credentials)
 * in a password protected {@link SecureDataStore} keystore file.
 *
 * <p>
 * The keystore password is asked for via a {@link CredentialsDialog} when
 * needed (an empty password is tried first) and may be kept in memory for the
 * lifetime of the application if the user chooses so.
 * </p>
 */
public class SecureDataGuiApplication extends JFrame {
	private static final long serialVersionUID = 1869492889654406822L;

	/** Keystore file for sensitive values, null for no keystore */
	private final File keystoreFile;

	/**
	 * Keystore password kept in memory for the lifetime of the application if the
	 * user chose so, null otherwise (transient: never serialized)
	 */
	private transient char[] rememberedKeystorePassword = null;

	/**
	 * Creates an application frame without keystore.
	 *
	 * @throws Exception declared for subclasses, not thrown here
	 */
	public SecureDataGuiApplication() throws Exception {
		this(null);
	}

	/**
	 * Creates an application frame.
	 *
	 * @param keystoreFile keystore file, may be null for no keystore
	 * @throws Exception declared for subclasses, not thrown here
	 */
	public SecureDataGuiApplication(final File keystoreFile) throws Exception {
		super();

		this.keystoreFile = keystoreFile;
	}

	/**
	 * Checks whether the keystore file exists.
	 *
	 * @return true if a keystore file is configured and exists
	 */
	protected boolean keystoreExists() {
		return keystoreFile != null && keystoreFile.exists();
	}

	/**
	 * Reads a value from the keystore. Asks the user for the keystore password if
	 * needed.
	 *
	 * @param <T> type of the value
	 * @param classType class of the value
	 * @param entryName name of the entry
	 * @return value, null if the keystore does not exist, could not be opened or
	 *         does not contain the entry
	 */
	protected <T> T getKeystoreValue(final Class<T> classType, final String entryName) {
		final SecureDataStore secureDataStore = openKeystore();
		return secureDataStore != null ? secureDataStore.getEntry(classType, entryName) : null;
	}

	/**
	 * Stores a value in the keystore, creating the keystore file if it does not
	 * exist yet. Asks the user for the keystore password to save with. Nothing is
	 * stored if the existing keystore cannot be opened or the user cancels the
	 * password dialog.
	 *
	 * @param entryName name of the entry
	 * @param value value to store
	 * @throws Exception if the keystore cannot be saved
	 */
	protected void saveKeyStoreValue(final String entryName, final Object value) throws Exception {
		if (keystoreFile != null) {
			SecureDataStore secureDataStore = null;

			if (keystoreExists()) {
				secureDataStore = openKeystore();
				if (secureDataStore == null) {
					// Keystore could not be opened (e.g. password dialog canceled): a new keystore would overwrite all existing entries
					return;
				}
			} else if (keystoreFile.getAbsoluteFile().getParentFile() != null && !keystoreFile.getAbsoluteFile().getParentFile().exists()) {
				keystoreFile.getAbsoluteFile().getParentFile().mkdirs();
			}

			if (secureDataStore != null) {
				secureDataStore.addEntry(entryName, value);
				final CredentialsDialog keystoreCredentialsDialog = new CredentialsDialog(this, "Secure data", LangResources.get("enterKeystorePassword"), false, true);
				keystoreCredentialsDialog.setRememberCredentialsText(LangResources.get("keepPasswordInMemory"));
				final Credentials credentials = keystoreCredentialsDialog.open();
				if (credentials != null) {
					final char[] keystorePassword = credentials.getPassword();
					secureDataStore.save(keystoreFile, keystorePassword);
					if (keystoreCredentialsDialog.isRememberCredentials()) {
						rememberedKeystorePassword = keystorePassword;
					}
				}
			} else {
				secureDataStore = new SecureDataStore();
				secureDataStore.addEntry(entryName, value);
				final CredentialsDialog keystoreCredentialsDialog = new CredentialsDialog(this, "Secure data", LangResources.get("enterKeystorePassword"), false, true);
				keystoreCredentialsDialog.setRememberCredentialsText(LangResources.get("keepPasswordInMemory"));
				final Credentials credentials = keystoreCredentialsDialog.open();
				if (credentials != null) {
					final char[] keystorePassword = credentials.getPassword();
					secureDataStore.save(keystoreFile, keystorePassword);
					if (keystoreCredentialsDialog.isRememberCredentials()) {
						rememberedKeystorePassword = keystorePassword;
					}
				}
			}
		}
	}

	private SecureDataStore openKeystore() {
		if (keystoreFile != null && keystoreFile.exists()) {
			SecureDataStore keystore = null;
			char[] keystorePassword;

			if (rememberedKeystorePassword != null) {
				// try to use remembered keystore password
				try {
					keystorePassword = rememberedKeystorePassword;
					keystore = new SecureDataStore();
					keystore.load(keystoreFile, keystorePassword);
				} catch (@SuppressWarnings("unused") final Exception e) {
					keystorePassword = null;
					keystore = null;
					rememberedKeystorePassword = null;
				}
			} else {
				// try to use empty keystore password
				try {
					keystorePassword = "".toCharArray();
					keystore = new SecureDataStore();
					keystore.load(keystoreFile, keystorePassword);
				} catch (@SuppressWarnings("unused") final Exception e) {
					keystorePassword = null;
					keystore = null;
				}
			}

			if (keystorePassword == null && keystore == null) {
				boolean retry = true;
				while (retry) {
					final CredentialsDialog keystoreCredentialsDialog = new CredentialsDialog(this, LangResources.get("window_title"), LangResources.get("enterKeystorePassword"), false, true);
					keystoreCredentialsDialog.setRememberCredentialsText(LangResources.get("keepPasswordInMemory"));
					final Credentials credentials = keystoreCredentialsDialog.open();
					if (credentials == null) {
						retry = false;
					} else {
						keystorePassword = credentials.getPassword();

						// try to use this keystore password
						try {
							keystore = new SecureDataStore();
							keystore.load(keystoreFile, keystorePassword);

							if (keystoreCredentialsDialog.isRememberCredentials()) {
								rememberedKeystorePassword = keystorePassword;
							}

							retry = false;
						} catch (@SuppressWarnings("unused") final Exception e) {
							keystorePassword = null;
							keystore = null;
							rememberedKeystorePassword = null;
							retry = true;
						}
					}
				}
			}

			return keystore;
		} else {
			return null;
		}
	}
}
