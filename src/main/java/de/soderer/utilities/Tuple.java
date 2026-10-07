package de.soderer.utilities;

/**
 * Immutable pair of two values, which may be null.
 * Equality is identity based (equals and hashCode are not overridden).
 *
 * @param <T1>
 *            type of the first value
 * @param <T2>
 *            type of the second value
 */
public class Tuple<T1, T2> {
	private final T1 value1;
	private final T2 value2;

	/**
	 * Create a pair of values.
	 *
	 * @param value1
	 *            first value, may be null
	 * @param value2
	 *            second value, may be null
	 */
	public Tuple(final T1 value1, final T2 value2) {
		this.value1 = value1;
		this.value2 = value2;
	}

	/**
	 * Get the first value.
	 *
	 * @return first value, may be null
	 */
	public T1 getFirst() {
		return value1;
	}

	/**
	 * Get the second value.
	 *
	 * @return second value, may be null
	 */
	public T2 getSecond() {
		return value2;
	}

	/**
	 * Text representation like {@code <value1, value2>}, null values are shown as {@code <null>}.
	 */
	@Override
	public String toString() {
		final StringBuilder returnString = new StringBuilder("<");

		if (value1 != null) {
			returnString.append(value1.toString());
		} else {
			returnString.append("<null>");
		}

		returnString.append(", ");

		if (value2 != null) {
			returnString.append(value2.toString());
		} else {
			returnString.append("<null>");
		}

		returnString.append(">");
		return returnString.toString();
	}
}
