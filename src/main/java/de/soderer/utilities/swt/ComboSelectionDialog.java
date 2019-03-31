package de.soderer.utilities.swt;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;

public class ComboSelectionDialog extends ModalDialog<String> {
	private final String text;
	private final List<String> items;
	private Combo comboBox;
	private Button okButton;
	private final int preSelectedItemIndex;

	public ComboSelectionDialog(final Shell shell, final String title, final String text, final List<String> items) {
		this(shell, title, text, items, -1);
	}

	public ComboSelectionDialog(final Shell shell, final String title, final String text, final List<String> items, final int preSelectedItemIndex) {
		super(shell, SWT.RESIZE, title);

		this.text = text;
		this.items = items;
		this.preSelectedItemIndex = preSelectedItemIndex;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new FillLayout());
		shell.setSize(475, 120);
		shell.setMinimumSize(475, 120);
		final Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(2, false));

		final Label label = new Label(composite, SWT.FILL);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
		label.setText(text);

		comboBox = new Combo(composite, SWT.FILL | SWT.READ_ONLY);
		comboBox.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		comboBox.addSelectionListener(new ComboBoxButtonSelectionListener());
		if (items != null && items.size() > 0) {
			final String[] comboBoxItems = items.toArray(new String[items.size()]);
			comboBox.setItems(comboBoxItems);
			if (preSelectedItemIndex < 0) {
				comboBox.select(comboBox.getItemCount() - 1);
			} else {
				comboBox.select(preSelectedItemIndex);
			}
		}

		okButton = new Button(composite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(LangResources.get("ok"));
		okButton.setEnabled(comboBox.getItemCount() > 0);
		okButton.addSelectionListener(new OkButtonSelectionListener());

		final Button cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new CancelButtonSelectionListener());
	}

	private class ComboBoxButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(final SelectionEvent e) {
			okButton.setEnabled(true);
		}
	}

	private class OkButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(final SelectionEvent e) {
			if (comboBox.getSelectionIndex() > -1) {
				setReturnValue(comboBox.getItems()[comboBox.getSelectionIndex()]);
			}
			getParent().close();
		}
	}

	private class CancelButtonSelectionListener extends SelectionAdapter {
		@Override
		public void widgetSelected(final SelectionEvent e) {
			getParent().close();
		}
	}
}
