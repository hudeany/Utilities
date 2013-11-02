package de.soderer.utilities.swt;

import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
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

import de.soderer.utilities.LangResources;

public class PasswordDialog extends ModalDialog<char[]> {
	private String text;
	private Text passwordTextField;
	private String currentText = "";
	private int currentPosition = 0;
	private Pattern inputPattern = null;
	private boolean isTechnicalChange = false;
	private String keepInMemoryText = null;
	private boolean keepInMemory = false;
	
	public PasswordDialog(Shell shell, String title, String text) {   
		super(shell, title);
		
		this.text = text;
	}

	public void setKeepInMemoryText(String keepInMemoryText) {
		this.keepInMemoryText = keepInMemoryText;
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
		shell.setLayout(new GridLayout(2, true));
		
		Label label = new Label(shell, SWT.NONE);
		label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
		label.setText(text);
		
		passwordTextField = new Text(shell, SWT.BORDER | SWT.PASSWORD);
		passwordTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
		passwordTextField.setText("");
		passwordTextField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (!isTechnicalChange) {
					if (inputPattern == null || inputPattern.matcher(passwordTextField.getText()).matches()) {
						currentText = passwordTextField.getText();
					} else {
						currentPosition = passwordTextField.getCaretPosition();
						isTechnicalChange = true;
						passwordTextField.setText(currentText);
						passwordTextField.setSelection(Math.min(currentText.length(), Math.max(0, currentPosition - 1)));
						isTechnicalChange = false;
					}
				}
			}
		});
		passwordTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent event) {
				if (event.keyCode == SWT.CR || event.keyCode == SwtUtilities.BOTTOM_RIGHT_ENTER_KEY) {
					setReturnValue(passwordTextField.getTextChars());
					getParent().close();
				}
			}
		});
		
		if (StringUtils.isNotBlank(keepInMemoryText)) {
			Button keepInMemoryButton = new Button(shell, SWT.CHECK);
			keepInMemoryButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
			keepInMemoryButton.setText(keepInMemoryText);
			keepInMemoryButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					keepInMemory = ((Button) e.widget).getSelection();
				}
			});
		}
		
		Button okButton = new Button(shell, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(LangResources.get("ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(passwordTextField.getTextChars());
				getParent().close();
			}
		});
	    
		Button cancelButton = new Button(shell, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(LangResources.get("cancel"));
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(null);
				getParent().close();
			}
		});

		shell.pack();
	}
	
	public boolean keepInMemory() {
		return keepInMemory;
	}
}
