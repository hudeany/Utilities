package de.soderer.utilities.mail;

import java.util.Date;
import java.util.Properties;

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

public class Mailer {
	private String smtpMailRelayHostname = "localhost";
	private int smtpMailRelayPort = 0;

	private String smtpUsername = null;
	private char[] smtpPassword = null;

	private boolean useStartTLS = false;

	public String getSmtpMailRelayHostname() {
		return smtpMailRelayHostname;
	}

	public Mailer setSmtpMailRelayHostname(final String smtpMailRelayHostname) {
		this.smtpMailRelayHostname = smtpMailRelayHostname;
		return this;
	}

	public int getSmtpMailRelayPort() {
		return smtpMailRelayPort;
	}

	public Mailer setSmtpMailRelayPort(final int smtpMailRelayPort) {
		this.smtpMailRelayPort = smtpMailRelayPort;
		return this;
	}

	public String getSmtpUsername() {
		return smtpUsername;
	}

	public Mailer setSmtpUsername(final String smtpUsername) {
		this.smtpUsername = smtpUsername;
		return this;
	}

	public char[] getSmtpPassword() {
		return smtpPassword;
	}

	public Mailer setSmtpPassword(final char[] smtpPassword) {
		this.smtpPassword = smtpPassword;
		return this;
	}

	public boolean isUseStartTLS() {
		return useStartTLS;
	}

	public Mailer setUseStartTLS(final boolean useStartTLS) {
		this.useStartTLS = useStartTLS;
		return this;
	}

	public Mailer send(final Email email) throws Exception {
		final Properties mailProps = new Properties();
		mailProps.put("mail.smtp.from", email.getFromAddress().getAddress());
		mailProps.put("mail.smtp.host", smtpMailRelayHostname);
		mailProps.put("mail.smtp.port", smtpMailRelayPort);
		mailProps.put("mail.smtp.auth", true);
		mailProps.put("mail.smtp.socketFactory.port", smtpMailRelayPort);
		mailProps.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
		mailProps.put("mail.smtp.socketFactory.fallback", "false");
		if (useStartTLS) {
			mailProps.put("mail.smtp.starttls.enable", "true");
		}

		final Session mailSession;
		if (smtpUsername != null) {
			final Authenticator authenticator = new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(smtpUsername, new String(smtpPassword));
				}
			};
			mailSession = Session.getDefaultInstance(mailProps, authenticator);
		} else {
			mailSession = Session.getDefaultInstance(mailProps);
		}

		final MimeMessage message = new MimeMessage(mailSession);
		message.setFrom(email.getFromAddress());
		message.setRecipients(Message.RecipientType.TO, email.getToAddressList().toArray(new InternetAddress[0]));
		message.setSubject(email.getSubject(), email.getCharset().name());

		final Multipart multipart = new MimeMultipart();
		final MimeBodyPart mimeBodyPart = new MimeBodyPart();
		//		mimeBodyPart.setContent(email.getBodyHtml(), "text/html;charset=" + email.getCharset().name());
		mimeBodyPart.setContent(email.getBodyText(), "plain/text;charset=" + email.getCharset().name());
		multipart.addBodyPart(mimeBodyPart);
		message.setContent(multipart);
		message.setSentDate(new Date());

		Transport.send(message);

		return this;
	}
}
