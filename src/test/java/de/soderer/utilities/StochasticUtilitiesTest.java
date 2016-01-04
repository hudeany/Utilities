package de.soderer.utilities;

import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class StochasticUtilitiesTest {
	@Test
	public void test_UUID() {
		try {
			Collection<List<String>> result = StochasticUtilities.createPermutations(new String[]{"a", "b", "c"});
			Assert.assertNotNull(result);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
