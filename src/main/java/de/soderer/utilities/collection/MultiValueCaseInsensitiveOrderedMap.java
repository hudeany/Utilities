package de.soderer.utilities.collection;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Generic String keyed Map that ignores the String case but keeps the order of key entries and allows multiple entries for the same key
 *
 * @param <V> type of the values
 */
public class MultiValueCaseInsensitiveOrderedMap<V> {
	private final CaseInsensitiveLinkedMap<ArrayList<V>> map = new CaseInsensitiveLinkedMap<>();

	/**
	 * Creates an empty map.
	 */
	public MultiValueCaseInsensitiveOrderedMap() {
		// Nothing to initialize
	}

	/**
	 * Creates a new empty map. Convenience factory that allows the value type to be inferred.
	 *
	 * @param <V> type of the values
	 * @return new empty map
	 */
	public static <V> MultiValueCaseInsensitiveOrderedMap<V> create() {
		return new MultiValueCaseInsensitiveOrderedMap<>();
	}

	/**
	 * Adds a value under the given key (case-insensitive), creating the value list for that key if needed.
	 *
	 * @param key key to add the value under
	 * @param value value to add
	 * @return true if the key was not present before this call, false if the value was appended to an existing
	 *         value list. (Previously this returned a copy of the previous value list, which made repeated puts
	 *         on the same key quadratic in the number of values.)
	 */
	public boolean put(final String key, final V value) {
		ArrayList<V> values = map.get(key);
		final boolean keyIsNew = (values == null);
		if (values == null) {
			values = new ArrayList<>();
			map.put(key, values);
		}
		values.add(value);
		return keyIsNew;
	}

	/**
	 * Returns the values stored under the given key (case-insensitive).
	 *
	 * @param key key to look up
	 * @return an unmodifiable view of the values stored under this key, or an empty list
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
	 * @param key key the value is stored under (case-insensitive)
	 * @param value value to remove
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
	 * @param key key to remove (case-insensitive)
	 * @return the removed values, or an empty list if the key was not present. Never returns null.
	 */
	public List<V> removeAll(final String key) {
		final ArrayList<V> removedValues = map.remove(key);
		return removedValues == null ? Collections.emptyList() : removedValues;
	}

	/**
	 * Returns a live, read-only view of the entries of this map in key order, with the keys in their stored
	 * (lowercased) form and unmodifiable value lists, consistent with {@link #get(String)}. Use
	 * {@link #put(String, Object)}, {@link #remove(String, Object)} and {@link #removeAll(String)} for changes.
	 *
	 * @return read-only view of the entries of this map
	 */
	public Set<Entry<String, List<V>>> entrySet() {
		return new AbstractSet<>() {
			@Override
			public Iterator<Entry<String, List<V>>> iterator() {
				final Iterator<Entry<String, ArrayList<V>>> iterator = map.entrySet().iterator();
				// remove() is inherited from Iterator and throws UnsupportedOperationException
				return new Iterator<>() {
					@Override
					public boolean hasNext() {
						return iterator.hasNext();
					}

					@Override
					public Entry<String, List<V>> next() {
						final Entry<String, ArrayList<V>> entry = iterator.next();
						return new AbstractMap.SimpleImmutableEntry<>(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
					}
				};
			}

			@Override
			public int size() {
				return map.size();
			}
		};
	}

	/**
	 * Returns a live, read-only view of the value lists of all keys in key order. The lists are unmodifiable,
	 * consistent with {@link #get(String)}, so no key can be left behind with an empty value list.
	 *
	 * @return read-only view of the value lists of this map
	 */
	public Collection<List<V>> values() {
		return new AbstractCollection<>() {
			@Override
			public Iterator<List<V>> iterator() {
				final Iterator<ArrayList<V>> iterator = map.values().iterator();
				// remove() is inherited from Iterator and throws UnsupportedOperationException
				return new Iterator<>() {
					@Override
					public boolean hasNext() {
						return iterator.hasNext();
					}

					@Override
					public List<V> next() {
						return Collections.unmodifiableList(iterator.next());
					}
				};
			}

			@Override
			public int size() {
				return map.size();
			}
		};
	}

	/**
	 * Returns the keys of this map in their stored (lowercased) form.
	 * <p>
	 * Note: the returned view's own {@code remove}/{@code removeAll}/{@code retainAll}/{@code contains} do
	 * NOT apply case-insensitive key matching - they compare directly against the internally stored
	 * (already lowercased) keys, since this simply exposes the underlying {@link CaseInsensitiveLinkedMap}'s
	 * keySet(). Use this class's own {@link #remove(String, Object)} or {@link #removeAll(String)} instead
	 * for case-insensitive removal by key. Removing keys via this view removes their whole value list.
	 *
	 * @return view of the keys of this map
	 */
	public Set<String> keySet() {
		return map.keySet();
	}
}
