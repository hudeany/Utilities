package de.soderer.utilities;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class StochasticUtilitiesTest {
	@Test
	public void test_UUID() {
		try {
			final Collection<List<String>> result = StochasticUtilities.createPermutations(new String[]{"a", "b", "c"});
			assertNotNull(result);
		} catch (final Exception e) {
			fail(e.getMessage());
		}
	}
}
