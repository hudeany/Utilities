package de.soderer.utilities.mail;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Properties;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.mail.dkim.DkimSignedMessageForJakarta;
import jakarta.activation.DataHandler;
import jakarta.mail.Address;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.internet.MimeUtility;
import jakarta.mail.util.ByteArrayDataSource;

public class MailerForJakarta {
	private String smtpMailRelayHostname = "localhost";
	private int smtpMailRelayPort = 0;

	private String smtpUsername = null;
	private char[] smtpPassword = null;

	private MailerConnectionSecurity connectionSecurity = MailerConnectionSecurity.None;

	public String getSmtpMailRelayHostname() {
		return smtpMailRelayHostname;
	}

	public MailerForJakarta setSmtpMailRelayHostname(final String smtpMailRelayHostname) {
		this.smtpMailRelayHostname = smtpMailRelayHostname;
		return this;
	}

	public int getSmtpMailRelayPort() {
		return smtpMailRelayPort;
	}

	public MailerForJakarta setSmtpMailRelayPort(final int smtpMailRelayPort) {
		this.smtpMailRelayPort = smtpMailRelayPort;
		return this;
	}

	public String getSmtpUsername() {
		return smtpUsername;
	}

	public MailerForJakarta setSmtpUsername(final String smtpUsername) {
		this.smtpUsername = smtpUsername;
		return this;
	}

	public char[] getSmtpPassword() {
		return smtpPassword;
	}

	public MailerForJakarta setSmtpPassword(final char[] smtpPassword) {
		this.smtpPassword = smtpPassword;
		return this;
	}

	public MailerConnectionSecurity getConnectionSecurity() {
		return connectionSecurity;
	}

	public MailerForJakarta setConnectionSecurity(final MailerConnectionSecurity connectionSecurity) {
		this.connectionSecurity = connectionSecurity;
		return this;
	}

