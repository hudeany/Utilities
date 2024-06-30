package de.soderer.utilities.http;

import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;
import java.util.List;

import de.soderer.utilities.NumberUtilities;
import de.soderer.utilities.Utilities;
import de.soderer.utilities.pac.PacScriptParser;

public class ProxyConfiguration {
	public enum ProxyConfigurationType {
		None,
		System,
		ProxyURL,
		WPAD,
		PACURL;

		public static ProxyConfigurationType getFromString(final String proxyConfigurationTypeString) {
			for (final ProxyConfigurationType dataType : ProxyConfigurationType.values()) {
				if (dataType.toString().equalsIgnoreCase(proxyConfigurationTypeString.replace("_", "").replace("-", ""))) {
					return dataType;
				}
			}
			return ProxyConfigurationType.None;
		}
	}

	private final ProxyConfigurationType proxyConfigurationType;
	private String proxyOrPacUrl;
	private boolean searchByWpad = true;

	public ProxyConfiguration(final ProxyConfigurationType proxyConfigurationType, final String proxyOrPacUrl) {
		if (proxyConfigurationType == null) {
			this.proxyConfigurationType = ProxyConfigurationType.None;
		} else {
			this.proxyConfigurationType = proxyConfigurationType;
		}

		this.proxyOrPacUrl = proxyOrPacUrl;
	}

	public ProxyConfigurationType getProxyConfigurationType() {
		return proxyConfigurationType;
	}

	public String getProxyOrPacUrl() {
		return proxyOrPacUrl;
	}

	public Proxy getProxy(final String url) {
		switch (proxyConfigurationType) {
			case None:
				return Proxy.NO_PROXY;
			case System:
				return getSystemProxy(url);
			case ProxyURL:
				if (proxyOrPacUrl == null || proxyOrPacUrl.trim().length() == 0) {
					return Proxy.NO_PROXY;
				} else {
					String proxyHost = proxyOrPacUrl;
					String proxyPort = "8080";
					if (proxyHost.contains(":")) {
						proxyPort = proxyHost.substring(proxyHost.indexOf(":") + 1);
						proxyHost = proxyHost.substring(0, proxyHost.indexOf(":"));
					}
					return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
				}
			case WPAD:
				if ((proxyOrPacUrl == null || proxyOrPacUrl.trim().length() == 0) && searchByWpad) {
					proxyOrPacUrl = PacScriptParser.findPacFileUrlByWpad();
					searchByWpad = false;
				}
				//$FALL-THROUGH$
			case PACURL:
				if (proxyOrPacUrl == null || proxyOrPacUrl.trim().length() == 0) {
					return Proxy.NO_PROXY;
				} else {
					URL pacUrl;
					try {
						pacUrl = new URL(proxyOrPacUrl);
					} catch (final MalformedURLException e) {
						throw new RuntimeException("Invalid PAC url: " + proxyOrPacUrl, e);
					}
					final List<Proxy> multipleAllowedProxySettingsForThisUrl = new PacScriptParser(pacUrl).discoverProxy(url);
					if (multipleAllowedProxySettingsForThisUrl == null || multipleAllowedProxySettingsForThisUrl.isEmpty()) {
						return Proxy.NO_PROXY;
					} else {
						final Proxy proxy = multipleAllowedProxySettingsForThisUrl.get(0);
						if (proxy == null) {
							return Proxy.NO_PROXY;
						} else {
							return proxy;
						}
					}
				}
			default:
				return Proxy.NO_PROXY;
		}
	}

	/**
	 * System proxy configuration is set via JVM properties on startup or via environment properties:<br />
	 * java ... -Dhttp.proxyHost=proxy.url.local -Dhttp.proxyPort=8080 -Dhttp.nonProxyHosts='127.0.0.1|localhost'
	 */
	private static Proxy getSystemProxy(final String url) {
		final String proxyHost = System.getProperty("http.proxyHost");
		if (Utilities.isBlank(proxyHost)) {
			return Proxy.NO_PROXY;
		} else {
			final String proxyPort = System.getProperty("http.proxyPort");
			final String nonProxyHosts = System.getProperty("http.nonProxyHosts");

			if (Utilities.isBlank(nonProxyHosts)) {
				if (Utilities.isNotBlank(proxyHost)) {
					if (Utilities.isNotBlank(proxyPort) && NumberUtilities.isNumber(proxyPort)) {
						return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
					} else {
						return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, 8080));
					}
				} else {
					return Proxy.NO_PROXY;
				}
			} else {
				boolean ignoreProxy = false;
				final String urlDomain = getDomainFromUrl(url);
				for (String nonProxyHost : nonProxyHosts.split("\\|")) {
					nonProxyHost = nonProxyHost.trim();
					if (urlDomain == null || urlDomain.equalsIgnoreCase(url)) {
						ignoreProxy = true;
						break;
					}
				}
				if (!ignoreProxy) {
					if (Utilities.isNotBlank(proxyHost)) {
						if (Utilities.isNotBlank(proxyPort) && NumberUtilities.isNumber(proxyPort)) {
							return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
						} else {
							return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, 8080));
						}
					} else {
						return Proxy.NO_PROXY;
					}
				} else {
					return Proxy.NO_PROXY;
				}
			}
		}
	}

	private static String getDomainFromUrl(String url) {
		if (!url.startsWith("http") && !url.startsWith("https")) {
			url = "http://" + url;
		}
		URL netUrl;
		try {
			netUrl = new URL(url);
		} catch (@SuppressWarnings("unused") final MalformedURLException e) {
			return null;
		}
		return netUrl.getHost();
	}
}
