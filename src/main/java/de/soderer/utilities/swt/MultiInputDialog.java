package de.soderer.utilities.swt;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

public class MultiInputDialog extends ModalDialog<List<String>> {
	private final String prologText;
	private final String[] labelTexts;
	private String[] defaultTexts = null;
	private int width = 200;

	private final List<Text> textFields = new ArrayList<>();

	public MultiInputDialog(final Shell shell, final String title, final String prologText, final String... labelTexts) {
		super(SwtUtilities.createCenteredShellIfNeeded(shell), title);

		this.prologText = prologText;
		this.labelTexts = labelTexts;
	}

	public MultiInputDialog setDefaultTexts(final String[] defaultTexts) {
		this.defaultTexts = defaultTexts;
		return this;
	}

	public MultiInputDialog setWidth(final int width) {
		this.width = width;
		return this;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(2, false));

		if (Utilities.isNotBlank(prologText)) {
			final StyledText styledText = new StyledText(shell, SWT.NONE);
			styledText.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
			styledText.setLayout(SwtUtilities.createSmallMarginGridLayout(1, false));
			styledText.setText(prologText);
			styledText.setBackground(shell.getBackground());
			styledText.setEditable(false);
		}

		int index = 0;
		for (final String text : labelTexts) {
			final Label label = new Label(shell, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
			label.setText(text);

			final Text textField = new Text(shell, SWT.BORDER);
			if (defaultTexts != null && defaultTexts.length > index && defaultTexts[index] != null) {
				textField.setText(defaultTexts[index]);
			}
			textField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(final KeyEvent event) {
					// on Enter-key
					if (event.keyCode == SWT.CR || event.keyCode == SwtUtilities.BOTTOM_RIGHT_ENTER_KEY) {
						final List<String> returnValueList = new ArrayList<>();
						for (final Text textFieldForReturnValue: textFields) {
							returnValueList.add(textFieldForReturnValue.getText());
						}
						setReturnValue(returnValueList);
						getParent().close();
					}
				}
			});

			final GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
			gridData.widthHint = width;
			textField.setLayoutData(gridData);

			if (index == 0) {
				textField.forceFocus();
			}

			textFields.add(textField);

			index++;
		}

		final Composite mainButtonComposite = new Composite(shell, SWT.NONE);
		mainButtonComposite.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, false, 2, 1));
		mainButtonComposite.setLayout(SwtUtilities.createNoMarginGridLayout(2, true));

		final Button okButton = new Button(mainButtonComposite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.CENTER, SWT.UP, true, false));
		okButton.setText(getI18NString("ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				final List<String> retunValueList = new ArrayList<>();
				for (final Text textFieldForReturnValue: textFields) {
					retunValueList.add(textFieldForReturnValue.getText());
				}
				setReturnValue(retunValueList);
				getParent().close();
			}
		});

		final GridData gridDataOk = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gridDataOk.widthHint = 100;
		okButton.setLayoutData(gridDataOk);

		final Button cancelButton = new Button(mainButtonComposite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.CENTER, SWT.UP, true, false));
		cancelButton.setText(getI18NString("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				setReturnValue(null);
				getParent().close();
			}
		});

		final GridData gridDataCancel = new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1);
		gridDataCancel.widthHint = 100;
		cancelButton.setLayoutData(gridDataCancel);

		shell.pack();
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
