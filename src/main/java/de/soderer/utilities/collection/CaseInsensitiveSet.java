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

	@Override
	protected String convertItem(final Object item) {
		return item == null ? null : item.toString().toLowerCase(Locale.ROOT);
	}
}
