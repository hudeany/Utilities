package de.soderer.utilities.swing;

import java.awt.Color;
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
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;

/**
 * Modal dialog showing a (possibly multi-line) text with a row of answer
 * buttons and an optional checkbox.
 *
 * <p>
 * {@link #open()} returns the index of the pressed button, or null if the
 * dialog was closed via the window decoration or (without button texts) via
 * the close button.
 * </p>
 */
public class QuestionDialog extends ModalDialog<Integer> {
	private static final long serialVersionUID = 2020023796052296409L;

	/** Text to show */
	private final String text;

	/** Background color of the text area, null for the panel's background */
	private Color backgroundcolor = null;

	/** Texts of the answer buttons; if none are given, a single "close" button is shown */
	private final String[] buttonTexts;

	/** Text of the additional checkbox, blank for no checkbox */
	private String checkboxText = null;

	/** Status of the checkbox, also available after the dialog was closed */
	private boolean checkboxStatus = false;

	/** Content panel, rebuilt whenever an option affecting the layout changes */
	private JPanel panel;

	/**
	 * Creates a question dialog. The first button is the default button.
	 *
	 * @param parent owner window, may be null
	 * @param title title of the dialog
	 * @param text text to show
	 * @param buttonTexts texts of the answer buttons; if none are given, a single
	 *            "close" button is shown
	 */
	public QuestionDialog(final Window parent, final String title, final String text, final String... buttonTexts) {
		super(parent, title);

		this.text = text;
		this.buttonTexts = buttonTexts;

		setResizable(false);

		createComponents(this);
	}

	private void createComponents(final QuestionDialog questionDialog) {
		panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.PAGE_AXIS));

		panel.add(Box.createRigidArea(new Dimension(0, 5)));

		final JTextArea textField = new JTextArea();

		final Font f = textField.getFont();
		final Font f2 = new Font("Arial", f.getStyle(), f.getSize());
		textField.setFont(f2);

		textField.setText(text);
		textField.setEditable(false);
		if (backgroundcolor != null) {
			textField.setBackground(backgroundcolor);
		} else {
			textField.setBackground(panel.getBackground());
		}

		final JScrollPane textScrollpane = new JScrollPane(textField);
		textScrollpane.setBorder(BorderFactory.createEmptyBorder());
		textScrollpane.setPreferredSize(new Dimension(Math.min(textScrollpane.getPreferredSize().width, 600), Math.min(textScrollpane.getPreferredSize().height, 400)));

		// Text Panel
		final JPanel textPanel = new JPanel();
		textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.LINE_AXIS));

		textPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		textPanel.add(textScrollpane);
		textPanel.add(Box.createRigidArea(new Dimension(5, 0)));

		panel.add(textPanel);

		if (Utilities.isNotBlank(checkboxText)) {
			// Checkbox Panel
			final JPanel checkboxPanel = new JPanel();
			checkboxPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
			final JCheckBox checkboxCheckBox = new JCheckBox(checkboxText);
			checkboxCheckBox.setSelected(checkboxStatus);
			checkboxCheckBox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent event) {
					checkboxStatus = checkboxCheckBox.isSelected();
				}
			});
			checkboxPanel.add(checkboxCheckBox);

			panel.add(checkboxPanel);
		}

		// Button Panel
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));

		JButton buttonToSelect = null;

		if (buttonTexts != null && buttonTexts.length > 0) {
			for (int i = 0; i < buttonTexts.length; i++) {
				if (i == 0) {
					buttonPanel.add(Box.createRigidArea(new Dimension(5, 0)));
				}

				final int answerButtonIndexFinal = i;

				final JButton nextButton = new JButton(buttonTexts[i]);
				nextButton.addActionListener(new ActionListener() {
					@Override
					public void actionPerformed(final ActionEvent event) {
						returnValue = answerButtonIndexFinal;
						questionDialog.dispose();
					}
				});
				buttonPanel.add(nextButton);

				if (i == 0) {
					buttonToSelect = nextButton;
				}
			}
		} else {
			final JButton cancelButton = new JButton(LangResources.get("close"));
			cancelButton.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(final ActionEvent event) {
					questionDialog.dispose();
				}
			});
			buttonPanel.add(cancelButton);

			buttonToSelect = cancelButton;
		}

		panel.add(buttonPanel);

		add(panel);

		pack();

		if (textScrollpane.getHorizontalScrollBar().isVisible()) {
			textScrollpane.setPreferredSize(new Dimension(textScrollpane.getPreferredSize().width, textScrollpane.getPreferredSize().height + 20));
			textScrollpane.setSize(new Dimension(textScrollpane.getPreferredSize().width, textScrollpane.getPreferredSize().height));
			pack();
		}

		setLocationRelativeTo(getOwner());

		if (buttonToSelect != null) {
			getRootPane().setDefaultButton(buttonToSelect);
		}
	}

	/**
	 * Sets the background color of the text area, e.g.
	 * {@link SwingColor#LightRed} for error messages.
	 *
	 * @param backgroundcolor background color, null for the panel's background
	 * @return this dialog for chaining
	 */
	public QuestionDialog setBackgroundColor(final Color backgroundcolor) {
		this.backgroundcolor = backgroundcolor;
		remove(panel);
		createComponents(this);
		return this;
	}

	/**
	 * Sets the text of an additional checkbox shown below the text.
	 *
	 * @param checkboxText text of the checkbox, blank for no checkbox
	 * @return this dialog for chaining
	 */
	public QuestionDialog setCheckboxText(final String checkboxText) {
		this.checkboxText = checkboxText;
		remove(panel);
		createComponents(this);
		return this;
	}

	/**
	 * Sets the initial status of the checkbox.
	 *
	 * @param checkboxStatus true to check the checkbox initially
	 * @return this dialog for chaining
	 */
	public QuestionDialog setCheckboxStatus(final boolean checkboxStatus) {
		this.checkboxStatus = checkboxStatus;
		remove(panel);
		createComponents(this);
		return this;
	}

	/**
	 * Returns the status of the checkbox, also after the dialog was closed.
	 *
	 * @return true if the checkbox is checked
	 */
	public boolean getCheckboxStatus() {
		return checkboxStatus;
	}
}