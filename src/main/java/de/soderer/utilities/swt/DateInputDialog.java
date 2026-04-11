package de.soderer.utilities.swt;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.regex.Pattern;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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

public class DateInputDialog extends ModalDialog<LocalDateTime> {
	private final String text;
	private final DateTimeFormatter formatter;

	private Text textField;
	private String currentText = "";
	private Pattern inputPattern = null;
	private boolean isTechnicalChange = false;
	private String defaultText = null;

	public DateInputDialog(final Shell shell, final String title, final String text, final String datePattern) {
		super(SwtUtilities.createCenteredShellIfNeeded(shell), title);
		this.text = text;
		formatter = DateTimeFormatter.ofPattern(datePattern);
	}

	public void setDefaultText(final String defaultText) {
		this.defaultText = defaultText;
	}

	public void setInputPattern(final String patternString) {
		if (Utilities.isBlank(patternString)) {
			inputPattern = null;
		} else {
			inputPattern = Pattern.compile(patternString);
		}
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));

		final StyledText styledText = new StyledText(shell, SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI);
		styledText.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
		styledText.setText(text);
		styledText.setBackground(shell.getBackground());
		styledText.setAlwaysShowScrollBars(false);
		styledText.setEditable(false);

		final Composite textFieldComposite = new Composite(shell, SWT.NONE);
		textFieldComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		textFieldComposite.setLayout(SwtUtilities.createNoMarginGridLayout(1, true));

		textField = new Text(textFieldComposite, SWT.BORDER);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		textField.setText(defaultText != null ? defaultText : "");

		textField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(final ModifyEvent event) {
				if (!isTechnicalChange) {

					final String value = textField.getText();

					if (inputPattern == null || inputPattern.matcher(value).matches()) {
						currentText = value;
					} else {
						final int pos = textField.getCaretPosition();
						isTechnicalChange = true;
						textField.setText(currentText);
						textField.setSelection(Math.max(0, pos - 1));
						isTechnicalChange = false;
					}
				}
			}
		});

		final Composite mainButtonComposite = new Composite(shell, SWT.NONE);
		mainButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		mainButtonComposite.setLayout(SwtUtilities.createNoMarginGridLayout(2, true));

		final Button okButton = new Button(mainButtonComposite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(getI18NString("ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				try {
					final LocalDateTime parsed = parseDate(textField.getText());
					setReturnValue(parsed);
					getParent().close();
				} catch (@SuppressWarnings("unused") final DateTimeParseException e) {
					textField.setBackground(shell.getDisplay().getSystemColor(SWT.COLOR_RED));
				}
			}
		});

		final Button cancelButton = new Button(mainButtonComposite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(getI18NString("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				setReturnValue(null);
				getParent().close();
			}
		});

		shell.pack();
		textField.forceFocus();
	}

	private LocalDateTime parseDate(final String value) {
		return LocalDateTime.parse(value, formatter);
	}

	private static String getI18NString(final String resourceKey, final Object... arguments) {
		if (LangResources.existsKey(resourceKey)) {
			return LangResources.get(resourceKey, arguments);
		} else if ("de".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			switch (resourceKey) {
				case "ok": return "OK";
				case "cancel": return "Abbrechen";
				default: return "MessageKey unbekannt: " + resourceKey;
			}
		} else {
			switch (resourceKey) {
				case "ok": return "Cancel";
				case "cancel": return "Cancel";
				default: return "MessageKey unknown: " + resourceKey;
			}
		}
	}
}