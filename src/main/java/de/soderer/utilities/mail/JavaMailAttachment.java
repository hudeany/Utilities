package de.soderer.utilities.mail;

public class JavaMailAttachment {
	private final String name;
	private final byte[] data;
	private final String mimeType;

	public JavaMailAttachment(final String name, final byte[] data, final String mimeType) {
		this.name = name;
		this.data = data;
		this.mimeType = mimeType;
	}

	public String getName() {
		return name;
	}

	public byte[] getData() {
		return data;
	}

	public String getMimeType() {
		return mimeType;
	}
}
