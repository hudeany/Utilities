package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Test;

public class NetworkUtilitiesTest {
	@Test
	public void testIpV4() {
		try {
			Assert.assertTrue(NetworkUtilities.isValidIpV4("0.0.0.0"));
			Assert.assertTrue(NetworkUtilities.isValidIpV4("255.255.255.255"));
			Assert.assertTrue(NetworkUtilities.isValidIpV4("192.168.0.5"));

			Assert.assertFalse(NetworkUtilities.isValidIpV4("0.0.0"));
			Assert.assertFalse(NetworkUtilities.isValidIpV4("256.0.0.0"));
			Assert.assertFalse(NetworkUtilities.isValidIpV4("0.256.0.0"));
			Assert.assertFalse(NetworkUtilities.isValidIpV4("0.0.256.0"));
			Assert.assertFalse(NetworkUtilities.isValidIpV4("0.0.0.256"));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testIpV6() {
		try {
			Assert.assertTrue(NetworkUtilities.isValidIpV6("1080:0:0:0:8:800:200C:417A"));
			Assert.assertTrue(NetworkUtilities.isValidIpV6("1080:0:0:0:8:800:200c:417A"));
			Assert.assertTrue(NetworkUtilities.isValidIpV6("1080::8:800:200C:417A"));
			Assert.assertTrue(NetworkUtilities.isValidIpV6("1080::8:800:200C:417a"));
			Assert.assertTrue(NetworkUtilities.isValidIpV6("::FFFF:129.144.52.38"));
			Assert.assertTrue(NetworkUtilities.isValidIpV6("::ffff:129.144.52.38"));
			Assert.assertTrue(NetworkUtilities.isValidIpV6("::129.144.52.38"));
			Assert.assertTrue(NetworkUtilities.isValidIpV6("::FFFF:255"));

			Assert.assertFalse(NetworkUtilities.isValidIpV6("::FFFF:144.52.38"));
			Assert.assertFalse(NetworkUtilities.isValidIpV6("::ffff:144.52.38"));
			Assert.assertFalse(NetworkUtilities.isValidIpV6("::FFFF:52.38"));
			Assert.assertFalse(NetworkUtilities.isValidIpV6("::ffff:52.38"));
			Assert.assertFalse(NetworkUtilities.isValidIpV6("::52.38"));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testHttpsWithPem() {
		try {
			// "Digital Signature Trust Co." valid until 30.9.2021
			final String dst_CA_PemString = "-----BEGIN CERTIFICATE-----\n"
					+ "MIIDSjCCAjKgAwIBAgIQRK+wgNajJ7qJMDmGLvhAazANBgkqhkiG9w0BAQUFADA/\n"
					+ "MSQwIgYDVQQKExtEaWdpdGFsIFNpZ25hdHVyZSBUcnVzdCBDby4xFzAVBgNVBAMT\n"
					+ "DkRTVCBSb290IENBIFgzMB4XDTAwMDkzMDIxMTIxOVoXDTIxMDkzMDE0MDExNVow\n"
					+ "PzEkMCIGA1UEChMbRGlnaXRhbCBTaWduYXR1cmUgVHJ1c3QgQ28uMRcwFQYDVQQD\n"
					+ "Ew5EU1QgUm9vdCBDQSBYMzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n"
					+ "AN+v6ZdQCINXtMxiZfaQguzH0yxrMMpb7NnDfcdAwRgUi+DoM3ZJKuM/IUmTrE4O\n"
					+ "rz5Iy2Xu/NMhD2XSKtkyj4zl93ewEnu1lcCJo6m67XMuegwGMoOifooUMM0RoOEq\n"
					+ "OLl5CjH9UL2AZd+3UWODyOKIYepLYYHsUmu5ouJLGiifSKOeDNoJjj4XLh7dIN9b\n"
					+ "xiqKqy69cK3FCxolkHRyxXtqqzTWMIn/5WgTe1QLyNau7Fqckh49ZLOMxt+/yUFw\n"
					+ "7BZy1SbsOFU5Q9D8/RhcQPGX69Wam40dutolucbY38EVAjqr2m7xPi71XAicPNaD\n"
					+ "aeQQmxkqtilX4+U9m5/wAl0CAwEAAaNCMEAwDwYDVR0TAQH/BAUwAwEB/zAOBgNV\n"
					+ "HQ8BAf8EBAMCAQYwHQYDVR0OBBYEFMSnsaR7LHH62+FLkHX/xBVghYkQMA0GCSqG\n"
					+ "SIb3DQEBBQUAA4IBAQCjGiybFwBcqR7uKGY3Or+Dxz9LwwmglSBd49lZRNI+DT69\n"
					+ "ikugdB/OEIKcdBodfpga3csTS7MgROSR6cz8faXbauX+5v3gTt23ADq1cEmv8uXr\n"
					+ "AvHRAosZy5Q6XkjEGB5YGV8eAlrwDPGxrancWYaLbumR9YbK+rlmM6pZW87ipxZz\n"
					+ "R8srzJmwN0jP41ZL9c8PDHIyh8bwRLtTcm1D9SZImlJnt1ir/md2cXjbDaJWFBM5\n"
					+ "JDGFoqgCWjBH4d1QB7wCCZAA62RjYJsWvIjJEubSfZGL+T0yjWW06XyxV3bqxbYo\n"
					+ "Ob8VZRzI9neWagqNdwvYkQsEjgfbKbYK7p2CNTUQ\n"
					+ "-----END CERTIFICATE-----";

			try (InputStream dataStream = NetworkUtilities.openHttpsDataInputStreamWithPemCertificate("https://soderer.de", new ByteArrayInputStream(dst_CA_PemString.getBytes(StandardCharsets.UTF_8)))) {
				Assert.assertTrue(dataStream != null);
				final String data = IoUtilities.toString(dataStream, StandardCharsets.UTF_8);
				Assert.assertTrue(data != null && data.length() > 10);
			}
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
