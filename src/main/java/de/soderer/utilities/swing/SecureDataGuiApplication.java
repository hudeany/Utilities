package de.soderer.utilities.swing;

import java.io.File;

import javax.swing.JFrame;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.SecureDataStore;

public class SecureDataGuiApplication extends JFrame {
	private static final long serialVersionUID = 1869492889654406822L;

	private final File keystoreFile;
	private final byte[] keystoreSalt;

	private char[] rememberedKeystorePassword = null;

	public SecureDataGuiApplication() throws Exception {
		this(null, null);
	}

	public SecureDataGuiApplication(final File keystoreFile, final byte[] keystoreSalt) throws Exception {
		super();

		this.keystoreFile = keystoreFile;
		this.keystoreSalt = keystoreSalt;
	}

	protected boolean keystoreExists() {
		return keystoreFile != null && keystoreFile.exists();
	}

	protected <T> T getKeystoreValue(final Class<T> classType, final String entryName) throws Exception {
		final SecureDataStore secureDataStore = openKeystore();
		return secureDataStore != null ? secureDataStore.getEntry(classType, entryName) : null;
	}

	protected void saveKeyStoreValue(final String entryName, final Object value) throws Exception {
		if (keystoreFile != null) {
			SecureDataStore secureDataStore = null;

			if (keystoreExists()) {
				secureDataStore = openKeystore();
			}

			if (secureDataStore != null) {
				secureDataStore.addEntry(entryName, value);
				final CredentialsDialog keystoreCredentialsDialog = new CredentialsDialog(this, "Secure data", LangResources.get("enterKeystorePassword"), false, true);
				keystoreCredentialsDialog.setRememberCredentialsText(LangResources.get("keepPasswordInMemory"));
				keystoreCredentialsDialog.setVisible(true);
				if (keystoreCredentialsDialog.getCredentials() != null) {
					final char[] keystorePassword = keystoreCredentialsDialog.getCredentials().getPassword();
					secureDataStore.save(keystoreFile, keystorePassword, keystoreSalt);
					if (keystoreCredentialsDialog.isRememberCredentials()) {
						rememberedKeystorePassword = keystorePassword;
					}
				}
			} else {
				secureDataStore = new SecureDataStore();
				secureDataStore.addEntry(entryName, value);
				final CredentialsDialog keystoreCredentialsDialog = new CredentialsDialog(this, "Secure data", LangResources.get("enterKeystorePassword"), false, true);
				keystoreCredentialsDialog.setRememberCredentialsText(LangResources.get("keepPasswordInMemory"));
				keystoreCredentialsDialog.setVisible(true);
				if (keystoreCredentialsDialog.getCredentials() != null) {
					final char[] keystorePassword = keystoreCredentialsDialog.getCredentials().getPassword();
					secureDataStore.save(keystoreFile, keystorePassword, keystoreSalt);
					if (keystoreCredentialsDialog.isRememberCredentials()) {
						rememberedKeystorePassword = keystorePassword;
					}
				}
			}
		}
	}

	private SecureDataStore openKeystore() throws Exception {
		if (keystoreFile != null && keystoreFile.exists()) {
			SecureDataStore keystore = null;
			char[] keystorePassword;

			if (rememberedKeystorePassword != null) {
				// try to use remembered keystore password
				try {
					keystorePassword = rememberedKeystorePassword;
					keystore = new SecureDataStore();
					keystore.load(keystoreFile, keystorePassword, keystoreSalt);
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
					keystore.load(keystoreFile, keystorePassword, keystoreSalt);
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
					keystoreCredentialsDialog.setVisible(true);
					if (keystoreCredentialsDialog.getCredentials() == null) {
						retry = false;
					} else {
						keystorePassword = keystoreCredentialsDialog.getCredentials().getPassword();

						// try to use this keystore password
						try {
							keystore = new SecureDataStore();
							keystore.load(keystoreFile, keystorePassword, keystoreSalt);

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
