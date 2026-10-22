package de.soderer.utilities.collection;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * LinkedHashMap that additionally allows access to its keys and values by their position in insertion order.
 * <p>
 * The positions are taken from a lazily built list of the keys, which is discarded by all mutating methods of
 * this map itself. Structural changes made through the views ({@link #keySet()}, {@link #values()},
 * {@link #entrySet()}) or their iterators do not discard it, so after such changes index based access may
 * return outdated results until the next mutation via the map's own methods.
 *
 * @param <K> type of the keys
 * @param <V> type of the values
 */
public class IndexedLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
	private static final long serialVersionUID = -1676269301623131177L;

	/**
	 * Cached index view of the current keySet(), lazily (re)built by getKeyList().
	 * Transient: it is a derived cache, not part of this map's actual state, and is rebuilt on first
	 * access after deserialization or cloning instead of being carried along.
	 */
	private transient List<K> keyList = null;

	/**
	 * Creates an empty map.
	 */
	public IndexedLinkedHashMap() {
		super();
	}

	@SuppressWarnings("unlikely-arg-type")
	@Override
	public V remove(final Object key) {
		keyList = null;
		return super.remove(key);
	}

	@Override
	public boolean remove(final Object key, final Object value) {
		keyList = null;
		return super.remove(key, value);
	}

	@Override
	public V put(final K key, final V value) {
		keyList = null;
		return super.put(key, value);
	}

	@Override
	public void putAll(final Map<? extends K, ? extends V> map) {
		keyList = null;
		super.putAll(map);
	}

	@Override
	public V putIfAbsent(final K key, final V value) {
		keyList = null;
		return super.putIfAbsent(key, value);
	}

	@Override
	public void clear() {
		keyList = null;
		super.clear();
	}

	@Override
	public V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) {
		keyList = null;
		return super.computeIfAbsent(key, mappingFunction);
	}

	@Override
	public V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		keyList = null;
		return super.computeIfPresent(key, remappingFunction);
	}

	@Override
	public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		keyList = null;
		return super.compute(key, remappingFunction);
	}

	@Override
	public V merge(final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		keyList = null;
		return super.merge(key, value, remappingFunction);
	}

	/**
	 * Removes and returns the first entry in insertion order.
	 * <p>
	 * On Java 21+ this overrides {@code SequencedMap.pollFirstEntry()}, whose default implementation removes
	 * via the entry set iterator and would therefore leave the cached key list stale. Declared without
	 * {@code @Override} and implemented without calling super, so this class still compiles with --release 17.
	 *
	 * @return an unmodifiable snapshot of the removed entry, or null if this map is empty
	 */
	public Map.Entry<K, V> pollFirstEntry() {
		final Iterator<Map.Entry<K, V>> iterator = entrySet().iterator();
		if (!iterator.hasNext()) {
			return null;
		}
		final Map.Entry<K, V> result = new AbstractMap.SimpleImmutableEntry<>(iterator.next());
		keyList = null;
		iterator.remove();
		return result;
	}

	/**
	 * Removes and returns the last entry in insertion order.
	 * <p>
	 * On Java 21+ this overrides {@code SequencedMap.pollLastEntry()} for the same reason as
	 * {@link #pollFirstEntry()}. Without access to the reverse iteration of Java 21 this walks the whole entry
	 * set (O(n)), which is in line with the O(n) key list rebuild that every mutation of this map causes anyway.
	 *
	 * @return an unmodifiable snapshot of the removed entry, or null if this map is empty
	 */
	public Map.Entry<K, V> pollLastEntry() {
		Map.Entry<K, V> lastEntry = null;
		for (final Map.Entry<K, V> entry : entrySet()) {
			lastEntry = entry;
		}
		if (lastEntry == null) {
			return null;
		}
		final Map.Entry<K, V> result = new AbstractMap.SimpleImmutableEntry<>(lastEntry);
		remove(result.getKey());
		return result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public IndexedLinkedHashMap<K, V> clone() {
		// HashMap.clone() does a shallow copy of all fields, which would otherwise leave this clone sharing
		// the same keyList reference as the original until either one is mutated - reset it instead, it will
		// be rebuilt lazily on next access.
		final IndexedLinkedHashMap<K, V> clone = (IndexedLinkedHashMap<K, V>) super.clone();
		clone.keyList = null;
		return clone;
	}

	/**
	 * Returns the keys of this map in insertion order.
	 *
	 * @return unmodifiable list of the keys in insertion order
	 */
	public List<K> getKeyList() {
		if (keyList == null) {
			// Unmodifiable, because callers changing this cached list would corrupt all later index lookups
			keyList = Collections.unmodifiableList(new ArrayList<>(keySet()));
		}

		return keyList;
	}

	/**
	 * Returns the position of the given key in insertion order.
	 *
	 * @param key key to search for, may be null
	 * @return position of the key, or -1 if the key is not contained in this map
	 */
	public int getIndexOfKey(final K key) {
		for (int i = 0; i < getKeyList().size(); i++) {
			if (Objects.equals(getKeyList().get(i), key)) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Returns the key at the given position in insertion order.
	 *
	 * @param i position of the key
	 * @return key at the given position
	 * @throws IndexOutOfBoundsException if the position is out of range
	 */
	public K getKey(final int i) {
		return getKeyList().get(i);
	}

	/**
	 * Returns the value of the key at the given position in insertion order.
	 *
	 * @param i position of the key
	 * @return value of the key at the given position
	 * @throws IndexOutOfBoundsException if the position is out of range
	 */
	public V getValue(final int i) {
		return get(getKey(i));
	}
}
