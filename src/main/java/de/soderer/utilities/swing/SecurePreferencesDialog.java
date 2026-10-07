package de.soderer.utilities.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import de.soderer.utilities.Credentials;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.SecureDataStore;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.WrongPasswordException;

/**
 * Modal dialog to manage named, password protected preference entries (e.g.
 * connection settings) stored in a {@link SecureDataStore} file.
 *
 * <p>
 * The caller sets the current data entry via
 * {@link #setCurrentDataEntry(Object)} before opening the dialog; its class
 * determines which entries are listed. The user can load an entry (afterwards
 * available via {@link #getCurrentDataEntry()}), store the current entry under a
 * new or an existing name, or delete entries. After storing or deleting, the
 * current data entry is null.
 * </p>
 */
public class SecurePreferencesDialog extends ModalDialog<Boolean> {
	private static final long serialVersionUID = -2855354859482098916L;

	/** File the preference entries are loaded from and saved to */
	private final File preferencesFile;

	/** Decrypted data store with all preference entries, null until loaded or first created */
	private SecureDataStore secureDataStore = null;

	/** Data entry to store, or the loaded entry after loading; its class determines the listed entries */
	private Object currentDataEntry;

	/** Text of the password dialog */
	private final String text_PasswordText;

	/** Label of the password field of the password dialog */
	private final String text_Password;

	/** Text of the OK button of the password dialog */
	private final String text_OK;

	/** Text of the cancel button of the password dialog */
	private final String text_Cancel;

	/** Button loading the selected entry */
	private final JButton loadButton;

	/** Button storing the current data entry under the name entered in newNameField */
	private final JButton createButton;

	/** Button overwriting the selected entry with the current data entry */
	private final JButton updateButton;

	/** Button deleting the selected entries */
	private final JButton deleteButton;

	/**
	 * Password of the preferences file, entered by the user or set via
	 * {@link #setPassword(char[])}; null if not known yet (transient: never serialized).
	 */
	private transient char[] latestPassword = null;

	/** Input field for the name of a new entry */
	private final JTextField newNameField;

	/** List of the names of all stored entries of the current data entry's class */
	private final JTable preferencesTable;

