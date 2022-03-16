package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.collection.MapUtilities;

public class UtilitiesTest {
	@Test
	public void test_UUID() {
		try {
			final String uuidString = Utilities.generateUUID();
			Assert.assertNotNull(uuidString);
			final UUID uuid = Utilities.getUUIDFromString(uuidString);
			Assert.assertNotNull(uuid);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_Gzipping() {
		try {
			final byte[] zippedData = Utilities.gzipByteArray(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
			final byte[] unzippedData = Utilities.gunzipByteArray(zippedData);
			Assert.assertEquals(TextUtilities.GERMAN_TEST_STRING, new String(unzippedData, StandardCharsets.UTF_8));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_Base64() {
		try {
			final String encodedData = Utilities.encodeBase64(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
			final byte[] decodedData = Utilities.decodeBase64(encodedData);
			Assert.assertEquals(TextUtilities.GERMAN_TEST_STRING, new String(decodedData, StandardCharsets.UTF_8));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_Empty() {
		try {
			Assert.assertTrue(Utilities.isEmpty((String) null));
			Assert.assertTrue(Utilities.isEmpty(""));
			Assert.assertFalse(Utilities.isEmpty("."));
			Assert.assertFalse(Utilities.isEmpty("a"));
			Assert.assertFalse(Utilities.isEmpty(" "));
			Assert.assertFalse(Utilities.isEmpty("\t"));
			Assert.assertFalse(Utilities.isEmpty("\n"));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_Blank() {
		try {
			Assert.assertTrue(Utilities.isBlank((String) null));
			Assert.assertTrue(Utilities.isBlank(""));
			Assert.assertFalse(Utilities.isBlank("."));
			Assert.assertFalse(Utilities.isBlank("a"));
			Assert.assertTrue(Utilities.isBlank(" "));
			Assert.assertTrue(Utilities.isBlank("\t"));
			Assert.assertTrue(Utilities.isBlank("\n"));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_repeat() {
		try {
			Assert.assertEquals(null, Utilities.repeat(null, 1));
			Assert.assertEquals("", Utilities.repeat("a", 0));
			Assert.assertEquals("a", Utilities.repeat("a", 1));
			Assert.assertEquals("aaa", Utilities.repeat("a", 3));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_join() {
		try {
			Assert.assertEquals(null, Utilities.join((String[]) null, ","));
			Assert.assertEquals("", Utilities.join(new String[0], ","));
			Assert.assertEquals("abc", Utilities.join(new String[]{"a", "b", "c"}, ""));
			Assert.assertEquals("a,b,c", Utilities.join(new String[]{"a", "b", "c"}, ","));
			Assert.assertEquals("a,b, c", Utilities.join(new String[]{"a", "b", " c"}, ","));
			Assert.assertEquals("a,,c", Utilities.join(new String[]{"a", null, "c"}, ","));
			final List<Object> list = new ArrayList<>();
			list.add("a");
			list.add("b");
			list.add("c");
			Assert.assertEquals("a,b,c", Utilities.join(list, ","));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_remove() {
		try {
			final String[] result1 = Utilities.remove(new String[]{"a", "b", "c"}, "b");
			Assert.assertTrue(Utilities.compare(new String[]{"a", "c"}, result1));

			final String[] result2 = Utilities.remove(new String[]{"a", "b", "c"}, "d");
			Assert.assertTrue(Utilities.compare(new String[]{"a", "b", "c"}, result2));

			final String[] result3 = Utilities.remove(new String[]{"a", "b", null, "c"}, null);
			Assert.assertTrue(Utilities.compare(new String[]{"a", "b", "c"}, result3));

			final String[] result4 = Utilities.remove(new String[]{"a", "b", "c", "b"}, "b");
			Assert.assertTrue(Utilities.compare(new String[]{"a", "c", "b"}, result4));

			final String[] result5 = Utilities.remove(new String[]{"a", "b", "c"}, "a");
			Assert.assertTrue(Utilities.compare(new String[]{"b", "c"}, result5));

			final String[] result6 = Utilities.remove(new String[]{"a", "b", "c"}, "c");
			Assert.assertTrue(Utilities.compare(new String[]{"a", "b"}, result6));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_copy() {
		try {
			final InputStream in = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8));
			final OutputStream out = new ByteArrayOutputStream();
			Assert.assertEquals(TextUtilities.GERMAN_TEST_STRING.getBytes(StandardCharsets.UTF_8).length, IoUtilities.copy(in, out));
			Assert.assertEquals(TextUtilities.GERMAN_TEST_STRING, new String(((ByteArrayOutputStream) out).toByteArray(), StandardCharsets.UTF_8));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_rightPad() {
		try {
			Assert.assertEquals("  ", Utilities.rightPad("", 2));
			Assert.assertEquals("a ", Utilities.rightPad("a", 2));
			Assert.assertEquals("ab", Utilities.rightPad("ab", 2));
			Assert.assertEquals("abc", Utilities.rightPad("abc", 2));
			Assert.assertEquals("abc", Utilities.rightPad("abc", 0));
			Assert.assertEquals("", Utilities.rightPad("", 0));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void test_leftPad() {
		try {
			Assert.assertEquals("  ", Utilities.leftPad("", 2));
			Assert.assertEquals(" a", Utilities.leftPad("a", 2));
			Assert.assertEquals("ab", Utilities.leftPad("ab", 2));
			Assert.assertEquals("abc", Utilities.leftPad("abc", 2));
			Assert.assertEquals("abc", Utilities.leftPad("abc", 0));
			Assert.assertEquals("", Utilities.leftPad("", 0));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode1() {
		try {
			final String punycode = PunycodeCodec.encode(TextUtilities.GERMAN_TEST_STRING);
			final String expected = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 !?@$%&/\\<>(){}[]'\"`^*#.,;:=+-~_|-3dd92chmsgwdqewdvx0krdw8ag4dssek1so8dhtz957z";
			Assert.assertEquals(expected, punycode);
			final String result = PunycodeCodec.decode(punycode);
			Assert.assertEquals(TextUtilities.GERMAN_TEST_STRING, result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode2() {
		try {
			final String result = PunycodeCodec.encode("abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
			Assert.assertEquals("abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode3() {
		try {
			final String result = PunycodeCodec.encode("österreich.at");
			Assert.assertEquals("sterreich.at-3ib", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode4() {
		try {
			final String punycode = PunycodeCodec.encodeDomainName("Östereich.aäd");
			Assert.assertEquals("xn--stereich-m4a.xn--ad-via", punycode);
			final String result = PunycodeCodec.decodeDomainName(punycode);
			Assert.assertEquals("östereich.aäd", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode5() {
		try {
			final String punycode = PunycodeCodec.encodeEmailAdress("Jemand@Östereich.at");
			Assert.assertEquals("Jemand@xn--stereich-m4a.at", punycode);
			final String result = PunycodeCodec.decodeEmailAdress(punycode);
			Assert.assertEquals("Jemand@östereich.at", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode6() {
		try {
			PunycodeCodec.encodeEmailAdress("Jemand@.Östereich.at");
			Assert.fail("Missing expected exception");
		} catch (final Exception e) {
			// Expected exception
		}
	}

	@Test
	public void testPunycode7() {
		try {
			final String punycode = PunycodeCodec.encodeEmailAdress("Norm.mail@example.com");
			Assert.assertEquals("Norm.mail@example.com", punycode);
			final String result = PunycodeCodec.decodeEmailAdress(punycode);
			Assert.assertEquals("Norm.mail@example.com", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable1() {
		try {
			final String result = Utilities.getHumanReadableNumber(0.1, null, true, 5, false, Locale.GERMAN);
			Assert.assertEquals("0,1", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable8() {
		try {
			final String result = Utilities.getHumanReadableNumber(1.0, null, true, 5, false, Locale.GERMAN);
			Assert.assertEquals("1,0", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable9() {
		try {
			final String result = Utilities.getHumanReadableNumber(1, "Unit", true, 5, false, Locale.ENGLISH);
			Assert.assertEquals("1 Unit", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable10() {
		try {
			final String result = Utilities.getHumanReadableNumber(10000, "Unit", true, 5, false, Locale.GERMAN);
			Assert.assertEquals("10,0 kUnit", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable11() {
		try {
			final String result = Utilities.getHumanReadableNumber(10000, "Unit", false, 5, false, Locale.GERMAN);
			Assert.assertEquals("9,7656 KiUnit", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable2() {
		try {
			final String result = Utilities.getHumanReadableNumber(1, null, true, 5, false, Locale.ENGLISH);
			Assert.assertEquals("1", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable3() {
		try {
			final String result = Utilities.getHumanReadableNumber(10, null, true, 5, false, Locale.ENGLISH);
			Assert.assertEquals("10", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable4() {
		try {
			final String result = Utilities.getHumanReadableNumber(100, null, true, 5, false, Locale.ENGLISH);
			Assert.assertEquals("100", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable5() {
		try {
			final String result = Utilities.getHumanReadableNumber(1000, null, true, 5, false, Locale.GERMAN);
			Assert.assertEquals("1,0 k", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable6() {
		try {
			final String result = Utilities.getHumanReadableNumber(10000, null, true, 5, false, Locale.GERMAN);
			Assert.assertEquals("10,0 k", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testHumanReadable7() {
		try {
			final String result = Utilities.getHumanReadableNumber(100000, null, true, 5, false, Locale.GERMAN);
			Assert.assertEquals("100,0 k", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testVersion() {
		try {
			final Version testVersion = new Version("03.02.01");
			Assert.assertEquals(2, testVersion.getMinorVersionNumber());
			final Version testVersion2 = new Version("3.2.1");
			Assert.assertEquals(1, testVersion2.getMicroVersionNumber());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}

		try {
			final Version versionToFail = new Version("03.x02.01");
			versionToFail.toString();
			Assert.fail("Missing error on invalid version string");
		} catch (final Exception e) {
			// Expected exception
		}

		try {
			final Version testVersion3 = Version.getFirstFullVersionNumberFromText("Testtext 3.2.1 Texttest");
			Assert.assertEquals(3, testVersion3.getMajorVersionNumber());
			Assert.assertEquals(2, testVersion3.getMinorVersionNumber());
			Assert.assertEquals(1, testVersion3.getMicroVersionNumber());
			Assert.assertEquals("3.2.1", testVersion3.toString());
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testShortenStringToMaxLength() {
		try {
			Assert.assertEquals("Abcdefgh", Utilities.shortenStringToMaxLengthCutRight("Abcdefgh", 10));
			Assert.assertEquals("Abc ...", Utilities.shortenStringToMaxLengthCutRight("Abcdefgh", 7));
			Assert.assertEquals("Abc ...", Utilities.shortenStringToMaxLengthCutRight("Abcdefgh1234567890", 7));
			Assert.assertEquals("Abcdefgh", Utilities.shortenStringToMaxLengthCutMiddle("Abcdefgh", 10));
			Assert.assertEquals("A ... h", Utilities.shortenStringToMaxLengthCutMiddle("Abcdefgh", 7));
			Assert.assertEquals("Ab ... 90", Utilities.shortenStringToMaxLengthCutMiddle("Abcdefgh1234567890", 9));
			Assert.assertEquals("Abcdefgh", Utilities.shortenStringToMaxLengthCutLeft("Abcdefgh", 10));
			Assert.assertEquals("... fgh", Utilities.shortenStringToMaxLengthCutLeft("Abcdefgh", 7));
			Assert.assertEquals("... 890", Utilities.shortenStringToMaxLengthCutLeft("Abcdefgh1234567890", 7));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
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
			Assert.assertEquals(
					"abc122=null"
							+ ",abc123=123"
							+ ",\"aBc\\\"125\"=\"aBc\\\"125\""
							+ ",abc 124=abc 124",
							parsedMapString);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
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

			Assert.assertEquals("122", map.get("abc122"));
			Assert.assertEquals("123", map.get("abc123"));
			Assert.assertEquals("abc 124", map.get("abc 124"));
			Assert.assertEquals("Actual map was: " + map.toString(), "aBc\"125", map.get("aBc\"125"));

			final String parsedMapString = MapUtilities.mapToString(map);
			Assert.assertEquals(
					"Actual map was: " + map.toString(),
					"abc122=122"
							+ ",abc123=123"
							+ ",abc 124=abc 124"
							+ ",\"aBc\\\"125\"=\"aBc\\\"125\""
							+ ",abc 126=abc 12 6",
							parsedMapString);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
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

			Assert.assertEquals("Actual map was: " + map.toString(), "122", map.get("abc122"));
			Assert.assertEquals("123", map.get("abc123"));
			Assert.assertEquals("abc 124", map.get("abc 124"));
			Assert.assertEquals("aBc\"125", map.get("aBc\"125"));

			final String parsedMapString = MapUtilities.mapToString(map, '"', false, ' ');
			Assert.assertEquals(
					"abc122=122"
							+ " abc123=123"
							+ " \"abc 124\"=\"abc 124\""
							+ " \"aBc\\\"125\"=\"aBc\\\"125\"",
							parsedMapString);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
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

			Assert.assertEquals("Actual map was: " + map.toString(), "123", map.get("abc123"));
			Assert.assertEquals("abc 124", map.get("abc 124"));
			Assert.assertEquals("aBc\"125", map.get("aBc\"125"));

			final String parsedMapString = MapUtilities.mapToString(map, '\"', false, ' ');
			Assert.assertEquals(
					"abc123=123"
							+ " \"abc 124\"=\"abc 124\""
							+ " \"aBc\\\"125\"=\"aBc\\\"125\"",
							parsedMapString);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
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

			Assert.assertEquals("123", map.get("abc123"));
			Assert.assertEquals("abc 124", map.get("abc 124"));
			Assert.assertEquals("Actual map was: " + map.toString(), "aBc\"125", map.get("aBc\"125"));

			final String parsedMapString = MapUtilities.mapToString(map, '\"', true, ',');
			Assert.assertEquals(
					"\"abc123\"=\"123\""
							+ ",\"abc 124\"=\"abc 124\""
							+ ",\"aBc\\\"125\"=\"aBc\\\"125\"",
							parsedMapString);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testIntToByteArrayConversion() {
		try {
			final int testInt1 = 123456789;
			final byte[] testArray1 = Utilities.convertIntToByteArray(testInt1);
			final int testResult1 = Utilities.convertByteArrayToInt(testArray1);
			Assert.assertEquals(testInt1, testResult1);

			final int testInt2 = 5;
			final byte[] testArray2 = Utilities.convertIntToByteArray(testInt2);
			final int testResult2 = Utilities.convertByteArrayToInt(testArray2);
			Assert.assertEquals(testInt2, testResult2);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testReverseArray() {
		try {
			final int[] testArrayOddLength = new int[] { 0,1,2,3,4 };
			Utilities.reverseArray(testArrayOddLength);
			Assert.assertArrayEquals(new int[] { 4,3,2,1,0 }, testArrayOddLength);

			final int[] testArrayEvenLength = new int[] { 0,1,2,3,4, 5 };
			Utilities.reverseArray(testArrayEvenLength);
			Assert.assertArrayEquals(new int[] { 5, 4,3,2,1,0 }, testArrayEvenLength);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
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
	//			Assert.assertTrue(encodings.size() == 1 && encodings.contains(StandardCharsets.UTF_8));
	//		} catch (Exception e) {
	//			Assert.fail(e.getMessage());
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
	//			Assert.assertTrue(encodings.size() == 1 && encodings.contains(Charset.forName(StandardCharsets.ISO_8859_1)));
	//		} catch (Exception e) {
	//			Assert.fail(e.getMessage());
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
	//			Assert.assertTrue(encodings.size() == 1 && encodings.contains(Charset.forName("UTF-16")));
	//		} catch (Exception e) {
	//			Assert.fail(e.getMessage());
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
	//			Assert.assertTrue(encodings.size() == 1 && encodings.contains(Charset.forName("ISO-8859-15")));
	//		} catch (Exception e) {
	//			Assert.fail(e.getMessage());
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
	//			Assert.assertTrue(encodings.size() == 1 && encodings.contains(Charset.forName("ASCII")));
	//		} catch (Exception e) {
	//			Assert.fail(e.getMessage());
	//		}
	//	}
}
