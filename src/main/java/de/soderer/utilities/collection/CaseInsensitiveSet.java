package de.soderer.utilities.collection;

import java.util.Collection;
import java.util.Locale;

/**
 * Generic String Set that ignores the String case
 */
public class CaseInsensitiveSet extends AbstractHashSet<String> {
	private static final long serialVersionUID = 9146520978777587374L;

	public CaseInsensitiveSet() {
		super();
	}

	public CaseInsensitiveSet(final int initialCapacity, final float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public CaseInsensitiveSet(final int initialCapacity) {
		super(initialCapacity);
	}

	public CaseInsensitiveSet(final Collection<? extends String> collection) {
		super(collection);
	}

	public CaseInsensitiveSet(final String[] values) {
		for (final String value : values) {
			add(value);
		}
	}

	/**
	 * Sentinel value used for lookups with a non-String item. It is never stored as an actual item (this set
	 * only ever stores lowercased Strings), so passing it to the underlying HashSet always reports "not found"
	 * instead of the previous behavior of converting any Object via toString(), which could cause false-positive
	 * matches (e.g. an Integer 5 matching a stored String "5").
	 */
	private static final String NON_STRING_ITEM_SENTINEL = "\u0000non-string-item-sentinel-" + java.util.UUID.randomUUID();

	@Override
	protected String convertItem(final Object item) {
		if (item == null) {
			return null;
		} else if (item instanceof String) {
			return ((String) item).toLowerCase(Locale.ROOT);
		} else {
			return NON_STRING_ITEM_SENTINEL;
		}
	}
}
