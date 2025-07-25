package de.soderer.utilities;

import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

@SuppressWarnings("static-method")
public class StochasticUtilitiesTest {
	@Test
	public void test_UUID() {
		try {
			final Collection<List<String>> result = StochasticUtilities.createPermutations(new String[]{"a", "b", "c"});
			Assert.assertNotNull(result);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
