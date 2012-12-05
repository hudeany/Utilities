package de.soderer.utilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;

public class TextUtilitiesTest {
	String testStringWindows = "abc\r\ndef\r\näöü\r\n123\r\nabc\r\ndef\r\n123";
	String testStringUnix = "abc\ndef\näöü\n123\nabc\ndef\n123";
	
	@Test
	public void testSearchAndReplace1() {
		try {
			Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringWindows, 5, "abc", false, true, false);
			assertEquals(new Integer(20), result.getFirst());
			assertEquals(new Integer(3), result.getSecond());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearchAndReplace2() {
		try {
			Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringWindows, 5, "abc", false, true, true);
			assertEquals(new Integer(0), result.getFirst());
			assertEquals(new Integer(3), result.getSecond());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearchAndReplace3() {
		try {
			Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringWindows, 5, "123$", false, true, false);
			assertNull(result);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearchAndReplace4() {
		try {
			Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringWindows, 5, "ä", false, true, false);
			assertEquals(new Integer(10), result.getFirst());
			assertEquals(new Integer(1), result.getSecond());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearchAndReplace5() {
		try {
			Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringWindows, 5, "123$", true, true, false);
			assertEquals(new Integer(15), result.getFirst());
			assertEquals(new Integer(3), result.getSecond());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearchAndReplace6() {
		try {
			Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringUnix, 5, "123$", true, true, false);
			assertEquals(new Integer(12), result.getFirst());
			assertEquals(new Integer(3), result.getSecond());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearchAndReplace7() {
		try {
			Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringWindows, 5, "^123", true, true, false);
			assertEquals(new Integer(15), result.getFirst());
			assertEquals(new Integer(3), result.getSecond());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearchAndReplace8() {
		try {
			Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringUnix, 5, "^123", true, true, false);
			assertEquals(new Integer(12), result.getFirst());
			assertEquals(new Integer(3), result.getSecond());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testSearchAndReplace9() {
		try {
			Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringWindows, 5, "123\r\n", true, true, false);
			assertEquals(new Integer(15), result.getFirst());
			assertEquals(new Integer(5), result.getSecond());
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}
	
	@Test
	public void testRightAlign1() {
		try {
			String test = TextUtilities.trimStringToLengthAlignedRight("123  ", 7);
			Assert.assertEquals("  123  ", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testRightAlign2() {
		try {
			String test = TextUtilities.trimStringToLengthAlignedRight("abc", 15);
			Assert.assertEquals("            abc", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testRightAlign3() {
		try {
			String test = TextUtilities.trimStringToLengthAlignedRight("abc12345", 5);
			Assert.assertEquals("12345", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testRightAlign4() {
		try {
			String test = TextUtilities.trimStringToLengthAlignedRight("123", 7);
			Assert.assertEquals("    123", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLeftAlign1() {
		try {
			String test = TextUtilities.trimStringToLengthAlignedLeft("  123", 7);
			Assert.assertEquals("  123  ", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLeftAlign2() {
		try {
			String test = TextUtilities.trimStringToLengthAlignedLeft("abc", 15);
			Assert.assertEquals("abc            ", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLeftAlign3() {
		try {
			String test = TextUtilities.trimStringToLengthAlignedLeft("12345abc", 5);
			Assert.assertEquals("12345", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLeftAlign4() {
		try {
			String test = TextUtilities.trimStringToLengthAlignedLeft("123", 7);
			Assert.assertEquals("123    ", test);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLineNumber1() {
		try {
			int index = TextUtilities.getStartIndexOfLine("123", 1);
			Assert.assertEquals(0, index);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLineNumber2() {
		try {
			int index = TextUtilities.getStartIndexOfLine("123", 3);
			Assert.assertEquals(0, index);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLineNumber3() {
		try {
			int index = TextUtilities.getStartIndexOfLine("123\n456\n789", 1);
			Assert.assertEquals(0, index);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLineNumber4() {
		try {
			int index = TextUtilities.getStartIndexOfLine("123\n456\n789", 3);
			Assert.assertEquals(8, index);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLineNumber5() {
		try {
			int index = TextUtilities.getStartIndexOfLine("123\r\n456\r\n789", 2);
			Assert.assertEquals(5, index);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testLineNumber6() {
		try {
			int index = TextUtilities.getStartIndexOfLine("123\n456\n789", 5);
			Assert.assertEquals(8, index);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testIndention1() {
		try {
			String data = "123\n  456\n    789\n  456\n123";
			String result = TextUtilities.replaceIndentingBlanks(data, 1, "\t");
			Assert.assertEquals("123\n\t\t456\n\t\t\t\t789\n\t\t456\n123", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testIndention2() {
		try {
			String data = "123\n  456\n    789\n  456\n123";
			String result = TextUtilities.replaceIndentingBlanks(data, 2, "\t");
			Assert.assertEquals("123\n\t456\n\t\t789\n\t456\n123", result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testEndIndex() {
		try {
			String dataString = "123\n  456";
			int result = TextUtilities.getEndIndexOfLineAtIndex(dataString, 6);
			Assert.assertEquals(8, result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testEndIndex2() {
		try {
			String dataString = "123\n  456\n789";
			int result = TextUtilities.getEndIndexOfLineAtIndex(dataString, 6);
			Assert.assertEquals(8, result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testEndIndex3() {
		try {
			String dataString = "123\r\n456";
			int result = TextUtilities.getEndIndexOfLineAtIndex(dataString, 5);
			Assert.assertEquals(7, result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void testEndIndex4() {
		try {
			String dataString = "123\r\n456\r\n789";
			int result = TextUtilities.getEndIndexOfLineAtIndex(dataString, 5);
			Assert.assertEquals(7, result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
