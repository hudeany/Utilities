package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class StringUtilitiesTest {
	@Test
	public void test_Empty() {
		try {
			assertTrue(StringUtilities.isEmpty((String) null));
			assertTrue(StringUtilities.isEmpty(""));
			assertFalse(StringUtilities.isEmpty("."));
			assertFalse(StringUtilities.isEmpty("a"));
			assertFalse(StringUtilities.isEmpty(" "));
			assertFalse(StringUtilities.isEmpty("\t"));
			assertFalse(StringUtilities.isEmpty("\n"));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void test_Blank() {
		try {
			assertTrue(StringUtilities.isBlank((String) null));
			assertTrue(StringUtilities.isBlank(""));
			assertFalse(StringUtilities.isBlank("."));
			assertFalse(StringUtilities.isBlank("a"));
			assertTrue(StringUtilities.isBlank(" "));
			assertTrue(StringUtilities.isBlank("\t"));
			assertTrue(StringUtilities.isBlank("\n"));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void test_repeat() {
		try {
			assertEquals(null, StringUtilities.repeat(null, 1));
			assertEquals("", StringUtilities.repeat("a", 0));
			assertEquals("a", StringUtilities.repeat("a", 1));
			assertEquals("aaa", StringUtilities.repeat("a", 3));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void repeatString() {
		assertEquals("ababab", StringUtilities.repeat("ab", 3));
		assertEquals("", StringUtilities.repeat("ab", 0));
		assertEquals("ab, ab, ab", StringUtilities.repeat("ab", 3, ", "));
		assertEquals("ab", StringUtilities.repeat("ab", 1, ", "));
	}

	@Test
	public void trimStringToMaximumLength() {
		assertNull(StringUtilities.trimStringToMaximumLength(null, 5, "..."));
		assertEquals("abc", StringUtilities.trimStringToMaximumLength("abc", 5, "..."));
		assertEquals("abcde", StringUtilities.trimStringToMaximumLength("abcde", 5, "..."));
		assertEquals("ab...ij", StringUtilities.trimStringToMaximumLength("abcdefghij", 7, "..."));
		assertEquals("ab...hij", StringUtilities.trimStringToMaximumLength("abcdefghij", 8, "..."));
	}
}
