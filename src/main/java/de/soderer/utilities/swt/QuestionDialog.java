package de.soderer.utilities.swt;

import java.text.MessageFormat;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

public class QuestionDialog extends ModalDialog<Integer> {
	private final String text;
	private Color backgroundcolor = null;
	private String checkboxText = null;
	private boolean checkboxStatus = false;
	private final String[] buttonTexts;

	public QuestionDialog(final Shell shell, final String title, final String text, final String... buttonTexts) {
		super(shell, title);

		this.text = text;
		this.buttonTexts = buttonTexts;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));

		final StyledText textField = new StyledText(shell, SWT.READ_ONLY | SWT.BORDER | SWT.MULTI);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		textField.setText(text);
		textField.setWordWrap(true);

		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(final KeyEvent e) {
				if ((e.stateMask & SWT.MOD1) != 0 && (e.keyCode == 'a' || e.keyCode == 'A')) {
					textField.selectAll();
					e.doit = false;
				} else if ((e.stateMask & SWT.MOD1) != 0 && (e.keyCode == 'c' || e.keyCode == 'C')) {
					textField.copy();
					e.doit = false;
				}
			}
		});

		if (backgroundcolor != null) {
			textField.setBackground(backgroundcolor);
		}

		if (Utilities.isNotBlank(checkboxText)) {
			final Button checkboxCheckBox = new Button(shell, SWT.CHECK);
			checkboxCheckBox.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			checkboxCheckBox.setText(checkboxText);
			checkboxCheckBox.setSelection(checkboxStatus);
			checkboxCheckBox.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					checkboxStatus = ((Button) event.widget).getSelection();
				}
			});
		}

		Button buttonToSelect = null;

		if (buttonTexts != null && buttonTexts.length > 0) {
			final Composite buttonComposite = new Composite(shell, SWT.NONE);
			buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			buttonComposite.setLayout(SwtUtilities.createNoMarginGridLayout(buttonTexts.length, true));

			for (int i = 0; i < buttonTexts.length; i++) {
				final String buttonText = buttonTexts[i];
				final int buttonIndex = i;
				final Button nextButton = new Button(buttonComposite, SWT.PUSH);
				nextButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
				nextButton.setText(buttonText);
				nextButton.addSelectionListener(new SelectionAdapter() {
					@Override
					public void widgetSelected(final SelectionEvent event) {
						setReturnValue(buttonIndex);
						getParent().close();
					}
				});

				if (i == 0) {
					buttonToSelect = nextButton;
				}
			}
		} else {
			final Composite buttonComposite = new Composite(shell, SWT.NONE);
			buttonComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			buttonComposite.setLayout(SwtUtilities.createNoMarginGridLayout(1, true));

			final Button cancelButton = new Button(buttonComposite, SWT.PUSH);
			cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
			cancelButton.setText(getI18NString("close"));
			cancelButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					getParent().close();
				}
			});

			buttonToSelect = cancelButton;
		}

		shell.setMinimumSize(250, 50);
		shell.pack();

		if (buttonToSelect != null) {
			getParent().setDefaultButton(buttonToSelect);
			buttonToSelect.setFocus();
		}
	}

	public QuestionDialog setBackgroundColor(final Color backgroundcolor) {
		this.backgroundcolor = backgroundcolor;
		return this;
	}

	public QuestionDialog setCheckboxText(final String checkboxText) {
		this.checkboxText = checkboxText;
		return this;
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
			switch (resourceKey) {
				case "close":
					pattern = "Schließen";
					break;
				default:
					pattern = "MessageKey unbekannt: " + resourceKey
							+ (arguments != null && arguments.length > 0
									? " Argumente: " + Utilities.join(arguments, ", ")
									: "");
			}
			return MessageFormat.format(pattern, arguments);
		} else {
			String pattern;
			switch (resourceKey) {
				case "close":
					pattern = "Close";
					break;
				default:
					pattern = "MessageKey unknown: " + resourceKey
							+ (arguments != null && arguments.length > 0
									? " arguments: " + Utilities.join(arguments, ", ")
									: "");
			}
			return MessageFormat.format(pattern, arguments);
		}
	}
}
