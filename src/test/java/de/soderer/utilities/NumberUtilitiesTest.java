package de.soderer.utilities;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class NumberUtilitiesTest {
	@Test
	public void test1() {
		try {
			Assert.assertTrue(NumberUtilities.isInteger("1"));
			Assert.assertTrue(NumberUtilities.isInteger("1234567890"));
			Assert.assertTrue(NumberUtilities.isInteger("+1234567890"));
			Assert.assertTrue(NumberUtilities.isInteger("-1234567890"));

			Assert.assertFalse(NumberUtilities.isInteger("+123456.7890"));
			Assert.assertFalse(NumberUtilities.isInteger("123456.7890"));
			Assert.assertFalse(NumberUtilities.isInteger("123456,7890"));
			Assert.assertFalse(NumberUtilities.isInteger("-123456.7890"));
			Assert.assertFalse(NumberUtilities.isInteger("1234890 "));
			Assert.assertFalse(NumberUtilities.isInteger("1234890abc"));
			Assert.assertFalse(NumberUtilities.isInteger("ab1234890"));
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
