package de.soderer.utilities.collection;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

/**
 * Base class for HashSet variants that transform their items on the way in and out via
 * {@link #convertItem(Object)} (e.g. case-insensitive lookup by lowercasing String items).
 * <p>
 * {@link #contains(Object)}, {@link #add(Object)}, {@link #remove(Object)}, {@link #removeAll(Collection)}
 * and {@link #retainAll(Collection)} all convert the given items before comparing them with the stored ones.
 * <p>
 * {@link #equals(Object)} and {@link #hashCode()} are inherited unchanged: equals() converts the other set's items
 * via {@link #contains(Object)}, hashCode() works on the stored (converted) items. Comparing with a set that stores
 * unconverted items is therefore asymmetric and inconsistent with hashCode(): e.g.
 * {@code caseInsensitiveSet.equals(Set.of("FOO"))} can be true while {@code Set.of("FOO").equals(caseInsensitiveSet)}
 * is false and both hash codes differ (the same caveat applies to a TreeSet with a case-insensitive comparator).
 * Do not mix both kinds of sets in equality based contexts, such as keys of a map.
 *
 * @param <V> type of the items
 */
public abstract class AbstractHashSet<V> extends HashSet<V> {
	private static final long serialVersionUID = -8774751629113337123L;

	/**
	 * Creates an empty set with default initial capacity and load factor.
	 */
	public AbstractHashSet() {
		super();
	}

	/**
	 * Creates an empty set with the given initial capacity and load factor.
	 *
	 * @param initialCapacity initial capacity of the set
	 * @param loadFactor load factor of the set
	 */
	public AbstractHashSet(final int initialCapacity, final float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	/**
	 * Creates an empty set with the given initial capacity.
	 *
	 * @param initialCapacity initial capacity of the set
	 */
	public AbstractHashSet(final int initialCapacity) {
		super(initialCapacity);
	}

	/**
	 * Creates a set containing all items of the given collection, converted via {@link #convertItem(Object)}.
	 *
	 * @param collection items to add to this set
	 */
	public AbstractHashSet(final Collection<? extends V> collection) {
		super(collection.size());
		addAll(collection);
	}

	@Override
	public boolean contains(final Object item) {
		return super.contains(convertItem(item));
	}

	@Override
	public boolean add(final V item) {
		return super.add(convertItemForStorage(item));
	}

	@Override
	public boolean remove(final Object item) {
		return super.remove(convertItem(item));
	}

	@Override
	public boolean removeAll(final Collection<?> collection) {
		// Not overriding this would fall back to AbstractSet's implementation. Depending on the sizes it either
		// calls our remove() per item of "collection" (converted), or checks collection.contains(ownElement) for
		// each of our (already converted, e.g. lowercased) elements - against the *unconverted* values in
		// "collection". So the result would silently depend on the collection sizes.
		//
		// Building a converted lookup set first and then mutating only via our own iterator (instead of
		// iterating "collection" directly and calling remove(item) on ourselves inside that loop) also
		// avoids a ConcurrentModificationException when collection is this very set itself (e.g.
		// set.removeAll(set)), since iterating and mutating the same underlying set at the same time is
		// what triggers that exception.
		final java.util.Set<V> convertedItemsToRemove = new HashSet<>();
		for (final Object item : collection) {
			convertedItemsToRemove.add(convertItem(item));
		}
		boolean result = false;
		final Iterator<V> iterator = iterator();
		while (iterator.hasNext()) {
			if (convertedItemsToRemove.contains(iterator.next())) {
				iterator.remove();
				result = true;
			}
		}
		return result;
	}

	@Override
	public boolean retainAll(final Collection<?> collection) {
		// Same reasoning as removeAll(): convert the given collection's items the same way our own elements
		// are, so membership comparisons use the same normalized form instead of comparing against the
		// collection's original (unconverted) values.
		final java.util.Set<V> convertedItemsToRetain = new HashSet<>();
		for (final Object item : collection) {
			convertedItemsToRetain.add(convertItem(item));
		}
		boolean result = false;
		final Iterator<V> iterator = iterator();
		while (iterator.hasNext()) {
			if (!convertedItemsToRetain.contains(iterator.next())) {
				iterator.remove();
				result = true;
			}
		}
		return result;
	}

	/**
	 * Converts an item that is about to be stored in this set. Used by {@link #add(Object)} (and thereby by all
	 * methods based on it). Subclasses can override this to reject items that may be looked up but must never
	 * be stored.
	 *
	 * @param item item as given by the caller, may be any object or null
	 * @return the converted item used for storage, by default the result of {@link #convertItem(Object)}
	 * @throws ClassCastException if the item must not be stored in this set
	 */
	protected V convertItemForStorage(final Object item) {
		return convertItem(item);
	}

	/**
	 * Converts an item into the form in which it is stored and looked up in this set.
	 *
	 * @param item item as given by the caller, may be any object or null
	 * @return the converted item used for storage and lookup
	 */
	protected abstract V convertItem(Object item);
}
