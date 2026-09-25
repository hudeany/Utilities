package de.soderer.utilities.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.StringSelection;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import de.soderer.utilities.DefaultLangResources;

/**
 * Modal dialog showing a (possibly long) read-only text, e.g. a report, with
 * buttons to copy the text to the clipboard and to close the dialog.
 */
public class ShowDataDialog extends ModalDialog<Boolean> {
	private static final long serialVersionUID = -3818237915734861152L;

	private static final int DEFAULT_MAX_WIDTH = 800;
	private static final int DEFAULT_MAX_HEIGHT = 500;
	private static final int MIN_WIDTH = 400;
	private static final int MIN_HEIGHT = 200;

	private final JTextArea textArea;
	private final JScrollPane textScrollPane;

	public ShowDataDialog(final Window parent, final String title, final String text) {
		super(parent, title);

		final int margin = 5;

		getContentPane().setLayout(new BorderLayout(margin, margin));
		((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));

		textArea = new JTextArea(text == null ? "" : text);
		textArea.setEditable(false);
		textArea.setCaretPosition(0);
		textArea.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));

		textScrollPane = new JScrollPane(textArea);
		add(textScrollPane, BorderLayout.CENTER);

		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, margin, 0));

		final JButton copyButton = new JButton(DefaultLangResources.getI18NStringOrDefault("copyToClipboard"));
		copyButton.addActionListener(e -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textArea.getText()), null));
		buttonPanel.add(copyButton);

		final JButton closeButton = new JButton(DefaultLangResources.getI18NStringOrDefault("close"));
		closeButton.addActionListener(e -> {
			returnValue = true;
			dispose();
		});
		buttonPanel.add(closeButton);

		add(buttonPanel, BorderLayout.SOUTH);

		getRootPane().setDefaultButton(closeButton);

		setResizable(false);

		// Size to the text, but never larger than a sensible maximum (long reports scroll instead)
		final Dimension preferredTextSize = textScrollPane.getPreferredSize();
		textScrollPane.setPreferredSize(new Dimension(
				Math.max(MIN_WIDTH, Math.min(preferredTextSize.width + 20, DEFAULT_MAX_WIDTH)),
				Math.max(MIN_HEIGHT, Math.min(preferredTextSize.height + 20, DEFAULT_MAX_HEIGHT))));

		pack();

		setLocationRelativeTo(parent);
	}

	/**
	 * Allows the user to resize the dialog (e.g. for long reports).
	 */
	public ShowDataDialog withResizable(final boolean resizable) {
		setResizable(resizable);

		return this;
	}

	/**
	 * Uses a monospaced font, e.g. for column aligned data.
	 */
	public ShowDataDialog withMonospacedFont(final boolean monospaced) {
		final Font currentFont = textArea.getFont();
		textArea.setFont(new Font(monospaced ? Font.MONOSPACED : Font.DIALOG, currentFont.getStyle(), currentFont.getSize()));

		return this;
	}

	/**
	 * Sets an explicit dialog size. A value &lt;= 0 keeps the current width or
	 * height.
	 */
	public ShowDataDialog withSize(final int width, final int height) {
		setSize(width > 0 ? width : getWidth(), height > 0 ? height : getHeight());
		setLocationRelativeTo(getOwner());

		return this;
	}
}
