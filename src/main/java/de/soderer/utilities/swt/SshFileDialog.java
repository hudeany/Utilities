package de.soderer.utilities.swt;

import java.io.File;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
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
import de.soderer.utilities.ServerAccessData;
import de.soderer.utilities.ServerEntryKeyStore;
import de.soderer.utilities.Tuple;

public class SshFileDialog extends ModalDialog<Boolean> {
	public enum Mode {
		Open,
		Save
	}
	
	private String title;
	private String serverEntryName = null;
	private String hostnameOrIp = null;
	private String username = null;
	private String keyFile = null;
	private String password = null;
	private String selectedFilePath = null;
	private String selectedFileName = null;
	private boolean connected = false;
	private Text hostnameField;
	private Text usernameField;
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
	private String inMemoryPassword;
	private File keystoreFile;
	private ServerEntryKeyStore keyStore = null;
	
	private Mode dialogMode;
	private Text fileNameField;
	private Button createFolderButton;
	
	public SshFileDialog(Mode dialogMode, Shell parentShell, String title) {
		super(parentShell);
		this.title = title;
		this.keystoreFile = null;
		this.inMemoryPassword = null;
		this.dialogMode = dialogMode;
	}
	
	public SshFileDialog(Mode dialogMode, Shell parentShell, String title, File keystoreFile, String inMemoryPassword) {
		super(parentShell);
		this.title = title;
		this.keystoreFile = keystoreFile;
		this.inMemoryPassword = inMemoryPassword;
		this.dialogMode = dialogMode;
	}

