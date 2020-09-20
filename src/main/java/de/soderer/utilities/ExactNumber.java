package de.soderer.utilities;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * real value = value * 10 ^ dimension
 */
public class ExactNumber {
	private long value;
	private int dimension = 0;

	public ExactNumber() {
		value = 0;
	}

	public ExactNumber(final long value) {
		this.value = value;
	}

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

		if (postDecimalSeparator.contains(".") || postDecimalSeparator.contains(",")) {
			throw new Exception("Invalid number data");
		} else if (!NumberUtilities.isInteger(preDecimalSeparator)) {
			throw new Exception("Invalid number data");
		} else if (!"".equals(postDecimalSeparator) && !NumberUtilities.isInteger(postDecimalSeparator)) {
			throw new Exception("Invalid number data");
		}

		while (preDecimalSeparator.startsWith("0")) {
			preDecimalSeparator = preDecimalSeparator.substring(1);
		}

		if ("".equals(preDecimalSeparator)) {
			preDecimalSeparator = "0";
		}

		while (postDecimalSeparator.endsWith("0")) {
			postDecimalSeparator = postDecimalSeparator.substring(0, postDecimalSeparator.length() - 2);
		}

		return new Tuple<>(Long.parseLong(preDecimalSeparator + postDecimalSeparator), postDecimalSeparator.length());
	}

	@Override
	public String toString() {
		return toString(Locale.getDefault());
	}

	public String toString(final Locale locale) {
		String returnValue = Long.toString(value);
		if (dimension > 0) {
			final char separator = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
			returnValue = returnValue.substring(0, returnValue.length() - dimension) + separator + returnValue.substring(returnValue.length() - dimension);
		}
		return returnValue;
	}

	public void add(final long valueToAdd) {
		value += valueToAdd * Math.pow(10, dimension);
	}

	public ExactNumber add(final String valueToAdd) throws Exception {
		if (!Utilities.isBlank(valueToAdd)) {
			final Tuple<Long, Integer> valueData = parseNumber(valueToAdd);
			long valueToAddValue = valueData.getFirst();
			final int valueToAddDimension = valueData.getSecond();
			if (valueToAddDimension < dimension) {
				valueToAddValue *= Math.pow(10, dimension - valueToAddDimension);
			} else if (valueToAddDimension > dimension) {
				value *= Math.pow(10, valueToAddDimension - dimension);
				dimension = valueToAddDimension;
			}
			value += valueToAddValue;
		}
		return this;
	}
}
