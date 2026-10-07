package de.soderer.utilities.collection;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Static helper methods for sorting, filtering and formatting maps.
 */
public class MapUtilities {
	private MapUtilities() {
		// Utility class with static methods only
	}

	/**
	 * Returns a new map with the entries of the given map, ordered by the natural order of their keys.
	 *
	 * @param <K> type of the keys
	 * @param <V> type of the values
	 * @param map map to sort, is not changed
	 * @return new insertion-ordered map with the sorted entries
	 */
	public static <K extends Comparable<K>, V> LinkedHashMap<K, V> sort(final Map<K, V> map) {
		final LinkedHashMap<K, V> returnMap = new LinkedHashMap<>();
		final List<K> keys = new LinkedList<>(map.keySet());
		Collections.sort(keys);
		for (final K key : keys) {
			returnMap.put(key, map.get(key));
		}
		return returnMap;
	}

	/**
	 * Returns a new map with the entries of the given map, ordered by their keys using the given comparator.
	 *
	 * @param <K> type of the keys
	 * @param <V> type of the values
	 * @param map map to sort, is not changed
	 * @param comparator comparator for the keys
	 * @return new insertion-ordered map with the sorted entries
	 */
	public static <K, V> LinkedHashMap<K, V> sort(final Map<K, V> map, final Comparator<K> comparator) {
		final LinkedHashMap<K, V> returnMap = new LinkedHashMap<>();
		final List<K> keys = new LinkedList<>(map.keySet());
		Collections.sort(keys, comparator);
		for (final K key : keys) {
			returnMap.put(key, map.get(key));
		}
		return returnMap;
	}

	/**
	 * Returns a new map with the entries of the given map, ordered using the given entry comparator.
	 *
	 * @param <K> type of the keys
	 * @param <V> type of the values
	 * @param map map to sort, is not changed
	 * @param comparator comparator for the entries
	 * @return new insertion-ordered map with the sorted entries
	 */
	public static <K, V> LinkedHashMap<K, V> sortEntries(final Map<K, V> map, final Comparator<Entry<K, V>> comparator) {
		final LinkedHashMap<K, V> returnMap = new LinkedHashMap<>();
		final List<Entry<K, V>> entries = new LinkedList<>(map.entrySet());
		Collections.sort(entries, comparator);
		for (final Entry<K, V> entry : entries) {
			returnMap.put(entry.getKey(), entry.getValue());
		}
		return returnMap;
	}

	/**
	 * Sorts the given map's entries by key and returns a new map of the same concrete type, built via
	 * {@code map.clone()}.
	 * <p>
	 * Note: if {@code map} is a {@link LinkedHashMap} constructed in access-order mode (accessOrder=true,
	 * e.g. for an LRU cache), the clone preserves that flag. The returned map will then silently reorder
	 * itself on the very next {@code get()}, discarding the sort order just established. This is fine for
	 * the common case of insertion-order maps, but is worth knowing when passing in an access-order map.
	 *
	 * @param <K> type of the keys
	 * @param <V> type of the values
	 * @param <L> concrete map type
	 * @param map map to sort, is not changed
	 * @return new map of the same type with the sorted entries
	 */
	public static <K extends Comparable<K>, V, L extends LinkedHashMap<K, V>> L sort(final L map) {
		@SuppressWarnings("unchecked")
		final
		L returnMap = (L) map.clone();
		returnMap.clear();
		final List<K> keys = new LinkedList<>(map.keySet());
		Collections.sort(keys);
		for (final K key : keys) {
			returnMap.put(key, map.get(key));
		}
		return returnMap;
	}

	/**
	 * Sorts the given map's entries using the given key comparator and returns a new map of the same concrete
	 * type, built via {@code map.clone()}.
	 * <p>
	 * Note: if {@code map} is a {@link LinkedHashMap} constructed in access-order mode (accessOrder=true,
	 * e.g. for an LRU cache), the clone preserves that flag. The returned map will then silently reorder
	 * itself on the very next {@code get()}, discarding the sort order just established. This is fine for
	 * the common case of insertion-order maps, but is worth knowing when passing in an access-order map.
	 *
	 * @param <K> type of the keys
	 * @param <V> type of the values
	 * @param <L> concrete map type
	 * @param map map to sort, is not changed
	 * @param comparator comparator for the keys
	 * @return new map of the same type with the sorted entries
	 */
	public static <K, V, L extends LinkedHashMap<K, V>> L sort(final L map, final Comparator<K> comparator) {
		@SuppressWarnings("unchecked")
		final
		L returnMap = (L) map.clone();
		returnMap.clear();
		final List<K> keys = new LinkedList<>(map.keySet());
		Collections.sort(keys, comparator);
		for (final K key : keys) {
			returnMap.put(key, map.get(key));
		}
		return returnMap;
	}

