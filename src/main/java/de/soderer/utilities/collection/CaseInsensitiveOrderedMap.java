package de.soderer.utilities.collection;

import java.util.Locale;
import java.util.Map;

/**
 * Generic String keyed Map that ignores the String case but keeps the order of key entries
 */
public class CaseInsensitiveOrderedMap<V> extends AbstractLinkedHashMap<String, V> {
	private static final long serialVersionUID = 7467218114617744333L;

	public static <V> CaseInsensitiveOrderedMap<V> create() {
		return new CaseInsensitiveOrderedMap<>();
	}

	public CaseInsensitiveOrderedMap() {
		super();
	}

	public CaseInsensitiveOrderedMap(final int initialCapacity, final float loadFactor, final boolean accessOrder) {
		super(initialCapacity, loadFactor, accessOrder);
	}

	public CaseInsensitiveOrderedMap(final int initialCapacity, final float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public CaseInsensitiveOrderedMap(final int initialCapacity) {
		super(initialCapacity);
	}

	public CaseInsensitiveOrderedMap(final Map<? extends String, ? extends V> map) {
		super(map.size());
		putAll(map);
	}

	@Override
	protected String convertKey(final Object key) {
		return key == null ? null : key.toString().toLowerCase(Locale.ROOT);
	}
}
