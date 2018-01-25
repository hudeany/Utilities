package de.soderer.utilities;

import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.util.Map;

public class HttpResponse {
	private int httpStatusCode;
	private byte[] data;
	private Map<String, String> sessionData;
	private Map<String, String> cookieData;
	
	public HttpResponse(int httpStatusCode, byte[] data) {
		this.httpStatusCode = httpStatusCode;
		this.data = data;
	}

	/**
	 * Result code like:
	 * 	HttpURLConnection.HTTP_OK (= 200)
	 * @return
	 */
	public int getHttpStatusCode() {
		return httpStatusCode;
	}
	
	public String getHttpStatusText(int httpStatusCode) {
		return HttpUtilities.getHttpStatusText(httpStatusCode);
	}
	
	public boolean isHttpStatusOK() {
		return getHttpStatusCode() == HttpURLConnection.HTTP_OK;
	}

	public byte[] getData() {
		return data;
	}

	public String getDataString() {
		return new String(data, Charset.forName("UTF-8"));
	}

	public Map<String, String> getSessionData() {
		return sessionData;
	}

	public void setSessionData(Map<String, String> sessionData) {
		this.sessionData = sessionData;
	}

	public Map<String, String> getCookieData() {
		return cookieData;
	}

	public void setCookieData(Map<String, String> cookieData) {
		this.cookieData = cookieData;
	}
}