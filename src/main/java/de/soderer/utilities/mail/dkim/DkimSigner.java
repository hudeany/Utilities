package de.soderer.utilities.mail.dkim;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import javax.mail.Header;
import javax.mail.MessagingException;

import org.apache.commons.io.IOUtils;

import com.sun.mail.util.CRLFOutputStream;
import com.sun.mail.util.QPEncoderStream;

/**
 * Main class providing a signature according to DKIM RFC 4871.
 */
public class DkimSigner {
	private static final int MAX_HEADER_LENGTH = 67;
	private static final String DKIM_SIGNATUR_HEADER = "DKIM-Signature";
	private static final Pattern SIGNING_DOMAIN_PATTERN = Pattern.compile("(.+)\\.(.+)");
	private static final Set<String> MANDATORY_HEADERS_TO_SIGN = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	private static final Set<String> DEFAULT_HEADERS_TO_SIGN = new HashSet<>();

	static {
		MANDATORY_HEADERS_TO_SIGN.add("From");

		DEFAULT_HEADERS_TO_SIGN.addAll(MANDATORY_HEADERS_TO_SIGN);
		DEFAULT_HEADERS_TO_SIGN.add("To");
		DEFAULT_HEADERS_TO_SIGN.add("Subject");
		DEFAULT_HEADERS_TO_SIGN.add("Content-Description");
		DEFAULT_HEADERS_TO_SIGN.add("Content-ID");
		DEFAULT_HEADERS_TO_SIGN.add("Content-Type");
		DEFAULT_HEADERS_TO_SIGN.add("Content-Transfer-Encoding");
		DEFAULT_HEADERS_TO_SIGN.add("Cc");
		DEFAULT_HEADERS_TO_SIGN.add("Date");
		DEFAULT_HEADERS_TO_SIGN.add("In-Reply-To");
		DEFAULT_HEADERS_TO_SIGN.add("List-Subscribe");
		DEFAULT_HEADERS_TO_SIGN.add("List-Post");
		DEFAULT_HEADERS_TO_SIGN.add("List-Owner");
		DEFAULT_HEADERS_TO_SIGN.add("List-Id");
		DEFAULT_HEADERS_TO_SIGN.add("List-Archive");
		DEFAULT_HEADERS_TO_SIGN.add("List-Help");
		DEFAULT_HEADERS_TO_SIGN.add("List-Unsubscribe");
		DEFAULT_HEADERS_TO_SIGN.add("MIME-Version");
		DEFAULT_HEADERS_TO_SIGN.add("Message-ID");
		DEFAULT_HEADERS_TO_SIGN.add("Resent-Sender");
		DEFAULT_HEADERS_TO_SIGN.add("Resent-Cc");
		DEFAULT_HEADERS_TO_SIGN.add("Resent-Date");
		DEFAULT_HEADERS_TO_SIGN.add("Resent-To");
		DEFAULT_HEADERS_TO_SIGN.add("Reply-To");
		DEFAULT_HEADERS_TO_SIGN.add("References");
		DEFAULT_HEADERS_TO_SIGN.add("Resent-Message-ID");
		DEFAULT_HEADERS_TO_SIGN.add("Resent-From");
		DEFAULT_HEADERS_TO_SIGN.add("Sender");
	}

	private final Set<String> headersToSign = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

	private final String signingDomain;
	private final String selector;

	private final DkimKeyPairType keyPairType;
	private final PrivateKey privateKey;

	private DkimSigningAlgorithm signingAlgorithm;
	private MessageDigest messageDigest;
	private Signature signature;

	private DkimCanonicalization headerCanonicalization;
	private DkimCanonicalization bodyCanonicalization;

	private String identity;

	private boolean checkDomainKey = false;

