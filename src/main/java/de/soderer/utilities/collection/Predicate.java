package de.soderer.utilities.collection;

/**
 * Condition that decides whether an object is accepted, e.g. by {@link MapUtilities#filterEntries}.
 *
 * @param <K> type of the objects to evaluate
 */
public abstract class Predicate<K> {
	/**
	 * Constructor for subclasses.
	 */
	protected Predicate() {
		// Nothing to initialize
	}

	/**
	 * Checks the given object against this condition.
	 *
	 * @param object object to check
	 * @return true if the object is accepted
	 */
	public abstract boolean evaluate(K object);
}