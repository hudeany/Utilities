package de.soderer.utilities;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class UtilitiesTest {
	@Test
	public void testRightAlign1() {
		try {
			String test = Utilities.trimStringToLengthAlignedRight("123  ", 7);
			Assert.assertEquals("  123  ", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testRightAlign2() {
		try {
			String test = Utilities.trimStringToLengthAlignedRight("abc", 15);
			Assert.assertEquals("            abc", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testRightAlign3() {
		try {
			String test = Utilities.trimStringToLengthAlignedRight("abc12345", 5);
			Assert.assertEquals("12345", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testRightAlign4() {
		try {
			String test = Utilities.trimStringToLengthAlignedRight("123", 7);
			Assert.assertEquals("    123", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLeftAlign1() {
		try {
			String test = Utilities.trimStringToLengthAlignedLeft("  123", 7);
			Assert.assertEquals("  123  ", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLeftAlign2() {
		try {
			String test = Utilities.trimStringToLengthAlignedLeft("abc", 15);
			Assert.assertEquals("abc            ", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLeftAlign3() {
		try {
			String test = Utilities.trimStringToLengthAlignedLeft("12345abc", 5);
			Assert.assertEquals("12345", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLeftAlign4() {
		try {
			String test = Utilities.trimStringToLengthAlignedLeft("123", 7);
			Assert.assertEquals("123    ", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLineNumber1() {
		try {
			int index = Utilities.getStartIndexOfLine("123", 1);
			Assert.assertEquals(0, index);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLineNumber2() {
		try {
			int index = Utilities.getStartIndexOfLine("123", 3);
			Assert.assertEquals(0, index);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLineNumber3() {
		try {
			int index = Utilities.getStartIndexOfLine("123\n456\n789", 1);
			Assert.assertEquals(0, index);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLineNumber4() {
		try {
			int index = Utilities.getStartIndexOfLine("123\n456\n789", 3);
			Assert.assertEquals(8, index);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLineNumber5() {
		try {
			int index = Utilities.getStartIndexOfLine("123\r\n456\r\n789", 2);
			Assert.assertEquals(5, index);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLineNumber6() {
		try {
			int index = Utilities.getStartIndexOfLine("123\n456\n789", 5);
			Assert.assertEquals(8, index);
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
