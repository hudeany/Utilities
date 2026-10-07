package de.soderer.utilities;

/**
 * Mutable group of three values, which may be null.
 * Equality is identity based (equals and hashCode are not overridden).
 *
 * @param <T1>
 *            type of the first value
 * @param <T2>
 *            type of the second value
 * @param <T3>
 *            type of the third value
 */
public class Triple<T1, T2, T3> {
	private T1 value1;
	private T2 value2;
	private T3 value3;

	/**
	 * Create a triple with all values set to null.
	 */
	public Triple() {
		value1 = null;
		value2 = null;
		value3 = null;
	}

	/**
	 * Create a triple of values.
	 *
	 * @param value1
	 *            first value, may be null
	 * @param value2
	 *            second value, may be null
	 * @param value3
	 *            third value, may be null
	 */
	public Triple(final T1 value1, final T2 value2, final T3 value3) {
		this.value1 = value1;
		this.value2 = value2;
		this.value3 = value3;
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
	 * Get the third value.
	 *
	 * @return third value, may be null
	 */
	public T3 getThird() {
		return value3;
	}

	/**
	 * Set the first value.
	 *
	 * @param value1
	 *            new first value, may be null
	 */
	public void setFirst(final T1 value1) {
		this.value1 = value1;
	}

	/**
	 * Set the second value.
	 *
	 * @param value2
	 *            new second value, may be null
	 */
	public void setSecond(final T2 value2) {
		this.value2 = value2;
	}

	/**
	 * Set the third value.
	 *
	 * @param value3
	 *            new third value, may be null
	 */
	public void setThird(final T3 value3) {
		this.value3 = value3;
	}

	/**
	 * Text representation like {@code <value1, value2, value3>}, null values are shown as {@code <null>}.
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

		returnString.append(", ");

		if (value3 != null) {
			returnString.append(value3.toString());
		} else {
			returnString.append("<null>");
		}

		returnString.append(">");
		return returnString.toString();
	}
}