	/**
	 * Created a new {@code DkimSigner} for the given signing domain and
	 * selector with the given {@link RSAPrivateKey}.
	 *
	 * @param signingDomain
	 *            The signing domain to be used.
	 * @param selector
	 *            The selector to be used.
	 * @param privateKey
	 *            The {@link RSAPrivateKey} to be used to sign
	 *            {@link DkimMessage DkimMessage}s.
	 * @throws DkimException
	 *             If the given signing domain is invalid.
	 */
	public DkimSigner(final String signingDomain, final String selector, final RSAPrivateKey privateKey) throws DkimException {
		checkSigningDomain(signingDomain);
		headersToSign.addAll(DEFAULT_HEADERS_TO_SIGN);
		this.signingDomain = signingDomain;
		this.selector = selector.trim();
		keyPairType = DkimKeyPairType.RSA;
		this.privateKey = privateKey;
		setSigningAlgorithm(keyPairType.getDefaultSigningAlgorithm());
		setHeaderCanonicalization(DkimCanonicalization.RELAXED);
		setBodyCanonicalization(DkimCanonicalization.SIMPLE);
		setCheckDomainKey(true);
	}

	private static void checkSigningDomain(final String signingDomainToCheck) {
		if (null == signingDomainToCheck || !SIGNING_DOMAIN_PATTERN.matcher(signingDomainToCheck).matches()) {
			throw new DkimException(signingDomainToCheck + " is an invalid signing domain");
		}
	}

	/**
	 * Adds a header to the set of headers that will be included in the
	 * signature, if present.
	 *
	 * @param header
	 *            The name of the header.
	 */
	public void addHeaderToSign(final String header) {
		if (null != header && 0 != header.length() && !DKIM_SIGNATUR_HEADER.equalsIgnoreCase(header)) {
			headersToSign.add(header);
		}
	}

	/**
	 * Removes a header from the set of headers that will be included in the
	 * signature, unless it is one of the required headers ('From', 'To',
	 * 'Subject').
	 *
	 * @param header
	 *            The name of the header.
	 */
	public void removeHeaderToSign(final String header) {
		if (null != header && 0 != header.length() && !isMandatoryHeader(header)) {
			headersToSign.remove(header);
		}
	}

	private static boolean isMandatoryHeader(final String header) {
		return MANDATORY_HEADERS_TO_SIGN.contains(header);
	}

	/**
	 * Returns the configured {@link DkimSigningAlgorithm}.
	 *
	 * @return The configured {@link DkimSigningAlgorithm}.
	 */
	public DkimSigningAlgorithm getSigningAlgorithm() {
		return signingAlgorithm;
	}

	/**
	 * Sets the {@link DkimSigningAlgorithm} to be used.
	 *
	 * @param signingAlgorithm
	 *            The {@link DkimSigningAlgorithm} to be used.
	 *
	 * @throws DkimException
	 *             If either the signing algorithm or the hashing algorithm is
	 *             not supported or the {@link Signature} couldn't be
	 *             initialized.
	 */
	public void setSigningAlgorithm(final DkimSigningAlgorithm signingAlgorithm) throws DkimException {
		if (!keyPairType.supportsSigningAlgorithm(signingAlgorithm)) {
			throw new DkimException("Unsupported signing algorithm: " + signingAlgorithm);
		}

		try {
			messageDigest = MessageDigest.getInstance(signingAlgorithm.getHashNotation());
		} catch (final NoSuchAlgorithmException e) {
			throw new DkimException("Unknown hashing algorithm: " + signingAlgorithm.getHashNotation(), e);
		}

		try {
			signature = Signature.getInstance(signingAlgorithm.getJavaNotation());
			signature.initSign(privateKey);
		} catch (final NoSuchAlgorithmException e) {
			throw new DkimException("Unknown signing algorithm " + signingAlgorithm.getJavaNotation(), e);
		} catch (final InvalidKeyException e) {
			throw new DkimException("Invalid private key", e);
		}

		this.signingAlgorithm = signingAlgorithm;
	}

	/**
	 * Returns the configured {@link DkimCanonicalization} to be used for the
	 * headers.
	 *
	 * @return The configured {@link DkimCanonicalization} to be used for the
	 *         headers.
	 */
	public DkimCanonicalization getHeaderCanonicalization() {
		return headerCanonicalization;
	}

