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
	private String text = null;
	private String username = "";
	private char[] password = "".toCharArray();
	private String keepInMemoryText = null;
	private boolean keepInMemory = false;
	
	public CredentialsDialog(Shell shell, String title, String text) {
		super(shell, title);
		this.text = text;
	}

	public void setKeepInMemoryText(String keepInMemoryText) {
		this.keepInMemoryText = keepInMemoryText;
	}
	
	@Override
	protected void createComponents(Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, true));
		
		if (StringUtils.isNotBlank(text)) {
			Label label = new Label(shell, SWT.NONE);
			label.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
			label.setText(text);
		}
		
		Label usernameLabel = new Label(shell, SWT.NONE);
		usernameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
		usernameLabel.setText(LangResources.get("username"));
		
		Text usernameTextField = new Text(shell, SWT.BORDER);
		usernameTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
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
					getParent().close();
				}
			}
		});
		
		Label passwordLabel = new Label(shell, SWT.NONE);
		passwordLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true));
		passwordLabel.setText(LangResources.get("password"));
		
		Text passwordTextField = new Text(shell, SWT.BORDER | SWT.PASSWORD);
		passwordTextField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		passwordTextField.setText("");
		passwordTextField.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				password = ((Text) e.widget).getTextChars();
			}
		});
		passwordTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent event) {
				if (event.keyCode == SWT.CR || event.keyCode == SwtUtilities.BOTTOM_RIGHT_ENTER_KEY) {
					setReturnValue(true);
					getParent().close();
				}
			}
		});
		
		if (StringUtils.isNotBlank(keepInMemoryText)) {
			Button keepInMemoryButton = new Button(shell, SWT.CHECK);
			keepInMemoryButton.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
			keepInMemoryButton.setText(keepInMemoryText);
			keepInMemoryButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					keepInMemory = ((Button) e.widget).getSelection();
				}
			});
		}
		
		SwtUtilities.addMainButtonComposite(shell, this);

		shell.pack();
	}
	
	public String getUsername() {
		return username;
	}

	public char[] getPassword() {
		return password;
	}

	public boolean keepInMemory() {
		return keepInMemory;
	}
}
