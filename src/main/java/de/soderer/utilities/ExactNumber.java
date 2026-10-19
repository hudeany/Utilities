package de.soderer.utilities;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Decimal number without floating point rounding errors, stored as a long value and a number of decimal places.
 *
 * <p>real value = value * 10 ^ (-dimension)</p>
 *
 * <p>The number is always kept normalized (no trailing zeros in the decimal places), so equal numbers have equal
 * text representations and are {@link #equals(Object) equal}.</p>
 *
 * <p>Calculations throw an {@link ArithmeticException} if the internal long value overflows.</p>
 */
public class ExactNumber {
	/** Arabic decimal separator, used by the arabic locales of the JDK. */
	private static final char ARABIC_DECIMAL_SEPARATOR = '٫';

	private long value;
	private int dimension = 0;

	/**
	 * Create the number 0.
	 */
	public ExactNumber() {
		value = 0;
	}

	/**
	 * Create an integer number.
	 *
	 * @param value
	 *            integer value
	 */
	public ExactNumber(final long value) {
		this.value = value;
	}

	/**
	 * Parse a decimal number. '.', ',' or the arabic decimal separator U+066B is accepted as decimal separator, so the
	 * text of {@link #toString(Locale)} can be parsed for every locale. Grouping separators are not allowed. Blanks and
	 * tabs are ignored. The integer part or the decimal places may be omitted (".5", "5."), but not both.
	 *
	 * @param valueString
	 *            number text like "-12.50", blank or null for 0
	 * @throws Exception
	 *             if the text is no valid decimal number
	 */
	public ExactNumber(final String valueString) throws Exception {
		if (Utilities.isBlank(valueString)) {
			value = 0;
		} else {
			final Tuple<Long, Integer> data = parseNumber(valueString);
			value = data.getFirst();
			dimension = data.getSecond();
		}
	}

	private static Tuple<Long, Integer> parseNumber(final String valueString) throws Exception {
		final String value = valueString.replace(" ", "").replace("\t", "");

		String preDecimalSeparator;
		String postDecimalSeparator;

		final int separatorIndex = indexOfDecimalSeparator(value);
		if (separatorIndex >= 0) {
			preDecimalSeparator = value.substring(0, separatorIndex);
			postDecimalSeparator = value.substring(separatorIndex + 1);
		} else {
			preDecimalSeparator = value;
			postDecimalSeparator = "";
		}

		final boolean isNegative = preDecimalSeparator.startsWith("-");
		if (isNegative || preDecimalSeparator.startsWith("+")) {
			preDecimalSeparator = preDecimalSeparator.substring(1);
		}

		// Only plain digits are allowed in both parts, a second decimal separator is no digit either
		if (preDecimalSeparator.isEmpty() && postDecimalSeparator.isEmpty()) {
			throw new Exception("Invalid number data, no digits: '" + valueString + "'");
		} else if (!isAsciiDigits(preDecimalSeparator) || !isAsciiDigits(postDecimalSeparator)) {
			throw new Exception("Invalid number data: '" + valueString + "'");
		}

		while (preDecimalSeparator.startsWith("0")) {
			preDecimalSeparator = preDecimalSeparator.substring(1);
		}

		if ("".equals(preDecimalSeparator)) {
			preDecimalSeparator = "0";
		}

		while (postDecimalSeparator.endsWith("0")) {
			postDecimalSeparator = postDecimalSeparator.substring(0, postDecimalSeparator.length() - 1);
		}

		try {
			return new Tuple<>(Long.parseLong((isNegative ? "-" : "") + preDecimalSeparator + postDecimalSeparator), postDecimalSeparator.length());
		} catch (final NumberFormatException e) {
			throw new Exception("Invalid number data, value out of range: '" + valueString + "'", e);
		}
	}

	/**
	 * Returns the index of the first decimal separator ('.', ',' or U+066B).
	 *
	 * @param value
	 *            number text
	 * @return index, or -1 if there is no decimal separator
	 */
	private static int indexOfDecimalSeparator(final String value) {
		for (int i = 0; i < value.length(); i++) {
			final char character = value.charAt(i);
			if (character == '.' || character == ',' || character == ARABIC_DECIMAL_SEPARATOR) {
				return i;
			}
		}
		return -1;
	}

	private static boolean isAsciiDigits(final String value) {
		for (int i = 0; i < value.length(); i++) {
			final char character = value.charAt(i);
			if (character < '0' || character > '9') {
				return false;
			}
		}
		return true;
	}

	/**
	 * Removes trailing zeros of the decimal places, so equal numbers have the same internal representation.
	 */
	private void normalize() {
		while (dimension > 0 && value % 10 == 0) {
			value /= 10;
			dimension--;
		}
	}

	/**
	 * Text representation with the decimal separator of the default format locale.
	 */
	@Override
	public String toString() {
		return toString(Locale.getDefault(Locale.Category.FORMAT));
	}

	/**
	 * Text representation with the decimal separator of the given locale and without grouping separators. Digits are
	 * always ASCII digits.
	 *
	 * @param locale
	 *            locale defining the decimal separator
	 * @return number text like "-0.05" (or "-0,05" for german locale)
	 */
	public String toString(final Locale locale) {
		if (dimension > 0) {
			// BigDecimal handles values with less digits than decimal places (like 0.05) and negative values correctly
			final String plainValue = toBigDecimal().toPlainString();
			final char separator = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
			return plainValue.replace('.', separator);
		} else {
			return Long.toString(value);
		}
	}

	/**
	 * Value as BigDecimal.
	 *
	 * @return the exact value
	 */
	public BigDecimal toBigDecimal() {
		return BigDecimal.valueOf(value, dimension);
	}

	/**
	 * Add an integer value to this number.
	 *
	 * @param valueToAdd
	 *            integer value to add
	 * @return this number for chaining
	 * @throws ArithmeticException
	 *             if the result overflows the internal long value, this number is unchanged then
	 */
	public ExactNumber add(final long valueToAdd) {
		value = Math.addExact(value, Math.multiplyExact(valueToAdd, powerOfTen(dimension)));
		normalize();
		return this;
	}

	/**
	 * Add a decimal number to this number.
	 *
	 * @param valueToAdd
	 *            decimal number text (see {@link #ExactNumber(String)}), blank or null adds nothing
	 * @return this number for chaining
	 * @throws Exception
	 *             if the text is no valid decimal number
	 * @throws ArithmeticException
	 *             if the result overflows the internal long value, this number is unchanged then
	 */
	public ExactNumber add(final String valueToAdd) throws Exception {
		if (!Utilities.isBlank(valueToAdd)) {
			final Tuple<Long, Integer> valueData = parseNumber(valueToAdd);
			long valueToAddValue = valueData.getFirst();
			final int valueToAddDimension = valueData.getSecond();

			// Calculate in local variables first, so an overflow leaves this number unchanged
			long newValue = value;
			int newDimension = dimension;
			if (valueToAddDimension < dimension) {
				valueToAddValue = Math.multiplyExact(valueToAddValue, powerOfTen(dimension - valueToAddDimension));
			} else if (valueToAddDimension > dimension) {
				newValue = Math.multiplyExact(value, powerOfTen(valueToAddDimension - dimension));
				newDimension = valueToAddDimension;
			}
			value = Math.addExact(newValue, valueToAddValue);
			dimension = newDimension;
			normalize();
		}
		return this;
	}

	private static long powerOfTen(final int exponent) {
		long result = 1;
		for (int i = 0; i < exponent; i++) {
			result = Math.multiplyExact(result, 10L);
		}
		return result;
	}

	/**
	 * Two numbers are equal, if they have the same value, regardless of how they were written (e.g. "1.5" and "1.50").
	 */
	@Override
	public boolean equals(final Object other) {
		if (this == other) {
			return true;
		} else if (!(other instanceof ExactNumber)) {
			return false;
		} else {
			final ExactNumber otherNumber = (ExactNumber) other;
			return value == otherNumber.value && dimension == otherNumber.dimension;
		}
	}

	@Override
	public int hashCode() {
		return 31 * Long.hashCode(value) + dimension;
	}
}
