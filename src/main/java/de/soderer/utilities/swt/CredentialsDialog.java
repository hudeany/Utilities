package de.soderer.utilities.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import de.soderer.utilities.Credentials;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

public class CredentialsDialog extends ModalDialog<Credentials> {
	private final String text;
	private final String text_Username;
	private final String text_Password;
	private final String text_OK;
	private final String text_Cancel;

	private boolean enableNoPassword = false;
	private String enableNoPasswordButtonText;
	private Button enableNoPasswordButton;

	private String rememberCredentialsText = null;

	private Button okButton;

	private final boolean aquireUsername;
	private final boolean aquirePassword;

	private String username = null;
	private char[] password = null;

	private boolean rememberCredentials = false;

	public CredentialsDialog(final Shell shell, final String title, final String text, final boolean aquireUsername, final boolean aquirePassword) {
		this(SwtUtilities.createCenteredShellIfNeeded(shell), title, text, aquireUsername, aquirePassword, LangResources.get("username"), LangResources.get("password"), LangResources.get("ok"), LangResources.get("cancel"));
	}

	public CredentialsDialog(final Shell shell, final String title, final String text, final boolean aquireUsername, final boolean aquirePassword,
			final String text_Username, final String text_Password, final String text_OK, final String text_Cancel) {
		super(shell, title);

		this.text = text;
		this.text_Username = text_Username;
		this.text_Password = text_Password;
		this.text_OK = text_OK;
		this.text_Cancel = text_Cancel;

		this.aquireUsername = aquireUsername;
		this.aquirePassword = aquirePassword;
	}

	@Override
	protected void createComponents(final Shell shell) throws Exception {
		shell.setLayout(new GridLayout(1, false));

		if (Utilities.isNotBlank(text)) {
			final StyledText styledText = new StyledText(shell, SWT.NONE);
			styledText.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, true, 2, 1));
			styledText.setLayout(SwtUtilities.createSmallMarginGridLayout(1, false));
			styledText.setText(text);
			styledText.setBackground(shell.getBackground());
			styledText.setEditable(false);
		}

		final Composite credentialsComposite = new Composite(shell, SWT.NONE);
		credentialsComposite.setLayout(new GridLayout(2, false));

		if (aquireUsername) {
			final Label usernameLabel = new Label(credentialsComposite, SWT.NONE);
			usernameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			usernameLabel.setText(text_Username);

			final Text usernameTextField = new Text(credentialsComposite, SWT.BORDER);

			final GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1);
			gridData.widthHint = 200;
			usernameTextField.setLayoutData(gridData);

			usernameTextField.setText("");
			usernameTextField.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent event) {
					username = ((Text) event.widget).getText();
					checkButtonStatus();
				}
			});
			usernameTextField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(final KeyEvent event) {
					if (event.keyCode == SWT.CR || event.keyCode == SwtUtilities.BOTTOM_RIGHT_ENTER_KEY) {
						setReturnValue(getCredentials());
						getParent().close();
					}
				}
			});
			usernameTextField.forceFocus();
		}

		if (aquirePassword) {
			final Label passwordLabel = new Label(credentialsComposite, SWT.NONE);
			passwordLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
			passwordLabel.setText(text_Password);

			final Text passwordTextField = new Text(credentialsComposite, SWT.BORDER | SWT.PASSWORD);

			final GridData gridData = new GridData(SWT.LEFT, SWT.CENTER, true, true, 1, 1);
			gridData.widthHint = 200;
			passwordTextField.setLayoutData(gridData);

			passwordTextField.setText("");
			password = "".toCharArray();
			passwordTextField.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent event) {
					password = ((Text) event.widget).getTextChars();
					checkButtonStatus();
				}
			});
			passwordTextField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyReleased(final KeyEvent event) {
					if (event.keyCode == SWT.CR || event.keyCode == SwtUtilities.BOTTOM_RIGHT_ENTER_KEY) {
						setReturnValue(getCredentials());
						getParent().close();
					}
				}
			});
			if (!aquireUsername) {
				passwordTextField.forceFocus();
			}
		}

		if (Utilities.isNotBlank(rememberCredentialsText)) {
			final Button rememberCredentialsButton = new Button(shell, SWT.CHECK);
			rememberCredentialsButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
			rememberCredentialsButton.setText(rememberCredentialsText);
			rememberCredentialsButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					rememberCredentials = ((Button) event.widget).getSelection();
				}
			});
		}

		final Composite mainButtonComposite = new Composite(shell, SWT.NONE);
		mainButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		mainButtonComposite.setLayout(SwtUtilities.createNoMarginGridLayout(enableNoPassword ? 3 : 2, true));

		if (enableNoPassword) {
			enableNoPasswordButton = new Button(mainButtonComposite, SWT.PUSH);
			enableNoPasswordButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
			enableNoPasswordButton.setText(enableNoPasswordButtonText);
			enableNoPasswordButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent event) {
					setReturnValue(getCredentials().setPassword(null));
					getParent().close();
				}
			});
		}

		okButton = new Button(mainButtonComposite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(text_OK);
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				setReturnValue(getCredentials());
				getParent().close();
			}
		});

		final Button cancelButton = new Button(mainButtonComposite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(text_Cancel);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent event) {
				setReturnValue(null);
				getParent().close();
			}
		});

		shell.pack();

		checkButtonStatus();
	}

	private void checkButtonStatus() {
		okButton.setEnabled(!aquireUsername || Utilities.isNotEmpty(username));
	}

	public CredentialsDialog setRememberCredentialsText(final String rememberCredentialsText) {
		this.rememberCredentialsText = rememberCredentialsText;
		return this;
	}

	public CredentialsDialog setRememberCredentials(final boolean rememberCredentials) {
		this.rememberCredentials = rememberCredentials;
		return this;
	}

	public boolean isRememberCredentials() {
		return rememberCredentials;
	}

	public Credentials getCredentials() {
		if (aquireUsername && aquirePassword) {
			if (username != null) {
				return new Credentials(username, password);
			} else {
				return null;
			}
		} else if (aquireUsername) {
			if (username != null) {
				return new Credentials(username);
			} else {
				return null;
			}
		} else if (aquirePassword) {
			return new Credentials(password);
		} else {
			return null;
		}
	}

	public CredentialsDialog enableNoPassword(final boolean enable) {
		enableNoPassword(enable, LangResources.get("noPassword"));
		return this;
	}

	public CredentialsDialog enableNoPassword(final boolean enable, final String enableNoPasswordButtonTextParam) {
		enableNoPassword = enable;
		enableNoPasswordButtonText = enableNoPasswordButtonTextParam;
		return this;
	}
}
