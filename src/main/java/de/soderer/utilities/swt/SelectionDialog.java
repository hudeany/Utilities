package de.soderer.utilities.swt;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;

public class SelectionDialog extends ModalDialog<String> {

	private final String text;
	private final List<String> items;

	private org.eclipse.swt.widgets.List list;
	private Button okButton;

	public SelectionDialog(final Shell parent, final String title, final String text, final List<String> items) {
		super(parent, SWT.RESIZE, title);
		this.text = text;
		this.items = items;
	}

	@Override
	protected void createComponents(final Shell shell) {
		shell.setText(getParent().getText());
		shell.setLayout(new GridLayout(1, false));

		final Label label = new Label(shell, SWT.NONE);
		label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		label.setText(text);

		list = new org.eclipse.swt.widgets.List(shell, SWT.BORDER | SWT.SINGLE | SWT.V_SCROLL);
		for (final String item : items) {
			list.add(item);
		}
		final GridData listData = new GridData(SWT.FILL, SWT.TOP, true, false);
		listData.heightHint = list.getItemHeight() * 5 + 2;
		list.setLayoutData(listData);

		list.addListener(SWT.Selection, e -> checkStatus());
		list.addListener(SWT.DefaultSelection, e -> okPressed(shell));

		final Composite buttonBar = new Composite(shell, SWT.NONE);
		buttonBar.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
		buttonBar.setLayout(new GridLayout(2, true));

		okButton = new Button(buttonBar, SWT.PUSH);
		okButton.setText(LangResources.get("ok"));
		okButton.setLayoutData(new GridData(90, SWT.DEFAULT));
		okButton.addListener(SWT.Selection, e -> okPressed(shell));

		final Button cancelButton = new Button(buttonBar, SWT.PUSH);
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.setLayoutData(new GridData(90, SWT.DEFAULT));
		cancelButton.addListener(SWT.Selection, e -> shell.close());

		shell.pack();

		checkStatus();
	}

	private void okPressed(final Shell shell) {
		final int index = list.getSelectionIndex();
		if (index >= 0) {
			setReturnValue(list.getItem(index));
		}
		shell.close();
	}

	private void checkStatus() {
		okButton.setEnabled(list.getSelectionCount() > 0);
	}
}
