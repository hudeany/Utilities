package de.soderer.utilities.swt;

import java.text.MessageFormat;
import java.util.Locale;

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

	private String rememberCredentialsText = null;

	private Button okButton;

	private final boolean aquireUsername;
	private final boolean aquirePassword;

	private String username = null;
	private char[] password = null;

	private boolean rememberCredentials = false;

	public CredentialsDialog(final Shell shell, final String title, final String text, final boolean aquireUsername, final boolean aquirePassword) {
		this(shell, title, text, aquireUsername, aquirePassword, getI18NString("username"), getI18NString("password"), getI18NString("ok"), getI18NString("cancel"));
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
		shell.setLayout(new GridLayout(1, true));

		if (Utilities.isNotBlank(text)) {
			final Label label = new Label(shell, SWT.NONE);
			label.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
			label.setText(text);
		}

		if (aquireUsername) {
			final Label usernameLabel = new Label(shell, SWT.NONE);
			usernameLabel.setLayoutData(new GridData(SWT.LEFT, SWT.UP, true, true));
			usernameLabel.setText(text_Username);

			final Text usernameTextField = new Text(shell, SWT.BORDER);
			usernameTextField.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
			usernameTextField.setText("");
			usernameTextField.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					username = ((Text) e.widget).getText();
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
		}

		if (aquirePassword) {
			final Label passwordLabel = new Label(shell, SWT.NONE);
			passwordLabel.setLayoutData(new GridData(SWT.LEFT, SWT.UP, true, true));
			passwordLabel.setText(text_Password);

			final Text passwordTextField = new Text(shell, SWT.BORDER | SWT.PASSWORD);
			passwordTextField.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
			passwordTextField.setText("");
			passwordTextField.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(final ModifyEvent e) {
					password = ((Text) e.widget).getTextChars();
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
		}

		if (Utilities.isNotBlank(rememberCredentialsText)) {
			final Button rememberCredentialsButton = new Button(shell, SWT.CHECK);
			rememberCredentialsButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, true));
			rememberCredentialsButton.setText(rememberCredentialsText);
			rememberCredentialsButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(final SelectionEvent e) {
					rememberCredentials = ((Button) e.widget).getSelection();
				}
			});
		}

		final Composite mainButtonComposite = new Composite(shell, SWT.NONE);
		mainButtonComposite.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		mainButtonComposite.setLayout(SwtUtilities.createNoMarginGridLayout(2, true));

		okButton = new Button(mainButtonComposite, SWT.PUSH);
		okButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		okButton.setText(text_OK);
		okButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				setReturnValue(getCredentials());
				getParent().close();
			}
		});

		final Button cancelButton = new Button(mainButtonComposite, SWT.PUSH);
		cancelButton.setLayoutData(new GridData(SWT.FILL, SWT.UP, true, false));
		cancelButton.setText(text_Cancel);
		cancelButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
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

	private static String getI18NString(final String resourceKey, final Object... arguments) {
		if (LangResources.existsKey(resourceKey)) {
			return LangResources.get(resourceKey, arguments);
		} else if ("de".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			String pattern;
			switch(resourceKey) {
				case "username": pattern = "Username"; break;
				case "password": pattern = "Passwort"; break;
				case "ok": pattern = "OK"; break;
				case "cancel": pattern = "Abbrechen"; break;
				default: pattern = "MessageKey unbekannt: " + resourceKey + (arguments != null && arguments.length > 0 ? " Argumente: " + Utilities.join(arguments, ", ") : "");
			}
			return MessageFormat.format(pattern, arguments);
		} else {
			String pattern;
			switch(resourceKey) {
				case "username": pattern = "Username"; break;
				case "password": pattern = "Passwort"; break;
				case "ok": pattern = "OK"; break;
				case "cancel": pattern = "Cancel"; break;
				default: pattern = "MessageKey unknown: " + resourceKey + (arguments != null && arguments.length > 0 ? " arguments: " + Utilities.join(arguments, ", ") : "");
			}
			return MessageFormat.format(pattern, arguments);
		}
	}
}
