package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

public class UtilitiesTest {
	@Test
	public void testPunycode1() {
		try {
			String punycode = PunycodeCodec.encode(Utilities.GERMAN_TEST_STRING);
			Assert.assertEquals("abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890 !?@$%&/\\<>(){}[]'\"`^*#.,;:=+-~_|-3dd50g5fvd9dyd97f80c3ryjzs95dssz547y", punycode);
			String result = PunycodeCodec.decode(punycode);
			Assert.assertEquals(Utilities.GERMAN_TEST_STRING, result);
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
