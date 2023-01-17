package de.soderer.utilities.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import java.util.Locale;

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

import de.soderer.utilities.Credentials;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

public class CredentialsDialog extends ModalDialog<Credentials> {
	private static final long serialVersionUID = 2020023796052296409L;

	private final String text;
	private final String text_Username;
	private final String text_Password;
	private final String text_OK;
	private final String text_Cancel;

	private boolean enableNoPassword = false;
	private String enableNoPasswordButtonText;
	private JButton enableNoPasswordButton;

	private String rememberCredentialsText = null;

	private JPanel panel;
	private JButton okButton;

	private final boolean aquireUsername;
	private final boolean aquirePassword;

	private String username = null;
	private char[] password = null;

	private JTextField userField;
	private JTextField passwordField;

	private boolean rememberCredentials = false;

	public CredentialsDialog(final Window parent, final String title, final String text, final boolean aquireUsername, final boolean aquirePassword) {
		this(parent, title, text, aquireUsername, aquirePassword, getI18NString("username"), getI18NString("password"), getI18NString("ok"), getI18NString("cancel"));
	}

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
			textField.setText(text);
			textField.setEditable(false);
			textField.setBackground(Color.LIGHT_GRAY);

			final JScrollPane textScrollpane = new JScrollPane(textField);
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
			userField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(final KeyEvent event) {
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
			passwordField.addKeyListener(new KeyAdapter() {
				@Override
				public void keyTyped(final KeyEvent event) {
					password = passwordField.getText().toCharArray();
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
					returnValue = getCredentials().setPassword(null);
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

	public CredentialsDialog setRememberCredentialsText(final String rememberCredentialsText) {
		this.rememberCredentialsText = rememberCredentialsText;
		remove(panel);
		createComponents(this);
		return this;
	}

	public CredentialsDialog setRememberCredentials(final boolean rememberCredentials) {
		this.rememberCredentials = rememberCredentials;
		remove(panel);
		createComponents(this);
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
				case "noPassword": pattern = "Kein Passwort"; break;
				case "ok": pattern = "OK"; break;
				case "cancel": pattern = "Abbrechen"; break;
				default: pattern = "MessageKey unbekannt: " + resourceKey + (arguments != null && arguments.length > 0 ? " Argumente: " + Utilities.join(arguments, ", ") : "");
			}
			return MessageFormat.format(pattern, arguments);
		} else {
			String pattern;
			switch(resourceKey) {
				case "username": pattern = "Username"; break;
				case "password": pattern = "Password"; break;
				case "noPassword": pattern = "No password"; break;
				case "ok": pattern = "OK"; break;
				case "cancel": pattern = "Cancel"; break;
				default: pattern = "MessageKey unknown: " + resourceKey + (arguments != null && arguments.length > 0 ? " arguments: " + Utilities.join(arguments, ", ") : "");
			}
			return MessageFormat.format(pattern, arguments);
		}
	}

	public CredentialsDialog enableNoPassword(final boolean enable) {
		enableNoPassword(enable, getI18NString("noPassword"));
		return this;
	}

	public CredentialsDialog enableNoPassword(final boolean enable, final String enableNoPasswordButtonTextParam) {
		enableNoPassword = enable;
		enableNoPasswordButtonText = enableNoPasswordButtonTextParam;
		return this;
	}
}
