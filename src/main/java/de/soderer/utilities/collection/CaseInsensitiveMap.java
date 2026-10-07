package de.soderer.utilities.collection;

import java.util.Locale;
import java.util.Map;

/**
 * Generic String keyed Map that ignores the String case.
 * Keys are stored lowercased (with {@link Locale#ROOT}), so {@link #keySet()} returns the lowercased keys.
 *
 * @param <V> type of the values
 */
public class CaseInsensitiveMap<V> extends AbstractHashMap<String, V> {
	private static final long serialVersionUID = -528027610172636779L;

	/**
	 * Creates a new empty map. Convenience factory that allows the value type to be inferred.
	 *
	 * @param <V> type of the values
	 * @return new empty map
	 */
	public static <V> CaseInsensitiveMap<V> create() {
		return new CaseInsensitiveMap<>();
	}

	/**
	 * Creates an empty map with default initial capacity and load factor.
	 */
	public CaseInsensitiveMap() {
		super();
	}

	/**
	 * Creates an empty map with the given initial capacity and load factor.
	 *
	 * @param initialCapacity initial capacity of the map
	 * @param loadFactor load factor of the map
	 */
	public CaseInsensitiveMap(final int initialCapacity, final float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates an empty map with the given initial capacity.
	 *
	 * @param initialCapacity initial capacity of the map
	 */
	public CaseInsensitiveMap(final int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a map containing all entries of the given map. Keys that differ only in case are merged, the
	 * value of the last one in the given map's iteration order wins.
	 *
	 * @param map map whose entries are copied into this map
	 */
	public CaseInsensitiveMap(final Map<? extends String, ? extends V> map) {
		super(map.size());
		putAll(map);
	}

	/**
	 * Sentinel value used for lookups with a non-String key. It is never stored as an actual key (this map
	 * only ever stores lowercased Strings), so passing it to the underlying HashMap always reports "not found"
	 * instead of the previous behavior of converting any Object via toString(), which could cause false-positive
	 * matches (e.g. an Integer key 5 matching a stored String key "5").
	 */
	private static final String NON_STRING_KEY_SENTINEL = "\u0000non-string-key-sentinel-" + java.util.UUID.randomUUID();

	@Override
	protected String convertKey(final Object key) {
		if (key == null) {
			return null;
		} else if (key instanceof String) {
			return ((String) key).toLowerCase(Locale.ROOT);
		} else {
			return NON_STRING_KEY_SENTINEL;
		}
	}
}
