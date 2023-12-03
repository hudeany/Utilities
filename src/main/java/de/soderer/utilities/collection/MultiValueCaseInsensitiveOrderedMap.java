package de.soderer.utilities.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Generic String keyed Map that ignores the String case but keeps the order of key entries and allows multiple entries for the same key
 */
public class MultiValueCaseInsensitiveOrderedMap<V> {
	private final CaseInsensitiveOrderedMap<ArrayList<V>> map = new CaseInsensitiveOrderedMap<>();

	public static <V> MultiValueCaseInsensitiveOrderedMap<V> create() {
		return new MultiValueCaseInsensitiveOrderedMap<>();
	}

	public ArrayList<V> put(final String key, final V value) {
		final ArrayList<V> previousValue = map.get(key);
		map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
		return previousValue;
	}

	public List<V> get(final String key) {
		return map.get(key);
	}

	public void remove(final String key, final V value) {
		map.computeIfPresent(key, (k, v) -> {
			v.remove(value);
			return v;
		});
	}

	public Set<Entry<String, ArrayList<V>>> entrySet() {
		return map.entrySet();
	}

	public Collection<ArrayList<V>> values() {
		return map.values();
	}

	public Set<String> keySet() {
		return map.keySet();
	}
}
