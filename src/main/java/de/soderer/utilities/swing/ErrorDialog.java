package de.soderer.utilities.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.io.InputStream;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import de.soderer.utilities.ExceptionUtilities;
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
		super(parent, applicationName + (Utilities.isNotBlank(applicationVersion) ? " (" + applicationVersion + ")" : "") + " " + getText("ERROR"));

		this.applicationName = applicationName;
		this.applicationVersion = applicationVersion;
		this.emailRecipient = emailRecipient;
		this.throwable = throwable;

		initUI();
	}

	private void initUI() {
		setLayout(new BorderLayout(10, 10));

		// 🔴 Icon setzen
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("images/icons/exclamation.png")) {
			if (is != null) {
				final ImageIcon icon = new ImageIcon(is.readAllBytes());
				setIconImage(icon.getImage());
			}
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Do nothing
		}

		// 🔹 Haupttext
		final JTextArea mainText = new JTextArea(getMainText());
		mainText.setEditable(false);
		mainText.setLineWrap(true);
		mainText.setWrapStyleWord(true);

		final JScrollPane scrollPane = new JScrollPane(mainText);
		scrollPane.setPreferredSize(new Dimension(500, 120));
		add(scrollPane, BorderLayout.NORTH);

		// 🔹 CENTER (Buttons + Details)
		final JPanel centerPanel = new JPanel();
		centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
		add(centerPanel, BorderLayout.CENTER);

		// 🔹 Button Panel
		final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		final JButton mailButton = new JButton(getText("OPEN_MAIL"));
		mailButton.addActionListener(this::openMail);
		buttonPanel.add(mailButton);

		detailButton = new JButton(getText("DETAILS_MORE"));
		detailButton.addActionListener(e -> toggleDetails());
		buttonPanel.add(detailButton);

		centerPanel.add(buttonPanel);

		// 🔹 Detail Panel
		detailPanel = new JPanel();
		detailPanel.setLayout(new BoxLayout(detailPanel, BoxLayout.Y_AXIS));
		centerPanel.add(detailPanel);

		// 🔹 Close Button
		final JButton closeButton = new JButton(getText("CLOSE"));
		closeButton.addActionListener(e -> dispose());

		final JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		southPanel.add(closeButton);

		add(southPanel, BorderLayout.SOUTH);

		pack();
		setLocationRelativeTo(getParent());
	}

	private String getMainText() {
		if (throwable instanceof VisibleException) {
			return ((VisibleException) throwable).getVisibleMessage();
		} else if (throwable != null && throwable.getMessage() != null) {
			return throwable.getMessage();
		} else {
			return getText("ERROR_OCCURRED");
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
					getText("CANNOT_OPEN_MAIL"),
					getTitle(),
					JOptionPane.ERROR_MESSAGE);
		}
	}

	private void toggleDetails() {
		final boolean show = detailButton.getText().equals(getText("DETAILS_MORE"));
		detailButton.setText(show ? getText("DETAILS_LESS") : getText("DETAILS_MORE"));
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

	private static String getText(final String key) {
		if ("DETAILS_MORE".equalsIgnoreCase(key)) {
			return "Details >>>";
		} else if ("DETAILS_LESS".equalsIgnoreCase(key)) {
			return "Details <<<";
		} else if ("de".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
			if ("OPEN_MAIL".equalsIgnoreCase(key)) {
				return "Mail mit Fehlerdaten an den Entwickler öffnen";
			} else if ("CANNOT_OPEN_MAIL".equalsIgnoreCase(key)) {
				return "Email client konnte nicht gefunden werden.";
			} else if ("CLOSE".equalsIgnoreCase(key)) {
				return "Schließen";
			} else if ("ERROR".equalsIgnoreCase(key)) {
				return "Fehler";
			} else if ("ERROR_OCCURRED".equalsIgnoreCase(key)) {
				return "Es ist ein Fehler aufgetreten";
			}
		} else {
			if ("OPEN_MAIL".equalsIgnoreCase(key)) {
				return "Open mail with errordata to developer";
			} else if ("CANNOT_OPEN_MAIL".equalsIgnoreCase(key)) {
				return "Email client was not found.";
			} else if ("CLOSE".equalsIgnoreCase(key)) {
				return "Close";
			} else if ("ERROR".equalsIgnoreCase(key)) {
				return "Error";
			} else if ("ERROR_OCCURRED".equalsIgnoreCase(key)) {
				return "An error occurred";
			}
		}
		return key;
	}
}
