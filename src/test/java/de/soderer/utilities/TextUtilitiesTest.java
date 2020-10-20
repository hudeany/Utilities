package de.soderer.utilities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class TextUtilitiesTest {
	String testStringWindows = "abc\r\ndef\r\näöü\r\n123\r\nabc\r\ndef\r\n123";
	String testStringUnix = "abc\ndef\näöü\n123\nabc\ndef\n123";

	@Test
	public void testSearchAndReplace1() {
		try {
			final Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringWindows, 5, "abc", false, true, false);
			assertEquals(new Integer(20), result.getFirst());
			assertEquals(new Integer(3), result.getSecond());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearchAndReplace2() {
		try {
			final Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringWindows, 5, "abc", false, true, true);
			assertEquals(new Integer(0), result.getFirst());
			assertEquals(new Integer(3), result.getSecond());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearchAndReplace3() {
		try {
			final Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringWindows, 5, "123$", false, true, false);
			assertNull(result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearchAndReplace4() {
		try {
			final Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringWindows, 5, "ä", false, true, false);
			assertEquals(new Integer(10), result.getFirst());
			assertEquals(new Integer(1), result.getSecond());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearchAndReplace5() {
		try {
			final Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringWindows, 5, "123$", true, true, false);
			assertEquals(new Integer(15), result.getFirst());
			assertEquals(new Integer(3), result.getSecond());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearchAndReplace6() {
		try {
			final Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringUnix, 5, "123$", true, true, false);
			assertEquals(new Integer(12), result.getFirst());
			assertEquals(new Integer(3), result.getSecond());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearchAndReplace7() {
		try {
			final Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringWindows, 5, "^123", true, true, false);
			assertEquals(new Integer(15), result.getFirst());
			assertEquals(new Integer(3), result.getSecond());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearchAndReplace8() {
		try {
			final Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringUnix, 5, "^123", true, true, false);
			assertEquals(new Integer(12), result.getFirst());
			assertEquals(new Integer(3), result.getSecond());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testSearchAndReplace9() {
		try {
			final Tuple<Integer, Integer> result = TextUtilities.searchNextPosition(testStringWindows, 5, "123\r\n", true, true, false);
			assertEquals(new Integer(15), result.getFirst());
			assertEquals(new Integer(5), result.getSecond());
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void testRightAlign1() {
		try {
			final String test = TextUtilities.trimStringToLengthAlignedRight("123  ", 7);
			Assert.assertEquals("  123  ", test);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testRightAlign2() {
		try {
			final String test = TextUtilities.trimStringToLengthAlignedRight("abc", 15);
			Assert.assertEquals("            abc", test);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testRightAlign3() {
		try {
			final String test = TextUtilities.trimStringToLengthAlignedRight("abc12345", 5);
			Assert.assertEquals("12345", test);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testRightAlign4() {
		try {
			final String test = TextUtilities.trimStringToLengthAlignedRight("123", 7);
			Assert.assertEquals("    123", test);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testLeftAlign1() {
		try {
			final String test = TextUtilities.trimStringToLengthAlignedLeft("  123", 7);
			Assert.assertEquals("  123  ", test);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testLeftAlign2() {
		try {
			final String test = TextUtilities.trimStringToLengthAlignedLeft("abc", 15);
			Assert.assertEquals("abc            ", test);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testLeftAlign3() {
		try {
			final String test = TextUtilities.trimStringToLengthAlignedLeft("12345abc", 5);
			Assert.assertEquals("12345", test);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testLeftAlign4() {
		try {
			final String test = TextUtilities.trimStringToLengthAlignedLeft("123", 7);
			Assert.assertEquals("123    ", test);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testLineNumber1() {
		try {
			final int index = TextUtilities.getStartIndexOfLine("123", 1);
			Assert.assertEquals(0, index);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testLineNumber2() {
		try {
			final int index = TextUtilities.getStartIndexOfLine("123", 3);
			Assert.assertEquals(0, index);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testLineNumber3() {
		try {
			final int index = TextUtilities.getStartIndexOfLine("123\n456\n789", 1);
			Assert.assertEquals(0, index);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testLineNumber4() {
		try {
			final int index = TextUtilities.getStartIndexOfLine("123\n456\n789", 3);
			Assert.assertEquals(8, index);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testLineNumber5() {
		try {
			final int index = TextUtilities.getStartIndexOfLine("123\r\n456\r\n789", 2);
			Assert.assertEquals(5, index);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testLineNumber6() {
		try {
			final int index = TextUtilities.getStartIndexOfLine("123\n456\n789", 5);
			Assert.assertEquals(8, index);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testIndention1() {
		try {
			final String data = "123\n  456\n    789\n  456\n123";
			final String result = TextUtilities.replaceIndentingBlanks(data, 1, "\t");
			Assert.assertEquals("123\n\t\t456\n\t\t\t\t789\n\t\t456\n123", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testIndention2() {
		try {
			final String data = "123\n  456\n    789\n  456\n123";
			final String result = TextUtilities.replaceIndentingBlanks(data, 2, "\t");
			Assert.assertEquals("123\n\t456\n\t\t789\n\t456\n123", result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testEndIndex() {
		try {
			final String dataString = "123\n  456";
			final int result = TextUtilities.getEndIndexOfLineAtIndex(dataString, 6);
			Assert.assertEquals(9, result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testEndIndex2() {
		try {
			final String dataString = "123\n  456\n789";
			final int result = TextUtilities.getEndIndexOfLineAtIndex(dataString, 6);
			Assert.assertEquals(9, result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testEndIndex3() {
		try {
			final String dataString = "123\r\n456";
			final int result = TextUtilities.getEndIndexOfLineAtIndex(dataString, 5);
			Assert.assertEquals(8, result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testEndIndex4() {
		try {
			final String dataString = "123\r\n456\r\n789";
			final int result = TextUtilities.getEndIndexOfLineAtIndex(dataString, 5);
			Assert.assertEquals(8, result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testScanLineStarts1() {
		try {
			final String dataString = "123\r\n456\n789\r101112";
			final List<Long> result = TextUtilities.scanLineStartIndexes(new ByteArrayInputStream(dataString.getBytes(StandardCharsets.UTF_8)));
			Assert.assertEquals(4, result.size());
			Assert.assertEquals(new Long(0), result.get(0));
			Assert.assertEquals(new Long(5), result.get(1));
			Assert.assertEquals(new Long(9), result.get(2));
			Assert.assertEquals(new Long(13), result.get(3));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testScanLineStarts2() {
		try {
			final String dataString = "123\r\n456\n789\r101112\r\n";
			final List<Long> result = TextUtilities.scanLineStartIndexes(new ByteArrayInputStream(dataString.getBytes(StandardCharsets.UTF_8)));
			Assert.assertEquals(5, result.size());
			Assert.assertEquals(new Long(0), result.get(0));
			Assert.assertEquals(new Long(5), result.get(1));
			Assert.assertEquals(new Long(9), result.get(2));
			Assert.assertEquals(new Long(13), result.get(3));
			Assert.assertEquals(new Long(21), result.get(4));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testScanLineStarts3() {
		try {
			final String dataString = "123\r\n456\n789\r101112\r";
			final List<Long> result = TextUtilities.scanLineStartIndexes(new ByteArrayInputStream(dataString.getBytes(StandardCharsets.UTF_8)));
			Assert.assertEquals(5, result.size());
			Assert.assertEquals(new Long(0), result.get(0));
			Assert.assertEquals(new Long(5), result.get(1));
			Assert.assertEquals(new Long(9), result.get(2));
			Assert.assertEquals(new Long(13), result.get(3));
			Assert.assertEquals(new Long(20), result.get(4));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testScanLinebreakIndexes1() {
		try {
			final String dataString = "123\r\n456\n789\r101112";
			final List<Long> result = TextUtilities.scanLinebreakIndexes(new ByteArrayInputStream(dataString.getBytes(StandardCharsets.UTF_8)));
			Assert.assertEquals(3, result.size());
			Assert.assertEquals(new Long(3), result.get(0));
			Assert.assertEquals(new Long(8), result.get(1));
			Assert.assertEquals(new Long(12), result.get(2));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testScanLinebreakIndexes2() {
		try {
			final String dataString = "123\r\n456\n789\r101112\r\n";
			final List<Long> result = TextUtilities.scanLinebreakIndexes(new ByteArrayInputStream(dataString.getBytes(StandardCharsets.UTF_8)));
			Assert.assertEquals(4, result.size());
			Assert.assertEquals(new Long(3), result.get(0));
			Assert.assertEquals(new Long(8), result.get(1));
			Assert.assertEquals(new Long(12), result.get(2));
			Assert.assertEquals(new Long(19), result.get(3));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void testScanLinebreakIndexes3() {
		try {
			final String dataString = "123\r\n456\n789\r101112\r";
			final List<Long> result = TextUtilities.scanLinebreakIndexes(new ByteArrayInputStream(dataString.getBytes(StandardCharsets.UTF_8)));
			Assert.assertEquals(4, result.size());
			Assert.assertEquals(new Long(3), result.get(0));
			Assert.assertEquals(new Long(8), result.get(1));
			Assert.assertEquals(new Long(12), result.get(2));
			Assert.assertEquals(new Long(19), result.get(3));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
