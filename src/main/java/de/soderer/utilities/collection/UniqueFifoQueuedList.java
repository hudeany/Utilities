package de.soderer.utilities.collection;

import java.util.Collection;
import java.util.LinkedList;

/**
 * List that keeps each item only once, in the order of its latest addition, optionally limited to a maximum
 * size. Adding an item that is already contained moves it to the end. When the maximum size is reached, the
 * oldest items at the front are dropped (e.g. for "recently used" lists).
 * <p>
 * Only {@link #add(Object)}, {@link #addAll(Collection)} and the methods based on them (like {@code offer})
 * apply these rules. Inserting via {@code add(int, Object)}, {@code addFirst}, {@code addLast}, {@code push},
 * {@code offerFirst}, {@code offerLast} or {@code set} bypasses them.
 *
 * @param <T> type of the items
 */
public class UniqueFifoQueuedList<T> extends LinkedList<T> {
	private static final long serialVersionUID = 2008975571883451672L;

	/** Maximum number of items to keep, -1 for no limit */
	private int maxSize = -1;

	/**
	 * Creates an empty list without size limit. Items are only stored uniquely and adding an item again moves it to the end. That's the same effect as an infinite queue length.
	 */
	public UniqueFifoQueuedList() {
	}

	/**
	 * Creates an empty list with the given maximum size.
	 *
	 * @param maxSize maximum number of items to keep, -1 for no limit
	 */
	public UniqueFifoQueuedList(final int maxSize) {
		this(maxSize, null);
	}

	/**
	 * Creates a list with the given maximum size, filled with the given items in their iteration order.
	 *
	 * @param maxSize maximum number of items to keep, -1 for no limit
	 * @param initialContent items to add initially, may be null
	 */
	public UniqueFifoQueuedList(final int maxSize, final Collection<T> initialContent) {
		this.maxSize = maxSize;
		if (initialContent != null) {
			addAll(initialContent);
		}
	}

	@Override
	public boolean add(final T item) {
		if (contains(item)) {
			remove(item);
		} else if (maxSize > -1) {
			// Evict from the front until there is room for exactly one more entry. Using while (not just a
			// single eviction) also correctly handles maxSize == 0: previously the eviction condition required
			// the list to be non-empty first, so the very first add() always slipped one entry in regardless
			// of the cap, and the list then behaved like maxSize == 1 forever instead of maxSize == 0.
			while (super.size() >= maxSize && super.size() > 0) {
				remove(0);
			}
			if (maxSize == 0) {
				// A cap of zero means nothing is ever actually retained
				return false;
			}
		}

		return super.add(item);
	}

	@Override
	public boolean addAll(final Collection<? extends T> collection) {
		boolean modified = false;
		for (final T item : collection) {
			if (add(item)) {
				modified = true;
			}
		}
		return modified;
	}

	/**
	 * Returns the item that was added last.
	 *
	 * @return last item of this list, or null if the list is empty
	 */
	public T getLatestAdded() {
		if (size() > 0) {
			return get(size() - 1);
		} else {
			return null;
		}
	}
}