	/**
	 * Creates a secure preferences dialog.
	 *
	 * @param parent owner window, may be null
	 * @param title title of the dialog
	 * @param text tooltip text of the name field and the entry list
	 * @param securePreferencesFile file of the secure data store
	 * @param textButton_Load text of the load button
	 * @param textButton_Create text of the create button
	 * @param textButton_Update text of the update button
	 * @param textButton_Delete text of the delete button
	 * @param textButton_Save text of the save button
	 * @param textButton_Cancel text of the cancel button
	 * @param text_PasswordText text of the password dialog
	 * @param text_Password label of the password field
	 * @param text_OK text of the OK button of the password dialog
	 * @param text_Cancel text of the cancel button of the password dialog
	 */
	public SecurePreferencesDialog(final Window parent, final String title, final String text, final File securePreferencesFile,
			final String textButton_Load, final String textButton_Create, final String textButton_Update, final String textButton_Delete, final String textButton_Save, final String textButton_Cancel, final String text_PasswordText,
			final String text_Password, final String text_OK, final String text_Cancel) {
		super(parent, title);

		this.text_PasswordText = text_PasswordText;
		this.text_Password = text_Password;
		this.text_OK = text_OK;
		this.text_Cancel = text_Cancel;

		preferencesFile = securePreferencesFile;

		setResizable(false);

		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		panel.add(Box.createRigidArea(new Dimension(0, 5)));

		final JPanel interimPanel = new JPanel();
		interimPanel.setLayout(new BoxLayout(interimPanel, BoxLayout.LINE_AXIS));

		interimPanel.add(Box.createRigidArea(new Dimension(5, 0)));

		final JPanel comboPanel = new JPanel();
		comboPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		comboPanel.setLayout(new BoxLayout(comboPanel, BoxLayout.PAGE_AXIS));

		newNameField = new JTextField();
		newNameField.setToolTipText(text);
		newNameField.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(final DocumentEvent event) {
				createButton.setEnabled(newNameField.getText().length() > 0);
			}

			@Override
			public void removeUpdate(final DocumentEvent event) {
				createButton.setEnabled(newNameField.getText().length() > 0);
			}

			@Override
			public void changedUpdate(final DocumentEvent event) {
				createButton.setEnabled(newNameField.getText().length() > 0);
			}
		});
		comboPanel.add(newNameField);

		preferencesTable = new JTable(new DefaultTableModel(0, 1));
		preferencesTable.setBorder(BorderFactory.createLineBorder(Color.GRAY));
		preferencesTable.setToolTipText(text);
		preferencesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(final ListSelectionEvent event) {
				loadButton.setEnabled(preferencesTable.getSelectedRows().length == 1);
				updateButton.setEnabled(preferencesTable.getSelectedRows().length == 1);
				deleteButton.setEnabled(preferencesTable.getSelectedRows().length > 0);
			}
		});
		preferencesTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(final MouseEvent event) {
				if (event.getClickCount() >= 2) {
					final int rowIndex = preferencesTable.rowAtPoint(event.getPoint());
					if (rowIndex >= 0) {
						final DefaultTableModel tableModel = (DefaultTableModel) preferencesTable.getModel();
						final String entryName = (String) tableModel.getValueAt(rowIndex, 0);
						currentDataEntry = secureDataStore.getEntry(currentDataEntry.getClass(), entryName);
						dispose();
					}
				}
			}
		});
		comboPanel.add(preferencesTable);

		interimPanel.add(comboPanel);

		interimPanel.add(Box.createRigidArea(new Dimension(5, 0)));

		panel.add(interimPanel);

		panel.add(Box.createRigidArea(new Dimension(0, 5)));

		// Button Panel
		final JPanel buttonPanel1 = new JPanel();
		buttonPanel1.setLayout(new BoxLayout(buttonPanel1, BoxLayout.LINE_AXIS));

		buttonPanel1.add(Box.createRigidArea(new Dimension(5, 0)));

		loadButton = new JButton(textButton_Load);
		loadButton.setEnabled(false);
		loadButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				if (preferencesTable.getSelectedRows().length == 1) {
					final DefaultTableModel tableModel = (DefaultTableModel) preferencesTable.getModel();
					final String entryName = (String) tableModel.getValueAt(preferencesTable.getSelectedRows()[0], 0);
					currentDataEntry = secureDataStore.getEntry(currentDataEntry.getClass(), entryName);
					dispose();
				}
			}
		});
		buttonPanel1.add(loadButton);

		buttonPanel1.add(Box.createRigidArea(new Dimension(5, 0)));

		createButton = new JButton(textButton_Create);
		createButton.setEnabled(false);
		createButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				if (secureDataStore == null) {
					secureDataStore = new SecureDataStore();
				}

				if (Utilities.isNotBlank(newNameField.getText())) {
					secureDataStore.addEntry(newNameField.getText(), currentDataEntry);
					if (getPassword() == null) {
						final CredentialsDialog credentialsDialog = new CredentialsDialog((Window) getParent(), getTitle(), text_PasswordText, false, true, null, text_Password, text_OK, text_Cancel);
						final Credentials credentials = credentialsDialog.open();
						if (credentials != null) {
							setPassword(credentials.getPassword());
						} else {
							setPassword(null);
						}
					}
					if (getPassword() != null) {
						try {
							secureDataStore.save(securePreferencesFile, getPassword());
						} catch (final Exception e) {
							e.printStackTrace();
							new QuestionDialog((Window) getParent(), getTitle(), LangResources.get("cannotSaveKeystore"), LangResources.get("ok")).setBackgroundColor(SwingColor.LightRed).open();
						}
						currentDataEntry = null;
						dispose();
					}
				}
			}
		});
		buttonPanel1.add(createButton);

		buttonPanel1.add(Box.createRigidArea(new Dimension(5, 0)));

		updateButton = new JButton(textButton_Update);
		updateButton.setEnabled(false);
		updateButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				if (preferencesTable.getSelectedRows().length == 1) {
					final DefaultTableModel tableModel = (DefaultTableModel) preferencesTable.getModel();
					for (final int rowNum : preferencesTable.getSelectedRows()) {
						final String entryName = (String) tableModel.getValueAt(rowNum, 0);
						secureDataStore.addEntry(entryName, currentDataEntry);
						if (getPassword() == null) {
							final CredentialsDialog credentialsDialog = new CredentialsDialog((Window) getParent(), getTitle(), text_PasswordText, false, true, null, text_Password, text_OK, text_Cancel);
							final Credentials credentials = credentialsDialog.open();
							if (credentials != null) {
								setPassword(credentials.getPassword());
							} else {
								setPassword(null);
							}
						}
						if (getPassword() != null) {
							try {
								secureDataStore.save(securePreferencesFile, getPassword());
							} catch (final Exception e) {
								e.printStackTrace();
								new QuestionDialog((Window) getParent(), getTitle(), LangResources.get("cannotSaveKeystore"), LangResources.get("ok")).setBackgroundColor(SwingColor.LightRed).open();
							}
							currentDataEntry = null;
							dispose();
						}
					}
				}
			}
		});
		buttonPanel1.add(updateButton);

		buttonPanel1.add(Box.createRigidArea(new Dimension(5, 0)));

		deleteButton = new JButton(textButton_Delete);
		deleteButton.setEnabled(false);
		deleteButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				if (preferencesTable.getSelectedRows().length > 0) {
					final DefaultTableModel tableModel = (DefaultTableModel) preferencesTable.getModel();
					for (int i = preferencesTable.getSelectedRows().length - 1; i >= 0; i--) {
						final int rowNum = preferencesTable.getSelectedRows()[i];
						final String entryName = (String) tableModel.getValueAt(rowNum, 0);
						secureDataStore.removeEntry(currentDataEntry.getClass(), entryName);
						tableModel.removeRow(rowNum);
					}
					if (getPassword() == null) {
						final CredentialsDialog credentialsDialog = new CredentialsDialog((Window) getParent(), getTitle(), text_PasswordText, false, true, null, text_Password, text_OK, text_Cancel);
						final Credentials credentials = credentialsDialog.open();
						if (credentials != null) {
							setPassword(credentials.getPassword());
						} else {
							setPassword(null);
						}
					}
					if (getPassword() != null) {
						try {
							secureDataStore.save(securePreferencesFile, getPassword());
						} catch (final Exception e) {
							e.printStackTrace();
							new QuestionDialog((Window) getParent(), getTitle(), LangResources.get("cannotSaveKeystore"), LangResources.get("ok")).setBackgroundColor(SwingColor.LightRed).open();
						}
						currentDataEntry = null;
						dispose();
					}
				}
			}
		});
		buttonPanel1.add(deleteButton);

		buttonPanel1.add(Box.createRigidArea(new Dimension(5, 0)));

		final JPanel buttonPanel2 = new JPanel();
		buttonPanel2.setLayout(new BoxLayout(buttonPanel2, BoxLayout.LINE_AXIS));

		buttonPanel2.add(Box.createRigidArea(new Dimension(5, 0)));

		final JButton saveButton = new JButton(textButton_Save);
		saveButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				if (secureDataStore == null) {
					secureDataStore = new SecureDataStore();
				}

				if (Utilities.isNotBlank(newNameField.getText())) {
					secureDataStore.addEntry(newNameField.getText(), currentDataEntry);
				} else if (preferencesTable.getSelectedRows().length == 1) {
					// Only update the entry here, it is saved below (saving here too asked for the password twice and wrote the file twice)
					final DefaultTableModel tableModel = (DefaultTableModel) preferencesTable.getModel();
					final String entryName = (String) tableModel.getValueAt(preferencesTable.getSelectedRows()[0], 0);
					secureDataStore.addEntry(entryName, currentDataEntry);
				}

				final CredentialsDialog credentialsDialog = new CredentialsDialog((Window) getParent(), getTitle(), text_PasswordText, false, true, null, text_Password, text_OK, text_Cancel);
				final Credentials credentials = credentialsDialog.open();
				if (credentials != null) {
					setPassword(credentials.getPassword());
					try {
						secureDataStore.save(securePreferencesFile, getPassword());
					} catch (final Exception e) {
						e.printStackTrace();
						new QuestionDialog((Window) getParent(), getTitle(), LangResources.get("cannotSaveKeystore"), LangResources.get("ok")).setBackgroundColor(SwingColor.LightRed).open();
					}
					currentDataEntry = null;
					dispose();
				}
			}
		});
		buttonPanel2.add(saveButton);

		buttonPanel2.add(Box.createRigidArea(new Dimension(5, 0)));

		final JButton cancelButton = new JButton(textButton_Cancel);
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				currentDataEntry = null;
				dispose();
			}
		});
		buttonPanel2.add(cancelButton);

		buttonPanel2.add(Box.createRigidArea(new Dimension(5, 0)));

		panel.add(buttonPanel1);

		panel.add(Box.createRigidArea(new Dimension(0, 5)));

		panel.add(buttonPanel2);

		panel.add(Box.createRigidArea(new Dimension(0, 5)));

		add(panel);

		pack();

		setLocationRelativeTo(parent);

		newNameField.requestFocus();
	}

	/**
	 * Sets the data entry to store. Must be set before {@link #open()}, because
	 * its class determines which entries are listed.
	 *
	 * @param dataEntry data entry, not null
	 */
	public void setCurrentDataEntry(final Object dataEntry) {
		currentDataEntry = dataEntry;
	}

	/**
	 * Returns the current data entry: the loaded entry after loading, null after
	 * storing, deleting or canceling.
	 *
	 * @return current data entry, may be null
	 */
	public Object getCurrentDataEntry() {
		return currentDataEntry;
	}

	/**
	 * Returns the password of the preferences file.
	 *
	 * @return copy of the password, null if it is not known yet
	 */
	public char[] getPassword() {
		if (latestPassword == null) {
			return null;
		} else {
			return Arrays.copyOf(latestPassword, latestPassword.length);
		}
	}

	/**
	 * Sets the password of the preferences file, so the user is not asked for it
	 * when the file is opened.
	 *
	 * @param password password, null if unknown
	 */
	public void setPassword(final char[] password) {
		latestPassword = password;
	}

	@Override
	public Boolean open() {
		try {
			if (preferencesFile.exists()) {
				boolean retry = true;
				while (secureDataStore == null && retry) {
					try {
						secureDataStore = new SecureDataStore();
						secureDataStore.load(preferencesFile, getPassword());
					} catch (@SuppressWarnings("unused") final WrongPasswordException e) {
						secureDataStore = null;
						final CredentialsDialog credentialsDialog = new CredentialsDialog((Window) getParent(), getTitle(), text_PasswordText, false, true, null, text_Password, text_OK, text_Cancel);
						final Credentials credentials = credentialsDialog.open();
						if (credentials != null) {
							setPassword(credentials.getPassword());
						} else {
							setPassword(null);
							retry = false;
						}
					}
				}

				if (secureDataStore != null) {
					final DefaultTableModel tableModel = (DefaultTableModel) preferencesTable.getModel();
					for (final String entryName : secureDataStore.getEntryNames(currentDataEntry.getClass())) {
						tableModel.addRow(new Object[] { entryName });
					}

					pack();

					setVisible(true);
				}
			} else {
				setVisible(true);
			}
		} catch (final Exception e) {
			setPassword(null);
			new QuestionDialog((Window) getParent(), "ERROR", "ERROR:\n" + e.getMessage()).setBackgroundColor(SwingColor.LightRed).open();
		}
		return true;
	}
}
