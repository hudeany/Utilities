package de.soderer.utilities.swing;

import java.awt.Color;
import java.awt.Dimension;
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

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

public class SimpleInputDialog extends ModalDialog<String> {
	private static final long serialVersionUID = 2945587731926296004L;

	private final String text;

	private JPanel panel;

	private JTextArea inputField;
	private JButton okButton;

	private String currentInputText = "";
	private int currentPosition = 0;
	private Pattern inputPattern = null;
	private boolean isTechnicalChange = false;
	private String defaultText = null;

	public SimpleInputDialog(final Window parent, final String title, final String text) {
		super(parent, title);

		this.text = text;

		setResizable(false);

		createComponents();

		returnValue = null;
	}

	public SimpleInputDialog setDefaultText(final String defaultText) {
		this.defaultText = defaultText;
		createComponents();
		return this;
	}

	public void setInputPattern(final String patternString) {
		if (Utilities.isBlank(patternString)) {
			inputPattern = null;
		} else {
			inputPattern = Pattern.compile(patternString);
		}
	}

	protected void createComponents() {
		removeAll();

		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		panel.add(Box.createRigidArea(new Dimension(5, 5)));

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

		panel.add(Box.createRigidArea(new Dimension(5, 5)));

		inputField = new JTextArea();
		inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		inputField.setPreferredSize(new Dimension(200, inputField.getPreferredSize().height));
		inputField.setText(defaultText != null ? defaultText : "");
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
		panel.add(inputField);

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

		setLocationRelativeTo(getOwner());

		inputField.requestFocus();

		checkButtonStatus();
	}

	private void checkButtonStatus() {
		okButton.setEnabled(Utilities.isNotEmpty(inputField.getText()));
	}
}
