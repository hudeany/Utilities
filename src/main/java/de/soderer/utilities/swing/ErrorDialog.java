package de.soderer.utilities.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import de.soderer.utilities.ExceptionUtilities;
import de.soderer.utilities.LangResources;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.VisibleException;
import de.soderer.utilities.mail.MailUtilities;

public class ErrorDialog extends ModalDialog<Boolean> {
	private static final long serialVersionUID = 8028942277125473906L;

	private final String applicationName;
	private final String applicationVersion;
	private final String emailRecipient;
	private final Throwable throwable;

	private JButton detailButton;
	private JPanel detailPanel;

	public ErrorDialog(final Frame parent, final String applicationName, final String applicationVersion, final String emailRecipient, final Throwable throwable) {
		super(parent, applicationName + (Utilities.isNotBlank(applicationVersion) ? " (" + applicationVersion + ")" : "") + " " + getI18NString("error"));

		this.applicationName = applicationName;
		this.applicationVersion = applicationVersion;
		this.emailRecipient = emailRecipient;
		this.throwable = throwable;

		initUI();
	}

	private void initUI() {
		final int margin = 5;

		getContentPane().setLayout(new BorderLayout(margin, margin));
		((JPanel) getContentPane()).setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));

		try (InputStream is = getClass().getClassLoader().getResourceAsStream("images/icons/exclamation.png")) {
			if (is != null) {
				final ImageIcon icon = new ImageIcon(is.readAllBytes());
				setIconImage(icon.getImage());
			}
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Do nothing
		}

		final JTextArea mainText = new JTextArea(getMainText());
		mainText.setEditable(false);
		mainText.setLineWrap(true);
		mainText.setWrapStyleWord(true);
		mainText.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));

		final int lineHeight = mainText.getFontMetrics(mainText.getFont()).getHeight();
		final int lineCount = Math.max(1, mainText.getLineCount());
		final int maxLines = 5;
		final int preferredHeight = lineHeight * Math.min(lineCount, maxLines) + 10;

		final JScrollPane scrollPane = new JScrollPane(mainText);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
		scrollPane.setPreferredSize(new Dimension(500, preferredHeight));
		scrollPane.setBorder(BorderFactory.createEmptyBorder());
		add(scrollPane, BorderLayout.NORTH);

		final JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		add(centerPanel, BorderLayout.CENTER);

		final JPanel buttonPanel = new JPanel(new BorderLayout(margin, 0));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, margin, 0));

		final JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, margin, 0));
		final JButton mailButton = new JButton(getI18NString("openMail"));
		mailButton.addActionListener(this::openMail);
		leftButtons.add(mailButton);

		detailButton = new JButton(getI18NString("detailsMore"));
		detailButton.addActionListener(e -> toggleDetails());
		leftButtons.add(detailButton);

		buttonPanel.add(leftButtons, BorderLayout.WEST);

		final JButton closeButton = new JButton(getI18NString("close"));
		closeButton.addActionListener(e -> dispose());
		buttonPanel.add(closeButton, BorderLayout.CENTER);

		centerPanel.add(buttonPanel);

		detailPanel = new JPanel();
		detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
		detailPanel.setBorder(BorderFactory.createEmptyBorder(margin, margin, margin, margin));
		centerPanel.add(detailPanel);

		pack();
		setLocationRelativeTo(getParent());
	}

	private String getMainText() {
		if (throwable instanceof VisibleException) {
			return ((VisibleException) throwable).getVisibleMessage();
		} else if (throwable != null && throwable.getMessage() != null) {
			return throwable.getMessage();
		} else {
			return getI18NString("errorOccurred");
		}
	}

	private void openMail(@SuppressWarnings("unused") final ActionEvent ev) {
		final boolean success = MailUtilities.openMailInStandardClient(
				emailRecipient,
				"Error in " + applicationName
				+ (Utilities.isNotBlank(applicationVersion) ? " (" + applicationVersion + ")" : ""),
				ExceptionUtilities.throwableToString(throwable)
				);

		if (!success) {
			JOptionPane.showMessageDialog(this,
					getI18NString("cannotOpenMail"),
					getTitle(),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void toggleDetails() {
		final boolean show = detailButton.getText().equals(getI18NString("detailsMore"));
		detailButton.setText(show ? getI18NString("detailsLess") : getI18NString("detailsMore"));
		setupDetailPanel(show);
	}

	private void setupDetailPanel(final boolean show) {
		detailPanel.removeAll();

		if (show) {
			final String text2 = throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
			final String text3 = ExceptionUtilities.throwableToString(throwable);

			final JTextArea summary = new JTextArea(text2);
			summary.setEditable(false);
			summary.setLineWrap(true);
			summary.setWrapStyleWord(true);
			summary.setBorder(BorderFactory.createTitledBorder("Summary"));

			detailPanel.add(summary);

			if (Utilities.isNotEmpty(text3)) {
				final JTextArea stackTrace = new JTextArea(text3);
				stackTrace.setEditable(false);
				stackTrace.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 10));

				final JScrollPane scroll = new JScrollPane(stackTrace);
				scroll.setPreferredSize(new Dimension(500, 300));

				detailPanel.add(scroll);
			}
		}

		detailPanel.revalidate();
		detailPanel.repaint();

		pack();
		setLocationRelativeTo(getParent());
	}

	private static String getI18NString(final String resourceKey, final Object... arguments) {
		if (LangResources.existsKey(resourceKey)) {
			return LangResources.get(resourceKey, arguments);
		} else if ("de".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			String pattern;
			switch(resourceKey) {
				case "detailsMore": pattern = "Details >>>"; break;
				case "detailsLess": pattern = "Details <<<"; break;
				case "openMail": pattern = "Mail mit Fehlerdaten an den Entwickler öffnen"; break;
				case "cannotOpenMail": pattern = "Email client konnte nicht gefunden werden."; break;
				case "close": pattern = "Schließen"; break;
				case "error": pattern = "Fehler"; break;
				case "errorOccurred": pattern = "Es ist ein Fehler aufgetreten"; break;
				default: pattern = "MessageKey unbekannt: " + resourceKey + (arguments != null && arguments.length > 0 ? " Argumente: " + Utilities.join(arguments, ", ") : "");
			}
			return MessageFormat.format(pattern, arguments);
		} else {
			String pattern;
			switch(resourceKey) {
				case "detailsMore": pattern = "Details >>>"; break;
				case "detailsLess": pattern = "Details <<<"; break;
				case "openMail": pattern = "Open mail with errordata to developer"; break;
				case "cannotOpenMail": pattern = "Email client was not found."; break;
				case "close": pattern = "Close"; break;
				case "error": pattern = "Error"; break;
				case "errorOccurred": pattern = "An error occurred"; break;
				default: pattern = "MessageKey unknown: " + resourceKey + (arguments != null && arguments.length > 0 ? " arguments: " + Utilities.join(arguments, ", ") : "");
			}
			return MessageFormat.format(pattern, arguments);
		}
	}
}
