package de.soderer.utilities.swt;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

public class ComboSelectionDialog extends ModalDialog<String> {
	private final String text;

	/**
	 * Current item order, kept in sync with the DropDown's popup whenever the
	 * user reorders entries via drag&amp;drop, so callers can retrieve and
	 * reuse the (possibly reordered) list after the dialog was closed -
	 * regardless of whether it was closed via OK or Cancel.
	 */
	private final List<String> items;
	private DropDown comboBox;
	private Button okButton;
	private final int preSelectedItemIndex;

	public ComboSelectionDialog(final Shell shell, final String title, final String text, final List<String> items) {
		this(shell, title, text, items, -1);
	}

	public ComboSelectionDialog(final Shell shell, final String title, final String text, final List<String> items, final int preSelectedItemIndex) {
		super(shell, SWT.RESIZE, title);

		this.text = text;
		this.items = items == null ? new ArrayList<>() : new ArrayList<>(items);
		this.preSelectedItemIndex = preSelectedItemIndex;
	}

	/**
	 * Returns the current item order, i.e. the items passed into the
	 * constructor, possibly reordered by the user via drag&amp;drop in the
	 * DropDown's popup. Reflects the latest order regardless of whether the
	 * dialog was closed via OK or Cancel, so callers can persist and reuse a
	 * reordering even if the user did not confirm a new selection.
	 */
	public List<String> getItems() {
		return new ArrayList<>(items);
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));

		final Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(2, false));

		final Label label = new Label(composite, SWT.FILL);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
		label.setText(text);

		comboBox = new DropDown(composite, SWT.NONE);
		comboBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		comboBox.setAllowCustomValues(false);
		comboBox.setReorderable(true);
		comboBox.addItemsReorderedListener(new ItemsReorderedListener());
		comboBox.addSelectionListener(new ComboBoxButtonSelectionListener());
		if (items.size() > 0) {
			comboBox.setItems(items);
			if (preSelectedItemIndex < 0) {
				comboBox.setText(items.get(items.size() - 1));
			} else {
				comboBox.setText(items.get(preSelectedItemIndex));
			}
		}

		okButton = new Button(composite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(getI18NString("ok"));
		okButton.setEnabled(comboBox.isValidValue());
		okButton.addSelectionListener(new OkButtonSelectionListener());

		final Button cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(getI18NString("cancel"));
		cancelButton.addSelectionListener(new CancelButtonSelectionListener());

		shell.setMinimumSize(250, 50);
		shell.pack();
	}

	private class ComboBoxButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(final SelectionEvent event) {
			okButton.setEnabled(true);
		}
	}

	private class OkButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(final SelectionEvent event) {
			if (comboBox.getText() != null) {
				setReturnValue(comboBox.getText());
			}
			getParent().close();
		}
	}

	/**
	 * Keeps "items" in sync whenever the user reorders entries via
	 * drag&amp;drop in the DropDown's popup, so the reordered list remains
	 * available through {@link #getItems()} after the dialog closes.
	 */
	private class ItemsReorderedListener implements Consumer<List<String>> {
		@Override
		public void accept(final List<String> newOrder) {
			items.clear();
			items.addAll(newOrder);
		}
	}

	private class CancelButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(final SelectionEvent event) {
			getParent().close();
		}
	}

	private static String getI18NString(final String resourceKey, final Object... arguments) {
		if (LangResources.existsKey(resourceKey)) {
			return LangResources.get(resourceKey, arguments);
		} else if ("de".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			String pattern;
			switch(resourceKey) {
				case "ok": pattern = "OK"; break;
				case "cancel": pattern = "Abbrechen"; break;
				default: pattern = "MessageKey unbekannt: " + resourceKey + (arguments != null && arguments.length > 0 ? " Argumente: " + Utilities.join(arguments, ", ") : "");
			}
			return MessageFormat.format(pattern, arguments);
		} else {
			String pattern;
			switch(resourceKey) {
				case "ok": pattern = "OK"; break;
				case "cancel": pattern = "Cancel"; break;
				default: pattern = "MessageKey unknown: " + resourceKey + (arguments != null && arguments.length > 0 ? " arguments: " + Utilities.join(arguments, ", ") : "");
			}
			return MessageFormat.format(pattern, arguments);
		}
	}
}
