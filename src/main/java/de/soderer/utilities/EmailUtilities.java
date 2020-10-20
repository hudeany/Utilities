package de.soderer.utilities;

import java.awt.Desktop;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.eclipse.swt.program.Program;

import de.soderer.utilities.http.HttpUtilities;

public class EmailUtilities {
	/**
	 * Send an email
	 *
	 * @param senderAdress
	 * @param toAdressList
	 * @param ccAdressList
	 * @param subject
	 * @param bodyText
	 * @param bodyHtml
	 * @param mailtype
	 * @param charset
	 * @return
	 */
	public static boolean sendEmail(final String senderAdress, final String toAdressList, final String ccAdressList, final String subject, final String bodyText, final String bodyHtml, final int mailtype, final String charset) {
		try {
			// create some properties and get the default Session
			final Properties props = new Properties();
			props.put("system.mail.host", "localhost");
			final Session session = Session.getDefaultInstance(props, null);
			// session.setDebug(debug);

			// create a message
			final MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(senderAdress));
			msg.setSubject(subject, charset);
			msg.setSentDate(new Date());

			// Set to-recipient email addresses
			final InternetAddress[] toAddresses = getEmailAddressesFromList(toAdressList);
			if (toAddresses != null && toAddresses.length > 0) {
				msg.setRecipients(Message.RecipientType.TO, toAddresses);
			}

			// Set cc-recipient email addresses
			final InternetAddress[] ccAddresses = getEmailAddressesFromList(ccAdressList);
			if (ccAddresses != null && ccAddresses.length > 0) {
				msg.setRecipients(Message.RecipientType.CC, ccAddresses);
			}

			switch (mailtype) {
				case 0:
					msg.setText(bodyText, charset);
					break;
				case 1:
					final Multipart mp = new MimeMultipart("alternative");
					MimeBodyPart mbp = new MimeBodyPart();
					mbp.setText(bodyText, charset);
					mp.addBodyPart(mbp);
					mbp = new MimeBodyPart();
					mbp.setContent(bodyHtml, "text/html; charset=" + charset);
					mp.addBodyPart(mbp);
					msg.setContent(mp);
					break;
				default:
					msg.setText(bodyText, charset);
					break;
			}

			Transport.send(msg);
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Get emails from string list
	 *
	 * @param listString
	 * @return
	 */
	public static InternetAddress[] getEmailAddressesFromList(final String listString) {
		if (Utilities.isNotBlank(listString)) {
			final List<InternetAddress> emailAddresses = new ArrayList<>();
			for (String singleAdr : listString.split(";|,| ")) {
				singleAdr = singleAdr.trim();
				if (Utilities.isNotBlank(singleAdr)) {
					try {
						final InternetAddress nextAddress = new InternetAddress(singleAdr.trim());
						nextAddress.validate();
						emailAddresses.add(nextAddress);
					} catch (final AddressException e) {
						e.printStackTrace();
					}
				}
			}

			return emailAddresses.toArray(new InternetAddress[emailAddresses.size()]);
		} else {
			return new InternetAddress[0];
		}
	}

	/**
	 * Try to open en email in the standard email client
	 *
	 * @param toAdress
	 * @param subject
	 * @param body
	 * @return
	 */
	public static boolean openMailInStandardClient(final String toAdress, final String subject, final String body) {
		return openMailInStandardClient(toAdress, null, subject, body);
	}

	/**
	 * Try to open en email in the standard email client
	 *
	 * @param toAdress
	 * @param ccAdress
	 * @param subject
	 * @param body
	 * @return
	 */
	public static boolean openMailInStandardClient(final String toAdress, final String ccAdress, final String subject, final String body) {
		try {
			final String mailtoString = createMailtoLink(toAdress, ccAdress, subject, body);

			if (SystemUtilities.isWindowsSystem()) {
				if (Desktop.isDesktopSupported()) {
					final Desktop desktop = Desktop.getDesktop();
					if (desktop.isSupported(Desktop.Action.MAIL)) {
						desktop.mail(new URI(mailtoString));
						return true;
					}
				}

				try {
					if (Program.launch(mailtoString)) {
						return true;
					}
				} catch (@SuppressWarnings("unused") final Exception e) {
					// Try some other method of mail creation
				}
			} else {
				try {
					Runtime.getRuntime().exec("xdg-open " + mailtoString);
					return true;
				} catch (@SuppressWarnings("unused") final Exception e) {
					// Try some other method of mail creation
				}

				try {
					// For using the parameters check the following description
					// http://kb.mozillazine.org/Command_line_arguments_%28Thunderbird%29
					Runtime.getRuntime().exec("thunderbird -compose");
					if (Program.launch(mailtoString)) {
						return true;
					}
				} catch (@SuppressWarnings("unused") final Exception e) {
					// Try some other method of mail creation
				}
			}

			throw new Exception("Cannot create mail in standard mailclient");
		} catch (final Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	private static String createMailtoLink(final String toAdress, final String ccAdress, final String subject, final String body) throws Exception {
		if (Utilities.isBlank(toAdress)) {
			throw new Exception("TO address is missing or empty");
		}

		String mailtoString = "mailto:" + HttpUtilities.urlEncode(toAdress, StandardCharsets.UTF_8);

		boolean isFirstParameter = true;

		if (Utilities.isNotBlank(ccAdress)) {
			if (isFirstParameter) {
				mailtoString += "?";
			} else {
				mailtoString += "&";
			}
			mailtoString += "cc=" + HttpUtilities.urlEncode(ccAdress, StandardCharsets.UTF_8);
			isFirstParameter = false;
		}

		if (Utilities.isNotBlank(subject)) {
			if (isFirstParameter) {
				mailtoString += "?";
			} else {
				mailtoString += "&";
			}
			mailtoString += "subject=" + HttpUtilities.urlEncode(subject, StandardCharsets.UTF_8).replace("+", "%20");
			isFirstParameter = false;
		}

		if (Utilities.isNotBlank(body)) {
			if (isFirstParameter) {
				mailtoString += "?";
			} else {
				mailtoString += "&";
			}
			mailtoString += "body=" + HttpUtilities.urlEncode(body, StandardCharsets.UTF_8).replace("+", "%20");
			isFirstParameter = false;
		}
		// + (attachmentFile != null ? "&attachment=" + urlEncode(attachmentFile.getAbsolutePath(), StandardCharsets.UTF_8) : ""); // maybe it is "&attach="

		return mailtoString;
	}
}
