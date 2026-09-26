package de.soderer.utilities.swt;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.DefaultLangResources;

public class SelectOrBrowseFolderDialog extends ModalDialog<String> {
	private final String text;
	private final String browseButtonText;
	/** Mutable working copy of the offered folders, so every browsed folder stays selectable */
	private final List<String> items = new ArrayList<>();
	private DropDown comboBox;
	private Button okButton;

	public SelectOrBrowseFolderDialog(final Shell shell, final String title, final String text, final String browseButtonText, final List<String> items) {
		super(shell, SWT.RESIZE, title);

		this.text = text;
		this.browseButtonText = browseButtonText;
		if (items != null) {
			this.items.addAll(items);
		}
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(4, true));
		shell.setSize(475, 120);
		shell.setMinimumSize(475, 120);

		final Label label = new Label(shell, SWT.FILL);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 4, 1));
		label.setText(text);

		// Fixed list of folders: no custom values (replaces the former read-only SWT Combo).
		// New folders are only added via the browse button.
		comboBox = new DropDown(shell, SWT.NONE);
		comboBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		// Case-sensitive, since folder paths on Linux/macOS may differ only in case
		comboBox.setCaseSensitive(true);
		comboBox.setMatchMode(DropDown.MatchMode.CONTAINS);
		comboBox.setAllowCustomValues(false);
		comboBox.setItems(items);
		if (!items.isEmpty()) {
			comboBox.setText(items.get(items.size() - 1));
		}

		final Button browseButton = new Button(shell, SWT.PUSH);
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, false, false, 1, 1));
		browseButton.setText(browseButtonText);
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				final DirectoryDialog dialog = new DirectoryDialog(getParent(), SWT.OPEN);
				dialog.setText(getText());
				final String folderPath = dialog.open();
				if (folderPath != null) {
					// Move an already offered folder to the end instead of listing it twice
					items.remove(folderPath);
					items.add(folderPath);
					comboBox.setItems(items);
					comboBox.setText(folderPath);
					updateOkButtonStatus();
				}
			}
		});

		okButton = new Button(shell, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 2, 1));
		okButton.setText(DefaultLangResources.getI18NStringOrDefault("ok"));
		okButton.addSelectionListener(new OkButtonSelectionListener());

		final Button cancelButton = new Button(shell, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 2, 1));
		cancelButton.setText(DefaultLangResources.getI18NStringOrDefault("cancel"));
		cancelButton.addSelectionListener(new CancelButtonSelectionListener());

		// Registered only after okButton exists, because setText() above may already fire these
		comboBox.addListener(SWT.Selection, e -> updateOkButtonStatus());
		comboBox.addValidationListener(valid -> updateOkButtonStatus());
		updateOkButtonStatus();
	}

	/** OK is only possible while the DropDown shows one of the offered folders */
	private void updateOkButtonStatus() {
		final String selectedFolder = comboBox.getText();
		okButton.setEnabled(selectedFolder != null && !selectedFolder.isBlank() && items.contains(selectedFolder));
	}

	private class OkButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(final SelectionEvent event) {
			final String selectedFolder = comboBox.getText();
			if (selectedFolder != null && items.contains(selectedFolder)) {
				setReturnValue(selectedFolder);
			}
			getParent().close();
		}
	}

	private class CancelButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(final SelectionEvent event) {
			getParent().close();
		}
	}
}
