package de.soderer.utilities.swt;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.CryptographicUtilities;
import de.soderer.utilities.FileDownloadWorker;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.SecureDataStore;
import de.soderer.utilities.SimpleCredentialsData;
import de.soderer.utilities.SystemUtilities;
import de.soderer.utilities.UserError;
import de.soderer.utilities.UserError.Reason;
import de.soderer.utilities.Utilities;

public class UpdateApplicationDialog extends ProgressShowingModalDialog<Boolean> {
	private static final transient Logger logger = Logger.getLogger(UpdateApplicationDialog.class);

	private final String text;
	private final String downloadUrl;
	private boolean dailyUpdateCheck;
	private final Collection<X509Certificate> trustedCertificates;
	private SecureDataStore secureDataStore = null;
	private File keystoreFile = null;
	private static final byte[] keystoreFileSalt = "q34;kUGh@78".getBytes(Charset.forName("UTF-8"));
	private SimpleCredentialsData storedDownloadCredentials = null;
	private char[] keyStorePassword = null;
	private boolean storedCredentialsChanged = false;

	public UpdateApplicationDialog(final Shell shell, final String title, final String text, final String downloadUrl, final boolean dailyUpdateCheck, final Collection<X509Certificate> trustedCertificates) {
		super(shell, title);

		this.text = text;
		this.downloadUrl = downloadUrl;
		this.dailyUpdateCheck = dailyUpdateCheck;
		this.trustedCertificates = trustedCertificates;
	}

	public UpdateApplicationDialog(final Shell shell, final String title, final String text, final String downloadUrl, final boolean dailyUpdateCheck, final Collection<X509Certificate> trustedCertificates, final File keystoreFile,
			final char[] keystorePassword) throws Exception {
		super(shell, title);

		this.text = text;
		this.downloadUrl = downloadUrl;
		this.dailyUpdateCheck = dailyUpdateCheck;
		this.trustedCertificates = trustedCertificates;
		this.keystoreFile = keystoreFile;

		if (keystoreFile != null) {
			secureDataStore = openKeyStore(shell, keystoreFile, keystorePassword);
		}

		if (secureDataStore != null) {
			storedDownloadCredentials = secureDataStore.getEntry(SimpleCredentialsData.class,
					Utilities.getDomainFromUrl(downloadUrl.replace("<username>", "").replace("<password>", "").replace("<system>", "").replace("<bitmode>", "")) + "_Credentials");
		}
	}

