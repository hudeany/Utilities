package de.soderer.utilities.pac;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import javax.net.ssl.HttpsURLConnection;

import de.soderer.utilities.pac.utilities.Method;
import de.soderer.utilities.pac.utilities.PacScriptParserUtilities;

// TODO:
//const bypassList = [
//	'*.*.aadrm.com',
//	'*.aadrm.com'
//]
//
//array.length
//
//while (condition) {
//  // code block to be executed
//}

public class PacScriptParser {
	private String pacScriptData = null;
	private Map<String, Method> pacScriptMethods = null;

	private final Map<String, List<String>> pacProxyCache = new HashMap<>();

	/**
	 * Default: CacheByDomain
	 */
	public enum CacheType {
		None,
		CacheByDomain,
		CacheByFullUrl
	}

	public PacScriptParser(final URL pacUrl) {
		pacScriptData = PacScriptParserUtilities.readPacData(pacUrl);
	}

	public PacScriptParser(final String pacScriptData) throws Exception {
		if (pacScriptData.trim().toLowerCase().startsWith("http")) {
			this.pacScriptData = PacScriptParserUtilities.readPacData(new URL(pacScriptData.trim()));
		} else {
			this.pacScriptData = pacScriptData;
		}
	}

	public Map<String, Method> parsePacScript() {
		final Map<String, Method> methodDefinitions = new HashMap<>();

		List<String> pacScriptTokens = PacScriptParserUtilities.tokenize(PacScriptParserUtilities.removeComments(pacScriptData));

		pacScriptTokens = PacScriptParserUtilities.replaceAliases(pacScriptTokens);

		int tokenIndex = 0;
		String nextToken = pacScriptTokens.get(tokenIndex);
		while ("function".equals(nextToken)) {
			tokenIndex++;
			final String methodName = pacScriptTokens.get(tokenIndex);
			tokenIndex++;
			nextToken = pacScriptTokens.get(tokenIndex);
			if (!"(".equals(nextToken)) {
				throw new RuntimeException("Unexpected code token: " + nextToken);
			}
			tokenIndex++;
			nextToken = pacScriptTokens.get(tokenIndex);
			final List<String> methodParameterNames = new ArrayList<>();
			while (!")".equals(nextToken)) {
				if (methodParameterNames.size() > 0) {
					if (!",".equals(nextToken)) {
						throw new RuntimeException("Unexpected code token: " + nextToken);
					} else {
						tokenIndex++;
						nextToken = pacScriptTokens.get(tokenIndex);
					}
				}
				methodParameterNames.add(nextToken);
				tokenIndex++;
				nextToken = pacScriptTokens.get(tokenIndex);
			}
			tokenIndex++;
			nextToken = pacScriptTokens.get(tokenIndex);
			if (!"{".equals(nextToken)) {
				throw new RuntimeException("Unexpected code token: " + nextToken);
			}
			final int methodBlockStart = tokenIndex;
			final int methodBlockEnd = PacScriptParserUtilities.findClosingBracketToken(pacScriptTokens, tokenIndex);

			methodDefinitions.put(methodName, new Method(methodName, methodParameterNames, pacScriptTokens.subList(methodBlockStart + 1, methodBlockEnd)));
			tokenIndex = methodBlockEnd;

			tokenIndex++;
			if (pacScriptTokens.size() > tokenIndex) {
				nextToken = pacScriptTokens.get(tokenIndex);
			}
		}

		if (pacScriptTokens.size() != tokenIndex) {
			throw new RuntimeException("Invalid PAC data found: " + nextToken);
		}

		return methodDefinitions;
	}

	public static String findPacFileUrlByWpad() {
		try {
			final String fqdnAddress = InetAddress.getLocalHost().getCanonicalHostName();
			String fullDomain;
			if (fqdnAddress.contains(".")) {
				fullDomain = fqdnAddress.substring(fqdnAddress.indexOf(".") + 1);
			} else {
				return null;
			}

			final String[] domainParts = fullDomain.split("\\.");
			final List<String> pacUrlCandidates = new ArrayList<>();
			pacUrlCandidates.add("https://proxypac." + fullDomain + "/proxy.pac");

			// Exclude TLD domain like 'com' from pacUrlCandidates
			for (int i = 0; i < domainParts.length - 1; i++) {
				final String subDomain = PacScriptParserUtilities.join(Arrays.copyOfRange(domainParts, i, domainParts.length), ".");
				pacUrlCandidates.add("http://wpad." + subDomain + "/wpad.dat");
			}

			for (final String pacUrlCandidate : pacUrlCandidates) {
				try {
					final HttpsURLConnection pacConnection = (HttpsURLConnection) new URL(pacUrlCandidate).openConnection();
					pacConnection.connect();
				} catch (@SuppressWarnings("unused") final Exception e) {
					continue;
				}
				return pacUrlCandidate;
			}
			return null;
		} catch (@SuppressWarnings("unused") final UnknownHostException e) {
			return null;
		}
	}

