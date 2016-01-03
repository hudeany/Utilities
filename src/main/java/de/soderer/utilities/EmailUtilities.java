package de.soderer.utilities;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
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
	public static boolean sendEmail(String senderAdress, String toAdressList, String ccAdressList, String subject, String bodyText, String bodyHtml, int mailtype, String charset) {
		try {
			// create some properties and get the default Session
			Properties props = new Properties();
			props.put("system.mail.host", "localhost");
			Session session = Session.getDefaultInstance(props, null);
			// session.setDebug(debug);

			// create a message
			MimeMessage msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(senderAdress));
			msg.setSubject(subject, charset);
			msg.setSentDate(new Date());

			// Set to-recipient email addresses
			InternetAddress[] toAddresses = getEmailAddressesFromList(toAdressList);
			if (toAddresses != null && toAddresses.length > 0) {
				msg.setRecipients(Message.RecipientType.TO, toAddresses);
			}

			// Set cc-recipient email addresses
			InternetAddress[] ccAddresses = getEmailAddressesFromList(ccAdressList);
			if (ccAddresses != null && ccAddresses.length > 0) {
				msg.setRecipients(Message.RecipientType.CC, ccAddresses);
			}

			switch (mailtype) {
				case 0:
					msg.setText(bodyText, charset);
					break;
				case 1:
					Multipart mp = new MimeMultipart("alternative");
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
		} catch (Exception e) {
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
	public static InternetAddress[] getEmailAddressesFromList(String listString) {
		if (Utilities.isNotBlank(listString)) {
			List<InternetAddress> emailAddresses = new ArrayList<InternetAddress>();
			for (String singleAdr : listString.split(";|,| ")) {
				singleAdr = singleAdr.trim();
				if (Utilities.isNotBlank(singleAdr)) {
					try {
						InternetAddress nextAddress = new InternetAddress(singleAdr.trim());
						nextAddress.validate();
						emailAddresses.add(nextAddress);
					} catch (AddressException e) {
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
	public static boolean openMailInStandardClient(String toAdress, String subject, String body) {
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
	public static boolean openMailInStandardClient(String toAdress, String ccAdress, String subject, String body) {
		try {
			File attachmentFile = null;
			String mailtoString = "mailto:" + URLEncoder.encode(toAdress, "UTF-8") + "?" + (Utilities.isNotBlank(ccAdress) ? "cc=" + URLEncoder.encode(ccAdress, "UTF-8") + "&" : "") + "subject="
					+ URLEncoder.encode(subject, "UTF-8").replace("+", "%20") + "&body=" + URLEncoder.encode(body, "UTF-8").replace("+", "%20")
					+ (attachmentFile != null ? "&attachment=" + attachmentFile.getAbsolutePath() : ""); // maybe it is "&attach="
			boolean success;
			try {
				success = Program.launch(mailtoString);
			} catch (Exception e) {
				throw new Exception("Open mail by swt.Program failed");
			}

			if (!success) {
				if (SystemUtilities.isWindowsSystem()) {
					if (Desktop.isDesktopSupported()) {
						Desktop desktop = Desktop.getDesktop();
						if (desktop.isSupported(Desktop.Action.MAIL)) {
							desktop.mail(new URI(mailtoString));
							return true;
						} else {
							return false;
						}
					} else {
						return false;
					}
				} else {
					try {
						// For using the parameters check the following description
						// http://kb.mozillazine.org/Command_line_arguments_%28Thunderbird%29
						Runtime.getRuntime().exec("thunderbird -compose");
						success = Program.launch(mailtoString);
					} catch (Exception e) {
						throw new Exception("Open mail by thunderbird failed");
					}
				}
				return true;
			} else {
				return true;
			}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}
}
