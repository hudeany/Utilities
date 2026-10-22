package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class ExactNumberTest {
	@Test
	public void testDefaultConstructor() {
		final ExactNumber number = new ExactNumber();
		assertEquals("0", number.toString(Locale.ROOT));
		assertEquals(BigDecimal.ZERO, number.toBigDecimal());
	}

	@Test
	public void testLongConstructor() {
		assertEquals("42", new ExactNumber(42).toString(Locale.ROOT));
		assertEquals("-42", new ExactNumber(-42).toString(Locale.ROOT));
		assertEquals(String.valueOf(Long.MAX_VALUE), new ExactNumber(Long.MAX_VALUE).toString(Locale.ROOT));
		assertEquals(String.valueOf(Long.MIN_VALUE), new ExactNumber(Long.MIN_VALUE).toString(Locale.ROOT));
	}

	@Test
	public void testParseValidValues() throws Exception {
		assertEquals("12.5", new ExactNumber("12.5").toString(Locale.ROOT));
		assertEquals("12.5", new ExactNumber("12,5").toString(Locale.ROOT));
		assertEquals("12.5", new ExactNumber("12\u066B5").toString(Locale.ROOT));
		assertEquals("-12.5", new ExactNumber("-12.5").toString(Locale.ROOT));
		assertEquals("12.5", new ExactNumber("+12.5").toString(Locale.ROOT));
		assertEquals("0.5", new ExactNumber(".5").toString(Locale.ROOT));
		assertEquals("-0.5", new ExactNumber("-.5").toString(Locale.ROOT));
		assertEquals("5", new ExactNumber("5.").toString(Locale.ROOT));
		assertEquals("0.05", new ExactNumber("0.05").toString(Locale.ROOT));
		assertEquals("-0.05", new ExactNumber("-0.05").toString(Locale.ROOT));
		assertEquals("1234567.890123", new ExactNumber("1234567.890123").toString(Locale.ROOT));
	}

	@Test
	public void testParseNormalizesValue() throws Exception {
		assertEquals("12.5", new ExactNumber("0012.500").toString(Locale.ROOT));
		assertEquals("12", new ExactNumber("12.000").toString(Locale.ROOT));
		assertEquals("0", new ExactNumber("000").toString(Locale.ROOT));
		assertEquals("0", new ExactNumber("0.000").toString(Locale.ROOT));
		assertEquals("0", new ExactNumber("-0").toString(Locale.ROOT));
		assertEquals("0", new ExactNumber("-0.0").toString(Locale.ROOT));
		assertEquals(new ExactNumber("1.5"), new ExactNumber("1.50"));
		assertEquals(new ExactNumber(0), new ExactNumber("-0.00"));
	}

	@Test
	public void testParseIgnoresBlanksAndTabs() throws Exception {
		assertEquals("1234.5", new ExactNumber(" 1 234 . 5\t").toString(Locale.ROOT));
		assertEquals("-12", new ExactNumber("- 12").toString(Locale.ROOT));
	}

	@Test
	public void testParseBlankOrNullIsZero() throws Exception {
		assertEquals(new ExactNumber(), new ExactNumber((String) null));
		assertEquals(new ExactNumber(), new ExactNumber(""));
		assertEquals(new ExactNumber(), new ExactNumber("  \t "));
	}

	@Test
	public void testParseLongLimits() throws Exception {
		assertEquals(String.valueOf(Long.MAX_VALUE), new ExactNumber("9223372036854775807").toString(Locale.ROOT));
		assertEquals(String.valueOf(Long.MIN_VALUE), new ExactNumber("-9223372036854775808").toString(Locale.ROOT));
		assertEquals("922337203685477580.7", new ExactNumber("922337203685477580.7").toString(Locale.ROOT));
		// Trailing zeros of the decimal places do not count against the range
		assertEquals("1", new ExactNumber("1.0000000000000000000000000000").toString(Locale.ROOT));

		assertThrows(Exception.class, () -> new ExactNumber("9223372036854775808"));
		assertThrows(Exception.class, () -> new ExactNumber("-9223372036854775809"));
		assertThrows(Exception.class, () -> new ExactNumber("92233720368547758.080"));
		assertThrows(Exception.class, () -> new ExactNumber("0.92345678901234567891"));
	}

	@Test
	public void testParseInvalidValues() {
		assertThrows(Exception.class, () -> new ExactNumber("."));
		assertThrows(Exception.class, () -> new ExactNumber("-"));
		assertThrows(Exception.class, () -> new ExactNumber("+"));
		assertThrows(Exception.class, () -> new ExactNumber("-."));
		assertThrows(Exception.class, () -> new ExactNumber("1.2.3"));
		assertThrows(Exception.class, () -> new ExactNumber("1,234.5"));
		assertThrows(Exception.class, () -> new ExactNumber("1.234,5"));
		assertThrows(Exception.class, () -> new ExactNumber("--1"));
		assertThrows(Exception.class, () -> new ExactNumber("+-1"));
		assertThrows(Exception.class, () -> new ExactNumber("1-"));
		assertThrows(Exception.class, () -> new ExactNumber("1.-5"));
		assertThrows(Exception.class, () -> new ExactNumber("1e3"));
		assertThrows(Exception.class, () -> new ExactNumber("abc"));
		assertThrows(Exception.class, () -> new ExactNumber("0x10"));
		assertThrows(Exception.class, () -> new ExactNumber("1\n"));
		// Only ASCII digits are accepted
		assertThrows(Exception.class, () -> new ExactNumber("\u0661\u0662"));
		assertThrows(Exception.class, () -> new ExactNumber("\uFF11"));
	}

	@Test
	public void testToStringWithLocale() throws Exception {
		final ExactNumber number = new ExactNumber("-1234.05");
		assertEquals("-1234.05", number.toString(Locale.US));
		assertEquals("-1234,05", number.toString(Locale.GERMANY));
		assertEquals("-1234,05", number.toString(Locale.FRANCE));
		// Integers never have a decimal separator
		assertEquals("1234", new ExactNumber("1234.000").toString(Locale.GERMANY));
	}

	@Test
	public void testToStringUsesDefaultFormatLocale() throws Exception {
		final Locale defaultFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
		try {
			Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
			assertEquals("1,5", new ExactNumber("1.5").toString());
			Locale.setDefault(Locale.Category.FORMAT, Locale.US);
			assertEquals("1.5", new ExactNumber("1.5").toString());
		} finally {
			Locale.setDefault(Locale.Category.FORMAT, defaultFormatLocale);
		}
	}

	@Test
	public void testToStringCanBeParsedForAllLocales() throws Exception {
		final ExactNumber number = new ExactNumber("-1234.05");
		for (final Locale locale : Locale.getAvailableLocales()) {
			final String text = number.toString(locale);
			final char separator = DecimalFormatSymbols.getInstance(locale).getDecimalSeparator();
			assertEquals(number, new ExactNumber(text), "Locale " + locale + " with decimal separator U+" + Integer.toHexString(separator) + ": " + text);
		}
	}

	@Test
	public void testToBigDecimal() throws Exception {
		assertEquals(new BigDecimal("12.5"), new ExactNumber("12.50").toBigDecimal());
		assertEquals(new BigDecimal("-0.05"), new ExactNumber("-0.05").toBigDecimal());
		assertEquals(new BigDecimal("1000"), new ExactNumber(1000).toBigDecimal());
	}

	@Test
	public void testAddLong() throws Exception {
		final ExactNumber number = new ExactNumber("1.5");
		assertSame(number, number.add(2));
		assertEquals("3.5", number.toString(Locale.ROOT));
		number.add(-5);
		assertEquals("-1.5", number.toString(Locale.ROOT));
		assertEquals("7", new ExactNumber(3).add(4).toString(Locale.ROOT));
	}

	@Test
	public void testAddString() throws Exception {
		final ExactNumber number = new ExactNumber("1.5");
		assertSame(number, number.add("0.25"));
		assertEquals("1.75", number.toString(Locale.ROOT));
		number.add("0,25");
		assertEquals("2", number.toString(Locale.ROOT));
		number.add("-2.001");
		assertEquals("-0.001", number.toString(Locale.ROOT));
		number.add("10");
		assertEquals("9.999", number.toString(Locale.ROOT));
		number.add(".001");
		assertEquals("10", number.toString(Locale.ROOT));
	}

	@Test
	public void testAddWithoutFloatingPointErrors() throws Exception {
		final ExactNumber number = new ExactNumber();
		for (int i = 0; i < 10; i++) {
			number.add("0.1");
		}
		assertEquals(new ExactNumber(1), number);
		// The double calculation is not exact
		double doubleSum = 0;
		for (int i = 0; i < 10; i++) {
			doubleSum += 0.1;
		}
		assertNotEquals(1.0, doubleSum);
	}

	@Test
	public void testAddResultIsNormalized() throws Exception {
		final ExactNumber number = new ExactNumber("0.5");
		number.add("0.5");
		assertEquals(new ExactNumber(1), number);
		assertEquals(new ExactNumber(1).hashCode(), number.hashCode());

		final ExactNumber zero = new ExactNumber("0.5").add("-0.5");
		assertEquals(new ExactNumber(), zero);
		assertEquals("0", zero.toString(Locale.ROOT));
	}

	@Test
	public void testAddBlankOrNullAddsNothing() throws Exception {
		final ExactNumber number = new ExactNumber("1.5");
		number.add((String) null);
		number.add("");
		number.add("  ");
		assertEquals(new ExactNumber("1.5"), number);
	}

	@Test
	public void testAddInvalidStringLeavesNumberUnchanged() throws Exception {
		final ExactNumber number = new ExactNumber("1.5");
		assertThrows(Exception.class, () -> number.add("abc"));
		assertThrows(Exception.class, () -> number.add("."));
		assertEquals(new ExactNumber("1.5"), number);
	}

	@Test
	public void testAddLongOverflowLeavesNumberUnchanged() throws Exception {
		final ExactNumber number = new ExactNumber(Long.MAX_VALUE);
		assertThrows(ArithmeticException.class, () -> number.add(1));
		assertEquals(new ExactNumber(Long.MAX_VALUE), number);

		// The integer value is scaled by the decimal places, which can overflow too
		final ExactNumber decimalNumber = new ExactNumber("0.000000001");
		assertThrows(ArithmeticException.class, () -> decimalNumber.add(Long.MAX_VALUE / 10));
		assertEquals(new ExactNumber("0.000000001"), decimalNumber);
	}

	@Test
	public void testAddStringOverflowLeavesNumberUnchanged() throws Exception {
		final ExactNumber number = new ExactNumber("9223372036854775807");
		assertThrows(ArithmeticException.class, () -> number.add("1"));
		assertEquals(new ExactNumber(Long.MAX_VALUE), number);

		// Scaling this number to more decimal places overflows
		assertThrows(ArithmeticException.class, () -> number.add("0.5"));
		assertEquals(new ExactNumber(Long.MAX_VALUE), number);

		// Scaling this number succeeds, but the sum overflows: the decimal places must not change either
		final ExactNumber largeNumber = new ExactNumber("922337203685477580");
		assertThrows(ArithmeticException.class, () -> largeNumber.add("0.8"));
		assertEquals(new ExactNumber("922337203685477580"), largeNumber);
		assertEquals("922337203685477580", largeNumber.toString(Locale.ROOT));
		largeNumber.add("0.7");
		assertEquals("922337203685477580.7", largeNumber.toString(Locale.ROOT));

		// Scaling the added value overflows
		final ExactNumber decimalNumber = new ExactNumber("0.0000000001");
		assertThrows(ArithmeticException.class, () -> decimalNumber.add("9223372036854775807"));
		assertEquals(new ExactNumber("0.0000000001"), decimalNumber);
	}

	@Test
	public void testAddNegativeLimit() throws Exception {
		final ExactNumber number = new ExactNumber(Long.MIN_VALUE + 1);
		number.add(-1);
		assertEquals(new ExactNumber(Long.MIN_VALUE), number);
		assertThrows(ArithmeticException.class, () -> number.add(-1));
		assertEquals(new ExactNumber(Long.MIN_VALUE), number);
	}

	@SuppressWarnings("unlikely-arg-type")
	@Test
	public void testEqualsAndHashCode() throws Exception {
		final ExactNumber a = new ExactNumber("2.50");
		final ExactNumber b = new ExactNumber("+002.5");
		final ExactNumber c = new ExactNumber("1.5").add(1);
		assertEquals(a, b);
		assertEquals(a, c);
		assertEquals(a.hashCode(), b.hashCode());
		assertEquals(a.hashCode(), c.hashCode());
		assertEquals(a, a);

		assertNotEquals(a, new ExactNumber("2.51"));
		assertNotEquals(a, new ExactNumber("-2.5"));
		// Same digits, different decimal places
		assertNotEquals(new ExactNumber("2.5"), new ExactNumber("25"));
		assertNotEquals(new ExactNumber("2.5"), new ExactNumber("0.25"));
		assertFalse(a.equals(null));
		assertFalse(a.equals("2.5"));
		assertFalse(a.equals(new BigDecimal("2.5")));
		assertTrue(new ExactNumber(25).equals(new ExactNumber("25.0")));
	}
}