	public List<String> discoverProxySettings(final String destinationUrl) throws Exception {
		return discoverProxySettings(destinationUrl, null);
	}

	public List<String> discoverProxySettings(final String destinationUrl, final CacheType cacheType) throws Exception {
		if (cacheType == CacheType.None) {
			return discoverProxySettingsInternal(destinationUrl);
		} else if (cacheType == CacheType.CacheByFullUrl) {
			if (pacProxyCache.containsKey(destinationUrl)) {
				return pacProxyCache.get(destinationUrl);
			} else {
				final List<String> result = discoverProxySettingsInternal(destinationUrl);
				pacProxyCache.put(destinationUrl, result);
				return result;
			}
		} else {
			final String domain = getDomainFromUrl(destinationUrl);
			if (pacProxyCache.containsKey(domain)) {
				return pacProxyCache.get(domain);
			} else {
				final List<String> result = discoverProxySettingsInternal(destinationUrl);
				pacProxyCache.put(domain, result);
				return result;
			}
		}
	}

	public List<Proxy> discoverProxy(final String destinationUrl) throws Exception {
		return discoverProxy(destinationUrl, null);
	}

	public List<Proxy> discoverProxy(final String destinationUrl, final CacheType cacheType) throws Exception {
		return discoverProxyInternal(destinationUrl, cacheType);
	}

	private List<String> discoverProxySettingsInternal(final String destinationUrl) {
		final String hostname = PacScriptParserUtilities.getHostnameFromRequestString(destinationUrl);
		if (pacScriptMethods == null) {
			pacScriptMethods = parsePacScript();
		}
		final Map<String, Object> environmentVariables = new HashMap<>();
		final List<Object> methodParameters = new ArrayList<>();
		methodParameters.add(destinationUrl);
		methodParameters.add(hostname);

		final Object pacScriptMethodReturnValue = pacScriptMethods.get("FindProxyForURL").executeMethod(methodParameters, environmentVariables, pacScriptMethods);
		if (pacScriptMethodReturnValue == null) {
			return null;
		} else if (pacScriptMethodReturnValue instanceof String) {
			return Arrays.stream(((String) pacScriptMethodReturnValue).split(";")).map(x -> x.trim()).collect(Collectors.toList());
		} else {
			return null;
		}
	}

	private List<Proxy> discoverProxyInternal(final String destinationUrl, final CacheType cacheType) throws Exception {
		final List<String> proxySettings = discoverProxySettings(destinationUrl, cacheType);
		final List<Proxy> proxyConfigurations = new ArrayList<>();
		if (proxySettings != null) {
			for (String proxyConfigurationString : proxySettings) {
				if ("DIRECT".equals(proxyConfigurationString)) {
					proxyConfigurations.add(null);
				} else if (proxyConfigurationString.startsWith("PROXY ")) {
					proxyConfigurationString = proxyConfigurationString.substring(6);
					String proxyHost;
					int proxyPort;
					if (proxyConfigurationString.contains(":")) {
						proxyHost = proxyConfigurationString.substring(0, proxyConfigurationString.indexOf(":"));
						try {
							proxyPort = Integer.parseInt(proxyConfigurationString.substring(proxyConfigurationString.indexOf(":") + 1));
						} catch (@SuppressWarnings("unused") final NumberFormatException e) {
							throw new RuntimeException("Invalid port number for proxy url '" + proxyHost + "': " + proxyConfigurationString.indexOf(":" + 1));
						}
					} else {
						proxyHost = proxyConfigurationString;
						proxyPort = 80;
					}
					proxyConfigurations.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
				} else {
					throw new RuntimeException("Unsupported proxy configuration type: " + proxyConfigurationString);
				}
			}
		} else {
			proxyConfigurations.add(null);
		}
		return proxyConfigurations;
	}

	public static String getDomainFromUrl(final String url) throws Exception {
		final URI uri = new URI(url);
		final String domain = uri.getHost();
		return domain.startsWith("www.") ? domain.substring(4) : domain;
	}

	@Override
	public String toString() {
		if (pacScriptMethods == null) {
			pacScriptMethods = parsePacScript();
		}
		String returnValue = "";
		if (pacScriptMethods.containsKey("FindProxyForURL")) {
			returnValue += pacScriptMethods.get("FindProxyForURL").toString() + "\n";
		}
		for (final Entry<String, Method> method : pacScriptMethods.entrySet()) {
			if (!("FindProxyForURL").equals(method.getKey())) {
				returnValue += method.getValue().toString() + "\n";
			}
		}
		return returnValue;
	}
}
