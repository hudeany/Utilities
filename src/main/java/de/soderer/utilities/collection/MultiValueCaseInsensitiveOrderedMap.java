package de.soderer.utilities.collection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Generic String keyed Map that ignores the String case but keeps the order of key entries and allows multiple entries for the same key
 */
public class MultiValueCaseInsensitiveOrderedMap<V> {
	private final CaseInsensitiveLinkedMap<ArrayList<V>> map = new CaseInsensitiveLinkedMap<>();

	public static <V> MultiValueCaseInsensitiveOrderedMap<V> create() {
		return new MultiValueCaseInsensitiveOrderedMap<>();
	}

	/**
	 * Adds a value under the given key (case-insensitive), creating the value list for that key if needed.
	 *
	 * @return a snapshot of the values that were stored under this key before this call, or null if the key
	 *         was not present before this call. Note this is the previous value *list*, not a single previous
	 *         value as in the usual Map.put() contract.
	 */
	public ArrayList<V> put(final String key, final V value) {
		final ArrayList<V> existingList = map.get(key);
		final ArrayList<V> previousValue = existingList == null ? null : new ArrayList<>(existingList);
		map.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
		return previousValue;
	}

	/**
	 * @return an unmodifiable view of the values stored under this key (case-insensitive), or an empty list
	 *         if the key is not present. Never returns null.
	 */
	public List<V> get(final String key) {
		final ArrayList<V> values = map.get(key);
		return values == null ? Collections.emptyList() : Collections.unmodifiableList(values);
	}

	/**
	 * Removes a single value from the list stored under the given key. If this was the last remaining value
	 * for that key, the key itself is removed from the map instead of being left behind with an empty list.
	 *
	 * @return true if the value was found under this key and removed, false if the key was absent or did not
	 *         contain this value
	 */
	public boolean remove(final String key, final V value) {
		final ArrayList<V> values = map.get(key);
		if (values == null) {
			return false;
		}
		final boolean removed = values.remove(value);
		if (values.isEmpty()) {
			map.remove(key);
		}
		return removed;
	}

	/**
	 * Removes the given key entirely, along with all values stored under it.
	 *
	 * @return the removed values, or an empty list if the key was not present. Never returns null.
	 */
	public List<V> removeAll(final String key) {
		final ArrayList<V> removedValues = map.remove(key);
		return removedValues == null ? Collections.emptyList() : removedValues;
	}

	/**
	 * {@inheritDoc}
	 *
	 * Note: the returned view's own {@code remove}/{@code removeAll}/{@code retainAll}/{@code contains} do
	 * NOT apply case-insensitive key matching - they compare directly against the internally stored
	 * (already lowercased) keys, since this simply exposes the underlying {@link CaseInsensitiveLinkedMap}'s
	 * entrySet(). Use this class's own {@link #remove(String, Object)} or {@link #removeAll(String)} instead
	 * for case-insensitive removal by key.
	 */
	public Set<Entry<String, ArrayList<V>>> entrySet() {
		return map.entrySet();
	}

	public Collection<ArrayList<V>> values() {
		return map.values();
	}

	/**
	 * {@inheritDoc}
	 *
	 * Note: the returned view's own {@code remove}/{@code removeAll}/{@code retainAll}/{@code contains} do
	 * NOT apply case-insensitive key matching - they compare directly against the internally stored
	 * (already lowercased) keys, since this simply exposes the underlying {@link CaseInsensitiveLinkedMap}'s
	 * keySet(). Use this class's own {@link #remove(String, Object)} or {@link #removeAll(String)} instead
	 * for case-insensitive removal by key.
	 */
	public Set<String> keySet() {
		return map.keySet();
	}
}
