package de.soderer.utilities.swt;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
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
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.SecureDataStore;
import de.soderer.utilities.ServerAccessData;
import de.soderer.utilities.UnknownHostKeyException;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.remote.FtpConnection;
import de.soderer.utilities.remote.HttpFileConnection;
import de.soderer.utilities.remote.RemoteConnection;
import de.soderer.utilities.remote.SftpConnection;
import de.soderer.utilities.remote.Smb1Connection;
import de.soderer.utilities.remote.SmbConnection;
import de.soderer.utilities.swt.worker.DirectoryDownloadWorker;
import de.soderer.utilities.swt.worker.DirectoryUploadWorker;
import de.soderer.utilities.swt.worker.RemoteFileDownloadWorker;
import de.soderer.utilities.swt.worker.RemoteFileUploadWorker;

public class RemoteFileDialog extends DualProgressShowingModalDialog<Boolean> {
	private static final List<String> PROTOCOL_LIST = Arrays.asList("ssh", "sftp", "ftp", "smb", "smb1", "http", "https");
	private static final String STANDARD_KNOWNHOSTS_FILE = "~/.ssh/known_hosts";

	public enum Mode {
		Open, Save
	}

	private String protocol = "ssh";
	private String hostnameOrIp = null;
	private int port = 0;
	private String username = null;
	private String keyFilePath = null;
	private char[] password = null;
	private boolean checkHostKey = true;
	private String hostKey = STANDARD_KNOWNHOSTS_FILE;
	private String sharename = null;
	private String selectedFilePath = null;
	private String selectedFileName = null;

	private LabeledCombo protocolField;
	private LabeledText hostnameField;
	private LabeledText portField;
	private LabeledText usernameField;
	private Button useKeyFileButton;
	private LabeledText passwordField;
	private Button checkHostKeyButton;
	private LabeledText sharenameField;
	private Text hostKeyField;
	private Tree folderTree;
	private Tree filesTree;
	private Combo serverEntriesCombo;
	private Button loadServerEntriesButton;
	private Button removeServerEntryButton;
	private Button saveServerEntryButton;
	private Button connectButton;
	private Button action1Button;
	private Button action2Button;
	private char[] inMemoryPassword;
	private final File keystoreFile;
	private final byte[] keystoreFileSalt;
	private SecureDataStore keyStore = null;

	private final Mode dialogMode;
	private LabeledText fileNameField;
	private LabeledText currentPathField;
	private Button createFolderButton;
	private Button deleteFolderButton;
	private Button deleteFileButton;

	private RemoteConnection connection = null;

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

		final RemoteFileDialog remoteFileDialog = this;

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
		serverCredentialsComposite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		serverCredentialsComposite.setLayout(SwtUtilities.createNoMarginGridLayout(9, false));

