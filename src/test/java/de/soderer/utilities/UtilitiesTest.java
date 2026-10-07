package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import de.soderer.utilities.collection.MapUtilities;

@SuppressWarnings("static-method")
public class UtilitiesTest {
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
	public void test_Empty() {
		try {
			assertTrue(Utilities.isEmpty((String) null));
			assertTrue(Utilities.isEmpty(""));
			assertFalse(Utilities.isEmpty("."));
			assertFalse(Utilities.isEmpty("a"));
			assertFalse(Utilities.isEmpty(" "));
			assertFalse(Utilities.isEmpty("\t"));
			assertFalse(Utilities.isEmpty("\n"));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void test_Blank() {
		try {
			assertTrue(Utilities.isBlank((String) null));
			assertTrue(Utilities.isBlank(""));
			assertFalse(Utilities.isBlank("."));
			assertFalse(Utilities.isBlank("a"));
			assertTrue(Utilities.isBlank(" "));
			assertTrue(Utilities.isBlank("\t"));
			assertTrue(Utilities.isBlank("\n"));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void test_repeat() {
		try {
			assertEquals(null, Utilities.repeat(null, 1));
			assertEquals("", Utilities.repeat("a", 0));
			assertEquals("a", Utilities.repeat("a", 1));
			assertEquals("aaa", Utilities.repeat("a", 3));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void test_join() {
		try {
			assertEquals(null, Utilities.join((String[]) null, ","));
			assertEquals("", Utilities.join(new String[0], ","));
			assertEquals("abc", Utilities.join(new String[]{"a", "b", "c"}, ""));
			assertEquals("a,b,c", Utilities.join(new String[]{"a", "b", "c"}, ","));
			assertEquals("a,b, c", Utilities.join(new String[]{"a", "b", " c"}, ","));
			assertEquals("a,,c", Utilities.join(new String[]{"a", null, "c"}, ","));
			final List<Object> list = new ArrayList<>();
			list.add("a");
			list.add("b");
			list.add("c");
			assertEquals("a,b,c", Utilities.join(list, ","));
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
	public void testPunycode1() {
		try {
			final String punycode = PunycodeCodec.encode(TextUtilities.GERMAN_TEST_STRING);
			final String expected = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 !?@$%&/\\<>(){}[]'\"`^*#.,;:=+-~_|-3dd92chmsgwdqewdvx0krdw8ag4dssek1so8dhtz957z";
			assertEquals(expected, punycode);
			final String result = PunycodeCodec.decode(punycode);
			assertEquals(TextUtilities.GERMAN_TEST_STRING, result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode2() {
		try {
			final String result = PunycodeCodec.encode("abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
			assertEquals("abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode3() {
		try {
			final String result = PunycodeCodec.encode("österreich.at");
			assertEquals("sterreich.at-3ib", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode4() {
		try {
			final String punycode = PunycodeCodec.encodeDomainName("Östereich.aäd");
			assertEquals("xn--stereich-m4a.xn--ad-via", punycode);
			final String result = PunycodeCodec.decodeDomainName(punycode);
			assertEquals("östereich.aäd", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode5() {
		try {
			final String punycode = PunycodeCodec.encodeEmailAdress("Jemand@Östereich.at");
			assertEquals("Jemand@xn--stereich-m4a.at", punycode);
			final String result = PunycodeCodec.decodeEmailAdress(punycode);
			assertEquals("Jemand@östereich.at", result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode6() {
		try {
			PunycodeCodec.encodeEmailAdress("Jemand@.Östereich.at");
			fail("Missing expected exception");
		} catch (@SuppressWarnings("unused") final Exception e) {
			// Expected exception
		}
	}

	@Test
	public void testPunycode7() {
		try {
			final String punycode = PunycodeCodec.encodeEmailAdress("Norm.mail@example.com");
			assertEquals("Norm.mail@example.com", punycode);
			final String result = PunycodeCodec.decodeEmailAdress(punycode);
			assertEquals("Norm.mail@example.com", result);
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
	public void testShortenStringToMaxLength() {
		try {
			assertEquals("Abcdefgh", Utilities.shortenStringToMaxLengthCutRight("Abcdefgh", 10));
			assertEquals("Abc ...", Utilities.shortenStringToMaxLengthCutRight("Abcdefgh", 7));
			assertEquals("Abc ...", Utilities.shortenStringToMaxLengthCutRight("Abcdefgh1234567890", 7));
			assertEquals("Abcdefgh", Utilities.shortenStringToMaxLengthCutMiddle("Abcdefgh", 10));
			assertEquals("A ... h", Utilities.shortenStringToMaxLengthCutMiddle("Abcdefgh", 7));
			assertEquals("Ab ... 90", Utilities.shortenStringToMaxLengthCutMiddle("Abcdefgh1234567890", 9));
			assertEquals("Abcdefgh", Utilities.shortenStringToMaxLengthCutLeft("Abcdefgh", 10));
			assertEquals("... fgh", Utilities.shortenStringToMaxLengthCutLeft("Abcdefgh", 7));
			assertEquals("... 890", Utilities.shortenStringToMaxLengthCutLeft("Abcdefgh1234567890", 7));
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