	/**
	 * Sets the {@link DkimCanonicalization} to be used for the headers.
	 *
	 * @param canonicalization
	 *            The {@link DkimCanonicalization} to be used for the headers.
	 */
	public void setHeaderCanonicalization(final DkimCanonicalization canonicalization) {
		headerCanonicalization = canonicalization;
	}

	/**
	 * Returns the configured {@link DkimCanonicalization} to be used for the body.
	 *
	 * @return The configured {@link DkimCanonicalization} to be used for the body.
	 */
	public DkimCanonicalization getBodyCanonicalization() {
		return bodyCanonicalization;
	}

	/**
	 * Sets the {@link DkimCanonicalization} to be used for the body.
	 *
	 * @param canonicalization
	 *            The {@link DkimCanonicalization} to be used for the body.
	 */
	public void setBodyCanonicalization(final DkimCanonicalization canonicalization) {
		bodyCanonicalization = canonicalization;
	}

	/**
	 * Returns the configured identity parameter.
	 *
	 * @return The configured identity parameter.
	 */
	public String getIdentity() {
		return identity;
	}

	/**
	 * Sets the identity parameter to be used.
	 *
	 * @param identity
	 *            The identity to be used.
	 * @throws DkimException
	 *             If the given identity parameter isn't the signing domain of
	 *             this {@code DkimSigner} or an subdomain thereof.
	 */
	public void setIdentity(final String identity) throws DkimException {
		if (null != identity) {
			checkIdentity(identity);
		}
		this.identity = identity;
	}

	private void checkIdentity(final String identityToCheck) {
		if (!identityToCheck.endsWith("@" + signingDomain) && !identityToCheck.endsWith("." + signingDomain)) {
			throw new DkimException("The domain part of " + identityToCheck + " isn't " + signingDomain + " or a subdomain thereof");
		}
	}

	/**
	 * Returns whether the domain key should be retrieved and checked.
	 *
	 * @return Whether the domain key should be retrieved and checked.
	 * @see DomainKey#check(String, RSAPrivateKey)
	 */
	public boolean isCheckDomainKey() {
		return checkDomainKey;
	}

	/**
	 * Sets, whether the domain key should be retrieved and checked.
	 *
	 * @param checkDomainKey
	 *            Whether the domain key should be retrieved and checked.
	 */
	public void setCheckDomainKey(final boolean checkDomainKey) {
		this.checkDomainKey = checkDomainKey;
	}

	/**
	 * Returns the DKIM signature header line.
	 *
	 * @param message
	 *            The {@link DkimMessage} to sign.
	 * @return The DKIM signature header line
	 * @throws IOException
	 * @throws DkimSigningException
	 *             If the {@link DkimMessage} couldn't be signed.
	 */
	protected String sign(final DkimMessage message) throws MessagingException, IOException {
		if (checkDomainKey) {
			checkDomainKey();
		}

		final Map<String, String> signatureData = new LinkedHashMap<>();
		signatureData.put("v", "1");
		signatureData.put("a", signingAlgorithm.getDkimNotation());

		// "q=dns/txt" is the default value, so don't add it
		// signatureData.put("q", "dns/txt");

		signatureData.put("c", getHeaderCanonicalization().getType() + "/" + getBodyCanonicalization().getType());
		signatureData.put("d", signingDomain);
		signatureData.put("s", selector);
		signatureData.put("t", Long.toString(getSentDate(message).getTime() / 1000l));

		if (null != identity) {
			signatureData.put("i", quotedPrintable(identity));
		}

		final List<String> headerNames = new ArrayList<>();
		final StringBuilder headerValues = new StringBuilder();
		final Set<String> mandatoryHeaders = compileMandatoryHeaders();

		for (final Header header : compileHeadersToSign(message)) {
			final String headerName = header.getName();
			final String headerValue = header.getValue();
			headerNames.add(headerName);
			headerValues.append(headerCanonicalization.canonicalizeHeader(headerName, headerValue));
			headerValues.append("\r\n");
			mandatoryHeaders.remove(headerName);
		}

		if (!mandatoryHeaders.isEmpty()) {
			throw new DkimSigningException("Could not find mandatory headers: " + join(mandatoryHeaders, ", "));
		}

		signatureData.put("h", serializeHeaderNames(headerNames, 3));

		final String canonicalBody = canonicalizeBody(message);
		signatureData.put("bh", base64Encode(messageDigest.digest(canonicalBody.getBytes(StandardCharsets.UTF_8))));

		// "b=" must be included in serialized signature data
		final String serializedSignature = serializeSignature(signatureData, DKIM_SIGNATUR_HEADER.length() + 2);
		headerValues.append(headerCanonicalization.canonicalizeHeader(DKIM_SIGNATUR_HEADER, serializedSignature));
		final byte[] signatureBytes = createSignature(headerValues.toString().getBytes(StandardCharsets.UTF_8));

		return DKIM_SIGNATUR_HEADER + ": " + serializedSignature + serializeBase64String(base64Encode(signatureBytes), 3);
	}

