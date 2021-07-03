package de.soderer.utilities.mail;

import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.SendFailedException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;

import com.sun.mail.smtp.SMTPAddressFailedException;

import de.soderer.utilities.Utilities;
import de.soderer.utilities.crypto.CryptographicUtilities;
import de.soderer.utilities.mail.dkim.DkimSignedMessage;

public class Mailer {
	private String smtpMailRelayHostname = "localhost";
	private int smtpMailRelayPort = 0;

	private String smtpUsername = null;
	private char[] smtpPassword = null;

	private MailerConnectionSecurity connectionSecurity = MailerConnectionSecurity.None;

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

	public MailerConnectionSecurity getConnectionSecurity() {
		return connectionSecurity;
	}

	public Mailer setConnectionSecurity(final MailerConnectionSecurity connectionSecurity) {
		this.connectionSecurity = connectionSecurity;
		return this;
	}

	public Mailer send(final Email email) throws Exception {
		if (Utilities.isBlank(smtpMailRelayHostname)) {
			smtpMailRelayHostname = "localhost";
		}

		final Properties mailProps = new Properties();
		mailProps.put("mail.smtp.from", email.getFromAddress().getAddress());
		mailProps.put("mail.smtp.host", smtpMailRelayHostname);
		mailProps.put("mail.smtp.port", smtpMailRelayPort);

		if (connectionSecurity == MailerConnectionSecurity.SSL_TLS) {
			mailProps.put("mail.smtp.socketFactory.port", smtpMailRelayPort);
			mailProps.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
			mailProps.put("mail.smtp.socketFactory.fallback", "false");
		} else if (connectionSecurity == MailerConnectionSecurity.STARTTLS) {
			mailProps.put("mail.smtp.starttls.enable", "true");
		}

		final Session session;
		if (smtpUsername != null) {
			mailProps.put("mail.smtp.auth", true);
			final Authenticator authenticator = new Authenticator() {
				@Override
				protected PasswordAuthentication getPasswordAuthentication() {
					return new PasswordAuthentication(smtpUsername, new String(smtpPassword));
				}
			};
			session = Session.getDefaultInstance(mailProps, authenticator);
		} else {
			mailProps.put("mail.smtp.auth", false);
			session = Session.getDefaultInstance(mailProps);
		}

		// create a message
		final String messageID = Utilities.generateUUID();
		final MimeMessage mimeMessage;
		if (email.getDkimPrivateKey() != null) {
			mimeMessage = new DkimSignedMessage(session, messageID);
			((DkimSignedMessage) mimeMessage).setDkimKeyData(email.getDkimDomain(), email.getDkimSelector(), email.getDkimPrivateKey(), email.getDkimIdentity());
		} else {
			mimeMessage = new de.soderer.utilities.mail.MimeMessage(session, messageID);
		}

		mimeMessage.setFrom(email.getFromAddress().getAddress());
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
				final Multipart multipart = new de.soderer.utilities.mail.MimeMultipart("alternative", Utilities.generateUUID());

				final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
				textMimeBodyPart.setContent(email.getBodyText(), "text/plain; charset=" + email.getCharset().name());

				final MimeBodyPart htmlMimeBodyPart = new MimeBodyPart();
				htmlMimeBodyPart.setContent(email.getBodyHtml(), "text/html; charset=" + email.getCharset().name());

				multipart.addBodyPart(textMimeBodyPart);
				multipart.addBodyPart(htmlMimeBodyPart);

				mimeMessage.setContent(multipart, multipart.getContentType());
			}
		} else {
			final Multipart multipartMixed = new de.soderer.utilities.mail.MimeMultipart("mixed", Utilities.generateUUID());

			if (Utilities.isBlank(email.getBodyText()) && Utilities.isBlank(email.getBodyHtml())) {
				// Use a multipart text email with only text content and attachements
				final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
				textMimeBodyPart.setText("", email.getCharset().name());
				multipartMixed.addBodyPart(textMimeBodyPart);
			} else if (Utilities.isBlank(email.getBodyHtml())) {
				final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
				// Use a multipart text email with only text content and attachements
				textMimeBodyPart.setContent(email.getBodyText(), "text/plain; charset=" + email.getCharset().name());
				multipartMixed.addBodyPart(textMimeBodyPart);
			} else if (Utilities.isBlank(email.getBodyText())) {
				final MimeBodyPart textMimeBodyPart = new MimeBodyPart();
				// Use a multipart html email with only html content and attachements
				textMimeBodyPart.setContent(email.getBodyHtml(), "text/html; charset=" + email.getCharset().name());
				multipartMixed.addBodyPart(textMimeBodyPart);
			} else {
				// Use a multipart html email with text and html content and attachements
				final Multipart multipartTextContent = new de.soderer.utilities.mail.MimeMultipart("alternative", Utilities.generateUUID());

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

		if (email.getCryptoType() == CryptoType.S_MIME) {
			if (email.getSignatureKey() != null) {
				if (email.getSignatureCertificate() == null) {
					throw new Exception("Signature certificate is missing");
				}
				signMessageWithSmime(mimeMessage, email.getSignatureKey(), email.getSignatureCertificate(), email.getSignatureMethodName());
			}

			if (email.getEncryptionCertificate() != null) {
				encryptMessageWithSmime(mimeMessage, email.getEncryptionCertificate(), email.getEncryptionMethodName());
			}
		} else if (email.getCryptoType() == CryptoType.PGP) {
			if (email.getSignatureKey() != null) {
				if (email.getSignatureCertificate() == null) {
					throw new Exception("Signature certificate is missing");
				}
				signMessageWithPgp(mimeMessage, email.getSignatureKey(), email.getSignatureCertificate(), email.getSignatureMethodName());
			}

			if (email.getEncryptionCertificate() != null) {
				encryptMessageWithPgp(mimeMessage, email.getEncryptionCertificate(), email.getEncryptionMethodName());
			}
		}

		try {
			Transport.send(mimeMessage);
		} catch (final SendFailedException e) {
			if (e.getCause() != null && e.getCause() instanceof SMTPAddressFailedException && e.getCause().getMessage() != null) {
				if (e.getCause().getMessage().contains("Relay access denied")) {
					throw new Exception("Server '" + smtpMailRelayHostname + "' denied relay of this email", e);
				} else if (e.getCause().getMessage().contains("Client host rejected")) {
					throw new Exception("Server '" + smtpMailRelayHostname + "' denied relay of this email. Your host is not allowed to send emails.", e);
				} else if (e.getCause().getMessage().contains("Access denied")) {
					throw new Exception("Server '" + smtpMailRelayHostname + "' denied relay of this email. Maybe authentification is needed.", e);
				} else {
					throw e;
				}
			} else {
				throw e;
			}
		}

		return this;
	}

	private static MimeMessage signMessageWithSmime(final MimeMessage message, final PrivateKey privateKey, final X509Certificate signingCertificate, final String signatureMethodName) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final SMIMECapabilityVector capabilities = new SMIMECapabilityVector();
		capabilities.addCapability(SMIMECapability.dES_EDE3_CBC);
		capabilities.addCapability(SMIMECapability.rC2_CBC, 128);
		capabilities.addCapability(SMIMECapability.dES_CBC);
		capabilities.addCapability(SMIMECapability.aES256_CBC);

		final ASN1EncodableVector attributes = new ASN1EncodableVector();
		attributes.add(new SMIMECapabilitiesAttribute(capabilities));

		final SMIMESignedGenerator signer = new SMIMESignedGenerator();


		signer.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder().setProvider("BC")
				.setSignedAttributeGenerator(new AttributeTable(attributes))
				.build(Utilities.isBlank(signatureMethodName) ? "SHA512withRSA" : signatureMethodName, privateKey, signingCertificate));

		final List<X509Certificate> certList = new ArrayList<>();
		certList.add(signingCertificate);
		signer.addCertificates(new JcaCertStore(certList));

		final MimeMultipart mimeMultipart = signer.generate(message);

		message.setContent(mimeMultipart, mimeMultipart.getContentType());
		message.saveChanges();

		return message;
	}

	private static MimeMessage encryptMessageWithSmime(final MimeMessage message, final X509Certificate recipientCert, final String encryptionMethodName) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		final SMIMEEnvelopedGenerator envelopedGenerator = new SMIMEEnvelopedGenerator();
		envelopedGenerator.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(recipientCert).setProvider("BC"));

		final MimeBodyPart mimeBodyPart = new MimeBodyPart();
		mimeBodyPart.setContent(message.getContent(), message.getContentType());
		final ASN1ObjectIdentifier encryptionAlgorithmIdentifier = Utilities.isBlank(encryptionMethodName) ? CMSAlgorithm.AES256_CBC : CryptographicUtilities.getASN1ObjectIdentifierByEncryptionMethodName(encryptionMethodName);
		final MimeBodyPart encryptedMimeBodyPart = envelopedGenerator.generate(mimeBodyPart, new JceCMSContentEncryptorBuilder(encryptionAlgorithmIdentifier).setProvider("BC").build());
		message.setContent(encryptedMimeBodyPart.getContent(), encryptedMimeBodyPart.getContentType());
		message.saveChanges();

		return message;
	}

	private static MimeMessage signMessageWithPgp(final MimeMessage message, final PrivateKey privateKey, final X509Certificate signingCertificate, final String signatureMethodName) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		//		final EncryptionUtils pgpUtils = EncryptionManager.getEncryptionUtils(EncryptionManager.PGP);
		//		final MimeMessage pgpSignedMsg = pgpUtils.signMessage(mailSession, newMessage, pgpKey);

		return message;
	}

	private static MimeMessage encryptMessageWithPgp(final MimeMessage message, final X509Certificate recipientCert, final String encryptionMethodName) throws Exception {
		Security.addProvider(new BouncyCastleProvider());

		//		final EncryptionUtils pgpUtils = EncryptionManager.getEncryptionUtils(EncryptionManager.PGP);
		//		final MimeMessage pgpEncryptedMsg = pgpUtils.encryptMessage(mailSession, newMessage, pgpKey);

		return message;
	}
}
