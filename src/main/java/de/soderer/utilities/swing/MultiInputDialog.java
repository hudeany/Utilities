package de.soderer.utilities.swing;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

public class MultiInputDialog extends ModalDialog<List<String>> {
	private static final long serialVersionUID = -4064311167723709652L;

	private final String text;
	private final String[] labelTexts;
	private String[] defaultTexts = null;

	private final JPanel panel;
	private final List<JTextField> textFields = new ArrayList<>();

	public MultiInputDialog(final Window parent, final String title, final String text, final String... labelTexts) {
		super(parent, title);

		this.text = text;
		this.labelTexts = labelTexts;

		setResizable(false);

		panel = new JPanel();

		createComponents();

		returnValue = null;
	}

	public MultiInputDialog setDefaultTexts(final String[] defaultTexts) {
		this.defaultTexts = defaultTexts;
		createComponents();
		return this;
	}

	protected void createComponents() {
		panel.removeAll();
		textFields.clear();

		panel.setLayout(new GridBagLayout());

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

			textPanel.add(textScrollpane);

			final GridBagConstraints textGridBagConstraints = new GridBagConstraints();
			textGridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
			textGridBagConstraints.gridwidth = 2;
			textGridBagConstraints.gridx = 0;
			textGridBagConstraints.gridy = 0;
			textGridBagConstraints.insets = new Insets(5, 5, 5, 5);
			panel.add(textPanel, textGridBagConstraints);
		}

		int index = 0;
		for (final String labelText : labelTexts) {
			final JLabel label = new JLabel(labelText + ": ");

			final GridBagConstraints labelGridBagConstraints = new GridBagConstraints();
			labelGridBagConstraints.anchor = GridBagConstraints.EAST;
			labelGridBagConstraints.gridx = 0;
			labelGridBagConstraints.gridy = index + 1;
			labelGridBagConstraints.insets = new Insets(5, 5, 5, 0);
			panel.add(label, labelGridBagConstraints);

			final JTextField inputField = new JTextField();
			final Font f = inputField.getFont();
			final Font f2 = new Font("Arial", f.getStyle(), f.getSize() + 2);
			inputField.setFont(f2);
			inputField.setBorder(BorderFactory.createLineBorder(Color.BLACK));
			inputField.setPreferredSize(new Dimension(250, inputField.getPreferredSize().height));
			if (defaultTexts != null && defaultTexts.length > index && defaultTexts[index] != null) {
				inputField.setText(defaultTexts[index]);
				inputField.setCaretPosition(0);
			}

			final GridBagConstraints textFieldGridBagConstraints = new GridBagConstraints();
			textFieldGridBagConstraints.gridx = 1;
			textFieldGridBagConstraints.gridy = index + 1;
			textFieldGridBagConstraints.insets = new Insets(5, 5, 5, 5);
			panel.add(inputField, textFieldGridBagConstraints);

			index++;
			textFields.add(inputField);
		}

		// Button Panel
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));

		// ok Button
		final JButton okButton = new JButton(LangResources.get("ok"));
		okButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(final ActionEvent event) {
				final List<String> returnValueList = new ArrayList<>();
				for (final JTextField textFieldForReturnValue: textFields) {
					returnValueList.add(textFieldForReturnValue.getText());
				}

				returnValue = returnValueList;
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

		final GridBagConstraints buttonGridBagConstraints = new GridBagConstraints();
		buttonGridBagConstraints.gridwidth = 2;
		buttonGridBagConstraints.anchor = GridBagConstraints.CENTER;
		buttonGridBagConstraints.gridx = 0;
		buttonGridBagConstraints.gridy = index + 1;
		buttonGridBagConstraints.insets = new Insets(5, 5, 5, 5);
		panel.add(buttonPanel, buttonGridBagConstraints);

		getRootPane().setDefaultButton(okButton);

		add(panel);

		pack();

		setLocationRelativeTo(getOwner());

		textFields.get(0).requestFocus();
	}
}