	/**
	 * Sorts the given map's entries using the given entry comparator and returns a new map of the same concrete
	 * type, built via {@code map.clone()}.
	 * <p>
	 * Note: if {@code map} is a {@link LinkedHashMap} constructed in access-order mode (accessOrder=true,
	 * e.g. for an LRU cache), the clone preserves that flag. The returned map will then silently reorder
	 * itself on the very next {@code get()}, discarding the sort order just established. This is fine for
	 * the common case of insertion-order maps, but is worth knowing when passing in an access-order map.
	 *
	 * @param <K> type of the keys
	 * @param <V> type of the values
	 * @param <L> concrete map type
	 * @param map map to sort, is not changed
	 * @param comparator comparator for the entries
	 * @return new map of the same type with the sorted entries
	 */
	public static <K, V, L extends LinkedHashMap<K, V>> L sortEntries(final L map, final Comparator<Entry<K, V>> comparator) {
		@SuppressWarnings("unchecked")
		final
		L returnMap = (L) map.clone();
		returnMap.clear();
		final List<Entry<K, V>> entries = new LinkedList<>(map.entrySet());
		Collections.sort(entries, comparator);
		for (final Entry<K, V> entry : entries) {
			returnMap.put(entry.getKey(), entry.getValue());
		}
		return returnMap;
	}

	/**
	 * Returns a new map of the same concrete type (built via {@code map.clone()}) that contains only the
	 * entries of the given map that are accepted by the given predicate.
	 *
	 * @param <K> type of the keys
	 * @param <V> type of the values
	 * @param <L> concrete map type
	 * @param map map to filter, is not changed
	 * @param predicate predicate that decides which entries are kept
	 * @return new map of the same type with the accepted entries
	 */
	public static <K, V, L extends HashMap<K, V>> L filterEntries(final L map, final Predicate<Entry<K, V>> predicate) {
		@SuppressWarnings("unchecked")
		final
		L returnMap = (L) map.clone();
		returnMap.clear();
		for (final Entry<K, V> entry : map.entrySet()) {
			if (predicate.evaluate(entry)) {
				returnMap.put(entry.getKey(), entry.getValue());
			}
		}
		return returnMap;
	}

	/**
	 * Formats a map as {@code key=value} entries separated by commas. Keys and values containing a double quote
	 * or a comma are enclosed in double quotes, contained double quotes are escaped with a backslash.
	 *
	 * @param map map to format
	 * @return formatted map
	 */
	public static String mapToString(final Map<?, ?> map) {
		return mapToString(map, '\"', false, ',');
	}

	/**
	 * Formats a map as {@code key=value} entries separated by the given separator. Null keys and values are
	 * written as {@code null}. Keys and values containing the quote or the separator character are enclosed in
	 * quote characters, contained quote characters are escaped with a backslash.
	 *
	 * @param map map to format
	 * @param textQuoteChar character used to quote keys and values
	 * @param alwaysQuote true to quote all keys and values, false to quote only where needed
	 * @param entrySeparatorChar character between the entries
	 * @return formatted map
	 */
	public static String mapToString(final Map<?, ?> map, final char textQuoteChar, final boolean alwaysQuote, final char entrySeparatorChar) {
		final StringBuilder returnValue = new StringBuilder();

		final String quoteString = Character.toString(textQuoteChar);
		final String escapedQuote = "\\" + quoteString;
		final String entrySeparatorString = Character.toString(entrySeparatorChar);

		for (final Entry<?, ?> entry : map.entrySet()) {
			if (returnValue.length() > 0) {
				returnValue.append(entrySeparatorChar);
			}

			String key;
			if (entry.getKey() == null) {
				key = "null";
			} else {
				key = entry.getKey().toString();
			}
			if (key.contains(quoteString) || key.contains(entrySeparatorString) || alwaysQuote) {
				key = quoteString + key.replace(quoteString, escapedQuote) + quoteString;
			}
			returnValue.append(key);

			returnValue.append('=');

			String value;
			if (entry.getValue() == null) {
				value = "null";
			} else {
				value = entry.getValue().toString();
			}
			if (value.contains(quoteString) || value.contains(entrySeparatorString) || alwaysQuote) {
				value = quoteString + value.replace(quoteString, escapedQuote) + quoteString;
			}
			returnValue.append(value);
		}

		return returnValue.toString();
	}
}
