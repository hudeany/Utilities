package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class NumberUtilitiesTest {
	@Test
	public void test1() {
		try {
			assertTrue(NumberUtilities.isInteger("1"));
			assertTrue(NumberUtilities.isInteger("1234567890"));
			assertTrue(NumberUtilities.isInteger("+1234567890"));
			assertTrue(NumberUtilities.isInteger("-1234567890"));

			assertFalse(NumberUtilities.isInteger("+123456.7890"));
			assertFalse(NumberUtilities.isInteger("123456.7890"));
			assertFalse(NumberUtilities.isInteger("123456,7890"));
			assertFalse(NumberUtilities.isInteger("-123456.7890"));
			assertFalse(NumberUtilities.isInteger("1234890 "));
			assertFalse(NumberUtilities.isInteger("1234890abc"));
			assertFalse(NumberUtilities.isInteger("ab1234890"));
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}
}
