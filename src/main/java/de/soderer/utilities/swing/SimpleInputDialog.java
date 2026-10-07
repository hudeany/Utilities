package de.soderer.utilities.swing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

/**
 * Modal dialog with a text and a single input field.
 *
 * <p>
 * {@link #open()} returns the entered value, or null if the dialog was
 * canceled. The OK button is only enabled while the input is not empty. An
 * optional input pattern rejects typed characters that would make the input
 * not match the pattern.
 * </p>
 */
public class SimpleInputDialog extends ModalDialog<String> {
	private static final long serialVersionUID = 2945587731926296004L;

	/** Text shown above the input field, may be blank */
	private final String text;

	/** Content panel, refilled by createComponents() */
	private final JPanel panel;

	/** Input field of the value */
	private JTextField inputField;

	/** OK button, only enabled while the input is not empty */
	private JButton okButton;

	/** Last input matching inputPattern, restored when a typed character breaks the pattern */
	private String currentInputText = "";

	/** Caret position at the time an invalid character was typed */
	private int currentPosition = 0;

	/** Pattern the whole input must match, null for no restriction */
	private Pattern inputPattern = null;

	/** Suppresses the input check while the field is reset programmatically */
	private boolean isTechnicalChange = false;

	/** Initial value of the input field, null for an empty field */
	private String defaultText = null;

	/**
	 * Creates a simple input dialog.
	 *
	 * @param parent owner window, may be null
	 * @param title title of the dialog
	 * @param text text shown above the input field, may be blank
	 */
	public SimpleInputDialog(final Window parent, final String title, final String text) {
		super(parent, title);

		this.text = text;

		setResizable(false);

		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		createComponents();

		returnValue = null;
	}

	/**
	 * Sets the initial value of the input field.
	 *
	 * @param defaultText initial value, may be null for an empty field
	 * @return this dialog for chaining
	 */
	public SimpleInputDialog setDefaultText(final String defaultText) {
		this.defaultText = defaultText;
		createComponents();
		return this;
	}

	/**
	 * Sets a regular expression the whole input must match while typing. Typed
	 * characters that make the input not match are removed again.
	 *
	 * @param patternString regular expression, blank for no restriction
	 */
	public void setInputPattern(final String patternString) {
		if (Utilities.isBlank(patternString)) {
			inputPattern = null;
		} else {
			inputPattern = Pattern.compile(patternString);
		}
	}

	/**
	 * (Re)creates all components of the dialog from the current settings.
	 */
	protected void createComponents() {
		panel.removeAll();

		panel.add(Box.createRigidArea(new Dimension(5, 5)));

		JScrollPane textScrollpane = null;
		if (Utilities.isNotBlank(text)) {
			final JTextArea textField = new JTextArea();
			final Font f = textField.getFont();
			final Font f2 = new Font("Arial", f.getStyle(), f.getSize());
			textField.setFont(f2);
			textField.setText(text);
			textField.setEditable(false);
			textField.setBackground(panel.getBackground());

			textScrollpane = new JScrollPane(textField);
			textScrollpane.setBorder(BorderFactory.createEmptyBorder());
			textScrollpane.setPreferredSize(new Dimension(Math.min(textScrollpane.getPreferredSize().width, 600), Math.min(textScrollpane.getPreferredSize().height, 400)));

			// Text Panel
			final JPanel textPanel = new JPanel();
			textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.LINE_AXIS));

			textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
			textPanel.add(textScrollpane);
			textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
			panel.add(textPanel);
		}

		panel.add(Box.createRigidArea(new Dimension(5, 5)));

		inputField = new JTextField();
		final Font f = inputField.getFont();
		final Font f2 = new Font("Arial", f.getStyle(), f.getSize() + 2);
		inputField.setFont(f2);
		inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		inputField.setText(defaultText != null ? defaultText : "");
		// The last valid input must be the default text, otherwise an invalid keystroke would revert the field to the empty text
		currentInputText = inputField.getText();
		inputField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(final KeyEvent event) {
				if (!isTechnicalChange) {
					if (inputPattern == null || inputPattern.matcher(inputField.getText()).matches()) {
						currentInputText = inputField.getText();
					} else {
						// Remove the invalid input character
						currentPosition = inputField.getCaretPosition();
						isTechnicalChange = true;
						inputField.setText(currentInputText);
						inputField.setSelectionStart(Math.min(currentInputText.length(), Math.max(0, currentPosition - 1)));
						inputField.setSelectionEnd(Math.min(currentInputText.length(), Math.max(0, currentPosition - 1)));
						isTechnicalChange = false;
					}

					if (event.getKeyChar() == '\n') {
						returnValue = inputField.getText();
						dispose();
					}
				}

				checkButtonStatus();
			}
		});
		final JPanel textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.LINE_AXIS));

		textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		textPanel.add(inputField);
		textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		panel.add(textPanel);

		panel.add(Box.createRigidArea(new Dimension(5, 5)));

		// Button Panel
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

		// ok Button
		okButton = new JButton(LangResources.get("ok"));
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				returnValue = inputField.getText();
				dispose();
			}
		});
		buttonPanel.add(okButton);

		buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));

		// Close Button
		final JButton closeButton = new JButton(LangResources.get("cancel"));
		closeButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				returnValue = null;
				dispose();
			}
		});
		buttonPanel.add(closeButton);

		panel.add(Box.createRigidArea(new Dimension(5, 5)));
		panel.add(buttonPanel);
		getRootPane().setDefaultButton(okButton);

		panel.add(Box.createRigidArea(new Dimension(5, 5)));

		add(panel);

		pack();

		if (textScrollpane != null && textScrollpane.getHorizontalScrollBar().isVisible()) {
			textScrollpane.setPreferredSize(new Dimension(textScrollpane.getPreferredSize().width, textScrollpane.getPreferredSize().height + 20));
			textScrollpane.setSize(new Dimension(textScrollpane.getPreferredSize().width, textScrollpane.getPreferredSize().height));
			pack();
		}

		setLocationRelativeTo(getOwner());

		inputField.requestFocus();

		checkButtonStatus();
	}

	private void checkButtonStatus() {
		okButton.setEnabled(Utilities.isNotEmpty(inputField.getText()));
	}
}
