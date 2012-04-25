package de.soderer.utilities;

import java.util.Collection;
import java.util.LinkedList;

public class UniqueFifoQueuedList<T> extends LinkedList<T> {
	private static final long serialVersionUID = 2008975571883451672L;
	
	private int size;
	
	public UniqueFifoQueuedList(int size) {
		this.size = size;
	}
	
	@Override
	public boolean add(T item) {
		if (contains(item)) {
			remove(item);
		} else if (super.size() >= size) {
			remove(0);
		}

		return super.add(item);
	}
	
	public boolean addAll(Collection<? extends T> collection) {
		for (T item : collection)
			add(item);
		return true;
	}
	
	public T getLatestAdded() {
		if (size() > 0)
			return get(size() - 1);
		else return null;
	}
}
