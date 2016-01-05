package de.soderer.utilities;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SendMail {

	private static final Logger log = LoggerFactory.getLogger(SendMail.class);

	private static String SENDER_ADRESSE = "absender@sender.de";

	// adressenListe ist ein ";"-getrennter String mit den EmailAdressen
	public static boolean sendWarningMail(String adressenListe, String pBetreff, String pMailtext) throws Exception {
		List<String> zielEmailAdressen = new ArrayList<String>();
		while (adressenListe.indexOf(";") > -1) {
			zielEmailAdressen.add(adressenListe.substring(0, adressenListe.indexOf(";")).trim());
			adressenListe = adressenListe.substring(adressenListe.indexOf(";") + 1);
		}
		zielEmailAdressen.add(adressenListe.trim());

		StringBuffer subject = new StringBuffer();
		StringBuffer mailtext = new StringBuffer();

		// Betreff erstellen
		subject.append("WarningMail: " + pBetreff);

		// Mailtext erstellen
		mailtext.append(pMailtext);
		String lineFeed = "\n";
		mailtext.append(lineFeed + lineFeed);
		mailtext.append("HINWEIS: Diese E-Mail wurde automatisch von D7-IT-KuV3-B6 erstellt." + lineFeed);
		// + "Bitte schicken Sie keine Antwort an die absendende interne Mailadresse, da dort eine weitere Bearbeitung nicht möglich ist." + lineFeed);

		try {
			return sendStraightMail(zielEmailAdressen, null, null, subject.toString(), mailtext.toString(), null, null, null, true);
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean sendMail(String[] toEmailAdressen, String pBetreff, String pMailtext, File... filesToAttach) throws Exception {
		List<String> to = new ArrayList<String>();
		for (String adresse : toEmailAdressen) {
			to.add(adresse);
		}
		return sendMailWithSignature(to, null, null, pBetreff, pMailtext, filesToAttach);
	}

	public static boolean sendMail(String toEmailAdresse, String pBetreff, String pMailtext, File... filesToAttach) throws Exception {
		List<String> to = new ArrayList<String>();
		to.add(toEmailAdresse);
		return sendMailWithSignature(to, null, null, pBetreff, pMailtext, filesToAttach);
	}

	public static boolean sendMailWithSignature(List<String> pTO, List<String> pCC, List<String> pBCC, String pBetreff, String pMailtext, File... filesToAttach) throws Exception {
		return sendMailWithSignature(pTO, pCC, pBCC, pBetreff, pMailtext, false, filesToAttach);
	}

	/**
	 * Versenden einer Email mit Standardtextanhängen ohne die Rückgabe einer Exception => Optimistisch
	 * 
	 * @return
	 * @throws Exception
	 */
	public static boolean sendMailWithSignature(List<String> pTO, List<String> pCC, List<String> pBCC, String pBetreff, String pMailtext, boolean sendAsHtmlMail, File... filesToAttach)
			throws Exception {
		StringBuffer subject = new StringBuffer();
		StringBuffer mailtext = new StringBuffer();

		// Betreff erstellen
		subject.append(pBetreff);

		// Mailtext erstellen
		mailtext.append(pMailtext);
		String lineFeed = "\n";
		if (sendAsHtmlMail) {
			lineFeed = "<br>";
		}
		mailtext.append(lineFeed + lineFeed);
		mailtext.append("HINWEIS: Diese E-Mail wurde automatisch von D7-IT-KuV3-B6 erstellt." + lineFeed);
		// + "Bitte schicken Sie keine Antwort an die absendende interne Mailadresse, da dort eine weitere Bearbeitung nicht möglich ist." + lineFeed);

		try {
			return sendStraightMail(pTO, pCC, pBCC, subject.toString(), mailtext.toString(), null, null, null, sendAsHtmlMail, filesToAttach);
		} catch (Exception e) {
			// geloggt wurde bereits
			return false;
		}
	}

	public static boolean sendStraightMail(List<String> pTO, List<String> pCC, List<String> pBCC, String pBetreff, String pMailtext, byte[] pFileData, String fileName, String mimeType,
			boolean sendAsHtmlMail, File... filesToAttach) throws Exception {
		MimeMessage mimeMessage = null;
		// Email verschicken
		try {
			java.util.Properties sessionProperties = new java.util.Properties();
			sessionProperties.put("mail.smtp.user", "I_NET");
			sessionProperties.put("mail.smtp.host", "techuser1.mail.allianz");
			Session session = Session.getInstance(sessionProperties);
			session.setDebug(log.isDebugEnabled());
			// Email-Betreff darf auf keinen Fall Zeilenumbrüche enthalten
			pBetreff = pBetreff.replaceAll("\r", "").replaceAll("\n", " ");
			// Emailobjekt erzeugen und gegebenfalls Anhänge anfügen
			if (!ArrayUtils.isEmpty(filesToAttach) || !ArrayUtils.isEmpty(pFileData)) {
				// Create new Mail
				mimeMessage = new MimeMessage(session);
				mimeMessage.setFrom(new InternetAddress(SENDER_ADRESSE));
				mimeMessage.setSubject(MimeUtility.encodeText(pBetreff, "ISO-8859-1", null));
				mimeMessage.setSentDate(new Date());
				MimeBodyPart mainBody = new MimeBodyPart();
				mainBody.setText(pMailtext, "ISO-8859-1");
				MimeMultipart multiPart = new MimeMultipart();
				multiPart.addBodyPart(mainBody);
				mimeMessage.setContent(multiPart);
				// Add Attachments from Files
				if (!ArrayUtils.isEmpty(filesToAttach)) {
					for (File attachFile : filesToAttach) {
						MimeBodyPart attachmentBodyPart = new MimeBodyPart();
						attachFile(attachmentBodyPart, attachFile);
						// Nur mit JavaMail 1.4 (das offenbar nicht installiert ist) attachmentBodyPart.attachFile(attachFile);
						multiPart.addBodyPart(attachmentBodyPart);

						/*
						 * FileDataSource fileDataSource = new FileDataSource(attachFile); MimeBodyPart attachmentBodyPart = new MimeBodyPart(); attachmentBodyPart.setDataHandler(new
						 * DataHandler(fileDataSource)); multiPart.addBodyPart(attachmentBodyPart);
						 */
					}
				}
				// Add Attachment from Byte-Array
				if (!ArrayUtils.isEmpty(pFileData)) {
					MimeBodyPart attachmentBodyPart = new MimeBodyPart();
					attachmentBodyPart.setDataHandler(new DataHandler(new ByteArrayDataSource(pFileData, mimeType)));
					attachmentBodyPart.setFileName(fileName);
					multiPart.addBodyPart(attachmentBodyPart);
				}
			} else {
				if (sendAsHtmlMail) {
					mimeMessage = new MimeMessage(session);
					mimeMessage.setFrom(new InternetAddress(SENDER_ADRESSE));
					mimeMessage.setSubject(MimeUtility.encodeText(pBetreff, "ISO-8859-1", null));
					mimeMessage.setSentDate(new Date());
					mimeMessage.setContent(pMailtext, "text/html; charset=ISO-8859-1");
				} else {
					mimeMessage = new MimeMessage(session);
					mimeMessage.setFrom(new InternetAddress(SENDER_ADRESSE));
					mimeMessage.setSubject(MimeUtility.encodeText(pBetreff, "ISO-8859-1", null));
					mimeMessage.setSentDate(new Date());
					mimeMessage.setText(pMailtext, "ISO-8859-1");
				}
			}
			// TO Emails
			List<InternetAddress> toAdresses = new ArrayList<InternetAddress>();
			for (String recipient : pTO) {
				toAdresses.add(new InternetAddress(recipient));
			}
			InternetAddress[] toAddress = toAdresses.toArray(new InternetAddress[pTO.size()]);
			if ((toAddress != null) && (toAddress.length > 0)) {
				mimeMessage.setRecipients(Message.RecipientType.TO, toAddress);
			}
			// CC Emails
			if (pCC != null) {
				List<InternetAddress> ccAdresses = new ArrayList<InternetAddress>();
				for (String ccRecipient : pCC) {
					ccAdresses.add(new InternetAddress(ccRecipient));
				}
				InternetAddress[] ccAddress = ccAdresses.toArray(new InternetAddress[pCC.size()]);
				if ((ccAddress != null) && (ccAddress.length > 0)) {
					mimeMessage.setRecipients(Message.RecipientType.CC, ccAddress);
				}
			}
			// BCC Emails
			if (pBCC != null) {
				List<InternetAddress> bccAdresses = new ArrayList<InternetAddress>();
				for (String bccRecipient : pBCC) {
					bccAdresses.add(new InternetAddress(bccRecipient));
				}
				InternetAddress[] bccAddress = bccAdresses.toArray(new InternetAddress[pBCC.size()]);
				if ((bccAddress != null) && (bccAddress.length > 0)) {
					mimeMessage.setRecipients(Message.RecipientType.BCC, bccAddress);
				}
			}
			// Tatsächlich senden
			Transport transport = session.getTransport("smtp");
			transport.connect();
			Transport.send(mimeMessage);
			transport.close();
			return true;
		} catch (AddressException e) {
			log.error(e.getMessage(), e);
			throw new Exception("AddressException beim Emailversand", e);
		} catch (MessagingException e) {
			log.error(e.getMessage(), e);
			throw new Exception("MessagingException beim Emailversand", e);
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
			throw new Exception("UnsupportedEncodingException beim Emailversand", e);
		}
	}

	private static void attachFile(MimeBodyPart attachmentBodyPart, File attachFile) throws MessagingException {
		FileDataSource fds = new FileDataSource(attachFile);
		attachmentBodyPart.setDataHandler(new DataHandler(fds));
		attachmentBodyPart.setFileName(fds.getName());
	}
}