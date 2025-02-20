package de.soderer.utilities.mail;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;

public class MimeMessageWithId extends MimeMessage {
	private String messageID = null;

	public MimeMessageWithId(final Session session, final String messageID) {
		super(session);

		this.messageID = messageID;
	}

	@Override
	protected void updateMessageID() throws MessagingException {
		if (messageID != null) {
			setHeader("Message-ID", messageID);
		}
	}
}
