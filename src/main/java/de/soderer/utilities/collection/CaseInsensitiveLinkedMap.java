package de.soderer.utilities.collection;

import java.util.Map;

/**
 * Generic String keyed Map that ignores the String case
 */
public class CaseInsensitiveLinkedMap<V> extends AbstractLinkedHashMap<String, V> {
	private static final long serialVersionUID = 6204601427841356043L;

	public static <V> CaseInsensitiveLinkedMap<V> create() {
		return new CaseInsensitiveLinkedMap<>();
	}

	public CaseInsensitiveLinkedMap() {
		super();
	}

	public CaseInsensitiveLinkedMap(final int initialCapacity, final float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public CaseInsensitiveLinkedMap(final int initialCapacity) {
		super(initialCapacity);
	}

	public CaseInsensitiveLinkedMap(final Map<? extends String, ? extends V> map) {
		super(map.size());
		putAll(map);
	}

	@Override
	protected String convertKey(final Object key) {
		return key == null ? null : key.toString().toLowerCase();
	}
}
