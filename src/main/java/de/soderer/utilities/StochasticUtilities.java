package de.soderer.utilities;

import java.math.BigInteger;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

public class StochasticUtilities {
	public static <T> Collection<List<T>> createPermutations(@SuppressWarnings("unchecked") T... items) {
		if (items == null || items.length == 0) {
			return new LinkedList<>();
		} else if (items.length == 1) {
			List<List<T>> returnList = new LinkedList<>();
			List<T> itemList = new LinkedList<>();
			itemList.add(items[0]);
			returnList.add(itemList);
			return returnList;
		} else {
			List<List<T>> returnList = new LinkedList<>();

			for (T item : items) {
				// Recursive call
				for (List<T> subItem : createPermutations(Utilities.remove(items, item))) {
					List<T> newItem = new LinkedList<>();
					newItem.add(item);
					for (T part : subItem) {
						newItem.add(part);
					}
					returnList.add(newItem);
				}
			}
			return returnList;
		}
	}

	public static <T> Collection<List<T>> createCombinations(int length, @SuppressWarnings("unchecked") T... items) {
		if (length <= 0 || items == null || items.length == 0) {
			return new LinkedList<>();
		} else if (length == 1) {
			List<List<T>> returnList = new LinkedList<>();
			for (T item : items) {
				List<T> itemList = new LinkedList<>();
				itemList.add(item);
				returnList.add(itemList);
			}
			return returnList;
		} else {
			List<List<T>> returnList = new LinkedList<>();

			for (T item : items) {
				// Recursive call
				for (List<T> subItem : createCombinations(length - 1, items)) {
					List<T> newItem = new LinkedList<>();
					newItem.add(item);
					for (T part : subItem) {
						newItem.add(part);
					}
					returnList.add(newItem);
				}
			}
			return returnList;
		}
	}

	public static BigInteger factorial(int n) {
		BigInteger fact = BigInteger.ONE;
		for (int i = n; i > 1; i--) {
			fact = fact.multiply(new BigInteger(Integer.toString(i)));
		}
		return fact;
	}
}
