package de.soderer.utilities.swt;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.soderer.utilities.FtpConnection;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.SecureDataStore;
import de.soderer.utilities.ServerAccessData;
import de.soderer.utilities.SftpConnection;
import de.soderer.utilities.Utilities;

public class RemoteFileDialog extends ModalDialog<Boolean> {
	private static final List<String> PROTOCOL_LIST = Arrays.asList("SSH", "SFTP", "FTP");

	public enum Mode {
		Open, Save
	}

	private String protocol = "SSH";
	private String hostnameOrIp = null;
	private int port = 0;
	private String username = null;
	private String keyFilePath = null;
	private char[] password = null;
	private boolean checkHostKey = false;
	private String hostKey = null;
	private String selectedFilePath = null;
	private String selectedFileName = null;
	private LabeledCombo protocolField;
	private LabeledText hostnameField;
	private LabeledText portField;
	private LabeledText usernameField;
	private Button useKeyFileButton;
	private LabeledText passwordField;
	private Button checkHostKeyButton;
	private Text hostKeyField;
	private Tree folderTree;
	private Tree filesTree;
	private Combo serverEntriesCombo;
	private Button loadServerEntriesButton;
	private Button removeServerEntryButton;
	private Button saveServerEntryButton;
	private Button connectButton;
	private Button actionButton;
	private char[] inMemoryPassword;
	private final File keystoreFile;
	private final byte[] keystoreFileSalt;
	private SecureDataStore keyStore = null;

	private final Mode dialogMode;
	private LabeledText fileNameField;
	private Button createFolderButton;
	private Button deleteFolderButton;
	private Button deleteFileButton;

	private SftpConnection sftpConnection = null;
	private FtpConnection ftpConnection = null;

	public RemoteFileDialog(final Shell parentShell, final String title, final Mode dialogMode) {
		super(parentShell, title);
		keystoreFile = null;
		keystoreFileSalt = null;
		inMemoryPassword = null;
		this.dialogMode = dialogMode;
	}

	public RemoteFileDialog(final Shell parentShell, final String title, final Mode dialogMode, final File keystoreFile, final byte[] keystoreFileSalt, final char[] inMemoryPassword) {
		super(parentShell, title);
		this.keystoreFile = keystoreFile;
		this.keystoreFileSalt = keystoreFileSalt;
		this.inMemoryPassword = inMemoryPassword;
		this.dialogMode = dialogMode;
	}

