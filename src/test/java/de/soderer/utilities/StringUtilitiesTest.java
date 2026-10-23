package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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

	@Test
	public void test_join() {
		try {
			assertEquals(null, StringUtilities.join((String[]) null, ","));
			assertEquals("", StringUtilities.join(new String[0], ","));
			assertEquals("abc", StringUtilities.join(new String[]{"a", "b", "c"}, ""));
			assertEquals("a,b,c", StringUtilities.join(new String[]{"a", "b", "c"}, ","));
			assertEquals("a,b, c", StringUtilities.join(new String[]{"a", "b", " c"}, ","));
			assertEquals("a,,c", StringUtilities.join(new String[]{"a", null, "c"}, ","));
			final List<Object> list = new ArrayList<>();
			list.add("a");
			list.add("b");
			list.add("c");
			assertEquals("a,b,c", StringUtilities.join(list, ","));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}

	@Test
	public void join_variants() {
		assertEquals("a-b-c", StringUtilities.join(new char[] { 'a', 'b', 'c' }, "-"));
		assertEquals("abc", StringUtilities.join(new char[] { 'a', 'b', 'c' }, null));
		assertEquals("", StringUtilities.join(new char[0], "-"));
		assertNull(StringUtilities.join((char[]) null, "-"));

		assertEquals("ab", StringUtilities.join(new Object[] { "a", "b" }, null));
		assertEquals("1;;x", StringUtilities.join(Arrays.asList(1, null, "x"), ";"));
		assertEquals("ab", StringUtilities.join(List.of("a", "b"), null));
		assertEquals("", StringUtilities.join(List.of(), ";"));
		assertNull(StringUtilities.join((Iterable<?>) null, ";"));
	}

	@Test
	public void checkForValidUserName() {
		assertTrue(StringUtilities.checkForValidUserName("user_Name-01"));
		assertFalse(StringUtilities.checkForValidUserName("user name"));
		assertFalse(StringUtilities.checkForValidUserName("user@example"));
		assertFalse(StringUtilities.checkForValidUserName(""));
		assertFalse(StringUtilities.checkForValidUserName(null));
	}

	@Test
	public void shortenStringToMaxLength() {
		assertEquals("abcd ...", StringUtilities.shortenStringToMaxLengthCutRight("abcdefghij", 8));
		assertEquals("ab..", StringUtilities.shortenStringToMaxLengthCutRight("abcdef", 4, ".."));
		assertEquals("abcd", StringUtilities.shortenStringToMaxLengthCutRight("abcdef", 4, null));
		// The cut sign does not fit, so the end is cut off without it
		assertEquals("ab", StringUtilities.shortenStringToMaxLengthCutRight("abcdef", 2, "..."));
		assertEquals("...", StringUtilities.shortenStringToMaxLengthCutRight("abcdef", 3, "..."));
		assertEquals("", StringUtilities.shortenStringToMaxLengthCutRight("abcdef", -1, "..."));
		assertEquals("abc", StringUtilities.shortenStringToMaxLengthCutRight("abc", 8));
		assertNull(StringUtilities.shortenStringToMaxLengthCutRight(null, 8));

		assertEquals("... ghij", StringUtilities.shortenStringToMaxLengthCutLeft("abcdefghij", 8));
		assertEquals("hij", StringUtilities.shortenStringToMaxLengthCutLeft("abcdefghij", 3));
		assertEquals("", StringUtilities.shortenStringToMaxLengthCutLeft("abcdefghij", -1));
		assertEquals("abc", StringUtilities.shortenStringToMaxLengthCutLeft("abc", 8));
		assertNull(StringUtilities.shortenStringToMaxLengthCutLeft(null, 8));
	}

	@Test
	public void indexOfIgnoreCase() {
		assertEquals(6, StringUtilities.indexOfIgnoreCase("Hello World", "WORLD"));
		assertEquals(0, StringUtilities.indexOfIgnoreCase("Hello", ""));
		assertEquals(-1, StringUtilities.indexOfIgnoreCase("Hello", "xyz"));
		assertEquals(-1, StringUtilities.indexOfIgnoreCase("Hi", "Hello"));
		assertEquals(0, StringUtilities.indexOfIgnoreCase(null, null));
		assertEquals(-1, StringUtilities.indexOfIgnoreCase("a", null));
		assertEquals(-1, StringUtilities.indexOfIgnoreCase(null, "a"));
	}

	@Test
	public void substring_nullSafe() {
		assertEquals("bc", StringUtilities.substring("abc", 1));
		assertEquals("", StringUtilities.substring("abc", 3));
		assertEquals("", StringUtilities.substring("abc", 5));
		assertNull(StringUtilities.substring(null, 1));

		assertEquals("ab", StringUtilities.substring("abc", 0, 2));
		assertEquals("bc", StringUtilities.substring("abc", 1, 10));
		assertEquals("", StringUtilities.substring("abc", 4, 5));
		assertNull(StringUtilities.substring(null, 0, 1));
	}

	@Test
	public void contains_nullSafe() {
		assertTrue(StringUtilities.contains("abc", "b"));
		assertFalse(StringUtilities.contains("abc", "x"));
		assertFalse(StringUtilities.contains((String) null, "a"));
	}

	@Test
	public void joinNotBlank() {
		assertEquals("a,b", StringUtilities.joinNotBlank(Arrays.asList("a", " ", null, "", "b"), ","));
		assertEquals("", StringUtilities.joinNotBlank(Arrays.asList(" ", null), ","));
	}

	@Test
	public void upperCase() {
		assertEquals("STRASSE", StringUtilities.upperCase("straße"));
		assertNull(StringUtilities.upperCase(null));

		final Locale defaultLocale = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag("tr-TR"));
			assertEquals("TITLE", StringUtilities.upperCase("title"));
		} finally {
			Locale.setDefault(defaultLocale);
		}
	}

	@Test
	public void trim() {
		assertEquals("a b", StringUtilities.trim("  a b \t"));
		assertNull(StringUtilities.trim(null));

		assertEquals("a", StringUtilities.trim("xxaxx", 'x'));
		assertEquals("axa", StringUtilities.trim("xaxax", 'x'));
		assertEquals("", StringUtilities.trim("xxx", 'x'));
		assertNull(StringUtilities.trim(null, 'x'));

		assertEquals("a b  ", StringUtilities.trimLeft(" \t\na b  "));
		assertEquals("  a b", StringUtilities.trimRight("  a b \t\n"));
		assertNull(StringUtilities.trimLeft(null));
		assertNull(StringUtilities.trimRight(null));
	}
}