	@Override
	protected void createComponents(Shell parentShell) throws Exception {
		if (StringUtils.isEmpty(title)) {
			parentShell.setText(getText());
		} else {
			parentShell.setText(title);
		}
		
		parentShell.setLayout(new GridLayout(1, false));

		if (keystoreFile != null) {
			Composite serverEntriesComposite = new Composite(parentShell, SWT.BORDER);
			serverEntriesComposite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
			serverEntriesComposite.setLayout(new GridLayout(5, false));
			
			Label serverEntriesLabel = new Label(serverEntriesComposite, SWT.NONE);
			serverEntriesLabel.setText(LangResources.get("serverEntries"));
			
			serverEntriesCombo = new Combo(serverEntriesComposite, SWT.NONE);
			serverEntriesCombo.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		    serverEntriesCombo.setItems(new String[]{});
		    serverEntryName = serverEntriesCombo.getText();
		    serverEntriesCombo.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					checkButtonStatus();
				}
			});
		    serverEntriesCombo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					String selectionName = ((Combo) event.widget).getText();
					if (StringUtils.isNotBlank(selectionName)) {
						ServerAccessData entry = keyStore.getEntry(selectionName);
						serverEntryName = selectionName;
						hostnameField.setText(entry.getHostname());
						usernameField.setText(entry.getUsername());
						keyFile = entry.getKeyFile();
						useKeyFileButton.setSelection(StringUtils.isNotBlank(keyFile));
						passwordField.setText(entry.getPassword());
					}
				}
			});
		    
		    loadServerEntriesButton = new Button(serverEntriesComposite, SWT.PUSH);
		    loadServerEntriesButton.setText(LangResources.get("loadServerEntries"));
		    loadServerEntriesButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					loadServerEntries();
					
					checkButtonStatus();
				}
			});
		    
		    removeServerEntryButton = new Button(serverEntriesComposite, SWT.PUSH);
		    removeServerEntryButton.setText(LangResources.get("removeServerEntry"));
		    removeServerEntryButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					if (keyStore != null) {
						keyStore.remove(serverEntriesCombo.getText());
						serverEntriesCombo.setItems(keyStore.getServerEntryNames().toArray(new String[0]));
						serverEntriesCombo.clearSelection();
						serverEntriesCombo.setText("");
					}
					
					serverEntryName = null;
					hostnameField.setText("");
					usernameField.setText("");
					keyFile = "";
					useKeyFileButton.setSelection(false);
					passwordField.setText("");
					
					checkButtonStatus();
				}
			});
		    
		    saveServerEntryButton = new Button(serverEntriesComposite, SWT.PUSH);
		    saveServerEntryButton.setText(LangResources.get("saveServerEntry"));
		    saveServerEntryButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					if (keyStore == null) {
						keyStore = new ServerEntryKeyStore(keystoreFile);
					}
					String newEntryName = serverEntriesCombo.getText();
					keyStore.setEntry(serverEntriesCombo.getText(), new ServerAccessData(serverEntriesCombo.getText(), hostnameOrIp, username, keyFile, password));
					String password = new PasswordDialog(LangResources.get("enterKeystorePassword"), getShell(), LangResources.get("ok"), LangResources.get("cancel")).open();
					if (password != null) {
						keyStore.saveKeyStore(password.toCharArray());
					}
					serverEntriesCombo.setItems(keyStore.getServerEntryNames().toArray(new String[0]));
					serverEntriesCombo.setText(newEntryName);
				}
			});
		}
	    
		Composite sshCredentialsComposite = new Composite(parentShell, SWT.BORDER);
		sshCredentialsComposite.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false));
		sshCredentialsComposite.setLayout(new GridLayout(8, false));

		Label hostnameLabel = new Label(sshCredentialsComposite, SWT.NONE);
		hostnameLabel.setText(LangResources.get("hostnameOrIp"));
		hostnameField = new Text(sshCredentialsComposite, SWT.BORDER);
		GridData hostnameFieldGridData = new GridData(SWT.FILL, SWT.UP, true, true);
		hostnameFieldGridData.widthHint = 150;
		hostnameField.setLayoutData(hostnameFieldGridData);
		hostnameField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				hostnameOrIp = ((Text) event.widget).getText();
				checkButtonStatus();
			}
		});
		
		Label usernameLabel = new Label(sshCredentialsComposite, SWT.NONE);
		usernameLabel.setText(LangResources.get("username"));
		usernameField = new Text(sshCredentialsComposite, SWT.BORDER);
		GridData usernameFieldGridData = new GridData(SWT.FILL, SWT.UP, false, false);
		usernameFieldGridData.widthHint = 100;
		usernameField.setLayoutData(usernameFieldGridData);
		usernameField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				username = ((Text) event.widget).getText();
				checkButtonStatus();
			}
		});
		
		useKeyFileButton = new Button(sshCredentialsComposite, SWT.CHECK);
		useKeyFileButton.setText(LangResources.get("useKeyFile"));
		useKeyFileButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (((Button) e.widget).getSelection()) {
					FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);
			        dialog.setText(LangResources.get("openKeyFile"));
			        dialog.setFileName(keyFile);
			        String fileName = dialog.open();
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
		
		Label passwordLabel = new Label(sshCredentialsComposite, SWT.NONE);
		passwordLabel.setText(LangResources.get("password"));
		passwordField = new Text(sshCredentialsComposite, SWT.PASSWORD | SWT.BORDER);
		passwordField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent event) {
				password = ((Text) event.widget).getText();
				checkButtonStatus();
			}
		});
		
		connectButton = new Button(sshCredentialsComposite, SWT.PUSH);
		connectButton.setText(LangResources.get("connect"));
		connectButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (!connected) {
					connect();
				} else {
					disconnect();
				}
				checkButtonStatus();
			}
		});
		
		SashForm sashForm = new SashForm(parentShell, SWT.SMOOTH | SWT.HORIZONTAL | SWT.BORDER);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		
		Composite treePartComposite = new Composite(sashForm, SWT.NONE);
		treePartComposite.setLayout(new GridLayout(1, false));
		folderTree = new Tree(treePartComposite, SWT.BORDER);
		GridData folderTreeGridData = new GridData(SWT.FILL, SWT.UP, true, true);
		folderTreeGridData.heightHint = 300;
		folderTree.setLayoutData(folderTreeGridData);
		folderTree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				Session session = null;
				try {
					TreeItem selectedFolderItem = folderTree.getSelection()[0];
					String path = selectedFolderItem.getText();
					TreeItem parentTreeItem = selectedFolderItem.getParentItem();
					while (parentTreeItem != null) {
						path = parentTreeItem.getText() + (parentTreeItem.getText().endsWith("/") ? "" : "/") + path;
						parentTreeItem = parentTreeItem.getParentItem();
					}
					session = JschWrapper.openSession(getServerEntry());
					Tuple<List<String>, List<String>> directory = JschWrapper.getDirectory(session, path);
					for (TreeItem subItem : selectedFolderItem.getItems()) {
						if (!directory.getFirst().contains(subItem.getText())) {
							subItem.dispose();
						}
					}
					for (String folderName : directory.getFirst()) {
						boolean subItemExists = false;
						for (TreeItem subItem : selectedFolderItem.getItems()) {
							if (subItem.getText().equals(folderName)) {
								subItemExists = true;
								break;
							}
						}
						if (!subItemExists) {
							TreeItem folderTreeItem = new TreeItem(selectedFolderItem, SWT.NONE);
							folderTreeItem.setText(folderName);
						}
					}
					selectedFolderItem.setExpanded(true);
					filesTree.removeAll();
					for (String fileName : directory.getSecond()) {
						TreeItem filesTreeItem = new TreeItem(filesTree, SWT.NONE);
						filesTreeItem.setText(fileName);
					}
					if (directory.getSecond().size() > 0) {
						filesTree.setSelection(filesTree.getItem(0));
					}
					selectedFilePath = path;
				} catch (Exception e) {
					new QuestionDialog(title, "Error while connecting to server: " + e.getMessage(), getShell(), LangResources.get("ok")).open();
				} finally {
					JschWrapper.closeQuietly(session);
				}
				checkButtonStatus();
			}
		});
		
		Composite detailPartComposite = new Composite(sashForm, SWT.NONE);
		detailPartComposite.setLayout(new GridLayout(1, false));
		filesTree = new Tree(detailPartComposite, SWT.BORDER);
		GridData filesTreeGridData = new GridData(SWT.FILL, SWT.UP, true, true);
		filesTreeGridData.heightHint = 300;
		filesTree.setLayoutData(filesTreeGridData);
		filesTree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				try {
					TreeItem selectedFilesItem = filesTree.getSelection()[0];
					selectedFileName = selectedFilesItem.getText();
					if (dialogMode == Mode.Save) {
						fileNameField.setText(selectedFileName);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				checkButtonStatus();
			}
		});
		filesTree.addMouseListener(new MouseListener() {
			@Override
			public void mouseUp(MouseEvent e) {
			}
			
			@Override
			public void mouseDown(MouseEvent e) {
			}
			
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if (((Tree) e.widget).getSelection().length == 1) {
					setReturnValue(true);
					getShell().close();
				}
			}
		});
		
		if (dialogMode == Mode.Save) {
			Composite additionalSaveComposite = new Composite(parentShell, SWT.NONE);
			additionalSaveComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
			additionalSaveComposite.setLayout(new GridLayout(3, false));
			
			createFolderButton = new Button(additionalSaveComposite, SWT.PUSH);
			createFolderButton.setText(LangResources.get("createFolder"));
			createFolderButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent event) {
					String folderName = new SimpleInputDialog(LangResources.get("enterFolderName"), getShell(), LangResources.get("ok"), LangResources.get("cancel")).open();
					if (StringUtils.isNotBlank(folderName)) {
						Session session = null;
						try {
							JschWrapper.createFolder(session, selectedFilePath, folderName);
						} catch (Exception e) {
							new QuestionDialog(title, "Error while connecting to server: " + e.getMessage(), getShell(), LangResources.get("ok")).open();
						} finally {
							JschWrapper.closeQuietly(session);
						}
					}
				}
			});
	
			Label fileNameLabel = new Label(additionalSaveComposite, SWT.NONE);
			fileNameLabel.setText(LangResources.get("fileName"));
			fileNameField = new Text(additionalSaveComposite, SWT.BORDER);
			fileNameField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
			fileNameField.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent event) {
					selectedFileName = ((Text) event.widget).getText();
					checkButtonStatus();
				}
			});
		}
				
		Composite mainButtonComposite = new Composite(parentShell, SWT.NONE);
		mainButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		mainButtonComposite.setLayout(new FillLayout());

		actionButton = new Button(mainButtonComposite, SWT.PUSH);
		actionButton.setText(LangResources.get(dialogMode == Mode.Open ? "open" : "save"));
		actionButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent event) {
				setReturnValue(true);
				getShell().close();
			}
		});
		
	    Button cancelButton = new Button(mainButtonComposite, SWT.PUSH);
	    cancelButton.setText(LangResources.get("cancel"));
	    cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(false);
				getShell().close();
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
			Tuple<List<String>, List<String>> directory = JschWrapper.getDirectory(session, "/");
			folderTree.removeAll();
			TreeItem rootTreeItem = new TreeItem(folderTree, SWT.NONE);
			rootTreeItem.setText("/");
			for (String folderName : directory.getFirst()) {
				TreeItem folderTreeItem = new TreeItem(rootTreeItem, SWT.NONE);
				folderTreeItem.setText(folderName);
			}
			for (String fileName : directory.getSecond()) {
				TreeItem filesTreeItem = new TreeItem(filesTree, SWT.NONE);
				filesTreeItem.setText(fileName);
			}
			rootTreeItem.setExpanded(true);
			folderTree.select(folderTree.getItem(0));
			selectedFilePath = "/";

			connected = true;
		} catch (Exception e) {
			connected = false;
			new QuestionDialog(title, "Error while connecting to server: " + e.getMessage(), getShell(), LangResources.get("ok")).open();
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
			actionButton.setEnabled(StringUtils.isNotBlank(selectedFileName) && StringUtils.isNotBlank(selectedFilePath));
			hostnameField.setEnabled(false);
			usernameField.setEnabled(false);
			passwordField.setEnabled(false);
			useKeyFileButton.setEnabled(false);
			if (dialogMode == Mode.Save) {
				fileNameField.setEnabled(true);
				createFolderButton.setEnabled(true);
			}
		} else {
			if (keystoreFile != null) {
				serverEntriesCombo.setEnabled(serverEntriesCombo.getItemCount() > 0 || (StringUtils.isNotBlank(hostnameOrIp) && StringUtils.isNotBlank(username)));
				loadServerEntriesButton.setEnabled(keystoreFile.exists());
				removeServerEntryButton.setEnabled(StringUtils.isNotBlank(serverEntriesCombo.getText()));
				saveServerEntryButton.setEnabled(StringUtils.isNotBlank(serverEntriesCombo.getText()) && StringUtils.isNotBlank(hostnameOrIp) && StringUtils.isNotBlank(username));
			}
			connectButton.setEnabled(StringUtils.isNotBlank(hostnameOrIp) && StringUtils.isNotBlank(username));
			connectButton.setText(LangResources.get("connect"));
			actionButton.setEnabled(false);
			hostnameField.setEnabled(true);
			usernameField.setEnabled(true);
			passwordField.setEnabled(true);
			useKeyFileButton.setEnabled(true);
			if (dialogMode == Mode.Save) {
				fileNameField.setEnabled(false);
				createFolderButton.setEnabled(false);
			}
		}
	}

	public ServerAccessData getServerEntry() {
		return new ServerAccessData(serverEntryName, hostnameOrIp, username, keyFile, password);
	}

	public String getSelectedFile() {
		return selectedFilePath + (selectedFilePath.endsWith("/") ? "" : "/") + selectedFileName;
	}

	public String getInMemoryPassword() {
		return inMemoryPassword;
	}
	
	private void loadServerEntries() {
		if (keystoreFile.exists()) {
			String password = inMemoryPassword;
			
			if (password == null) {
				// try to use empty password
				try {
					password = "";
					keyStore = new ServerEntryKeyStore(keystoreFile);
					keyStore.loadKeyStore(password.toCharArray());
				} catch (Exception e1) {
					password = null;
					keyStore = null;
				}
			}
			
			if (password == null && keyStore == null) {
				PasswordDialog dialog = new PasswordDialog(LangResources.get("enterKeystorePassword"), getShell(), LangResources.get("ok"), LangResources.get("cancel"), LangResources.get("keepPasswordInMemory"));
				password = dialog.open();
				if (dialog.keepInMemory()) {
					inMemoryPassword = password;
				}
			}
			
			if (password != null && keyStore == null) {
				try {
					keyStore = new ServerEntryKeyStore(keystoreFile);
					keyStore.loadKeyStore(password.toCharArray());
				} catch (Exception e) {
					new QuestionDialog(title, LangResources.get("cannotLoadKeystore"), getShell(), LangResources.get("ok")).open();
					keyStore = null;
				}
			}
		} else {
			keyStore = null;
		}
		
		if (keyStore != null) {
			serverEntriesCombo.setItems(keyStore.getServerEntryNames().toArray(new String[0]));
			if (serverEntriesCombo.getItemCount() > 0) {
				serverEntriesCombo.select(0);
				String selectionName = serverEntriesCombo.getText();
				if (StringUtils.isNotBlank(selectionName)) {
					ServerAccessData entry = keyStore.getEntry(selectionName);
					hostnameField.setText(entry.getHostname());
					usernameField.setText(entry.getUsername());
					keyFile = entry.getKeyFile();
					useKeyFileButton.setSelection(StringUtils.isNotBlank(keyFile));
					passwordField.setText(entry.getPassword());
				}
			}
		} else {
			serverEntriesCombo.setItems(new String[] {});
		}
	    serverEntryName = serverEntriesCombo.getText();
	}
}
