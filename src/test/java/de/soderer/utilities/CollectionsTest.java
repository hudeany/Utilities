package de.soderer.utilities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.junit.Assert;
import org.junit.Test;

import de.soderer.utilities.collection.CaseInsensitiveOrderedMap;
import de.soderer.utilities.collection.MapUtils;

public class CollectionsTest {

	@Test
	public void test() {
		HashMap<String, Integer> testMap = new LinkedHashMap<String, Integer>(2);
		testMap.put("DEF", 1);
		testMap.put("aBc", 2);
		testMap.put("abC", 3);			
		HashMap<String, Integer> testMap2 = new CaseInsensitiveOrderedMap<Integer>(testMap);
		LinkedHashMap<String, Integer> sortedMap = MapUtils.sort(testMap2);
		Assert.assertEquals(2, sortedMap.size());
		List<Entry<String, Integer>> entries = new ArrayList<Entry<String, Integer>>(sortedMap.entrySet());
		Assert.assertEquals("abc", entries.get(0).getKey());
		Assert.assertEquals(3, entries.get(0).getValue().intValue());
		Assert.assertEquals("def", entries.get(1).getKey());
		Assert.assertEquals(1, entries.get(1).getValue().intValue());
	}
}