	@Override
	protected void createComponents(final Shell parentShell) throws Exception {
		parentShell.setLayout(new GridLayout(1, true));

		if (keystoreFile != null) {
			final Composite serverEntriesComposite = new Composite(parentShell, SWT.BORDER);
			serverEntriesComposite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
			serverEntriesComposite.setLayout(SwtUtilities.createNoMarginGridLayout(5, false));

			final Label serverEntriesLabel = new Label(serverEntriesComposite, SWT.NONE);
			serverEntriesLabel.setText(LangResources.get("serverEntries"));

			serverEntriesCombo = new Combo(serverEntriesComposite, SWT.NONE);
			serverEntriesCombo.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
			serverEntriesCombo.setItems(new String[] {});
			serverEntriesCombo.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					checkButtonStatus();
				}
			});
			serverEntriesCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					if (Utilities.isNotBlank(serverEntriesCombo.getText())) {
						setServerAccessData(keyStore.getEntry(ServerAccessData.class, serverEntriesCombo.getText()));
					}
				}
			});

			loadServerEntriesButton = new Button(serverEntriesComposite, SWT.PUSH);
			loadServerEntriesButton.setText(LangResources.get("loadServerEntries"));
			loadServerEntriesButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					loadServerEntries();

					checkButtonStatus();
				}
			});

			removeServerEntryButton = new Button(serverEntriesComposite, SWT.PUSH);
			removeServerEntryButton.setText(LangResources.get("removeServerEntry"));
			removeServerEntryButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					if (keyStore != null) {
						keyStore.removeEntry(ServerAccessData.class, serverEntriesCombo.getText());
						serverEntriesCombo.setItems(keyStore.getEntryNames(ServerAccessData.class).toArray(new String[0]));
						serverEntriesCombo.clearSelection();
						serverEntriesCombo.setText("");
					}

					serverEntriesCombo.setText("");

					final char[] password = new PasswordDialog(getParent(), getText(), LangResources.get("enterKeystorePassword")).open();
					if (password != null) {
						try {
							keyStore.save(keystoreFile, password, keystoreFileSalt);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}

					checkButtonStatus();
				}
			});

			saveServerEntryButton = new Button(serverEntriesComposite, SWT.PUSH);
			saveServerEntryButton.setText(LangResources.get("saveServerEntry"));
			saveServerEntryButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					if (keyStore == null) {
						keyStore = new SecureDataStore();
					}
					final String newEntryName = serverEntriesCombo.getText();
					keyStore.addEntry(serverEntriesCombo.getText(), getServerEntry());
					final char[] password = new PasswordDialog(getParent(), getText(), LangResources.get("enterKeystorePassword")).open();
					if (password != null) {
						try {
							keyStore.save(keystoreFile, password, keystoreFileSalt);
						} catch (final Exception e) {
							e.printStackTrace();
						}
					}
					serverEntriesCombo.setItems(keyStore.getEntryNames(ServerAccessData.class).toArray(new String[0]));
					serverEntriesCombo.setText(newEntryName);
				}
			});
		}

		final Composite serverCredentialsComposite = new Composite(parentShell, SWT.BORDER);
		serverCredentialsComposite.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false));
		serverCredentialsComposite.setLayout(SwtUtilities.createNoMarginGridLayout(9, false));

		protocolField = new LabeledCombo(serverCredentialsComposite, SWT.NONE, LangResources.get("protocol"), PROTOCOL_LIST, 0, ArrangementStyle.Vertical);
		final GridData protocolFieldGridData = new GridData(SWT.FILL, SWT.UP, true, true);
		protocolFieldGridData.widthHint = 50;
		protocolField.setLayoutData(protocolFieldGridData);
		protocolField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				protocol = PROTOCOL_LIST.get(((LabeledCombo) event.widget).getSelectedItemIndex());
				if (("ssh".equalsIgnoreCase(protocol) || "sftp".equalsIgnoreCase(protocol)) && portField.getText().equals("21")) {
					portField.setText("22");
				} else if ("ftp".equalsIgnoreCase(protocol) && portField.getText().equals("22")) {
					portField.setText("21");
				}
				checkButtonStatus();
			}
		});

		hostnameField = new LabeledText(serverCredentialsComposite, SWT.NONE, LangResources.get("hostnameOrIp"), ArrangementStyle.Vertical);
		final GridData hostnameFieldGridData = new GridData(SWT.FILL, SWT.UP, true, true);
		hostnameFieldGridData.widthHint = 195;
		hostnameField.setLayoutData(hostnameFieldGridData);
		hostnameField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				hostnameOrIp = ((LabeledText) event.widget).getText();
				checkButtonStatus();
			}
		});

		portField = new LabeledText(serverCredentialsComposite, SWT.NONE, LangResources.get("port"), ArrangementStyle.Vertical, "[0-9]*");
		portField.setText("");
		final GridData portFieldGridData = new GridData(SWT.FILL, SWT.UP, true, true);
		portFieldGridData.widthHint = 40;
		portField.setLayoutData(portFieldGridData);
		portField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				try {
					port = Integer.parseInt(((LabeledText) event.widget).getText());
				} catch (final Exception e) {
					port = 0;
				}
				checkButtonStatus();
			}
		});

		usernameField = new LabeledText(serverCredentialsComposite, SWT.NONE, LangResources.get("username"), ArrangementStyle.Vertical);
		final GridData usernameFieldGridData = new GridData(SWT.FILL, SWT.UP, false, false);
		usernameFieldGridData.widthHint = 95;
		usernameField.setLayoutData(usernameFieldGridData);
		usernameField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				username = ((LabeledText) event.widget).getText();
				checkButtonStatus();
			}
		});

		useKeyFileButton = new Button(serverCredentialsComposite, SWT.CHECK);
		useKeyFileButton.setText(LangResources.get("useKeyFile"));
		useKeyFileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					final FileDialog dialog = new FileDialog(getParent(), SWT.OPEN);
					dialog.setText(LangResources.get("openKeyFile"));
					dialog.setFileName(keyFilePath);
					final String fileName = dialog.open();
					if (fileName != null) {
						keyFilePath = fileName;
					} else {
						keyFilePath = null;
					}
				} else {
					keyFilePath = null;
				}
				checkButtonStatus();
			}
		});

		passwordField = new LabeledText(serverCredentialsComposite, SWT.PASSWORD, LangResources.get("password"), ArrangementStyle.Vertical);
		passwordField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				password = ((LabeledText) event.widget).getText().toCharArray();
				checkButtonStatus();
			}
		});


		final Composite checkHostKeyComposite = new Composite(serverCredentialsComposite, SWT.NONE);
		final GridData serverCredentialsCompositeGridData = new GridData(SWT.LEFT, SWT.CENTER, false, false);
		serverCredentialsCompositeGridData.widthHint = 255;
		checkHostKeyComposite.setLayoutData(serverCredentialsCompositeGridData);
		checkHostKeyComposite.setLayout(SwtUtilities.createNoMarginGridLayout(1, false));

		checkHostKeyButton = new Button(checkHostKeyComposite, SWT.CHECK);
		checkHostKeyButton.setText(LangResources.get("checkHostKey"));
		checkHostKeyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				checkHostKey = ((Button) event.widget).getSelection();
				if (checkHostKey && Utilities.isBlank(hostKeyField.getText())) {
					hostKeyField.setText("~/.ssh/known_hosts");
				}
				checkButtonStatus();
			}
		});

		hostKeyField = new Text(checkHostKeyComposite, SWT.BORDER);
		hostKeyField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		hostKeyField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				hostKey = ((Text) event.widget).getText();
				checkButtonStatus();
			}
		});

		connectButton = new Button(serverCredentialsComposite, SWT.PUSH);
		connectButton.setText(LangResources.get("connect"));
		connectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (sftpConnection == null && ftpConnection == null) {
					if ("SSH".equalsIgnoreCase(protocol) || "SFTP".equalsIgnoreCase(protocol)) {
						connectViaSSH();
					} else if ("FTP".equalsIgnoreCase(protocol)) {
						connectViaFTP();
					}
				} else {
					disconnect();
				}
				checkButtonStatus();
			}
		});

		final SashForm sashForm = new SashForm(parentShell, SWT.SMOOTH | SWT.HORIZONTAL | SWT.BORDER);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));

		final Composite treePartComposite = new Composite(sashForm, SWT.NONE);
		treePartComposite.setLayout(SwtUtilities.createNoMarginGridLayout(1, false));
		folderTree = new Tree(treePartComposite, SWT.BORDER);
		final GridData folderTreeGridData = new GridData(SWT.FILL, SWT.UP, true, true);
		folderTreeGridData.heightHint = 300;
		folderTree.setLayoutData(folderTreeGridData);
		folderTree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				if ("SSH".equalsIgnoreCase(protocol) || "SFTP".equalsIgnoreCase(protocol)) {
					showPathDirectorySSH();
				} else if ("FTP".equalsIgnoreCase(protocol)) {
					showPathDirectoryFTP();
				}
				checkButtonStatus();
			}
		});

		final Composite detailPartComposite = new Composite(sashForm, SWT.NONE);
		detailPartComposite.setLayout(SwtUtilities.createNoMarginGridLayout(1, false));
		filesTree = new Tree(detailPartComposite, SWT.BORDER);
		final GridData filesTreeGridData = new GridData(SWT.FILL, SWT.UP, true, true);
		filesTreeGridData.heightHint = 300;
		filesTree.setLayoutData(filesTreeGridData);
		filesTree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				try {
					final TreeItem selectedFilesItem = filesTree.getSelection()[0];
					selectedFileName = selectedFilesItem.getText();
					if (dialogMode == Mode.Save) {
						fileNameField.setText(selectedFileName);
					}
				} catch (final Exception e) {
					e.printStackTrace();
				}
				checkButtonStatus();
			}
		});
		filesTree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(final MouseEvent e) {
				if (((Tree) e.widget).getSelection().length == 1) {
					setReturnValue(true);
					getParent().close();
				}
			}
		});

		if (dialogMode == Mode.Save) {
			final Composite additionalButtonsComposite = new Composite(parentShell, SWT.NONE);
			additionalButtonsComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			additionalButtonsComposite.setLayout(SwtUtilities.createNoMarginGridLayout(3, false));

			createFolderButton = new Button(additionalButtonsComposite, SWT.PUSH);
			createFolderButton.setText(LangResources.get("createFolder"));
			createFolderButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					final String folderName = new SimpleInputDialog(getParent(), getText(), LangResources.get("enterFolderName")).open();
					if (Utilities.isNotBlank(folderName)) {
						try {
							if ("SSH".equalsIgnoreCase(protocol) || "SFTP".equalsIgnoreCase(protocol)) {
								sftpConnection.createFolder(selectedFilePath, folderName);
								showPathDirectorySSH();
							} else if ("FTP".equalsIgnoreCase(protocol)) {
								ftpConnection.createFolder(selectedFilePath, folderName);
								showPathDirectoryFTP();
							}
						} catch (final Exception e) {
							new QuestionDialog(getParent(), getText(), "Error while creating folder on server: " + e.getMessage(), LangResources.get("ok")).open();
						}
					}
				}
			});

			deleteFolderButton = new Button(additionalButtonsComposite, SWT.PUSH);
			deleteFolderButton.setText(LangResources.get("deleteFolder"));
			deleteFolderButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					if (Utilities.isNotBlank(selectedFilePath)) {
						if (new QuestionDialog(getParent(), LangResources.get("deleteFolder"), LangResources.get("reallyDeleteFolder", selectedFilePath), LangResources.get("ok"), LangResources.get("cancel")).open() == 0) {
							try {
								if ("SSH".equalsIgnoreCase(protocol) || "SFTP".equalsIgnoreCase(protocol)) {
									sftpConnection.deleteFolder(selectedFilePath);
									folderTree.select(folderTree.getSelection()[0].getParentItem());
									showPathDirectorySSH();
								} else if ("FTP".equalsIgnoreCase(protocol)) {
									ftpConnection.deleteFolder(selectedFilePath);
									folderTree.select(folderTree.getSelection()[0].getParentItem());
									showPathDirectoryFTP();
								}
							} catch (final Exception e) {
								new QuestionDialog(getParent(), getText(), "Error while connecting to server: " + e.getMessage(), LangResources.get("ok")).open();
							}
						}
					}
				}
			});

			deleteFileButton = new Button(additionalButtonsComposite, SWT.PUSH);
			deleteFileButton.setText(LangResources.get("deleteFile"));
			deleteFileButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					if (Utilities.isNotBlank(selectedFilePath) && Utilities.isNotBlank(selectedFileName)) {
						if (new QuestionDialog(getParent(), LangResources.get("deleteFile"), LangResources.get("reallyDeleteFile", selectedFilePath + "/" + selectedFileName), LangResources.get("ok"), LangResources.get("cancel")).open() == 0) {
							try {
								if ("SSH".equalsIgnoreCase(protocol) || "SFTP".equalsIgnoreCase(protocol)) {
									sftpConnection.deleteFile(selectedFilePath + "/" + selectedFileName);
									showPathDirectorySSH();
								} else if ("FTP".equalsIgnoreCase(protocol)) {
									ftpConnection.deleteFile(selectedFilePath + "/" + selectedFileName);
									showPathDirectoryFTP();
								}
							} catch (final Exception e) {
								new QuestionDialog(getParent(), getText(), "Error while connecting to server: " + e.getMessage(), LangResources.get("ok")).open();
							}
						}
					}
				}
			});

			final Composite additionalSaveComposite = new Composite(parentShell, SWT.BORDER);
			additionalSaveComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			additionalSaveComposite.setLayout(SwtUtilities.createNoMarginGridLayout(1, false));

			fileNameField = new LabeledText(additionalSaveComposite, SWT.NONE, LangResources.get("fileName"), ArrangementStyle.Vertical);
			fileNameField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
			fileNameField.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent event) {
					selectedFileName = ((LabeledText) event.widget).getText();
					checkButtonStatus();
				}
			});
		}

		final Composite mainButtonComposite = new Composite(parentShell, SWT.NONE);
		mainButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		mainButtonComposite.setLayout(new FillLayout());

		actionButton = new Button(mainButtonComposite, SWT.PUSH);
		actionButton.setText(LangResources.get(dialogMode == Mode.Open ? "open" : "save"));
		actionButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				disconnect();
				setReturnValue(true);
				getParent().close();
			}
		});

		final Button cancelButton = new Button(mainButtonComposite, SWT.PUSH);
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (password != null) {
					Arrays.fill(password, '\u0000');
					password = null;
				}
				disconnect();
				setReturnValue(false);
				getParent().close();
			}
		});

		loadServerEntries();

		parentShell.pack();

		checkButtonStatus();
	}

	private void connectViaSSH() {
		try {
			sftpConnection = new SftpConnection(getServerEntry());
			final List<String> folders = sftpConnection.getFolders("/");
			final List<String> files = sftpConnection.getFiles("/");
			folderTree.removeAll();
			final TreeItem rootTreeItem = new TreeItem(folderTree, SWT.NONE);
			rootTreeItem.setText("/");
			for (final String folderName : folders) {
				final TreeItem folderTreeItem = new TreeItem(rootTreeItem, SWT.NONE);
				folderTreeItem.setText(folderName);
			}
			for (final String fileName : files) {
				final TreeItem filesTreeItem = new TreeItem(filesTree, SWT.NONE);
				filesTreeItem.setText(fileName);
			}
			rootTreeItem.setExpanded(true);
			folderTree.select(folderTree.getItem(0));
			selectedFilePath = "/";
		} catch (final Exception e) {
			disconnect();
			new QuestionDialog(getParent(), getText(), "Error while connecting to server: " + e.getMessage(), LangResources.get("ok")).open();
		}

		checkButtonStatus();
	}

	private void connectViaFTP() {
		try {
			ftpConnection = new FtpConnection(getServerEntry());
			final List<String> folders = ftpConnection.getFolders("/");
			final List<String> files = ftpConnection.getFiles("/");
			folderTree.removeAll();
			final TreeItem rootTreeItem = new TreeItem(folderTree, SWT.NONE);
			rootTreeItem.setText("/");
			for (final String folderName : folders) {
				final TreeItem folderTreeItem = new TreeItem(rootTreeItem, SWT.NONE);
				folderTreeItem.setText(folderName);
			}
			for (final String fileName : files) {
				final TreeItem filesTreeItem = new TreeItem(filesTree, SWT.NONE);
				filesTreeItem.setText(fileName);
			}
			rootTreeItem.setExpanded(true);
			folderTree.select(folderTree.getItem(0));
			selectedFilePath = "/";
		} catch (final Exception e) {
			FtpConnection.closeQuietly(ftpConnection);
			ftpConnection = null;
			new QuestionDialog(getParent(), getText(), "Error while connecting to server: " + e.getMessage(), LangResources.get("ok")).open();
		}

		checkButtonStatus();
	}

	private void disconnect() {
		if (ftpConnection != null) {
			Utilities.closeQuietly(ftpConnection);
			ftpConnection = null;
		}

		if (sftpConnection != null) {
			Utilities.closeQuietly(sftpConnection);
			sftpConnection = null;
		}

		folderTree.removeAll();
		filesTree.removeAll();
	}

	private void checkButtonStatus() {
		if (sftpConnection != null || ftpConnection != null) {
			if (keystoreFile != null) {
				serverEntriesCombo.setEnabled(false);
				loadServerEntriesButton.setEnabled(false);
				removeServerEntryButton.setEnabled(false);
				saveServerEntryButton.setEnabled(false);
			}
			connectButton.setEnabled(true);
			connectButton.setText(LangResources.get("disconnect"));
			actionButton.setEnabled(Utilities.isNotBlank(selectedFileName) && Utilities.isNotBlank(selectedFilePath));
			protocolField.setEnabled(false);
			hostnameField.setEnabled(false);
			portField.setEnabled(false);
			usernameField.setEnabled(false);
			passwordField.setEnabled(false);
			useKeyFileButton.setEnabled(false);
			if (dialogMode == Mode.Save) {
				fileNameField.setEnabled(true);
				createFolderButton.setEnabled(true);
				deleteFolderButton.setEnabled(true);
				deleteFileButton.setEnabled(true);
			}
			checkHostKeyButton.setEnabled(false);
			hostKeyField.setEnabled(false);
		} else {
			if (keystoreFile != null) {
				serverEntriesCombo.setEnabled(serverEntriesCombo.getItemCount() > 0 || (Utilities.isNotBlank(hostnameOrIp) && Utilities.isNotBlank(username)));
				loadServerEntriesButton.setEnabled(keystoreFile.exists());
				removeServerEntryButton.setEnabled(Utilities.isNotBlank(serverEntriesCombo.getText()));
				saveServerEntryButton.setEnabled(Utilities.isNotBlank(serverEntriesCombo.getText()) && Utilities.isNotBlank(hostnameOrIp) && Utilities.isNotBlank(username));
			}
			connectButton.setEnabled(Utilities.isNotBlank(hostnameOrIp) && Utilities.isNotBlank(username));
			connectButton.setText(LangResources.get("connect"));
			actionButton.setEnabled(false);
			protocolField.setEnabled(true);
			hostnameField.setEnabled(true);
			portField.setEnabled(true);
			usernameField.setEnabled(true);
			passwordField.setEnabled(true);
			useKeyFileButton.setEnabled(true);
			if (dialogMode == Mode.Save) {
				fileNameField.setEnabled(false);
				createFolderButton.setEnabled(false);
				deleteFolderButton.setEnabled(false);
				deleteFileButton.setEnabled(false);
			}
			checkHostKeyButton.setEnabled("ssh".equalsIgnoreCase(protocol) || "sftp".equalsIgnoreCase(protocol));
			hostKeyField.setEnabled(("ssh".equalsIgnoreCase(protocol) || "sftp".equalsIgnoreCase(protocol)) && checkHostKeyButton.getSelection());
		}
	}

	public ServerAccessData getServerEntry() {
		return new ServerAccessData(protocol, hostnameOrIp, port, username, keyFilePath, password, ("ssh".equalsIgnoreCase(protocol) || "sftp".equalsIgnoreCase(protocol)) && checkHostKey ? hostKey : null);
	}

	public String getSelectedFile() {
		return selectedFilePath + (selectedFilePath.endsWith("/") ? "" : "/") + selectedFileName;
	}

	public char[] getInMemoryPassword() {
		return inMemoryPassword;
	}

	private void loadServerEntries() {
		if (keystoreFile.exists()) {
			boolean passwordWasWrong = false;

			char[] password = inMemoryPassword == null ? "".toCharArray() : Arrays.copyOf(inMemoryPassword, inMemoryPassword.length);
			try {
				keyStore = new SecureDataStore();
				keyStore.load(keystoreFile, password, keystoreFileSalt);
			} catch (final Exception e) {
				password = null;
				keyStore = null;
				if (inMemoryPassword != null) {
					Arrays.fill(inMemoryPassword, '\u0000');
				}
				inMemoryPassword = null;
				passwordWasWrong = true;
			}

			while (passwordWasWrong) {
				final PasswordDialog dialog = new PasswordDialog(getParent(), getText(), LangResources.get("enterKeystorePassword"));
				dialog.setKeepInMemoryText(LangResources.get("keepPasswordInMemory"));
				password = dialog.open();
				if (password != null) {
					if (dialog.keepInMemory()) {
						inMemoryPassword = Arrays.copyOf(password, password.length);
					}
					try {
						keyStore = new SecureDataStore();
						// Kills the password after usage
						keyStore.load(keystoreFile, password, keystoreFileSalt);
						passwordWasWrong = false;
					} catch (final Exception e) {
						e.printStackTrace();
						new QuestionDialog(getParent(), getText(), LangResources.get("cannotLoadKeystore"), LangResources.get("ok")).open();
						keyStore = null;
						passwordWasWrong = true;
						if (inMemoryPassword != null) {
							Arrays.fill(inMemoryPassword, '\u0000');
						}
						inMemoryPassword = null;
					}
				} else {
					passwordWasWrong = false;
					keyStore = null;
				}
			}
		} else {
			keyStore = null;
		}

		if (keyStore != null) {
			serverEntriesCombo.setItems(keyStore.getEntryNames(ServerAccessData.class).toArray(new String[0]));
			if (serverEntriesCombo.getItemCount() > 0) {
				serverEntriesCombo.select(0);
				final String selectionName = serverEntriesCombo.getText();
				if (Utilities.isNotBlank(selectionName)) {
					final ServerAccessData entry = keyStore.getEntry(ServerAccessData.class, selectionName);
					setServerAccessData(entry);
				}
			}
		} else {
			serverEntriesCombo.setItems(new String[] {});
		}
	}

	private void setServerAccessData(final ServerAccessData entry) {
		for (int i = 0; i < PROTOCOL_LIST.size(); i++) {
			final String protocol = PROTOCOL_LIST.get(i);
			if (protocol.equalsIgnoreCase(entry.getProtocol())) {
				protocolField.setSelectedItemIndex(i);
			}
		}
		hostnameField.setText(entry.getHostname());
		portField.setText(Integer.toString(entry.getPort()));
		usernameField.setText(entry.getUsername());
		keyFilePath = entry.getKeyFile();
		useKeyFileButton.setSelection(Utilities.isNotBlank(keyFilePath));
		passwordField.setText(new String(entry.getPassword()));
		hostKey = entry.getHostKey();
		checkHostKey = hostKey != null;
		checkHostKeyButton.setSelection(checkHostKey);
		hostKeyField.setText(checkHostKey ? entry.getHostKey() : "");
	}

	private void showPathDirectorySSH() {
		try {
			final TreeItem selectedFolderItem = folderTree.getSelection()[0];
			String path = selectedFolderItem.getText();
			TreeItem parentTreeItem = selectedFolderItem.getParentItem();
			while (parentTreeItem != null) {
				path = parentTreeItem.getText() + (parentTreeItem.getText().endsWith("/") ? "" : "/") + path;
				parentTreeItem = parentTreeItem.getParentItem();
			}
			final List<String> folders = sftpConnection.getFolders(path);
			final List<String> files = sftpConnection.getFiles(path);
			for (final TreeItem subItem : selectedFolderItem.getItems()) {
				if (!folders.contains(subItem.getText())) {
					subItem.dispose();
				}
			}
			for (final String folderName : folders) {
				boolean subItemExists = false;
				for (final TreeItem subItem : selectedFolderItem.getItems()) {
					if (subItem.getText().equals(folderName)) {
						subItemExists = true;
						break;
					}
				}
				if (!subItemExists) {
					final TreeItem folderTreeItem = new TreeItem(selectedFolderItem, SWT.NONE);
					folderTreeItem.setText(folderName);
				}
			}
			selectedFolderItem.setExpanded(true);
			filesTree.removeAll();
			for (final String fileName : files) {
				final TreeItem filesTreeItem = new TreeItem(filesTree, SWT.NONE);
				filesTreeItem.setText(fileName);
			}
			if (files.size() > 0) {
				filesTree.setSelection(filesTree.getItem(0));
			}
			selectedFilePath = path;
		} catch (final Exception e) {
			new QuestionDialog(getParent(), getText(), "Error while connecting to server: " + e.getMessage(), LangResources.get("ok")).open();
		}
	}

	private void showPathDirectoryFTP() {
		try {
			final TreeItem selectedFolderItem = folderTree.getSelection()[0];
			String path = selectedFolderItem.getText();
			TreeItem parentTreeItem = selectedFolderItem.getParentItem();
			while (parentTreeItem != null) {
				path = parentTreeItem.getText() + (parentTreeItem.getText().endsWith("/") ? "" : "/") + path;
				parentTreeItem = parentTreeItem.getParentItem();
			}
			final List<String> folders = ftpConnection.getFolders(path);
			final List<String> files = ftpConnection.getFiles(path);
			for (final TreeItem subItem : selectedFolderItem.getItems()) {
				if (!folders.contains(subItem.getText())) {
					subItem.dispose();
				}
			}
			for (final String folderName : folders) {
				boolean subItemExists = false;
				for (final TreeItem subItem : selectedFolderItem.getItems()) {
					if (subItem.getText().equals(folderName)) {
						subItemExists = true;
						break;
					}
				}
				if (!subItemExists) {
					final TreeItem folderTreeItem = new TreeItem(selectedFolderItem, SWT.NONE);
					folderTreeItem.setText(folderName);
				}
			}
			selectedFolderItem.setExpanded(true);
			filesTree.removeAll();
			for (final String fileName : files) {
				final TreeItem filesTreeItem = new TreeItem(filesTree, SWT.NONE);
				filesTreeItem.setText(fileName);
			}
			if (files.size() > 0) {
				filesTree.setSelection(filesTree.getItem(0));
			}
			selectedFilePath = path;
		} catch (final Exception e) {
			new QuestionDialog(getParent(), getText(), "Error while connecting to server: " + e.getMessage(), LangResources.get("ok")).open();
		}
	}
}
