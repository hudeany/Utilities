package de.soderer.utilities.http;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.Proxy;
import java.security.KeyStore;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public class TrustManagerUtilities {
	/**
	 * Use systems default trusted keystore and define an additionlaly used trusted keystore
	 *
	 * Usage:
	 *   SSLContext sslContext = SSLContext.getInstance("TLS");
	 *   sslContext.init(null, new TrustManager[] { getDefaultTrustManagers() }, null);
	 *   SSLContext.setDefault(sslContext);
	 *
	 * @param trustedKeyStore
	 * @return
	 * @throws Exception
	 */
	public static TrustManager[] getDefaultTrustManagers() throws Exception {
		final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());

		// Init the TrustmanagerFactory with systems default trust store.
		trustManagerFactory.init((KeyStore) null);

		return trustManagerFactory.getTrustManagers();
	}

	public static X509TrustManager createTrustAllTrustManager() throws Exception {
		return new X509TrustManager() {
			@Override
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			@Override
			public void checkClientTrusted(final java.security.cert.X509Certificate[] certificates, final String authType) {
				// nothing to do
			}

			@Override
			public void checkServerTrusted(final java.security.cert.X509Certificate[] certificates, final String authType) {
				// nothing to do
			}
		};
	}

	public static KeyStore readKeyStore(final File keystoreFile) throws Exception {
		return readKeyStore(keystoreFile, null);
	}

	public static KeyStore readKeyStore(final File keystoreFile, final char[] keystorePassword) throws Exception {
		KeyStore trustedKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		try (FileInputStream myKeys = new FileInputStream(keystoreFile)) {
			trustedKeyStore.load(myKeys, keystorePassword);
		}
		if (trustedKeyStore.size() == 0 && keystorePassword == null) {
			trustedKeyStore = KeyStore.getInstance(KeyStore.getDefaultType());
			try (FileInputStream myKeys = new FileInputStream(keystoreFile)) {
				trustedKeyStore.load(myKeys, "".toCharArray());
			}
		}
		return trustedKeyStore;
	}

	public static TrustManager createTrustManagerForKeyStore(final File keystoreFile) throws Exception {
		return createTrustManagerForKeyStore(readKeyStore(keystoreFile, null));
	}

	public static TrustManager createTrusManagerForKeyStore(final File keystoreFile, final char[] keystorePassword) throws Exception {
		return createTrustManagerForKeyStore(readKeyStore(keystoreFile, keystorePassword));
	}

	/**
	 * Override systems default trusted keystore and define a trusted keystore to be used as single trusted keystore for certificate checks
	 *
	 * Usage:
	 *   SSLContext sslContext = SSLContext.getInstance("TLS");
	 *   sslContext.init(null, new TrustManager[] { createTrustmanagerForKeyStore(myOnlyTrustedKeyStore) }, null);
	 *   SSLContext.setDefault(sslContext);
	 *
	 * @param trustedKeyStore
	 * @return
	 * @throws Exception
	 */
	public static TrustManager createTrustManagerForKeyStore(final KeyStore trustedKeyStore) throws Exception {
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init(trustedKeyStore);

		for (final TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
			if (trustManager instanceof X509TrustManager) {
				return trustManager;
			}
		}

		throw new Exception("Cannot create TrustManager");
	}

	public static void createTrustStoreFile(final String hostnameOrIpAndPort, final int defaultPort, final File trustStoreFile, final char[] trustStorePassword, final Proxy proxy) throws Exception {
		if (trustStoreFile.exists()) {
			throw new Exception("File '" + trustStoreFile.getAbsolutePath() + "' already exists");
		}

		String hostnameOrIp;
		int port;
		final String[] hostParts = hostnameOrIpAndPort.split(":");
		if (hostParts.length == 2) {
			hostnameOrIp = hostParts[0];
			try {
				port = Integer.parseInt(hostParts[1]);
			} catch (@SuppressWarnings("unused") final Exception e) {
				throw new Exception("Invalid port: " + hostParts[1]);
			}
		} else {
			hostnameOrIp = hostnameOrIpAndPort;
			port = defaultPort;
		}

		final X509Certificate certificate = HttpUtilities.getServerTlsCertificate(hostnameOrIp, port, proxy);
		if (certificate == null) {
			throw new Exception("Cannot get TLS certificate for '" + hostnameOrIp + ":" + port + "'");
		}

		final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(null);

		final char[] password = trustStorePassword == null ? new char[0] : trustStorePassword;
		keyStore.setCertificateEntry(hostnameOrIp, certificate);

		try (OutputStream javaKeyStoreOutputStream = new FileOutputStream(trustStoreFile)) {
			keyStore.store(javaKeyStoreOutputStream, password);
		}
	}
}
