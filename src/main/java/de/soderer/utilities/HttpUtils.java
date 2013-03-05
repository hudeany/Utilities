package de.soderer.utilities;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.io.IOUtils;

public class HttpUtils {
	public static void pingUrlWithoutSslCheckNoWaitForAnswer(String pingUrl) throws IOException, NoSuchAlgorithmException, KeyManagementException {
		InputStream downloadStream = null;
		try {
			if (pingUrl.startsWith("https")) {
				// Überprüfung der SSL-zertifikate ausschalten
				SSLContext sslContext = SSLContext.getInstance("TLS");
				TrustManager[] tms = new TrustManager[] {
					new X509TrustManager() {
						public X509Certificate[] getAcceptedIssuers() {
							return null;
						}
						public void checkServerTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
						}
						public void checkClientTrusted(X509Certificate[] arg0, String arg1) throws CertificateException {
						}
					}	
				};
				sslContext.init(null, tms, null);
				// SocketFactory für die Erstellung von HTTPS-Verbindungen erzeugen
				SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
				HttpsURLConnection connection = (HttpsURLConnection) new URL(pingUrl).openConnection();
				connection.setSSLSocketFactory(sslSocketFactory);
				connection.setRequestMethod("POST");
				connection.setConnectTimeout(5000);
				// möglichst kurzer ReadTimeout
				connection.setReadTimeout(100);
				connection.setDoInput(true);
				connection.setDoOutput(false);
				downloadStream = connection.getInputStream();
			}
			else {
				HttpURLConnection connection = (HttpURLConnection) new URL(pingUrl).openConnection();
				connection.setRequestMethod("POST");
				connection.setConnectTimeout(5000);
				// möglichst kurzer ReadTimeout
				connection.setReadTimeout(100);
				connection.setDoInput(true);
				connection.setDoOutput(false);
				downloadStream = connection.getInputStream();
			}
		}
		catch (SocketTimeoutException stex) {
			//Diese Exception wird erwartet, da der ReadTimeout sehr kurz sein soll
		}
		finally {
			if (downloadStream != null)
				IOUtils.closeQuietly(downloadStream);
		}
	}
}
