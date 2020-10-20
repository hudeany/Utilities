package de.soderer.utilities.mail;

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

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;

import org.apache.log4j.Logger;

import de.soderer.utilities.NetworkUtilities;
import de.soderer.utilities.Utilities;

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
		if(!m.matches()) {
			return false;
		}

		// Check if user-part is valid
		if(!isValidUser(m.group(1))) {
			return false;
		}

		// Check if domain-part is valid
		if(!isValidDomain(m.group(2))) {
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
	public static boolean sendEmail(final String smtpMailRelayHostname, final String toAddressList, final String subject, final String bodyText, final String bodyHtml, final JavaMailAttachment... attachments) {
		return sendEmail(smtpMailRelayHostname, null, null, null, null, null, toAddressList, null, subject, bodyText, bodyHtml, null, attachments);
	}

	public static boolean sendEmail(final String smtpMailRelayHostname, final String toAddressList, final String fromAddress, final String replyToAddress, final String subject, final String bodyText, final String bodyHtml, final JavaMailAttachment... attachments) {
		return sendEmail(smtpMailRelayHostname, fromAddress, null, replyToAddress, null, null, toAddressList, null, subject, bodyText, bodyHtml, null, attachments);
	}

	/**
	 * Sends an email via Java (not via EMM Backend)
	 *
	 * For default senderaddress (from address) leave fromAddress empty or null
	 *
	 * For pure textmails leave bodyHtml empty or null
	 */
	public static boolean sendEmail(String smtpMailRelayHostname, String fromAddress, String fromName, final String replyToAddress, final String replyToName, final String bounceAddress, final String toAddressList, final String ccAddressList, final String subject, final String bodyText, final String bodyHtml, Charset charset, final JavaMailAttachment... attachments) {
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
			final Properties props = new Properties();
			props.put("system.mail.host", smtpMailRelayHostname);
			props.put("mail.smtp.host", smtpMailRelayHostname);
			props.put("mail.host", smtpMailRelayHostname);
			props.put("mail.smtp.starttls.enable", "true");
			props.put("mail.smtp.ssl.enable", "false");
			props.put("mail.smtp.ssl.trust", smtpMailRelayHostname);

			// Set bounce address
			if (Utilities.isNotBlank(bounceAddress)) {
				props.put("mail.smtp.from", bounceAddress);
			} else {
				// Use fromAddress as fallback
				props.put("mail.smtp.from", fromAddress);
			}

			final Session session = Session.getDefaultInstance(props, null);

			// create a message
			final MimeMessage msg = new MimeMessage(session);
			if (Utilities.isBlank(fromName)) {
				msg.setFrom(new InternetAddress(fromAddress));
			} else {
				msg.setFrom(new InternetAddress(fromAddress, fromName));
			}
			msg.setSubject(subject, charset.name());
			msg.setSentDate(new Date());

			// Set reply-to address
			if (Utilities.isNotBlank(replyToAddress)) {
				if (Utilities.isBlank(replyToName)) {
					msg.setReplyTo(new Address[] { new InternetAddress(replyToAddress) });
				} else {
					msg.setReplyTo(new Address[] { new InternetAddress(replyToAddress, replyToName) });
				}
			} else {
				// Use fromAddress as fallback
				if (Utilities.isBlank(fromName)) {
					msg.setReplyTo(new Address[] { new InternetAddress(fromAddress) });
				} else {
					msg.setReplyTo(new Address[] { new InternetAddress(fromAddress, fromName) });
				}
			}

			// Set to-recipient email addresses
			final InternetAddress[] toAddresses = getEmailAddressesFromList(toAddressList);
			if (toAddresses.length > 0) {
				msg.setRecipients(Message.RecipientType.TO, toAddresses);
			}

			// Set cc-recipient email addresses
			final InternetAddress[] ccAddresses = getEmailAddressesFromList(ccAddressList);
			if (ccAddresses.length > 0) {
				msg.setRecipients(Message.RecipientType.CC, ccAddresses);
			}

			if (attachments == null || attachments.length <= 0) {
				if (Utilities.isBlank(bodyText) && Utilities.isBlank(bodyHtml)) {
					// Use a simple text email with only text content
					// Set to a single space. If body is totally empty, Commons Emails rejects sending email with exception "Invalid message supplied" (-> EMM-4133)
					msg.setText(" ", charset.name());
				} else if (Utilities.isBlank(bodyHtml)) {
					// Use a simple text email with only text content
					msg.setText(bodyText, charset.name());
				} else if (Utilities.isBlank(bodyText)) {
					// Use a simple html email with only html content
					msg.setContent(bodyHtml, "text/html; charset=" + charset);
				} else {
					// Use a multipart email with text and html content
					final Multipart multipart = new MimeMultipart("alternative");

					final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
					textMimeBodyPart.setText(bodyText, charset.name());
					multipart.addBodyPart(textMimeBodyPart);

					final MimeBodyPart htmlMimeBodyPart = new MimeBodyPart();
					htmlMimeBodyPart.setContent(bodyHtml, "text/html; charset=" + charset);
					multipart.addBodyPart(htmlMimeBodyPart);

					msg.setContent(multipart);
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

				for (final JavaMailAttachment attachment : attachments) {
					final MimeBodyPart attachmentMimeBodyPart = new MimeBodyPart();
					attachmentMimeBodyPart.setFileName(MimeUtility.encodeText(attachment.getName(), StandardCharsets.UTF_8.name(), null));
					final ByteArrayDataSource bds = new ByteArrayDataSource(attachment.getData(), attachment.getMimeType());
					attachmentMimeBodyPart.setDataHandler(new DataHandler(bds));
					multipartMixed.addBodyPart(attachmentMimeBodyPart);
				}

				msg.setContent(multipartMixed);
			}

			try(final Transport transport = session.getTransport("smtp")) {
				transport.connect();
				transport.sendMessage(msg, msg.getRecipients(Message.RecipientType.TO));

				if (logger.isDebugEnabled()) {
					logger.debug("Sending java email:\nfrom: " + fromAddress + "\nto: " + toAddressList + "\nsubject: " + subject);
				}

				return true;
			}
		} catch (final Exception e) {
			logger.error("Error sending email via " + smtpMailRelayHostname + ": " + e.getMessage() + " \nemailSubject: " + subject + " \nemailContent: " + (Utilities.isBlank(bodyText) ? bodyHtml : bodyText), e);
			return false;
		}
	}

	private static InternetAddress[] getEmailAddressesFromList(final String listString) {
		final List<InternetAddress> emailAddresses = new ArrayList<>();
		for (String address : splitAndNormalizeEmails(listString)) {
			address = address == null ? "" : Utilities.trim(address);
			if(MailUtilities.isEmailValid(address)) {
				try {
					final InternetAddress nextAddress = new InternetAddress(address);
					nextAddress.validate();
					emailAddresses.add(nextAddress);
				} catch (final AddressException e) {
					logger.error("Invalid Emailaddress found: " + address, e);
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
}
