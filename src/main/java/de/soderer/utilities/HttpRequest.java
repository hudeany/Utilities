package de.soderer.utilities;

import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
	private String requestMethod;
	private String url;
	
	private Map<String, Object> urlParameters = new HashMap<String, Object>();
	private Map<String, Object> postParameters= new HashMap<String, Object>();
	private Map<String, byte[]> fileAttachments= new HashMap<String, byte[]>();
	private Map<String, Object> sessionData= new HashMap<String, Object>();
	private Map<String, String> cookieData= new HashMap<String, String>();
	
	private boolean checkSslCertificates = true;
	
	public HttpRequest(String requestMethod, String url) {
		this.requestMethod = requestMethod;
		this.url = url;
	}

	public boolean isCheckSslCertificates() {
		return checkSslCertificates;
	}

	public void setCheckSslCertificates(boolean checkSslCertificates) {
		this.checkSslCertificates = checkSslCertificates;
	}

	public String getRequestMethod() {
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
	
	public void addPostParameter(String key, Object value) {
		postParameters.put(key, value);
	}

	public Map<String, byte[]> getFileAttachments() {
		return fileAttachments;
	}
	
	public void addFileData(String name, byte[] data) {
		urlParameters.put(name, data);
	}

	public Map<String, Object> getSessionData() {
		return sessionData;
	}
	
	public void addSessionParameter(String key, Object value) {
		sessionData.put(key, value);
	}

	public Map<String, String> getCookieData() {
		return cookieData;
	}
	
	public void addCookieData(String name, String value) {
		cookieData.put(name, value);
	}
}