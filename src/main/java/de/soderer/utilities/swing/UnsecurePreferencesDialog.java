package de.soderer.utilities.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;

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

import de.soderer.utilities.DefaultLangResources;
import de.soderer.utilities.UnsecureDataStore;
import de.soderer.utilities.Utilities;

public class UnsecurePreferencesDialog extends ModalDialog<Boolean> {
	private static final long serialVersionUID = -2855354859482098916L;

	private final File preferencesFile;
	private UnsecureDataStore unsecureDataStore = null;
	private Object currentDataEntry;

	private final JButton loadButton;
	private final JButton createButton;
	private final JButton updateButton;
	private final JButton deleteButton;

	private final JTextField newNameField;
	private final JTable preferencesTable;

	public UnsecurePreferencesDialog(final Window parent, final String title, final String text, final File unsecurePreferencesFile,
			final String textButton_Load, final String textButton_Create, final String textButton_Update, final String textButton_Delete,
			final String textButton_Save, final String textButton_Cancel) {
		super(parent, title);

		preferencesFile = unsecurePreferencesFile;

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
						currentDataEntry = unsecureDataStore.getEntry(currentDataEntry.getClass(), entryName);
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
					currentDataEntry = unsecureDataStore.getEntry(currentDataEntry.getClass(), entryName);
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
				if (unsecureDataStore == null) {
					unsecureDataStore = new UnsecureDataStore();
				}

				if (Utilities.isNotBlank(newNameField.getText())) {
					unsecureDataStore.addEntry(newNameField.getText(), currentDataEntry);
					try {
						unsecureDataStore.save(preferencesFile);
					} catch (final Exception e) {
						e.printStackTrace();
						new QuestionDialog((Window) getParent(), getTitle(), DefaultLangResources.getI18NStringOrDefault("cannotSaveKeystore"), DefaultLangResources.getI18NStringOrDefault("ok")).setBackgroundColor(SwingColor.LightRed).open();
					}
					currentDataEntry = null;
					dispose();
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
						unsecureDataStore.addEntry(entryName, currentDataEntry);
						try {
							unsecureDataStore.save(preferencesFile);
						} catch (final Exception e) {
							e.printStackTrace();
							new QuestionDialog((Window) getParent(), getTitle(), DefaultLangResources.getI18NStringOrDefault("cannotSaveKeystore"), DefaultLangResources.getI18NStringOrDefault("ok")).setBackgroundColor(SwingColor.LightRed).open();
						}
						currentDataEntry = null;
						dispose();
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
						unsecureDataStore.removeEntry(currentDataEntry.getClass(), entryName);
						tableModel.removeRow(rowNum);
					}
					try {
						unsecureDataStore.save(preferencesFile);
					} catch (final Exception e) {
						e.printStackTrace();
						new QuestionDialog((Window) getParent(), getTitle(), DefaultLangResources.getI18NStringOrDefault("cannotSaveKeystore"), DefaultLangResources.getI18NStringOrDefault("ok")).setBackgroundColor(SwingColor.LightRed).open();
					}
					currentDataEntry = null;
					dispose();
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
				if (unsecureDataStore == null) {
					unsecureDataStore = new UnsecureDataStore();
				}

				if (Utilities.isNotBlank(newNameField.getText())) {
					unsecureDataStore.addEntry(newNameField.getText(), currentDataEntry);
				} else if (preferencesTable.getSelectedRows().length == 1) {
					final DefaultTableModel tableModel = (DefaultTableModel) preferencesTable.getModel();
					for (final int rowNum : preferencesTable.getSelectedRows()) {
						final String entryName = (String) tableModel.getValueAt(rowNum, 0);
						unsecureDataStore.addEntry(entryName, currentDataEntry);
						try {
							unsecureDataStore.save(preferencesFile);
						} catch (final Exception e) {
							e.printStackTrace();
							new QuestionDialog((Window) getParent(), getTitle(), DefaultLangResources.getI18NStringOrDefault("cannotSaveKeystore"), DefaultLangResources.getI18NStringOrDefault("ok")).setBackgroundColor(SwingColor.LightRed).open();
						}
						currentDataEntry = null;
						dispose();
					}
				}

				try {
					unsecureDataStore.save(preferencesFile);
				} catch (final Exception e) {
					e.printStackTrace();
					new QuestionDialog((Window) getParent(), getTitle(), DefaultLangResources.getI18NStringOrDefault("cannotSaveKeystore"), DefaultLangResources.getI18NStringOrDefault("ok")).setBackgroundColor(SwingColor.LightRed).open();
				}
				currentDataEntry = null;
				dispose();
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

	public void setCurrentDataEntry(final Object dataEntry) {
		currentDataEntry = dataEntry;
	}

	public Object getCurrentDataEntry() {
		return currentDataEntry;
	}

	@Override
	public Boolean open() {
		try {
			if (preferencesFile.exists()) {
				unsecureDataStore = new UnsecureDataStore();
				unsecureDataStore.load(preferencesFile);

				if (unsecureDataStore != null) {
					final DefaultTableModel tableModel = (DefaultTableModel) preferencesTable.getModel();
					for (final String entryName : unsecureDataStore.getEntryNames(currentDataEntry.getClass())) {
						tableModel.addRow(new Object[] { entryName });
					}

					pack();

					setVisible(true);
				}
			} else {
				setVisible(true);
			}
		} catch (final Exception e) {
			new QuestionDialog((Window) getParent(), "ERROR", "ERROR:\n" + e.getMessage()).setBackgroundColor(SwingColor.LightRed).open();
		}
		return true;
	}
}
