package de.soderer.utilities.mail;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.List;

import javax.mail.internet.InternetAddress;

import org.apache.commons.lang3.StringUtils;

public class EmailForJavaxMail {
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

	private RSAPrivateKey dkimPrivateKey = null;
	private String dkimDomain = null;
	private String dkimSelector = null;
	private String dkimIdentity = null;

	private PrivateKey signatureKey = null;
	private X509Certificate encryptionCertificate = null;

	public EmailForJavaxMail(final InternetAddress fromAddress, final String subject, final Charset charset) {
		this.fromAddress = fromAddress;
		this.subject = subject;
		this.charset = charset;
	}

	public EmailForJavaxMail(final String fromAddress, final String subject) throws Exception {
		this(new InternetAddress(fromAddress), subject, StandardCharsets.UTF_8);
	}

	public EmailForJavaxMail(final String fromAddress, final String subject, final Charset charset) throws Exception {
		this(new InternetAddress(fromAddress), subject, charset);
	}

	public EmailForJavaxMail(final String fromAddress, final String fromName, final String subject, final Charset charset) throws Exception {
		this(new InternetAddress(fromAddress, fromName), subject, charset);
	}

	public InternetAddress getFromAddress() {
		return fromAddress;
	}

	public String getSubject() {
		return subject;
	}

	public Charset getCharset() {
		if (charset != null) {
			return charset;
		} else {
			return StandardCharsets.UTF_8;
		}
	}

	public InternetAddress getBounceAddress() {
		return bounceAddress;
	}

	public EmailForJavaxMail setBounceAddress(final InternetAddress bounceAddress) {
		this.bounceAddress = bounceAddress;
		return this;
	}

	public EmailForJavaxMail setBounceAddress(final String bounceAddress) throws Exception {
		this.bounceAddress = new InternetAddress(bounceAddress);
		return this;
	}

	public EmailForJavaxMail setBounceAddress(final String bounceAddress, final String bounceAddressName) throws Exception {
		this.bounceAddress = new InternetAddress(bounceAddress, bounceAddressName);
		return this;
	}

	public List<InternetAddress> getReplyToAddressList() {
		return replyToAddressList;
	}

	public EmailForJavaxMail addReplyToAddress(final InternetAddress replyToAddress) {
		replyToAddressList.add(replyToAddress);
		return this;
	}

	public EmailForJavaxMail addReplyToAddress(final String replyToAddress) throws Exception {
		replyToAddressList.add(new InternetAddress(replyToAddress));
		return this;
	}

	public EmailForJavaxMail addReplyToAddress(final String replyToAddress, final String replyToAddressName) throws Exception {
		replyToAddressList.add(new InternetAddress(replyToAddress, replyToAddressName));
		return this;
	}

	public List<InternetAddress> getToAddressList() {
		return toAddressList;
	}

	public EmailForJavaxMail setToAddressList(final List<InternetAddress> toAddressList) {
		this.toAddressList = toAddressList;
		return this;
	}

	public EmailForJavaxMail addToAddress(final InternetAddress toAddress) {
		toAddressList.add(toAddress);
		return this;
	}

	public EmailForJavaxMail addToAddress(final String toAddress) throws Exception {
		toAddressList.add(new InternetAddress(toAddress));
		return this;
	}

	public EmailForJavaxMail addToAddress(final String toAddress, final String toAddressName) throws Exception {
		toAddressList.add(new InternetAddress(toAddress, toAddressName));
		return this;
	}

	public List<InternetAddress> getCcAddressList() {
		return ccAddressList;
	}

	public EmailForJavaxMail setCcAddressList(final List<InternetAddress> ccAddressList) {
		this.ccAddressList = ccAddressList;
		return this;
	}

	public EmailForJavaxMail addCcAddress(final InternetAddress ccAddress) {
		ccAddressList.add(ccAddress);
		return this;
	}

	public EmailForJavaxMail addCcAddress(final String ccAddress) throws Exception {
		ccAddressList.add(new InternetAddress(ccAddress));
		return this;
	}

	public EmailForJavaxMail addCcAddress(final String ccAddress, final String ccAddressName) throws Exception {
		ccAddressList.add(new InternetAddress(ccAddress, ccAddressName));
		return this;
	}

	public List<InternetAddress> getBccAddressList() {
		return bccAddressList;
	}

	public EmailForJavaxMail setBccAddressList(final List<InternetAddress> bccAddressList) {
		this.bccAddressList = bccAddressList;
		return this;
	}

	public EmailForJavaxMail addBccAddress(final InternetAddress bccAddress) {
		bccAddressList.add(bccAddress);
		return this;
	}

	public EmailForJavaxMail addBccAddress(final String bccAddress) throws Exception {
		bccAddressList.add(new InternetAddress(bccAddress));
		return this;
	}

	public EmailForJavaxMail addBccAddress(final String bccAddress, final String bccAddressName) throws Exception {
		bccAddressList.add(new InternetAddress(bccAddress, bccAddressName));
		return this;
	}

	public String getBodyText() {
		return bodyText;
	}

	public EmailForJavaxMail setBodyText(final String bodyText) {
		this.bodyText = bodyText;
		return this;
	}

	public String getBodyHtml() {
		return bodyHtml;
	}

	public EmailForJavaxMail setBodyHtml(final String bodyHtml) {
		this.bodyHtml = bodyHtml;
		return this;
	}

	public List<MailAttachment> getAttachments() {
		return attachments;
	}

	public EmailForJavaxMail setAttachments(final List<MailAttachment> attachments) {
		this.attachments = attachments;
		return this;
	}

	public EmailForJavaxMail addAttachment(final MailAttachment attachment) {
		attachments.add(attachment);
		return this;
	}

	public EmailForJavaxMail setDkimData(final String domain, final String selector, final RSAPrivateKey privateKey) throws Exception {
		return setDkimData(domain, selector, privateKey, null);
	}

	public EmailForJavaxMail setDkimData(final String domain, final String selector, final RSAPrivateKey privateKey, final String identity) throws Exception {
		if (StringUtils.isBlank(domain)) {
			throw new Exception("DKIM domain may not be empty");
		} else if (StringUtils.isBlank(selector)) {
			throw new Exception("DKIM key selector may not be empty");
		} else if (privateKey == null) {
			throw new Exception("DKIM private key may not be empty");
		}

		dkimDomain = domain;
		dkimSelector = selector;
		dkimPrivateKey = privateKey;
		dkimIdentity = identity;

		return this;
	}

	public RSAPrivateKey getDkimPrivateKey() {
		return dkimPrivateKey;
	}

	public String getDkimDomain() {
		return dkimDomain;
	}

	public String getDkimSelector() {
		return dkimSelector;
	}

	public String getDkimIdentity() {
		return dkimIdentity;
	}

	public PrivateKey getSignatureKey() {
		return signatureKey;
	}

	public void setSignaturePrivateKey(final PrivateKey signatureKey) {
		this.signatureKey = signatureKey;
	}

	public X509Certificate getEncryptionCertificate() {
		return encryptionCertificate;
	}

	public void setEncryptionCertificate(final X509Certificate encryptionCertificate) {
		this.encryptionCertificate = encryptionCertificate;
	}
}
