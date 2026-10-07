package de.soderer.utilities;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A thread-safe LRU cache implementation based on internal LinkedHashMap.
 * When the maximum size is exceeded, the least recently accessed entry (by get or put) is removed.
 *
 * @param <K>
 *            Entry Key Type
 * @param <V>
 *            Entry Value Type
 */
public class LRUCache<K, V> {
	/** Default maximum number of entries */
	public static final int DEFAULT_MAX_SIZE = 1000;

	private final Map<K, V> internalMap;

	/**
	 * Create a cache with {@link #DEFAULT_MAX_SIZE} entries at most.
	 */
	public LRUCache() {
		this(DEFAULT_MAX_SIZE);
	}

	/**
	 * Create a cache with the given maximum number of entries.
	 *
	 * @param maxSize
	 *            maximum number of entries, must not be negative
	 */
	public LRUCache(final int maxSize) {
		internalMap = Collections.synchronizedMap(new LinkedHashMap<K, V>(maxSize + 1, .75F, true) {
			private static final long serialVersionUID = 6658309225064456992L;

			@Override
			protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
				return size() > maxSize;
			}
		});
	}

	/**
	 * Store a value, possibly removing the least recently used entry.
	 *
	 * @param key
	 *            key of the entry, may be null
	 * @param value
	 *            value of the entry, may be null
	 * @return previous value of the key, or null if there was none
	 */
	public V put(final K key, final V value) {
		return internalMap.put(key, value);
	}

	/**
	 * Get a cached value and mark its entry as recently used.
	 *
	 * @param key
	 *            key of the entry
	 * @return cached value, or null if the key is not cached
	 */
	public V get(final K key) {
		return internalMap.get(key);
	}
}