	private static String serializeHeaderNames(final List<String> headerNames, final int offset) {
		final StringBuilder headerNamesSerialized = new StringBuilder();
		int currentLinePosition = offset;
		for (int i = 0; i < headerNames.size(); i++) {
			final String headerName = headerNames.get(i);
			final boolean isLastHeaderName = ((i + 1) >= headerNames.size());
			if (headerNamesSerialized.length() == 0) {
				// first header without leading separator
				headerNamesSerialized.append(headerName);
				currentLinePosition += headerName.length();
			} else if (currentLinePosition + 1 + headerName.length() + (isLastHeaderName ? 0 : 1) > MAX_HEADER_LENGTH) {
				// header content would exceed limit, so linebreak is added
				headerNamesSerialized.append(":");
				headerNamesSerialized.append("\r\n\t ");
				headerNamesSerialized.append(headerName);
				currentLinePosition = 2 + headerName.length();
			} else {
				// simply adding separator and headername
				headerNamesSerialized.append(":");
				headerNamesSerialized.append(headerName);
				currentLinePosition += 1 + headerName.length();
			}
		}
		return headerNamesSerialized.toString();
	}

	private void checkDomainKey() throws DkimSigningException {
		try {
			DomainKeyUtil.getDomainKey(signingDomain, selector).check(identity, privateKey);
		} catch (final DkimException e) {
			throw new DkimSigningException("Failed to obtain the domain key for " + signingDomain + "." + selector, e);
		}
	}

	private static Date getSentDate(final DkimMessage message) throws MessagingException {
		Date sentDate = message.getSentDate();
		if (null == sentDate) {
			sentDate = new Date();
		}
		return sentDate;
	}

	private static Set<String> compileMandatoryHeaders() {
		final Set<String> mandatoryHeaders = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
		mandatoryHeaders.addAll(MANDATORY_HEADERS_TO_SIGN);
		return mandatoryHeaders;
	}

	private List<Header> compileHeadersToSign(final DkimMessage message) throws DkimSigningException {
		final List<Header> reverseOrderHeaderLines = new LinkedList<>();
		for (final Header header : getMessageHeaders(message)) {
			if (headersToSign.contains(header.getName()) && !DKIM_SIGNATUR_HEADER.equalsIgnoreCase(header.getName())) {
				reverseOrderHeaderLines.add(0, header);
			}
		}
		return reverseOrderHeaderLines;
	}

	private static Iterable<Header> getMessageHeaders(final DkimMessage message) throws DkimSigningException {
		try {
			return headerIterable(message.getAllHeaders());
		} catch (final MessagingException e) {
			throw new DkimSigningException("Could not retrieve the header fields for signing", e);
		}
	}

