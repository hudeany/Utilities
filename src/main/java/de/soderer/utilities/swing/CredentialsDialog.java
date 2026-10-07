package de.soderer.utilities.swing;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import de.soderer.utilities.Credentials;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

/**
 * Modal dialog asking for a username and/or a password.
 *
 * <p>
 * {@link #open()} returns the entered credentials, or null if the dialog was
 * canceled. The OK button is only enabled while a username is entered (if a
 * username is requested). An optional checkbox lets the user choose to remember
 * the credentials (see {@link #isRememberCredentials()}), and an optional "no
 * password" button returns the credentials without password.
 * </p>
 */
public class CredentialsDialog extends ModalDialog<Credentials> {
	private static final long serialVersionUID = 2020023796052296409L;

	/** Text shown above the input fields, may be blank */
	private final String text;

	/** Label of the username field */
	private final String text_Username;

	/** Label of the password field */
	private final String text_Password;

	/** Text of the OK button */
	private final String text_OK;

	/** Text of the cancel button */
	private final String text_Cancel;

	/** Whether a "no password" button is shown */
	private boolean enableNoPassword = false;

	/** Text of the "no password" button */
	private String enableNoPasswordButtonText;

	/** Button closing the dialog and returning the credentials without password */
	private JButton enableNoPasswordButton;

	/** Text of the "remember credentials" checkbox, blank for no checkbox */
	private String rememberCredentialsText = null;

	/** Content panel, rebuilt whenever an option affecting the layout changes */
	private JPanel panel;

	/** OK button, only enabled while a username is entered (if a username is requested) */
	private JButton okButton;

	/** Whether the dialog asks for a username */
	private final boolean aquireUsername;

	/** Whether the dialog asks for a password */
	private final boolean aquirePassword;

	/** Currently entered username, null if no username is requested */
	private String username = null;

	/** Currently entered password, null if no password is requested (transient: never serialized) */
	private transient char[] password = null;

	/** Input field of the username, null if no username is requested */
	private JTextField userField;

	/** Input field of the password, null if no password is requested */
	private JPasswordField passwordField;

	/** Status of the "remember credentials" checkbox */
	private boolean rememberCredentials = false;

	/**
	 * Creates a credentials dialog with the default texts of the language
	 * resources.
	 *
	 * @param parent owner window, may be null
	 * @param title title of the dialog
	 * @param text text shown above the input fields, may be blank
	 * @param aquireUsername true to ask for a username
	 * @param aquirePassword true to ask for a password
	 */
	public CredentialsDialog(final Window parent, final String title, final String text, final boolean aquireUsername, final boolean aquirePassword) {
		this(parent, title, text, aquireUsername, aquirePassword, LangResources.get("username"), LangResources.get("password"), LangResources.get("ok"), LangResources.get("cancel"));
	}

	/**
	 * Creates a credentials dialog with custom label and button texts.
	 *
	 * @param parent owner window, may be null
	 * @param title title of the dialog
	 * @param text text shown above the input fields, may be blank
	 * @param aquireUsername true to ask for a username
	 * @param aquirePassword true to ask for a password
	 * @param text_Username label of the username field
	 * @param text_Password label of the password field
	 * @param text_OK text of the OK button
	 * @param text_Cancel text of the cancel button
	 */
	public CredentialsDialog(final Window parent, final String title, final String text, final boolean aquireUsername, final boolean aquirePassword,
			final String text_Username, final String text_Password, final String text_OK, final String text_Cancel) {
		super(parent, title);

		this.text = text;
		this.text_Username = text_Username;
		this.text_Password = text_Password;
		this.text_OK = text_OK;
		this.text_Cancel = text_Cancel;

		this.aquireUsername = aquireUsername;
		this.aquirePassword = aquirePassword;

		setResizable(false);

		createComponents(this);
	}

	private void createComponents(@SuppressWarnings("unused") final CredentialsDialog credentialsDialog) {
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		panel.add(Box.createRigidArea(new Dimension(0, 5)));

		if (Utilities.isNotBlank(text)) {
			final JTextArea textField = new JTextArea();
			final Font f = textField.getFont();
			final Font f2 = new Font("Arial", f.getStyle(), f.getSize());
			textField.setFont(f2);
			textField.setText(text);
			textField.setEditable(false);
			textField.setBackground(panel.getBackground());

			final JScrollPane textScrollpane = new JScrollPane(textField);
			textScrollpane.setBorder(BorderFactory.createEmptyBorder());
			textScrollpane.setPreferredSize(new Dimension(textField.getPreferredSize().width + 5, textField.getPreferredSize().height + 5));

			// Text Panel
			final JPanel textPanel = new JPanel();
			textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.LINE_AXIS));

			textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
			textPanel.add(textScrollpane);
			textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
			panel.add(textPanel);
		}

		if (aquireUsername) {
			// User Panel
			final JPanel userPanel = new JPanel();
			userPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			final JLabel userLabel = new JLabel(text_Username);
			userPanel.add(userLabel);
			userField = new JTextField();
			userField.setPreferredSize(new Dimension(200, userField.getPreferredSize().height));
			userField.setText("");
			username = "";
			// A DocumentListener also covers input that is not typed, like pasting via mouse or drag and drop
			userField.getDocument().addDocumentListener(new FieldChangeListener() {
				@Override
				protected void fieldChanged() {
					username = userField.getText();
					checkButtonStatus();
				}
			});
			userPanel.add(userField);
			panel.add(userPanel);
		}

		if (aquirePassword) {
			// Password Panel
			final JPanel passwordPanel = new JPanel();
			passwordPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
			final JLabel passwordLabel = new JLabel(text_Password);
			passwordPanel.add(passwordLabel);
			passwordField = new JPasswordField();
			passwordField.setPreferredSize(new Dimension(200, passwordField.getPreferredSize().height));
			passwordField.setText("");
			password = "".toCharArray();
			passwordField.getDocument().addDocumentListener(new FieldChangeListener() {
				@Override
				protected void fieldChanged() {
					// getPassword() avoids an immutable String copy of the password
					password = passwordField.getPassword();
					checkButtonStatus();
				}
			});
			passwordPanel.add(passwordField);
			panel.add(passwordPanel);
		}

		if (Utilities.isNotBlank(rememberCredentialsText)) {
			// RememberCredentials Panel
			final JPanel rememberCredentialsPanel = new JPanel();
			rememberCredentialsPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			final JCheckBox rememberCredentialsCheckBox = new JCheckBox(rememberCredentialsText);
			rememberCredentialsCheckBox.setSelected(rememberCredentials);
			rememberCredentialsCheckBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent event) {
					rememberCredentials = rememberCredentialsCheckBox.isSelected();
				}
			});
			rememberCredentialsPanel.add(rememberCredentialsCheckBox);
			panel.add(rememberCredentialsPanel);
		}

		// Button Panel
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

		if (enableNoPassword) {
			enableNoPasswordButton = new JButton(enableNoPasswordButtonText);
			enableNoPasswordButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent event) {
					final Credentials credentials = getCredentials();
					returnValue = credentials == null ? new Credentials() : credentials.setPassword(null);
					dispose();
				}
			});
			buttonPanel.add(enableNoPasswordButton);
		}

		// ok Button
		okButton = new JButton(text_OK);
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				returnValue = getCredentials();
				dispose();
			}
		});
		buttonPanel.add(okButton);

		buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));

		// Close Button
		final JButton closeButton = new JButton(text_Cancel);
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				returnValue = null;
				dispose();
			}
		});
		buttonPanel.add(closeButton);

		panel.add(Box.createRigidArea(new Dimension(5, 0)));
		panel.add(buttonPanel);
		getRootPane().setDefaultButton(okButton);

		panel.add(Box.createRigidArea(new Dimension(0, 5)));

		add(panel);

		pack();

		setLocationRelativeTo(getOwner());

		if (userField != null) {
			userField.requestFocus();
		} else if (passwordField != null) {
			passwordField.requestFocus();
		}

		checkButtonStatus();
	}

	private void checkButtonStatus() {
		okButton.setEnabled(!aquireUsername || Utilities.isNotEmpty(username));
	}

	/**
	 * Sets the text of a "remember credentials" checkbox.
	 *
	 * @param rememberCredentialsText text of the checkbox, blank for no checkbox
	 * @return this dialog for chaining
	 */
	public CredentialsDialog setRememberCredentialsText(final String rememberCredentialsText) {
		this.rememberCredentialsText = rememberCredentialsText;
		remove(panel);
		createComponents(this);
		return this;
	}

	/**
	 * Sets the initial status of the "remember credentials" checkbox.
	 *
	 * @param rememberCredentials true to check the checkbox initially
	 * @return this dialog for chaining
	 */
	public CredentialsDialog setRememberCredentials(final boolean rememberCredentials) {
		this.rememberCredentials = rememberCredentials;
		remove(panel);
		createComponents(this);
		return this;
	}

	/**
	 * Returns the status of the "remember credentials" checkbox, also after the
	 * dialog was closed.
	 *
	 * @return true if the user wants the credentials to be remembered
	 */
	public boolean isRememberCredentials() {
		return rememberCredentials;
	}

	/**
	 * Returns the currently entered credentials.
	 *
	 * @return credentials with the requested parts (username and/or password),
	 *         null if neither username nor password is requested
	 */
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

	/**
	 * Shows or hides a "no password" button with the default text of the language
	 * resources. The button closes the dialog and returns the credentials without
	 * password.
	 *
	 * @param enable true to show the button
	 * @return this dialog for chaining
	 */
	public CredentialsDialog enableNoPassword(final boolean enable) {
		enableNoPassword(enable, LangResources.get("noPassword"));
		return this;
	}

	/**
	 * Shows or hides a "no password" button. The button closes the dialog and
	 * returns the credentials without password.
	 *
	 * @param enable true to show the button
	 * @param enableNoPasswordButtonTextParam text of the button
	 * @return this dialog for chaining
	 */
	public CredentialsDialog enableNoPassword(final boolean enable, final String enableNoPasswordButtonTextParam) {
		enableNoPassword = enable;
		enableNoPasswordButtonText = enableNoPasswordButtonTextParam;
		// Rebuild the components, otherwise the button would only appear if another setter is called afterwards
		remove(panel);
		createComponents(this);
		return this;
	}

	/**
	 * Calls {@link #fieldChanged()} on every change of a text field's document.
	 */
	private abstract static class FieldChangeListener implements DocumentListener {
		protected abstract void fieldChanged();

		@Override
		public void insertUpdate(final DocumentEvent event) {
			fieldChanged();
		}

		@Override
		public void removeUpdate(final DocumentEvent event) {
			fieldChanged();
		}

		@Override
		public void changedUpdate(final DocumentEvent event) {
			// Attribute changes only, no text change
		}
	}
}
