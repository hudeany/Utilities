package de.soderer.utilities.mail;

import javax.mail.internet.ContentType;

import de.soderer.utilities.Utilities;

public class MimeMultipart extends javax.mail.internet.MimeMultipart {
	public MimeMultipart(final String multipartSubtype, final String boundary) throws Exception {
		super(multipartSubtype);

		final ContentType newContentType = new ContentType(contentType);
		if (!Utilities.isBlank(boundary)) {
			newContentType.setParameter("boundary", boundary);
		}
		contentType = newContentType.toString();
	}
}