		protocolField = new LabeledCombo(serverCredentialsComposite, SWT.NONE, LangResources.get("protocol"), PROTOCOL_LIST, 0, ArrangementStyle.Vertical);
		final GridData protocolFieldGridData = new GridData(SWT.FILL, SWT.UP, true, true);
		protocolFieldGridData.widthHint = 65;
		protocolField.setLayoutData(protocolFieldGridData);
		protocolField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				protocol = PROTOCOL_LIST.get(((LabeledCombo) event.widget).getSelectedItemIndex());
				if (portField.getText().equals("21") || portField.getText().equals("22") || portField.getText().equals("445") || portField.getText().equals("80") || portField.getText().equals("443")) {
					if ("ssh".equalsIgnoreCase(protocol) || "sftp".equalsIgnoreCase(protocol)) {
						portField.setText("22");
					} else if ("ftp".equalsIgnoreCase(protocol)) {
						portField.setText("21");
					} else if ("smb".equalsIgnoreCase(protocol)) {
						portField.setText("445");
					} else if ("smb1".equalsIgnoreCase(protocol)) {
						portField.setText("445");
					} else if ("http".equalsIgnoreCase(protocol)) {
						portField.setText("80");
					} else if ("https".equalsIgnoreCase(protocol)) {
						portField.setText("443");
					}
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

		portField = new LabeledText(serverCredentialsComposite, SWT.NONE, LangResources.get("port"), ArrangementStyle.Vertical, "[0-9]{0,5}");
		portField.setText("");
		final GridData portFieldGridData = new GridData(SWT.FILL, SWT.UP, true, true);
		portFieldGridData.widthHint = 60;
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
					if (keyFilePath == null || "".equals(keyFilePath.trim())) {
						dialog.setFilterPath(Utilities.replaceUsersHome("~/.ssh/"));
					} else {
						dialog.setFileName(keyFilePath);
					}
					final String fileName = dialog.open();
					if (fileName != null) {
						keyFilePath = fileName;
					} else {
						((Button) e.widget).setSelection(false);
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
		checkHostKeyButton.setSelection(checkHostKey);
		checkHostKeyButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				checkHostKey = ((Button) event.widget).getSelection();
				if (checkHostKey && Utilities.isBlank(hostKeyField.getText())) {
					hostKeyField.setText(STANDARD_KNOWNHOSTS_FILE);
				}
				checkButtonStatus();
			}
		});

		hostKeyField = new Text(checkHostKeyComposite, SWT.BORDER);
		hostKeyField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		hostKeyField.setText(hostKey);
		hostKeyField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				hostKey = ((Text) event.widget).getText();
				checkButtonStatus();
			}
		});

		sharenameField = new LabeledText(serverCredentialsComposite, SWT.NONE, LangResources.get("sharename"), ArrangementStyle.Vertical);
		final GridData sharenameFieldGridData = new GridData(SWT.FILL, SWT.UP, false, false);
		sharenameFieldGridData.widthHint = 95;
		sharenameField.setLayoutData(sharenameFieldGridData);
		sharenameField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				sharename = ((LabeledText) event.widget).getText();
				checkButtonStatus();
			}
		});

		connectButton = new Button(serverCredentialsComposite, SWT.PUSH);
		connectButton.setText(LangResources.get("connect"));
		connectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (connection == null) {
					connect();
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
				showPathDirectory();
				if (dialogMode == Mode.Open) {
					final TreeItem selectedFolderItem = folderTree.getSelection()[0];
					currentPathField.setText(SwtUtilities.getAbsolutePath(selectedFolderItem) + "/");
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
					} else {
						final TreeItem selectedFolderItem = folderTree.getSelection()[0];
						currentPathField.setText(SwtUtilities.getAbsolutePath(selectedFolderItem) + "/" + selectedFileName);
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
							connection.createFolder(selectedFilePath, folderName);
							showPathDirectory();
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
								connection.deleteFolder(selectedFilePath);
								folderTree.select(folderTree.getSelection()[0].getParentItem());
								showPathDirectory();
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
								connection.deleteFile(selectedFilePath + "/" + selectedFileName);
								showPathDirectory();
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
		} else {
			final Composite additionalSaveComposite = new Composite(parentShell, SWT.BORDER);
			additionalSaveComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			additionalSaveComposite.setLayout(SwtUtilities.createNoMarginGridLayout(1, false));

			currentPathField = new LabeledText(additionalSaveComposite, SWT.NONE, LangResources.get("currentPath"), ArrangementStyle.Vertical);
			currentPathField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
			currentPathField.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent event) {
					// TODO
					checkButtonStatus();
				}
			});
		}

		final Composite mainButtonComposite = new Composite(parentShell, SWT.NONE);
		mainButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		mainButtonComposite.setLayout(new FillLayout());

		action1Button = new Button(mainButtonComposite, SWT.PUSH);
		action1Button.setText(LangResources.get(dialogMode == Mode.Open ? "open" : "save"));
		action1Button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				disconnect();
				setReturnValue(true);
				getParent().close();
			}
		});

		action2Button = new Button(mainButtonComposite, SWT.PUSH);
		action2Button.setText(LangResources.get(dialogMode == Mode.Open ? "download" : "upload"));
		action2Button.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				if (dialogMode == Mode.Save) {
					final String currentPath = currentPathField.getText();
					if (currentPath.endsWith("/")) {
						final DirectoryDialog dialog = new DirectoryDialog(parentShell, SWT.OPEN);
						dialog.setText(LangResources.get("upload"));
						final String sourcePath = dialog.open();
						if (sourcePath != null) {
							try {
								final DirectoryUploadWorker directoryUploadWorker = new DirectoryUploadWorker(remoteFileDialog, connection, sourcePath, currentPath);
								progressDialog = new DualProgressDialog(getParent(), getText(), LangResources.get("upload") + " \"" + currentPath + "\"", "");
								new Thread(directoryUploadWorker).start();
								if (progressDialog.open()) {
									directoryUploadWorker.cancel(true);
									showMessage(LangResources.get("upload"), LangResources.get("canceledByUser"));
								} else {
									// check for errors
									directoryUploadWorker.get();
									showMessage(LangResources.get("upload"), LangResources.get("actionSuccessfullyCompleted"));
								}
							} catch (final Exception e) {
								new QuestionDialog(getParent(), getText(), "Error while uploading directory: " + e.getMessage(), LangResources.get("ok")).open();
							}
						}
					} else {
						final String currentFileName = currentPath.substring(currentPath.lastIndexOf("/") + 1);
						final FileDialog dialog = new FileDialog(parentShell, SWT.OPEN);
						dialog.setText(LangResources.get("upload"));
						dialog.setFileName(currentFileName);
						final String sourcePath = dialog.open();
						if (sourcePath != null) {
							try {
								final RemoteFileUploadWorker fileUploadWorker = new RemoteFileUploadWorker(remoteFileDialog, connection, sourcePath, currentPath);
								progressDialog = new ProgressDialog(getParent(), getText(), LangResources.get("upload"));
								new Thread(fileUploadWorker).start();
								if (progressDialog.open()) {
									fileUploadWorker.cancel(true);
									showMessage(LangResources.get("upload"), LangResources.get("canceledByUser"));
								} else {
									// check for errors
									fileUploadWorker.get();
									showMessage(LangResources.get("upload"), LangResources.get("actionSuccessfullyCompleted"));
								}
							} catch (final Exception e) {
								new QuestionDialog(getParent(), getText(), "Error while uploading file: " + e.getMessage(), LangResources.get("ok")).open();
							}
						}
					}
				} else {
					final String currentPath = currentPathField.getText();
					if (currentPath.endsWith("/")) {
						final DirectoryDialog dialog = new DirectoryDialog(parentShell, SWT.SAVE);
						dialog.setText(LangResources.get("download"));
						final String destinationPath = dialog.open();
						if (destinationPath != null) {
							try {
								final DirectoryDownloadWorker directoryDownloadWorker = new DirectoryDownloadWorker(remoteFileDialog, connection, currentPath, destinationPath);
								progressDialog = new DualProgressDialog(getParent(), getText(), LangResources.get("download") + " \"" + currentPath + "\"", "");
								new Thread(directoryDownloadWorker).start();
								if (progressDialog.open()) {
									directoryDownloadWorker.cancel(true);
									showMessage(LangResources.get("download"), LangResources.get("canceledByUser"));
								} else {
									// check for errors
									directoryDownloadWorker.get();
									showMessage(LangResources.get("download"), LangResources.get("actionSuccessfullyCompleted"));
								}
							} catch (final Exception e) {
								new QuestionDialog(getParent(), getText(), "Error while downloading directory: " + e.getMessage(), LangResources.get("ok")).open();
							}
						}
					} else {
						final String currentFileName = currentPath.substring(currentPath.lastIndexOf("/") + 1);
						final FileDialog dialog = new FileDialog(parentShell, SWT.SAVE);
						dialog.setText(LangResources.get("download"));
						dialog.setFileName(currentFileName);
						final String destinationPath = dialog.open();
						if (destinationPath != null) {
							try {
								final RemoteFileDownloadWorker fileDownloadWorker = new RemoteFileDownloadWorker(remoteFileDialog, connection, currentPath, destinationPath);
								progressDialog = new ProgressDialog(getParent(), getText(), LangResources.get("download"));
								new Thread(fileDownloadWorker).start();
								if (progressDialog.open()) {
									fileDownloadWorker.cancel(true);
									showMessage(LangResources.get("download"), LangResources.get("canceledByUser"));
								} else {
									// check for errors
									fileDownloadWorker.get();
									showMessage(LangResources.get("download"), LangResources.get("actionSuccessfullyCompleted"));
								}
							} catch (final Exception e) {
								new QuestionDialog(getParent(), getText(), "Error while downloading file: " + e.getMessage(), LangResources.get("ok")).open();
							}
						}
					}
				}
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

	protected void recursiveDownload(String currentPath, final String destinationPath) throws Exception {
		if (connection != null) {
			if (currentPath.endsWith("/")) {
				currentPath = currentPath.substring(0, currentPath.length() - 1);
			}
			for (final String folder : connection.getFolders(currentPath)) {
				new File(destinationPath + "/" + folder).mkdir();
				recursiveDownload(currentPath + "/" + folder, destinationPath + "/" + folder);
			}
			for (final String file : connection.getFiles(currentPath)) {
				try (InputStream inputStream = connection.getFile(currentPath + "/" + file);
						OutputStream outputStream = new FileOutputStream(destinationPath + "/" + file)) {
					Utilities.copy(inputStream, outputStream);
				}
			}
		}
	}

	private void connect() {
		try {
			if ("ssh".equalsIgnoreCase(protocol) || "sftp".equalsIgnoreCase(protocol)) {
				try {
					connection = new SftpConnection(getServerEntry());
				} catch (final UnknownHostKeyException e) {
					if (new QuestionDialog(getParent(), getText(), LangResources.get("confirmNewHostKey", e.getHostnameOrIp(), e.getHostKeyFingerPrint()), LangResources.get("yes"), LangResources.get("no")).open() == 0) {
						final ByteArrayOutputStream newKnownHosts = new ByteArrayOutputStream();
						final ServerAccessData serverEntry = getServerEntry();
						connection = new SftpConnection(serverEntry.getHostname(), serverEntry.getUsername(), FileUtils.readFileToByteArray(new File(serverEntry.getKeyFile())), serverEntry.getPassword(), serverEntry.getPort(), e.getHostKeyFingerPrint(), newKnownHosts);
						if ("".equals(hostKey.trim())) {
							hostKey = STANDARD_KNOWNHOSTS_FILE;
						}
						if (new File(Utilities.replaceUsersHome(hostKey)).isFile()) {
							try (FileOutputStream fileOutputStream = new FileOutputStream(Utilities.replaceUsersHome(hostKey), true)) {
								fileOutputStream.write(newKnownHosts.toByteArray());
							}
						} else if (!hostKey.matches("[A-Fa-f0-9:]")) {
							try (FileOutputStream fileOutputStream = new FileOutputStream(Utilities.replaceUsersHome(hostKey))) {
								fileOutputStream.write(newKnownHosts.toByteArray());
							}
						}
					} else {
						throw e;
					}
				}
			} else if ("ftp".equalsIgnoreCase(protocol)) {
				connection = new FtpConnection(getServerEntry());
			} else if ("smb".equalsIgnoreCase(protocol)) {
				connection = new SmbConnection(getServerEntry());
			} else if ("smb1".equalsIgnoreCase(protocol)) {
				connection = new Smb1Connection(getServerEntry());
			} else if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
				connection = new HttpFileConnection(getServerEntry());
			}

			final List<String> folders = connection.getFolders("/");
			final List<String> files = connection.getFiles("/");
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

	private void disconnect() {
		if (connection != null) {
			Utilities.closeQuietly(connection);
			connection = null;
		}

		folderTree.removeAll();
		filesTree.removeAll();
		checkButtonStatus();
	}

	private void checkButtonStatus() {
		if (connection != null) {
			if (keystoreFile != null) {
				serverEntriesCombo.setEnabled(false);
				loadServerEntriesButton.setEnabled(false);
				removeServerEntryButton.setEnabled(false);
				saveServerEntryButton.setEnabled(false);
			}
			connectButton.setEnabled(true);
			connectButton.setText(LangResources.get("disconnect"));
			action1Button.setEnabled(Utilities.isNotBlank(selectedFileName) && Utilities.isNotBlank(selectedFilePath));
			action2Button.setEnabled(Utilities.isNotBlank(selectedFileName) || Utilities.isNotBlank(selectedFilePath));
			protocolField.setEnabled(false);
			hostnameField.setEnabled(false);
			portField.setEnabled(false);
			usernameField.setEnabled(false);
			passwordField.setEnabled(false);
			useKeyFileButton.setEnabled(false);
			sharenameField.setEnabled(false);
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
				serverEntriesCombo.setEnabled(serverEntriesCombo.getItemCount() > 0 || (Utilities.isNotBlank(hostnameOrIp) && (Utilities.isNotBlank(username) || "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol))));
				loadServerEntriesButton.setEnabled(keystoreFile.exists());
				removeServerEntryButton.setEnabled(Utilities.isNotBlank(serverEntriesCombo.getText()));
				saveServerEntryButton.setEnabled(Utilities.isNotBlank(serverEntriesCombo.getText()) && Utilities.isNotBlank(hostnameOrIp) && (Utilities.isNotBlank(username) || "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)));
			}
			connectButton.setEnabled(Utilities.isNotBlank(hostnameOrIp) && (Utilities.isNotBlank(username) || "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)));
			connectButton.setText(LangResources.get("connect"));
			action1Button.setEnabled(false);
			action2Button.setEnabled(false);
			protocolField.setEnabled(true);
			hostnameField.setEnabled(true);
			portField.setEnabled(!"smb1".equalsIgnoreCase(protocol));
			usernameField.setEnabled(true);
			passwordField.setEnabled(true);
			useKeyFileButton.setEnabled(!"ftp".equalsIgnoreCase(protocol) && !"smb".equalsIgnoreCase(protocol) && !"smb1".equalsIgnoreCase(protocol));
			if (dialogMode == Mode.Save) {
				fileNameField.setEnabled(false);
				createFolderButton.setEnabled(false);
				deleteFolderButton.setEnabled(false);
				deleteFileButton.setEnabled(false);
			}
			checkHostKeyButton.setEnabled("ssh".equalsIgnoreCase(protocol) || "sftp".equalsIgnoreCase(protocol));
			hostKeyField.setEnabled(("ssh".equalsIgnoreCase(protocol) || "sftp".equalsIgnoreCase(protocol)) && checkHostKeyButton.getSelection());
			sharenameField.setEnabled("smb".equalsIgnoreCase(protocol) || "smb1".equalsIgnoreCase(protocol) || "http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol));
		}
	}

	public ServerAccessData getServerEntry() {
		if ("ssh".equalsIgnoreCase(protocol) || "sftp".equalsIgnoreCase(protocol)) {
			return new ServerAccessData(protocol, hostnameOrIp, port, null, username, keyFilePath, password, checkHostKey ? hostKey : null);
		} else if ("ftp".equalsIgnoreCase(protocol)) {
			return new ServerAccessData(protocol, hostnameOrIp, port, null, username, null, password, null);
		} else if ("smb".equalsIgnoreCase(protocol)) {
			return new ServerAccessData(protocol, hostnameOrIp, port, sharename, username, null, password, null);
		} else if ("smb1".equalsIgnoreCase(protocol)) {
			return new ServerAccessData(protocol, hostnameOrIp, 0, sharename, username, null, password, null);
		} else if ("http".equalsIgnoreCase(protocol) || "https".equalsIgnoreCase(protocol)) {
			return new ServerAccessData(protocol, hostnameOrIp, port, sharename, username, null, password, null);
		} else {
			throw new IllegalArgumentException("Unknown protocol: " + protocol);
		}
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
		sharenameField.setText(entry.getSharename());
	}

	private void showPathDirectory() {
		try {
			final TreeItem selectedFolderItem = folderTree.getSelection()[0];
			final String path = SwtUtilities.getAbsolutePath(selectedFolderItem);
			final List<String> folders = connection.getFolders(path);
			final List<String> files = connection.getFiles(path);
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
