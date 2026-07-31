package de.soderer.utilities.collection;

import java.util.Collection;
import java.util.LinkedList;

public class UniqueFifoQueuedList<T> extends LinkedList<T> {
	private static final long serialVersionUID = 2008975571883451672L;

	private int maxSize = -1;

	/**
	 * No length of queue set means the elements are only stored uniquely and double storage puts the item to the end. Thats the same effect as an infinite queue length.
	 */
	public UniqueFifoQueuedList() {
	}

	public UniqueFifoQueuedList(final int maxSize) {
		this(maxSize, null);
	}

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

	public T getLatestAdded() {
		if (size() > 0) {
			return get(size() - 1);
		} else {
			return null;
		}
	}
}
