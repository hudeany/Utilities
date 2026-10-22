package de.soderer.utilities.collection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
		// Sort the entries instead of only the keys, so no get() on the source map is needed: get() would
		// reorder an access-order LinkedHashMap and thereby change the map that is documented as unchanged
		final List<Entry<K, V>> entries = new ArrayList<>(map.entrySet());
		entries.sort(Entry.comparingByKey());
		for (final Entry<K, V> entry : entries) {
			returnMap.put(entry.getKey(), entry.getValue());
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
		// Sort the entries instead of only the keys, so no get() on the source map is needed: get() would
		// reorder an access-order LinkedHashMap and thereby change the map that is documented as unchanged
		final List<Entry<K, V>> entries = new ArrayList<>(map.entrySet());
		entries.sort(Entry.comparingByKey(comparator));
		for (final Entry<K, V> entry : entries) {
			returnMap.put(entry.getKey(), entry.getValue());
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
		final List<Entry<K, V>> entries = new ArrayList<>(map.entrySet());
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
		// Sort the entries instead of only the keys, so no get() on the source map is needed: get() would
		// reorder an access-order LinkedHashMap and thereby change the map that is documented as unchanged
		final List<Entry<K, V>> entries = new ArrayList<>(map.entrySet());
		entries.sort(Entry.comparingByKey());
		for (final Entry<K, V> entry : entries) {
			returnMap.put(entry.getKey(), entry.getValue());
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
		// Sort the entries instead of only the keys, so no get() on the source map is needed: get() would
		// reorder an access-order LinkedHashMap and thereby change the map that is documented as unchanged
		final List<Entry<K, V>> entries = new ArrayList<>(map.entrySet());
		entries.sort(Entry.comparingByKey(comparator));
		for (final Entry<K, V> entry : entries) {
			returnMap.put(entry.getKey(), entry.getValue());
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
		final List<Entry<K, V>> entries = new ArrayList<>(map.entrySet());
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
	 * Formats a map as {@code key=value} entries separated by commas, see
	 * {@link #mapToString(Map, char, boolean, char)} for the quoting rules (with double quotes as quote character).
	 *
	 * @param map map to format
	 * @return formatted map
	 */
	public static String mapToString(final Map<?, ?> map) {
		return mapToString(map, '\"', false, ',');
	}

	/**
	 * Formats a map as {@code key=value} entries separated by the given separator.
	 * <p>
	 * The output is unambiguous, so it can be parsed back:
	 * <ul>
	 * <li>Null keys and values are written as unquoted {@code null}. The String "null" is always quoted, so both
	 * can be told apart.</li>
	 * <li>Keys and values containing the quote character, the separator, {@code '='}, a backslash or a line break
	 * are enclosed in quote characters (all of them if alwaysQuote is true).</li>
	 * <li>Within quotes, backslashes are escaped as {@code \\} and quote characters as backslash + quote
	 * character. Unquoted text never contains a backslash.</li>
	 * </ul>
	 *
	 * @param map map to format
	 * @param textQuoteChar character used to quote keys and values
	 * @param alwaysQuote true to quote all non-null keys and values, false to quote only where needed
	 * @param entrySeparatorChar character between the entries
	 * @return formatted map
	 * @throws IllegalArgumentException if textQuoteChar or entrySeparatorChar is a backslash or {@code '='}, or
	 *             if both are the same character
	 */
	public static String mapToString(final Map<?, ?> map, final char textQuoteChar, final boolean alwaysQuote, final char entrySeparatorChar) {
		if (textQuoteChar == '\\' || textQuoteChar == '=' || entrySeparatorChar == '\\' || entrySeparatorChar == '=' || textQuoteChar == entrySeparatorChar) {
			throw new IllegalArgumentException("Invalid textQuoteChar '" + textQuoteChar + "' or entrySeparatorChar '" + entrySeparatorChar + "': both must differ from each other, from backslash and from '='");
		}

		final StringBuilder returnValue = new StringBuilder();
		for (final Entry<?, ?> entry : map.entrySet()) {
			if (returnValue.length() > 0) {
				returnValue.append(entrySeparatorChar);
			}
			appendMapToStringItem(returnValue, entry.getKey(), textQuoteChar, alwaysQuote, entrySeparatorChar);
			returnValue.append('=');
			appendMapToStringItem(returnValue, entry.getValue(), textQuoteChar, alwaysQuote, entrySeparatorChar);
		}
		return returnValue.toString();
	}

	private static void appendMapToStringItem(final StringBuilder builder, final Object item, final char textQuoteChar, final boolean alwaysQuote, final char entrySeparatorChar) {
		if (item == null) {
			// Never quoted, so it can be distinguished from the quoted String "null"
			builder.append("null");
			return;
		}

		final String text = item.toString();
		boolean needsQuotes = alwaysQuote || "null".equals(text);
		for (int i = 0; !needsQuotes && i < text.length(); i++) {
			final char nextChar = text.charAt(i);
			needsQuotes = nextChar == textQuoteChar || nextChar == entrySeparatorChar || nextChar == '=' || nextChar == '\\' || nextChar == '\r' || nextChar == '\n';
		}

		if (!needsQuotes) {
			builder.append(text);
		} else {
			builder.append(textQuoteChar);
			for (int i = 0; i < text.length(); i++) {
				final char nextChar = text.charAt(i);
				if (nextChar == '\\' || nextChar == textQuoteChar) {
					builder.append('\\');
				}
				builder.append(nextChar);
			}
			builder.append(textQuoteChar);
		}
	}
}
