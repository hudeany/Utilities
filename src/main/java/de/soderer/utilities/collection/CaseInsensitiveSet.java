package de.soderer.utilities.collection;

import java.util.Collection;

/**
 * Generic String Set that ignores the String case.
 * Items are stored case-folded: each code point is mapped to {@code toLowerCase(toUpperCase(codePoint))}, so two
 * items are considered the same exactly when {@link String#equalsIgnoreCase(String)} considers them equal. For
 * most text (and always for ASCII) this is simply the lowercased item, which is what iteration returns.
 * <p>
 * Only Strings (and null) can be stored. Lookups with other types never match, storing them throws a
 * {@link ClassCastException} (only reachable via raw types or unchecked casts).
 */
public class CaseInsensitiveSet extends AbstractHashSet<String> {
	private static final long serialVersionUID = 9146520978777587374L;

	/**
	 * Creates an empty set with default initial capacity and load factor.
	 */
	public CaseInsensitiveSet() {
		super();
	}

	/**
	 * Creates an empty set with the given initial capacity and load factor.
	 *
	 * @param initialCapacity initial capacity of the set
	 * @param loadFactor load factor of the set
	 */
	public CaseInsensitiveSet(final int initialCapacity, final float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates an empty set with the given initial capacity.
	 *
	 * @param initialCapacity initial capacity of the set
	 */
	public CaseInsensitiveSet(final int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a set containing all Strings of the given collection, case-folded.
	 *
	 * @param collection Strings to add to this set
	 */
	public CaseInsensitiveSet(final Collection<? extends String> collection) {
		super(collection);
	}

	/**
	 * Creates a set containing all given Strings, case-folded.
	 *
	 * @param values Strings to add to this set
	 */
	public CaseInsensitiveSet(final String[] values) {
		super(Math.max((int) (values.length / .75f) + 1, 16));
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
			return CaseFolding.fold((String) item);
		} else {
			return NON_STRING_ITEM_SENTINEL;
		}
	}

	@Override
	protected String convertItemForStorage(final Object item) {
		// Never store the lookup sentinel: it would turn into an item that matches every non-String lookup
		if (item != null && !(item instanceof String)) {
			throw new ClassCastException("Only String items are allowed in " + getClass().getSimpleName() + ", got " + item.getClass().getName());
		}
		return convertItem(item);
	}
}
