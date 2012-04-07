package de.soderer.utilities;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CollectionUtils {
	public static <K extends Comparable<K>, V> LinkedHashMap<K, V> sort(Map<K, V> map) {
		LinkedHashMap<K, V> returnMap = new LinkedHashMap<K, V>();
		List<K> keys = new LinkedList<K>(map.keySet());
		Collections.sort(keys);
		for (K key : keys) {
			returnMap.put(key, map.get(key));
		}
		return returnMap;
	}

	public static <K, V> LinkedHashMap<K, V> sort(Map<K, V> map, Comparator<K> comparator) {
		LinkedHashMap<K, V> returnMap = new LinkedHashMap<K, V>();
		List<K> keys = new LinkedList<K>(map.keySet());
		Collections.sort(keys, comparator);
		for (K key : keys) {
			returnMap.put(key, map.get(key));
		}
		return returnMap;
	}
	
	public static <K, V> LinkedHashMap<K, V> sortEntries(Map<K, V> map, Comparator<Entry<K, V>> comparator) {
		LinkedHashMap<K, V> returnMap = new LinkedHashMap<K, V>();
		List<Entry<K, V>> entries = new LinkedList<Entry<K, V>>(map.entrySet());
		Collections.sort(entries, comparator);
		for (Entry<K, V> entry : entries) {
			returnMap.put(entry.getKey(), entry.getValue());
		}
		return returnMap;
	}

	public static <K extends Comparable<K>, V> LinkedHashMap<K, V> sort(LinkedHashMap<K, V> map) {
		@SuppressWarnings("unchecked")
		LinkedHashMap<K, V> returnMap = (LinkedHashMap<K, V>) map.clone();
		returnMap.clear();
		List<K> keys = new LinkedList<K>(map.keySet());
		Collections.sort(keys);
		for (K key : keys) {
			returnMap.put(key, map.get(key));
		}
		return returnMap;
	}
	
	public static <K, V> LinkedHashMap<K, V> sort(LinkedHashMap<K, V> map, Comparator<K> comparator) {
		@SuppressWarnings("unchecked")
		LinkedHashMap<K, V> returnMap = (LinkedHashMap<K, V>) map.clone();
		returnMap.clear();
		List<K> keys = new LinkedList<K>(map.keySet());
		Collections.sort(keys, comparator);
		for (K key : keys) {
			returnMap.put(key, map.get(key));
		}
		return returnMap;
	}
	
	public static <K, V> LinkedHashMap<K, V> sortEntries(LinkedHashMap<K, V> map, Comparator<Entry<K, V>> comparator) {
		@SuppressWarnings("unchecked")
		LinkedHashMap<K, V> returnMap = (LinkedHashMap<K, V>) map.clone();
		returnMap.clear();
		List<Entry<K, V>> entries = new LinkedList<Entry<K, V>>(map.entrySet());
		Collections.sort(entries, comparator);
		for (Entry<K, V> entry : entries) {
			returnMap.put(entry.getKey(), entry.getValue());
		}
		return returnMap;
	}
}
