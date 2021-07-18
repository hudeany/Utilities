package de.soderer.utilities.mail;

import java.util.Map;
import java.util.Map.Entry;

import javax.mail.internet.ContentType;

import de.soderer.utilities.Utilities;

public class MimeMultipart extends javax.mail.internet.MimeMultipart {
	public MimeMultipart(final String multipartSubtype, final String boundary) throws Exception {
		this(multipartSubtype, boundary, null);
	}

	public MimeMultipart(final String multipartSubtype, final String boundary, final Map<String, String> additionalContentParameters) throws Exception {
		super(multipartSubtype);

		final ContentType newContentType = new ContentType(contentType);

		if (additionalContentParameters != null) {
			for (final Entry<String, String> entry : additionalContentParameters.entrySet()) {
				newContentType.setParameter(entry.getKey(), entry.getValue());
			}
		}

		if (!Utilities.isBlank(boundary)) {
			newContentType.setParameter("boundary", boundary);
		}

		contentType = newContentType.toString();
	}
}
