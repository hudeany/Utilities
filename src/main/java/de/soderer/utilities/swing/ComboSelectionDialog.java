package de.soderer.utilities.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

/**
 * Modal dialog to select one value out of a list of values.
 *
 * <p>
 * Uses {@link DropDown} with autocomplete. The entries can be reordered via
 * drag&amp;drop and removed via the Delete key in the popup list. Callers can
 * take over those changes via {@link #getItems()} after the dialog was closed,
 * regardless of whether a value was selected or the dialog was canceled.
 * </p>
 *
 * <p>
 * {@link #open()} returns the selected value, or {@code null} if the dialog
 * was canceled.
 * </p>
 */
public class ComboSelectionDialog extends ModalDialog<String> {
	private static final long serialVersionUID = 6203467613120944811L;

	private final DropDown dropDown;
	private final JButton okButton;

	public ComboSelectionDialog(final Window parent, final String title, final String text, final Collection<String> items) {
		this(parent, title, text, items, -1);
	}

	/**
	 * @param preselectedIndex index of the item shown initially, or -1 for an
	 *                         empty selection (a single item is always
	 *                         preselected)
	 */
	public ComboSelectionDialog(final Window parent, final String title, final String text, final Collection<String> items, final int preselectedIndex) {
		super(parent, title);

		returnValue = null;

		final int margin = 5;

		final JPanel panel = new JPanel(new BorderLayout(margin, margin));
		panel.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));

		final JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.PAGE_AXIS));

		if (Utilities.isNotBlank(text)) {
			final JTextArea textField = new JTextArea(text);
			textField.setEditable(false);
			textField.setFocusable(false);
			textField.setBackground(panel.getBackground());
			textField.setAlignmentX(LEFT_ALIGNMENT);
			centerPanel.add(textField);
		}

		final List<String> itemList = items == null ? new ArrayList<>() : new ArrayList<>(items);

		dropDown = new DropDown()
				.withItems(itemList)
				.withAllowCustomValues(false)
				.withMatchMode(DropDown.MatchMode.CONTAINS)
				.withReorderable(true)
				.withDeletable(true);
		dropDown.setAlignmentX(LEFT_ALIGNMENT);
		if (preselectedIndex >= 0 && preselectedIndex < itemList.size()) {
			dropDown.setText(itemList.get(preselectedIndex));
		} else if (itemList.size() == 1) {
			// Only one possible choice: preselect it, so it can be confirmed directly
			dropDown.setText(itemList.get(0));
		}
		dropDown.addChangeListener(e -> checkButtonStatus());
		dropDown.addValidationListener(valid -> checkButtonStatus());
		dropDown.addItemDeletedListener(deletedItem -> checkButtonStatus());

		final JPanel dropDownPanel = new JPanel(new BorderLayout());
		dropDownPanel.setBorder(BorderFactory.createEmptyBorder(margin, 0, 0, 0));
		dropDownPanel.setAlignmentX(LEFT_ALIGNMENT);
		dropDownPanel.add(dropDown, BorderLayout.CENTER);
		centerPanel.add(dropDownPanel);

		panel.add(centerPanel, BorderLayout.CENTER);

		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, margin, 0));

		okButton = new JButton(getI18NString("ok"));
		okButton.addActionListener(e -> {
			returnValue = dropDown.getText();
			dispose();
		});
		buttonPanel.add(okButton);

		final JButton cancelButton = new JButton(getI18NString("cancel"));
		cancelButton.addActionListener(e -> {
			returnValue = null;
			dispose();
		});
		buttonPanel.add(cancelButton);

		panel.add(buttonPanel, BorderLayout.SOUTH);

		add(panel);

		// Enter is only passed through by the DropDown if there is no suggestion to accept, so it confirms a valid value
		getRootPane().setDefaultButton(okButton);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowOpened(final WindowEvent event) {
				dropDown.requestFocusInWindow();
			}
		});

		setResizable(false);

		pack();

		setMinimumSize(new Dimension(Math.max(getWidth(), 300), getHeight()));
		setSize(getMinimumSize());

		setLocationRelativeTo(parent);

		checkButtonStatus();
	}

	public ComboSelectionDialog(final Window parent, final String title, final String text, final String... items) {
		this(parent, title, text, items == null ? null : Arrays.asList(items), -1);
	}

	/**
	 * Sets an explicit dialog size. A value &lt;= 0 keeps the current width or
	 * height.
	 */
	public ComboSelectionDialog withSize(final int width, final int height) {
		setSize(width > 0 ? width : getWidth(), height > 0 ? height : getHeight());
		setLocationRelativeTo(getOwner());

		return this;
	}

	/**
	 * Current items in their current order, including reordering and deletions
	 * done by the user in the dialog.
	 */
	public List<String> getItems() {
		return new ArrayList<>(Arrays.asList(dropDown.getItems()));
	}

	private void checkButtonStatus() {
		okButton.setEnabled(dropDown.isValidValue() && Utilities.isNotEmpty(dropDown.getText()));
	}

	private static String getI18NString(final String resourceKey, final Object... arguments) {
		if (LangResources.existsKey(resourceKey)) {
			return LangResources.get(resourceKey, arguments);
		} else if ("de".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			String pattern;
			switch (resourceKey) {
				case "ok": pattern = "OK"; break;
				case "cancel": pattern = "Abbrechen"; break;
				default: pattern = "MessageKey unbekannt: " + resourceKey + (arguments != null && arguments.length > 0 ? " Argumente: " + Utilities.join(arguments, ", ") : "");
			}
			return MessageFormat.format(pattern, arguments);
		} else {
			String pattern;
			switch (resourceKey) {
				case "ok": pattern = "OK"; break;
				case "cancel": pattern = "Cancel"; break;
				default: pattern = "MessageKey unknown: " + resourceKey + (arguments != null && arguments.length > 0 ? " arguments: " + Utilities.join(arguments, ", ") : "");
			}
			return MessageFormat.format(pattern, arguments);
		}
	}
}