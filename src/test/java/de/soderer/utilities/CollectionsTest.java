package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import de.soderer.utilities.collection.CaseInsensitiveLinkedMap;
import de.soderer.utilities.collection.CaseInsensitiveMap;
import de.soderer.utilities.collection.CaseInsensitiveSet;
import de.soderer.utilities.collection.IndexedLinkedHashMap;
import de.soderer.utilities.collection.MapUtilities;
import de.soderer.utilities.collection.MultiValueCaseInsensitiveOrderedMap;
import de.soderer.utilities.collection.Predicate;
import de.soderer.utilities.collection.UniqueFifoQueuedList;

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

	@Nested
	public class CaseInsensitiveMapTests {
		@Test
		public void testAllKeyMethodsIgnoreCase() {
			final CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>();
			map.put("Key", "a");

			assertTrue(map.containsKey("KEY"));
			assertEquals("a", map.getOrDefault("kEy", "default"));
			assertEquals("default", map.getOrDefault("other", "default"));
			assertEquals("a", map.putIfAbsent("KEY", "b"));
			assertEquals("a", map.computeIfAbsent("KEY", k -> "c"));
			assertEquals("a!", map.computeIfPresent("KEY", (k, v) -> v + "!"));
			assertEquals("a!?", map.compute("KEY", (k, v) -> v + "?"));
			assertEquals("a!?+", map.merge("KEY", "+", String::concat));
			assertEquals("a!?+", map.replace("KEY", "x"));
			assertTrue(map.replace("KEY", "x", "y"));
			assertFalse(map.remove("KEY", "wrong value"));
			assertTrue(map.remove("KEY", "y"));
			assertTrue(map.isEmpty());

			map.put("Other", "o");
			assertEquals("o", map.remove("OTHER"));
			assertTrue(map.isEmpty());
		}

		@Test
		public void testComputeFunctionsGetStoredKeyForm() {
			final CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>();
			assertEquals("key", map.computeIfAbsent("KEY", k -> k));
			assertEquals(Set.of("key"), map.keySet());
		}

		@Test
		public void testKeysAreStoredLowercased() {
			final CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>();
			map.put("Content-Type", "text/plain");
			map.put("X-Custom-HEADER", "1");
			assertEquals(Set.of("content-type", "x-custom-header"), map.keySet());
		}

		@Test
		public void testCopyConstructorMergesKeysDifferingOnlyInCase() {
			final Map<String, String> source = new LinkedHashMap<>();
			source.put("Key", "first");
			source.put("KEY", "second");
			final CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>(source);
			assertEquals(1, map.size());
			assertEquals("second", map.get("key"));
		}

		@Test
		public void testNullKey() {
			final CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>();
			map.put(null, "null value");
			assertTrue(map.containsKey(null));
			assertEquals("null value", map.get(null));
		}

		@SuppressWarnings("unlikely-arg-type")
		@Test
		public void testNonStringLookupNeverMatches() {
			final CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>();
			map.put("5", "five");
			// Previously any Object was converted via toString(), so Integer 5 matched the String key "5"
			assertNull(map.get(5));
			assertFalse(map.containsKey(5));
			assertNull(map.remove(5));
			assertEquals(1, map.size());
		}

		@Test
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void testNonStringKeysCannotBeStored() {
			final Map rawMap = new CaseInsensitiveMap<String>();
			assertThrows(ClassCastException.class, () -> rawMap.put(5, "x"));
			assertThrows(ClassCastException.class, () -> rawMap.putIfAbsent(5, "x"));
			assertThrows(ClassCastException.class, () -> rawMap.computeIfAbsent(5, k -> "x"));
			assertThrows(ClassCastException.class, () -> rawMap.compute(5, (k, v) -> "x"));
			assertThrows(ClassCastException.class, () -> rawMap.merge(5, "x", (a, b) -> a));
			assertThrows(ClassCastException.class, () -> rawMap.putAll(Map.of(5, "x")));
			assertTrue(rawMap.isEmpty());
			// A stored sentinel would have made every non-String lookup match
			assertNull(rawMap.get(42));
		}

		@Test
		public void testFoldingMatchesEqualsIgnoreCase() {
			final CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>();
			map.put("s", "s");
			map.put("i", "i");
			map.put("Straße", "street");

			// Long s (U+017F) and dotless i (U+0131) are equalsIgnoreCase to "s" and "I"
			assertTrue("ſ".equalsIgnoreCase("s"));
			assertEquals("s", map.get("ſ"));
			assertTrue("ı".equalsIgnoreCase("I"));
			assertEquals("i", map.get("ı"));

			// Like equalsIgnoreCase there is no expansion of ß to SS
			assertEquals("street", map.get("STRAßE"));
			assertFalse("Straße".equalsIgnoreCase("STRASSE"));
			assertNull(map.get("STRASSE"));
		}

		@Test
		public void testIndependentOfDefaultLocale() {
			final Locale defaultLocale = Locale.getDefault();
			try {
				// In Turkish locale "I".toLowerCase() would be the dotless i
				Locale.setDefault(Locale.forLanguageTag("tr-TR"));
				final CaseInsensitiveMap<String> map = new CaseInsensitiveMap<>();
				map.put("TITLE", "value");
				assertEquals(Set.of("title"), map.keySet());
				assertEquals("value", map.get("title"));
			} finally {
				Locale.setDefault(defaultLocale);
			}
		}

		@Test
		public void testCreateFactory() {
			final CaseInsensitiveMap<Integer> map = CaseInsensitiveMap.create();
			map.put("A", Integer.valueOf(1));
			assertEquals(Integer.valueOf(1), map.get("a"));
		}
	}

	@Nested
	public class CaseInsensitiveLinkedMapTests {
		@Test
		public void testKeepsInsertionOrder() {
			final CaseInsensitiveLinkedMap<String> map = new CaseInsensitiveLinkedMap<>();
			map.put("Zeta", "1");
			map.put("Alpha", "2");
			map.put("Mid", "3");
			map.put("ZETA", "4");
			assertEquals(Arrays.asList("zeta", "alpha", "mid"), new ArrayList<>(map.keySet()));
			assertEquals("4", map.get("zeta"));
		}

		@Test
		public void testTwoArgRemoveIgnoresCase() {
			// Was missing in AbstractLinkedHashMap, so remove("FOO", value) silently removed nothing
			final CaseInsensitiveLinkedMap<String> map = new CaseInsensitiveLinkedMap<>();
			map.put("foo", "value");
			assertTrue(map.remove("FOO", "value"));
			assertTrue(map.isEmpty());
		}

		@Test
		public void testAllKeyMethodsIgnoreCase() {
			final CaseInsensitiveLinkedMap<String> map = new CaseInsensitiveLinkedMap<>();
			map.put("Key", "a");
			assertTrue(map.containsKey("KEY"));
			assertEquals("a", map.putIfAbsent("KEY", "b"));
			assertEquals("a!", map.computeIfPresent("KEY", (k, v) -> v + "!"));
			assertEquals("a!?", map.merge("KEY", "?", String::concat));
			assertTrue(map.replace("KEY", "a!?", "c"));
			assertEquals("c", map.remove("KEY"));
			assertTrue(map.isEmpty());
		}

		@Test
		public void testAccessOrder() {
			final CaseInsensitiveLinkedMap<String> map = new CaseInsensitiveLinkedMap<>(16, 0.75f, true);
			map.put("a", "1");
			map.put("b", "2");
			map.put("c", "3");
			map.get("A");
			assertEquals(Arrays.asList("b", "c", "a"), new ArrayList<>(map.keySet()));
		}

		@Test
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void testNonStringKeys() {
			final Map rawMap = new CaseInsensitiveLinkedMap<String>();
			assertThrows(ClassCastException.class, () -> rawMap.put(5, "x"));
			assertTrue(rawMap.isEmpty());
			assertNull(rawMap.get(5));
		}
	}

	@Nested
	public class CaseInsensitiveSetTests {
		@Test
		public void testConstructors() {
			final CaseInsensitiveSet fromArray = new CaseInsensitiveSet(new String[] { "A", "a", "B" });
			assertEquals(2, fromArray.size());
			assertTrue(fromArray.contains("b"));

			final CaseInsensitiveSet fromCollection = new CaseInsensitiveSet(Arrays.asList("X", "x", "Y"));
			assertEquals(Set.of("x", "y"), new HashSet<>(fromCollection));
		}

		@Test
		public void testRemoveIgnoresCase() {
			final CaseInsensitiveSet set = new CaseInsensitiveSet(new String[] { "Foo" });
			assertTrue(set.remove("FOO"));
			assertTrue(set.isEmpty());
		}

		@Test
		public void testRemoveAllIgnoresCaseForSmallerAndLargerCollections() {
			// AbstractSet.removeAll uses different strategies depending on the sizes, both must ignore case
			final CaseInsensitiveSet set = new CaseInsensitiveSet(new String[] { "a", "b", "c" });
			assertTrue(set.removeAll(Arrays.asList("A")));
			assertEquals(Set.of("b", "c"), new HashSet<>(set));

			assertTrue(set.removeAll(Arrays.asList("B", "X", "Y", "Z")));
			assertEquals(Set.of("c"), new HashSet<>(set));

			assertFalse(set.removeAll(Arrays.asList("nope")));
		}

		@Test
		public void testRemoveAllWithItself() {
			// Used to throw ConcurrentModificationException
			final CaseInsensitiveSet set = new CaseInsensitiveSet(new String[] { "a", "b" });
			assertTrue(set.removeAll(set));
			assertTrue(set.isEmpty());
		}

		@Test
		public void testRetainAllIgnoresCase() {
			final CaseInsensitiveSet set = new CaseInsensitiveSet(new String[] { "a", "b", "c" });
			assertTrue(set.retainAll(Arrays.asList("A", "C")));
			assertEquals(Set.of("a", "c"), new HashSet<>(set));
			assertFalse(set.retainAll(set));
		}

		@Test
		public void testContainsAllIgnoresCase() {
			final CaseInsensitiveSet set = new CaseInsensitiveSet(new String[] { "a", "b" });
			assertTrue(set.containsAll(Arrays.asList("A", "B")));
			assertFalse(set.containsAll(Arrays.asList("A", "C")));
		}

		@Test
		public void testNullItem() {
			final CaseInsensitiveSet set = new CaseInsensitiveSet();
			assertTrue(set.add(null));
			assertTrue(set.contains(null));
			assertTrue(set.remove(null));
		}

		@Test
		@SuppressWarnings({ "rawtypes", "unchecked", "unlikely-arg-type" })
		public void testNonStringItems() {
			final CaseInsensitiveSet set = new CaseInsensitiveSet(new String[] { "5" });
			assertFalse(set.contains(5));
			assertFalse(set.remove(5));

			final Set rawSet = set;
			assertThrows(ClassCastException.class, () -> rawSet.add(5));
			assertEquals(1, set.size());
		}

		@Test
		public void testFoldingMatchesEqualsIgnoreCase() {
			final CaseInsensitiveSet set = new CaseInsensitiveSet(new String[] { "s", "I" });
			assertTrue(set.contains("ſ"));
			assertTrue(set.contains("ı"));
		}
	}

	@Nested
	public class IndexedLinkedHashMapTests {
		private IndexedLinkedHashMap<String, String> createMap(final String... keys) {
			final IndexedLinkedHashMap<String, String> map = new IndexedLinkedHashMap<>();
			for (final String key : keys) {
				map.put(key, key.toUpperCase(Locale.ROOT));
			}
			return map;
		}

		@Test
		public void testIndexAccess() {
			final IndexedLinkedHashMap<String, String> map = createMap("a", "b", "c");
			assertEquals("b", map.getKey(1));
			assertEquals("C", map.getValue(2));
			assertEquals(0, map.getIndexOfKey("a"));
			assertEquals(-1, map.getIndexOfKey("x"));
			assertThrows(IndexOutOfBoundsException.class, () -> map.getKey(3));
		}

		@Test
		public void testNullKey() {
			// Used to throw NullPointerException in getIndexOfKey()
			final IndexedLinkedHashMap<String, String> map = createMap("a");
			map.put(null, "null value");
			assertEquals(1, map.getIndexOfKey(null));
			assertEquals("null value", map.getValue(1));
		}

		@Test
		public void testKeyListIsUnmodifiable() {
			final IndexedLinkedHashMap<String, String> map = createMap("a");
			assertThrows(UnsupportedOperationException.class, () -> map.getKeyList().add("x"));
		}

		@Test
		public void testCacheIsInvalidatedByAllMutatingMethods() {
			// Each mutation must discard the cached key list, so build it before every step
			final IndexedLinkedHashMap<String, String> map = createMap("a", "b");

			map.getKeyList();
			map.put("c", "C");
			assertEquals(Arrays.asList("a", "b", "c"), map.getKeyList());

			map.remove("a");
			assertEquals(Arrays.asList("b", "c"), map.getKeyList());

			map.remove("b", "B");
			assertEquals(Arrays.asList("c"), map.getKeyList());

			map.putIfAbsent("d", "D");
			assertEquals(Arrays.asList("c", "d"), map.getKeyList());

			map.putAll(Map.of("e", "E"));
			assertEquals(Arrays.asList("c", "d", "e"), map.getKeyList());

			map.computeIfAbsent("f", k -> "F");
			assertEquals(Arrays.asList("c", "d", "e", "f"), map.getKeyList());

			map.computeIfPresent("f", (k, v) -> null);
			assertEquals(Arrays.asList("c", "d", "e"), map.getKeyList());

			map.compute("g", (k, v) -> "G");
			assertEquals(Arrays.asList("c", "d", "e", "g"), map.getKeyList());

			map.merge("c", "x", (oldValue, value) -> null);
			assertEquals(Arrays.asList("d", "e", "g"), map.getKeyList());

			map.clear();
			assertTrue(map.getKeyList().isEmpty());
		}

		@Test
		public void testPollFirstAndLastEntry() {
			final IndexedLinkedHashMap<String, String> map = createMap("a", "b", "c");
			map.getKeyList();

			final Entry<String, String> first = map.pollFirstEntry();
			assertEquals("a", first.getKey());
			assertEquals("A", first.getValue());
			assertEquals(Arrays.asList("b", "c"), map.getKeyList());

			final Entry<String, String> last = map.pollLastEntry();
			assertEquals("c", last.getKey());
			assertEquals("C", last.getValue());
			assertEquals(Arrays.asList("b"), map.getKeyList());

			assertThrows(UnsupportedOperationException.class, () -> last.setValue("x"));

			map.clear();
			assertNull(map.pollFirstEntry());
			assertNull(map.pollLastEntry());
		}

		@Test
		public void testCloneHasIndependentCache() {
			final IndexedLinkedHashMap<String, String> original = createMap("a", "b");
			original.getKeyList();
			final IndexedLinkedHashMap<String, String> clone = original.clone();
			assertNotSame(original, clone);

			clone.put("c", "C");
			assertEquals(Arrays.asList("a", "b"), original.getKeyList());
			assertEquals(Arrays.asList("a", "b", "c"), clone.getKeyList());

			original.remove("a");
			assertEquals(Arrays.asList("b"), original.getKeyList());
			assertEquals(Arrays.asList("a", "b", "c"), clone.getKeyList());
		}
	}

	@Nested
	public class MapUtilitiesTests {
		private LinkedHashMap<String, Integer> createAccessOrderMap() {
			final LinkedHashMap<String, Integer> map = new LinkedHashMap<>(16, 0.75f, true);
			map.put("c", Integer.valueOf(3));
			map.put("a", Integer.valueOf(1));
			map.put("b", Integer.valueOf(2));
			return map;
		}

		@Test
		public void testSortByKey() {
			final Map<String, Integer> map = new HashMap<>(Map.of("c", Integer.valueOf(3), "a", Integer.valueOf(1), "b", Integer.valueOf(2)));
			final LinkedHashMap<String, Integer> sorted = MapUtilities.sort(map);
			assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(sorted.keySet()));
			assertEquals(Integer.valueOf(1), sorted.get("a"));

			final LinkedHashMap<String, Integer> reverse = MapUtilities.sort(map, (o1, o2) -> o2.compareTo(o1));
			assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(reverse.keySet()));
		}

		@Test
		public void testSortEntries() {
			final Map<String, Integer> map = new HashMap<>(Map.of("x", Integer.valueOf(3), "y", Integer.valueOf(1), "z", Integer.valueOf(2)));
			final LinkedHashMap<String, Integer> sorted = MapUtilities.sortEntries(map, Entry.comparingByValue());
			assertEquals(Arrays.asList("y", "z", "x"), new ArrayList<>(sorted.keySet()));
		}

		@Test
		public void testSortDoesNotChangeAccessOrderSource() {
			// get() on an access-order LinkedHashMap reorders it, sort() must not use it on the source
			final LinkedHashMap<String, Integer> source = createAccessOrderMap();
			final Map<String, Integer> sourceAsMap = source;

			MapUtilities.sort(sourceAsMap);
			MapUtilities.sort(sourceAsMap, String::compareTo);
			MapUtilities.sort(source);
			MapUtilities.sort(source, String::compareTo);
			MapUtilities.sortEntries(sourceAsMap, Entry.comparingByKey());
			MapUtilities.sortEntries(source, Entry.comparingByKey());

			assertEquals(Arrays.asList("c", "a", "b"), new ArrayList<>(source.keySet()));
		}

		@Test
		public void testSortKeepsConcreteMapType() {
			final CaseInsensitiveLinkedMap<Integer> map = new CaseInsensitiveLinkedMap<>();
			map.put("Zeta", Integer.valueOf(1));
			map.put("Alpha", Integer.valueOf(2));

			final CaseInsensitiveLinkedMap<Integer> sorted = MapUtilities.sort(map);
			assertEquals(Arrays.asList("alpha", "zeta"), new ArrayList<>(sorted.keySet()));
			assertEquals(Integer.valueOf(1), sorted.get("ZETA"));
			// Source unchanged
			assertEquals(Arrays.asList("zeta", "alpha"), new ArrayList<>(map.keySet()));
		}

		@Test
		public void testFilterEntries() {
			final CaseInsensitiveMap<Integer> map = new CaseInsensitiveMap<>();
			map.put("One", Integer.valueOf(1));
			map.put("Two", Integer.valueOf(2));
			map.put("Three", Integer.valueOf(3));

			final CaseInsensitiveMap<Integer> filtered = MapUtilities.filterEntries(map, new Predicate<Entry<String, Integer>>() {
				@Override
				public boolean evaluate(final Entry<String, Integer> entry) {
					return entry.getValue().intValue() % 2 == 1;
				}
			});

			assertEquals(Set.of("one", "three"), filtered.keySet());
			assertEquals(Integer.valueOf(3), filtered.get("THREE"));
			assertEquals(3, map.size());
		}

		@Test
		public void testMapToStringSimple() {
			final Map<String, String> map = new LinkedHashMap<>();
			map.put("a", "1");
			map.put("b", "2");
			assertEquals("a=1,b=2", MapUtilities.mapToString(map));
			assertEquals("", MapUtilities.mapToString(new HashMap<>()));
		}

		@Test
		public void testMapToStringQuoting() {
			final Map<String, String> map = new LinkedHashMap<>();
			map.put("a=b", "c");
			map.put("x", "y\\");
			map.put("q", "say \"hi\", ok");
			map.put("n", null);
			map.put("lit", "null");
			map.put(null, "v");
			map.put("nl", "1\n2");
			assertEquals("\"a=b\"=c,x=\"y\\\\\",q=\"say \\\"hi\\\", ok\",n=null,lit=\"null\",null=v,nl=\"1\n2\"", MapUtilities.mapToString(map));
		}

		@Test
		public void testMapToStringAlwaysQuoteAndCustomChars() {
			final Map<String, String> map = new LinkedHashMap<>();
			map.put("k", "it's");
			map.put("n", null);
			assertEquals("'k'='it\\'s';'n'=null", MapUtilities.mapToString(map, '\'', true, ';'));
		}

		@Test
		public void testMapToStringRoundTrip() {
			final Map<String, String> map = new LinkedHashMap<>();
			map.put("a=b", "c");
			map.put("back\\slash", "x\\\"y");
			map.put("comma,key", "comma,value");
			map.put("n", null);
			map.put("lit", "null");
			map.put(null, "v");
			map.put("plain", "text");

			assertEquals(map, parseMapString(MapUtilities.mapToString(map), '"', ','));
			assertEquals(map, parseMapString(MapUtilities.mapToString(map, '\'', true, ';'), '\'', ';'));
		}

		@Test
		public void testMapToStringRejectsInvalidChars() {
			final Map<String, String> map = Map.of("a", "b");
			assertThrows(IllegalArgumentException.class, () -> MapUtilities.mapToString(map, '\\', false, ','));
			assertThrows(IllegalArgumentException.class, () -> MapUtilities.mapToString(map, '"', false, '='));
			assertThrows(IllegalArgumentException.class, () -> MapUtilities.mapToString(map, ',', false, ','));
		}

		/**
		 * Minimal parser for the mapToString() format, used to prove that its output is unambiguous.
		 */
		private Map<String, String> parseMapString(final String text, final char quoteChar, final char separatorChar) {
			final Map<String, String> result = new LinkedHashMap<>();
			final int[] position = { 0 };
			while (position[0] < text.length()) {
				final String key = parseMapStringItem(text, position, quoteChar, separatorChar);
				assertEquals('=', text.charAt(position[0]));
				position[0]++;
				final String value = parseMapStringItem(text, position, quoteChar, separatorChar);
				result.put(key, value);
				if (position[0] < text.length()) {
					assertEquals(separatorChar, text.charAt(position[0]));
					position[0]++;
				}
			}
			return result;
		}

		private String parseMapStringItem(final String text, final int[] position, final char quoteChar, final char separatorChar) {
			final StringBuilder item = new StringBuilder();
			if (text.charAt(position[0]) == quoteChar) {
				position[0]++;
				while (text.charAt(position[0]) != quoteChar) {
					if (text.charAt(position[0]) == '\\') {
						position[0]++;
					}
					item.append(text.charAt(position[0]++));
				}
				position[0]++;
				return item.toString();
			} else {
				while (position[0] < text.length() && text.charAt(position[0]) != '=' && text.charAt(position[0]) != separatorChar) {
					item.append(text.charAt(position[0]++));
				}
				return "null".equals(item.toString()) ? null : item.toString();
			}
		}
	}

	@Nested
	public class MultiValueCaseInsensitiveOrderedMapTests {
		private MultiValueCaseInsensitiveOrderedMap<String> createMap() {
			final MultiValueCaseInsensitiveOrderedMap<String> map = MultiValueCaseInsensitiveOrderedMap.create();
			map.put("Accept", "text/html");
			map.put("ACCEPT", "text/plain");
			map.put("Host", "example.com");
			return map;
		}

		@Test
		public void testPutAndGet() {
			final MultiValueCaseInsensitiveOrderedMap<String> map = new MultiValueCaseInsensitiveOrderedMap<>();
			assertTrue(map.put("Key", "a"));
			assertFalse(map.put("KEY", "b"));
			assertEquals(Arrays.asList("a", "b"), map.get("key"));
			assertTrue(map.get("unknown").isEmpty());
		}

		@Test
		public void testGetIsUnmodifiable() {
			final MultiValueCaseInsensitiveOrderedMap<String> map = createMap();
			assertThrows(UnsupportedOperationException.class, () -> map.get("accept").add("x"));
			assertThrows(UnsupportedOperationException.class, () -> map.get("unknown").add("x"));
		}

		@Test
		public void testKeyOrder() {
			final MultiValueCaseInsensitiveOrderedMap<String> map = createMap();
			assertEquals(Arrays.asList("accept", "host"), new ArrayList<>(map.keySet()));
		}

		@Test
		public void testRemoveSingleValue() {
			final MultiValueCaseInsensitiveOrderedMap<String> map = createMap();
			assertTrue(map.remove("ACCEPT", "text/html"));
			assertEquals(Arrays.asList("text/plain"), map.get("accept"));
			assertFalse(map.remove("accept", "not contained"));
			assertFalse(map.remove("unknown", "x"));

			// Removing the last value removes the key
			assertTrue(map.remove("accept", "text/plain"));
			assertEquals(Arrays.asList("host"), new ArrayList<>(map.keySet()));
		}

		@Test
		public void testRemoveAll() {
			final MultiValueCaseInsensitiveOrderedMap<String> map = createMap();
			assertEquals(Arrays.asList("text/html", "text/plain"), map.removeAll("ACCEPT"));
			assertTrue(map.removeAll("accept").isEmpty());
			assertEquals(Arrays.asList("host"), new ArrayList<>(map.keySet()));
		}

		@Test
		public void testViewsAreReadOnly() {
			final MultiValueCaseInsensitiveOrderedMap<String> map = createMap();

			final Collection<List<String>> values = map.values();
			assertThrows(UnsupportedOperationException.class, () -> values.iterator().next().clear());
			final Iterator<List<String>> valuesIterator = values.iterator();
			valuesIterator.next();
			assertThrows(UnsupportedOperationException.class, valuesIterator::remove);

			final Set<Entry<String, List<String>>> entries = map.entrySet();
			final Entry<String, List<String>> firstEntry = entries.iterator().next();
			assertThrows(UnsupportedOperationException.class, () -> firstEntry.getValue().clear());
			assertThrows(UnsupportedOperationException.class, () -> firstEntry.setValue(new ArrayList<>()));

			assertEquals(Arrays.asList("text/html", "text/plain"), map.get("accept"));
		}

		@Test
		public void testViewsAreLive() {
			final MultiValueCaseInsensitiveOrderedMap<String> map = createMap();
			final Collection<List<String>> values = map.values();
			final Set<Entry<String, List<String>>> entries = map.entrySet();
			map.put("New", "value");
			assertEquals(3, values.size());
			assertEquals(3, entries.size());

			final Entry<String, List<String>> firstEntry = entries.iterator().next();
			assertEquals("accept", firstEntry.getKey());
			assertEquals(Arrays.asList("text/html", "text/plain"), firstEntry.getValue());
		}

		@Test
		public void testKeySetRemovalRemovesWholeKey() {
			final MultiValueCaseInsensitiveOrderedMap<String> map = createMap();
			assertTrue(map.keySet().remove("accept"));
			assertTrue(map.get("ACCEPT").isEmpty());
		}
	}

	@Nested
	public class UniqueFifoQueuedListTests {
		@Test
		public void testUniqueAndReAddMovesToEnd() {
			final UniqueFifoQueuedList<String> list = new UniqueFifoQueuedList<>();
			list.add("a");
			list.add("b");
			list.add("a");
			assertEquals(Arrays.asList("b", "a"), list);
			assertEquals("a", list.getLatestAdded());
		}

		@Test
		public void testMaxSizeDropsOldest() {
			final UniqueFifoQueuedList<String> list = new UniqueFifoQueuedList<>(2);
			list.add("a");
			list.add("b");
			list.add("c");
			assertEquals(Arrays.asList("b", "c"), list);

			// Re-adding a contained item at full size must not drop anything else
			list.add("b");
			assertEquals(Arrays.asList("c", "b"), list);
		}

		@Test
		public void testMaxSizeZeroKeepsNothing() {
			// Used to behave like maxSize 1
			final UniqueFifoQueuedList<String> list = new UniqueFifoQueuedList<>(0);
			assertFalse(list.add("a"));
			assertFalse(list.add("b"));
			assertTrue(list.isEmpty());
			assertNull(list.getLatestAdded());
		}

		@Test
		public void testInvalidMaxSize() {
			assertThrows(IllegalArgumentException.class, () -> new UniqueFifoQueuedList<String>(-2));
		}

		@Test
		public void testInitialContent() {
			final UniqueFifoQueuedList<String> list = new UniqueFifoQueuedList<>(3, Arrays.asList("a", "b", "a", "c", "d"));
			assertEquals(Arrays.asList("a", "c", "d"), list);

			final UniqueFifoQueuedList<String> withoutContent = new UniqueFifoQueuedList<>(3, null);
			assertTrue(withoutContent.isEmpty());
		}

		@Test
		public void testAddAllReturnValue() {
			final UniqueFifoQueuedList<String> list = new UniqueFifoQueuedList<>(2);
			assertTrue(list.addAll(Arrays.asList("a", "b")));
			assertFalse(list.addAll(new ArrayList<>()));

			final UniqueFifoQueuedList<String> zero = new UniqueFifoQueuedList<>(0);
			assertFalse(zero.addAll(Arrays.asList("a", "b")));
		}

		@Test
		public void testOfferUsesRules() {
			final UniqueFifoQueuedList<String> list = new UniqueFifoQueuedList<>(2);
			list.offer("a");
			list.offer("b");
			list.offer("a");
			list.offer("c");
			assertEquals(Arrays.asList("a", "c"), list);
		}

		@Test
		public void testAddRepairsStateFromBypassingMethods() {
			final UniqueFifoQueuedList<String> list = new UniqueFifoQueuedList<>(2);
			list.add("a");
			list.add("b");
			// Bypasses the rules: creates a duplicate of "b" and exceeds maxSize
			list.addAll(0, Arrays.asList("b", "c"));
			assertEquals(4, list.size());

			list.add("b");
			assertEquals(Arrays.asList("a", "b"), list);
		}

		@Test
		public void testNullItem() {
			final UniqueFifoQueuedList<String> list = new UniqueFifoQueuedList<>(3);
			list.add(null);
			list.add("a");
			list.add(null);
			assertEquals(Arrays.asList("a", null), list);
		}
	}
}