	private static Iterable<Header> headerIterable(final Enumeration<Header> headers) {
		return new Iterable<>() {
			@Override
			public Iterator<Header> iterator() {
				return headerIterator(headers);
			}
		};
	}

	private static Iterator<Header> headerIterator(final Enumeration<Header> headers) {
		return new Iterator<>() {
			@Override
			public boolean hasNext() {
				return headers.hasMoreElements();
			}

			@Override
			public Header next() {
				return headers.nextElement();
			}
		};
	}

	private String canonicalizeBody(final DkimMessage message) throws DkimSigningException {
		try {
			final byte[] bodyBytes = message.getEncodedBody().getBytes(StandardCharsets.UTF_8);
			final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			IOUtils.copy(new ByteArrayInputStream(bodyBytes), new CRLFOutputStream(buffer));
			return bodyCanonicalization.canonicalizeBody(buffer.toString());
		} catch (final Exception e) {
			throw new DkimSigningException("Failed to canonicalize the line terminators of the message body", e);
		}
	}

	private static String serializeSignature(final Map<String, String> signatureData, final int offset) {
		int position = 0;
		final StringBuilder builder = new StringBuilder();

		for (final Entry<String, String> entry : signatureData.entrySet()) {
			final StringBuilder entryBuilder = new StringBuilder();
			entryBuilder.append(entry.getKey()).append("=").append(entry.getValue()).append(";");

			if (position + entryBuilder.length() + 1 + offset > MAX_HEADER_LENGTH) {
				position = entryBuilder.length();
				builder.append("\r\n\t").append(entryBuilder);
			} else {
				builder.append(" ").append(entryBuilder);
				position += 1 + entryBuilder.length();
			}
		}

		builder.append("\r\n\t" + "b=");

		return builder.toString().trim();
	}

	private byte[] createSignature(final byte[] bytes) throws DkimSigningException {
		try {
			signature.update(bytes);
			return signature.sign();
		} catch (final SignatureException e) {
			throw new DkimSigningException("Faild to create signature", e);
		}
	}

	private static String serializeBase64String(final String base64String, int offset) {
		int base64StringReadIndex = 0;
		final StringBuilder builder = new StringBuilder();

		while (true) {
			if (offset > 0 && (base64String.substring(base64StringReadIndex).length() + offset) > MAX_HEADER_LENGTH) {
				builder.append(base64String.substring(base64StringReadIndex, base64StringReadIndex + MAX_HEADER_LENGTH - offset));
				base64StringReadIndex += MAX_HEADER_LENGTH - offset;
				offset = 0;
			} else if (2 + base64String.substring(base64StringReadIndex).length() > MAX_HEADER_LENGTH) {
				builder.append("\r\n\t ").append(base64String.substring(base64StringReadIndex, base64StringReadIndex + MAX_HEADER_LENGTH - 2));
				base64StringReadIndex += MAX_HEADER_LENGTH -2;
			} else {
				builder.append("\r\n\t ").append(base64String.substring(base64StringReadIndex));
				break;
			}
		}

		return builder.toString();
	}

	private static String join(final Collection<String> values, final String separator) {
		final StringBuilder builder = new StringBuilder();
		for (final String value : values) {
			builder.append(value);
			builder.append(separator);
		}
		return builder.substring(0, builder.length() - separator.length());
	}

	private static String quotedPrintable(final String s) {
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			try (QPEncoderStream encodeStream = new QPEncoderStream(out)) {
				encodeStream.write(s.getBytes(StandardCharsets.UTF_8));
			}

			String encoded = out.toString();
			encoded = encoded.replaceAll(";", "=3B");
			encoded = encoded.replaceAll(" ", "=20");

			return encoded;
		} catch (@SuppressWarnings("unused") final IOException e) {
			return null;
		}
	}

	private static String base64Encode(final byte[] bytes) {
		String encoded = Base64.getEncoder().encodeToString(bytes);

		// remove unnecessary line feeds after 76 characters
		encoded = encoded.replace("\n", "");
		encoded = encoded.replace("\r", "");

		return encoded;
	}
}
