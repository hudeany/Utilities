package de.soderer.utilities.mail.dkim;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

public final class DomainKeyUtil {
	private static final Map<String, DomainKey> DomainKey_CACHE = new HashMap<>();
	private static final Pattern RECORD_PATTERN = Pattern.compile("(?:\"(.*?)\"(?: |$))|(?:'(.*?)'(?: |$))|(?:(.*?)(?: |$))");
	private static final long DEFAULT_CACHE_TTL = 2 * 60 * 60 * 1000;
	private static long cacheTtl = DEFAULT_CACHE_TTL;

	/**
	 * Returns the configured TTL (time to live) for retrieved {@link DomainKey}
	 * s.
	 *
	 * @return The configured TTL for retrieved {@link DomainKey}s.
	 */
	public static synchronized long getCacheTtl() {
		return cacheTtl;
	}

	/**
	 * Sets the TTL (time to live) for retrieved {@link DomainKey}s.
	 *
	 * @param cacheTtl
	 *            The TTL for retrieved {@link DomainKey}s.
	 */
	public static synchronized void setCacheTtl(long cacheTtl) {
		if (cacheTtl < 0) {
			cacheTtl = DEFAULT_CACHE_TTL;
		}
		DomainKeyUtil.cacheTtl = cacheTtl;
	}

	/**
	 * Retrieves the {@link DomainKey} for the given signing domain and
	 * selector.
	 *
	 * @param signingDomain
	 *            The signing domain.
	 * @param selector
	 *            The selector.
	 * @return The retrieved {@link DomainKey}.
	 * @throws DkimException
	 *             If the domain key couldn't be retrieved or if either the
	 *             version, key type or service type given in the tags of the
	 *             retrieved domain key is incompatible to this library
	 *             ('DKIM1', 'RSA' and 'email' respectively).
	 */
	public static synchronized DomainKey getDomainKey(final String signingDomain, final String selector) throws DkimException {
		return getDomainKey(getRecordName(signingDomain, selector));
	}

	private static synchronized DomainKey getDomainKey(final String recordName) throws DkimException {
		DomainKey domainKey = DomainKey_CACHE.get(recordName);
		if (null != domainKey && 0 != cacheTtl && isRecent(domainKey)) {
			return domainKey;
		} else {
			domainKey = new DomainKey(getTags(recordName));
			DomainKey_CACHE.put(recordName, domainKey);
			return domainKey;
		}
	}

	private static boolean isRecent(final DomainKey domainKey) {
		return domainKey.getTimestamp() + cacheTtl > System.currentTimeMillis();
	}

	/**
	 * Retrieves the tags of a domain key for the given signing domain and
	 * selector.
	 *
	 * @param signingDomain
	 *            The signing domain.
	 * @param selector
	 *            The selector.
	 * @return The retrieved tags.
	 * @throws DkimException
	 *             If the domain key couldn't be retrieved.
	 */
	public static Map<Character, String> getTags(final String signingDomain, final String selector) throws DkimException {
		return getTags(getRecordName(signingDomain, selector));
	}

	private static Map<Character, String> getTags(final String recordName) throws DkimException {
		final Map<Character, String> tags = new HashMap<>();

		final String recordValue = getValue(recordName);

		for (String tag : recordValue.split(";")) {
			try {
				tag = tag.trim();
				tags.put(tag.charAt(0), tag.substring(2));
			} catch (final IndexOutOfBoundsException e) {
				throw new DkimException("The tag " + tag + " in RR " + recordName + " couldn't be decoded.", e);
			}
		}
		return tags;
	}

	/**
	 * Retrieves the raw domain key for the given signing domain and selector.
	 *
	 * @param signingDomain
	 *            The signing domain.
	 * @param selector
	 *            The selector.
	 * @return The raw domain key.
	 * @throws DkimException
	 *             If the domain key couldn't be retrieved.
	 */
	public static String getValue(final String signingDomain, final String selector) throws DkimException {
		return getValue(getRecordName(signingDomain, selector));
	}

	private static String getValue(final String recordName) throws DkimException {
		try {
			final DirContext dnsContext = new InitialDirContext(getEnvironment());
			final Attributes attributes = dnsContext.getAttributes(recordName, new String[] { "TXT" });
			final Attribute txtRecord = attributes.get("txt");

			if (txtRecord == null) {
				throw new DkimException("There is no TXT record available for " + recordName);
			}

			final StringBuilder builder = new StringBuilder();
			final NamingEnumeration<?> e = txtRecord.getAll();
			while (e.hasMore()) {
				builder.append((String) e.next());
			}

			final String value = builder.toString();
			if (value.isEmpty()) {
				throw new DkimException("Value of RR " + recordName + " couldn't be retrieved");
			}

			return unquoteRecordValue(value);

		} catch (final NamingException ne) {
			throw new DkimException("Selector lookup failed", ne);
		}
	}

	/*
	 * Unquote a recordValue string.
	 *
	 * The Java DNS provider does something very odd. In the instance there are
	 * multiple entries for the TXT record, the first is quoted however the
	 * second is unquoted. That makes removing quotes difficult. In the normal
	 * case, there should be a "\" \"" string, however, as confirmed in actual
	 * records, the last item may not be quoted. This seems to happen if there
	 * are no spaces.
	 *
	 * @param recordValue Domain record value.
	 *
	 * @return Domain record value unquoted.
	 */
	private static String unquoteRecordValue(final String recordValue) {
		final Matcher recordMatcher = RECORD_PATTERN.matcher(recordValue);

		final StringBuilder builder = new StringBuilder();
		while (recordMatcher.find()) {
			for (int i = 1; i <= recordMatcher.groupCount(); i++) {
				final String match = recordMatcher.group(i);
				if (null != match) {
					builder.append(match);
				}
			}
		}

		final String unquotedRecordValue = builder.toString();
		if (null == unquotedRecordValue || 0 == unquotedRecordValue.length()) {
			throw new DkimException("Unable to parse DKIM record: " + recordValue);
		}

		return unquotedRecordValue;
	}

	private static String getRecordName(final String signingDomain, final String selector) {
		return selector + "._domainkey." + signingDomain;
	}

	private static Hashtable<String, String> getEnvironment() {
		final Hashtable<String, String> environment = new Hashtable<>();
		environment.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
		return environment;
	}
}
