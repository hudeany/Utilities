package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

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
		} catch (Exception e) {
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
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testHttpsWithPem() {
		try {
			// "GeoTrust Global CA" valid until 21.05.2022
			String geoTrustGlobalCAPemString = "-----BEGIN CERTIFICATE-----\n"
			+ "MIIEfjCCA2agAwIBAgIIK2oDOOdYa9MwDQYJKoZIhvcNAQELBQAwSTELMAkGA1UE\n"
			+ "BhMCVVMxEzARBgNVBAoTCkdvb2dsZSBJbmMxJTAjBgNVBAMTHEdvb2dsZSBJbnRl\n"
			+ "cm5ldCBBdXRob3JpdHkgRzIwHhcNMTcwNTAzMDg0NjI2WhcNMTcwNzI2MDg0MjAw\n"
			+ "WjBnMQswCQYDVQQGEwJVUzETMBEGA1UECAwKQ2FsaWZvcm5pYTEWMBQGA1UEBwwN\n"
			+ "TW91bnRhaW4gVmlldzETMBEGA1UECgwKR29vZ2xlIEluYzEWMBQGA1UEAwwNd3d3\n"
			+ "Lmdvb2dsZS5kZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMZDhY2e\n"
			+ "jlvOeLxB+xYWPXZYDdA2i0jTWTuO4/jm4YsUuaAjBS9rnceAp9yv3BoPyVIx7xd2\n"
			+ "RcCYVexJ5+mymY3k16Leqvp8fagOymBCbrp3KIXhD2KL836QK6Z0/uLhYMbGMBVa\n"
			+ "bmPCmxFIcKsqN2AYWSi3lB4mEm839QQc+bLro04hdWpFwnJxCMNkZugdyN1vve72\n"
			+ "cuDyuNxWayWT+f7uWWAkyWDnC+4aIEFfLyKKJ+jZFCox+L/4TOShTbmzRIrUx4gH\n"
			+ "zwnMKgun7Uz2RtoxdbCmspVJHJpbSqYTBCnUFleQUSJLzG1fzqOX0TtagEwiNSsu\n"
			+ "8wxpddYOmendwJcCAwEAAaOCAUowggFGMB0GA1UdJQQWMBQGCCsGAQUFBwMBBggr\n"
			+ "BgEFBQcDAjAYBgNVHREEETAPgg13d3cuZ29vZ2xlLmRlMGgGCCsGAQUFBwEBBFww\n"
			+ "WjArBggrBgEFBQcwAoYfaHR0cDovL3BraS5nb29nbGUuY29tL0dJQUcyLmNydDAr\n"
			+ "BggrBgEFBQcwAYYfaHR0cDovL2NsaWVudHMxLmdvb2dsZS5jb20vb2NzcDAdBgNV\n"
			+ "HQ4EFgQUbFE8K73jQAir/Xij8BVB4hOz/xAwDAYDVR0TAQH/BAIwADAfBgNVHSME\n"
			+ "GDAWgBRK3QYWG7z2aLV29YG2u2IaulqBLzAhBgNVHSAEGjAYMAwGCisGAQQB1nkC\n"
			+ "BQEwCAYGZ4EMAQICMDAGA1UdHwQpMCcwJaAjoCGGH2h0dHA6Ly9wa2kuZ29vZ2xl\n"
			+ "LmNvbS9HSUFHMi5jcmwwDQYJKoZIhvcNAQELBQADggEBAGx2r1R7Q9sSmuUiRYX8\n"
			+ "CbFic8AjfepkGZ7zKXvoeNJOo4CUe5hq4w2c0ZspsyvITux31qEC6e5lbmMtlbem\n"
			+ "y4hG+O13kkESspgCFjx/R6h2+bCwB0gtQc9TDoK8ZQIAQflK99r1kEwcJ4M7VMs6\n"
			+ "uCSvAerbFOWM7FrxzDj9kiUzQFvsvVLYBHw4O8a14ThLXbUe3NoVHz4MSmWt4sj7\n"
			+ "bmJHgmpiOHzUrXv3jJ0W/gGELBGrF8d/zAHSvN4Qn3lxTTIHTyKOVHjvRaPIHQEC\n"
			+ "v/8avmUFQ0qvJoJE6wGYbWbZNx+IKxAm8DBPJmyxaIolKHh5GUO9FBbk6wrKdb+O\n"
			+ "F3Y=\n"
			+ "-----END CERTIFICATE-----";
			
			try (InputStream dataStream = NetworkUtilities.openHttpsDataInputStreamWithPemCertificate("https://www.google.de", new ByteArrayInputStream(geoTrustGlobalCAPemString.getBytes("UTF-8")))) {
				Assert.assertTrue(dataStream != null);
			}
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
