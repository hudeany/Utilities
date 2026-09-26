package de.soderer.utilities.swt;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.DefaultLangResources;

public class SelectOrBrowseFolderDialog extends ModalDialog<String> {
	private final String text;
	private final String browseButtonText;
	private final List<String> items;
	private Combo comboBox;
	private Button okButton;

	public SelectOrBrowseFolderDialog(final Shell shell, final String title, final String text, final String browseButtonText, final List<String> items) {
		super(shell, SWT.RESIZE, title);

		this.text = text;
		this.browseButtonText = browseButtonText;
		this.items = items;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(4, true));
		shell.setSize(475, 120);
		shell.setMinimumSize(475, 120);

		final Label label = new Label(shell, SWT.FILL);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 4, 1));
		label.setText(text);

		comboBox = new Combo(shell, SWT.FILL | SWT.READ_ONLY);
		comboBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 3, 1));
		comboBox.addSelectionListener(new ComboBoxButtonSelectionListener());
		if (items != null && items.size() > 0) {
			comboBox.setItems(items.toArray(new String[items.size()]));
			comboBox.select(comboBox.getItemCount() - 1);
		}

		final Button browseButton = new Button(shell, SWT.PUSH);
		browseButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, false, false, 1, 1));
		browseButton.setText(browseButtonText);
		browseButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				final DirectoryDialog dialog = new DirectoryDialog(getParent(), SWT.OPEN);
				dialog.setText(getText());
				final String filePath = dialog.open();
				if (filePath != null) {
					String[] newItems;
					if (items != null && items.size() > 0) {
						newItems = new String[items.size() + 1];
						for (int i = 0; i < items.size(); i++) {
							newItems[i] = items.get(i).toString();
						}
						newItems[items.size()] = filePath;
					} else {
						newItems = new String[] { filePath };
					}
					comboBox.setItems(newItems);
					comboBox.select(comboBox.getItemCount() - 1);
					okButton.setEnabled(true);
				}
			}
		});

		okButton = new Button(shell, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 2, 1));
		okButton.setText(DefaultLangResources.getI18NStringOrDefault("ok"));
		okButton.setEnabled(comboBox.getItemCount() > 0);
		okButton.addSelectionListener(new OkButtonSelectionListener());

		final Button cancelButton = new Button(shell, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false, 2, 1));
		cancelButton.setText(DefaultLangResources.getI18NStringOrDefault("cancel"));
		cancelButton.addSelectionListener(new CancelButtonSelectionListener());
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
			if (comboBox.getSelectionIndex() > -1) {
				setReturnValue(comboBox.getItems()[comboBox.getSelectionIndex()]);
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
