package de.soderer.utilities.collection;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Base class for HashMap variants that transform keys on the way in and out via {@link #convertKey(Object)}
 * (e.g. case-insensitive lookup by lowercasing String keys).
 * <p>
 * Note: {@link #keySet()} and {@link #entrySet()} return the standard {@link HashMap} views. Their own
 * mutation/lookup methods that take an externally supplied key (e.g. {@code keySet().remove(Object)},
 * {@code keySet().removeAll(Collection)}, {@code keySet().retainAll(Collection)},
 * {@code keySet().contains(Object)}, and the {@link java.util.Map.Entry}-based equivalents on
 * {@code entrySet()}) do NOT go through {@link #convertKey(Object)} - they compare directly against the
 * internally stored (already converted) keys. So e.g. {@code caseInsensitiveMap.keySet().remove("FOO")} can
 * silently fail to remove an entry that is actually stored under the converted key "foo", even though
 * {@code caseInsensitiveMap.remove("FOO")} works correctly. Iterating via {@link java.util.Iterator} and
 * calling {@code Iterator.remove()} is unaffected, since that always operates on the key as already stored.
 * <p>
 * Also note that {@link #equals(Object)} and {@link #hashCode()} are inherited unchanged and work on the stored
 * (converted) keys. Comparing with a map that stores unconverted keys is therefore asymmetric and inconsistent
 * with hashCode(): e.g. {@code new HashMap<>(Map.of("FOO", 1)).equals(caseInsensitiveMap)} can be true while
 * {@code caseInsensitiveMap.equals(thatHashMap)} is false and both hash codes differ (the same caveat applies to
 * a TreeMap with a case-insensitive comparator). Do not mix both kinds of maps in equality based contexts, such as
 * keys of another map.
 *
 * @param <K> type of the keys
 * @param <V> type of the values
 */
public abstract class AbstractHashMap<K, V> extends HashMap<K, V> {
	private static final long serialVersionUID = 868647429993685054L;

	/**
	 * Creates an empty map with default initial capacity and load factor.
	 */
	public AbstractHashMap() {
		super();
	}

	/**
	 * Creates an empty map with the given initial capacity and load factor.
	 *
	 * @param initialCapacity initial capacity of the map
	 * @param loadFactor load factor of the map
	 */
	public AbstractHashMap(final int initialCapacity, final float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates an empty map with the given initial capacity.
	 *
	 * @param initialCapacity initial capacity of the map
	 */
	public AbstractHashMap(final int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a map containing all entries of the given map, with their keys converted via {@link #convertKey(Object)}.
	 *
	 * @param map map whose entries are copied into this map
	 */
	public AbstractHashMap(final Map<? extends K, ? extends V> map) {
		super(map.size());
		putAll(map);
	}

	/**
	 * {@inheritDoc}
	 *
	 * See the class-level note: the returned view's own {@code remove}/{@code removeAll}/{@code retainAll}/
	 * {@code contains} do not route external keys through {@link #convertKey(Object)}.
	 */
	@Override
	public Set<K> keySet() {
		return super.keySet();
	}

	/**
	 * {@inheritDoc}
	 *
	 * See the class-level note: the returned view's own {@code remove}/{@code removeAll}/{@code retainAll}/
	 * {@code contains} do not route external keys through {@link #convertKey(Object)}.
	 */
	@Override
	public Set<Entry<K, V>> entrySet() {
		return super.entrySet();
	}

	@Override
	public boolean containsKey(final Object key) {
		return super.containsKey(convertKey(key));
	}

	@Override
	public V get(final Object key) {
		return super.get(convertKey(key));
	}

	@Override
	public V put(final K key, final V value) {
		return super.put(convertKeyForStorage(key), value);
	}

	@Override
	public void putAll(final Map<? extends K, ? extends V> map) {
		for (final Entry<? extends K, ? extends V> entry : map.entrySet()) {
			put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public V remove(final Object key) {
		return super.remove(convertKey(key));
	}

	@Override
	public V computeIfAbsent(final K key, final Function<? super K, ? extends V> mappingFunction) {
		return super.computeIfAbsent(convertKeyForStorage(key), mappingFunction);
	}

	@Override
	public V computeIfPresent(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return super.computeIfPresent(convertKey(key), remappingFunction);
	}

	@Override
	public V getOrDefault(final Object key, final V defaultValue) {
		return super.getOrDefault(convertKey(key), defaultValue);
	}

	@Override
	public V putIfAbsent(final K key, final V value) {
		return super.putIfAbsent(convertKeyForStorage(key), value);
	}

	@Override
	public boolean remove(final Object key, final Object value) {
		return super.remove(convertKey(key), value);
	}

	@Override
	public V replace(final K key, final V value) {
		return super.replace(convertKey(key), value);
	}

	@Override
	public boolean replace(final K key, final V oldValue, final V newValue) {
		return super.replace(convertKey(key), oldValue, newValue);
	}

	@Override
	public V merge(final K key, final V value, final BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
		return super.merge(convertKeyForStorage(key), value, remappingFunction);
	}

	@Override
	public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return super.compute(convertKeyForStorage(key), remappingFunction);
	}

	/**
	 * Converts a key that is about to be stored in this map. Used by all methods that may insert a new key
	 * ({@code put}, {@code putIfAbsent}, {@code computeIfAbsent}, {@code compute}, {@code merge}). Subclasses can
	 * override this to reject keys that may be looked up but must never be stored.
	 *
	 * @param key key as given by the caller, may be any object or null
	 * @return the converted key used for storage, by default the result of {@link #convertKey(Object)}
	 * @throws ClassCastException if the key must not be stored in this map
	 */
	protected K convertKeyForStorage(final Object key) {
		return convertKey(key);
	}

	/**
	 * Converts a key into the form in which it is stored and looked up in this map.
	 *
	 * @param key key as given by the caller, may be any object or null
	 * @return the converted key used for storage and lookup
	 */
	protected abstract K convertKey(Object key);
}
