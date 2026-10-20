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

	/** Upper limit for the initial capacity, so a large maximum size does not allocate a huge table up front */
	private static final int MAX_INITIAL_CAPACITY = 1024;

	private static final float LOAD_FACTOR = 0.75F;

	private final int maxSize;

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
	 *            maximum number of entries, must not be negative. 0 creates a cache that never
	 *            keeps any entry.
	 * @throws IllegalArgumentException
	 *             if maxSize is negative
	 */
	public LRUCache(final int maxSize) {
		if (maxSize < 0) {
			throw new IllegalArgumentException("Maximum size must not be negative: " + maxSize);
		}
		this.maxSize = maxSize;
		internalMap = Collections.synchronizedMap(new LRUMap<>(maxSize));
	}

	/**
	 * Returns the maximum number of entries.
	 *
	 * @return the maximum size given on creation
	 */
	public int getMaxSize() {
		return maxSize;
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

	/**
	 * Check whether a key is cached, without marking its entry as recently used.
	 *
	 * @param key
	 *            key of the entry, may be null
	 * @return true if the key is cached (also if its value is null)
	 */
	public boolean containsKey(final K key) {
		return internalMap.containsKey(key);
	}

	/**
	 * Remove an entry from the cache.
	 *
	 * @param key
	 *            key of the entry, may be null
	 * @return removed value, or null if the key was not cached
	 */
	public V remove(final K key) {
		return internalMap.remove(key);
	}

	/**
	 * Remove all entries from the cache.
	 */
	public void clear() {
		internalMap.clear();
	}

	/**
	 * Returns the current number of entries.
	 *
	 * @return number of cached entries, never more than {@link #getMaxSize()}
	 */
	public int size() {
		return internalMap.size();
	}

	/**
	 * Check whether the cache contains no entries.
	 *
	 * @return true if no entry is cached
	 */
	public boolean isEmpty() {
		return internalMap.isEmpty();
	}

	/**
	 * LinkedHashMap in access order, which removes the eldest entry when the maximum size is
	 * exceeded. A static class, so it does not keep a reference to the enclosing cache.
	 *
	 * @param <K>
	 *            Entry Key Type
	 * @param <V>
	 *            Entry Value Type
	 */
	private static final class LRUMap<K, V> extends LinkedHashMap<K, V> {
		private static final long serialVersionUID = 6658309225064456992L;

		private final int maxSize;

		private LRUMap(final int maxSize) {
			// Capacity for maxSize entries without rehashing, computed as long to avoid an int overflow for large sizes
			super((int) Math.min((long) Math.ceil(maxSize / LOAD_FACTOR) + 1, MAX_INITIAL_CAPACITY), LOAD_FACTOR, true);
			this.maxSize = maxSize;
		}

		@Override
		protected boolean removeEldestEntry(final Map.Entry<K, V> eldest) {
			return size() > maxSize;
		}
	}
}
