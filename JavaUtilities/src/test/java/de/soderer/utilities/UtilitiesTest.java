package de.soderer.utilities;

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
}
