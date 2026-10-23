package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.sun.net.httpserver.HttpServer;

import de.soderer.utilities.collection.MapUtilities;

@SuppressWarnings("static-method")
public class UtilitiesTest {
	/** Self-signed test certificate with subject "EMAILADDRESS=test.user@example.com, CN=Test User, O=Soderer Test" */
	private static final String TEST_CERTIFICATE_PEM = ""
			+ "-----BEGIN CERTIFICATE-----\n"
			+ "MIIBvDCCAWGgAwIBAgIIZVd/S5M9yYwwCgYIKoZIzj0EAwMwUTEVMBMGA1UEChMM\n"
			+ "U29kZXJlciBUZXN0MRIwEAYDVQQDEwlUZXN0IFVzZXIxJDAiBgkqhkiG9w0BCQEW\n"
			+ "FXRlc3QudXNlckBleGFtcGxlLmNvbTAgFw0yNjEwMjMxMTU2MzhaGA8yMTI2MDky\n"
			+ "OTExNTYzOFowUTEVMBMGA1UEChMMU29kZXJlciBUZXN0MRIwEAYDVQQDEwlUZXN0\n"
			+ "IFVzZXIxJDAiBgkqhkiG9w0BCQEWFXRlc3QudXNlckBleGFtcGxlLmNvbTBZMBMG\n"
			+ "ByqGSM49AgEGCCqGSM49AwEHA0IABL2EKEfa3mqS5AHMkHopxGcyK0ZYs3qmLYb1\n"
			+ "nz6bY2fbDMO4fTjAf+nX3siHQJeJgDLw3/MOFK/scBM3P47f1eijITAfMB0GA1Ud\n"
			+ "DgQWBBTIp82pVTMPh/VNtUW99GJ+HCwVEjAKBggqhkjOPQQDAwNJADBGAiEAvjQv\n"
			+ "UTlQJBmNqklVcNSP8tt4M1wM343xySIxNS/LMcQCIQCsYoz9zB7ktF0K4d8+MtpD\n"
			+ "nzA6r3hWNbECV/RtN1xLYg==\n"
			+ "-----END CERTIFICATE-----\n";

	@TempDir
	Path tempDir;

