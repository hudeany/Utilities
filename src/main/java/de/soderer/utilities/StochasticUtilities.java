package de.soderer.utilities;

import java.math.BigInteger;
import java.util.ArrayList;
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
	 * For n items there are n! permutations, also if some items are equal to each other (by equals) or null. Equal
	 * items produce duplicate permutations.
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
		} else {
			// Copied by a loop, so the varargs array is not passed on to another varargs method
			final List<T> itemList = new ArrayList<>(items.length);
			for (final T item : items) {
				itemList.add(item);
			}
			return createPermutations(itemList);
		}
	}

	/**
	 * Permutations of a non-empty list. Items are removed by their position, not by equals, so equal items and null
	 * items are handled like all other items.
	 */
	private static <T> List<List<T>> createPermutations(final List<T> items) {
		final List<List<T>> returnList = new LinkedList<>();
		if (items.size() == 1) {
			final List<T> itemList = new LinkedList<>();
			itemList.add(items.get(0));
			returnList.add(itemList);
		} else {
			for (int i = 0; i < items.size(); i++) {
				final List<T> remainingItems = new ArrayList<>(items);
				final T item = remainingItems.remove(i);
				// Recursive call
				for (final List<T> subItem : createPermutations(remainingItems)) {
					final List<T> newItem = new LinkedList<>();
					newItem.add(item);
					newItem.addAll(subItem);
					returnList.add(newItem);
				}
			}
		}
		return returnList;
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
		return createCombinationsOfArray(length, items);
	}

	/**
	 * Recursion uses a plain array parameter instead of varargs, so the array is never passed on to a varargs method
	 */
	private static <T> Collection<List<T>> createCombinationsOfArray(final int length, final T[] items) {
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

			// Computed once instead of once per item, because the shorter combinations are the same for every item
			final Collection<List<T>> subCombinations = createCombinationsOfArray(length - 1, items);
			for (final T item : items) {
				for (final List<T> subItem : subCombinations) {
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
