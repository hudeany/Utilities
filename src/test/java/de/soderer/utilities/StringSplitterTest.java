package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class StringSplitterTest {
	@Test
	public void splitAndTrimList_defaultSeparators() {
		assertEquals(List.of("a", "b", "c", "d", "e", "f", "g"), StringSplitter.splitAndTrimList("a, b;c|d e\tf\ng"));
		assertEquals(List.of("a", "b"), StringSplitter.splitAndTrimList("a,,\r\nb"));
		assertEquals(List.of(), StringSplitter.splitAndTrimList(""));
		assertNull(StringSplitter.splitAndTrimList(null));
	}

	@Test
	public void splitAndTrimList_givenSeparators() {
		assertEquals(List.of("a", "b", "c"), StringSplitter.splitAndTrimList("a . b|c", '.', '|'));
		assertEquals(List.of("a b", "c"), StringSplitter.splitAndTrimList("a b;c", (Character) null, ';'));
		assertEquals(List.of("a,b"), StringSplitter.splitAndTrimList(" a,b ", new Character[0]));
		assertEquals(List.of(), StringSplitter.splitAndTrimList("  ", new Character[0]));
		assertEquals(List.of("a", "b"), StringSplitter.splitAndTrimList("a; ;b", ';'));
		assertNull(StringSplitter.splitAndTrimList(null, ';'));
	}

	@Test
	public void splitAndTrimListQuoted() {
		assertEquals(List.of("a", "'b,c'", "\"d,e\"", "f"), StringSplitter.splitAndTrimListQuoted("a, 'b,c', \"d,e\" ,, f", ','));
		assertEquals(List.of("'a\"b'", "c"), StringSplitter.splitAndTrimListQuoted("'a\"b',c", ','));
		assertEquals(List.of("a", "b", "c"), StringSplitter.splitAndTrimListQuoted("a;b,c", ',', ';'));
		assertEquals(List.of(), StringSplitter.splitAndTrimListQuoted(" , ", ','));
	}

	@Test
	public void split_withEscapeCharacter() {
		assertArrayEquals(new String[] { "a", "b,c", "d" }, StringSplitter.split("a,b\\,c,d", ',', '\\'));
		assertArrayEquals(new String[] { "a\\", "b" }, StringSplitter.split("a\\\\,b", ',', '\\'));
		// Escape characters before other characters are kept
		assertArrayEquals(new String[] { "a\\b" }, StringSplitter.split("a\\b", ',', '\\'));
		assertArrayEquals(new String[] { "a\\" }, StringSplitter.split("a\\", ',', '\\'));
	}

	@Test
	public void split_limitLikeStringSplit() {
		assertArrayEquals(new String[] { "a", "b,c" }, StringSplitter.split("a,b,c", ',', '\\', 2));
		assertArrayEquals(new String[] { "a", "b" }, StringSplitter.split("a,b,,", ',', '\\'));
		assertArrayEquals(new String[] { "a", "b", "", "" }, StringSplitter.split("a,b,,", ',', '\\', -1));
		assertArrayEquals(new String[] { "" }, StringSplitter.split("", ',', '\\'));
		assertArrayEquals(new String[0], StringSplitter.split(",,", ',', '\\'));

		// Same results as String.split for texts without escape characters
		for (final String text : new String[] { "a,b,c", "a,,b", ",a", "a,b,,", ",,", "", "abc" }) {
			for (final int limit : new int[] { -1, 0, 1, 2 }) {
				assertArrayEquals(text.split(",", limit), StringSplitter.split(text, ',', '\\', limit));
			}
		}
	}

	@Test
	public void parseArguments() throws Exception {
		assertEquals(List.of("-a", "b c", "d e", "--name=x y", "", "q\"r", "C:\\Temp"),
				StringSplitter.parseArguments("-a \"b c\" 'd e' --name=\"x y\" \"\" \"q\\\"r\" \"C:\\Temp\""));
		// Single quotes do not support escaping
		assertEquals(List.of("a\\b"), StringSplitter.parseArguments("'a\\b'"));
		assertEquals(List.of("a", "b", "c"), StringSplitter.parseArguments("  a\r\nb\tc  "));
		assertEquals(List.of(), StringSplitter.parseArguments(""));
		assertThrows(Exception.class, () -> StringSplitter.parseArguments("a \"b"));
	}

	@Test
	public void parseTokens() throws Exception {
		assertEquals(List.of("a", "b c", "d"), StringSplitter.parseTokens("a;\"b c\";;d", ';'));
	}
}
