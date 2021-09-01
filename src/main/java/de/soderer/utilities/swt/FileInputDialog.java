package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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

public class FileInputDialog extends ModalDialog<String> {
	private final String text;
	private LabeledFileText labeledFileText;
	private Button okButton;

	public FileInputDialog(final Shell shell, final String title, final String text) {
		super(shell, SWT.RESIZE, title);

		this.text = text;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));

		final Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(2, false));

		if (text != null) {
			final Label label = new Label(composite, SWT.NONE);
			label.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
			label.setText(text);
		}

		labeledFileText = new LabeledFileText(shell, composite, SWT.NONE, null, LangResources.get("openDataFile"), false, true, ArrangementStyle.Horizontal);
		labeledFileText.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
		labeledFileText.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent e) {
				setReturnValue(((LabeledFileText) e.widget).getText());
				checkStatus();
			}
		});

		okButton = new Button(composite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(LangResources.get("ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				getParent().close();
			}
		});

		final Button cancelButton = new Button(composite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				setReturnValue(null);
				getParent().close();
			}
		});

		checkStatus();

		shell.pack();
		shell.setSize(400, shell.getSize().y);
	}

	private void checkStatus() {
		okButton.setEnabled(Utilities.isNotBlank(labeledFileText.getText()));
	}
}
