package de.soderer.utilities;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Decimal number without floating point rounding errors, stored as a long value and a number of decimal places.
 *
 * <p>real value = value * 10 ^ (-dimension)</p>
 *
 * <p>Calculations throw an {@link ArithmeticException} if the internal long value overflows.</p>
 */
public class ExactNumber {
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
	 * Parse a decimal number. Either '.' or ',' is accepted as decimal separator, grouping separators are not allowed.
	 * Blanks and tabs are ignored.
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

	private static Tuple<Long, Integer> parseNumber(String value) throws Exception {
		value = value.replace(" ", "").replace("\t", "");

		String preDecimalSeparator;
		String postDecimalSeparator;

		if (value.contains(".")) {
			preDecimalSeparator = value.substring(0, value.indexOf("."));
			postDecimalSeparator = value.substring(value.indexOf(".") + 1);
		} else if (value.contains(",")) {
			preDecimalSeparator = value.substring(0, value.indexOf(","));
			postDecimalSeparator = value.substring(value.indexOf(",") + 1);
		} else {
			preDecimalSeparator = value;
			postDecimalSeparator = "";
		}

		final boolean isNegative = preDecimalSeparator.startsWith("-");
		if (isNegative || preDecimalSeparator.startsWith("+")) {
			preDecimalSeparator = preDecimalSeparator.substring(1);
		}

		// Only plain digits are allowed in both parts (before, signs within the decimals like "1.-5" were accepted, and
		// decimals were limited to the int range by NumberUtilities.isInteger)
		if (postDecimalSeparator.contains(".") || postDecimalSeparator.contains(",")) {
			throw new Exception("Invalid number data");
		} else if (preDecimalSeparator.isEmpty()) {
			throw new Exception("Invalid number data");
		} else if (!isAsciiDigits(preDecimalSeparator) || !isAsciiDigits(postDecimalSeparator)) {
			throw new Exception("Invalid number data");
		}

		while (preDecimalSeparator.startsWith("0")) {
			preDecimalSeparator = preDecimalSeparator.substring(1);
		}

		if ("".equals(preDecimalSeparator)) {
			preDecimalSeparator = "0";
		}

		while (postDecimalSeparator.endsWith("0")) {
			// Remove one trailing zero (before, two characters were removed, so "1.50" became 1 instead of 1.5)
			postDecimalSeparator = postDecimalSeparator.substring(0, postDecimalSeparator.length() - 1);
		}

		try {
			return new Tuple<>(Long.parseLong((isNegative ? "-" : "") + preDecimalSeparator + postDecimalSeparator), postDecimalSeparator.length());
		} catch (final NumberFormatException e) {
			throw new Exception("Invalid number data: value out of range", e);
		}
	}

	private static boolean isAsciiDigits(final String value) {
		for (final char character : value.toCharArray()) {
			if (character < '0' || character > '9') {
				return false;
			}
		}
		return true;
	}

	/**
	 * Text representation with the decimal separator of the default locale.
	 */
	@Override
	public String toString() {
		return toString(Locale.getDefault());
	}

	/**
	 * Text representation with the decimal separator of the given locale and without grouping separators.
	 *
	 * @param locale
	 *            locale defining the decimal separator
	 * @return number text like "-0.05" (or "-0,05" for german locale)
	 */
	public String toString(final Locale locale) {
		if (dimension > 0) {
			// BigDecimal handles values with less digits than decimal places (like 0.05) and negative values correctly
			final String plainValue = BigDecimal.valueOf(value, dimension).toPlainString();
			final char separator = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
			return plainValue.replace('.', separator);
		} else {
			return Long.toString(value);
		}
	}

	/**
	 * Add an integer value to this number.
	 *
	 * @param valueToAdd
	 *            integer value to add
	 * @throws ArithmeticException
	 *             if the result overflows the internal long value
	 */
	public void add(final long valueToAdd) {
		// Exact integer arithmetic (before, Math.pow used double arithmetic, losing precision for large values)
		value = Math.addExact(value, Math.multiplyExact(valueToAdd, powerOfTen(dimension)));
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
	 *             if the result overflows the internal long value
	 */
	public ExactNumber add(final String valueToAdd) throws Exception {
		if (!Utilities.isBlank(valueToAdd)) {
			final Tuple<Long, Integer> valueData = parseNumber(valueToAdd);
			long valueToAddValue = valueData.getFirst();
			final int valueToAddDimension = valueData.getSecond();
			if (valueToAddDimension < dimension) {
				valueToAddValue = Math.multiplyExact(valueToAddValue, powerOfTen(dimension - valueToAddDimension));
			} else if (valueToAddDimension > dimension) {
				value = Math.multiplyExact(value, powerOfTen(valueToAddDimension - dimension));
				dimension = valueToAddDimension;
			}
			value = Math.addExact(value, valueToAddValue);
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
}
