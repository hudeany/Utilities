package de.soderer.utilities.mail;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

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
