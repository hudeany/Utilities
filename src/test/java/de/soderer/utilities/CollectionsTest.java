package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.soderer.utilities.collection.CaseInsensitiveMap;
import de.soderer.utilities.collection.CaseInsensitiveSet;

@SuppressWarnings("static-method")
public class CollectionsTest {
	@Test
	public void testCaseInsensitiveMap() {
		final CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>();
		map.put("AbcD123äÄ", "Test1");
		map.put("abcd123ää", "Test2");
		map.put("AbcD123äÄ", "Test3");
		map.put("abcd124ää", "Test4");
		map.put("AbcD124äÄ", "Test5");

		assertEquals("Test3", map.get("AbcD123äÄ"));
		assertEquals("Test3", map.get("abcd123ää"));
		assertEquals("Test5", map.get("abcd124ää"));
		assertEquals("Test5", map.get("AbcD124äÄ"));
		assertNull(map.get("AbcD125äÄ"));
	}

	@Test
	public void testCaseInsensitiveSet() {
		final CaseInsensitiveSet set = new CaseInsensitiveSet();
		set.add("AbcD123äÄ");
		set.add("abcd123ää");
		set.add("AbcD123äÄ");
		set.add("abcd124ää");
		set.add("AbcD124äÄ");

		assertTrue(set.contains("AbcD123äÄ"));
		assertTrue(set.contains("abcd123ää"));
		assertTrue(set.contains("abcd124ää"));
		assertTrue(set.contains("AbcD124äÄ"));
		assertFalse(set.contains("AbcD125äÄ"));
	}
}