	@Override
	protected Boolean getDefaultReturnValue() {
		return false;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, false));

		final Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(2, true));

		final Label label = new Label(composite, SWT.FILL);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
		label.setText(text);

		final Button dailyUpdateCheckCheckBox = new Button(composite, SWT.CHECK);
		dailyUpdateCheckCheckBox.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true, 2, 1));
		dailyUpdateCheckCheckBox.setSelection(dailyUpdateCheck);
		dailyUpdateCheckCheckBox.setText(LangResources.get("dailyUpdateCheck"));
		dailyUpdateCheckCheckBox.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				dailyUpdateCheck = ((Button) e.widget).getSelection();
			}
		});

		final Button updateButton = new Button(composite, SWT.PUSH);
		updateButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		updateButton.setText(LangResources.get("update"));
		updateButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				updateApplication();
				setReturnValue(true);
				getParent().close();
			}
		});

		final Button cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				setReturnValue(false);
				getParent().close();
			}
		});

		shell.pack();
	}

	private void updateApplication() {
		try {
			final String jarFilePath = System.getProperty(SystemUtilities.SYSTEM_PARAMETER_NAME_CURRENT_RUNNING_JAR);
			final File downloadTempFile = new File(new File(jarFilePath).getParent() + File.separator + "temp_" + new File(jarFilePath).getName());

			if (Utilities.isNotBlank(jarFilePath)) {
				boolean tryDownloadAgain = true;
				while (tryDownloadAgain) {
					tryDownloadAgain = false;
					try {
						final boolean success = downloadUpdateFile(downloadTempFile, downloadUrl);
						if (!success) {
							// download stopped by user
							return;
						} else if (trustedCertificates != null) {
							final boolean isVerifiedJar = CryptographicUtilities.verifyJarSignature(downloadTempFile, trustedCertificates);
							if (!isVerifiedJar) {
								if (!showQuestion(LangResources.get("error.invalidOrNotTrustedFileSignature"), "yes", "no")) {
									return;
								}
							}
						}
					} catch (final UserError e) {
						showMessage(LangResources.get("menu.updateFileDownload"), e.getMessage());
						if (e.getReason() == Reason.UnauthenticatedOrUnauthorized) {
							tryDownloadAgain = true;
							storedDownloadCredentials = null;
						} else {
							return;
						}
					} catch (final Exception e) {
						if (e instanceof ExecutionException && e.getCause() instanceof UserError) {
							showMessage(LangResources.get("menu.updateFileDownload"), ((UserError) e.getCause()).getMessage());
							if (((UserError) e.getCause()).getReason() == Reason.UnauthenticatedOrUnauthorized) {
								tryDownloadAgain = true;
								storedDownloadCredentials = null;
							} else {
								return;
							}
						} else {
							showMessage(LangResources.get("menu.updateFileDownload"), e.getMessage());
						}
						return;
					}
				}

				if (SystemUtilities.isWindowsSystem()) {
					final File batchFile = new File(new File(jarFilePath).getParent() + File.separator + "batchUpdate_" + new File(jarFilePath).getName() + ".cmd");

					// Create Batchfile
					FileUtils.write(batchFile,
							"@echo off\r\n" + "del \"" + jarFilePath + "\"\r\n" + "if exist \"" + jarFilePath + "\" (\r\n" + "ping -n 3 127.0.0.1 >nul\r\n" + "del \"" + jarFilePath + "\"\r\n"
									+ "if exist \"" + jarFilePath + "\" (\r\n" + "ping -n 3 127.0.0.1 >nul\r\n" + "del \"" + jarFilePath + "\"\r\n" + "if exist \"" + jarFilePath + "\" (\r\n"
									+ "ping -n 3 127.0.0.1 >nul\r\n" + "del \"" + jarFilePath + "\"\r\n" + ")\r\n" + ")\r\n" + ")\r\n" + "ren \"" + downloadTempFile.getAbsolutePath() + "\" \""
									+ new File(jarFilePath).getName() + "\"\r\n" + "if not exist \"" + downloadTempFile.getAbsolutePath() + "\" (\r\n" + "java -jar \"" + jarFilePath + "\"\r\n"
									+ "del \"" + batchFile.getAbsolutePath() + "\"\r\n" + ")\r\n");

					Runtime.getRuntime().exec("cmd /c start /B " + batchFile.getAbsolutePath());
					Runtime.getRuntime().exit(0);
				} else if (SystemUtilities.isLinuxSystem()) {
					final File batchFile = new File(new File(jarFilePath).getParent() + File.separator + "batchUpdate_" + new File(jarFilePath).getName() + ".sh");

					// Create Batchfile
					FileUtils.write(batchFile,
							"#!/bin/bash\n" + "rm \"" + jarFilePath + "\"\n" + "if [ -f \"" + jarFilePath + "\" ]; then\n" + "sleep 3\n" + "rm \"" + jarFilePath + "\"\n" + "if [ -f \"" + jarFilePath
							+ "\" ]; then\n" + "sleep 3\n" + "rm \"" + jarFilePath + "\"\n" + "if [ -f \"" + jarFilePath + "\" ]; then\n" + "sleep 3\n" + "rm \"" + jarFilePath + "\"\n"
							+ "fi\n" + "fi\n" + "fi\n" + "mv \"" + downloadTempFile.getAbsolutePath() + "\" \"" + jarFilePath + "\"\n" + "if ! [ -f \"" + downloadTempFile.getAbsolutePath()
							+ "\" ]; then\n" + "java -jar \"" + jarFilePath + "\"\n" + "rm \"" + batchFile.getAbsolutePath() + "\"\n" + "fi\n");

					Runtime.getRuntime().exec("sh " + batchFile.getAbsolutePath());
					Runtime.getRuntime().exit(0);
				}
			} else {
				new QuestionDialog(getParent(), getText(), LangResources.get("cannotUpdate", "Jar file not found")).open();
			}
		} catch (final Exception e) {
			logger.error(LangResources.get("cannotUpdate", e.getMessage()), e);
		}
	}

	public boolean downloadUpdateFile(final File downloadTempFile, String downloadUrl) throws Exception {
		try {
			downloadTempFile.delete();
			if (downloadUrl.contains("<username>") && downloadUrl.contains("<password>")) {
				if (storedDownloadCredentials != null) {
					downloadUrl = downloadUrl.replace("<username>", URLEncoder.encode(storedDownloadCredentials.getUserName(), "UTF-8"));
					downloadUrl = downloadUrl.replace("<password>", URLEncoder.encode(new String(storedDownloadCredentials.getPassword()), "UTF-8"));
				} else {
					final CredentialsDialog credentialsDialog = new CredentialsDialog(getParent(), getText(), LangResources.get("enterDownloadCredentials"));
					credentialsDialog.setKeepInMemoryText(LangResources.get("storeUpdateAccount"));
					final Boolean success = credentialsDialog.open();
					if (success) {
						storedCredentialsChanged = true;
						downloadUrl = downloadUrl.replace("<username>", URLEncoder.encode(credentialsDialog.getUsername(), "UTF-8"));
						downloadUrl = downloadUrl.replace("<password>", URLEncoder.encode(new String(credentialsDialog.getPassword()), "UTF-8"));
						if (credentialsDialog.keepInMemory()) {
							storedDownloadCredentials = new SimpleCredentialsData(credentialsDialog.getUsername(), credentialsDialog.getPassword());
						} else {
							storedDownloadCredentials = null;
						}
					} else {
						storedDownloadCredentials = null;
						return false;
					}
				}
			} else {
				if (downloadUrl.contains("<username>")) {
					if (storedDownloadCredentials != null) {
						downloadUrl = downloadUrl.replace("<username>", URLEncoder.encode(storedDownloadCredentials.getUserName(), "UTF-8"));
					} else {
						final SimpleInputDialog usernameDialog = new SimpleInputDialog(getParent(), getText(), LangResources.get("enterDownloadUserName"));
						final String username = usernameDialog.open();
						if (Utilities.isNotBlank(username)) {
							downloadUrl = downloadUrl.replace("<username>", URLEncoder.encode(username, "UTF-8"));
						} else {
							return false;
						}
					}
				}
				if (downloadUrl.contains("<password>")) {
					if (storedDownloadCredentials != null) {
						downloadUrl = downloadUrl.replace("<password>", URLEncoder.encode(new String(storedDownloadCredentials.getPassword()), "UTF-8"));
					} else {
						final PasswordDialog passwordDialog = new PasswordDialog(getParent(), getText(), LangResources.get("enterDownloadPassword"));
						final char[] password = passwordDialog.open();
						if (password != null) {
							downloadUrl = downloadUrl.replace("<password>", URLEncoder.encode(new String(password), "UTF-8"));
						} else {
							return false;
						}
					}
				}
			}
			if (downloadUrl.contains("<system>")) {
				if (SystemUtilities.isWindowsSystem()) {
					downloadUrl = downloadUrl.replace("<system>", "Windows");
				} else if (SystemUtilities.isLinuxSystem()) {
					downloadUrl = downloadUrl.replace("<system>", "Linux");
				}
			}
			if (downloadUrl.contains("<bitmode>")) {
				if (System.getProperty("os.arch") != null && System.getProperty("os.arch").contains("64")) {
					downloadUrl = downloadUrl.replace("<bitmode>", "64");
				} else if (SystemUtilities.isLinuxSystem()) {
					downloadUrl = downloadUrl.replace("<bitmode>", "32");
				}
			}

			final FileDownloadWorker worker = new FileDownloadWorker(this, downloadUrl, downloadTempFile);
			progressDialog = new ProgressDialog(getParent(), LangResources.get("menu.updateFileDownload"), LangResources.get("menu.updateFileDownload"));
			new Thread(worker).start();
			if (progressDialog.open()) {
				worker.cancel(true);
				showMessage(LangResources.get("menu.updateFileDownload"), LangResources.get("canceledByUser"));
				return false;
			}

			if (worker.get()) {
				if (storedCredentialsChanged) {
					if (secureDataStore != null) {
						if (storedDownloadCredentials != null) {
							secureDataStore.addEntry(Utilities.getDomainFromUrl(downloadUrl.replace("<username>", "").replace("<password>", "").replace("<system>", "").replace("<bitmode>", "")) + "_Credentials", storedDownloadCredentials);
						} else {
							secureDataStore.removeEntry(SimpleCredentialsData.class, Utilities.getDomainFromUrl(downloadUrl.replace("<username>", "").replace("<password>", "").replace("<system>", "").replace("<bitmode>", "")) + "_Credentials");
						}
						keyStorePassword = new PasswordDialog(getParent(), getText(), LangResources.get("enterKeystorePassword")).open();
						secureDataStore.save(keystoreFile, keyStorePassword, keystoreFileSalt);
					} else {
						if (storedDownloadCredentials != null) {
							secureDataStore = new SecureDataStore();

							secureDataStore.addEntry(Utilities.getDomainFromUrl(downloadUrl.replace("<username>", "").replace("<password>", "").replace("<system>", "").replace("<bitmode>", "")) + "_Credentials", storedDownloadCredentials);

							keyStorePassword = new PasswordDialog(getParent(), getText(), LangResources.get("enterKeystorePassword")).open();
							secureDataStore.save(keystoreFile, keyStorePassword, keystoreFileSalt);
						}
					}
				}
				return true;
			} else {
				return false;
			}
		} catch (final Exception e) {
			if (e instanceof ExecutionException && e.getCause() instanceof UserError) {
				throw (UserError) e.getCause();
			} else {
				throw e;
			}
		}
	}

	public SecureDataStore openKeyStore(final Shell shell, final File keystoreFile, char[] password) {
		if (keystoreFile.exists()) {
			SecureDataStore keyStore = null;

			if (password == null) {
				// try to use empty password
				try {
					password = "".toCharArray();
					keyStore = new SecureDataStore();
					keyStore.load(keystoreFile, password, keystoreFileSalt);
				} catch (final Exception e1) {
					password = null;
					keyStore = null;
				}
			}

			if (password == null && keyStore == null) {
				final PasswordDialog dialog = new PasswordDialog(shell, LangResources.get("window_title"), LangResources.get("enterKeystorePassword"));
				dialog.setKeepInMemoryText(LangResources.get("keepPasswordInMemory"));
				password = dialog.open();
				if (dialog.keepInMemory()) {
					keyStorePassword = password;
				}
			}

			if (password != null && keyStore == null) {
				try {
					keyStore = new SecureDataStore();
					keyStore.load(keystoreFile, password, keystoreFileSalt);
				} catch (final Exception e) {
					e.printStackTrace();
					new QuestionDialog(shell, getText(), LangResources.get("cannotLoadKeystore"), LangResources.get("ok")).open();
					keyStore = null;
				}
			}

			return keyStore;
		} else {
			return null;
		}
	}

	public boolean getDailyUpdateCheck() {
		return dailyUpdateCheck;
	}

	public char[] getKeyStorePassword() {
		return keyStorePassword;
	}
}
