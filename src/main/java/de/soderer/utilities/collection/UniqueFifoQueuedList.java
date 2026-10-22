package de.soderer.utilities.collection;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Objects;

/**
 * List that keeps each item only once, in the order of its latest addition, optionally limited to a maximum
 * size. Adding an item that is already contained moves it to the end. When the maximum size is reached, the
 * oldest items at the front are dropped (e.g. for "recently used" lists).
 * <p>
 * Only {@link #add(Object)}, {@link #addAll(Collection)} and the methods based on them (like {@code offer})
 * apply these rules. Inserting via {@code add(int, Object)}, {@code addAll(int, Collection)}, {@code addFirst},
 * {@code addLast}, {@code push}, {@code offerFirst}, {@code offerLast}, {@code set}, {@code listIterator().add()}
 * or the {@code reversed()} view (Java 21+) bypasses them. Duplicates and an exceeded maximum size caused that way
 * are cleaned up by the next call of {@link #add(Object)}.
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
	 * @throws IllegalArgumentException if maxSize is less than -1
	 */
	public UniqueFifoQueuedList(final int maxSize) {
		this(maxSize, null);
	}

	/**
	 * Creates a list with the given maximum size, filled with the given items in their iteration order.
	 *
	 * @param maxSize maximum number of items to keep, -1 for no limit
	 * @param initialContent items to add initially, may be null
	 * @throws IllegalArgumentException if maxSize is less than -1
	 */
	public UniqueFifoQueuedList(final int maxSize, final Collection<T> initialContent) {
		if (maxSize < -1) {
			throw new IllegalArgumentException("Invalid maxSize " + maxSize + ": must be -1 (no limit) or greater or equal 0");
		}
		this.maxSize = maxSize;
		if (initialContent != null) {
			addAll(initialContent);
		}
	}

	/**
	 * Appends the item as the latest one. Any existing occurrences of it are removed first, and afterwards the
	 * oldest items at the front are dropped until the maximum size is kept again.
	 *
	 * @param item item to add, may be null
	 * @return true, or false if maxSize is 0 and therefore nothing is retained
	 */
	@Override
	public boolean add(final T item) {
		if (maxSize == 0) {
			// A cap of zero means nothing is ever actually retained
			return false;
		}

		// Remove all occurrences (not only the first one), in case duplicates were inserted via a bypassing method
		removeIf(existingItem -> Objects.equals(existingItem, item));
		super.add(item);

		// Trim after adding (instead of making room before), which also repairs an exceeded maximum size that was
		// caused by a bypassing method
		if (maxSize > -1) {
			while (size() > maxSize) {
				removeFirst();
			}
		}
		return true;
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
