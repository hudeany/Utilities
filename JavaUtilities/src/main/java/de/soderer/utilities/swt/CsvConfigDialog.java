package de.soderer.utilities.swt;

import org.apache.commons.lang3.StringUtils;
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

public class CsvConfigDialog extends ModalDialog<Boolean> {
	private String title = null;
	private String text = null;

	private char separator;
	private char stringQuote;
	private boolean useStringQuote;
	private boolean allowUnderfilledLines;
	
	private Button comaButton;
	private Button semicolonButton;
	private Button useQuoteButton;
	private Button singleQuoteButton;
	private Button doubleQuoteButton;
	private Button underfilledLinesAllowedButton;
	private Button underfilledLinesNotAllowedButton;
	
	public CsvConfigDialog(String title, String text, Shell shell) {
		super(shell);
		this.title = title;
		this.text = text;
	}
	
	public CsvConfigDialog(String text, Shell shell) {
		super(shell);
		this.text = text;
	}
	
	public CsvConfigDialog(Shell shell) {
		super(shell);
	}
	
	@Override
	protected void createComponents(Shell shell) throws Exception {
		if (StringUtils.isEmpty(title)) {
			shell.setText(getText());
		} else {
			shell.setText(title);
		}
		shell.setLayout(new GridLayout(1, true));
		
		if (StringUtils.isNotBlank(text)) {
			Label label = new Label(shell, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false, 1, 1));
			label.setText(text);
		}

		Composite separatorComposite = new Composite(shell, SWT.NONE);
		separatorComposite.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false, 1, 1));
		separatorComposite.setLayout(new GridLayout(3, false));
		
		Label separatorLabel = new Label(separatorComposite, SWT.NONE);
		separatorLabel.setText(LangResources.get("csvSeparator") + ": ");
		
		comaButton = new Button(separatorComposite, SWT.RADIO);
		comaButton.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false));
		comaButton.setText(LangResources.get("csvSeparatorComa"));
		
		semicolonButton = new Button(separatorComposite, SWT.RADIO);
		semicolonButton.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false));
		semicolonButton.setText(LangResources.get("csvSeparatorSemicolon"));
		
		Composite quoteComposite = new Composite(shell, SWT.NONE);
		quoteComposite.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false, 1, 1));
		quoteComposite.setLayout(new GridLayout(3, false));
		
		useQuoteButton = new Button(quoteComposite, SWT.CHECK);
		useQuoteButton.setText(LangResources.get("csvUseQuote") + ": ");
		useQuoteButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				checkStatus();
			}
		});
		
		singleQuoteButton = new Button(quoteComposite, SWT.RADIO);
		singleQuoteButton.setLayoutData(new GridData(SWT.LEFT, SWT.UP, true, false));
		singleQuoteButton.setText(LangResources.get("csvSingleQuote"));
		singleQuoteButton.setSelection(true);
		
		doubleQuoteButton = new Button(quoteComposite, SWT.RADIO);
		doubleQuoteButton.setLayoutData(new GridData(SWT.LEFT, SWT.UP, true, false));
		doubleQuoteButton.setText(LangResources.get("csvDoubleQuote"));
		
		Composite underfilledComposite = new Composite(shell, SWT.NONE);
		underfilledComposite.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false, 1, 1));
		underfilledComposite.setLayout(new GridLayout(3, false));
		
		Label underfilledLinesLabel = new Label(underfilledComposite, SWT.NONE);
		underfilledLinesLabel.setText(LangResources.get("csvUnderfilledLines") + ": ");
		
		underfilledLinesAllowedButton = new Button(underfilledComposite, SWT.RADIO);
		underfilledLinesAllowedButton.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false));
		underfilledLinesAllowedButton.setText(LangResources.get("yes"));
		
		underfilledLinesNotAllowedButton = new Button(underfilledComposite, SWT.RADIO);
		underfilledLinesNotAllowedButton.setLayoutData(new GridData(SWT.LEFT, SWT.UP, false, false));
		underfilledLinesNotAllowedButton.setText(LangResources.get("no"));
		underfilledLinesNotAllowedButton.setSelection(true);
		
		Composite mainButtonComposite = new Composite(shell, SWT.NONE);
		mainButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 1, 1));
		mainButtonComposite.setLayout(new GridLayout(2, true));

		Button okButton = new Button(mainButtonComposite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(LangResources.get("ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				separator = comaButton.getSelection() ? ',' : ';';
				useStringQuote = useQuoteButton.getSelection();
				stringQuote = singleQuoteButton.getSelection() ? '\'' : '"';
				allowUnderfilledLines = underfilledLinesAllowedButton.getSelection();
				setReturnValue(true);
				getShell().close();
			}
		});
	    
		Button cancelButton = new Button(mainButtonComposite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(false);
				getShell().close();
			}
		});

		shell.pack();

		shell.setSize(shell.getSize().x, shell.getSize().y);
		shell.setMinimumSize(shell.getSize().x, shell.getSize().y);
	    
	    checkStatus();
	}

	private void checkStatus() {
		singleQuoteButton.setEnabled(useQuoteButton.getSelection());
		doubleQuoteButton.setEnabled(useQuoteButton.getSelection());
	}

	public char getSeparator() {
		return separator;
	}

	public char getStringQuote() {
		return stringQuote;
	}

	public boolean getUseStringQuote() {
		return useStringQuote;
	}

	public boolean getAllowUnderfilledLines() {
		return allowUnderfilledLines;
	}
}
