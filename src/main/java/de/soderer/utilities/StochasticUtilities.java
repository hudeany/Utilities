package de.soderer.utilities;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Combinatorics helpers: permutations, combinations with repetition and factorial.
 */
public class StochasticUtilities {
	private StochasticUtilities() {
		// Utility class
	}

	/**
	 * Create all permutations (orderings) of the given items.
	 * For n distinct items there are n! permutations. Items equal to each other (by equals) produce duplicate permutations.
	 *
	 * @param <T>
	 *            type of the items
	 * @param items
	 *            items to permute, may be null or empty
	 * @return all permutations, each as a new list; an empty collection for null or no items
	 */
	// The items array is only read and never stored or returned, so the varargs usage is safe
	@SafeVarargs
	public static <T> Collection<List<T>> createPermutations(final T... items) {
		if (items == null || items.length == 0) {
			return new LinkedList<>();
		} else if (items.length == 1) {
			final List<List<T>> returnList = new LinkedList<>();
			final List<T> itemList = new LinkedList<>();
			itemList.add(items[0]);
			returnList.add(itemList);
			return returnList;
		} else {
			final List<List<T>> returnList = new LinkedList<>();

			for (final T item : items) {
				// Recursive call
				for (final List<T> subItem : createPermutations(Utilities.remove(items, item))) {
					final List<T> newItem = new LinkedList<>();
					newItem.add(item);
					newItem.addAll(subItem);
					returnList.add(newItem);
				}
			}
			return returnList;
		}
	}

	/**
	 * Create all ordered combinations with repetition (variations) of the given length.
	 * For n items there are n ^ length combinations.
	 *
	 * @param <T>
	 *            type of the items
	 * @param length
	 *            number of items in each combination
	 * @param items
	 *            items to combine, may be null or empty
	 * @return all combinations, each as a new list; an empty collection for a length less than 1, null or no items
	 */
	// The items array is only read and never stored or returned, so the varargs usage is safe
	@SafeVarargs
	public static <T> Collection<List<T>> createCombinations(final int length, final T... items) {
		if (length <= 0 || items == null || items.length == 0) {
			return new LinkedList<>();
		} else if (length == 1) {
			final List<List<T>> returnList = new LinkedList<>();
			for (final T item : items) {
				final List<T> itemList = new LinkedList<>();
				itemList.add(item);
				returnList.add(itemList);
			}
			return returnList;
		} else {
			final List<List<T>> returnList = new LinkedList<>();

			for (final T item : items) {
				// Recursive call
				for (final List<T> subItem : createCombinations(length - 1, items)) {
					final List<T> newItem = new LinkedList<>();
					newItem.add(item);
					newItem.addAll(subItem);
					returnList.add(newItem);
				}
			}
			return returnList;
		}
	}

	/**
	 * Calculate the factorial n! without overflow.
	 *
	 * @param n
	 *            number, values less than 2 yield 1
	 * @return n!
	 */
	public static BigInteger factorial(final int n) {
		BigInteger fact = BigInteger.ONE;
		for (int i = n; i > 1; i--) {
			fact = fact.multiply(BigInteger.valueOf(i));
		}
		return fact;
	}
}
