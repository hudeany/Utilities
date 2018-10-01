package de.soderer.utilities;

import java.util.Map;

public class HttpResponse {
	private int httpCode;
	private String content;
	private String contentType;
	private Map<String, String> headers;
	
	public HttpResponse(int httpCode, String content, String contentType, Map<String, String> headers) {
		this.httpCode = httpCode;
		this.content = content;
		this.contentType = contentType;
		this.headers = headers;
	}

	public int getHttpCode() {
		return httpCode;
	}

	public String getContent() {
		return content;
	}

	public String getContentType() {
		return contentType;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}
}
