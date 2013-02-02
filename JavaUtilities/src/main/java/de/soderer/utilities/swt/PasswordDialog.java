package de.soderer.utilities.swt;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class PasswordDialog extends ModalDialog<String> {
	private String title;
	private String text;
	private Text textField;
	private String currentText = "";
	private int currentPosition = 0;
	private String[] buttonTexts;
	private Pattern inputPattern = null;
	private boolean isTechnicalChange = false;

	public PasswordDialog(String title, String text, Shell shell, String buttonTextOk, String buttonTextCancel) {   
		super(shell);
		this.title = title;
		this.text = text;
		this.buttonTexts = new String[]{buttonTextOk, buttonTextCancel};
	}
	
	public PasswordDialog(String text, Shell shell, String buttonTextOk, String buttonTextCancel) {   
		super(shell);
		this.text = text;
		this.buttonTexts = new String[]{buttonTextOk, buttonTextCancel};
	}
	
	public void setInputPattern(String patternString) {
		if (StringUtils.isBlank(patternString)) {
			inputPattern = null;
		} else {
			inputPattern = Pattern.compile(patternString);
		}
	}
	
	@Override
	protected void createComponents(Shell shell) throws Exception {
		if (StringUtils.isEmpty(title)) {
			shell.setText(getText());
		} else {
			shell.setText(title);
		}
		shell.setLayout(new GridLayout(buttonTexts.length, true));
		
		Label label = new Label(shell, SWT.NONE);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, buttonTexts.length, 1));
		label.setText(text);
		
		textField = new Text(shell, SWT.BORDER | SWT.PASSWORD);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, buttonTexts.length, 1));
		textField.setText("");
		textField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (!isTechnicalChange) {
					if (inputPattern == null || inputPattern.matcher(textField.getText()).matches()) {
						currentText = textField.getText();
					} else {
						currentPosition = textField.getCaretPosition();
						isTechnicalChange = true;
						textField.setText(currentText);
						textField.setSelection(Math.min(currentText.length(), Math.max(0, currentPosition - 1)));
						isTechnicalChange = false;
					}
				}
			}
		});
		textField.addKeyListener(new KeyListener() {
			@Override
			public void keyReleased(KeyEvent event) {
				if (event.keyCode == 13) {
					setReturnValue(textField.getText());
					getShell().close();
				}
			}
			
			@Override
			public void keyPressed(KeyEvent event) {
			}
		});
	    
		for (int i = 0; i < buttonTexts.length; i++) {
			String buttonText = buttonTexts[i];
			final int buttonIndex = i; 
			Button button = new Button(shell, SWT.PUSH);
		    button.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		    button.setText(buttonText);
		    button.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					if (buttonIndex == 0) {
						setReturnValue(textField.getText());
					} else {
						setReturnValue(null);
					}
					getShell().close();
				}
			});
		}

		shell.pack();
	}
}
