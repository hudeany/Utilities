package de.soderer.utilities.mail;

import java.awt.Desktop;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.eclipse.swt.program.Program;

import de.soderer.utilities.NetworkUtilities;
import de.soderer.utilities.SystemUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.http.HttpUtilities;
import jakarta.activation.DataHandler;
import jakarta.mail.Address;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.util.ByteArrayDataSource;

public class MailUtilities {
	/** The logger. */
	private static final transient Logger logger = Logger.getLogger(MailUtilities.class);

	private static final String SPECIAL_CHARS_REGEXP = "\\p{Cntrl}\\(\\)<>@,;:'\\\\\\\"\\.\\[\\]";
	private static final String VALID_CHARS_REGEXP = "[^\\s" + SPECIAL_CHARS_REGEXP + "]";
	private static final String QUOTED_USER_REGEXP = "(\"[^\"]*\")";
	private static final String WORD_REGEXP = "((" + VALID_CHARS_REGEXP + "|')+|" + QUOTED_USER_REGEXP + ")";

	private static final String DOMAIN_PART_REGEX = "\\p{Alnum}(?>[\\p{Alnum}-]*\\p{Alnum})*";
	private static final String TOP_DOMAIN_PART_REGEX = "\\p{Alpha}{2,}";
	private static final String DOMAIN_NAME_REGEX = "^(?:" + DOMAIN_PART_REGEX + "\\.)+" + "(" + TOP_DOMAIN_PART_REGEX + ")$";

	/**
	 * Regular expression for parsing email addresses.
	 *
	 * Taken from Apache Commons Validator.
	 * If this is not working, shame on Apache ;)
	 */
	private static final String EMAIL_REGEX = "^\\s*?(.+)@(.+?)\\s*$";

	private static final String USER_REGEX = "^\\s*" + WORD_REGEXP + "(\\." + WORD_REGEXP + ")*$";

	/** Regular expression pattern for parsing email addresses. */
	private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

	private static final Pattern USER_PATTERN = Pattern.compile(USER_REGEX);

	private static final Pattern DOMAIN_NAME_PATTERN = Pattern.compile(DOMAIN_NAME_REGEX);

	public static boolean isEmailValid(final String emailAddress) {
		final Matcher m = EMAIL_PATTERN.matcher(emailAddress);

		// Check, if email address matches outline structure
		if (!m.matches()) {
			return false;
		}

		// Check if user-part is valid
		if (!isValidUser(m.group(1))) {
			return false;
		}

		// Check if domain-part is valid
		if (!isValidDomain(m.group(2))) {
			return false;
		}

		return true;
	}

	private static boolean isValidUser(final String user) {
		return USER_PATTERN.matcher(user).matches();
	}

	private static boolean isValidDomain(final String domain) {
		String asciiDomainName;
		try {
			asciiDomainName = java.net.IDN.toASCII(domain);
		} catch (@SuppressWarnings("unused") final Exception e) {
			// invalid domain name like abc@.ch
			return false;
		}

		// Do not allow ".local" top level domain
		if (asciiDomainName.toLowerCase().endsWith(".local")) {
			return false;
		}

		return DOMAIN_NAME_PATTERN.matcher(asciiDomainName).matches();
	}

	/**
	 * Check if a given e-mail address is valid.
	 * Notice that a {@code null} value is invalid address.
	 *
	 * @param email an e-mail address to check.
	 * @return {@code true} if address is valid or {@code false} otherwise.
	 */
	public static boolean isEmailValidAndNormalized(final String email) {
		return email != null && isEmailValid(email) && email.equals(normalizeEmail(email));
	}

	/**
	 * Sends an email via Java (not via EMM Backend)
	 *
	 * For pure textmails leave bodyHtml empty or null
	 */
	public static boolean sendEmail(final String toAddressList, final String subject, final String bodyText, final String bodyHtml, final MailAttachment... attachments) {
		return sendEmail(null, 0, null, null, false, null, null, null, null, null, toAddressList, null, subject, bodyText, bodyHtml, null, attachments);
	}

