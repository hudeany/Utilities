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

public class SimpleInputDialog extends ModalDialog<String> {
	private String text;
	private Text textField;
	private String currentText = "";
	private int currentPosition = 0;
	private Pattern inputPattern = null;
	private boolean isTechnicalChange = false;

	public SimpleInputDialog(Shell shell, String title, String text) {   
		super(shell, title);
		
		this.text = text;
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
		
		textField = new Text(shell, SWT.BORDER);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
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
		textField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				// on Enter-key
				if (e.keyCode == SWT.CR || e.keyCode == SwtUtilities.BOTTOM_RIGHT_ENTER_KEY) {
					setReturnValue(textField.getText());
					getParent().close();
				}
			}
		});
	    
		Button okButton = new Button(shell, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(LangResources.get("ok"));
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setReturnValue(textField.getText());
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
}
