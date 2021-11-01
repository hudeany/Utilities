package de.soderer.utilities.collection;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class MapUtilities {
	public static <K extends Comparable<K>, V> LinkedHashMap<K, V> sort(final Map<K, V> map) {
		final LinkedHashMap<K, V> returnMap = new LinkedHashMap<>();
		final List<K> keys = new LinkedList<>(map.keySet());
		Collections.sort(keys);
		for (final K key : keys) {
			returnMap.put(key, map.get(key));
		}
		return returnMap;
	}

	public static <K, V> LinkedHashMap<K, V> sort(final Map<K, V> map, final Comparator<K> comparator) {
		final LinkedHashMap<K, V> returnMap = new LinkedHashMap<>();
		final List<K> keys = new LinkedList<>(map.keySet());
		Collections.sort(keys, comparator);
		for (final K key : keys) {
			returnMap.put(key, map.get(key));
		}
		return returnMap;
	}

	public static <K, V> LinkedHashMap<K, V> sortEntries(final Map<K, V> map, final Comparator<Entry<K, V>> comparator) {
		final LinkedHashMap<K, V> returnMap = new LinkedHashMap<>();
		final List<Entry<K, V>> entries = new LinkedList<>(map.entrySet());
		Collections.sort(entries, comparator);
		for (final Entry<K, V> entry : entries) {
			returnMap.put(entry.getKey(), entry.getValue());
		}
		return returnMap;
	}

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

	public static String mapToString(final Map<?, ?> map) {
		return mapToString(map, '\"', false, ',');
	}

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
