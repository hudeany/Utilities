package de.soderer.utilities.collection;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class AbstractLinkedHashMap<K, V> extends LinkedHashMap<K, V> {
	private static final long serialVersionUID = -7227298515931195921L;

	public AbstractLinkedHashMap() {
		super();
	}

	public AbstractLinkedHashMap(final int initialCapacity, final float loadFactor, final boolean accessOrder) {
		super(initialCapacity, loadFactor, accessOrder);
	}

	public AbstractLinkedHashMap(final int initialCapacity, final float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public AbstractLinkedHashMap(final int initialCapacity) {
		super(initialCapacity);
	}

	public AbstractLinkedHashMap(final Map<? extends K, ? extends V> map) {
		super(map.size());
		putAll(map);
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
		return super.put(convertKey(key), value);
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
		return super.computeIfAbsent(convertKey(key), mappingFunction);
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
		return super.putIfAbsent(convertKey(key), value);
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
		return super.merge(convertKey(key), value, remappingFunction);
	}

	@Override
	public V compute(final K key, final BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
		return super.compute(convertKey(key), remappingFunction);
	}

	protected abstract K convertKey(Object key);
}
