package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

public class UtilitiesTest {
	@Test
	public void testPunycode1() {
		try {
			String punycode = PunycodeCodec.encode(TextUtilities.GERMAN_TEST_STRING);
			String expected = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789 !?@$%&/\\<>(){}[]'\"`^*#.,;:=+-~_|-3dd50g5fvd9dyd97f80c3ryjzs95dssz547y";
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
//			byte[] data = Utilities.GERMAN_TEST_STRING.getBytes("UTF-8");
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
