package de.soderer.utilities.http;

import java.util.HashMap;
import java.util.Map;

import de.soderer.utilities.Utilities;

public class HttpRequest {
	public static final String SECURE_HTTP_PROTOCOL_SIGN = "https://";
	public static final String HTTP_PROTOCOL_SIGN = "http://";

	public enum HttpMethod {
		GET,
		HEAD,
		POST,
		PUT,
		DELETE,
		CONNECT,
		OPTIONS,
		TRACE,
		PATCH
	}

	private final HttpMethod requestMethod;
	private final String url;
	private String encoding = "UTF-8";

	private final Map<String, String> headers = new HashMap<>();
	private final Map<String, Object> urlParameters = new HashMap<>();
	private final Map<String, Object> postParameters= new HashMap<>();
	private String requestBody = null;
	private final Map<String, byte[]> fileAttachments= new HashMap<>();
	private final Map<String, Object> pathParameterData= new HashMap<>();
	private final Map<String, String> cookieData= new HashMap<>();

	private boolean checkSslCertificates = true;

	/**
	 * Http POST Request
	 *
	 * @param url
	 * @throws Exception
	 */
	public HttpRequest(final String url) throws Exception {
		this(HttpMethod.POST, url);
	}

	public HttpRequest(final HttpMethod requestMethod, final String url) throws Exception {
		if (Utilities.isBlank(url)) {
			throw new Exception("Invalid empty url");
		}
		this.requestMethod = requestMethod == null ? HttpMethod.GET : requestMethod;
		this.url = url;
	}

	public boolean isCheckSslCertificates() {
		return checkSslCertificates;
	}

	public HttpRequest setCheckSslCertificates(final boolean checkSslCertificates) {
		this.checkSslCertificates = checkSslCertificates;

		return this;
	}

	public HttpMethod getRequestMethod() {
		return requestMethod;
	}

	public String getUrl() {
		return url;
	}

	/**
	 * Check for protocol "https://" or "http://" (fallback: "http://")
	 *
	 * @return
	 * @throws Exception
	 */
	public String getUrlWithProtocol() throws Exception {
		if (Utilities.isBlank(url)) {
			throw new Exception("Invalid empty URL for http request");
		} else if (url.startsWith(SECURE_HTTP_PROTOCOL_SIGN) || url.startsWith(HTTP_PROTOCOL_SIGN)) {
			return url;
		} else {
			return HTTP_PROTOCOL_SIGN + url;
		}
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public HttpRequest addHeader(final String key, final String value) {
		headers.put(key, value);

		return this;
	}

	public Map<String, Object> getUrlParameters() {
		return urlParameters;
	}

	public HttpRequest addUrlParameter(final String key, final Object value) {
		urlParameters.put(key, value);

		return this;
	}

	public Map<String, Object> getPostParameters() {
		return postParameters;
	}

	public HttpRequest addPostParameter(final String key, final Object value) throws Exception {
		if (requestBody != null) {
			throw new Exception("RequestBody is already set. Post parameters cannot be set therefore");
		} else {
			postParameters.put(key, value);

			return this;
		}
	}

	public Map<String, byte[]> getFileAttachments() {
		return fileAttachments;
	}

	public HttpRequest addFileData(final String name, final byte[] data) {
		fileAttachments.put(name, data);

		return this;
	}

	public Map<String, Object> getPathParameterData() {
		return pathParameterData;
	}

	public HttpRequest addPathParameter(final String key, final Object value) {
		pathParameterData.put(key, value);

		return this;
	}

	public Map<String, String> getCookieData() {
		return cookieData;
	}

	public HttpRequest addCookieData(final String name, final String value) {
		cookieData.put(name, value);

		return this;
	}

	public String getEncoding() {
		return encoding;
	}

	public HttpRequest setEncoding(final String encoding) {
		this.encoding = encoding;

		return this;
	}

	public String getRequestBody() {
		return requestBody;
	}

	public HttpRequest setRequestBody(final String requestBody) throws Exception {
		if (postParameters.size() > 0) {
			throw new Exception("Post parameters are already set. RequestBody cannot be set therefore");
		} else {
			this.requestBody = requestBody;

			return this;
		}
	}
}