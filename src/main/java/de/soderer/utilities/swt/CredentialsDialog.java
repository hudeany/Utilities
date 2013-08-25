package de.soderer.utilities.swt;

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

public class CredentialsDialog extends ModalDialog<Boolean> {
	private String title = null;
	private String text = null;
	private String username = "";
	private String password = "";
	private String keepInMemoryText = null;
	private boolean keepInMemory = false;

	public CredentialsDialog(String title, String text, Shell shell, String keepInMemoryText) {
		super(shell);
		this.title = title;
		this.text = text;
		this.keepInMemoryText = keepInMemoryText;
	}

	public CredentialsDialog(String text, Shell shell, String keepInMemoryText) {
		super(shell);
		this.text = text;
		this.keepInMemoryText = keepInMemoryText;
	}
	
	public CredentialsDialog(String title, String text, Shell shell) {
		super(shell);
		this.title = title;
		this.text = text;
	}
	
	public CredentialsDialog(String text, Shell shell) {
		super(shell);
		this.text = text;
	}
	
	public CredentialsDialog(Shell shell) {
		super(shell);
	}
	
	@Override
	protected void createComponents(Shell shell) throws Exception {
		if (StringUtils.isEmpty(title)) {
			shell.setText(getText());
		} else {
			shell.setText(title);
		}
		shell.setLayout(new GridLayout(2, true));
		
		if (StringUtils.isNotBlank(text)) {
			Label label = new Label(shell, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
			label.setText(text);
		}
		
		Label usernameLabel = new Label(shell, SWT.NONE);
		usernameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
		usernameLabel.setText(LangResources.get("username"));
		
		Text usernameTextField = new Text(shell, SWT.BORDER);
		usernameTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
		usernameTextField.setText("");
		usernameTextField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				username = ((Text) e.widget).getText();
			}
		});
		usernameTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent event) {
				if (event.keyCode == SWT.CR || event.keyCode == SwtUtilities.BOTTOM_RIGHT_ENTER_KEY) {
					setReturnValue(true);
					getShell().close();
				}
			}
		});
		
		Label passwordLabel = new Label(shell, SWT.NONE);
		passwordLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
		passwordLabel.setText(LangResources.get("password"));
		
		Text passwordTextField = new Text(shell, SWT.BORDER | SWT.PASSWORD);
		passwordTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true, 2, 1));
		passwordTextField.setText("");
		passwordTextField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				password = ((Text) e.widget).getText();
			}
		});
		passwordTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent event) {
				if (event.keyCode == SWT.CR || event.keyCode == SwtUtilities.BOTTOM_RIGHT_ENTER_KEY) {
					setReturnValue(true);
					getShell().close();
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
				setReturnValue(true);
				getShell().close();
			}
		});
	    
		Button cancelButton = new Button(shell, SWT.PUSH);
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
	}
	
	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public boolean keepInMemory() {
		return keepInMemory;
	}
}
