package de.soderer.utilities.swt;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.swt.SWT;
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

public class CsvConfigDialog extends ModalDialog<Boolean> {
	protected String text = null;

	protected char separator = ',';
	protected char stringQuote = '\'';
	protected boolean useStringQuote;
	protected boolean allowUnderfilledLines;
	protected boolean showContainsHeadersInFirstRowButton = false;
	protected boolean containsHeadersInFirstRow = true;
	protected boolean showEncodingField = false;
	protected Charset encoding = StandardCharsets.UTF_8;

	protected RadioButtonGroup quoteTypeRadioButtonGroup;
	protected RadioButtonGroup underfilledLinesRadioButtonGroup;
	protected RadioButtonGroup separatorRadioButtonGroup;
	protected Button useQuoteButton;
	protected DropDown encodingDropDown;
	protected Button okButton;

	public CsvConfigDialog(final Shell shell, final String title, final String text) {
		super(shell, title);
		this.text = text;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));

		if (Utilities.isNotBlank(text)) {
			final Label label = new Label(shell, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false, 1, 1));
			label.setText(text);
		}

		separatorRadioButtonGroup = new RadioButtonGroup(shell, LangResources.get("csvSeparator"), SWT.NONE, ArrangementStyle.Horizontal, LangResources.get("csvSeparatorComma"),
				LangResources.get("csvSeparatorSemicolon"));
		separatorRadioButtonGroup.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false, 1, 1));
		separatorRadioButtonGroup.setSelectedButtonIndex(separator == ',' ? 0 : 1);

		final Composite quoteComposite = new Composite(shell, SWT.NONE);
		quoteComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		quoteComposite.setLayout(SwtUtilities.createNoMarginGridLayout(3, false));

		useQuoteButton = new Button(quoteComposite, SWT.CHECK);
		useQuoteButton.setText(LangResources.get("csvUseQuote") + ": ");
		useQuoteButton.setSelection(useStringQuote);
		useQuoteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				checkStatus();
			}
		});

		quoteTypeRadioButtonGroup = new RadioButtonGroup(quoteComposite, SWT.NONE, ArrangementStyle.Horizontal, LangResources.get("csvSingleQuote"), LangResources.get("csvDoubleQuote"));
		quoteTypeRadioButtonGroup.setSelectedButtonIndex(stringQuote == '\'' ? 0 : 1);

		if (showContainsHeadersInFirstRowButton) {
			final Button containsHeadersInFirstRowButton = new Button(quoteComposite, SWT.CHECK);
			containsHeadersInFirstRowButton.setText(LangResources.get("containsHeadersInFirstRow"));
			containsHeadersInFirstRowButton.setSelection(containsHeadersInFirstRow);
			containsHeadersInFirstRowButton.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false, 3, 1));
			containsHeadersInFirstRowButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					containsHeadersInFirstRow = ((Button) event.widget).getSelection();
					checkStatus();
				}
			});
		}

		underfilledLinesRadioButtonGroup = new RadioButtonGroup(shell, LangResources.get("csvUnderfilledLines"), SWT.NONE, ArrangementStyle.Horizontal, LangResources.get("yes"),
				LangResources.get("no"));
		underfilledLinesRadioButtonGroup.setSelectedButtonIndex(allowUnderfilledLines ? 0 : 1);

		if (showEncodingField) {
			final Composite encodingComposite = new Composite(shell, SWT.NONE);
			encodingComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
			encodingComposite.setLayout(SwtUtilities.createNoMarginGridLayout(2, false));

			final Label encodingLabel = new Label(encodingComposite, SWT.NONE);
			encodingLabel.setText(LangResources.get("encoding") + ": ");

			// Editable like the former SWT Combo: common encodings as presets, any supported charset name may be typed in.
			// Case-insensitive, because charset names are case-insensitive as well.
			encodingDropDown = new DropDown(encodingComposite, SWT.NONE);
			encodingDropDown.setCaseSensitive(false);
			encodingDropDown.setMatchMode(DropDown.MatchMode.CONTAINS);
			encodingDropDown.setAllowCustomValues(true);
			encodingDropDown.setItems(List.of(StandardCharsets.UTF_8.name(), StandardCharsets.ISO_8859_1.name(), "ISO-8859-15"));
			encodingDropDown.setText(encoding.name());
			// The typed charset name is only evaluated for the OK button status here,
			// "encoding" itself is only taken over when OK is pressed
			encodingDropDown.addListener(SWT.Modify, event -> checkStatus());
			encodingDropDown.addListener(SWT.Selection, event -> checkStatus());
		}

		final Composite mainButtonComposite = new Composite(shell, SWT.NONE);
		mainButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		mainButtonComposite.setLayout(SwtUtilities.createNoMarginGridLayout(2, true));

		okButton = new Button(mainButtonComposite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(LangResources.get("ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				separator = separatorRadioButtonGroup.getSelectedButtonIndex() == 0 ? ',' : ';';
				useStringQuote = useQuoteButton.getSelection();
				stringQuote = quoteTypeRadioButtonGroup.getSelectedButtonIndex() == 0 ? '\'' : '"';
				allowUnderfilledLines = underfilledLinesRadioButtonGroup.getSelectedButtonIndex() == 0;
				if (encodingDropDown != null) {
					final Charset selectedEncoding = getSupportedCharset(encodingDropDown.getText());
					if (selectedEncoding == null) {
						// Should not happen, because OK is disabled for unsupported encodings
						return;
					}
					encoding = selectedEncoding;
				}
				setReturnValue(true);
				getParent().close();
			}
		});

		final Button cancelButton = new Button(mainButtonComposite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
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

		if (okButton != null) {
			okButton.setEnabled(encodingDropDown == null || getSupportedCharset(encodingDropDown.getText()) != null);
		}
	}

	/**
	 * Returns the charset for the given name, or null if the name is blank, illegal or not supported by this JVM.
	 * Replaces the former direct Charset.forName() call, which threw on every incomplete name while typing (e.g. "UTF-").
	 */
	private static Charset getSupportedCharset(final String charsetName) {
		if (Utilities.isBlank(charsetName)) {
			return null;
		}
		try {
			return Charset.forName(charsetName.trim());
		} catch (@SuppressWarnings("unused") final IllegalArgumentException e) {
			// Covers IllegalCharsetNameException and UnsupportedCharsetException
			return null;
		}
	}

	public void setSeparator(final char value) {
		separator = value;
	}

	public char getSeparator() {
		return separator;
	}

	public void setStringQuote(final char value) {
		stringQuote = value;
	}

	public char getStringQuote() {
		return stringQuote;
	}

	public void setUseStringQuote(final boolean value) {
		useStringQuote = value;
	}

	public boolean getUseStringQuote() {
		return useStringQuote;
	}

	public void setAllowUnderfilledLines(final boolean value) {
		allowUnderfilledLines = value;
	}

	public boolean getAllowUnderfilledLines() {
		return allowUnderfilledLines;
	}

	public void setShowContainsHeadersInFirstRowButton(final boolean value) {
		showContainsHeadersInFirstRowButton = value;
	}

	public void setContainsHeadersInFirstRow(final boolean value) {
		containsHeadersInFirstRow = value;
	}

	public boolean isContainsHeadersInFirstRow() {
		return containsHeadersInFirstRow;
	}

	public void setShowEncodingField(final boolean value) {
		showEncodingField = value;
	}

	public void setEncoding(final Charset value) {
		encoding = value;
	}

	public Charset getEncoding() {
		return encoding;
	}
}