	public static boolean sendEmail(final String toAddressList, final String fromAddress, final String replyToAddress, final String subject, final String bodyText, final String bodyHtml, final MailAttachment... attachments) {
		return sendEmail(null, 0, null, null, false, fromAddress, null, replyToAddress, null, null, toAddressList, null, subject, bodyText, bodyHtml, null, attachments);
	}

	/**
	 * Sends an email via Java (not via EMM Backend)
	 *
	 * For default senderaddress (from address) leave fromAddress empty or null
	 *
	 * For pure textmails leave bodyHtml empty or null
	 */
	public static boolean sendEmail(String smtpMailRelayHostname, final int smtpMailRelayPort, final String smtpUsername, final String smtpPassword, final boolean useStartTLS, String fromAddress, String fromName, final String replyToAddress, final String replyToName, final String bounceAddress, final String toAddressList, final String ccAddressList, final String subject, final String bodyText, final String bodyHtml, Charset charset, final MailAttachment... attachments) {
		if (Utilities.isBlank(smtpMailRelayHostname)) {
			logger.error("smtpMailRelayHostname is missing, so no mail was sent. emailSubject: " + subject);
			return false;
		}

		if (Utilities.isBlank(smtpMailRelayHostname)) {
			logger.warn("STMP mail relay hostname is not set, using localhost as fallback");
			smtpMailRelayHostname = "localhost";
		}

		if (Utilities.isBlank(fromAddress)) {
			if (Utilities.isBlank(fromAddress)) {
				fromAddress = System.getProperty("user.name") + "@" + NetworkUtilities.getHostName();
				fromName = "EMM";
			}
		}

		if (charset == null) {
			charset = StandardCharsets.UTF_8;
		}

		try {
			// create some properties and get the default Session
			final Properties mailProperties = new Properties();
			mailProperties.put("system.mail.host", smtpMailRelayHostname);
			mailProperties.put("mail.smtp.host", smtpMailRelayHostname);
			if (smtpMailRelayPort > 0) {
				mailProperties.put("mail.smtp.port", smtpMailRelayPort);
			}
			mailProperties.put("mail.host", smtpMailRelayHostname);
			mailProperties.put("mail.smtp.starttls.enable", "true");
			mailProperties.put("mail.smtp.ssl.enable", "false");
			mailProperties.put("mail.smtp.ssl.trust", smtpMailRelayHostname);
			if (useStartTLS) {
				mailProperties.put("mail.smtp.starttls.enable", "true");
			}

			// Set bounce address
			if (Utilities.isNotBlank(bounceAddress)) {
				mailProperties.put("mail.smtp.from", bounceAddress);
			} else {
				// Use fromAddress as fallback
				mailProperties.put("mail.smtp.from", fromAddress);
			}

			Authenticator authenticator;
			if (Utilities.isNotBlank(smtpUsername) && Utilities.isNotBlank(smtpPassword)) {
				authenticator = new Authenticator() {
					@Override
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(smtpUsername, smtpPassword);
					}
				};
			} else {
				authenticator = null;
			}
			final Session session = Session.getInstance(mailProperties, authenticator);

			// create a message
			final MimeMessage mimeMessage = new MimeMessage(session);
			if (Utilities.isBlank(fromName)) {
				mimeMessage.setFrom(new InternetAddress(fromAddress));
			} else {
				mimeMessage.setFrom(new InternetAddress(fromAddress, fromName));
			}
			mimeMessage.setSubject(subject, charset.name());
			mimeMessage.setSentDate(new Date());

			// Set reply-to address
			if (Utilities.isNotBlank(replyToAddress)) {
				if (Utilities.isBlank(replyToName)) {
					mimeMessage.setReplyTo(new Address[] { new InternetAddress(replyToAddress) });
				} else {
					mimeMessage.setReplyTo(new Address[] { new InternetAddress(replyToAddress, replyToName) });
				}
			} else {
				// Use fromAddress as fallback
				if (Utilities.isBlank(fromName)) {
					mimeMessage.setReplyTo(new Address[] { new InternetAddress(fromAddress) });
				} else {
					mimeMessage.setReplyTo(new Address[] { new InternetAddress(fromAddress, fromName) });
				}
			}

			// Set to-recipient email addresses
			final InternetAddress[] toAddresses = getEmailAddressesFromList(toAddressList);
			if (toAddresses.length > 0) {
				mimeMessage.setRecipients(Message.RecipientType.TO, toAddresses);
			}

			// Set cc-recipient email addresses
			final InternetAddress[] ccAddresses = getEmailAddressesFromList(ccAddressList);
			if (ccAddresses.length > 0) {
				mimeMessage.setRecipients(Message.RecipientType.CC, ccAddresses);
			}

			if (attachments == null || attachments.length <= 0) {
				if (Utilities.isBlank(bodyText) && Utilities.isBlank(bodyHtml)) {
					// Use a simple text email with only text content
					// Set to a single space. If body is totally empty, Commons Emails rejects sending email with exception "Invalid message supplied" (-> EMM-4133)
					mimeMessage.setText(" ", charset.name());
				} else if (Utilities.isBlank(bodyHtml)) {
					// Use a simple text email with only text content
					mimeMessage.setText(bodyText, charset.name());
				} else if (Utilities.isBlank(bodyText)) {
					// Use a simple html email with only html content
					mimeMessage.setContent(bodyHtml, "text/html; charset=" + charset);
				} else {
					// Use a multipart email with text and html content
					final Multipart multipart = new MimeMultipart("alternative");

					final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
					textMimeBodyPart.setText(bodyText, charset.name());
					multipart.addBodyPart(textMimeBodyPart);

					final MimeBodyPart htmlMimeBodyPart = new MimeBodyPart();
					htmlMimeBodyPart.setContent(bodyHtml, "text/html; charset=" + charset);
					multipart.addBodyPart(htmlMimeBodyPart);

					mimeMessage.setContent(multipart);
				}
			} else {
				final Multipart multipartMixed = new MimeMultipart("mixed");

				if (Utilities.isBlank(bodyText) && Utilities.isBlank(bodyHtml)) {
					// Use a multipart text email with only text content and attachements
					final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
					// Set to a single space. If body is totally empty, Commons Emails rejects sending email with exception "Invalid message supplied" (-> EMM-4133)
					textMimeBodyPart.setText(" ", charset.name());
					multipartMixed.addBodyPart(textMimeBodyPart);
				} else if (Utilities.isBlank(bodyHtml)) {
					final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
					// Use a multipart text email with only text content and attachements
					textMimeBodyPart.setText(bodyText, charset.name());
					multipartMixed.addBodyPart(textMimeBodyPart);
				} else if (Utilities.isBlank(bodyText)) {
					final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
					// Use a multipart html email with only html content and attachements
					textMimeBodyPart.setContent(bodyHtml, "text/html; charset=" + charset);
					multipartMixed.addBodyPart(textMimeBodyPart);
				} else {
					// Use a multipart html email with text and html content and attachements
					final Multipart multipartTextContent = new MimeMultipart("alternative");

					final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
					textMimeBodyPart.setText(bodyText, charset.name());
					multipartTextContent.addBodyPart(textMimeBodyPart);

					final MimeBodyPart htmlMimeBodyPart = new MimeBodyPart();
					htmlMimeBodyPart.setContent(bodyHtml, "text/html; charset=" + charset);
					multipartTextContent.addBodyPart(htmlMimeBodyPart);

					final MimeBodyPart alternativeMimeBodyPart = new MimeBodyPart();
					multipartMixed.addBodyPart(alternativeMimeBodyPart);
					alternativeMimeBodyPart.setContent(multipartTextContent);
				}

				for (final MailAttachment attachment : attachments) {
					final MimeBodyPart attachmentMimeBodyPart = new MimeBodyPart();
					attachmentMimeBodyPart.setFileName(MimeUtility.encodeText(attachment.getName(), StandardCharsets.UTF_8.name(), null));
					final ByteArrayDataSource bds = new ByteArrayDataSource(attachment.getData(), attachment.getMimeType());
					attachmentMimeBodyPart.setDataHandler(new DataHandler(bds));
					multipartMixed.addBodyPart(attachmentMimeBodyPart);
				}

				mimeMessage.setContent(multipartMixed);
			}

			Transport.send(mimeMessage);

			if (logger.isDebugEnabled()) {
				logger.debug("Sending java email:\nfrom: " + fromAddress + "\nto: " + toAddressList + "\nsubject: " + subject);
			}

			return true;
		} catch (final Exception e) {
			logger.error("Error sending email via " + smtpMailRelayHostname + ": " + e.getMessage() + "\nemailSubject: " + subject + (Utilities.isNotBlank(bodyText) ? "\nemailContentText: " + bodyText : "") + (Utilities.isNotBlank(bodyHtml) ? "\nemailContentHtml: " + bodyHtml : ""), e);
			return false;
		}
	}

	public static void sendEmail(final String body, final String subject, final String recipient) throws Exception {
		final Properties mailProps = new Properties();
		mailProps.put("mail.smtp.from", "andreas@soderer");
		mailProps.put("mail.smtp.host", "smtp.mail.sasg.de");
		mailProps.put("mail.smtp.port", 587);
		mailProps.put("mail.smtp.auth", true);
		mailProps.put("mail.smtp.socketFactory.port", 587);
		mailProps.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		mailProps.put("mail.smtp.socketFactory.fallback", "false");
		mailProps.put("mail.smtp.starttls.enable", "true");

		final Session mailSession = Session.getDefaultInstance(mailProps, new Authenticator() {
			@Override
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication("v08060401p0005", "kL!5gfPi3$zu");
			}
		});

		final MimeMessage message = new MimeMessage(mailSession);
		message.setFrom(new InternetAddress("andreas@soderer.de"));
		final String[] emails = { recipient };
		final InternetAddress dests[] = new InternetAddress[emails.length];
		for (int i = 0; i < emails.length; i++) {
			dests[i] = new InternetAddress(emails[i].trim().toLowerCase());
		}
		message.setRecipients(Message.RecipientType.TO, dests);
		message.setSubject(subject, "UTF-8");
		final Multipart mp = new MimeMultipart();
		final MimeBodyPart mbp = new MimeBodyPart();
		mbp.setContent(body, "text/html;charset=utf-8");
		mp.addBodyPart(mbp);
		message.setContent(mp);
		message.setSentDate(new java.util.Date());

		Transport.send(message);
	}

	private static InternetAddress[] getEmailAddressesFromList(final String listString) throws Exception {
		final List<InternetAddress> emailAddresses = new ArrayList<>();

		if (Utilities.isNotBlank(listString)) {
			for (String address : splitAndNormalizeEmails(listString)) {
				address = address == null ? "" : Utilities.trim(address);
				if (Utilities.isNotBlank(address) && MailUtilities.isEmailValid(address)) {
					try {
						final InternetAddress nextAddress = new InternetAddress(address.trim());
						nextAddress.validate();
						emailAddresses.add(nextAddress);
					} catch (final AddressException e) {
						throw new Exception("Invalid Emailaddress found: " + address, e);
					}
				}
			}
		}

		return emailAddresses.toArray(new InternetAddress[0]);
	}

	private static Set<String> splitAndNormalizeEmails(final String emails) {
		if (Utilities.isBlank(emails)) {
			return Collections.emptySet();
		}

		final Set<String> addresses = new LinkedHashSet<>();

		for (final String part : emails.split(";|,| ")) {
			final String address = normalizeEmail(part);

			if (Utilities.isNotEmpty(address)) {
				addresses.add(address);
			}
		}

		return addresses;
	}

	/**
	 * Call lowercase and trim on email address. Watch out: apostrophe and other
	 * special characters !#$%&'*+-/=?^_`{|}~ are allowed in local parts of
	 * emailaddresses
	 *
	 * @param email
	 * @return
	 */
	public static String normalizeEmail(final String email) {
		if (Utilities.isBlank(email)) {
			return null;
		} else {
			return email.toLowerCase().trim();
		}
	}

	/**
	 * Try to open an email in the standard email client
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
	 * Try to open an email in the standard email client
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
