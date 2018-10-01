package de.soderer.utilities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import de.soderer.utilities.collection.CaseInsensitiveMap;

public class HttpUtilities {
	public static void pingUrlWithoutSslCheckNoWaitForAnswer(String pingUrl) throws IOException, NoSuchAlgorithmException, KeyManagementException {
		InputStream downloadStream = null;
		try {
			if (pingUrl.startsWith("https")) {
				// Deactivate SSL-Certificates check
				SSLContext sslContext = SSLContext.getInstance("TLS");
				TrustManager[] tms = new TrustManager[] { new X509TrustManager() {
					@Override
					public X509Certificate[] getAcceptedIssuers() {
						return null;
					}

					@Override
					public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
						// nothing to do
					}

					@Override
					public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
						// nothing to do
					}
				} };
				sslContext.init(null, tms, null);
				SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
				HttpsURLConnection connection = (HttpsURLConnection) new URL(pingUrl).openConnection();
				connection.setSSLSocketFactory(sslSocketFactory);
				connection.setRequestMethod("POST");
				connection.setConnectTimeout(5000);
				connection.setReadTimeout(100);
				connection.setDoInput(true);
				connection.setDoOutput(false);
				downloadStream = connection.getInputStream();
			} else {
				HttpURLConnection connection = (HttpURLConnection) new URL(pingUrl).openConnection();
				connection.setRequestMethod("POST");
				connection.setConnectTimeout(5000);
				connection.setReadTimeout(100);
				connection.setDoInput(true);
				connection.setDoOutput(false);
				downloadStream = connection.getInputStream();
			}
		} catch (SocketTimeoutException stex) {
			// This Exception is expected for the real short timeout
		} finally {
			if (downloadStream != null) {
				Utilities.closeQuietly(downloadStream);
			}
		}
	}
	
	public enum RequestMethod {
		GET,
		POST,
		PUT,
		DELETE
	}

	public static final String CONTENT_TYPE_JAVASCRIPT = "application/javascript";
	public static final String SECURE_HTTP_PROTOCOL_SIGN = "https://";
	public static final String HTTP_PROTOCOL_SIGN = "http://";

	public static final String IMAGE_CONTENT_TYPE = "image";
	public static final String IMAGE_PATH_NO_PREVIEW = "/assets/core/images/facelift/no_preview.png";

	public static final String CONTENT_TYPE_JSON = "application/json";
	public static final String CONTENT_ENCODING = "UTF-8";
	public static final String APPLICATION_JSON_UTF8 = CONTENT_TYPE_JSON + ";charset=" + CONTENT_ENCODING;

	private static TrustManager TRUSTALLCERTS_TRUSTMANAGER = new X509TrustManager() {
		@Override
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		@Override
		public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			// nothing to do
		}

		@Override
		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			// nothing to do
		}
	};
	
	private static HostnameVerifier TRUSTALLHOSTNAMES_HOSTNAMEVERIFIER = (hostname, session) -> true;

	public static String convertToParameterString(Map<String, Object> parameterMap, String encoding) throws UnsupportedEncodingException {
		if (parameterMap == null) {
			return null;
		} else {
			if (Utilities.isBlank(encoding)) {
				encoding = "UTF-8";
			}
			StringBuilder returnValue = new StringBuilder();
			try {
				for (Entry<String, Object> entry : parameterMap.entrySet()) {
					if (returnValue.length() > 0) {
						returnValue.append("&");
					}
					returnValue.append(URLEncoder.encode(entry.getKey(), encoding));
					returnValue.append("=");
					if (entry.getValue() != null) {
						returnValue.append(URLEncoder.encode(entry.getValue().toString(), encoding));
					}
				}
			} catch (UnsupportedEncodingException e) {
				throw e;
			}
			
			return returnValue.toString();
		}
	}
	
	public static Tuple<String, String> createHtmlFormMimetypeHeader(String encoding) {
		if (Utilities.isBlank(encoding)) {
			return new Tuple<>("content-type", "application/x-www-form-urlencoded");
		} else {
			return new Tuple<>("content-type", "application/x-www-form-urlencoded; charset=" + encoding.toLowerCase());
		}
	}
	
	public static String executeHttpGetRequest(String httpUrl) throws Exception {
		return executeHttpRequest(httpUrl, null, null);
	}
	
	public static String executeHttpGetRequest(String httpUrl, Map<String, Object> httpUrlParameter) throws Exception {
		return executeHttpRequest(httpUrl, httpUrlParameter, null);
	}
	
	public static String executeHttpGetRequest(String httpUrl, Map<String, Object> httpUrlParameter, boolean checkSslServerCert) throws Exception {
		return executeHttpRequest(httpUrl, httpUrlParameter, null, checkSslServerCert);
	}
	
	public static String executeHttpPostRequest(String httpUrl, Map<String, Object> httpPostParameter) throws Exception {
		return executeHttpRequest(httpUrl, null, httpPostParameter);
	}
	
	public static String executeHttpPostRequest(String httpUrl, Map<String, Object> httpPostParameter, boolean checkSslServerCert) throws Exception {
		return executeHttpRequest(httpUrl, null, httpPostParameter, checkSslServerCert);
	}
	
	public static String executeHttpRequest(String httpUrlString, Map<String, Object> httpUrlParameter, Map<String, Object> httpPostParameter) throws Exception {
		return executeHttpRequest(httpUrlString, httpUrlParameter, httpPostParameter, true);
	}
	
	public static String executeHttpRequest(String httpUrlString, Map<String, Object> httpUrlParameter, Map<String, Object> httpPostParameter, boolean checkSslServerCert) throws Exception {
		return executeHttpRequest(httpUrlString, null, httpUrlParameter, httpPostParameter, checkSslServerCert);
	}
	
	public static String executeHttpRequest(String httpUrlString, Map<String, String> httpRequestHeaders, Map<String, Object> httpUrlParameter, Map<String, Object> httpPostParameter, boolean checkSslServerCert) throws Exception {
		String httpRequestBody = null;
		if (httpPostParameter != null && httpPostParameter.size() > 0) {
			httpRequestBody = convertToParameterString(httpPostParameter, null);
		}
		
		HttpResponse response = executeHttpRequest(null, httpUrlString, httpRequestHeaders, httpUrlParameter, httpRequestBody, checkSslServerCert);
		
		if (response.getHttpCode() == HttpURLConnection.HTTP_OK) {
			return response.getContent();
		} else {
			throw new HttpException(httpUrlString, response.getHttpCode());
		}
	}
	
	public static HttpResponse executeHttpRequest(RequestMethod requestMethod, String httpUrlString, Map<String, String> httpRequestHeaders, Map<String, Object> httpUrlParameter, String httpRequestBody, boolean checkSslServerCert) throws Exception {
		return executeHttpRequest(requestMethod, httpUrlString, httpRequestHeaders, httpUrlParameter, httpRequestBody, null, checkSslServerCert);
	}
	
	public static HttpResponse executeHttpRequest(RequestMethod requestMethod, String urlString, Map<String, String> httpRequestHeaders, Map<String, Object> httpUrlParameter, String httpRequestBody, String requestEncoding, boolean checkSslServerCert) throws Exception {
		return executeHttpRequest(requestMethod, urlString, httpRequestHeaders,httpUrlParameter, httpRequestBody, requestEncoding, checkSslServerCert, null);
	}
	 
	public static HttpResponse executeHttpRequest(RequestMethod requestMethod, String urlString, Map<String, String> httpRequestHeaders, Map<String, Object> httpUrlParameter, String httpRequestBody, String requestEncoding, boolean checkSslServerCert, Proxy proxy) throws Exception {
		if (Utilities.isBlank(urlString)) {
			throw new RuntimeException("Invalid empty URL for http request");
		}
		
		// Check for protocol "https://" or "http://" (fallback: "http://")
		if (!urlString.startsWith(SECURE_HTTP_PROTOCOL_SIGN) && !urlString.startsWith(HTTP_PROTOCOL_SIGN)) {
			urlString = HTTP_PROTOCOL_SIGN + urlString;
		}
			
		try {
			if (httpUrlParameter != null && httpUrlParameter.size() > 0) {
				// Prepare Get parameter data
				String getParameterString = convertToParameterString(httpUrlParameter, requestEncoding);
				urlString += "?" + getParameterString;
			}

			HttpURLConnection urlConnection;
			if (proxy == null) {
				// Use systems default proxy, if set on JVM start. To override default proxy usage use "Proxy.NO_PROXY"
				urlConnection = (HttpURLConnection) new URL(urlString).openConnection(getProxyFromSystem(urlString));
			} else {
				urlConnection = (HttpURLConnection) new URL(urlString).openConnection(proxy);
			}
			
			if (requestMethod != null) {
				urlConnection.setRequestMethod(requestMethod.name());
			}
			
			if (urlString.startsWith(SECURE_HTTP_PROTOCOL_SIGN) && !checkSslServerCert) {
				SSLContext sslContext = SSLContext.getInstance("SSL");
				sslContext.init(null, new TrustManager[] { TRUSTALLCERTS_TRUSTMANAGER }, new java.security.SecureRandom());
				SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
				((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslSocketFactory);
				((HttpsURLConnection) urlConnection).setHostnameVerifier(TRUSTALLHOSTNAMES_HOSTNAMEVERIFIER);
			}
			
			if (httpRequestHeaders != null) {
				for (Entry<String, String> headerEntry : httpRequestHeaders.entrySet()) {
					urlConnection.setRequestProperty(headerEntry.getKey(), headerEntry.getValue());
				}
			}
			
			if (httpRequestBody != null) {
				// Send post parameter data
				urlConnection.setDoOutput(true);
				if (Utilities.isBlank(requestEncoding)) {
					requestEncoding = "UTF-8";
				}
			    try (OutputStreamWriter out = new OutputStreamWriter(urlConnection.getOutputStream(), requestEncoding)) {
				    out.write(httpRequestBody);
				    out.flush();
			    }
			}
			
			urlConnection.connect();
			
			int httpResponseCode = urlConnection.getResponseCode();
			if (httpResponseCode < HttpURLConnection.HTTP_BAD_REQUEST) {
				Map<String, String> headers = new CaseInsensitiveMap<>();
				for (String headerName : urlConnection.getHeaderFields().keySet()) {
					headers.put(headerName, urlConnection.getHeaderField(headerName));
				}
				
				String encoding = "UTF-8";
				if (headers.containsKey("content-type")) {
					String contentType = headers.get("content-type");
					if (contentType != null && contentType.toLowerCase().contains("charset="))  {
						contentType = contentType.toLowerCase();
						encoding = contentType.substring(contentType.indexOf("charset=") + 8).trim();
					}
				}
				
				try (BufferedReader httpResponseContentReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), encoding))) {
					StringBuilder httpResponseContent = new StringBuilder();
					String httpResponseContentLine;
					while ((httpResponseContentLine = httpResponseContentReader.readLine()) != null) {
						httpResponseContent.append(httpResponseContentLine);
						httpResponseContent.append("\n");
					}
					return new HttpResponse(httpResponseCode, httpResponseContent.toString(), urlConnection.getContentType(), headers);
				} catch (Exception e) {
					return new HttpResponse(httpResponseCode, null, null, null);
				}
			} else {
				Map<String, String> headers = new CaseInsensitiveMap<>();
				for (String headerName : urlConnection.getHeaderFields().keySet()) {
					headers.put(headerName, urlConnection.getHeaderField(headerName));
				}
				
				String encoding = "UTF-8";
				if (headers.containsKey("content-type")) {
					String contentType = headers.get("content-type");
					if (contentType != null && contentType.toLowerCase().contains("charset="))  {
						contentType = contentType.toLowerCase();
						encoding = contentType.substring(contentType.indexOf("charset=") + 8).trim();
					}
				}
				
				try (BufferedReader httpResponseContentReader = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream(), encoding))) {
					StringBuilder httpResponseContent = new StringBuilder();
					String httpResponseContentLine;
					while ((httpResponseContentLine = httpResponseContentReader.readLine()) != null) {
						httpResponseContent.append(httpResponseContentLine);
						httpResponseContent.append("\n");
					}
					return new HttpResponse(httpResponseCode, httpResponseContent.toString(), urlConnection.getContentType(), headers);
				} catch (Exception e) {
					return new HttpResponse(httpResponseCode, null, null, null);
				}
			}
		} catch (Exception e) {
			throw e;
		}
	}
	
	/**
	 * This proxy will be used as default proxy.
	 * To override default proxy usage use "Proxy.NO_PROXY"
	 * 
	 * It is set via JVM properties on startup:
	 * java ... -Dhttp.proxyHost=proxy.url.local -Dhttp.proxyPort=8080 -Dhttp.nonProxyHosts='127.0.0.1|localhost'
	 */
	public static Proxy getProxyFromSystem(String url) {
		String proxyHost = System.getProperty("http.proxyHost");
		if (Utilities.isNotBlank(proxyHost)) {
			String proxyPort = System.getProperty("http.proxyPort");
			String nonProxyHosts = System.getProperty("http.nonProxyHosts");
			
			if (Utilities.isBlank(nonProxyHosts)) {
				if (Utilities.isNotBlank(proxyHost)) {
					if (Utilities.isNotBlank(proxyPort) && NumberUtilities.isNumber(proxyPort)) {
						return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)));
					} else {
						return new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, 8080));
					}
				}
			} else {
				boolean ignoreProxy = false;
				String urlDomain = getDomainFromUrl(url);
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
					}
				}
			}
		}
		
		return Proxy.NO_PROXY;
	}
	
	public static String getDomainFromUrl(String url) {
		if (!url.startsWith("http") && !url.startsWith("https")) {
			url = "http://" + url;
		}
		URL netUrl;
		try {
			netUrl = new URL(url);
		} catch (MalformedURLException e) {
			return null;
		}
		return netUrl.getHost();
	}
}

