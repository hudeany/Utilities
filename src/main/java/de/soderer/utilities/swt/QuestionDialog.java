package de.soderer.utilities.swt;

import java.text.MessageFormat;
import java.util.Locale;

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
	private final String text;
	private String checkboxText = null;
	private boolean checkboxStatus = false;
	private final String[] buttonTexts;

	public QuestionDialog(final Shell shell, final String title, final String text, final String... buttonTexts) {
		super(shell, SWT.RESIZE, title);

		this.text = text;
		this.buttonTexts = buttonTexts;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));

		final Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		composite.setLayout(SwtUtilities.createNoMarginGridLayout(buttonTexts.length, true));

		final Text textField = new Text(composite, SWT.READ_ONLY | SWT.MULTI);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, buttonTexts.length, 1));
		textField.setText(text);

		if (Utilities.isNotBlank(checkboxText)) {
			final Button checkboxCheckBox = new Button(composite, SWT.CHECK);
			checkboxCheckBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, buttonTexts.length, 1));
			checkboxCheckBox.setSelection(checkboxStatus);
			checkboxCheckBox.setText(checkboxText);
			checkboxCheckBox.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					checkboxStatus = ((Button) e.widget).getSelection();
				}
			});
		}

		Button buttonToSelect = null;

		if (buttonTexts != null && buttonTexts.length > 0) {
			for (int i = 0; i < buttonTexts.length; i++) {
				final String buttonText = buttonTexts[i];
				final int buttonIndex = i;
				final Button nextButton = new Button(composite, SWT.PUSH);
				nextButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
				nextButton.setText(buttonText);
				nextButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent e) {
						setReturnValue(buttonIndex);
						getParent().close();
					}
				});
				if (i == 0) {
					buttonToSelect = nextButton;
				}
			}
		} else {
			final Button cancelButton = new Button(composite, SWT.PUSH);
			cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
			cancelButton.setText(getI18NString("close"));
			cancelButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					getParent().close();
				}
			});

			buttonToSelect = cancelButton;
		}

		shell.pack();
		shell.setMinimumSize(shell.getSize());

		if (buttonToSelect != null) {
			getParent().setDefaultButton(buttonToSelect);
			buttonToSelect.setFocus();
		}
	}

	public void setCheckboxText(final String checkboxText) {
		this.checkboxText = checkboxText;
	}

	public QuestionDialog setCheckboxStatus(final boolean checkboxStatus) {
		this.checkboxStatus = checkboxStatus;
		return this;
	}

	public boolean getCheckboxStatus() {
		return checkboxStatus;
	}

	@Override
	protected Integer getDefaultReturnValue() {
		return -1;
	}

	private static String getI18NString(final String resourceKey, final Object... arguments) {
		if (LangResources.existsKey(resourceKey)) {
			return LangResources.get(resourceKey, arguments);
		} else if ("de".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			String pattern;
			switch(resourceKey) {
				case "close": pattern = "Schließen"; break;
				default: pattern = "MessageKey unbekannt: " + resourceKey + (arguments != null && arguments.length > 0 ? " Argumente: " + Utilities.join(arguments, ", ") : "");
			}
			return MessageFormat.format(pattern, arguments);
		} else {
			String pattern;
			switch(resourceKey) {
				case "close": pattern = "Close"; break;
				default: pattern = "MessageKey unknown: " + resourceKey + (arguments != null && arguments.length > 0 ? " arguments: " + Utilities.join(arguments, ", ") : "");
			}
			return MessageFormat.format(pattern, arguments);
		}
	}
}
