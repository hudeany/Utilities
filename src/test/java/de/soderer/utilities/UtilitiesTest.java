package de.soderer.utilities;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class UtilitiesTest {
	@Test
	public void test_UUID() {
		try {
			String uuidString = Utilities.generateUUID();
			Assert.assertNotNull(uuidString);
			UUID uuid = Utilities.getUUIDFromString(uuidString);
			Assert.assertNotNull(uuid);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void test_Gzipping() {
		try {
			byte[] zippedData = Utilities.gzipByteArray(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"));
			byte[] unzippedData = Utilities.gunzipByteArray(zippedData);
			Assert.assertEquals(TextUtilities.GERMAN_TEST_STRING, new String(unzippedData, "UTF-8"));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void test_Base64() {
		try {
			String encodedData = Utilities.encodeBase64(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"));
			byte[] decodedData = Utilities.decodeBase64(encodedData);
			Assert.assertEquals(TextUtilities.GERMAN_TEST_STRING, new String(decodedData, "UTF-8"));
		} catch (Exception e) {
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
		} catch (Exception e) {
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
		} catch (Exception e) {
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
		} catch (Exception e) {
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
			List<Object> list = new ArrayList<Object>();
			list.add("a");
			list.add("b");
			list.add("c");
			Assert.assertEquals("a,b,c", Utilities.join(list, ","));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void test_remove() {
		try {
			String[] result1 = Utilities.remove(new String[]{"a", "b", "c"}, "b");
			Assert.assertTrue(Utilities.compare(new String[]{"a", "c"}, result1));

			String[] result2 = Utilities.remove(new String[]{"a", "b", "c"}, "d");
			Assert.assertTrue(Utilities.compare(new String[]{"a", "b", "c"}, result2));

			String[] result3 = Utilities.remove(new String[]{"a", "b", null, "c"}, null);
			Assert.assertTrue(Utilities.compare(new String[]{"a", "b", "c"}, result3));
			
			String[] result4 = Utilities.remove(new String[]{"a", "b", "c", "b"}, "b");
			Assert.assertTrue(Utilities.compare(new String[]{"a", "c", "b"}, result4));
			
			String[] result5 = Utilities.remove(new String[]{"a", "b", "c"}, "a");
			Assert.assertTrue(Utilities.compare(new String[]{"b", "c"}, result5));
			
			String[] result6 = Utilities.remove(new String[]{"a", "b", "c"}, "c");
			Assert.assertTrue(Utilities.compare(new String[]{"a", "b"}, result6));
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void test_copy() {
		try {
			InputStream in = new ByteArrayInputStream(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8"));
			OutputStream out = new ByteArrayOutputStream();
			Assert.assertEquals(TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8").length, Utilities.copy(in, out));
			Assert.assertEquals(TextUtilities.GERMAN_TEST_STRING, new String(((ByteArrayOutputStream) out).toByteArray(), "UTF-8"));
		} catch (Exception e) {
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
		} catch (Exception e) {
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
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testPunycode1() {
		try {
			String punycode = PunycodeCodec.encode(TextUtilities.GERMAN_TEST_STRING);
			String expected = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 !?@$%&/\\<>(){}[]'\"`^*#.,;:=+-~_|-3dd92chmsgwdqewdvx0krdw8ag4dssek1so8dhtz957z";
			Assert.assertEquals(expected, punycode);
			String result = PunycodeCodec.decode(punycode);
			Assert.assertEquals(TextUtilities.GERMAN_TEST_STRING, result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode2() {
		try {
			String result = PunycodeCodec.encode("abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890");
			Assert.assertEquals("abcdefghijklmnopqrstuvwxyz-ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode3() {
		try {
			String result = PunycodeCodec.encode("österreich.at");
			Assert.assertEquals("sterreich.at-3ib", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode4() {
		try {
			String punycode = PunycodeCodec.encodeDomainName("Östereich.aäd");
			Assert.assertEquals("xn--stereich-m4a.xn--ad-via", punycode);
			String result = PunycodeCodec.decodeDomainName(punycode);
			Assert.assertEquals("östereich.aäd", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testPunycode5() {
		try {
			String punycode = PunycodeCodec.encodeEmailAdress("Jemand@Östereich.at");
			Assert.assertEquals("Jemand@xn--stereich-m4a.at", punycode);
			String result = PunycodeCodec.decodeEmailAdress(punycode);
			Assert.assertEquals("Jemand@östereich.at", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testHumanReadable1() {
		try {
			String result = Utilities.getHumanReadableNumber(0.1);
			Assert.assertEquals("0,10000", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testHumanReadable8() {
		try {
			String result = Utilities.getHumanReadableNumber(1.0);
			Assert.assertEquals("1,0000", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testHumanReadable9() {
		try {
			String result = Utilities.getHumanReadableNumber(1, "Unit");
			Assert.assertEquals("1 Unit", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testHumanReadable10() {
		try {
			String result = Utilities.getHumanReadableNumber(10000, "Unit");
			Assert.assertEquals("10,000 kUnit", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testHumanReadable11() {
		try {
			String result = Utilities.getHumanReadableNumber(10000, "Unit", false);
			Assert.assertEquals("9,7656 KiUnit", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testHumanReadable2() {
		try {
			String result = Utilities.getHumanReadableNumber(1);
			Assert.assertEquals("1", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testHumanReadable3() {
		try {
			String result = Utilities.getHumanReadableNumber(10);
			Assert.assertEquals("10", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testHumanReadable4() {
		try {
			String result = Utilities.getHumanReadableNumber(100);
			Assert.assertEquals("100", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testHumanReadable5() {
		try {
			String result = Utilities.getHumanReadableNumber(1000);
			Assert.assertEquals("1,0000 k", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testHumanReadable6() {
		try {
			String result = Utilities.getHumanReadableNumber(10000);
			Assert.assertEquals("10,000 k", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testHumanReadable7() {
		try {
			String result = Utilities.getHumanReadableNumber(100000);
			Assert.assertEquals("100,00 k", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testVersion() {
		try {
			Version testVersion = new Version("03.02.01");
			Assert.assertEquals(2, testVersion.getMinorVersionNumber());
			Version testVersion2 = new Version("3.2.1");
			Assert.assertEquals(1, testVersion2.getMicroVersionNumber());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
		
		try {
			new Version("03.x02.01");
			Assert.fail("Missing error on invalid version string");
		} catch (Exception e) {
			// Expected exception
		}

		try {
			Version testVersion3 = Version.getFirstFullVersionNumberFromText("Testtext 3.2.1 Texttest");
			Assert.assertEquals(3, testVersion3.getMajorVersionNumber());
			Assert.assertEquals(2, testVersion3.getMinorVersionNumber());
			Assert.assertEquals(1, testVersion3.getMicroVersionNumber());
			Assert.assertEquals("3.2.1", testVersion3.toString());
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
//	@Test
//	public void testEncodingUTF_8() {
//		try {
//			List<Charset> encodingsToCheck = new ArrayList<Charset>();
//			encodingsToCheck.add(Charset.forName("UTF-8"));
//			encodingsToCheck.add(Charset.forName("ISO-8859-1"));
//			encodingsToCheck.add(Charset.forName("UTF-16"));
//			encodingsToCheck.add(Charset.forName("ISO-8859-15"));
//			encodingsToCheck.add(Charset.forName("ASCII"));
//			
//			byte[] data = TextUtilities.GERMAN_TEST_STRING.getBytes("UTF-8");
//			List<Charset> encodings = TextFilePropertiesReader.detectTextEncoding(data, encodingsToCheck);
//			Assert.assertTrue(encodings.size() == 1 && encodings.contains(Charset.forName("UTF-8")));
//		} catch (Exception e) {
//			Assert.fail(e.getMessage());
//		}
//	}
//	
//	@Test
//	public void testEncodingISO_8859_1() {
//		try {
//			List<Charset> encodingsToCheck = new ArrayList<Charset>();
//			encodingsToCheck.add(Charset.forName("UTF-8"));
//			encodingsToCheck.add(Charset.forName("ISO-8859-1"));
//			encodingsToCheck.add(Charset.forName("UTF-16"));
//			encodingsToCheck.add(Charset.forName("ISO-8859-15"));
//			encodingsToCheck.add(Charset.forName("ASCII"));
//			
//			byte[] data = Utilities.GERMAN_TEST_STRING.getBytes("ISO-8859-1");
//			List<Charset> encodings = TextFilePropertiesReader.detectTextEncoding(data, encodingsToCheck);
//			Assert.assertTrue(encodings.size() == 1 && encodings.contains(Charset.forName("ISO-8859-1")));
//		} catch (Exception e) {
//			Assert.fail(e.getMessage());
//		}
//	}
//	
//	@Test
//	public void testEncodingUTF_16() {
//		try {
//			List<Charset> encodingsToCheck = new ArrayList<Charset>();
//			encodingsToCheck.add(Charset.forName("UTF-8"));
//			encodingsToCheck.add(Charset.forName("ISO-8859-1"));
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
//			encodingsToCheck.add(Charset.forName("UTF-8"));
//			encodingsToCheck.add(Charset.forName("ISO-8859-1"));
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
//			encodingsToCheck.add(Charset.forName("UTF-8"));
//			encodingsToCheck.add(Charset.forName("ISO-8859-1"));
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
