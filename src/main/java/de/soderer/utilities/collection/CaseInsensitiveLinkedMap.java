package de.soderer.utilities.collection;

import java.util.Locale;
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

	public CaseInsensitiveLinkedMap(final int initialCapacity, final float loadFactor, final boolean accessOrder) {
		super(initialCapacity, loadFactor, accessOrder);
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

	/**
	 * Sentinel value used for lookups with a non-String key. It is never stored as an actual key (this map
	 * only ever stores lowercased Strings), so passing it to the underlying LinkedHashMap always reports
	 * "not found" instead of the previous behavior of converting any Object via toString(), which could cause
	 * false-positive matches (e.g. an Integer key 5 matching a stored String key "5").
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
