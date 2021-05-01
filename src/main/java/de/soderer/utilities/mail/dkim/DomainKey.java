package de.soderer.utilities.mail.dkim;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * A {@code DomainKey} holds the information about a domain key.
 */
public final class DomainKey {
	private static final String DKIM_VERSION = "DKIM1";
	private static final String EMAIL_SERVICE_TYPE = "email";

	private final long timestamp;
	private final Pattern granularity;
	private final DkimKeyPairType keyPairType;
	private final PublicKey publicKey;
	private final Set<String> serviceTypes;
	private final Map<Character, String> tags;

	/**
	 * Creates a new {@code DomainKey} from the given tags.
	 *
	 * @param tags
	 *            The tags to be used.
	 * @throws DkimException
	 *             If either the version, key type or service type given in the
	 *             tags is incompatible to this library ('DKIM1', 'RSA' or
	 *             'Ed25519' and 'email' respectively).
	 */
	public DomainKey(final Map<Character, String> tags) throws DkimException {
		timestamp = System.currentTimeMillis();
		this.tags = Collections.unmodifiableMap(tags);

		final String dkimVersionTagValue = getTagValue('v', DKIM_VERSION);
		if (!(DKIM_VERSION.equals(dkimVersionTagValue))) {
			throw new DkimException("Incompatible version v=" + getTagValue('v') + ".");
		}

		final String granularityTagValue = getTagValue('g', "*");
		granularity = getGranularityPattern(granularityTagValue);

		final String keyTypeTagValue = getTagValue('k', DkimKeyPairType.RSA.getDkimNotation());
		keyPairType = getPublicKeyType(keyTypeTagValue);
		if (null == keyPairType) {
			throw new DkimException("Incompatible key type k=" + getTagValue('k') + ".");
		}

		final String serviceTypesTagValue = getTagValue('s', "*");
		serviceTypes = getServiceTypes(serviceTypesTagValue);
		if (!(serviceTypes.contains("*") || serviceTypes.contains(EMAIL_SERVICE_TYPE))) {
			throw new DkimException("Incompatible service type s=" + getTagValue('s') + ".");
		}

		final String privateKeyTagValue = getTagValue('p');
		publicKey = getPublicKey(privateKeyTagValue);
		if (null == privateKeyTagValue) {
			throw new DkimException("Incompatible public key p=" + getTagValue('p') + ".");
		}
	}

	private static DkimKeyPairType getPublicKeyType(final String keyTypeTagValue) {
		for (final DkimKeyPairType keyPairTypeItem : DkimKeyPairType.values()) {
			if (keyPairTypeItem.getDkimNotation().equals(keyTypeTagValue)) {
				return keyPairTypeItem;
			}
		}
		return null;
	}

	private static Set<String> getServiceTypes(final String serviceTypesTagValue) {
		final Set<String> serviceTypesSet = new HashSet<>();
		final StringTokenizer tokenizer = new StringTokenizer(serviceTypesTagValue, ":", false);
		while (tokenizer.hasMoreElements()) {
			serviceTypesSet.add(tokenizer.nextToken().trim());
		}
		return serviceTypesSet;
	}

	private String getTagValue(final char tag) {
		return getTagValue(tag, null);
	}

	private String getTagValue(final char tag, final String fallback) {
		final String tagValue = tags.get(tag);
		return null == tagValue ? fallback : tagValue;
	}

	private PublicKey getPublicKey(final String publicKeyTagValue) throws DkimException {
		if (null != publicKeyTagValue) {
			switch (keyPairType) {
				case RSA:
					return getRsaPublicKey(publicKeyTagValue);
				default:
					throw new DkimException("Unknown public key type " + keyPairType + ".");
			}
		} else {
			throw new DkimException("Missing public key value.");
		}
	}

	private static RSAPublicKey getRsaPublicKey(final String publicKeyTagValue) {
		try {
			final KeyFactory keyFactory = KeyFactory.getInstance(DkimKeyPairType.RSA.getJavaNotation());
			final X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(Base64.getDecoder().decode(publicKeyTagValue));
			return (RSAPublicKey) keyFactory.generatePublic(publicKeySpec);
		} catch (final NoSuchAlgorithmException nsae) {
			throw new DkimException("RSA algorithm not found by JVM", nsae);
		} catch (final IllegalArgumentException e) {
			throw new DkimException("The public key " + publicKeyTagValue + " couldn't be read.", e);
		} catch (final InvalidKeySpecException e) {
			throw new DkimException("The public key " + publicKeyTagValue + " couldn't be decoded.", e);
		}
	}

