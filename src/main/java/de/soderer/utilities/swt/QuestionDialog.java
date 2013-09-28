package de.soderer.utilities.swt;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

import de.soderer.utilities.LangResources;

public class QuestionDialog extends ModalDialog<Integer> {
	private String title;
	private String text;
	private String rememberAnswerText = null;
	private boolean rememberAnswer = false;
	private String[] buttonTexts;

	public QuestionDialog(String title, String text, Shell shell, String... buttonTexts) {   
		super(shell);
		this.title = title;
		this.text = text;
		this.buttonTexts = buttonTexts;
		
		if (buttonTexts == null || buttonTexts.length == 0) {
			this.buttonTexts = new String[]{ LangResources.get("ok") };
		}
	}
	
	public QuestionDialog(String text, Shell shell, String... buttonTexts) {   
		super(shell);
		this.text = text;
		this.buttonTexts = buttonTexts;
		
		if (buttonTexts == null || buttonTexts.length == 0) {
			this.buttonTexts = new String[]{ LangResources.get("ok") };
		}
	}
	
	@Override
	protected void createComponents(Shell shell) throws Exception {
		if (StringUtils.isEmpty(title)) {
			shell.setText(getText());
		} else {
			shell.setText(title);
		}
		shell.setLayout(new FillLayout());
		
		Composite composite = new Composite(shell, SWT.NONE);
		composite.setLayout(new GridLayout(buttonTexts.length, true));
		
		Label label = new Label(composite, SWT.FILL);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, buttonTexts.length, 1));
		label.setText(text);
		
		if (StringUtils.isNotBlank(rememberAnswerText)) {
			Button rememberAnswerCheckBox = new Button(composite, SWT.CHECK);
			rememberAnswerCheckBox.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, buttonTexts.length, 1));
			rememberAnswerCheckBox.setSelection(false);
			rememberAnswerCheckBox.setText(rememberAnswerText);
			rememberAnswerCheckBox.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					rememberAnswer = ((Button) e.widget).getSelection();
				}
			});
		}
	    
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
						getShell().close();
					}
				});
			}
		}

		shell.pack();
	}
	
	public void setRememberAnswerText(String rememberAnswerText) {
		this.rememberAnswerText = rememberAnswerText;
	}
	
	public boolean getRememberAnswerStatus() {
		return rememberAnswer;
	}
}
