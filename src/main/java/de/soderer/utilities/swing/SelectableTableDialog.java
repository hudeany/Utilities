package de.soderer.utilities.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import de.soderer.utilities.Utilities;

/**
 * Modal dialog showing a table with a checkbox in front of every row, so the
 * user can select rows.
 *
 * <p>
 * {@link #open()} returns true if the user confirmed the selection, false if
 * the dialog was canceled and null if it was closed via the window decoration.
 * The selected rows are available via {@link #getSelectedItems()}.
 * </p>
 */
public class SelectableTableDialog extends ModalDialog<Boolean> {
	private static final long serialVersionUID = -718203702968699038L;

	/** Table with a checkbox column in front of the value columns */
	private JTable table;

	/** Rows selected by the user with their values in the order of the headers, null if the dialog was not confirmed */
	private List<List<String>> selectedItems;

	/**
	 * Creates a selectable table dialog.
	 *
	 * @param parent owner window, may be null
	 * @param title title of the dialog
	 * @param text text shown above the table, may be blank
	 * @param headers column headers
	 * @param items rows of the table, each with one value per header
	 * @param defaultState initial status of the row checkboxes
	 * @param selectAllButtonText text of the "select all" button, blank for no button
	 * @param selectNoneButtonText text of the "select none" button, blank for no button
	 * @param selectButtonText text of the button confirming the selection
	 * @param cancelButtonText text of the cancel button
	 */
	public SelectableTableDialog(final Window parent, final String title, final String text, final List<String> headers, final List<List<String>> items, final boolean defaultState, final String selectAllButtonText, final String selectNoneButtonText, final String selectButtonText, final String cancelButtonText) {
		super(parent, title);

		final SelectableTableDialog dialog = this;

		setResizable(false);

		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		if (Utilities.isNotBlank(text)) {
			final JTextArea textField = new JTextArea();
			textField.setText(text);
			textField.setEditable(false);
			textField.setBackground(Color.LIGHT_GRAY);

			final JScrollPane textScrollpane = new JScrollPane(textField);
			textScrollpane.setPreferredSize(new Dimension(textField.getPreferredSize().width + 5, textField.getPreferredSize().height + 5));

			// Text Panel
			final JPanel textPanel = new JPanel();
			textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.LINE_AXIS));

			textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
			textPanel.add(textScrollpane);
			textPanel.add(Box.createRigidArea(new Dimension(5, 0)));

			panel.add(Box.createRigidArea(new Dimension(0, 5)));
			panel.add(textPanel);
		}

		// table area
		final Object[] headerArray = new Object[headers.size() + 1];
		headerArray[0] = "";
		for (int i = 0; i < headers.size(); i++) {
			headerArray[i + 1] = headers.get(i);
		}

		final Object[][] itemsArray = new Object[items.size()][];
		for (int i = 0; i < items.size(); i++) {
			final Object[] itemArray = new Object[items.get(i).size() + 1];
			itemArray[0] = defaultState;
			for (int j = 0; j < items.get(i).size(); j++) {
				itemArray[j + 1] = items.get(i).get(j);
			}
			itemsArray[i] = itemArray;
		}

		final DefaultTableModel tableModel = new DefaultTableModel(itemsArray, headerArray) {
			private static final long serialVersionUID = -372185535503984868L;

			@Override
			public Class<?> getColumnClass(final int columnIndex) {
				if (columnIndex == 0) {
					return Boolean.class;
				} else {
					return String.class;
				}
			}
		};
		table = new JTable(tableModel);
		table.setBackground(Color.LIGHT_GRAY);

		final JScrollPane tableScrollpane = new JScrollPane(table);
		tableScrollpane.setPreferredSize(new Dimension(table.getPreferredSize().width + 5, 100));

		final JPanel tablePanel = new JPanel();
		tablePanel.setLayout(new BoxLayout(tablePanel, BoxLayout.LINE_AXIS));

		tablePanel.add(Box.createRigidArea(new Dimension(5, 0)));
		tablePanel.add(tableScrollpane);
		tablePanel.add(Box.createRigidArea(new Dimension(5, 0)));

		// Button Panel 1
		final JPanel buttonPanel1 = new JPanel();
		buttonPanel1.setLayout(new BoxLayout(buttonPanel1, BoxLayout.LINE_AXIS));

		buttonPanel1.add(Box.createRigidArea(new Dimension(5, 0)));

		if (Utilities.isNotBlank(selectAllButtonText)) {
			final JButton selectAllButton = new JButton(selectAllButtonText);
			selectAllButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent event) {
					// Model indexes: the user may have reordered the view columns
					for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); rowIndex++) {
						tableModel.setValueAt(true, rowIndex, 0);
					}
				}
			});
			buttonPanel1.add(selectAllButton);

			buttonPanel1.add(Box.createRigidArea(new Dimension(5, 0)));
		}

		if (Utilities.isNotBlank(selectNoneButtonText)) {
			final JButton selectNoneButton = new JButton(selectNoneButtonText);
			selectNoneButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent event) {
					// Model indexes: the user may have reordered the view columns
					for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); rowIndex++) {
						tableModel.setValueAt(false, rowIndex, 0);
					}
				}
			});
			buttonPanel1.add(selectNoneButton);

			buttonPanel1.add(Box.createRigidArea(new Dimension(5, 0)));
		}

		// Button Panel 2
		final JPanel buttonPanel2 = new JPanel();
		buttonPanel2.setLayout(new BoxLayout(buttonPanel2, BoxLayout.LINE_AXIS));

		buttonPanel2.add(Box.createRigidArea(new Dimension(5, 0)));

		final JButton selectButton = new JButton(selectButtonText);
		selectButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				// Read the model by model indexes (column 0 is the checkbox, column i + 1 is header i):
				// view column indexes differ from the model indexes after the user reordered columns
				selectedItems = new ArrayList<>();
				for (int rowIndex = 0; rowIndex < tableModel.getRowCount(); rowIndex++) {
					if (Boolean.TRUE.equals(tableModel.getValueAt(rowIndex, 0))) {
						final List<String> item = new ArrayList<>();
						for (int columnIndex = 1; columnIndex <= headers.size(); columnIndex++) {
							item.add((String) tableModel.getValueAt(rowIndex, columnIndex));
						}
						selectedItems.add(item);
					}
				}

				returnValue = true;

				dialog.dispose();
			}
		});
		buttonPanel2.add(selectButton);

		buttonPanel2.add(Box.createRigidArea(new Dimension(5, 0)));

		final JButton cancelButton = new JButton(cancelButtonText);
		cancelButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				selectedItems = null;
				returnValue = false;
				dialog.dispose();
			}
		});
		buttonPanel2.add(cancelButton);

		buttonPanel2.add(Box.createRigidArea(new Dimension(5, 0)));

		panel.add(Box.createRigidArea(new Dimension(0, 5)));
		panel.add(tablePanel);
		if (Utilities.isNotBlank(selectAllButtonText) || Utilities.isNotBlank(selectNoneButtonText)) {
			panel.add(buttonPanel1);
			panel.add(Box.createRigidArea(new Dimension(0, 5)));
		}
		panel.add(buttonPanel2);

		add(panel);

		pack();

		setLocationRelativeTo(parent);

		getRootPane().setDefaultButton(selectButton);
	}

	/**
	 * Returns the rows selected by the user.
	 *
	 * @return selected rows with their values in the order of the headers, null
	 *         if the dialog was not confirmed
	 */
	public List<List<String>> getSelectedItems() {
		return selectedItems;
	}
}