package de.soderer.utilities.swt;

import java.io.File;

import org.eclipse.swt.widgets.Display;

import de.soderer.utilities.DefaultLangResources;
import de.soderer.utilities.SecureDataStore;

public class SecureDataGuiApplication extends MainDialog {
	private final File keystoreFile;

	private char[] rememberedKeystorePassword = null;

	public char[] getRememberedKeystorePassword() {
		return rememberedKeystorePassword;
	}

	public void setRememberedKeystorePassword(final char[] rememberedKeystorePassword) {
		this.rememberedKeystorePassword = rememberedKeystorePassword;
	}

	public SecureDataGuiApplication(final Display display) {
		this(display, null);
	}

	public SecureDataGuiApplication(final Display display, final File keystoreFile) {
		super(display);

		this.keystoreFile = keystoreFile;
	}

	protected boolean keystoreExists() {
		return keystoreFile != null && keystoreFile.exists();
	}

	protected <T> T getKeystoreValue(final Class<T> classType, final String entryName) {
		final SecureDataStore secureDataStore = openKeystore();
		return secureDataStore != null ? secureDataStore.getEntry(classType, entryName) : null;
	}

	protected void saveKeyStoreValue(final String entryName, final Object value) throws Exception {
		if (keystoreFile != null) {
			SecureDataStore secureDataStore = null;

			if (keystoreExists()) {
				secureDataStore = openKeystore();
			} else if (keystoreFile != null && !keystoreFile.getParentFile().exists()) {
				keystoreFile.getParentFile().mkdirs();
			}

			if (secureDataStore != null) {
				secureDataStore.addEntry(entryName, value);
				final CredentialsDialog keystoreCredentialsDialog = new CredentialsDialog(getShell(), "Secure data", DefaultLangResources.getI18NStringOrDefault("enterKeystorePassword"), false, true);
				keystoreCredentialsDialog.setRememberCredentialsText(DefaultLangResources.getI18NStringOrDefault("keepPasswordInMemory"));
				if (keystoreCredentialsDialog.open() != null) {
					final char[] keystorePassword = keystoreCredentialsDialog.getCredentials().getPassword();
					secureDataStore.save(keystoreFile, keystorePassword);
					if (keystoreCredentialsDialog.isRememberCredentials()) {
						rememberedKeystorePassword = keystorePassword;
					}
				}
			} else {
				secureDataStore = new SecureDataStore();
				secureDataStore.addEntry(entryName, value);
				final CredentialsDialog keystoreCredentialsDialog = new CredentialsDialog(getShell(), "Secure data", DefaultLangResources.getI18NStringOrDefault("enterKeystorePassword"), false, true);
				keystoreCredentialsDialog.setRememberCredentialsText(DefaultLangResources.getI18NStringOrDefault("keepPasswordInMemory"));
				if (keystoreCredentialsDialog.open() != null) {
					final char[] keystorePassword = keystoreCredentialsDialog.getCredentials().getPassword();
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
					final CredentialsDialog keystoreCredentialsDialog = new CredentialsDialog(this, DefaultLangResources.getI18NStringOrDefault("window_title"), DefaultLangResources.getI18NStringOrDefault("enterKeystorePassword"), false, true);
					keystoreCredentialsDialog.setRememberCredentialsText(DefaultLangResources.getI18NStringOrDefault("keepPasswordInMemory"));
					if (keystoreCredentialsDialog.open() == null) {
						retry = false;
					} else {
						keystorePassword = keystoreCredentialsDialog.getCredentials().getPassword();

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
