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

import com.jcraft.jsch.Session;

import de.soderer.utilities.JschWrapper;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.SecureDataStore;
import de.soderer.utilities.ServerAccessData;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.swt.LabeledText.ArrangementStyle;

/**
 * TODO: use PPK key files
 */
public class SshFileDialog extends ModalDialog<Boolean> {
	public enum Mode {
		Open, Save
	}

	private String hostnameOrIp = null;
	private int port = 22;
	private String username = null;
	private String keyFile = null;
	private char[] password = null;
	private String selectedFilePath = null;
	private String selectedFileName = null;
	private boolean connected = false;
	private LabeledText hostnameField;
	private LabeledText portField;
	private LabeledText usernameField;
	private Button useKeyFileButton;
	private Text passwordField;
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

	public SshFileDialog(final Shell parentShell, final String title, final Mode dialogMode) {
		super(parentShell, title);
		keystoreFile = null;
		keystoreFileSalt = null;
		inMemoryPassword = null;
		this.dialogMode = dialogMode;
	}

	public SshFileDialog(final Shell parentShell, final String title, final Mode dialogMode, final File keystoreFile, final byte[] keystoreFileSalt, final char[] inMemoryPassword) {
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
						final ServerAccessData entry = keyStore.getEntry(ServerAccessData.class, serverEntriesCombo.getText());
						hostnameField.setText(entry.getHostname());
						portField.setText(Integer.toString(entry.getPort()));
						usernameField.setText(entry.getUsername());
						keyFile = entry.getKeyFile();
						useKeyFileButton.setSelection(Utilities.isNotBlank(keyFile));
						passwordField.setText(new String(entry.getPassword()));
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

		final Composite sshCredentialsComposite = new Composite(parentShell, SWT.BORDER);
		sshCredentialsComposite.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false));
		sshCredentialsComposite.setLayout(SwtUtilities.createNoMarginGridLayout(9, false));

		hostnameField = new LabeledText(sshCredentialsComposite, SWT.NONE, LangResources.get("hostnameOrIp"), ArrangementStyle.Horizontal);
		final GridData hostnameFieldGridData = new GridData(SWT.FILL, SWT.UP, true, true);
		hostnameFieldGridData.widthHint = 225;
		hostnameField.setLayoutData(hostnameFieldGridData);
		hostnameField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				hostnameOrIp = ((LabeledText) event.widget).getText();
				checkButtonStatus();
			}
		});

		portField = new LabeledText(sshCredentialsComposite, SWT.NONE, LangResources.get("port"), ArrangementStyle.Horizontal);
		portField.setText(Integer.toString(port));
		final GridData portFieldGridData = new GridData(SWT.FILL, SWT.UP, true, true);
		portFieldGridData.widthHint = 60;
		portField.setLayoutData(portFieldGridData);
		portField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				try {
					port = Integer.parseInt(((LabeledText) event.widget).getText());
				} catch (final Exception e) {
					port = 22;
					((Text) event.widget).setText("22");
				}
				checkButtonStatus();
			}
		});

		usernameField = new LabeledText(sshCredentialsComposite, SWT.NONE, LangResources.get("username"), ArrangementStyle.Horizontal);
		final GridData usernameFieldGridData = new GridData(SWT.FILL, SWT.UP, false, false);
		usernameFieldGridData.widthHint = 125;
		usernameField.setLayoutData(usernameFieldGridData);
		usernameField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				username = ((LabeledText) event.widget).getText();
				checkButtonStatus();
			}
		});

		useKeyFileButton = new Button(sshCredentialsComposite, SWT.CHECK);
		useKeyFileButton.setText(LangResources.get("useKeyFile"));
		useKeyFileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					final FileDialog dialog = new FileDialog(getParent(), SWT.OPEN);
					dialog.setText(LangResources.get("openKeyFile"));
					dialog.setFileName(keyFile);
					final String fileName = dialog.open();
					if (fileName != null) {
						keyFile = fileName;
					} else {
						keyFile = null;
					}
				} else {
					keyFile = null;
				}
				checkButtonStatus();
			}
		});

		final Label passwordLabel = new Label(sshCredentialsComposite, SWT.NONE);
		passwordLabel.setText(LangResources.get("password"));
		passwordField = new Text(sshCredentialsComposite, SWT.PASSWORD | SWT.BORDER);
		passwordField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				password = ((Text) event.widget).getText().toCharArray();
				checkButtonStatus();
			}
		});

		connectButton = new Button(sshCredentialsComposite, SWT.PUSH);
		connectButton.setText(LangResources.get("connect"));
		connectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				if (!connected) {
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
						Session session = null;
						try {
							session = JschWrapper.openSession(getServerEntry());
							JschWrapper.createFolder(session, selectedFilePath, folderName);
						} catch (final Exception e) {
							new QuestionDialog(getParent(), getText(), "Error while connecting to server: " + e.getMessage(), LangResources.get("ok")).open();
						} finally {
							JschWrapper.closeQuietly(session);
						}
						showPathDirectory();
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
							Session session = null;
							try {
								session = JschWrapper.openSession(getServerEntry());
								JschWrapper.deleteFolder(session, selectedFilePath);
								folderTree.select(folderTree.getSelection()[0].getParentItem());
							} catch (final Exception e) {
								new QuestionDialog(getParent(), getText(), "Error while connecting to server: " + e.getMessage(), LangResources.get("ok")).open();
							} finally {
								JschWrapper.closeQuietly(session);
							}
							showPathDirectory();
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
						if (new QuestionDialog(getParent(), LangResources.get("deleteFile"), LangResources.get("reallyDeleteFile", selectedFilePath), LangResources.get("ok"), LangResources.get("cancel")).open() == 0) {
							Session session = null;
							try {
								session = JschWrapper.openSession(getServerEntry());
								JschWrapper.deleteFile(session, selectedFilePath + "/" + selectedFileName);
							} catch (final Exception e) {
								new QuestionDialog(getParent(), getText(), "Error while connecting to server: " + e.getMessage(), LangResources.get("ok")).open();
							} finally {
								JschWrapper.closeQuietly(session);
							}
							showPathDirectory();
						}
					}
				}
			});

			final Composite additionalSaveComposite = new Composite(parentShell, SWT.BORDER);
			additionalSaveComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			additionalSaveComposite.setLayout(SwtUtilities.createNoMarginGridLayout(1, false));

			fileNameField = new LabeledText(additionalSaveComposite, SWT.NONE, LangResources.get("fileName"), ArrangementStyle.Horizontal);
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
				setReturnValue(false);
				getParent().close();
			}
		});

		loadServerEntries();

		parentShell.pack();

		checkButtonStatus();
	}

	private void connect() {
		Session session = null;
		try {
			session = JschWrapper.openSession(getServerEntry());
			final List<String> folders = JschWrapper.getFolders(session, "/");
			final List<String> files = JschWrapper.getFiles(session, "/");
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

			connected = true;
		} catch (final Exception e) {
			connected = false;
			new QuestionDialog(getParent(), getText(), "Error while connecting to server: " + e.getMessage(), LangResources.get("ok")).open();
		} finally {
			JschWrapper.closeQuietly(session);
		}

		checkButtonStatus();
	}

	private void disconnect() {
		connected = false;
		folderTree.removeAll();
		selectedFilePath = null;
		filesTree.removeAll();
		selectedFileName = null;
	}

	private void checkButtonStatus() {
		if (connected) {
			if (keystoreFile != null) {
				serverEntriesCombo.setEnabled(false);
				loadServerEntriesButton.setEnabled(false);
				removeServerEntryButton.setEnabled(false);
				saveServerEntryButton.setEnabled(false);
			}
			connectButton.setEnabled(true);
			connectButton.setText(LangResources.get("disconnect"));
			actionButton.setEnabled(Utilities.isNotBlank(selectedFileName) && Utilities.isNotBlank(selectedFilePath));
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
		}
	}

	public ServerAccessData getServerEntry() {
		return new ServerAccessData("ssh", hostnameOrIp, port, username, keyFile, password, false);
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
					if ("ssh".equalsIgnoreCase(entry.getProtocol())) {
						hostnameField.setText(entry.getHostname());
						portField.setText(Integer.toString(entry.getPort()));
						usernameField.setText(entry.getUsername());
						keyFile = entry.getKeyFile();
						useKeyFileButton.setSelection(Utilities.isNotBlank(keyFile));
						passwordField.setText(new String(entry.getPassword()));
					}
				}
			}
		} else {
			serverEntriesCombo.setItems(new String[] {});
		}
	}

	private void showPathDirectory() {
		Session session = null;
		try {
			final TreeItem selectedFolderItem = folderTree.getSelection()[0];
			String path = selectedFolderItem.getText();
			TreeItem parentTreeItem = selectedFolderItem.getParentItem();
			while (parentTreeItem != null) {
				path = parentTreeItem.getText() + (parentTreeItem.getText().endsWith("/") ? "" : "/") + path;
				parentTreeItem = parentTreeItem.getParentItem();
			}
			session = JschWrapper.openSession(getServerEntry());
			final List<String> folders = JschWrapper.getFolders(session, path);
			final List<String> files = JschWrapper.getFiles(session, path);
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
		} finally {
			JschWrapper.closeQuietly(session);
		}
	}
}
