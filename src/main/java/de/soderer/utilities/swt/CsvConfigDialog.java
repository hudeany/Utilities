package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.swt.RadioButtonGroup.ArrangementStyle;

public class CsvConfigDialog extends ModalDialog<Boolean> {
	protected String text = null;

	protected char separator = ',';
	protected char stringQuote = '\'';
	protected boolean useStringQuote;
	protected boolean allowUnderfilledLines;
	protected boolean showContainsHeadersInFirstRowButton = false;
	protected boolean containsHeadersInFirstRow = true;
	protected boolean showEncodingField = false;
	protected String encoding = "UTF-8";

	protected RadioButtonGroup quoteTypeRadioButtonGroup;
	protected RadioButtonGroup underfilledLinesRadioButtonGroup;
	protected RadioButtonGroup separatorRadioButtonGroup;
	protected Button useQuoteButton;

	public CsvConfigDialog(Shell shell, String title, String text) {
		super(shell, title);
		this.text = text;
	}

	@Override
	protected void createComponents(Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));

		if (Utilities.isNotBlank(text)) {
			Label label = new Label(shell, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false, 1, 1));
			label.setText(text);
		}

		separatorRadioButtonGroup = new RadioButtonGroup(shell, LangResources.get("csvSeparator"), SWT.NONE, ArrangementStyle.Horizontal, LangResources.get("csvSeparatorComa"),
				LangResources.get("csvSeparatorSemicolon"));
		separatorRadioButtonGroup.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false, 1, 1));
		separatorRadioButtonGroup.setSelectedButtonIndex(separator == ',' ? 0 : 1);

		Composite quoteComposite = new Composite(shell, SWT.NONE);
		quoteComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		quoteComposite.setLayout(SwtUtilities.createNoMarginGridLayout(3, false));

		useQuoteButton = new Button(quoteComposite, SWT.CHECK);
		useQuoteButton.setText(LangResources.get("csvUseQuote") + ": ");
		useQuoteButton.setSelection(useStringQuote);
		useQuoteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkStatus();
			}
		});

		quoteTypeRadioButtonGroup = new RadioButtonGroup(quoteComposite, SWT.NONE, ArrangementStyle.Horizontal, LangResources.get("csvSingleQuote"), LangResources.get("csvDoubleQuote"));
		quoteTypeRadioButtonGroup.setSelectedButtonIndex(stringQuote == '\'' ? 0 : 1);

		if (showContainsHeadersInFirstRowButton) {
			Button containsHeadersInFirstRowButton = new Button(quoteComposite, SWT.CHECK);
			containsHeadersInFirstRowButton.setText(LangResources.get("containsHeadersInFirstRow"));
			containsHeadersInFirstRowButton.setSelection(containsHeadersInFirstRow);
			containsHeadersInFirstRowButton.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false, 3, 1));
			containsHeadersInFirstRowButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					containsHeadersInFirstRow = ((Button) e.widget).getSelection();
					checkStatus();
				}
			});
		}

		underfilledLinesRadioButtonGroup = new RadioButtonGroup(shell, LangResources.get("csvUnderfilledLines"), SWT.NONE, ArrangementStyle.Horizontal, LangResources.get("yes"),
				LangResources.get("no"));
		underfilledLinesRadioButtonGroup.setSelectedButtonIndex(allowUnderfilledLines ? 0 : 1);

		if (showEncodingField) {
			Composite encodingComposite = new Composite(shell, SWT.NONE);
			encodingComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
			encodingComposite.setLayout(SwtUtilities.createNoMarginGridLayout(2, false));

			Label encodingLabel = new Label(encodingComposite, SWT.NONE);
			encodingLabel.setText(LangResources.get("encoding") + ": ");

			Combo encodingText = new Combo(encodingComposite, SWT.BORDER);
			encodingText.setItems(new String[] { "UTF-8", "ISO-8859-1", "ISO-8859-15" });
			encodingText.setText(encoding);
			encodingText.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					encoding = ((Combo) e.widget).getText();
					checkStatus();
				}
			});
		}

		Composite mainButtonComposite = new Composite(shell, SWT.NONE);
		mainButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		mainButtonComposite.setLayout(SwtUtilities.createNoMarginGridLayout(2, true));

		Button okButton = new Button(mainButtonComposite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(LangResources.get("ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				separator = separatorRadioButtonGroup.getSelectedButtonIndex() == 0 ? ',' : ';';
				useStringQuote = useQuoteButton.getSelection();
				stringQuote = quoteTypeRadioButtonGroup.getSelectedButtonIndex() == 0 ? '\'' : '"';
				allowUnderfilledLines = underfilledLinesRadioButtonGroup.getSelectedButtonIndex() == 0;
				setReturnValue(true);
				getParent().close();
			}
		});

		Button cancelButton = new Button(mainButtonComposite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(false);
				getParent().close();
			}
		});

		shell.pack();

		shell.setSize(shell.getSize().x, shell.getSize().y);
		shell.setMinimumSize(shell.getSize().x, shell.getSize().y);

		checkStatus();
	}

	private void checkStatus() {
		quoteTypeRadioButtonGroup.setEnabled(useQuoteButton.getSelection());
	}

	public void setSeparator(char value) {
		separator = value;
	}

	public char getSeparator() {
		return separator;
	}

	public void setStringQuote(char value) {
		stringQuote = value;
	}

	public char getStringQuote() {
		return stringQuote;
	}

	public void setUseStringQuote(boolean value) {
		useStringQuote = value;
	}

	public boolean getUseStringQuote() {
		return useStringQuote;
	}

	public void setAllowUnderfilledLines(boolean value) {
		allowUnderfilledLines = value;
	}

	public boolean getAllowUnderfilledLines() {
		return allowUnderfilledLines;
	}

	public void setShowContainsHeadersInFirstRowButton(boolean value) {
		showContainsHeadersInFirstRowButton = value;
	}

	public void setContainsHeadersInFirstRow(boolean value) {
		containsHeadersInFirstRow = value;
	}

	public boolean isContainsHeadersInFirstRow() {
		return containsHeadersInFirstRow;
	}

	public void setShowEncodingField(boolean value) {
		showEncodingField = value;
	}

	public void setEncoding(String value) {
		encoding = value;
	}

	public String getEncoding() {
		return encoding;
	}
}