	public MailerForJakarta send(final EmailForJakarta email) throws Exception {
		if (Utilities.isBlank(smtpMailRelayHostname)) {
			smtpMailRelayHostname = "localhost";
		}

		final Properties mailProps = new Properties();
		mailProps.put("mail.smtp.from", email.getFromAddress().getAddress());
		mailProps.put("mail.smtp.host", smtpMailRelayHostname);
		mailProps.put("mail.smtp.port", smtpMailRelayPort);
		mailProps.put("mail.smtp.auth", true);

		if (connectionSecurity == MailerConnectionSecurity.SSL_TLS) {
			mailProps.put("mail.smtp.socketFactory.port", smtpMailRelayPort);
			mailProps.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			mailProps.put("mail.smtp.socketFactory.fallback", "false");
		} else if (connectionSecurity == MailerConnectionSecurity.STARTTLS) {
			mailProps.put("mail.smtp.starttls.enable", "true");
		}

		final Session session;
		if (smtpUsername != null) {
			final Authenticator authenticator = new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(smtpUsername, new String(smtpPassword));
				}
			};
			session = Session.getDefaultInstance(mailProps, authenticator);
		} else {
			session = Session.getDefaultInstance(mailProps);
		}

		// create a message
		final MimeMessage mimeMessage;
		if (email.getDkimPrivateKey() != null) {
			mimeMessage = new DkimSignedMessageForJakarta(session);
			((DkimSignedMessageForJakarta) mimeMessage).setDkimKeyData(email.getDkimDomain(), email.getDkimSelector(), email.getDkimPrivateKey(), email.getDkimIdentity());
		} else {
			mimeMessage = new MimeMessage(session);
		}

		mimeMessage.setFrom(email.getFromAddress());
		mimeMessage.setSubject(email.getSubject(), email.getCharset().name());
		mimeMessage.setSentDate(new Date());

		// Set reply-to address
		if (email.getReplyToAddressList().size() > 0) {
			mimeMessage.setReplyTo(email.getReplyToAddressList().toArray(new InternetAddress[0]));
		} else {
			// Use fromAddress as fallback
			mimeMessage.setReplyTo(new Address[] { email.getFromAddress() });
		}

		// Set to-recipient email addresses
		if (email.getToAddressList().size() > 0) {
			mimeMessage.setRecipients(Message.RecipientType.TO, email.getToAddressList().toArray(new InternetAddress[0]));
		}

		// Set cc-recipient email addresses
		if (email.getCcAddressList().size() > 0) {
			mimeMessage.setRecipients(Message.RecipientType.CC, email.getCcAddressList().toArray(new InternetAddress[0]));
		}

		// Set bcc-recipient email addresses
		if (email.getBccAddressList().size() > 0) {
			mimeMessage.setRecipients(Message.RecipientType.BCC, email.getBccAddressList().toArray(new InternetAddress[0]));
		}

		if (email.getAttachments() == null || email.getAttachments().size() == 0) {
			if (Utilities.isBlank(email.getBodyText()) && Utilities.isBlank(email.getBodyHtml())) {
				// Use a simple text email with only text content
				mimeMessage.setText("", email.getCharset().name());
			} else if (Utilities.isBlank(email.getBodyHtml())) {
				// Use a simple text email with only text content
				mimeMessage.setText(email.getBodyText(), email.getCharset().name());
			} else if (Utilities.isBlank(email.getBodyText())) {
				// Use a simple html email with only html content
				mimeMessage.setContent(email.getBodyHtml(), "text/html; charset=" + email.getCharset().name());
			} else {
				// Use a multipart email with text and html content
				//				if (email.getEncryptionCertificate() != null && email.getSignatureKey() != null) {
				//					final Multipart multipart = new MimeMultipart("alternative");
				//
				//					final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
				//					textMimeBodyPart.setText(email.getBodyText(), email.getCharset().name());
				//					multipart.addBodyPart(textMimeBodyPart);
				//					mimeMessage.setContent(multipart, multipart.getContentType());
				//
				//					final MimeBodyPart htmlMimeBodyPart = new MimeBodyPart();
				//					htmlMimeBodyPart.setContent(email.getBodyHtml(), "text/html; charset=" + email.getCharset().name());
				//					multipart.addBodyPart(htmlMimeBodyPart);
				//					mimeMessage.setContent(multipart, multipart.getContentType());
				//
				//					multipart.addBodyPart(textMimeBodyPart);
				//					multipart.addBodyPart(htmlMimeBodyPart);
				//
				//					final SMIMEEnvelopedGenerator fact = new SMIMEEnvelopedGenerator();
				//
				//					fact.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(email.getEncryptionCertificate()).setProvider("BC"));
				//
				//					final MimeBodyPart mp = fact.generate(mimeMessage, new JceCMSContentEncryptorBuilder(CMSAlgorithm.RC2_CBC, 40).setProvider("BC").build());
				//
				//					final String boundary = Utilities.generateUUID();
				//					final Multipart multipart = new MultipartSigned(boundary);
				//
				//					final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
				//					textMimeBodyPart.setContent(email.getBodyText(), "text/plain; charset=" + email.getCharset().name() + "; format=flowed");
				//					textMimeBodyPart.setHeader("Content-Transfer-Encoding", "quoted-printable");
				//					multipart.addBodyPart(textMimeBodyPart);
				//
				//					//					final byte[] signatureData = CryptographicUtilities.signData(email.getBodyText().getBytes(email.getCharset()), email.getSignatureKey());
				//					//					final String signatureDataBase64String = Utilities.encodeBase64(signatureData, 64, "\r\n");
				//					//
				//					//					final MimeBodyPart signatureMimeBodyPart = new MimeBodyPart();
				//					//					signatureMimeBodyPart.setContent(signatureDataBase64String, "application/pkcs7-signature; name=\"smime.p7s\"");
				//					//					signatureMimeBodyPart.setHeader("Content-Transfer-Encoding", "base64");
				//					//					signatureMimeBodyPart.setHeader("Content-Disposition", "attachment; filename=\"smime.p7s\"");
				//					//					signatureMimeBodyPart.setHeader("Content-Description", "S/MIME Cryptographic Signature");
				//					//					multipart.addBodyPart(signatureMimeBodyPart);
				//
				//					mimeMessage.setContent(multipart);
				//				} else if (email.getEncryptionCertificate() != null) {
				//					throw new NotImplementedException();
				//				} else if (email.getSignatureKey() != null) {
				//					throw new NotImplementedException();
				//				} else {
				final Multipart multipart = new MimeMultipart("alternative");

				final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
				textMimeBodyPart.setContent(email.getBodyText(), "text/plain; charset=" + email.getCharset().name());

				final MimeBodyPart htmlMimeBodyPart = new MimeBodyPart();
				htmlMimeBodyPart.setContent(email.getBodyHtml(), "text/html; charset=" + email.getCharset().name());

				multipart.addBodyPart(textMimeBodyPart);
				multipart.addBodyPart(htmlMimeBodyPart);

				mimeMessage.setContent(multipart, multipart.getContentType());
				//				}
			}
		} else {
			final Multipart multipartMixed = new MimeMultipart("mixed");

			if (Utilities.isBlank(email.getBodyText()) && Utilities.isBlank(email.getBodyHtml())) {
				// Use a multipart text email with only text content and attachements
				final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
				// Set to a single space. If body is totally empty, Commons Emails rejects sending email with exception "Invalid message supplied" (-> EMM-4133)
				textMimeBodyPart.setText(" ", email.getCharset().name());
				multipartMixed.addBodyPart(textMimeBodyPart);
			} else if (Utilities.isBlank(email.getBodyHtml())) {
				final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
				// Use a multipart text email with only text content and attachements
				textMimeBodyPart.setText(email.getBodyText(), email.getCharset().name());
				multipartMixed.addBodyPart(textMimeBodyPart);
			} else if (Utilities.isBlank(email.getBodyText())) {
				final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
				// Use a multipart html email with only html content and attachements
				textMimeBodyPart.setContent(email.getBodyHtml(), "text/html; charset=" + email.getCharset().name());
				multipartMixed.addBodyPart(textMimeBodyPart);
			} else {
				// Use a multipart html email with text and html content and attachements
				final Multipart multipartTextContent = new MimeMultipart("alternative");

				final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
				textMimeBodyPart.setText(email.getBodyText(), email.getCharset().name());
				multipartTextContent.addBodyPart(textMimeBodyPart);

				final MimeBodyPart htmlMimeBodyPart = new MimeBodyPart();
				htmlMimeBodyPart.setContent(email.getBodyHtml(), "text/html; charset=" + email.getCharset().name());
				multipartTextContent.addBodyPart(htmlMimeBodyPart);

				final MimeBodyPart alternativeMimeBodyPart = new MimeBodyPart();
				multipartMixed.addBodyPart(alternativeMimeBodyPart);
				alternativeMimeBodyPart.setContent(multipartTextContent);
			}

			for (final MailAttachment attachment : email.getAttachments()) {
				final MimeBodyPart attachmentMimeBodyPart = new MimeBodyPart();
				attachmentMimeBodyPart.setFileName(MimeUtility.encodeText(attachment.getName(), StandardCharsets.UTF_8.name(), null));
				if (attachment.getData() == null) {
					throw new Exception("Invalid empty mail attachment");
				}
				final ByteArrayDataSource bds = new ByteArrayDataSource(attachment.getData(), attachment.getMimeType());
				attachmentMimeBodyPart.setDataHandler(new DataHandler(bds));
				multipartMixed.addBodyPart(attachmentMimeBodyPart);
			}

			mimeMessage.setContent(multipartMixed);
		}

		Transport.send(mimeMessage);

		return this;
	}
}
