package de.soderer.utilities.mail;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import jakarta.mail.internet.InternetAddress;

public class Email {
	private final InternetAddress fromAddress;
	private final String subject;
	private final Charset charset;

	private InternetAddress bounceAddress = null;

	private final List<InternetAddress> replyToAddressList = new ArrayList<>();

	private List<InternetAddress> toAddressList = new ArrayList<>();
	private List<InternetAddress> ccAddressList = new ArrayList<>();
	private List<InternetAddress> bccAddressList = new ArrayList<>();

	private String bodyText = null;
	private String bodyHtml = null;

	private List<MailAttachment> attachments = new ArrayList<>();

	public Email(final InternetAddress fromAddress, final String subject, final Charset charset) {
		this.fromAddress = fromAddress;
		this.subject = subject;
		this.charset = charset;
	}

	public Email(final String fromAddress, final String subject, final Charset charset) throws Exception {
		this(new InternetAddress(fromAddress), subject, charset);
	}

	public Email(final String fromAddress, final String fromName, final String subject, final Charset charset) throws Exception {
		this(new InternetAddress(fromAddress, fromName), subject, charset);
	}

	public InternetAddress getFromAddress() {
		return fromAddress;
	}

	public String getSubject() {
		return subject;
	}

	public Charset getCharset() {
		return charset;
	}

	public InternetAddress getBounceAddress() {
		return bounceAddress;
	}

	public Email setBounceAddress(final InternetAddress bounceAddress) {
		this.bounceAddress = bounceAddress;
		return this;
	}

	public Email setBounceAddress(final String bounceAddress) throws Exception {
		this.bounceAddress = new InternetAddress(bounceAddress);
		return this;
	}

	public Email setBounceAddress(final String bounceAddress, final String bounceAddressName) throws Exception {
		this.bounceAddress = new InternetAddress(bounceAddress, bounceAddressName);
		return this;
	}

	public List<InternetAddress> getReplyToAddressList() {
		return replyToAddressList;
	}

	public Email addReplyToAddress(final InternetAddress replyToAddress) {
		replyToAddressList.add(replyToAddress);
		return this;
	}

	public Email addReplyToAddress(final String replyToAddress) throws Exception {
		replyToAddressList.add(new InternetAddress(replyToAddress));
		return this;
	}

	public Email addReplyToAddress(final String replyToAddress, final String replyToAddressName) throws Exception {
		replyToAddressList.add(new InternetAddress(replyToAddress, replyToAddressName));
		return this;
	}

	public List<InternetAddress> getToAddressList() {
		return toAddressList;
	}

	public Email setToAddressList(final List<InternetAddress> toAddressList) {
		this.toAddressList = toAddressList;
		return this;
	}

	public Email addToAddress(final InternetAddress toAddress) {
		toAddressList.add(toAddress);
		return this;
	}

	public Email addToAddress(final String toAddress) throws Exception {
		toAddressList.add(new InternetAddress(toAddress));
		return this;
	}

	public Email addToAddress(final String toAddress, final String toAddressName) throws Exception {
		toAddressList.add(new InternetAddress(toAddress, toAddressName));
		return this;
	}

	public List<InternetAddress> getCcAddressList() {
		return ccAddressList;
	}

	public Email setCcAddressList(final List<InternetAddress> ccAddressList) {
		this.ccAddressList = ccAddressList;
		return this;
	}

	public Email addCcAddress(final InternetAddress ccAddress) {
		ccAddressList.add(ccAddress);
		return this;
	}

	public Email addCcAddress(final String ccAddress) throws Exception {
		ccAddressList.add(new InternetAddress(ccAddress));
		return this;
	}

	public Email addCcAddress(final String ccAddress, final String ccAddressName) throws Exception {
		ccAddressList.add(new InternetAddress(ccAddress, ccAddressName));
		return this;
	}

	public List<InternetAddress> getBccAddressList() {
		return bccAddressList;
	}

	public Email setBccAddressList(final List<InternetAddress> bccAddressList) {
		this.bccAddressList = bccAddressList;
		return this;
	}

	public Email addBccAddress(final InternetAddress bccAddress) {
		bccAddressList.add(bccAddress);
		return this;
	}

	public Email addBccAddress(final String bccAddress) throws Exception {
		bccAddressList.add(new InternetAddress(bccAddress));
		return this;
	}

	public Email addBccAddress(final String bccAddress, final String bccAddressName) throws Exception {
		bccAddressList.add(new InternetAddress(bccAddress, bccAddressName));
		return this;
	}

	public String getBodyText() {
		return bodyText;
	}

	public Email setBodyText(final String bodyText) {
		this.bodyText = bodyText;
		return this;
	}

	public String getBodyHtml() {
		return bodyHtml;
	}

	public Email setBodyHtml(final String bodyHtml) {
		this.bodyHtml = bodyHtml;
		return this;
	}

	public List<MailAttachment> getAttachments() {
		return attachments;
	}

	public Email setAttachments(final List<MailAttachment> attachments) {
		this.attachments = attachments;
		return this;
	}

	public Email addAttachment(final MailAttachment attachment) {
		attachments.add(attachment);
		return this;
	}
}
