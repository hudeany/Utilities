package de.soderer.utilities;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;

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
	
	public static HttpResponse executeRequest(HttpRequest httpRequest) throws Exception {
		String url = httpRequest.getUrl();
		if (httpRequest.getUrlParameters() != null && httpRequest.getUrlParameters().size() > 0) {
			for (Entry<String, Object> urlParameter : httpRequest.getUrlParameters().entrySet()) {
				StringBuilder urlParameterPart = new StringBuilder();
				urlParameterPart.append(URLEncoder.encode(urlParameter.getKey(), "UTF-8"));
				urlParameterPart.append("=");
				if (urlParameter.getValue() instanceof char[]) {
	            	urlParameterPart.append(URLEncoder.encode(new String((char[]) urlParameter.getValue()), "UTF-8"));
				} else if (urlParameter.getValue() instanceof Object[]) {
	            	boolean isFirstValue = true;
	            	for (Object value : (Object[]) urlParameter.getValue()) {
	            		if (!isFirstValue) {
	            			urlParameterPart.append(",");
	            		}
	            		urlParameterPart.append(URLEncoder.encode(String.valueOf(value), "UTF-8"));
	            		isFirstValue = false;
	            	}
	            } else {
	            	urlParameterPart.append(URLEncoder.encode(String.valueOf(urlParameter.getValue()), "UTF-8"));
				}
				
				if (urlParameterPart.length() > 0) {
					url = addUrlParameter(url, urlParameterPart.toString());
				}
            }
		}
		
		if (httpRequest.getPathParameterData() != null && httpRequest.getPathParameterData().size() > 0) {
			for (Entry<String, Object> pathParameterDataEntry : httpRequest.getPathParameterData().entrySet()) {
				StringBuilder pathParameterPart = new StringBuilder();
				pathParameterPart.append(URLEncoder.encode(pathParameterDataEntry.getKey(), "UTF-8"));
				pathParameterPart.append("=");
				if (pathParameterDataEntry.getValue() instanceof char[]) {
					pathParameterPart.append(URLEncoder.encode(new String((char[]) pathParameterDataEntry.getValue()), "UTF-8"));
				} else if (pathParameterDataEntry.getValue() instanceof Object[]) {
	            	boolean isFirstValue = true;
	            	for (Object value : (Object[]) pathParameterDataEntry.getValue()) {
	            		if (!isFirstValue) {
	            			pathParameterPart.append(",");
	            		}
	            		pathParameterPart.append(URLEncoder.encode(String.valueOf(value), "UTF-8"));
	            		isFirstValue = false;
	            	}
	            } else {
					pathParameterPart.append(URLEncoder.encode(String.valueOf(pathParameterDataEntry.getValue()), "UTF-8"));
				}
				
				if (pathParameterPart.length() > 0) {
					url = addPathParameter(url, pathParameterPart.toString());
				}
			}
		}
		
		InputStream downloadStream = null;
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			if (url.startsWith("https")) {
				connection = (HttpsURLConnection) new URL(url).openConnection();
				
				if (!httpRequest.isCheckSslCertificates()) {
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
					((HttpsURLConnection) connection).setSSLSocketFactory(sslSocketFactory);
				}
			}

	        connection.setRequestMethod(httpRequest.getRequestMethod().name());
			
			if (httpRequest.getCookieData() != null && httpRequest.getCookieData().size() > 0) {
				StringBuilder cookieDataString = new StringBuilder();
				for (Entry<String, String> cookie : httpRequest.getCookieData().entrySet()) {
					if (cookieDataString.length() != 0) {
						cookieDataString.append(';');
					}
					cookieDataString.append(URLEncoder.encode(cookie.getKey(), "UTF-8"));
					cookieDataString.append('=');
					cookieDataString.append(URLEncoder.encode(String.valueOf(cookie.getValue()), "UTF-8"));
				}
				connection.setRequestProperty("Cookie", cookieDataString.toString());
			}

			if (httpRequest.getPostParameters() != null && httpRequest.getPostParameters().size() > 0) {
				StringBuilder postData = new StringBuilder();
		        for (Entry<String, Object> param : httpRequest.getPostParameters().entrySet()) {
		            if (postData.length() != 0) {
		            	postData.append('&');
		            }
		            postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
		            postData.append('=');
		            if (param.getValue() instanceof char[]) {
		            	postData.append(URLEncoder.encode(new String((char[]) param.getValue()), "UTF-8"));
		            } else if (param.getValue() instanceof Object[]) {
		            	boolean isFirstValue = true;
		            	for (Object value : (Object[]) param.getValue()) {
		            		if (!isFirstValue) {
		            			postData.append(",");
		            		}
		            		postData.append(URLEncoder.encode(String.valueOf(value), "UTF-8"));
		            		isFirstValue = false;
		            	}
		            } else {
		            	postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
		            }
		        }
		        byte[] postParametersData = postData.toString().getBytes("UTF-8");

				connection.setDoInput(true);
		        connection.setDoOutput(true);
		        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
		        connection.setRequestProperty("Content-Length", String.valueOf(postParametersData.length));
		        connection.getOutputStream().write(postParametersData);
			} else {
				connection.setDoInput(true);
				connection.setDoOutput(false);
			}
			
			downloadStream = connection.getInputStream();
			
			if (downloadStream == null) {
				return new HttpResponse(connection.getResponseCode(), null);
			} else {
				ByteArrayOutputStream buffer = new ByteArrayOutputStream();
				IOUtils.copy(downloadStream, buffer);
				return new HttpResponse(connection.getResponseCode(), buffer.toByteArray());
			}
		} finally {
			if (downloadStream != null) {
				Utilities.closeQuietly(downloadStream);
			}
		}
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