	private static Pattern getGranularityPattern(final String granularityPattern) {
		final StringTokenizer tokenizer = new StringTokenizer(granularityPattern, "*", true);
		final StringBuffer pattern = new StringBuffer();
		while (tokenizer.hasMoreElements()) {
			final String token = tokenizer.nextToken();
			if ("*".equals(token)) {
				pattern.append(".*");
			} else {
				pattern.append(Pattern.quote(token));
			}
		}
		return Pattern.compile(pattern.toString());
	}

	/**
	 * Returns the construction time of this {@code DomainKey} as a timestamp.
	 *
	 * @return The construction time of this {@code DomainKey} as a timestamp.
	 */
	public long getTimestamp() {
		return timestamp;
	}

	/**
	 * Returns a {@link Pattern} that matches the granularity of this
	 * {@code DomainKey}, as described in the 'g' tag.
	 *
	 * @return A {@link Pattern} that matches the granularity of this
	 *         {@code DomainKey}.
	 */
	public Pattern getGranularity() {
		return granularity;
	}

	/**
	 * Returns the set of service types supported by this {@code DomainKey}, as
	 * described in the 's' tag.
	 *
	 * @return The set of service types supported by this {@code DomainKey}.
	 */
	public Set<String> getServiceTypes() {
		return serviceTypes;
	}

	/**
	 * Returns the type of public key of this {@code DomainKey}, as provided by
	 * the 'p' tag.
	 *
	 * @return The type of public key of this {@code DomainKey}.
	 */
	public DkimKeyPairType getPublicKeyType() {
		return keyPairType;
	}

	/**
	 * Returns the public key of this {@code DomainKey}, as provided by the 'p'
	 * tag.
	 *
	 * @return The public key of this {@code DomainKey}.
	 */
	public PublicKey getPublicKey() {
		return publicKey;
	}

	/**
	 * Returns the {@link Collections#unmodifiableMap(Map) unmodifiable} map of
	 * tags, this {@code DomainKey} was constructed from.
	 *
	 * @return The map of tags, this {@code DomainKey} was constructed from.
	 */
	public Map<Character, String> getTags() {
		return tags;
	}

	@Override
	public String toString() {
		return "DomainKey [timestamp=" + timestamp + ", tags=" + tags + "]";
	}

	/**
	 * Checks, whether this {@code DomainKey} fits to the given identity and
	 * {@link RSAPrivateKey}.
	 *
	 * @param identity
	 *            The identity.
	 * @param privateKey
	 *            The {@link RSAPrivateKey}.
	 * @throws DkimSigningException
	 *             If either the {@link DomainKey#getGranularity() granularity}
	 *             of this {@code DomainKey} doesn't match the given identity or
	 *             the {@link DomainKey#getPublicKey() public key} of this
	 *             {@code DomainKey} doesn't belong to the given
	 *             {@link RSAPrivateKey}.
	 */
	public void check(final String identity, final PrivateKey privateKey) throws DkimSigningException {
		checkIdentity(identity);
		checkKeyCompatiblilty(privateKey);
	}

	private void checkIdentity(final String identity) throws DkimAcceptanceException {
		if (null != identity && !identity.contains("@")) {
			throw new DkimAcceptanceException("Invalid identity: " + identity);
		}
		final String localPart = null == identity ? "" : identity.substring(0, identity.indexOf('@'));
		if (!granularity.matcher(localPart).matches()) {
			throw new DkimAcceptanceException("Incompatible identity for granularity " + getTagValue('g') + ": " + identity);
		}
	}

	private void checkKeyCompatiblilty(final PrivateKey privateKey) throws DkimSigningException {
		try {
			final DkimSigningAlgorithm signingAlgorithm = keyPairType.getDefaultSigningAlgorithm();

			final Signature signingSignature = Signature.getInstance(signingAlgorithm.getJavaNotation());
			signingSignature.initSign(privateKey);
			signingSignature.update("01189998819991197253".getBytes(StandardCharsets.UTF_8));
			final byte[] signatureBytes = signingSignature.sign();

			final Signature verifyingSignature = Signature.getInstance(signingAlgorithm.getJavaNotation());
			verifyingSignature.initVerify(publicKey);
			verifyingSignature.update("01189998819991197253".getBytes(StandardCharsets.UTF_8));

			if (!verifyingSignature.verify(signatureBytes)) {
				throw new DkimAcceptanceException("Incompatible private and public key.");
			}

		} catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
			throw new DkimSigningException("Performing cryptography failed.", e);
		}
	}
}