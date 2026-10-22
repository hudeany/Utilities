package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class NumberUtilitiesTest {
	@Test
	public void testDecimalFormatConstants() {
		// The constants use the symbols of the default locale at class initialization, which may have other digits than '0' - '9'
		assertEquals(localized("07", NumberUtilities.NUMBER_WITH_MIN_2_DIGITS), NumberUtilities.NUMBER_WITH_MIN_2_DIGITS.format(7));
		assertEquals(localized("123", NumberUtilities.NUMBER_WITH_MIN_2_DIGITS), NumberUtilities.NUMBER_WITH_MIN_2_DIGITS.format(123));
		assertEquals(localized("0007", NumberUtilities.NUMBER_WITH_MIN_4_DIGITS), NumberUtilities.NUMBER_WITH_MIN_4_DIGITS.format(7));
		assertEquals(localized("000007", NumberUtilities.NUMBER_WITH_MIN_6_DIGITS), NumberUtilities.NUMBER_WITH_MIN_6_DIGITS.format(7));
		assertEquals(localized("0000007", NumberUtilities.NUMBER_WITH_MIN_7_DIGITS), NumberUtilities.NUMBER_WITH_MIN_7_DIGITS.format(7));

		final char groupingSeparator = NumberUtilities.NUMBER_WITH_POINTS.getDecimalFormatSymbols().getGroupingSeparator();
		assertEquals(localized("1" + groupingSeparator + "234" + groupingSeparator + "567", NumberUtilities.NUMBER_WITH_POINTS), NumberUtilities.NUMBER_WITH_POINTS.format(1234567));
		assertEquals(localized("0", NumberUtilities.NUMBER_WITH_POINTS), NumberUtilities.NUMBER_WITH_POINTS.format(0));
	}

	/**
	 * Replaces the ASCII digits of a text by the digits of a DecimalFormat.
	 */
	private static String localized(final String text, final DecimalFormat decimalFormat) {
		final char zeroDigit = decimalFormat.getDecimalFormatSymbols().getZeroDigit();
		final StringBuilder result = new StringBuilder();
		for (final char character : text.toCharArray()) {
			result.append(character >= '0' && character <= '9' ? (char) (zeroDigit + character - '0') : character);
		}
		return result.toString();
	}

	@Test
	public void testIsDigitString() {
		assertTrue(NumberUtilities.isDigit("0123456789"));
		assertTrue(NumberUtilities.isDigit(""));
		assertFalse(NumberUtilities.isDigit((String) null));
		assertFalse(NumberUtilities.isDigit("12a"));
		assertFalse(NumberUtilities.isDigit("-1"));
		assertFalse(NumberUtilities.isDigit("1.0"));
		assertFalse(NumberUtilities.isDigit(" 1"));
		// Arabic-Indic and fullwidth digits are no ASCII digits
		assertFalse(NumberUtilities.isDigit("\u0661\u0662"));
		assertFalse(NumberUtilities.isDigit("\uFF11"));
	}

	@Test
	public void testIsDigitChar() {
		for (char c = '0'; c <= '9'; c++) {
			assertTrue(NumberUtilities.isDigit(c), "Character " + c);
		}
		assertFalse(NumberUtilities.isDigit('/'));
		assertFalse(NumberUtilities.isDigit(':'));
		assertFalse(NumberUtilities.isDigit('a'));
		assertFalse(NumberUtilities.isDigit('\u0661'));
	}

	@Test
	public void testIsIntegerString() {
		assertTrue(NumberUtilities.isInteger("0"));
		assertTrue(NumberUtilities.isInteger("-123"));
		assertTrue(NumberUtilities.isInteger("+123"));
		assertTrue(NumberUtilities.isInteger("2147483647"));
		assertTrue(NumberUtilities.isInteger("-2147483648"));
		assertFalse(NumberUtilities.isInteger("2147483648"));
		assertFalse(NumberUtilities.isInteger("-2147483649"));
		assertFalse(NumberUtilities.isInteger("1.0"));
		assertFalse(NumberUtilities.isInteger(" 1"));
		assertFalse(NumberUtilities.isInteger(""));
		assertFalse(NumberUtilities.isInteger("abc"));
		assertFalse(NumberUtilities.isInteger((String) null));
	}

	@Test
	public void testIsLong() {
		assertTrue(NumberUtilities.isLong("2147483648"));
		assertTrue(NumberUtilities.isLong("9223372036854775807"));
		assertTrue(NumberUtilities.isLong("-9223372036854775808"));
		assertFalse(NumberUtilities.isLong("9223372036854775808"));
		assertFalse(NumberUtilities.isLong("1.5"));
		assertFalse(NumberUtilities.isLong(""));
		assertFalse(NumberUtilities.isLong(null));
	}

	@Test
	public void testIsBigInteger() {
		assertTrue(NumberUtilities.isBigInteger("0"));
		assertTrue(NumberUtilities.isBigInteger("-1"));
		assertTrue(NumberUtilities.isBigInteger("+1"));
		assertTrue(NumberUtilities.isBigInteger("123456789012345678901234567890"));
		// Like BigInteger itself, non-ASCII Unicode digits are accepted
		assertTrue(NumberUtilities.isBigInteger("\u0661\u0662\u0663"));
		assertEquals(new BigInteger("123"), new BigInteger("\u0661\u0662\u0663"));

		assertFalse(NumberUtilities.isBigInteger(null));
		assertFalse(NumberUtilities.isBigInteger(""));
		assertFalse(NumberUtilities.isBigInteger("+"));
		assertFalse(NumberUtilities.isBigInteger("-"));
		assertFalse(NumberUtilities.isBigInteger("--1"));
		assertFalse(NumberUtilities.isBigInteger("1-"));
		assertFalse(NumberUtilities.isBigInteger("1.0"));
		assertFalse(NumberUtilities.isBigInteger(" 1"));
		assertFalse(NumberUtilities.isBigInteger("1e3"));
	}

	@Test
	public void testIsBigIntegerWithVeryLongValue() {
		final String longValue = "9".repeat(1_000_000);
		assertTrue(NumberUtilities.isBigInteger(longValue));
		assertFalse(NumberUtilities.isBigInteger(longValue + "x"));
	}

	@Test
	public void testIsDouble() {
		assertTrue(NumberUtilities.isDouble("1"));
		assertTrue(NumberUtilities.isDouble("-1.5e-3"));
		assertTrue(NumberUtilities.isDouble(".5"));
		// Everything accepted by Double.parseDouble
		assertTrue(NumberUtilities.isDouble(" 1.5 "));
		assertTrue(NumberUtilities.isDouble("NaN"));
		assertTrue(NumberUtilities.isDouble("-Infinity"));
		assertTrue(NumberUtilities.isDouble("1d"));
		assertTrue(NumberUtilities.isDouble("1f"));
		assertTrue(NumberUtilities.isDouble("0x1.8p1"));

		assertFalse(NumberUtilities.isDouble(null));
		assertFalse(NumberUtilities.isDouble(""));
		assertFalse(NumberUtilities.isDouble("1,5"));
		assertFalse(NumberUtilities.isDouble("abc"));
		assertFalse(NumberUtilities.isDouble("."));
	}

	@Test
	public void testIsNumber() {
		assertTrue(NumberUtilities.isNumber("0"));
		assertTrue(NumberUtilities.isNumber("-12"));
		assertTrue(NumberUtilities.isNumber("+12.50"));
		assertTrue(NumberUtilities.isNumber("12."));
		assertTrue(NumberUtilities.isNumber(".5"));
		assertTrue(NumberUtilities.isNumber("-12.5E-3"));
		assertTrue(NumberUtilities.isNumber("1e10"));
		assertTrue(NumberUtilities.isNumber("1E+10"));

		assertFalse(NumberUtilities.isNumber(null));
		assertFalse(NumberUtilities.isNumber(""));
		assertFalse(NumberUtilities.isNumber("."));
		assertFalse(NumberUtilities.isNumber("+"));
		assertFalse(NumberUtilities.isNumber("1e"));
		assertFalse(NumberUtilities.isNumber("e5"));
		assertFalse(NumberUtilities.isNumber("1.2.3"));
		assertFalse(NumberUtilities.isNumber("1,000"));
		assertFalse(NumberUtilities.isNumber(" 1"));
		assertFalse(NumberUtilities.isNumber("NaN"));
		assertFalse(NumberUtilities.isNumber("Infinity"));
		assertFalse(NumberUtilities.isNumber("1d"));
		assertFalse(NumberUtilities.isNumber("0x1F"));
		assertFalse(NumberUtilities.isNumber("\u0661"));
	}

	@Test
	public void testCompareMixedTypes() {
		assertEquals(0, NumberUtilities.compare(1, 1L));
		assertEquals(0, NumberUtilities.compare(1, 1.0));
		assertEquals(0, NumberUtilities.compare(new BigDecimal("1.000"), 1));
		assertEquals(0, NumberUtilities.compare(BigInteger.TEN, 10.0f));
		assertEquals(0, NumberUtilities.compare((byte) 5, (short) 5));
		assertEquals(-1, NumberUtilities.compare(1, 2L));
		assertEquals(1, NumberUtilities.compare(2.5, 2));
		assertEquals(-1, NumberUtilities.compare(-0.5f, 0));
		assertEquals(0, NumberUtilities.compare(new AtomicLong(42), 42));
	}

	@Test
	public void testCompareUsesDecimalRepresentationOfFloatingPointValues() {
		// The binary values of 0.1f and 0.1 differ, but both are written as 0.1
		assertEquals(0, NumberUtilities.compare(0.1f, 0.1));
		assertEquals(0, NumberUtilities.compare(0.1, new BigDecimal("0.1")));
		// 0.1 + 0.2 is written as 0.30000000000000004
		assertEquals(1, NumberUtilities.compare(0.1 + 0.2, 0.3));
	}

	@Test
	public void testCompareLargeValuesWithoutPrecisionLoss() {
		// As double both values would be equal
		assertEquals(1, NumberUtilities.compare(Long.MAX_VALUE, Long.MAX_VALUE - 1));
		assertEquals(-1, NumberUtilities.compare(Long.MAX_VALUE - 1, (double) Long.MAX_VALUE));
		assertEquals(1, NumberUtilities.compare(new BigInteger("123456789012345678901234567891"), new BigInteger("123456789012345678901234567890")));
		assertEquals(-1, NumberUtilities.compare(new BigDecimal("1.00000000000000000000001"), new BigDecimal("1.00000000000000000000002")));
	}

	@Test
	public void testCompareNonFiniteValues() {
		assertEquals(0, NumberUtilities.compare(Double.NaN, Double.NaN));
		assertEquals(0, NumberUtilities.compare(Float.NaN, Double.NaN));
		assertEquals(1, NumberUtilities.compare(Double.NaN, Double.POSITIVE_INFINITY));
		assertEquals(-1, NumberUtilities.compare(Double.POSITIVE_INFINITY, Double.NaN));
		assertEquals(1, NumberUtilities.compare(Double.NaN, 1));
		assertEquals(-1, NumberUtilities.compare(1, Double.NaN));
		assertEquals(1, NumberUtilities.compare(Double.POSITIVE_INFINITY, Long.MAX_VALUE));
		assertEquals(-1, NumberUtilities.compare(Double.NEGATIVE_INFINITY, Long.MIN_VALUE));
		assertEquals(1, NumberUtilities.compare(Long.MIN_VALUE, Float.NEGATIVE_INFINITY));
		assertEquals(0, NumberUtilities.compare(Float.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
		assertEquals(-1, NumberUtilities.compare(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
	}

	@Test
	public void testCompareBigDecimalBeyondDoubleRange() {
		final BigDecimal huge = new BigDecimal("1E+400");
		// huge.doubleValue() is infinity, but the value is still finite and smaller than infinity
		assertEquals(-1, NumberUtilities.compare(huge, Double.POSITIVE_INFINITY));
		assertEquals(1, NumberUtilities.compare(Double.POSITIVE_INFINITY, huge));
		assertEquals(1, NumberUtilities.compare(huge.negate(), Double.NEGATIVE_INFINITY));
		assertEquals(1, NumberUtilities.compare(huge, Double.MAX_VALUE));
		assertEquals(-1, NumberUtilities.compare(huge, Double.NaN));
	}

	@Test
	public void testCompareNull() {
		assertThrows(NullPointerException.class, () -> NumberUtilities.compare(null, 1));
		assertThrows(NullPointerException.class, () -> NumberUtilities.compare(1, null));
	}

	@Test
	public void testParseNumberIntegerTypes() {
		assertEquals(Integer.valueOf(0), NumberUtilities.parseNumber("0"));
		assertEquals(Integer.valueOf(-5), NumberUtilities.parseNumber("-5"));
		assertEquals(Integer.valueOf(5), NumberUtilities.parseNumber("+5"));
		assertEquals(Integer.valueOf(Integer.MAX_VALUE), NumberUtilities.parseNumber("2147483647"));
		assertEquals(Integer.valueOf(Integer.MIN_VALUE), NumberUtilities.parseNumber("-2147483648"));
		assertEquals(Long.valueOf(2147483648L), NumberUtilities.parseNumber("2147483648"));
		assertEquals(Long.valueOf(Long.MIN_VALUE), NumberUtilities.parseNumber("-9223372036854775808"));
		assertEquals(Long.valueOf(Long.MAX_VALUE), NumberUtilities.parseNumber("9223372036854775807"));
		assertEquals(new BigDecimal("9223372036854775808"), NumberUtilities.parseNumber("9223372036854775808"));
		assertEquals(new BigDecimal("-99999999999999999999"), NumberUtilities.parseNumber("-99999999999999999999"));
	}

	@Test
	public void testParseNumberWithGrouping() {
		assertEquals(Integer.valueOf(1234), NumberUtilities.parseNumber("1,234"));
		assertEquals(Integer.valueOf(1234567), NumberUtilities.parseNumber("1,234,567"));
		assertEquals(Integer.valueOf(-123456), NumberUtilities.parseNumber("-123,456"));
		assertEquals(Long.valueOf(12345678901L), NumberUtilities.parseNumber("12,345,678,901"));
		assertEquals(Float.valueOf(1234.5f), NumberUtilities.parseNumber("1,234.5"));

		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("1,23"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("1,2345"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("1234,567"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber(",123"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("1,,234"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("1.234,5"));
	}

	@Test
	public void testParseNumberDecimalTypes() {
		assertEquals(Float.valueOf(1.5f), NumberUtilities.parseNumber("1.5"));
		assertEquals(Float.valueOf(0.1f), NumberUtilities.parseNumber("0.1"));
		assertEquals(Float.valueOf(0.5f), NumberUtilities.parseNumber(".5"));
		assertEquals(Float.valueOf(1.0f), NumberUtilities.parseNumber("1."));
		assertEquals(Float.valueOf(1000f), NumberUtilities.parseNumber("1e3"));
		assertEquals(Float.valueOf(-0.0125f), NumberUtilities.parseNumber("-1.25E-2"));
		// More digits than a float can represent
		assertEquals(Double.valueOf(0.1000000001), NumberUtilities.parseNumber("0.1000000001"));
		assertEquals(Double.valueOf(1e300), NumberUtilities.parseNumber("1e300"));
		// Neither float nor double can represent these values exactly
		assertEquals(new BigDecimal("0.12345678901234567890"), NumberUtilities.parseNumber("0.12345678901234567890"));
		assertEquals(new BigDecimal("1e400"), NumberUtilities.parseNumber("1e400"));
		assertEquals(new BigDecimal("1e-400"), NumberUtilities.parseNumber("1e-400"));
	}

	@Test
	public void testParseNumberInvalid() {
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber(null));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber(""));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("."));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("-"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("e5"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("1e"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber(" 1"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("1.2.3"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("abc"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("NaN"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("0x10"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("\u0661"));
		// Exponent beyond the int range of BigDecimal
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseNumber("1e9999999999"));
	}

	@Test
	public void testIsHexNumber() {
		assertTrue(NumberUtilities.isHexNumber("0x0"));
		assertTrue(NumberUtilities.isHexNumber("0x1F"));
		assertTrue(NumberUtilities.isHexNumber("0Xabcdef"));
		assertFalse(NumberUtilities.isHexNumber(null));
		assertFalse(NumberUtilities.isHexNumber(""));
		assertFalse(NumberUtilities.isHexNumber("0x"));
		assertFalse(NumberUtilities.isHexNumber("1F"));
		assertFalse(NumberUtilities.isHexNumber("0x1G"));
		assertFalse(NumberUtilities.isHexNumber("-0x1"));
		assertFalse(NumberUtilities.isHexNumber(" 0x1"));
	}

	@Test
	public void testParseHexNumber() {
		assertEquals(Integer.valueOf(0), NumberUtilities.parseHexNumber("0x0"));
		assertEquals(Integer.valueOf(31), NumberUtilities.parseHexNumber("0x1f"));
		assertEquals(Integer.valueOf(Integer.MAX_VALUE), NumberUtilities.parseHexNumber("0x7FFFFFFF"));
		// Values are never negative, so 0xFFFFFFFF needs a Long
		assertEquals(Long.valueOf(2147483648L), NumberUtilities.parseHexNumber("0x80000000"));
		assertEquals(Long.valueOf(4294967295L), NumberUtilities.parseHexNumber("0xFFFFFFFF"));
		assertEquals(Long.valueOf(Long.MAX_VALUE), NumberUtilities.parseHexNumber("0x7FFFFFFFFFFFFFFF"));
		assertEquals(new BigInteger("8000000000000000", 16), NumberUtilities.parseHexNumber("0x8000000000000000"));
		// Leading zeros do not change the type
		assertEquals(Integer.valueOf(1), NumberUtilities.parseHexNumber("0x0000000000000000000001"));

		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseHexNumber(null));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseHexNumber("0x"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseHexNumber("FF"));
		assertThrows(NumberFormatException.class, () -> NumberUtilities.parseHexNumber("0xFG"));
	}

	@Test
	public void testFormatNumber() {
		assertNull(NumberUtilities.formatNumber(null, ',', null));
		assertEquals("1234,5", NumberUtilities.formatNumber(1234.5, ',', null));
		assertEquals("1234,5", NumberUtilities.formatNumber(1234.5, ',', '.'));
		assertEquals("-0,25", NumberUtilities.formatNumber(new BigDecimal("-0.25"), ',', null));
		assertEquals("42", NumberUtilities.formatNumber(42, ',', null));
		// A '.' as decimal separator returns the text unchanged
		assertEquals("1234.5", NumberUtilities.formatNumber(1234.5, '.', null));
		// The exponent notation of toString is kept
		assertEquals("1,0E10", NumberUtilities.formatNumber(1.0E10, ',', null));
	}

	@Test
	public void testFormatNumberReplacesGroupingSeparatorInSinglePass() {
		final Number groupedNumber = new GroupedNumber("1,234,567.89");
		assertEquals("1.234.567,89", NumberUtilities.formatNumber(groupedNumber, ',', '.'));
		assertEquals("1234567,89", NumberUtilities.formatNumber(groupedNumber, ',', null));
		assertEquals("1 234 567,89", NumberUtilities.formatNumber(groupedNumber, ',', ' '));
		assertEquals("1,234,567.89", NumberUtilities.formatNumber(groupedNumber, '.', null));
	}

	@Test
	public void testFormatNumberWithMinimumPreSeparatorCharacters() {
		assertNull(NumberUtilities.formatNumber(null, 5, ',', null));
		assertEquals("   12,5", NumberUtilities.formatNumber(12.5, 5, ',', null));
		assertEquals("  -12,5", NumberUtilities.formatNumber(-12.5, 5, ',', null));
		assertEquals("   42", NumberUtilities.formatNumber(42, 5, ',', null));
		assertEquals("123456,5", NumberUtilities.formatNumber(123456.5, 5, ',', null));
		assertEquals("12.5", NumberUtilities.formatNumber(12.5, 0, '.', null));
		assertEquals("  12.5", NumberUtilities.formatNumber(12.5, 4, '.', null));

		// Right alignment of a column
		final String a = NumberUtilities.formatNumber(1.25, 4, ',', null);
		final String b = NumberUtilities.formatNumber(1000.5, 4, ',', null);
		assertEquals(a.indexOf(','), b.indexOf(','));
	}

	@Test
	public void testIsIntegerBigDecimal() {
		assertFalse(NumberUtilities.isInteger((BigDecimal) null));
		assertTrue(NumberUtilities.isInteger(BigDecimal.ZERO));
		assertTrue(NumberUtilities.isInteger(new BigDecimal("0.000")));
		assertTrue(NumberUtilities.isInteger(new BigDecimal("5")));
		assertTrue(NumberUtilities.isInteger(new BigDecimal("5.000")));
		assertTrue(NumberUtilities.isInteger(new BigDecimal("-10.0")));
		assertTrue(NumberUtilities.isInteger(new BigDecimal("1E+3")));
		assertTrue(NumberUtilities.isInteger(new BigDecimal("1000").scaleByPowerOfTen(-3)));
		assertFalse(NumberUtilities.isInteger(new BigDecimal("5.5")));
		assertFalse(NumberUtilities.isInteger(new BigDecimal("0.5")));
		assertFalse(NumberUtilities.isInteger(new BigDecimal("-0.001")));
		assertFalse(NumberUtilities.isInteger(new BigDecimal("100.001")));
	}

	@Test
	public void testIsIntegerBigDecimalWithHugeScale() {
		// Must return quickly, BigInteger.TEN.pow(scale) would take very long
		assertFalse(NumberUtilities.isInteger(new BigDecimal("1E-999999999")));
		assertTrue(NumberUtilities.isInteger(new BigDecimal(BigInteger.TEN.pow(5), 4)));
		assertTrue(NumberUtilities.isInteger(new BigDecimal("1E+999999999")));
	}

	@Test
	public void testDecimalSeparatorOfDefaultLocaleIsIrrelevant() {
		// formatNumber works on toString, which never uses the locale
		final char localeSeparator = DecimalFormatSymbols.getInstance().getDecimalSeparator();
		assertEquals("1;5", NumberUtilities.formatNumber(1.5, ';', null), "Default locale separator: " + localeSeparator);
	}

	/**
	 * Number whose text contains grouping separators, which is not the case for the JDK number types.
	 */
	private static class GroupedNumber extends Number {
		private static final long serialVersionUID = 1L;

		private final String text;

		GroupedNumber(final String text) {
			this.text = text;
		}

		@Override
		public int intValue() {
			return (int) doubleValue();
		}

		@Override
		public long longValue() {
			return (long) doubleValue();
		}

		@Override
		public float floatValue() {
			return (float) doubleValue();
		}

		@Override
		public double doubleValue() {
			return Double.parseDouble(text.replace(",", ""));
		}

		@Override
		public String toString() {
			return text;
		}
	}
}
