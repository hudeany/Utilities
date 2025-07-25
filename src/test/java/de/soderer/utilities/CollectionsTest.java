package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

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

		Assert.assertEquals("Test3", map.get("AbcD123äÄ"));
		Assert.assertEquals("Test3", map.get("abcd123ää"));
		Assert.assertEquals("Test5", map.get("abcd124ää"));
		Assert.assertEquals("Test5", map.get("AbcD124äÄ"));
		Assert.assertNull(map.get("AbcD125äÄ"));
	}

	@Test
	public void testCaseInsensitiveSet() {
		final CaseInsensitiveSet set = new CaseInsensitiveSet();
		set.add("AbcD123äÄ");
		set.add("abcd123ää");
		set.add("AbcD123äÄ");
		set.add("abcd124ää");
		set.add("AbcD124äÄ");

		Assert.assertTrue(set.contains("AbcD123äÄ"));
		Assert.assertTrue(set.contains("abcd123ää"));
		Assert.assertTrue(set.contains("abcd124ää"));
		Assert.assertTrue(set.contains("AbcD124äÄ"));
		Assert.assertFalse(set.contains("AbcD125äÄ"));
	}
}