	@Test
	public void test_UUID() {
		try {
			final String uuidString = Utilities.generateUUID();
			assertNotNull(uuidString);
			final UUID uuid = Utilities.getUUIDFromString(uuidString);
			assertNotNull(uuid);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void test_Gzipping() {
		try {
			final byte[] zippedData = Utilities.gzipByteArray(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
			final byte[] unzippedData = Utilities.gunzipByteArray(zippedData);
			assertEquals(TextUtilities.GERMAN_TEST_STRING, new String(unzippedData, StandardCharsets.UTF_8));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void test_Base64() {
		try {
			final String encodedData = Utilities.encodeBase64(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
			final byte[] decodedData = Utilities.decodeBase64(encodedData);
			assertEquals(TextUtilities.GERMAN_TEST_STRING, new String(decodedData, StandardCharsets.UTF_8));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void test_Base64_maxLineLength() {
		try {
			final String shortDataString = "R0lGODdhDQAIAIAAAAAAANnZ2SwAAAAADQAIAAACF4SDGQar3xxbJ9p0qa7R0YxwzaFME1IAADs=";
			final byte[] shortData = Utilities.decodeBase64(shortDataString);
			final String encodedShortData = Utilities.encodeBase64(shortData, 80, "\n");
			assertEquals(shortDataString, encodedShortData);
			final byte[] decodedShortData = Utilities.decodeBase64(encodedShortData);
			assertTrue(Arrays.equals(shortData, decodedShortData));

			final String dataString = "R0lGODdhDQAIAIAAAAAAANnZ2SwAAAAADQAIAAACF4SDGQar3xxbJ9p0qa7R0YxwzYxwzYxwzYxwzYxwzYxwzaFME1IAADs=";
			final byte[] data = Utilities.decodeBase64(dataString);
			final String encodedData = Utilities.encodeBase64(data, 80, "\n");
			assertEquals("R0lGODdhDQAIAIAAAAAAANnZ2SwAAAAADQAIAAACF4SDGQar3xxbJ9p0qa7R0YxwzYxwzYxwzYxwzYxw\nzYxwzaFME1IAADs=", encodedData);
			final byte[] decodedData = Utilities.decodeBase64(encodedData);
			assertTrue(Arrays.equals(data, decodedData));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void test_remove() {
		try {
			final String[] result1 = Utilities.remove(new String[]{"a", "b", "c"}, "b");
			assertTrue(Utilities.compare(new String[]{"a", "c"}, result1));

			final String[] result2 = Utilities.remove(new String[]{"a", "b", "c"}, "d");
			assertTrue(Utilities.compare(new String[]{"a", "b", "c"}, result2));

			final String[] result3 = Utilities.remove(new String[]{"a", "b", null, "c"}, null);
			assertTrue(Utilities.compare(new String[]{"a", "b", "c"}, result3));

			final String[] result4 = Utilities.remove(new String[]{"a", "b", "c", "b"}, "b");
			assertTrue(Utilities.compare(new String[]{"a", "c", "b"}, result4));

			final String[] result5 = Utilities.remove(new String[]{"a", "b", "c"}, "a");
			assertTrue(Utilities.compare(new String[]{"b", "c"}, result5));

			final String[] result6 = Utilities.remove(new String[]{"a", "b", "c"}, "c");
			assertTrue(Utilities.compare(new String[]{"a", "b"}, result6));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void test_copy() {
		try {
			final InputStream in = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
			final OutputStream out = new ByteArrayOutputStream();
			assertEquals(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8).length, IoUtilities.copy(in, out));
			assertEquals(TextUtilities.GERMAN_TEST_STRING, new String(((ByteArrayOutputStream) out).toByteArray(), StandardCharsets.UTF_8));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void test_rightPad() {
		try {
			assertEquals("  ", Utilities.rightPad("", 2));
			assertEquals("a ", Utilities.rightPad("a", 2));
			assertEquals("ab", Utilities.rightPad("ab", 2));
			assertEquals("abc", Utilities.rightPad("abc", 2));
			assertEquals("abc", Utilities.rightPad("abc", 0));
			assertEquals("", Utilities.rightPad("", 0));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void test_leftPad() {
		try {
			assertEquals("  ", Utilities.leftPad("", 2));
			assertEquals(" a", Utilities.leftPad("a", 2));
			assertEquals("ab", Utilities.leftPad("ab", 2));
			assertEquals("abc", Utilities.leftPad("abc", 2));
			assertEquals("abc", Utilities.leftPad("abc", 0));
			assertEquals("", Utilities.leftPad("", 0));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable1() {
		try {
			final String result = Utilities.getHumanReadableNumber(0.1, null, true, 5, false, Locale.GERMAN);
			assertEquals("0,1", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable8() {
		try {
			final String result = Utilities.getHumanReadableNumber(1.0, null, true, 5, false, Locale.GERMAN);
			assertEquals("1,0", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable9() {
		try {
			final String result = Utilities.getHumanReadableNumber(1, "Unit", true, 5, false, Locale.ENGLISH);
			assertEquals("1 Unit", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable10() {
		try {
			final String result = Utilities.getHumanReadableNumber(10000, "Unit", true, 5, false, Locale.GERMAN);
			assertEquals("10,0 kUnit", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable11() {
		try {
			// 10000 / 1024 = 9.765625, rounded to 4 significant digits
			final String result = Utilities.getHumanReadableNumber(10000, "Unit", false, 4, false, Locale.GERMAN);
			assertEquals("9,766 KiUnit", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable2() {
		try {
			final String result = Utilities.getHumanReadableNumber(1, null, true, 5, false, Locale.ENGLISH);
			assertEquals("1", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable3() {
		try {
			final String result = Utilities.getHumanReadableNumber(10, null, true, 5, false, Locale.ENGLISH);
			assertEquals("10", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable4() {
		try {
			final String result = Utilities.getHumanReadableNumber(100, null, true, 5, false, Locale.ENGLISH);
			assertEquals("100", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable5() {
		try {
			final String result = Utilities.getHumanReadableNumber(1000, null, true, 5, false, Locale.GERMAN);
			assertEquals("1,0 k", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable6() {
		try {
			final String result = Utilities.getHumanReadableNumber(10000, null, true, 5, false, Locale.GERMAN);
			assertEquals("10,0 k", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable7() {
		try {
			final String result = Utilities.getHumanReadableNumber(100000, null, true, 5, false, Locale.GERMAN);
			assertEquals("100,0 k", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable12() {
		try {
			// 5 significant digits with keepTrailingZeros = true pads 1.116 to 1.1160
			final String result = Utilities.getHumanReadableNumber(1116, null, true, 5, true, Locale.GERMAN);
			assertEquals("1,1160 k", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadableRoundingIntoNextSiPrefix() {
		try {
			// Rounding of 999.96 to 4 significant digits carries over into the next unit prefix
			final String result = Utilities.getHumanReadableNumber(999.96, null, true, 4, false, Locale.GERMAN);
			assertEquals("1,0 k", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadableRoundingIntoNextBinaryPrefix() {
		try {
			// 1048575 B = 1023.999 KiB, rounding carries over into the next binary unit prefix
			final String result = Utilities.getHumanReadableNumber(1048575, "B", false, 4, false, Locale.GERMAN);
			assertEquals("1,0 MiB", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadableRoundingIntoNextDecade() {
		try {
			// 9.9996 rounds to 10, so only 2 decimals remain for 4 significant digits
			final String result = Utilities.getHumanReadableNumber(9.9996, null, true, 4, true, Locale.GERMAN);
			assertEquals("10,00", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadableNoTrailingDecimalSeparator() {
		try {
			// No decimal digits left for 4 significant digits, so no trailing decimal separator
			final String result = Utilities.getHumanReadableNumber(1000.0, "B", false, 4, true, Locale.GERMAN);
			assertEquals("1000 B", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadableLargestUnitPrefix() {
		try {
			// "E" is the largest unit prefix, larger values must not fail
			final String result = Utilities.getHumanReadableNumber(1e21, null, true, 5, false, Locale.GERMAN);
			assertEquals("1000,0 E", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadableSpeed() {
		try {
			final LocalDateTime startTime = LocalDateTime.now();
			final LocalDateTime endTime = startTime.plusSeconds(1);
			final String result = Utilities.getHumanReadableSpeed(startTime, endTime, 1216, null, true, Locale.GERMAN);
			assertEquals("1,216 k/s", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testVersion() {
		try {
			final Version testVersion = new Version("03.02.01");
			assertEquals(2, testVersion.getMinorVersionNumber());
			final Version testVersion2 = new Version("3.2.1");
			assertEquals(1, testVersion2.getMicroVersionNumber());
		} catch (final Exception e) {
			fail(e.getMessage());
		}

		try {
			final Version versionToFail = new Version("03.x02.01");
			versionToFail.toString();
			fail("Missing error on invalid version string");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected exception
		}

		try {
			final Version testVersion3 = Version.getFirstFullVersionNumberFromText("Testtext 3.2.1 Texttest");
			assertEquals(3, testVersion3.getMajorVersionNumber());
			assertEquals(2, testVersion3.getMinorVersionNumber());
			assertEquals(1, testVersion3.getMicroVersionNumber());
			assertEquals("3.2.1", testVersion3.toString());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testParseMapStringWithObjectValue() {
		try {
			final Map<String, Object> map = new HashMap<>();

			map.put("abc122", null);
			map.put("abc123", 123);
			map.put("abc 124", "abc 124");
			map.put("aBc\"125", "aBc\"125");

			final String parsedMapString = MapUtilities.mapToString(map);
			assertEquals(
					"abc122=null"
							+ ",abc123=123"
							+ ",\"aBc\\\"125\"=\"aBc\\\"125\""
							+ ",abc 124=abc 124",
							parsedMapString);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testParseMapString() {
		try {
			final String mapString =
					"abc122 = 122"
							+ ",abc123=123"
							+ ",\"abc 124\"=\"abc 124\""
							+ ",\"aBc\\\"125\"=\"aBc\\\"125\""
							+ ", abc 126 = abc 12 6 ";
			final Map<String, String> map = MapStringReader.readMap(mapString);

			assertEquals("122", map.get("abc122"));
			assertEquals("123", map.get("abc123"));
			assertEquals("abc 124", map.get("abc 124"));
			assertEquals("aBc\"125", map.get("aBc\"125"), "Actual map was: " + map.toString());

			final String parsedMapString = MapUtilities.mapToString(map);
			assertEquals(
					"abc122=122"
							+ ",abc123=123"
							+ ",abc 124=abc 124"
							+ ",\"aBc\\\"125\"=\"aBc\\\"125\""
							+ ",abc 126=abc 12 6",
							parsedMapString,
							"Actual map was: " + map.toString());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testParseMapStringWithBlankSeparator() {
		try {
			final String mapString =
					"abc122=122"
							+ " abc123=123"
							+ " \"abc 124\"=\"abc 124\""
							+ " \"aBc\\\"125\"=\"aBc\\\"125\"";
			final Map<String, String> map = MapStringReader.readMapWithBlankAsSeparator(mapString);

			assertEquals("122", map.get("abc122"), "Actual map was: " + map.toString());
			assertEquals("123", map.get("abc123"));
			assertEquals("abc 124", map.get("abc 124"));
			assertEquals("aBc\"125", map.get("aBc\"125"));

			final String parsedMapString = MapUtilities.mapToString(map, '"', false, ' ');
			assertEquals(
					"abc122=122"
							+ " abc123=123"
							+ " \"abc 124\"=\"abc 124\""
							+ " \"aBc\\\"125\"=\"aBc\\\"125\"",
							parsedMapString);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testParseMapStringBlankSeparated() {
		try {
			final String mapString =
					"abc123=123"
							+ " \"abc 124\"=\"abc 124\""
							+ " \"aBc\\\"125\"=\"aBc\\\"125\"";
			final Map<String, String> map = MapStringReader.readMapWithBlankAsSeparator(mapString);

			assertEquals("123", map.get("abc123"), "Actual map was: " + map.toString());
			assertEquals("abc 124", map.get("abc 124"));
			assertEquals("aBc\"125", map.get("aBc\"125"));

			final String parsedMapString = MapUtilities.mapToString(map, '\"', false, ' ');
			assertEquals(
					"abc123=123"
							+ " \"abc 124\"=\"abc 124\""
							+ " \"aBc\\\"125\"=\"aBc\\\"125\"",
							parsedMapString);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testParseMapStringQuoted() {
		try {
			final String mapString =
					"\"abc123\"=\"123\""
							+ ",\"abc 124\"=\"abc 124\""
							+ ",\"aBc\\\"125\"=\"aBc\\\"125\"";
			final Map<String, String> map = MapStringReader.readMap(mapString);

			assertEquals("123", map.get("abc123"));
			assertEquals("abc 124", map.get("abc 124"));
			assertEquals("aBc\"125", map.get("aBc\"125"), "Actual map was: " + map.toString());

			final String parsedMapString = MapUtilities.mapToString(map, '\"', true, ',');
			assertEquals(
					"\"abc123\"=\"123\""
							+ ",\"abc 124\"=\"abc 124\""
							+ ",\"aBc\\\"125\"=\"aBc\\\"125\"",
							parsedMapString);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testIntToByteArrayConversion() {
		try {
			final int testInt1 = 123456789;
			final byte[] testArray1 = Utilities.convertIntToByteArray(testInt1);
			final int testResult1 = Utilities.convertByteArrayToInt(testArray1);
			assertEquals(testInt1, testResult1);

			final int testInt2 = 5;
			final byte[] testArray2 = Utilities.convertIntToByteArray(testInt2);
			final int testResult2 = Utilities.convertByteArrayToInt(testArray2);
			assertEquals(testInt2, testResult2);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testReverseArray() {
		try {
			final int[] testArrayOddLength = new int[] { 0,1,2,3,4 };
			Utilities.reverseArray(testArrayOddLength);
			assertArrayEquals(new int[] { 4,3,2,1,0 }, testArrayOddLength);

			final int[] testArrayEvenLength = new int[] { 0,1,2,3,4, 5 };
			Utilities.reverseArray(testArrayEvenLength);
			assertArrayEquals(new int[] { 5, 4,3,2,1,0 }, testArrayEvenLength);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void generateUUID_formatAndRoundTrip() {
		final String uuidString = Utilities.generateUUID();

		assertTrue(uuidString.matches("[0-9A-F]{32}"), uuidString);
		final UUID uuid = Utilities.getUUIDFromString(uuidString);
		assertEquals(uuidString, uuid.toString().replace("-", "").toUpperCase(Locale.ROOT));
	}

	@Test
	public void getResourceAsStream() throws IOException {
		try (InputStream inputStream = Utilities.getResourceAsStream("java/lang/Object.class")) {
			assertNotNull(inputStream);
		}
		assertNull(Utilities.getResourceAsStream("does/not/exist.txt"));
	}

	@Test
	public void gzip_emptyData() throws Exception {
		assertArrayEquals(new byte[0], Utilities.gunzipByteArray(Utilities.gzipByteArray(new byte[0])));
	}

	@Test
	public void gunzipByteArray_invalidData() throws Exception {
		assertNull(Utilities.gunzipByteArray("no gzip data".getBytes(StandardCharsets.US_ASCII)));
	}

	@Test
	public void encodeBase64_lineLength() {
		final byte[] data = { 0, 0, 0, 0 };

		// "AAAAAA==" has exactly two lines of 4 characters, no trailing separator
		assertEquals("AAAA\r\nAA==", Utilities.encodeBase64(data, 4, "\r\n"));
		assertEquals("AAAAAA==", Utilities.encodeBase64(data, 4, null));
		assertEquals("AAAAAA==", Utilities.encodeBase64(data, 100, "\n"));
		assertEquals("", Utilities.encodeBase64(new byte[0], 4, "\n"));
		assertThrows(IllegalArgumentException.class, () -> Utilities.encodeBase64(data, 0, "\n"));
	}

	@Test
	public void decodeBase64_ignoresWhitespace() {
		assertEquals("ABC", new String(Utilities.decodeBase64("QU\r\n J\tD"), StandardCharsets.US_ASCII));
		assertThrows(IllegalArgumentException.class, () -> Utilities.decodeBase64("QU*D"));
	}

	@Test
	public void convertArrayListOfStringsToStringArray() {
		assertArrayEquals(new String[] { "a", "b" }, Utilities.convertArrayListOfStringsToStringArray(new ArrayList<>(List.of("a", "b"))));
		assertArrayEquals(new String[0], Utilities.convertArrayListOfStringsToStringArray(new ArrayList<>()));
	}

	@Test
	public void getIndex() {
		final int[] array = { 5, 7, 5 };

		assertEquals(0, Utilities.getIndex(5, array));
		assertEquals(1, Utilities.getIndex(7, array));
		assertEquals(-1, Utilities.getIndex(9, array));
		assertEquals(-1, Utilities.getIndex(1, new int[0]));
	}

	@Test
	public void readStreamUntilEndOrLinefeed() throws IOException {
		final InputStream inputStream = new ByteArrayInputStream("ab\ncd".getBytes(StandardCharsets.US_ASCII));

		assertEquals("ab\n", new String(Utilities.readStreamUntilEndOrLinefeed(inputStream), StandardCharsets.US_ASCII));
		assertEquals("cd", new String(Utilities.readStreamUntilEndOrLinefeed(inputStream), StandardCharsets.US_ASCII));
		assertEquals("", new String(Utilities.readStreamUntilEndOrLinefeed(inputStream), StandardCharsets.US_ASCII));
	}

	@Test
	public void getAttributesFromCertificate() throws Exception {
		final X509Certificate certificate = readTestCertificate();

		assertEquals("Test User", Utilities.getCnFromCertificate(certificate));
		assertEquals("test.user@example.com", Utilities.getEmailFromCertificate(certificate));
	}

	@Test
	public void chopListToChunks() {
		assertEquals(List.of(List.of(1, 2), List.of(3, 4), List.of(5)), Utilities.chopListToChunks(List.of(1, 2, 3, 4, 5), 2));
		assertEquals(List.of(List.of(1, 2), List.of(3, 4)), Utilities.chopListToChunks(List.of(1, 2, 3, 4), 2));
		assertEquals(List.of(List.of(1, 2)), Utilities.chopListToChunks(List.of(1, 2), 5));
		assertNull(Utilities.chopListToChunks(null, 2));
		assertNull(Utilities.chopListToChunks(new ArrayList<Integer>(), 2));
		assertNull(Utilities.chopListToChunks(List.of(1), 0));
	}

	@Test
	public void randomValues() {
		for (int i = 0; i < 100; i++) {
			final int number = Utilities.getRandomNumber(3);
			assertTrue(number >= 0 && number < 3, Integer.toString(number));
		}

		final String randomString = Utilities.getRandomString(50);
		assertEquals(50, randomString.length());
		assertTrue(randomString.matches("[A-Za-zÄÖÜäöüß]*"), randomString);

		final String alphanumericString = Utilities.getRandomAlphanumericString(50);
		assertEquals(50, alphanumericString.length());
		assertTrue(alphanumericString.matches("[A-Za-zÄÖÜäöüß0-9]*"), alphanumericString);

		final String numberString = Utilities.getRandomNumberString(50);
		assertEquals(50, numberString.length());
		assertTrue(numberString.matches("[0-9]*"), numberString);

		assertEquals("", Utilities.getRandomString(0));

		final byte[] array = new byte[32];
		assertSame(array, Utilities.getRandomByteArray(array));

		// Only checks that no exception is thrown, every byte value is possible
		Utilities.getRandomByte();
	}

	@Test
	public void checkForIntervalContainment() {
		final String intervals = "1; 3 - 5;;10+";

		assertTrue(Utilities.checkForIntervalContainment(intervals, 1));
		assertFalse(Utilities.checkForIntervalContainment(intervals, 2));
		assertTrue(Utilities.checkForIntervalContainment(intervals, 3));
		assertTrue(Utilities.checkForIntervalContainment(intervals, 5));
		assertFalse(Utilities.checkForIntervalContainment(intervals, 6));
		assertTrue(Utilities.checkForIntervalContainment(intervals, 10));
		assertTrue(Utilities.checkForIntervalContainment(intervals, 1000));
		assertTrue(Utilities.checkForIntervalContainment("-3", 2));
		assertFalse(Utilities.checkForIntervalContainment("-3", 4));
		assertFalse(Utilities.checkForIntervalContainment(null, 1));
		assertFalse(Utilities.checkForIntervalContainment("", 1));
		assertThrows(NumberFormatException.class, () -> Utilities.checkForIntervalContainment("abc", 1));
	}

	@Test
	public void getMinimumOfAllowedValues() {
		assertEquals(6, Utilities.getMinimumOfAllowedValues(5, 3, 7, 6));
		assertEquals(5, Utilities.getMinimumOfAllowedValues(5, 5, 7));
		assertEquals(Integer.MAX_VALUE, Utilities.getMinimumOfAllowedValues(10, 3));
		assertEquals(Integer.MAX_VALUE, Utilities.getMinimumOfAllowedValues(10, (int[]) null));
	}

	@Test
	public void interpretAsBool() {
		for (final String value : new String[] { "true", "TRUE", "+", "yes", "Y", "ja", "J", "ok", "on", "AN", " true " }) {
			assertTrue(Utilities.interpretAsBool(value), value);
		}
		for (final String value : new String[] { "false", "no", "0", "1", "", " ", "x" }) {
			assertFalse(Utilities.interpretAsBool(value), value);
		}
		assertFalse(Utilities.interpretAsBool(null));
	}

	@Test
	public void charComparisons() {
		assertTrue(Utilities.anyCharsAreEqual('a', 'b', 'a'));
		assertFalse(Utilities.anyCharsAreEqual('a', 'b', 'c'));
		assertFalse(Utilities.anyCharsAreEqual());

		assertTrue(Utilities.contains(new char[] { 'a', 'b' }, 'b'));
		assertFalse(Utilities.contains(new char[] { 'a', 'b' }, 'c'));
		assertFalse(Utilities.contains(null, 'a'));
		assertFalse(Utilities.contains(new char[] { 'a' }, (Character) null));

		assertTrue(Utilities.equalsAnyChar('b', 'a', 'b'));
		assertFalse(Utilities.equalsAnyChar('c', 'a', 'b'));
		assertFalse(Utilities.equalsAnyChar('c'));
	}

	@Test
	public void squareAndPow() {
		assertEquals(9, Utilities.square(-3));
		assertEquals(1024, Utilities.pow(2, 10));
		assertEquals(1, Utilities.pow(3, 0));
		assertEquals(-8, Utilities.pow(-2, 3));
		assertEquals(243, Utilities.pow(3, 5));
		assertThrows(IllegalArgumentException.class, () -> Utilities.pow(2, -1));
	}

	@Test
	public void asSortedList() {
		assertEquals(List.of("a", "b", "c"), Utilities.asSortedList(Set.of("c", "a", "b")));
	}

	@Test
	public void sortButPutItemsFirst() {
		assertEquals(List.of("x", "c", "a", "b", "d"), Utilities.sortButPutItemsFirst(List.of("d", "b", "a", "c", "x"), "x", "c"));
		// First items not contained in the collection are ignored
		assertEquals(List.of("b", "a"), Utilities.sortButPutItemsFirst(List.of("a", "b"), "z", "b"));
		assertEquals(List.of("a", "b"), Utilities.sortButPutItemsFirst(List.of("b", "a")));
		assertEquals(List.of("a", "a", "b"), Utilities.sortButPutItemsFirst(List.of("b", "a", "a"), "a"));
	}

	@Test
	public void sortMap() {
		final Map<String, Integer> map = new LinkedHashMap<>();
		map.put("b", 2);
		map.put("c", 3);
		map.put("a", 1);

		assertEquals(List.of("a", "b", "c"), new ArrayList<>(Utilities.sortMap(map).keySet()));
		assertEquals(List.of("c", "b", "a"), new ArrayList<>(Utilities.sortMap(map, Comparator.reverseOrder()).keySet()));
		assertEquals(Integer.valueOf(3), Utilities.sortMap(map).get("c"));
	}

	@Test
	public void systemValues() {
		assertEquals(System.getProperty("java.class.path"), Utilities.getClassPath());
		assertEquals(System.getProperty("java.io.tmpdir"), Utilities.getSystemTempDir());

		final Map<String, String> propertiesMap = Utilities.getSystemPropertiesMap();
		assertEquals(System.getProperty("java.version"), propertiesMap.get("java.version"));
		if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("linux")) {
			assertNotNull(propertiesMap.get("os.distribution.name"));
			assertNotNull(propertiesMap.get("os.distribution.version"));
		}
	}

	@Test
	public void compare() {
		assertTrue(Utilities.compare(new String[] { "a", null }, new String[] { "a", null }));
		assertFalse(Utilities.compare(new String[] { "a" }, new String[] { "b" }));
		assertTrue(Utilities.compare((String[]) null, (String[]) null));
		assertFalse(Utilities.compare(new String[0], (String[]) null));
	}

	@Test
	public void getStringFromMap() {
		final Map<String, Object> map = new LinkedHashMap<>();
		map.put("b", 2);
		map.put("a", null);
		map.put(null, "x");

		assertEquals("b=2;a=;=x", Utilities.getStringFromMap(map, ";", "=", false));
		assertEquals("=x;a=;b=2", Utilities.getStringFromMap(map, ";", "=", true));
		assertEquals("", Utilities.getStringFromMap(new LinkedHashMap<>(), ";", "=", true));
	}

	@Test
	public void getHumanReadableNumber_negativeAndUnits() {
		assertEquals("-1,5 k", Utilities.getHumanReadableNumber(-1500, null, true, 5, false, Locale.GERMAN));
		assertEquals("-1,5 kB", Utilities.getHumanReadableNumber(-1500.0, "B", true, 5, false, Locale.GERMAN));
		assertEquals("2,0 KiB", Utilities.getHumanReadableNumber(2048, "B", false, 5, false, Locale.GERMAN));
		assertEquals("1.5 MB", Utilities.getHumanReadableNumber(1_500_000L, "B", true, 5, false, Locale.ENGLISH));
		assertEquals("999 B", Utilities.getHumanReadableNumber(999L, "B", true, 5, false, Locale.ENGLISH));
		assertEquals("5", Utilities.getHumanReadableNumber(BigInteger.valueOf(5), " ", true, 5, false, Locale.ENGLISH));
		assertEquals("0.25", Utilities.getHumanReadableNumber(0.25, null, true, 5, false, Locale.ENGLISH));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void parseHumanReadable() {
		assertEquals(1500.0, Utilities.parseHumanReadableDouble("1,5 k", null, true, Locale.GERMAN), 0.0);
		assertEquals(2048.0, Utilities.parseHumanReadableDouble("2 KiB", "B", false, Locale.ENGLISH), 0.0);
		assertEquals(2.0 * 1024 * 1024, Utilities.parseHumanReadableDouble("2 Mi", null, false, Locale.ENGLISH), 0.0);
		assertEquals(1_500_000.0, Utilities.parseHumanReadableDouble("1.5M", null, true, Locale.ENGLISH), 0.0);
		assertEquals(1000.5, Utilities.parseHumanReadableDouble("1,000.5", null, true, Locale.ENGLISH), 0.0);
		assertEquals(3.0 * 1024 * 1024 * 1024, Utilities.parseHumanReadableDouble("3 G", null, false, Locale.ENGLISH), 0.0);
		assertEquals(1e12, Utilities.parseHumanReadableDouble("1T", null, true, Locale.ENGLISH), 0.0);
		assertEquals(1e15, Utilities.parseHumanReadableDouble("1P", null, true, Locale.ENGLISH), 0.0);
		assertEquals(1e18, Utilities.parseHumanReadableDouble("1E", null, true, Locale.ENGLISH), 0.0);
		assertEquals(42.0, Utilities.parseHumanReadableDouble("42", null, true, Locale.ENGLISH), 0.0);
		assertEquals(1000.0f, Utilities.parseHumanReadableNumber("1k", null, true, Locale.ENGLISH), 0.0f);
		assertThrows(RuntimeException.class, () -> Utilities.parseHumanReadableDouble(" ", null, true, Locale.ENGLISH));
		assertThrows(NumberFormatException.class, () -> Utilities.parseHumanReadableDouble("abc", null, true, Locale.ENGLISH));
	}

	@Test
	public void parseHumanReadable_roundTrip() {
		for (final long value : new long[] { 0, 999, 1500, 2_500_000, 7_000_000_000L }) {
			final String text = Utilities.getHumanReadableNumber((double) value, "B", true, 5, false, Locale.GERMAN);
			assertEquals(value, Utilities.parseHumanReadableDouble(text, "B", true, Locale.GERMAN), 0.0, text);
		}
	}

	@Test
	public void getHumanReadableSpeed_variants() {
		final LocalDateTime start = LocalDateTime.of(2026, 1, 1, 0, 0);

		assertEquals("Done in <1 ms", Utilities.getHumanReadableSpeed(start, start, 10, "B", true, Locale.ENGLISH));
		assertEquals("500.0 B/s", Utilities.getHumanReadableSpeed(start, start.plusSeconds(1), 500, "B", true, Locale.ENGLISH));
		assertEquals("0.5/s", Utilities.getHumanReadableSpeed(start, start.plusSeconds(2), 1, null, true, Locale.ENGLISH));
		assertEquals("2.0 KiB/s", Utilities.getHumanReadableSpeed(start, start.plusSeconds(1), 2048, "B", false, Locale.ENGLISH));
		// Rounding of 999.999999 k carries over into the next unit prefix
		assertEquals("1.0 M/s", Utilities.getHumanReadableSpeed(start, start.plusSeconds(1000), 999_999_999, null, true, Locale.ENGLISH));
		assertThrows(IllegalArgumentException.class, () -> Utilities.getHumanReadableSpeed(start.plusSeconds(1), start, 1, null, true, Locale.ENGLISH));
	}

	@Test
	public void getHumanReadableInteger() {
		assertEquals("1.234.567 B", Utilities.getHumanReadableInteger(1234567L, "B", Locale.GERMAN));
		assertEquals("9,223,372,036,854,775,807", Utilities.getHumanReadableInteger(Long.MAX_VALUE, null, Locale.ENGLISH));
		assertEquals("-5", Utilities.getHumanReadableInteger(-5L, " ", Locale.ENGLISH));
		assertEquals("0", Utilities.getHumanReadableInteger(0L, null, Locale.ENGLISH));
	}

	@Test
	public void hashes() throws Exception {
		// Test vectors of FIPS 180 and RFC 1321 for "abc"
		assertEquals("900150983cd24fb0d6963f7d28e17f72", HexFormat.of().formatHex(Utilities.getMD5Hash("abc")));
		assertEquals("a9993e364706816aba3e25717850c26c9cd0d89d", HexFormat.of().formatHex(Utilities.getSHA1Hash("abc")));
		assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", HexFormat.of().formatHex(Utilities.getSHA256Hash("abc")));
		assertEquals("cb00753f45a35e8bb5a03d699ac65007272c32ab0eded1631a8b605a43ff5bed8086072ba1e7cc2358baeca134c825a7", HexFormat.of().formatHex(Utilities.getSHA384Hash("abc")));
		assertEquals("ddaf35a193617abacc417349ae20413112e6fa4e89a97ea20a9eeee64b55d39a2192992a274fc1a836ba3c23a3feebbd454d4423643ce80e2a9ac94fa54ca49f", HexFormat.of().formatHex(Utilities.getSHA512Hash("abc")));
	}

	@Test
	public void getByteArray() {
		assertArrayEquals(new byte[] { 1, -1, 0 }, Utilities.getByteArray(List.of((byte) 1, (byte) -1, (byte) 0)));
		assertArrayEquals(new byte[0], Utilities.getByteArray(new ArrayList<>()));
	}

	@Test
	public void stacktraceToString() {
		final StackTraceElement[] stackTrace = {
				new StackTraceElement("de.Example", "first", "Example.java", 10),
				new StackTraceElement("de.Example", "second", "Example.java", 20) };

		assertEquals("de.Example.first(Example.java:10)\nde.Example.second(Example.java:20)\n", Utilities.stacktraceToString(stackTrace));
		assertEquals("", Utilities.stacktraceToString(null));
	}

	@Test
	public void downloadFile_fromFileUrl() throws Exception {
		final File sourceFile = tempDir.resolve("source.txt").toFile();
		Files.writeString(sourceFile.toPath(), "file content");
		final File destinationFile = tempDir.resolve("sub/destination.txt").toFile();
		Files.createDirectories(destinationFile.getParentFile().toPath());

		Utilities.downloadFile(sourceFile.toURI().toString(), destinationFile.getAbsolutePath());

		assertEquals("file content", Files.readString(destinationFile.toPath()));
		assertEquals(List.of("destination.txt"), Arrays.asList(destinationFile.getParentFile().list()));
	}

	@Test
	public void downloadFile_http() throws Exception {
		final HttpServer server = startTestServer();
		try {
			final String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
			final File destinationFile = tempDir.resolve("download.txt").toFile();

			Utilities.downloadFile(baseUrl + "/data", destinationFile.getAbsolutePath(), null);
			assertEquals("content", Files.readString(destinationFile.toPath()));

			Files.delete(destinationFile.toPath());
			Utilities.downloadFile(baseUrl + "/redirect", destinationFile.getAbsolutePath());
			assertEquals("content", Files.readString(destinationFile.toPath()));
		} finally {
			server.stop(0);
		}
	}

	@Test
	public void downloadFile_httpErrors() throws Exception {
		final HttpServer server = startTestServer();
		try {
			final String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
			final File destinationFile = tempDir.resolve("download.txt").toFile();

			assertThrows(Exception.class, () -> Utilities.downloadFile(baseUrl + "/missing", destinationFile.getAbsolutePath()));
			assertThrows(Exception.class, () -> Utilities.downloadFile(baseUrl + "/noLocation", destinationFile.getAbsolutePath()));
			assertThrows(Exception.class, () -> Utilities.downloadFile(baseUrl + "/loop", destinationFile.getAbsolutePath()));

			// Neither the destination file nor temporary files remain
			assertEquals(0, tempDir.toFile().list().length);
		} finally {
			server.stop(0);
		}
	}

	@Test
	public void downloadFile_keepsExistingFileOnError() throws Exception {
		final HttpServer server = startTestServer();
		try {
			final File destinationFile = tempDir.resolve("existing.txt").toFile();
			Files.writeString(destinationFile.toPath(), "old");

			assertThrows(Exception.class, () -> Utilities.downloadFile("http://127.0.0.1:" + server.getAddress().getPort() + "/missing", destinationFile.getAbsolutePath()));

			assertEquals("old", Files.readString(destinationFile.toPath()));
		} finally {
			server.stop(0);
		}
	}

	@Test
	public void checkForDuplicates() {
		assertTrue(Utilities.checkForDuplicates(new String[] { "a", "b", "a" }, false));
		assertFalse(Utilities.checkForDuplicates(new String[] { "a", "b" }, false));
		assertTrue(Utilities.checkForDuplicates(new String[] { "a", null, null }, false));
		assertFalse(Utilities.checkForDuplicates(new String[] { "a", null, null }, true));
		assertFalse(Utilities.checkForDuplicates(new String[0], true));
	}

	@Test
	public void selectItems() {
		final List<Object> items = List.of("a", 1, "b", 2.0);

		assertEquals(List.of("a", "b"), Utilities.selectItems(items, String.class));
		assertEquals(List.of(1, 2.0), Utilities.selectItems(items, Number.class));
		assertEquals(List.of("a", "b"), Utilities.selectItems(items.toArray(), String.class));
		assertEquals(List.of(), Utilities.selectItems(new Object[] { null }, String.class));
	}

	@Test
	public void revertArray_returnsReversedCopy() {
		final String[] array = { "a", "b", "c" };

		final String[] reversed = Utilities.revertArray(array);

		assertArrayEquals(new String[] { "c", "b", "a" }, reversed);
		assertArrayEquals(new String[] { "a", "b", "c" }, array);
		assertEquals(String[].class, reversed.getClass());
		assertArrayEquals(new String[0], Utilities.revertArray(new String[0]));
	}

	@Test
	public void reverseArray_inPlace() {
		final String[] stringArray = { "a", "b", "c" };
		assertSame(stringArray, Utilities.reverseArray(stringArray));
		assertArrayEquals(new String[] { "c", "b", "a" }, stringArray);

		final byte[] byteArray = { 1, 2, 3, 4 };
		assertSame(byteArray, Utilities.reverseArray(byteArray));
		assertArrayEquals(new byte[] { 4, 3, 2, 1 }, byteArray);

		assertArrayEquals(new byte[0], Utilities.reverseArray(new byte[0]));
	}

	@Test
	public void getDomainFromUrl() throws Exception {
		assertEquals("example.com", Utilities.getDomainFromUrl("https://www.example.com/path?x=1"));
		assertEquals("Example.com", Utilities.getDomainFromUrl("https://WWW.Example.com"));
		assertEquals("sub.example.com", Utilities.getDomainFromUrl("http://sub.example.com:8080/"));
		assertNull(Utilities.getDomainFromUrl("mailto:user@example.com"));
		assertNull(Utilities.getDomainFromUrl("relative/path"));
		assertThrows(Exception.class, () -> Utilities.getDomainFromUrl("http://exa mple.com"));
	}

	@Test
	public void isEmptyCollection() {
		assertTrue(Utilities.isEmpty((List<String>) null));
		assertTrue(Utilities.isEmpty(new ArrayList<>()));
		assertFalse(Utilities.isEmpty(List.of("a")));
		assertFalse(Utilities.isNotEmpty((List<String>) null));
		assertTrue(Utilities.isNotEmpty(List.of("a")));
	}

	@Test
	public void clearArrays() {
		final char[] chars = { 'a', 'b' };
		Utilities.clear(chars);
		assertArrayEquals(new char[] { 0, 0 }, chars);

		final byte[] bytes = { 1, 2 };
		Utilities.clear(bytes);
		assertArrayEquals(new byte[] { 0, 0 }, bytes);

		Utilities.clear((char[]) null);
		Utilities.clear((byte[]) null);
	}

	@Test
	public void closeQuietly_closeable() {
		final boolean[] closed = { false };
		Utilities.closeQuietly(() -> closed[0] = true);
		assertTrue(closed[0]);

		Utilities.closeQuietly((Closeable) () -> {
			throw new IOException("Expected test exception");
		});
		Utilities.closeQuietly((Closeable) null);
	}

	@Test
	public void closeQuietly_xml() throws Exception {
		final ClosingStateInputStream inputStream = new ClosingStateInputStream("<root/>".getBytes(StandardCharsets.UTF_8));
		final XMLStreamReader xmlReader = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
		Utilities.closeQuietly(xmlReader, inputStream);
		assertTrue(inputStream.closed);

		Utilities.closeQuietly((XMLStreamReader) null, new InputStream() {
			@Override
			public int read() {
				return -1;
			}

			@Override
			public void close() throws IOException {
				throw new IOException("Expected test exception");
			}
		});
		Utilities.closeQuietly((XMLStreamReader) null, null);

		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		final XMLStreamWriter xmlWriter = XMLOutputFactory.newInstance().createXMLStreamWriter(outputStream, "UTF-8");
		xmlWriter.writeEmptyElement("root");
		Utilities.closeQuietly(xmlWriter);
		Utilities.closeQuietly((XMLStreamWriter) null);
	}

	@Test
	public void remove_keepsComponentType() {
		assertNull(Utilities.remove((String[]) null, "a"));

		final String[] result = Utilities.remove(new String[] { "a", "b" }, "a");
		assertEquals(String[].class, result.getClass());
		assertArrayEquals(new String[] { "b" }, result);

		// Unchanged content is returned as a copy
		final String[] array = { "a" };
		final String[] unchanged = Utilities.remove(array, "x");
		assertArrayEquals(array, unchanged);
		assertFalse(array == unchanged);
	}

	@Test
	public void removeItemAtIndex() {
		final String[] array = { "a", "b", "c" };

		assertArrayEquals(new String[] { "b", "c" }, Utilities.removeItemAtIndex(array, 0));
		assertArrayEquals(new String[] { "a", "c" }, Utilities.removeItemAtIndex(array, 1));
		assertArrayEquals(new String[] { "a", "b" }, Utilities.removeItemAtIndex(array, 2));
		assertArrayEquals(new String[] { "a", "b", "c" }, array);
	}

	@Test
	public void pad_withPadCharAndNull() {
		assertEquals("00042", Utilities.leftPad("42", 5, '0'));
		assertEquals("42...", Utilities.rightPad("42", 5, '.'));
		assertEquals("42", Utilities.leftPad("42", -1));
		assertEquals("42", Utilities.rightPad("42", -1));
		assertNull(Utilities.leftPad(null, 5));
		assertNull(Utilities.rightPad(null, 5));
	}

	@Test
	public void trimSimultaneously() {
		assertEquals("abc", Utilities.trimSimultaneously("\"abc\"", "\""));
		assertEquals("", Utilities.trimSimultaneously("\"\"", "\""));
		assertEquals("\"", Utilities.trimSimultaneously("\"", "\""));
		assertEquals("\"abc", Utilities.trimSimultaneously("\"abc", "\""));
		assertEquals("abc", Utilities.trimSimultaneously("--abc--", "--"));
		assertEquals("abc", Utilities.trimSimultaneously("abc", ""));
		assertEquals("abc", Utilities.trimSimultaneously("abc", null));
		assertNull(Utilities.trimSimultaneously(null, "\""));
	}

	@Test
	public void readStreams() throws IOException {
		assertEquals("Grüße", Utilities.toString(new ByteArrayInputStream("Grüße".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
		assertEquals("Reader text", Utilities.toString(new StringReader("Reader text")));
		assertEquals(List.of("a", "b", "", "c"), Utilities.readLines(new ByteArrayInputStream("a\r\nb\n\nc\n".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
	}

	@Test
	public void containsIgnoreCase() {
		final List<String> list = Arrays.asList("Alpha", null);

		assertTrue(Utilities.containsIgnoreCase(list, "ALPHA"));
		assertTrue(Utilities.containsIgnoreCase(list, null));
		assertFalse(Utilities.containsIgnoreCase(list, "beta"));
		assertFalse(Utilities.containsIgnoreCase(List.of("a"), null));
		assertFalse(Utilities.containsIgnoreCase(null, "a"));
	}

	@Test
	public void limitValue() {
		assertEquals(5, Utilities.limitValue(5, 1, 10));
		assertEquals(10, Utilities.limitValue(5, 11, 10));
		assertEquals(7, Utilities.limitValue(5, 7, 10));
	}

	@Test
	public void createMap() throws Exception {
		final Map<String, String> map = Utilities.createMap("a", "1", "b", "2");

		assertEquals(2, map.size());
		assertEquals("1", map.get("a"));
		assertEquals("2", map.get("b"));
		assertTrue(Utilities.createMap().isEmpty());
		assertTrue(Utilities.createMap((String[]) null).isEmpty());
		assertThrows(Exception.class, () -> Utilities.createMap("a", "1", "b"));
	}

	@Test
	public void containsObject_byIdentity() {
		final String item = new String("a");
		final List<String> list = List.of(item);

		assertTrue(Utilities.containsObject(list, item));
		assertFalse(Utilities.containsObject(list, new String("a")));
	}

	@Test
	public void replaceUsersHome() {
		final String originalHome = System.getProperty("user.home");
		try {
			System.setProperty("user.home", "/home/tester");

			assertEquals("/home/tester", Utilities.replaceUsersHome("~"));
			assertEquals("/home/tester/data", Utilities.replaceUsersHome("~/data"));
			assertEquals("/home/tester/data", Utilities.replaceUsersHome("$HOME/data"));
			assertEquals("/home/tester/data", Utilities.replaceUsersHome("${HOME}/data"));
			// "~" within a path and other variables starting with "HOME" are kept
			assertEquals("/data/~backup", Utilities.replaceUsersHome("/data/~backup"));
			assertEquals("~user/data", Utilities.replaceUsersHome("~user/data"));
			assertEquals("$HOMEPATH/data", Utilities.replaceUsersHome("$HOMEPATH/data"));
			assertNull(Utilities.replaceUsersHome(null));

			// The home path is used literally, also if it contains special characters of regular expressions
			System.setProperty("user.home", "/home/a$1\\b");
			assertEquals("/home/a$1\\b/data", Utilities.replaceUsersHome("$HOME/data"));
		} finally {
			System.setProperty("user.home", originalHome);
		}
	}

	@Test
	public void replaceUsersHomeByTilde() {
		final String originalHome = System.getProperty("user.home");
		try {
			System.setProperty("user.home", "/home/user");

			assertEquals("~", Utilities.replaceUsersHomeByTilde("/home/user"));
			assertEquals("~/data", Utilities.replaceUsersHomeByTilde("/home/user/data"));
			assertEquals("/home/user2/data", Utilities.replaceUsersHomeByTilde("/home/user2/data"));
			assertEquals("/other", Utilities.replaceUsersHomeByTilde("/other"));
			assertNull(Utilities.replaceUsersHomeByTilde(null));
		} finally {
			System.setProperty("user.home", originalHome);
		}
	}

	@Test
	public void getUsersDefaultDownloadDirectory() throws IOException {
		final String originalHome = System.getProperty("user.home");
		try {
			System.setProperty("user.home", tempDir.toString());
			assertEquals(new File(tempDir.toFile(), "Downloads").getAbsolutePath(), Utilities.getUsersDefaultDownloadDirectory());

			final Path userDirsFile = tempDir.resolve(".config/user-dirs.dirs");
			Files.createDirectories(userDirsFile.getParent());
			Files.writeString(userDirsFile, "# comment\nXDG_DESKTOP_DIR=\"$HOME/Desktop\"\nXDG_DOWNLOAD_DIR=\"$HOME/Herunterladen\"\n");
			assertEquals(tempDir.toString() + "/Herunterladen", Utilities.getUsersDefaultDownloadDirectory());

			Files.writeString(userDirsFile, "XDG_DOWNLOAD_DIR=\"\"\n");
			assertEquals(new File(tempDir.toFile(), "Downloads").getAbsolutePath(), Utilities.getUsersDefaultDownloadDirectory());
		} finally {
			System.setProperty("user.home", originalHome);
		}
	}

	@Test
	public void skipInputStreamData() throws Exception {
		final InputStream inputStream = new ByteArrayInputStream("abcd".getBytes(StandardCharsets.US_ASCII));

		assertEquals(0, Utilities.skipInputStreamData(inputStream, 0));
		assertEquals(0, Utilities.skipInputStreamData(inputStream, -1));
		assertEquals(2, Utilities.skipInputStreamData(inputStream, 2));
		assertEquals('c', inputStream.read());
		assertThrows(Exception.class, () -> Utilities.skipInputStreamData(inputStream, 5));
	}

	@Test
	public void convertByteArrayToInt_variants() throws Exception {
		assertEquals(-1, Utilities.convertByteArrayToInt(Utilities.convertIntToByteArray(-1)));
		assertEquals(Integer.MIN_VALUE, Utilities.convertByteArrayToInt(Utilities.convertIntToByteArray(Integer.MIN_VALUE)));
		assertArrayEquals(new byte[] { 0x12, 0x34, 0x56, 0x78 }, Utilities.convertIntToByteArray(0x12345678));
		assertEquals(256, Utilities.convertByteArrayToInt(new byte[] { 1, 0 }));
		assertEquals(255, Utilities.convertByteArrayToInt(new byte[] { (byte) 0xFF }));
		assertEquals(0, Utilities.convertByteArrayToInt(new byte[0]));
		assertThrows(Exception.class, () -> Utilities.convertByteArrayToInt(new byte[5]));
	}

	@Test
	public void getList() {
		assertEquals(List.of("a", "b"), Utilities.getList("a", "b"));
		assertEquals(List.of(), Utilities.getList());
		// The list is modifiable
		Utilities.getList("a").add("b");
	}

	@Test
	public void convertPkcs12TrustStoreToJKS() throws Exception {
		final X509Certificate certificate = readTestCertificate();
		final File keyStoreFile = tempDir.resolve("truststore.p12").toFile();
		final KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");
		pkcs12KeyStore.load(null, null);
		pkcs12KeyStore.setCertificateEntry("test", certificate);
		try (OutputStream outputStream = new FileOutputStream(keyStoreFile)) {
			pkcs12KeyStore.store(outputStream, "".toCharArray());
		}

		Utilities.convertPkcs12TrustStoreToJKS(keyStoreFile);

		final KeyStore jksKeyStore = KeyStore.getInstance("JKS");
		try (InputStream inputStream = new FileInputStream(keyStoreFile)) {
			jksKeyStore.load(inputStream, "".toCharArray());
		}
		assertEquals("JKS", jksKeyStore.getType());
		assertEquals(certificate, jksKeyStore.getCertificate("test"));
		// No temporary files remain
		assertEquals(List.of("truststore.p12"), Arrays.asList(tempDir.toFile().list()));
	}

	@Test
	public void convertPkcs12TrustStoreToJKS_otherPasswordIsKept() throws Exception {
		final File keyStoreFile = tempDir.resolve("truststore.p12").toFile();
		final KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");
		pkcs12KeyStore.load(null, null);
		pkcs12KeyStore.setCertificateEntry("test", readTestCertificate());
		try (OutputStream outputStream = new FileOutputStream(keyStoreFile)) {
			pkcs12KeyStore.store(outputStream, "secret".toCharArray());
		}
		final byte[] originalData = Files.readAllBytes(keyStoreFile.toPath());

		Utilities.convertPkcs12TrustStoreToJKS(keyStoreFile);

		assertArrayEquals(originalData, Files.readAllBytes(keyStoreFile.toPath()));
	}

	@Test
	public void convertPkcs12TrustStoreToJKS_otherEntriesAreNotLost() throws Exception {
		final File keyStoreFile = tempDir.resolve("keystore.p12").toFile();
		final KeyStore pkcs12KeyStore = KeyStore.getInstance("PKCS12");
		pkcs12KeyStore.load(null, null);
		pkcs12KeyStore.setEntry("secret", new KeyStore.SecretKeyEntry(new SecretKeySpec(new byte[16], "AES")), new KeyStore.PasswordProtection("".toCharArray()));
		try (OutputStream outputStream = new FileOutputStream(keyStoreFile)) {
			pkcs12KeyStore.store(outputStream, "".toCharArray());
		}
		final byte[] originalData = Files.readAllBytes(keyStoreFile.toPath());

		assertThrows(Exception.class, () -> Utilities.convertPkcs12TrustStoreToJKS(keyStoreFile));

		assertArrayEquals(originalData, Files.readAllBytes(keyStoreFile.toPath()));
	}

	@Test
	public void fixStringEncodingIfNeeded() {
		final String original = "Grüße Ä è ° €";
		final String wronglyDecoded = new String(original.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);

		assertEquals(original, Utilities.fixStringEncodingIfNeeded(wronglyDecoded));
		assertEquals(original, Utilities.fixStringEncodingIfNeeded(original));
		assertEquals("Grüße", Utilities.fixStringEncodingIfNeeded("Grüße"));
		assertEquals("\u00c3", Utilities.fixStringEncodingIfNeeded("\u00c3"));
		assertEquals("abc", Utilities.fixStringEncodingIfNeeded("abc"));
		assertNull(Utilities.fixStringEncodingIfNeeded(null));
	}

	@Test
	public void arrayContains_objects() {
		final String[] data = { "a", null, "b", "c" };

		assertTrue(Utilities.arrayContains(data, new String[] { null, "b" }));
		assertTrue(Utilities.arrayContains(data, new String[] { "b", "c" }));
		assertTrue(Utilities.arrayContains(data, new String[0]));
		assertFalse(Utilities.arrayContains(data, new String[] { "c", "d" }));
		assertFalse(Utilities.arrayContains(data, new String[] { "a", "b" }));
		assertFalse(Utilities.arrayContains(new String[] { "a" }, new String[] { "a", "b" }));
		assertFalse(Utilities.arrayContains((String[]) null, new String[0]));
		assertFalse(Utilities.arrayContains(data, (String[]) null));
	}

	@Test
	public void arrayContains_bytes() {
		final byte[] data = { 1, 2, 3, 2, 4 };

		assertTrue(Utilities.arrayContains(data, new byte[] { 2, 4 }));
		assertTrue(Utilities.arrayContains(data, new byte[] { 1 }));
		assertTrue(Utilities.arrayContains(data, new byte[0]));
		assertFalse(Utilities.arrayContains(data, new byte[] { 4, 5 }));
		assertFalse(Utilities.arrayContains(new byte[] { 1 }, new byte[] { 1, 2 }));
		assertFalse(Utilities.arrayContains((byte[]) null, new byte[0]));
		assertFalse(Utilities.arrayContains(data, (byte[]) null));
	}

	@Test
	public void escapeJavaString() throws Exception {
		final String text = "a\\b\"c\n\r\t\b\fé\u0001'z";
		final String escaped = Utilities.escapeJavaString(text);

		assertEquals("a\\\\b\\\"c\\n\\r\\t\\b\\f\\u00E9\\u0001'z", escaped);
		assertEquals(text, Utilities.unescapeJavaString(escaped));
		assertEquals("", Utilities.escapeJavaString(""));
	}

	@Test
	public void escapeJavaString_surrogatePair() throws Exception {
		final String text = "\uD83D\uDE00";

		assertEquals("\\uD83D\\uDE00", Utilities.escapeJavaString(text));
		assertEquals(text, Utilities.unescapeJavaString(Utilities.escapeJavaString(text)));
	}

	@Test
	public void unescapeJavaString() throws Exception {
		assertEquals("A", Utilities.unescapeJavaString("\\x41"));
		assertEquals("é", Utilities.unescapeJavaString("\\u00e9"));
		assertEquals("\uD83D\uDE00", Utilities.unescapeJavaString("\\U0001F600"));
		assertEquals("'\"", Utilities.unescapeJavaString("\\'\\\""));
		// A trailing backslash is kept
		assertEquals("a\\", Utilities.unescapeJavaString("a\\"));
	}

	@Test
	public void unescapeJavaString_invalidSequences() {
		for (final String invalid : new String[] { "\\q", "\\uZZZZ", "\\u12", "\\xZ1", "\\x4", "\\U00110000", "\\UFFFFFFFF", "\\U0001F6", "\\U0001F60G" }) {
			assertThrows(Exception.class, () -> Utilities.unescapeJavaString(invalid), invalid);
		}
	}

	@Test
	public void escapePropertiesString() throws Exception {
		final String text = "  a b\\=:\n\r\t\f\b€";
		final String escaped = Utilities.escapePropertiesString(text);

		assertEquals("\\ \\ a b\\\\=:\\n\\r\\t\\f\\u0008\\u20AC", escaped);
		assertEquals(text, Utilities.unescapePropertiesString(escaped));

		// java.util.Properties reads the escaped value unchanged
		final Properties properties = new Properties();
		properties.load(new StringReader("key=" + escaped));
		assertEquals(text, properties.getProperty("key"));
	}

	@Test
	public void unescapePropertiesString() throws Exception {
		assertEquals("a=b: c", Utilities.unescapePropertiesString("a\\=b\\:\\ c"));
		assertEquals("\b'\"", Utilities.unescapePropertiesString("\\b\\'\\\""));
		assertEquals("A", Utilities.unescapePropertiesString("\\x41"));
		assertEquals("\uD83D\uDE00", Utilities.unescapePropertiesString("\\U0001F600"));
		assertEquals("a\\", Utilities.unescapePropertiesString("a\\"));
		for (final String invalid : new String[] { "\\uZZZZ", "\\u12", "\\xZ1", "\\x4", "\\U00110000", "\\U0001F6", "\\U0001F60G" }) {
			assertThrows(Exception.class, () -> Utilities.unescapePropertiesString(invalid), invalid);
		}
	}

	@Test
	public void getBoxedString() {
		assertEquals("*******\n* abc *\n*******", Utilities.getBoxedString("abc", '*'));
		assertEquals("####\n#  #\n####", Utilities.getBoxedString("", '#'));
	}

	@Test
	public void getListForIteratorAndEnumeration() {
		assertEquals(List.of("a", "b"), Utilities.getListForIterator(List.of("a", "b").iterator()));
		assertEquals(List.of(), Utilities.getListForIterator(Collections.emptyIterator()));
		assertEquals(List.of("a", "b"), Utilities.getListForEnumeration(new Vector<>(List.of("a", "b")).elements()));
	}

	private static X509Certificate readTestCertificate() throws Exception {
		return (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(TEST_CERTIFICATE_PEM.getBytes(StandardCharsets.US_ASCII)));
	}

	private static HttpServer startTestServer() throws IOException {
		final HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
		server.createContext("/data", exchange -> {
			final byte[] body = "content".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.write(body);
			}
		});
		server.createContext("/redirect", exchange -> {
			exchange.getResponseHeaders().add("Location", "/data");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/noLocation", exchange -> {
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/loop", exchange -> {
			exchange.getResponseHeaders().add("Location", "/loop");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/missing", exchange -> {
			exchange.sendResponseHeaders(404, -1);
			exchange.close();
		});
		server.start();
		return server;
	}

	/**
	 * Stream remembering whether it was closed
	 */
	private static class ClosingStateInputStream extends ByteArrayInputStream {
		boolean closed = false;

		ClosingStateInputStream(final byte[] data) {
			super(data);
		}

		@Override
		public void close() throws IOException {
			closed = true;
			super.close();
		}
	}

	//	@Test
	//	public void testEncodingUTF_8() {
	//		try {
	//			List<Charset> encodingsToCheck = new ArrayList<Charset>();
	//			encodingsToCheck.add(StandardCharsets.UTF_8);
	//			encodingsToCheck.add(Charset.forName(StandardCharsets.ISO_8859_1));
	//			encodingsToCheck.add(Charset.forName("UTF-16"));
	//			encodingsToCheck.add(Charset.forName("ISO-8859-15"));
	//			encodingsToCheck.add(Charset.forName("ASCII"));
	//
	//			byte[] data = TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8);
	//			List<Charset> encodings = TextFilePropertiesReader.detectTextEncoding(data, encodingsToCheck);
	//			assertTrue(encodings.size() == 1 && encodings.contains(StandardCharsets.UTF_8));
	//		} catch (Exception e) {
	//			fail(e.getMessage());
	//		}
	//	}
	//
	//	@Test
	//	public void testEncodingISO_8859_1() {
	//		try {
	//			List<Charset> encodingsToCheck = new ArrayList<Charset>();
	//			encodingsToCheck.add(StandardCharsets.UTF_8);
	//			encodingsToCheck.add(Charset.forName(StandardCharsets.ISO_8859_1));
	//			encodingsToCheck.add(Charset.forName("UTF-16"));
	//			encodingsToCheck.add(Charset.forName("ISO-8859-15"));
	//			encodingsToCheck.add(Charset.forName("ASCII"));
	//
	//			byte[] data = Utilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.ISO_8859_1);
	//			List<Charset> encodings = TextFilePropertiesReader.detectTextEncoding(data, encodingsToCheck);
	//			assertTrue(encodings.size() == 1 && encodings.contains(Charset.forName(StandardCharsets.ISO_8859_1)));
	//		} catch (Exception e) {
	//			fail(e.getMessage());
	//		}
	//	}
	//
	//	@Test
	//	public void testEncodingUTF_16() {
	//		try {
	//			List<Charset> encodingsToCheck = new ArrayList<Charset>();
	//			encodingsToCheck.add(StandardCharsets.UTF_8);
	//			encodingsToCheck.add(Charset.forName(StandardCharsets.ISO_8859_1));
	//			encodingsToCheck.add(Charset.forName("UTF-16"));
	//			encodingsToCheck.add(Charset.forName("ISO-8859-15"));
	//			encodingsToCheck.add(Charset.forName("ASCII"));
	//
	//			byte[] data = Utilities.GERMAN_TEST_STRING.getBytes("UTF-16");
	//			List<Charset> encodings = TextFilePropertiesReader.detectTextEncoding(data, encodingsToCheck);
	//			assertTrue(encodings.size() == 1 && encodings.contains(Charset.forName("UTF-16")));
	//		} catch (Exception e) {
	//			fail(e.getMessage());
	//		}
	//	}
	//
	//	@Test
	//	public void testEncodingISO_8859_15() {
	//		try {
	//			List<Charset> encodingsToCheck = new ArrayList<Charset>();
	//			encodingsToCheck.add(StandardCharsets.UTF_8);
	//			encodingsToCheck.add(Charset.forName(StandardCharsets.ISO_8859_1));
	//			encodingsToCheck.add(Charset.forName("UTF-16"));
	//			encodingsToCheck.add(Charset.forName("ISO-8859-15"));
	//			encodingsToCheck.add(Charset.forName("ASCII"));
	//
	//			byte[] data = Utilities.GERMAN_TEST_STRING.getBytes("ISO-8859-15");
	//			List<Charset> encodings = TextFilePropertiesReader.detectTextEncoding(data, encodingsToCheck);
	//			assertTrue(encodings.size() == 1 && encodings.contains(Charset.forName("ISO-8859-15")));
	//		} catch (Exception e) {
	//			fail(e.getMessage());
	//		}
	//	}
	//
	//	@Test
	//	public void testEncodingASCII() {
	//		try {
	//			List<Charset> encodingsToCheck = new ArrayList<Charset>();
	//			encodingsToCheck.add(StandardCharsets.UTF_8);
	//			encodingsToCheck.add(Charset.forName(StandardCharsets.ISO_8859_1));
	//			encodingsToCheck.add(Charset.forName("UTF-16"));
	//			encodingsToCheck.add(Charset.forName("ISO-8859-15"));
	//			encodingsToCheck.add(Charset.forName("ASCII"));
	//
	//			byte[] data = Utilities.GERMAN_TEST_STRING.getBytes("ASCII");
	//			List<Charset> encodings = TextFilePropertiesReader.detectTextEncoding(data, encodingsToCheck);
	//			assertTrue(encodings.size() == 1 && encodings.contains(Charset.forName("ASCII")));
	//		} catch (Exception e) {
	//			fail(e.getMessage());
	//		}
	//	}
}
