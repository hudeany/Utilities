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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
				TrustManager[] tms = new TrustManager[] {
					new X509TrustManager() {
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
					}
				};
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

	public static final String CONTENT_ENCODING = "UTF-8";
	
	public static final String CONTENT_TYPE_JAVASCRIPT = "application/javascript";
	public static final String CONTENT_TYPE_JSON = "application/json";
	public static final String APPLICATION_JSON_UTF8 = CONTENT_TYPE_JSON + ";charset=" + CONTENT_ENCODING;
	public static final String IMAGE_CONTENT_TYPE = "image";
	
	public static final String SECURE_HTTP_PROTOCOL_SIGN = "https://";
	public static final String HTTP_PROTOCOL_SIGN = "http://";


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
	
	public static String addUrlParameter(String url, String parameterName, Object parameterValue, String encodingCharSet) throws UnsupportedEncodingException {
		StringBuilder escapedParameterNameAndValue = new StringBuilder();

		if (Utilities.isEmpty(encodingCharSet)) {
			escapedParameterNameAndValue.append(parameterName);
		} else {
			escapedParameterNameAndValue.append(URLEncoder.encode(parameterName, encodingCharSet));
		}

		escapedParameterNameAndValue.append('=');

		if (parameterValue instanceof char[]) {
			if (Utilities.isEmpty(encodingCharSet)) {
				escapedParameterNameAndValue.append(new String((char[]) parameterValue));
			} else {
				escapedParameterNameAndValue.append(URLEncoder.encode(new String((char[]) parameterValue), encodingCharSet));
			}
		} else if (parameterValue instanceof Object[]) {
        	boolean isFirstValue = true;
        	for (Object value : (Object[]) parameterValue) {
        		if (!isFirstValue) {
        			escapedParameterNameAndValue.append(",");
        		}
        		if (Utilities.isEmpty(encodingCharSet)) {
    				escapedParameterNameAndValue.append(String.valueOf(value));
    			} else {
    				escapedParameterNameAndValue.append(URLEncoder.encode(String.valueOf(value), encodingCharSet));
    			}
        		isFirstValue = false;
        	}
		} else {
			if (Utilities.isEmpty(encodingCharSet)) {
				escapedParameterNameAndValue.append(String.valueOf(parameterValue));
			} else {
				escapedParameterNameAndValue.append(URLEncoder.encode(String.valueOf(parameterValue), encodingCharSet));
			}
		}
		return addUrlParameter(url, escapedParameterNameAndValue.toString());
	}

	public static String addPathParameter(String url, String escapedParameterNameAndValue) throws UnsupportedEncodingException {
		StringBuilder newUrl = new StringBuilder();
		int insertPosition = url.indexOf('?');
		if (insertPosition < 0) {
			insertPosition = url.indexOf('#');
		}
		
		if (insertPosition < 0) {
			newUrl.append(url);
			newUrl.append(";");
			newUrl.append(escapedParameterNameAndValue);
		} else {
			newUrl.append(url.substring(0, insertPosition));
			newUrl.append(";");
			newUrl.append(escapedParameterNameAndValue);
			newUrl.append(url.substring(insertPosition));
		}
		
		return newUrl.toString();
	}

	public static String addUrlParameter(String url, String escapedParameterNameAndValue) throws UnsupportedEncodingException {
		StringBuilder newUrl = new StringBuilder();
		int insertPosition = url.indexOf('#');
		
		if (insertPosition < 0) {
			newUrl.append(url);
			newUrl.append(url.indexOf('?') <= -1 ? '?' : '&');
			newUrl.append(escapedParameterNameAndValue);
		} else {
			newUrl.append(url.substring(0, insertPosition));
			newUrl.append(url.indexOf('?') <= -1 ? '?' : '&');
			newUrl.append(escapedParameterNameAndValue);
			newUrl.append(url.substring(insertPosition));
		}
		
		return newUrl.toString();
	}
	
	public static String getPlainParameterFromHtml(String htmlText, String parameterName) throws Exception {
		if (Utilities.isBlank(htmlText)) {
			return null;
		} else {
			Pattern parameterPattern = Pattern.compile("\\W" + parameterName + "\\s*=(\\w*)\\W", Pattern.MULTILINE);
			Matcher parameterMatcher = parameterPattern.matcher(htmlText);
			if (parameterMatcher.find()) {
				return parameterMatcher.group(1).trim();
			} else {
				return null;
			}
		}
	}
	
	public static String getQuotedParameterFromHtml(String htmlText, String parameterName) throws Exception {
		if (Utilities.isBlank(htmlText)) {
			return null;
		} else {
			Pattern parameterPattern = Pattern.compile("\\W" + parameterName + "\\s*=\\s\"(\\w*)\"\\W", Pattern.MULTILINE);
			Matcher parameterMatcher = parameterPattern.matcher(htmlText);
			if (parameterMatcher.find()) {
				return parameterMatcher.group(1).trim();
			} else {
				return null;
			}
		}
	}
	
	public static String getHttpStatusText(int httpStatusCode) {
		switch (httpStatusCode) {
			case HttpURLConnection.HTTP_OK:
				// 200
				return "OK";
			case HttpURLConnection.HTTP_CREATED:
				// 201
				return "Created";
			case HttpURLConnection.HTTP_ACCEPTED:
				// 202
				return "Accepted";
			case HttpURLConnection.HTTP_NOT_AUTHORITATIVE:
				// 203
				return "Non-Authoritative Information";
			case HttpURLConnection.HTTP_NO_CONTENT:
				// 204
				return "No Content";
			case HttpURLConnection.HTTP_RESET:
				// 205
				return "Reset Content";
			case HttpURLConnection.HTTP_PARTIAL:
				// 206
				return "Partial Content";
			case HttpURLConnection.HTTP_MULT_CHOICE:
				// 300
				return "Multiple Choices";
			case HttpURLConnection.HTTP_MOVED_PERM:
				// 301
				return "Moved Permanently";
			case HttpURLConnection.HTTP_MOVED_TEMP:
				// 302
				return "Temporary Redirect";
			case HttpURLConnection.HTTP_SEE_OTHER:
				// 303
				return "See Other";
			case HttpURLConnection.HTTP_NOT_MODIFIED:
				// 304
				return "Not Modified";
			case HttpURLConnection.HTTP_USE_PROXY:
				// 305
				return "Use Proxy";
			case HttpURLConnection.HTTP_BAD_REQUEST:
				// 400
				return "Bad Request";
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				// 401
				return "Unauthorized";
			case HttpURLConnection.HTTP_PAYMENT_REQUIRED:
				// 402
				return "Payment Required";
			case HttpURLConnection.HTTP_FORBIDDEN:
				// 403
				return "Forbidden";
			case HttpURLConnection.HTTP_NOT_FOUND:
				// 404
				return "Not Found";
			case HttpURLConnection.HTTP_BAD_METHOD:
				// 405
				return "Method Not Allowed";
			case HttpURLConnection.HTTP_NOT_ACCEPTABLE:
				// 406
				return "Not Acceptable";
			case HttpURLConnection.HTTP_PROXY_AUTH:
				// 407
				return "Proxy Authentication Required";
			case HttpURLConnection.HTTP_CLIENT_TIMEOUT:
				// 408
				return "Request Time-Out";
			case HttpURLConnection.HTTP_CONFLICT:
				// 409
				return "Conflict";
			case HttpURLConnection.HTTP_GONE:
				// 410
				return "Gone";
			case HttpURLConnection.HTTP_LENGTH_REQUIRED:
				// 411
				return "Length Required";
			case HttpURLConnection.HTTP_PRECON_FAILED:
				// 412
				return "Precondition Failed";
			case HttpURLConnection.HTTP_ENTITY_TOO_LARGE:
				// 413
				return "Request Entity Too Large";
			case HttpURLConnection.HTTP_REQ_TOO_LONG:
				// 414
				return "Request-URI Too Large";
			case HttpURLConnection.HTTP_UNSUPPORTED_TYPE:
				// 415
				return "Unsupported Media Type";
			case HttpURLConnection.HTTP_INTERNAL_ERROR:
				// 500
				return "Internal Server Error";
			case HttpURLConnection.HTTP_NOT_IMPLEMENTED:
				// 501
				return "Not Implemented";
			case HttpURLConnection.HTTP_BAD_GATEWAY:
				// 502
				return "Bad Gateway";
			case HttpURLConnection.HTTP_UNAVAILABLE:
				// 503
				return "Service Unavailable";
			case HttpURLConnection.HTTP_GATEWAY_TIMEOUT:
				// 504
				return "Gateway Timeout";
			case HttpURLConnection.HTTP_VERSION:
				// 505
				return "HTTP Version Not Supported";
			default:
				return "Unknown Http Status Code (" + httpStatusCode + ")";
		}
	}
}

