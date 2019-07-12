package de.soderer.utilities.http;

import java.util.HashMap;
import java.util.Map;

import de.soderer.utilities.Utilities;

public class HttpRequest {
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
	
	private HttpMethod requestMethod;
	private String url;
	
	private Map<String, Object> urlParameters = new HashMap<>();
	private Map<String, Object> postParameters= new HashMap<>();
	private Map<String, byte[]> fileAttachments= new HashMap<>();
	private Map<String, Object> pathParameterData= new HashMap<>();
	private Map<String, String> cookieData= new HashMap<>();
	
	private boolean checkSslCertificates = true;
	
	/**
	 * Http POST Request
	 * 
	 * @param url
	 * @throws Exception
	 */
	public HttpRequest(String url) throws Exception {
		this(HttpMethod.POST, url);
	}
	
	public HttpRequest(HttpMethod requestMethod, String url) throws Exception {
		if (requestMethod == null) {
			throw new Exception("Invalid empty HttpMethod parameter");
		}
		if (Utilities.isBlank(url)) {
			throw new Exception("Invalid empty url");
		}
		this.requestMethod = requestMethod;
		this.url = url;
	}

	public boolean isCheckSslCertificates() {
		return checkSslCertificates;
	}

	public void setCheckSslCertificates(boolean checkSslCertificates) {
		this.checkSslCertificates = checkSslCertificates;
	}

	public HttpMethod getRequestMethod() {
		return requestMethod;
	}

	public String getUrl() {
		return url;
	}
	
	public void addUrlParameter(String key, Object value) {
		urlParameters.put(key, value);
	}

	public Map<String, Object> getUrlParameters() {
		return urlParameters;
	}

	public Map<String, Object> getPostParameters() {
		return postParameters;
	}
	
	public void addPostParameter(String key, Object value) throws Exception {
		postParameters.put(key, value);
	}

	public Map<String, byte[]> getFileAttachments() {
		return fileAttachments;
	}
	
	public void addFileData(String name, byte[] data) {
		urlParameters.put(name, data);
	}

	public Map<String, Object> getPathParameterData() {
		return pathParameterData;
	}
	
	public void addPathParameter(String key, Object value) {
		pathParameterData.put(key, value);
	}

	public Map<String, String> getCookieData() {
		return cookieData;
	}
	
	public void addCookieData(String name, String value) {
		cookieData.put(name, value);
	}
}