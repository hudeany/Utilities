package de.soderer.utilities.mail;

import de.soderer.utilities.Utilities;
import jakarta.mail.internet.ContentType;
import jakarta.mail.internet.MimeMultipart;

public class MultipartSigned extends MimeMultipart {
	public MultipartSigned() throws Exception {
		this(null);
	}

	public MultipartSigned(final String boundary) throws Exception {
		//		super("signed");
		super("alternative");

		final ContentType newContentType = new ContentType(contentType);
		newContentType.setParameter("protocol", "application/pkcs7-signature");
		newContentType.setParameter("micalg", "sha-512");
		if (!Utilities.isBlank(boundary)) {
			newContentType.setParameter("boundary", boundary);
		}
		contentType = newContentType.toString();
	}
}
