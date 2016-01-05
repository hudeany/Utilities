package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

public class QuestionDialog extends ModalDialog<Integer> {
	private String text;
	private String rememberAnswerText = null;
	private boolean rememberAnswerStatus = false;
	private String[] buttonTexts;

	public QuestionDialog(Shell shell, String title, String text, String... buttonTexts) {
		super(shell, SWT.RESIZE, title);

		this.text = text;
		this.buttonTexts = buttonTexts;

		if (buttonTexts == null || buttonTexts.length == 0) {
			this.buttonTexts = new String[] { LangResources.get("ok") };
		}
	}

	@Override
	protected void createComponents(Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));

		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(buttonTexts.length, true));

		Text textField = new Text(composite, SWT.READ_ONLY | SWT.MULTI);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, buttonTexts.length, 1));
		textField.setText(text);

		if (Utilities.isNotBlank(rememberAnswerText)) {
			Button rememberAnswerCheckBox = new Button(composite, SWT.CHECK);
			rememberAnswerCheckBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, buttonTexts.length, 1));
			rememberAnswerCheckBox.setSelection(rememberAnswerStatus);
			rememberAnswerCheckBox.setText(rememberAnswerText);
			rememberAnswerCheckBox.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					rememberAnswerStatus = ((Button) e.widget).getSelection();
				}
			});
		}

		Button buttonToSelect = null;

		if (buttonTexts != null && buttonTexts.length > 0) {
			for (int i = 0; i < buttonTexts.length; i++) {
				String buttonText = buttonTexts[i];
				final int buttonIndex = i;
				Button button = new Button(composite, SWT.PUSH);
				button.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
				button.setText(buttonText);
				button.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						setReturnValue(buttonIndex);
						getParent().close();
					}
				});
				if (i == 0) {
					buttonToSelect = button;
				}
			}
		}

		if (buttonToSelect != null) {
			buttonToSelect.setFocus();
		}

		shell.pack();
		shell.setMinimumSize(shell.getSize());
	}

	public void setRememberAnswerText(String rememberAnswerText, boolean status) {
		this.rememberAnswerText = rememberAnswerText;
		rememberAnswerStatus = status;
	}

	public boolean getRememberAnswerStatus() {
		return rememberAnswerStatus;
	}
}
